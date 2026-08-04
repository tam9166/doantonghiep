-- Legacy demo staff accounts stored plaintext passwords, which Spring Security rejects.
-- The demo password remains 123 for the presentation environment and is BCrypt encoded.
UPDATE dbo.accounts
SET password = '$2a$10$THmTbUOv7EiCGEzTsbyxRuBb.SWk0zPwcWsMI1Jc31.10CqscP8vi',
    enabled = 1,
    must_change_password = 0
WHERE username IN ('waiter', 'kitchen', 'cashier')
  AND LEN(password) < 60;
