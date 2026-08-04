USE RestaurantDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.table_areas', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.table_areas (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_table_areas PRIMARY KEY,
        code VARCHAR(50) NULL,
        name_vi NVARCHAR(150) NOT NULL,
        name_en NVARCHAR(150) NULL,
        description_vi NVARCHAR(500) NULL,
        description_en NVARCHAR(500) NULL,
        image_url NVARCHAR(500) NULL,
        base_price DECIMAL(18,0) NULL CONSTRAINT DF_table_areas_base_price DEFAULT 0,
        capacity INT NULL CONSTRAINT DF_table_areas_capacity DEFAULT 0,
        status VARCHAR(30) NULL CONSTRAINT DF_table_areas_status DEFAULT 'ACTIVE',
        display_order INT NULL CONSTRAINT DF_table_areas_display_order DEFAULT 0,
        note NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_table_areas_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_table_areas_updated_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF COL_LENGTH('dbo.table_areas', 'code') IS NULL ALTER TABLE dbo.table_areas ADD code VARCHAR(50) NULL;
IF COL_LENGTH('dbo.table_areas', 'name_en') IS NULL ALTER TABLE dbo.table_areas ADD name_en NVARCHAR(150) NULL;
IF COL_LENGTH('dbo.table_areas', 'description_vi') IS NULL ALTER TABLE dbo.table_areas ADD description_vi NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.table_areas', 'description_en') IS NULL ALTER TABLE dbo.table_areas ADD description_en NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.table_areas', 'image_url') IS NULL ALTER TABLE dbo.table_areas ADD image_url NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.table_areas', 'base_price') IS NULL ALTER TABLE dbo.table_areas ADD base_price DECIMAL(18,0) NULL CONSTRAINT DF_table_areas_base_price_late DEFAULT 0;
IF COL_LENGTH('dbo.table_areas', 'capacity') IS NULL ALTER TABLE dbo.table_areas ADD capacity INT NULL CONSTRAINT DF_table_areas_capacity_late DEFAULT 0;
IF COL_LENGTH('dbo.table_areas', 'status') IS NULL ALTER TABLE dbo.table_areas ADD status VARCHAR(30) NULL CONSTRAINT DF_table_areas_status_late DEFAULT 'ACTIVE';
IF COL_LENGTH('dbo.table_areas', 'display_order') IS NULL ALTER TABLE dbo.table_areas ADD display_order INT NULL CONSTRAINT DF_table_areas_display_order_late DEFAULT 0;
IF COL_LENGTH('dbo.table_areas', 'note') IS NULL ALTER TABLE dbo.table_areas ADD note NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.table_areas', 'created_at') IS NULL ALTER TABLE dbo.table_areas ADD created_at DATETIME2 NOT NULL CONSTRAINT DF_table_areas_created_at_late DEFAULT SYSUTCDATETIME();
IF COL_LENGTH('dbo.table_areas', 'updated_at') IS NULL ALTER TABLE dbo.table_areas ADD updated_at DATETIME2 NOT NULL CONSTRAINT DF_table_areas_updated_at_late DEFAULT SYSUTCDATETIME();
GO

