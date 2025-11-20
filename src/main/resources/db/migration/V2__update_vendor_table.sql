-- Add new columns to vendors table
ALTER TABLE vendors
    ADD COLUMN IF NOT EXISTS vendor_site VARCHAR(50),
    ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_line3 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS branch_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vendor_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS start_date_activity DATE,
    ADD COLUMN IF NOT EXISTS attribute_12 VARCHAR(100),
    ADD COLUMN IF NOT EXISTS attribute_13 VARCHAR(100),
    ADD COLUMN IF NOT EXISTS freight_terms_lookup_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS payment_method_lookup_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bank_account_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS attribute_2 VARCHAR(100),
    ADD COLUMN IF NOT EXISTS attribute_3 VARCHAR(100);

-- Rename postal_code to pincode if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'vendors' AND column_name = 'postal_code') THEN
        ALTER TABLE vendors RENAME COLUMN postal_code TO pincode;
    END IF;
END $$;

-- Rename address to address_line1 if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'vendors' AND column_name = 'address' AND 
               NOT EXISTS (SELECT 1 FROM information_schema.columns 
                          WHERE table_name = 'vendors' AND column_name = 'address_line1')) THEN
        ALTER TABLE vendors RENAME COLUMN address TO address_line1;
    END IF;
END $$;

-- Update NOT NULL constraints to allow NULL for H2H-optional fields
ALTER TABLE vendors 
    ALTER COLUMN email DROP NOT NULL,
    ALTER COLUMN bank_account DROP NOT NULL,
    ALTER COLUMN ifsc_code DROP NOT NULL,
    ALTER COLUMN pan_number DROP NOT NULL,
    ALTER COLUMN pay_group DROP NOT NULL,
    ALTER COLUMN bank_name DROP NOT NULL,
    ALTER COLUMN address_line1 DROP NOT NULL;

-- Add comments for new columns
COMMENT ON COLUMN vendors.vendor_site IS 'Vendor site code';
COMMENT ON COLUMN vendors.address_line1 IS 'First line of address';
COMMENT ON COLUMN vendors.address_line2 IS 'Second line of address';
COMMENT ON COLUMN vendors.address_line3 IS 'Third line of address';
COMMENT ON COLUMN vendors.branch_name IS 'Branch name of the bank';
COMMENT ON COLUMN vendors.vendor_type IS 'Type of vendor';
COMMENT ON COLUMN vendors.start_date_activity IS 'Date when vendor became active';
COMMENT ON COLUMN vendors.freight_terms_lookup_code IS 'Freight terms code';
COMMENT ON COLUMN vendors.payment_method_lookup_code IS 'Payment method code';
COMMENT ON COLUMN vendors.bank_account_name IS 'Name on the bank account';
