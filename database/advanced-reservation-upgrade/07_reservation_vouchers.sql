BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.reservation_voucher_usages', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.reservation_voucher_usages (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            reservation_id bigint NOT NULL,
            voucher_id int NULL,
            voucher_code varchar(60) NOT NULL,
            discount_scope varchar(40) NOT NULL,
            discount_amount decimal(18,2) NOT NULL CONSTRAINT DF_reservation_voucher_discount DEFAULT(0),
            snapshot_json nvarchar(max) NULL,
            created_at datetime2 NOT NULL CONSTRAINT DF_reservation_voucher_created DEFAULT SYSUTCDATETIME(),
            CONSTRAINT FK_reservation_voucher_reservation FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id)
        );
    END;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
