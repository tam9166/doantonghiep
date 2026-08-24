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

IF OBJECT_ID(N'dbo.notifications', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.notifications (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_notifications PRIMARY KEY,
        type VARCHAR(50) NULL,
        title NVARCHAR(500) NULL,
        message NVARCHAR(MAX) NULL,
        target_role VARCHAR(50) NULL,
        is_read BIT NULL CONSTRAINT DF_notifications_is_read DEFAULT 0,
        created_at DATETIME2 NULL CONSTRAINT DF_notifications_created_at DEFAULT SYSUTCDATETIME(),
        related_entity VARCHAR(100) NULL,
        related_id VARCHAR(50) NULL,
        severity VARCHAR(20) NULL
    );
END;

IF OBJECT_ID(N'dbo.payment_webhook_logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_webhook_logs (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_payment_webhook_logs PRIMARY KEY,
        provider VARCHAR(40) NOT NULL,
        provider_transaction_id VARCHAR(120) NOT NULL,
        payment_code VARCHAR(40) NULL,
        transfer_content NVARCHAR(200) NULL,
        amount DECIMAL(18,2) NULL,
        account_number VARCHAR(40) NULL,
        signature_valid BIT NOT NULL CONSTRAINT DF_payment_webhook_signature DEFAULT 0,
        status VARCHAR(30) NOT NULL,
        raw_payload_hash VARCHAR(128) NULL,
        failure_reason NVARCHAR(500) NULL,
        received_at DATETIME2 NOT NULL CONSTRAINT DF_payment_webhook_received DEFAULT SYSUTCDATETIME(),
        processed_at DATETIME2 NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.payment_webhook_logs') AND name = N'UX_payment_webhook_provider_tx')
    CREATE UNIQUE INDEX UX_payment_webhook_provider_tx
        ON dbo.payment_webhook_logs(provider, provider_transaction_id);

IF OBJECT_ID(N'dbo.table_layouts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.table_layouts (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_table_layouts PRIMARY KEY,
        table_id INT NOT NULL,
        area_id INT NULL,
        floor_name NVARCHAR(80) NULL,
        x_position DECIMAL(10,2) NOT NULL CONSTRAINT DF_table_layout_x DEFAULT 0,
        y_position DECIMAL(10,2) NOT NULL CONSTRAINT DF_table_layout_y DEFAULT 0,
        width DECIMAL(10,2) NOT NULL CONSTRAINT DF_table_layout_width DEFAULT 96,
        height DECIMAL(10,2) NOT NULL CONSTRAINT DF_table_layout_height DEFAULT 72,
        shape VARCHAR(30) NOT NULL CONSTRAINT DF_table_layout_shape DEFAULT 'RECTANGLE',
        rotation DECIMAL(10,2) NOT NULL CONSTRAINT DF_table_layout_rotation DEFAULT 0,
        is_active BIT NOT NULL CONSTRAINT DF_table_layout_active DEFAULT 1,
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_table_layout_updated DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID(N'dbo.deposit_policies', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.deposit_policies (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_deposit_policies PRIMARY KEY,
        policy_code VARCHAR(40) NOT NULL,
        name_vi NVARCHAR(150) NOT NULL,
        name_en NVARCHAR(150) NULL,
        policy_type VARCHAR(30) NOT NULL,
        percentage_rate DECIMAL(5,2) NULL,
        fixed_amount DECIMAL(18,2) NULL,
        amount_per_guest DECIMAL(18,2) NULL,
        minimum_amount DECIMAL(18,2) NULL,
        maximum_amount DECIMAL(18,2) NULL,
        area_id INT NULL,
        table_type VARCHAR(40) NULL,
        day_of_week INT NULL,
        start_time TIME NULL,
        end_time TIME NULL,
        minimum_guests INT NULL,
        minimum_order_amount DECIMAL(18,2) NULL,
        priority INT NOT NULL CONSTRAINT DF_deposit_policies_priority DEFAULT 100,
        is_active BIT NOT NULL CONSTRAINT DF_deposit_policies_active DEFAULT 1,
        effective_from DATE NULL,
        effective_to DATE NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_deposit_policies_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.deposit_policies') AND name = N'UX_deposit_policies_code')
    CREATE UNIQUE INDEX UX_deposit_policies_code ON dbo.deposit_policies(policy_code);

IF OBJECT_ID(N'dbo.reservation_preorder_items', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_preorder_items (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_reservation_preorder_items PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        product_id INT NOT NULL,
        product_name NVARCHAR(200) NOT NULL,
        product_image NVARCHAR(255) NULL,
        category_name NVARCHAR(150) NULL,
        unit_price DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservation_preorder_unit_price DEFAULT 0,
        quantity INT NOT NULL,
        note NVARCHAR(300) NULL,
        line_total DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservation_preorder_line_total DEFAULT 0,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_reservation_preorder_status DEFAULT 'REQUESTED',
        created_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_preorder_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_reservation_preorder_items_reservations FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.reservation_preorder_items') AND name = N'IX_reservation_preorder_items_reservation')
    CREATE INDEX IX_reservation_preorder_items_reservation ON dbo.reservation_preorder_items(reservation_id);

IF OBJECT_ID(N'dbo.reservation_voucher_usages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_voucher_usages (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_reservation_voucher_usages PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        voucher_id INT NULL,
        voucher_code VARCHAR(60) NOT NULL,
        discount_scope VARCHAR(40) NOT NULL,
        discount_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_reservation_voucher_discount DEFAULT 0,
        snapshot_json NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_voucher_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_reservation_voucher_reservation FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id)
    );
END;

IF OBJECT_ID(N'dbo.reservation_reviews', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_reviews (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_reservation_reviews PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        reservation_code VARCHAR(30) NOT NULL,
        overall_rating INT NOT NULL,
        food_rating INT NULL,
        service_rating INT NULL,
        ambience_rating INT NULL,
        cleanliness_rating INT NULL,
        content NVARCHAR(1000) NULL,
        image_url NVARCHAR(500) NULL,
        anonymous BIT NOT NULL CONSTRAINT DF_reservation_reviews_anon DEFAULT 0,
        admin_reply NVARCHAR(1000) NULL,
        hidden BIT NOT NULL CONSTRAINT DF_reservation_reviews_hidden DEFAULT 0,
        hidden_reason NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_reviews_created DEFAULT SYSUTCDATETIME(),
        replied_at DATETIME2 NULL,
        CONSTRAINT FK_reservation_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id),
        CONSTRAINT CK_reservation_reviews_overall CHECK (overall_rating BETWEEN 1 AND 5)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.reservation_reviews') AND name = N'UX_reservation_reviews_reservation_id')
    CREATE UNIQUE INDEX UX_reservation_reviews_reservation_id ON dbo.reservation_reviews(reservation_id);

IF OBJECT_ID(N'dbo.v_customer_reservation_history', N'V') IS NULL
   AND COL_LENGTH(N'dbo.reservations', N'customer_phone') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'customer_name') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'reservation_status') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'total_amount') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'deposit_amount') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'created_at') IS NOT NULL
BEGIN
    EXEC(N'CREATE VIEW dbo.v_customer_reservation_history AS
        SELECT customer_phone,
               MAX(customer_name) AS latest_customer_name,
               COUNT_BIG(*) AS reservation_count,
               SUM(CASE WHEN reservation_status = ''COMPLETED'' THEN CONVERT(BIGINT, 1) ELSE CONVERT(BIGINT, 0) END) AS completed_count,
               SUM(CASE WHEN reservation_status = ''CANCELLED'' THEN CONVERT(BIGINT, 1) ELSE CONVERT(BIGINT, 0) END) AS cancelled_count,
               SUM(CASE WHEN reservation_status = ''NO_SHOW'' THEN CONVERT(BIGINT, 1) ELSE CONVERT(BIGINT, 0) END) AS no_show_count,
               SUM(ISNULL(total_amount, 0)) AS total_amount,
               SUM(ISNULL(deposit_amount, 0)) AS total_deposit_amount,
               MAX(created_at) AS last_reservation_at
        FROM dbo.reservations
        GROUP BY customer_phone');
END;
