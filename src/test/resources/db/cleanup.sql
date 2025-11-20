-- Clean up test data after tests
DELETE FROM payments;
DELETE FROM vendors;

-- Reset sequences if using PostgreSQL
-- ALTER SEQUENCE vendors_id_seq RESTART WITH 1;
-- ALTER SEQUENCE payments_id_seq RESTART WITH 1;