IF OBJECT_ID(N'dbo.reservations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservations (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_reservations PRIMARY KEY,
        reservation_code VARCHAR(30) NOT NULL,
        customer_name NVARCHAR(150) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        customer_email VARCHAR(150) NULL,
        contact_note NVARCHAR(500) NULL,
        reservation_date DATE NOT NULL,
        arrival_time TIME(0) NOT NULL,
        expected_duration_minutes INT NOT NULL CONSTRAINT DF_reservations_duration DEFAULT 120,
        guest_count INT NOT NULL,
        occasion NVARCHAR(80) NULL,
        special_request NVARCHAR(500) NULL,
        seating_preference NVARCHAR(255) NULL,
        area_id INT NULL,
        table_id INT NULL,
        reservation_status VARCHAR(30) NOT NULL CONSTRAINT DF_reservations_status DEFAULT 'PENDING',
        total_amount DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservations_total DEFAULT 0,
        deposit_rate DECIMAL(5,2) NOT NULL CONSTRAINT DF_reservations_deposit_rate DEFAULT 0.50,
        deposit_amount DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservations_deposit DEFAULT 0,
        remaining_amount DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservations_remaining DEFAULT 0,
        deposit_status VARCHAR(30) NOT NULL CONSTRAINT DF_reservations_deposit_status DEFAULT 'PENDING',
        manager_note NVARCHAR(500) NULL,
        confirmed_by VARCHAR(80) NULL,
        confirmed_at DATETIME2 NULL,
        rejected_reason NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_reservations_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_reservations_updated_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF COL_LENGTH('dbo.reservations', 'reservation_code') IS NULL ALTER TABLE dbo.reservations ADD reservation_code VARCHAR(30) NULL;
IF COL_LENGTH('dbo.reservations', 'phone') IS NULL ALTER TABLE dbo.reservations ADD phone NVARCHAR(30) NULL;
IF COL_LENGTH('dbo.reservations', 'arrive_at') IS NULL ALTER TABLE dbo.reservations ADD arrive_at DATETIME2 NULL;
IF COL_LENGTH('dbo.reservations', 'status') IS NULL ALTER TABLE dbo.reservations ADD status NVARCHAR(30) NULL;
IF COL_LENGTH('dbo.reservations', 'customer_phone') IS NULL ALTER TABLE dbo.reservations ADD customer_phone VARCHAR(20) NULL;
IF COL_LENGTH('dbo.reservations', 'customer_email') IS NULL ALTER TABLE dbo.reservations ADD customer_email VARCHAR(150) NULL;
IF COL_LENGTH('dbo.reservations', 'contact_note') IS NULL ALTER TABLE dbo.reservations ADD contact_note NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.reservations', 'reservation_date') IS NULL ALTER TABLE dbo.reservations ADD reservation_date DATE NULL;
IF COL_LENGTH('dbo.reservations', 'arrival_time') IS NULL ALTER TABLE dbo.reservations ADD arrival_time TIME(0) NULL;
IF COL_LENGTH('dbo.reservations', 'expected_duration_minutes') IS NULL ALTER TABLE dbo.reservations ADD expected_duration_minutes INT NULL CONSTRAINT DF_reservations_duration_late DEFAULT 120;
IF COL_LENGTH('dbo.reservations', 'occasion') IS NULL ALTER TABLE dbo.reservations ADD occasion NVARCHAR(80) NULL;
IF COL_LENGTH('dbo.reservations', 'seating_preference') IS NULL ALTER TABLE dbo.reservations ADD seating_preference NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.reservations', 'area_id') IS NULL ALTER TABLE dbo.reservations ADD area_id INT NULL;
IF COL_LENGTH('dbo.reservations', 'table_id') IS NULL ALTER TABLE dbo.reservations ADD table_id INT NULL;
IF COL_LENGTH('dbo.reservations', 'reservation_status') IS NULL ALTER TABLE dbo.reservations ADD reservation_status VARCHAR(30) NULL CONSTRAINT DF_reservations_status_late DEFAULT 'PENDING';
IF COL_LENGTH('dbo.reservations', 'total_amount') IS NULL ALTER TABLE dbo.reservations ADD total_amount DECIMAL(18,0) NULL CONSTRAINT DF_reservations_total_late DEFAULT 0;
IF COL_LENGTH('dbo.reservations', 'deposit_rate') IS NULL ALTER TABLE dbo.reservations ADD deposit_rate DECIMAL(5,2) NULL CONSTRAINT DF_reservations_deposit_rate_late DEFAULT 0.50;
IF COL_LENGTH('dbo.reservations', 'remaining_amount') IS NULL ALTER TABLE dbo.reservations ADD remaining_amount DECIMAL(18,0) NULL CONSTRAINT DF_reservations_remaining_late DEFAULT 0;
IF COL_LENGTH('dbo.reservations', 'deposit_status') IS NULL ALTER TABLE dbo.reservations ADD deposit_status VARCHAR(30) NULL CONSTRAINT DF_reservations_deposit_status_late DEFAULT 'PENDING';
IF COL_LENGTH('dbo.reservations', 'manager_note') IS NULL ALTER TABLE dbo.reservations ADD manager_note NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.reservations', 'confirmed_by') IS NULL ALTER TABLE dbo.reservations ADD confirmed_by VARCHAR(80) NULL;
IF COL_LENGTH('dbo.reservations', 'confirmed_at') IS NULL ALTER TABLE dbo.reservations ADD confirmed_at DATETIME2 NULL;
IF COL_LENGTH('dbo.reservations', 'rejected_reason') IS NULL ALTER TABLE dbo.reservations ADD rejected_reason NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.reservations', 'created_at') IS NULL ALTER TABLE dbo.reservations ADD created_at DATETIME2 NULL CONSTRAINT DF_reservations_created_at_late DEFAULT SYSUTCDATETIME();
IF COL_LENGTH('dbo.reservations', 'updated_at') IS NULL ALTER TABLE dbo.reservations ADD updated_at DATETIME2 NULL CONSTRAINT DF_reservations_updated_at_late DEFAULT SYSUTCDATETIME();
GO

UPDATE dbo.reservations
SET
    reservation_code = COALESCE(reservation_code, CONCAT('LEGACY-', RIGHT(CONCAT('000000', id), 6))),
    customer_phone = COALESCE(customer_phone, CONVERT(VARCHAR(20), phone)),
    reservation_date = COALESCE(reservation_date, CONVERT(DATE, arrive_at), CONVERT(DATE, GETDATE())),
    arrival_time = COALESCE(arrival_time, CONVERT(TIME(0), arrive_at), CONVERT(TIME(0), '18:00')),
    expected_duration_minutes = COALESCE(expected_duration_minutes, 120),
    reservation_status = COALESCE(reservation_status,
        CASE status
            WHEN N'DA_XAC_NHAN' THEN 'DEPOSIT_PAID'
            WHEN N'MOI_DAT' THEN 'PENDING'
            ELSE 'PENDING'
        END),
    total_amount = COALESCE(NULLIF(total_amount, 0), COALESCE(deposit_amount, 0) * 2),
    deposit_rate = COALESCE(deposit_rate, 0.50),
    deposit_amount = COALESCE(deposit_amount, 0),
    remaining_amount = COALESCE(NULLIF(remaining_amount, 0), COALESCE(deposit_amount, 0)),
    deposit_status = COALESCE(deposit_status, CASE WHEN COALESCE(deposit_amount, 0) > 0 THEN 'PAID' ELSE 'PENDING' END),
    created_at = COALESCE(created_at, SYSUTCDATETIME()),
    updated_at = COALESCE(updated_at, SYSUTCDATETIME());
GO

IF OBJECT_ID(N'dbo.reservation_status_history', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_status_history (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_reservation_status_history PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        old_status VARCHAR(30) NULL,
        new_status VARCHAR(30) NOT NULL,
        changed_by VARCHAR(80) NULL,
        note NVARCHAR(500) NULL,
        changed_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_status_history_changed_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF OBJECT_ID(N'dbo.reservation_images', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_images (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_reservation_images PRIMARY KEY,
        area_id INT NULL,
        table_id INT NULL,
        image_url NVARCHAR(500) NOT NULL,
        alt_text_vi NVARCHAR(255) NULL,
        alt_text_en NVARCHAR(255) NULL,
        sort_order INT NOT NULL CONSTRAINT DF_reservation_images_sort_order DEFAULT 0,
        is_primary BIT NOT NULL CONSTRAINT DF_reservation_images_is_primary DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_images_created_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF COL_LENGTH('dbo.reservation_images', 'alt_text_vi') IS NULL ALTER TABLE dbo.reservation_images ADD alt_text_vi NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.reservation_images', 'alt_text_en') IS NULL ALTER TABLE dbo.reservation_images ADD alt_text_en NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.reservation_images', 'sort_order') IS NULL ALTER TABLE dbo.reservation_images ADD sort_order INT NOT NULL CONSTRAINT DF_reservation_images_sort_order_late DEFAULT 0;
IF COL_LENGTH('dbo.reservation_images', 'is_primary') IS NULL ALTER TABLE dbo.reservation_images ADD is_primary BIT NOT NULL CONSTRAINT DF_reservation_images_is_primary_late DEFAULT 0;
GO

IF COL_LENGTH('dbo.restaurant_table', 'min_capacity') IS NULL ALTER TABLE dbo.restaurant_table ADD min_capacity INT NULL CONSTRAINT DF_restaurant_table_min_capacity DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'max_capacity') IS NULL ALTER TABLE dbo.restaurant_table ADD max_capacity INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'seat_count') IS NULL ALTER TABLE dbo.restaurant_table ADD seat_count INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'reservation_price') IS NULL ALTER TABLE dbo.restaurant_table ADD reservation_price DECIMAL(18,0) NULL CONSTRAINT DF_restaurant_table_reservation_price DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'area_id') IS NULL ALTER TABLE dbo.restaurant_table ADD area_id INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'position_description') IS NULL ALTER TABLE dbo.restaurant_table ADD position_description NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.restaurant_table', 'is_window_seat') IS NULL ALTER TABLE dbo.restaurant_table ADD is_window_seat BIT NULL CONSTRAINT DF_restaurant_table_window DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'is_private_room') IS NULL ALTER TABLE dbo.restaurant_table ADD is_private_room BIT NULL CONSTRAINT DF_restaurant_table_private DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'is_child_friendly') IS NULL ALTER TABLE dbo.restaurant_table ADD is_child_friendly BIT NULL CONSTRAINT DF_restaurant_table_child DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'is_active') IS NULL ALTER TABLE dbo.restaurant_table ADD is_active BIT NULL CONSTRAINT DF_restaurant_table_active DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'image_url') IS NULL ALTER TABLE dbo.restaurant_table ADD image_url NVARCHAR(500) NULL;
GO

