package com.bank.loan.common.security.checksum;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Scoped strictly to {@code /internal/**} — never applied to customer-facing
 * {@code /api/v1/**} routes, and never skipped on internal routes either.
 * Wired into the security filter chain only for that path pattern; see
 * {@code SecurityConfig}.
 *
 * <p>Validates the checksum <em>before</em> the request reaches the
 * controller, and re-wraps the body so downstream deserialization still
 * works after the filter has consumed the input stream once.
 */
@RequiredArgsConstructor
public class ChecksumValidationFilter extends OncePerRequestFilter {

    private final ChecksumProperties checksumProperties;
    private final ChecksumUtil checksumUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String caller = request.getHeader(checksumProperties.getCallerHeaderName());
        String timestamp = request.getHeader(checksumProperties.getTimestampHeaderName());
        String checksum = request.getHeader(checksumProperties.getHeaderName());

        if (caller == null || timestamp == null || checksum == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing checksum headers");
            return;
        }
        if (!withinTolerance(timestamp)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Checksum timestamp outside tolerance window");
            return;
        }

        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        String secret = checksumProperties.secretFor(caller);
        if (!checksumUtil.matches(new String(body, StandardCharsets.UTF_8), timestamp, secret, checksum)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Checksum mismatch");
            return;
        }

        chain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
    }

    private boolean withinTolerance(String timestamp) {
        try {
            Instant sent = Instant.parse(timestamp);
            long deltaSeconds = Math.abs(Instant.now().getEpochSecond() - sent.getEpochSecond());
            return deltaSeconds <= checksumProperties.getToleranceSeconds();
        } catch (Exception e) {
            return false;
        }
    }

    /** Replays the already-consumed request body to every downstream reader. */
    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // not needed for synchronous request handling
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
