-- Migration script for email domain configuration

-- Create sequence for email_domain_config table
CREATE SEQUENCE IF NOT EXISTS email_domain_config_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create email_domain_config table
CREATE TABLE IF NOT EXISTS email_domain_config (
    id BIGINT PRIMARY KEY DEFAULT nextval('email_domain_config_seq'),
    domain VARCHAR(255) NOT NULL UNIQUE,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    protocol VARCHAR(10) DEFAULT 'smtp',
    auth BOOLEAN DEFAULT TRUE,
    starttls_enable BOOLEAN DEFAULT TRUE,
    ssl_trust BOOLEAN DEFAULT TRUE,
    connection_timeout INTEGER DEFAULT 5000,
    timeout INTEGER DEFAULT 5000,
    write_timeout INTEGER DEFAULT 5000,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_email_domain_config_domain UNIQUE (domain)
);

-- Create index on domain for faster lookups
CREATE INDEX IF NOT EXISTS idx_email_domain_config_domain ON email_domain_config (domain);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_email_domain_config_updated_at
BEFORE UPDATE ON email_domain_config
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE email_domain_config IS 'Stores SMTP configurations for different email domains';
COMMENT ON COLUMN email_domain_config.domain IS 'The email domain this configuration applies to (e.g., example.com)';
COMMENT ON COLUMN email_domain_config.host IS 'SMTP server hostname';
COMMENT ON COLUMN email_domain_config.port IS 'SMTP server port';
COMMENT ON COLUMN email_domain_config.username IS 'SMTP authentication username';
COMMENT ON COLUMN email_domain_config.password IS 'SMTP authentication password';
COMMENT ON COLUMN email_domain_config.protocol IS 'Email protocol (default: smtp)';
COMMENT ON COLUMN email_domain_config.auth IS 'Whether SMTP authentication is required';
COMMENT ON COLUMN email_domain_config.starttls_enable IS 'Whether to enable STARTTLS';
COMMENT ON COLUMN email_domain_config.ssl_trust IS 'Whether to trust all SSL certificates';
COMMENT ON COLUMN email_domain_config.active IS 'Whether this configuration is active';

-- Create a view for active configurations
CREATE OR REPLACE VIEW vw_active_email_domains AS
SELECT id, domain, host, port, username, protocol, active, created_at, updated_at
FROM email_domain_config
WHERE active = TRUE;

-- Insert default configuration for gmail.com (example)
INSERT INTO email_domain_config (domain, host, port, username, password, protocol, auth, starttls_enable, ssl_trust)
VALUES ('gmail.com', 'smtp.gmail.com', 587, 'noreply@gmail.com', 'your-password', 'smtp', true, true, true)
ON CONFLICT (domain) DO NOTHING;
