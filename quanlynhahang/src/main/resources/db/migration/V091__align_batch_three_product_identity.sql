-- V073 seeds alcoholic products by semantic key, so SQL Server identity values
-- can differ between an upgraded database and a database built from scratch.
-- Resolve exactly one Saigon Special row, then keep the UPDATE itself ID-scoped.
DECLARE @saigon_special_id BIGINT;

IF (SELECT COUNT(*) FROM products WHERE name = N'Saigon Special') = 1
BEGIN
    SELECT @saigon_special_id = id
    FROM products
    WHERE name = N'Saigon Special';

    UPDATE products
    SET image = '/images/products/saigon-special-cc-by-sa.jpg'
    WHERE id = @saigon_special_id
      AND status = 1;
END;
