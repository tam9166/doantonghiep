IF OBJECT_ID('dbo.reservation_waitlist', 'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT linked_reservation_code
        FROM dbo.reservation_waitlist
        WHERE linked_reservation_code IS NOT NULL
        GROUP BY linked_reservation_code
        HAVING COUNT(*) > 1
    )
        THROW 51000, 'Cannot enforce waitlist conversion integrity: duplicate linked_reservation_code values exist.', 1;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID('dbo.reservation_waitlist')
          AND name = 'UX_waitlist_linked_reservation_code'
    )
        CREATE UNIQUE INDEX UX_waitlist_linked_reservation_code
            ON dbo.reservation_waitlist(linked_reservation_code)
            WHERE linked_reservation_code IS NOT NULL;
END;
