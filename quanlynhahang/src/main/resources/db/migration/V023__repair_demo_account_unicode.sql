-- These are the known presentation accounts whose Vietnamese text was irreversibly stored as '?'.
-- Restrict the repair to demo usernames so real customer profile data is never overwritten.
UPDATE dbo.accounts
SET fullname = N'Khách hàng mẫu', membership_tier = N'Đồng'
WHERE username = 'customer'
  AND (fullname LIKE '%?%' OR membership_tier LIKE '%?%');

UPDATE dbo.accounts
SET membership_tier = N'Đồng'
WHERE username IN ('waiter', 'kitchen', 'cashier')
  AND membership_tier LIKE '%?%';
