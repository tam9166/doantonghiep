-- Keep the manager demo credential consistent with the other staff demo accounts.
UPDATE dbo.accounts
SET password = '$2a$10$THmTbUOv7EiCGEzTsbyxRuBb.SWk0zPwcWsMI1Jc31.10CqscP8vi',
    enabled = 1,
    must_change_password = 0
WHERE username = 'manager';
