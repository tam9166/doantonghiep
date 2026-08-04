BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.table_layouts', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.table_layouts (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            table_id int NOT NULL,
            area_id int NULL,
            floor_name nvarchar(80) NULL,
            x_position decimal(10,2) NOT NULL CONSTRAINT DF_table_layout_x DEFAULT(0),
            y_position decimal(10,2) NOT NULL CONSTRAINT DF_table_layout_y DEFAULT(0),
            width decimal(10,2) NOT NULL CONSTRAINT DF_table_layout_width DEFAULT(96),
            height decimal(10,2) NOT NULL CONSTRAINT DF_table_layout_height DEFAULT(72),
            shape varchar(30) NOT NULL CONSTRAINT DF_table_layout_shape DEFAULT('RECTANGLE'),
            rotation decimal(10,2) NOT NULL CONSTRAINT DF_table_layout_rotation DEFAULT(0),
            is_active bit NOT NULL CONSTRAINT DF_table_layout_active DEFAULT(1),
            updated_at datetime2 NOT NULL CONSTRAINT DF_table_layout_updated DEFAULT SYSUTCDATETIME()
        );
    END;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