UPDATE dbo.restaurant_table
SET
    min_capacity = COALESCE(min_capacity, CASE WHEN capacity <= 2 THEN 1 WHEN capacity <= 4 THEN 2 WHEN capacity <= 6 THEN 4 ELSE 6 END),
    max_capacity = COALESCE(max_capacity, capacity, 4),
    seat_count = COALESCE(seat_count, capacity, 4),
    reservation_price = COALESCE(NULLIF(reservation_price, 0), COALESCE(capacity, 4) * 100000),
    is_active = COALESCE(is_active, 1),
    is_child_friendly = COALESCE(is_child_friendly, 1);
GO

IF OBJECT_ID(N'dbo.notifications', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.notifications', 'type') IS NULL ALTER TABLE dbo.notifications ADD [type] VARCHAR(50) NULL;
    IF COL_LENGTH('dbo.notifications', 'title') IS NULL ALTER TABLE dbo.notifications ADD title NVARCHAR(500) NULL;
    IF COL_LENGTH('dbo.notifications', 'message') IS NULL ALTER TABLE dbo.notifications ADD [message] NVARCHAR(MAX) NULL;
    IF COL_LENGTH('dbo.notifications', 'target_role') IS NULL ALTER TABLE dbo.notifications ADD target_role VARCHAR(50) NULL;
    IF COL_LENGTH('dbo.notifications', 'is_read') IS NULL ALTER TABLE dbo.notifications ADD is_read BIT NULL;
    IF COL_LENGTH('dbo.notifications', 'related_entity') IS NULL ALTER TABLE dbo.notifications ADD related_entity VARCHAR(100) NULL;
    IF COL_LENGTH('dbo.notifications', 'related_id') IS NULL ALTER TABLE dbo.notifications ADD related_id VARCHAR(50) NULL;
    IF COL_LENGTH('dbo.notifications', 'severity') IS NULL ALTER TABLE dbo.notifications ADD severity VARCHAR(20) NULL;
END;
GO

IF OBJECT_ID(N'dbo.notifications', N'U') IS NOT NULL
BEGIN
    UPDATE dbo.notifications
    SET
        [type] = COALESCE([type], 'SYSTEM'),
        title = COALESCE(title, sender_name, N'Thông báo'),
        [message] = COALESCE([message], content, N''),
        target_role = COALESCE(target_role, target_department, 'ROLE_MANAGER'),
        is_read = COALESCE(is_read, read_flag, 0),
        severity = COALESCE(severity, level, 'info')
    WHERE [type] IS NULL
       OR title IS NULL
       OR [message] IS NULL
       OR target_role IS NULL
       OR is_read IS NULL
       OR severity IS NULL;
END;
GO

IF OBJECT_ID(N'dbo.activity_logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.activity_logs (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_activity_logs PRIMARY KEY,
        username VARCHAR(50) NULL,
        action VARCHAR(50) NULL,
        entity_type VARCHAR(100) NULL,
        entity_id VARCHAR(50) NULL,
        description NVARCHAR(500) NULL,
        old_value NVARCHAR(MAX) NULL,
        new_value NVARCHAR(MAX) NULL,
        ip_address VARCHAR(50) NULL,
        [timestamp] DATETIME2 NULL CONSTRAINT DF_activity_logs_timestamp DEFAULT SYSUTCDATETIME()
    );
END;
GO
