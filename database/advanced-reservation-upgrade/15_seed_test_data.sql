IF OBJECT_ID('dbo.deposit_policies', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.deposit_policies WHERE policy_code = 'DEFAULT_50')
BEGIN
    INSERT INTO dbo.deposit_policies(policy_code, name_vi, name_en, policy_type, percentage_rate, minimum_amount, priority, is_active)
    VALUES ('DEFAULT_50', N'Cọc mặc định 50%', N'Default 50% deposit', 'PERCENTAGE', 0.50, 0, 100, 1);
END;

IF OBJECT_ID('dbo.notification_channels', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.notification_channels WHERE channel_code = 'IN_APP')
BEGIN
    INSERT INTO dbo.notification_channels(channel_code, name_vi, provider, enabled)
    VALUES ('IN_APP', N'Thông báo trong hệ thống', 'internal', 1);
END;
