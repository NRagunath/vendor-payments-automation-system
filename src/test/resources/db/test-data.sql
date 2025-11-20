-- Clear existing test data
DELETE FROM payments;
DELETE FROM vendors;

-- Insert test vendors
INSERT INTO vendors (vendor_id, name, email, phone, bank_account, bank_name, ifsc_code, pan_number, created_at, updated_at)
VALUES 
('VENDOR001', 'Test Vendor 1', 'vendor1@test.com', '9876543210', '12345678901234', 'Test Bank', 'TEST0012345', 'AAAAA1111A', NOW(), NOW()),
('VENDOR002', 'Test Vendor 2', 'vendor2@test.com', '9876543211', '12345678901235', 'Test Bank', 'TEST0012346', 'BBBBB2222B', NOW(), NOW());

-- Insert test payments
INSERT INTO payments (payment_id, vendor_id, amount, currency, status, invoice_number, description, created_at, updated_at)
VALUES 
('PAY001', 'VENDOR001', 1000.00, 'INR', 'COMPLETED', 'INV-2025-001', 'Test payment 1', NOW(), NOW()),
('PAY002', 'VENDOR002', 1500.50, 'INR', 'PENDING', 'INV-2025-002', 'Test payment 2', NOW(), NOW());
