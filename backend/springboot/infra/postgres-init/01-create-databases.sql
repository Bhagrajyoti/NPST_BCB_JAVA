-- Each service owns its own database and role; Flyway (run by each service on
-- startup) creates the tables. This script only provisions the databases.
CREATE DATABASE account_service;
CREATE DATABASE funds_transfer_service;
CREATE DATABASE term_deposit_service;
CREATE DATABASE loan_service;

CREATE USER account_service WITH PASSWORD 'changeme';
CREATE USER funds_transfer_service WITH PASSWORD 'changeme';
CREATE USER term_deposit_service WITH PASSWORD 'changeme';
CREATE USER loan_service WITH PASSWORD 'changeme';

GRANT ALL PRIVILEGES ON DATABASE account_service TO account_service;
GRANT ALL PRIVILEGES ON DATABASE funds_transfer_service TO funds_transfer_service;
GRANT ALL PRIVILEGES ON DATABASE term_deposit_service TO term_deposit_service;
GRANT ALL PRIVILEGES ON DATABASE loan_service TO loan_service;
