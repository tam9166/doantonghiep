-- Repair payment intents persisted while the development properties loader
-- interpreted UTF-8 literals as ISO-8859-1 mojibake.
UPDATE dbo.payment_intents
SET account_holder = N'Hoàng Nguyễn Minh Tâm'
WHERE account_holder = N'HoÃ ng Nguyá»n Minh TÃ¢m';
