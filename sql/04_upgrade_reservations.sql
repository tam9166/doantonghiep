-- Upgrade reservation and table management for Moc Vi.
-- Run on SQL Server database RestaurantDB before deploying the new backend.

IF OBJECT_ID('dbo.table_areas', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.table_areas (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name_vi NVARCHAR(150) NOT NULL,
        name_en NVARCHAR(150) NULL,
        description_vi NVARCHAR(500) NULL,
        description_en NVARCHAR(500) NULL,
        image_url NVARCHAR(500) NULL,
        base_price DECIMAL(18,0) NOT NULL DEFAULT 0,
        capacity INT NOT NULL DEFAULT 0,
        status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END;

IF COL_LENGTH('dbo.restaurant_table', 'min_capacity') IS NULL
    ALTER TABLE dbo.restaurant_table ADD min_capacity INT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'max_capacity') IS NULL
    ALTER TABLE dbo.restaurant_table ADD max_capacity INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'seat_count') IS NULL
    ALTER TABLE dbo.restaurant_table ADD seat_count INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'reservation_price') IS NULL
    ALTER TABLE dbo.restaurant_table ADD reservation_price DECIMAL(18,0) NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'area_id') IS NULL
    ALTER TABLE dbo.restaurant_table ADD area_id INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'position_description') IS NULL
    ALTER TABLE dbo.restaurant_table ADD position_description NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.restaurant_table', 'is_window_seat') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_window_seat BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'is_private_room') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_private_room BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'is_child_friendly') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_child_friendly BIT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'is_active') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_active BIT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'image_url') IS NULL
    ALTER TABLE dbo.restaurant_table ADD image_url NVARCHAR(500) NULL;

IF OBJECT_ID('dbo.FK_restaurant_table_area', 'F') IS NULL
BEGIN
    ALTER TABLE dbo.restaurant_table
        ADD CONSTRAINT FK_restaurant_table_area FOREIGN KEY (area_id) REFERENCES dbo.table_areas(id);
END;

-- SQL Server compiles a batch before executing ALTER TABLE statements. Split
-- here so the following update can safely reference the newly added columns.
GO

IF OBJECT_ID('dbo.reservations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservations (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        reservation_code VARCHAR(30) NOT NULL UNIQUE,
        customer_name NVARCHAR(150) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        customer_email VARCHAR(150) NULL,
        contact_note NVARCHAR(500) NULL,
        reservation_date DATE NOT NULL,
        arrival_time TIME NOT NULL,
        expected_duration_minutes INT NOT NULL DEFAULT 120,
        guest_count INT NOT NULL,
        occasion NVARCHAR(80) NULL,
        special_request NVARCHAR(500) NULL,
        seating_preference NVARCHAR(255) NULL,
        area_id INT NULL,
        table_id INT NULL,
        reservation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
        total_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
        deposit_rate DECIMAL(5,2) NOT NULL DEFAULT 0.50,
        deposit_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
        remaining_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
        deposit_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
        manager_note NVARCHAR(500) NULL,
        confirmed_by VARCHAR(80) NULL,
        confirmed_at DATETIME2 NULL,
        rejected_reason NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_reservations_area FOREIGN KEY (area_id) REFERENCES dbo.table_areas(id),
        CONSTRAINT FK_reservations_table FOREIGN KEY (table_id) REFERENCES dbo.restaurant_table(id)
    );
END;

-- Required by the payment ledger migration that runs when Spring Boot starts.
IF COL_LENGTH('dbo.reservations', 'payment_status') IS NULL
    ALTER TABLE dbo.reservations ADD payment_status VARCHAR(30) NOT NULL
        CONSTRAINT df_setup_reservations_payment_status DEFAULT 'UNPAID';
GO

IF OBJECT_ID('dbo.reservation_images', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_images (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        area_id INT NULL,
        table_id INT NULL,
        image_url NVARCHAR(500) NOT NULL,
        alt_text_vi NVARCHAR(255) NULL,
        alt_text_en NVARCHAR(255) NULL,
        is_primary BIT NOT NULL DEFAULT 0,
        sort_order INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_reservation_images_area FOREIGN KEY (area_id) REFERENCES dbo.table_areas(id),
        CONSTRAINT FK_reservation_images_table FOREIGN KEY (table_id) REFERENCES dbo.restaurant_table(id)
    );
END;

IF OBJECT_ID('dbo.reservation_status_history', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_status_history (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        old_status VARCHAR(30) NULL,
        new_status VARCHAR(30) NOT NULL,
        changed_by VARCHAR(80) NULL,
        note NVARCHAR(500) NULL,
        changed_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_reservation_status_history_reservation FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservations_table_date_status')
    CREATE INDEX IX_reservations_table_date_status ON dbo.reservations(table_id, reservation_date, reservation_status);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservations_code_phone')
    CREATE INDEX IX_reservations_code_phone ON dbo.reservations(reservation_code, customer_phone);

IF NOT EXISTS (SELECT 1 FROM dbo.table_areas)
BEGIN
    INSERT INTO dbo.table_areas (name_vi, name_en, description_vi, description_en, image_url, base_price, capacity, status)
    VALUES
        (N'Tầng 2 - Sảnh tiệc', 'Level 2 - Banquet Hall', N'Không gian rộng cho gia đình và nhóm đông.', 'Spacious hall for families and groups.', '/images/areas/banquet.jpg', 800000, 150, 'ACTIVE'),
        (N'Tầng 3-5 - Phòng VIP', 'Level 3-5 - Private Rooms', N'Phòng riêng yên tĩnh, phù hợp tiếp khách và sinh nhật.', 'Quiet private rooms for business meals and birthdays.', '/images/areas/vip.jpg', 1200000, 90, 'ACTIVE'),
        (N'Tầng 6 - Sân thượng', 'Level 6 - Rooftop', N'Không gian ngoài trời với view phố, sông và sân vườn.', 'Outdoor rooftop with city, river and garden views.', '/images/areas/rooftop.jpg', 600000, 80, 'ACTIVE');
END;

UPDATE dbo.restaurant_table
SET
    max_capacity = ISNULL(max_capacity, ISNULL(capacity, 4)),
    seat_count = ISNULL(seat_count, ISNULL(capacity, 4)),
    reservation_price = CASE WHEN ISNULL(reservation_price, 0) = 0 THEN ISNULL(capacity, 4) * 100000 ELSE reservation_price END,
    is_private_room = CASE WHEN floor LIKE '%VIP%' THEN 1 ELSE ISNULL(is_private_room, 0) END,
    is_window_seat = CASE WHEN ISNULL(has_view, 0) = 1 THEN 1 ELSE ISNULL(is_window_seat, 0) END,
    is_child_friendly = ISNULL(is_child_friendly, 1),
    is_active = ISNULL(is_active, 1)
WHERE 1 = 1;
