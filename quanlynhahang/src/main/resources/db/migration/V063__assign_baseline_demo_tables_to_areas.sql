IF OBJECT_ID(N'dbo.restaurant_table', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.table_areas', N'U') IS NOT NULL
BEGIN
    UPDATE rt
    SET area_id = CASE
        WHEN rt.name IN (N'Bàn 01', N'Bàn 02', N'Bàn 03', N'Bàn 04', N'Bàn 05',
                         N'Bàn 06', N'Bàn 07', N'Bàn 08', N'Bàn 09', N'Bàn 10') THEN 1
        WHEN rt.name IN (N'Bàn 11', N'Bàn 12', N'Bàn 13', N'Bàn 14', N'Bàn 15', N'Bàn 16') THEN 2
        ELSE 3
    END
    FROM dbo.restaurant_table rt
    WHERE rt.area_id IS NULL
      AND rt.name IN (N'Bàn 01', N'Bàn 02', N'Bàn 03', N'Bàn 04', N'Bàn 05',
                      N'Bàn 06', N'Bàn 07', N'Bàn 08', N'Bàn 09', N'Bàn 10',
                      N'Bàn 11', N'Bàn 12', N'Bàn 13', N'Bàn 14', N'Bàn 15',
                      N'Bàn 16', N'Bàn 17', N'Bàn 18', N'Bàn 19', N'Bàn 20')
      AND EXISTS (SELECT 1 FROM dbo.table_areas ta WHERE ta.id =
          CASE
              WHEN rt.name IN (N'Bàn 01', N'Bàn 02', N'Bàn 03', N'Bàn 04', N'Bàn 05',
                               N'Bàn 06', N'Bàn 07', N'Bàn 08', N'Bàn 09', N'Bàn 10') THEN 1
              WHEN rt.name IN (N'Bàn 11', N'Bàn 12', N'Bàn 13', N'Bàn 14', N'Bàn 15', N'Bàn 16') THEN 2
              ELSE 3
          END);
END;
