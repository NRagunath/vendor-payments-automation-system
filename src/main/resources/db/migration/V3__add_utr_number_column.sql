-- Add utr_number column to vendor_payments table
ALTER TABLE vendor_payments
ADD COLUMN utr_number VARCHAR(50);

-- Add comment to the column
COMMENT ON COLUMN vendor_payments.utr_number IS 'UTR/Reference number for the transaction';
