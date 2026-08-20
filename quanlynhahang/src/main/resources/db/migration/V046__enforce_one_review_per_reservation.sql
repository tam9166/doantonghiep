IF OBJECT_ID(N'dbo.reservation_reviews', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE object_id = OBJECT_ID(N'dbo.reservation_reviews')
         AND name = N'UX_reservation_reviews_reservation_id'
   )
BEGIN
    CREATE UNIQUE INDEX UX_reservation_reviews_reservation_id
        ON dbo.reservation_reviews(reservation_id);
END;
