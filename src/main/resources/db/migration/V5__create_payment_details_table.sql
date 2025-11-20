-- Create payment_details table
CREATE TABLE IF NOT EXISTS payment_details (
    id BIGSERIAL PRIMARY KEY,
    vendor_number VARCHAR(50) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    vendor_site VARCHAR(100),
    pay_group VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_reference VARCHAR(100) UNIQUE,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT fk_vendor FOREIGN KEY (vendor_number) REFERENCES vendor(vendor_number)
);

-- Create index for faster lookups
CREATE INDEX idx_payment_details_vendor_number ON payment_details(vendor_number);
CREATE INDEX idx_payment_details_status ON payment_details(status);
