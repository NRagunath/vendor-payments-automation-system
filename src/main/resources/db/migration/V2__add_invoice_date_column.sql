-- Add invoice_date column to vendor_payments table
ALTER TABLE vendor_payments 
ADD COLUMN IF NOT EXISTS invoice_date DATE;
