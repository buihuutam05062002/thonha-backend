-- V2: Insert default roles
INSERT INTO roles (name) VALUES ('ADMIN') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (name) VALUES ('CUSTOMER') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (name) VALUES ('WORKER') ON DUPLICATE KEY UPDATE name=name;