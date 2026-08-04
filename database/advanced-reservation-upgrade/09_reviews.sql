BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.reservation_reviews', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.reservation_reviews (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            reservation_id bigint NOT NULL,
            reservation_code varchar(30) NOT NULL,
            overall_rating int NOT NULL,
            food_rating int NULL,
            service_rating int NULL,
            ambience_rating int NULL,
            cleanliness_rating int NULL,
            content nvarchar(1000) NULL,
            image_url nvarchar(500) NULL,
            anonymous bit NOT NULL CONSTRAINT DF_reservation_reviews_anon DEFAULT(0),
            admin_reply nvarchar(1000) NULL,
            hidden bit NOT NULL CONSTRAINT DF_reservation_reviews_hidden DEFAULT(0),
            hidden_reason nvarchar(500) NULL,
            created_at datetime2 NOT NULL CONSTRAINT DF_reservation_reviews_created DEFAULT SYSUTCDATETIME(),
            replied_at datetime2 NULL,
            CONSTRAINT FK_reservation_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id),
            CONSTRAINT CK_reservation_reviews_overall CHECK (overall_rating BETWEEN 1 AND 5)
        );
    END;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
