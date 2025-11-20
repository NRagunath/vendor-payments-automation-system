-- Add index for active domain lookups
CREATE INDEX IF NOT EXISTS idx_email_domain_config_domain_active 
ON email_domain_config(domain) 
WHERE active = true;

-- Add index for domain lookups
CREATE INDEX IF NOT EXISTS idx_email_domain_config_domain 
ON email_domain_config(domain);

-- Add index for performance on frequently filtered columns
CREATE INDEX IF NOT EXISTS idx_email_domain_config_updated_at 
ON email_domain_config(updated_at);
