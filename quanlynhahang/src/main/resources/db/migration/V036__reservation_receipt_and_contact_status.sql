IF COL_LENGTH('reservations', 'receipt_email_status') IS NULL
    ALTER TABLE reservations ADD receipt_email_status varchar(30) NULL;
IF COL_LENGTH('reservations', 'receipt_email_sent_at') IS NULL
    ALTER TABLE reservations ADD receipt_email_sent_at datetime2 NULL;
IF COL_LENGTH('reservations', 'receipt_email_error') IS NULL
    ALTER TABLE reservations ADD receipt_email_error nvarchar(500) NULL;
IF COL_LENGTH('reservations', 'contact_status') IS NULL
    ALTER TABLE reservations ADD contact_status varchar(40) NOT NULL
        CONSTRAINT DF_reservations_contact_status DEFAULT 'NOT_CALLED';
IF COL_LENGTH('reservations', 'contact_call_note') IS NULL
    ALTER TABLE reservations ADD contact_call_note nvarchar(1000) NULL;
IF COL_LENGTH('reservations', 'contact_called_at') IS NULL
    ALTER TABLE reservations ADD contact_called_at datetime2 NULL;
IF COL_LENGTH('reservations', 'contact_called_by') IS NULL
    ALTER TABLE reservations ADD contact_called_by varchar(80) NULL;

EXEC(N'UPDATE reservations SET receipt_email_status = ''NOT_SENT'' WHERE receipt_email_status IS NULL');
