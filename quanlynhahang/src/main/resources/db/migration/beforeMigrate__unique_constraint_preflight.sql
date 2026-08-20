-- Fail with an actionable message before V046/V048 attempt to add unique indexes.
-- This callback is intentionally read-only and is skipped for a blank database.
IF OBJECT_ID(N'dbo.reservation_reviews', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM sys.indexes
       WHERE object_id = OBJECT_ID(N'dbo.reservation_reviews')
         AND name = N'UX_reservation_reviews_reservation_id'
   )
BEGIN
    EXEC(N'
        IF EXISTS (
            SELECT reservation_id
            FROM dbo.reservation_reviews
            GROUP BY reservation_id
            HAVING COUNT_BIG(*) > 1
        )
            THROW 51046,
                ''BUG-056 preflight: reservation_reviews contains duplicate reservation_id values. Resolve duplicates before applying V046.'',
                1;
    ');
END;

IF OBJECT_ID(N'dbo.reservations', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'reservation_code') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes index_definition
       JOIN sys.index_columns index_column
         ON index_column.object_id = index_definition.object_id
        AND index_column.index_id = index_definition.index_id
       JOIN sys.columns column_definition
         ON column_definition.object_id = index_column.object_id
        AND column_definition.column_id = index_column.column_id
       WHERE index_definition.object_id = OBJECT_ID(N'dbo.reservations')
         AND index_definition.is_unique = 1
         AND column_definition.name = N'reservation_code'
         AND index_column.key_ordinal = 1
         AND NOT EXISTS (
             SELECT 1 FROM sys.index_columns extra_column
             WHERE extra_column.object_id = index_definition.object_id
               AND extra_column.index_id = index_definition.index_id
               AND extra_column.key_ordinal > 1
         )
   )
BEGIN
    EXEC(N'
        IF EXISTS (
            SELECT reservation_code
            FROM dbo.reservations
            GROUP BY reservation_code
            HAVING COUNT_BIG(*) > 1
        )
            THROW 51048,
                ''BUG-056 preflight: reservations contains duplicate reservation_code values. Resolve duplicates before applying V048.'',
                1;
    ');
END;
