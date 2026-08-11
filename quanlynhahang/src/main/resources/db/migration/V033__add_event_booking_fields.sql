ALTER TABLE dbo.reservations ADD event_type VARCHAR(30) NULL, event_decoration_required BIT NOT NULL CONSTRAINT df_res_event_decor DEFAULT 0, event_mc_required BIT NOT NULL CONSTRAINT df_res_event_mc DEFAULT 0, event_note NVARCHAR(500) NULL;
GO
