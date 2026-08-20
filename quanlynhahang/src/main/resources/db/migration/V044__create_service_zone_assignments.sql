SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.service_zone_assignments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.service_zone_assignments (
        id bigint IDENTITY(1,1) NOT NULL,
        username varchar(50) NOT NULL,
        floor nvarchar(100) NOT NULL,
        shift nvarchar(50) NOT NULL,
        work_date date NOT NULL,
        CONSTRAINT PK_service_zone_assignments PRIMARY KEY (id),
        CONSTRAINT FK_service_zone_assignments_accounts
            FOREIGN KEY (username) REFERENCES dbo.accounts(username)
    );

    CREATE UNIQUE INDEX UX_service_zone_staff_floor_shift_date
        ON dbo.service_zone_assignments(username, floor, shift, work_date);
    CREATE INDEX IX_service_zone_work_date
        ON dbo.service_zone_assignments(work_date);
    CREATE INDEX IX_service_zone_username_work_date
        ON dbo.service_zone_assignments(username, work_date);
END;
