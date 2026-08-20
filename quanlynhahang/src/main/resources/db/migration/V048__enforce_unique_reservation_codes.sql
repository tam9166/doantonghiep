IF OBJECT_ID(N'dbo.reservations', N'U') IS NOT NULL
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
             SELECT 1
             FROM sys.index_columns extra_column
             WHERE extra_column.object_id = index_definition.object_id
               AND extra_column.index_id = index_definition.index_id
               AND extra_column.key_ordinal > 1
         )
   )
BEGIN
    CREATE UNIQUE INDEX UX_reservations_reservation_code
        ON dbo.reservations(reservation_code);
END;
