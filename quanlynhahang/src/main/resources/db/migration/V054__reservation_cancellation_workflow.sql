CREATE TABLE reservation_cancellation_requests (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version bigint NOT NULL CONSTRAINT DF_reservation_cancel_version DEFAULT 0,
    request_code varchar(30) NOT NULL,
    reservation_id bigint NOT NULL,
    reason nvarchar(1000) NULL,
    status varchar(30) NOT NULL,
    matched_field_count int NOT NULL,
    requested_at datetime2 NOT NULL,
    hours_before_reservation decimal(12,2) NOT NULL,
    refund_rate decimal(5,4) NOT NULL,
    paid_deposit_amount decimal(18,0) NOT NULL,
    expected_refund_amount decimal(18,0) NOT NULL,
    refund_transaction_id bigint NULL,
    processed_by varchar(80) NULL,
    processed_at datetime2 NULL,
    processing_note nvarchar(1000) NULL,
    CONSTRAINT UQ_reservation_cancel_request_code UNIQUE (request_code),
    CONSTRAINT FK_reservation_cancel_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT FK_reservation_cancel_refund FOREIGN KEY (refund_transaction_id) REFERENCES refund_transactions(id),
    CONSTRAINT CK_reservation_cancel_status CHECK (status IN ('PENDING','APPROVED','REJECTED','REFUND_PENDING','REFUNDED','REFUND_FAILED')),
    CONSTRAINT CK_reservation_cancel_match_count CHECK (matched_field_count BETWEEN 2 AND 4),
    CONSTRAINT CK_reservation_cancel_amounts CHECK (paid_deposit_amount >= 0 AND expected_refund_amount >= 0),
    CONSTRAINT CK_reservation_cancel_rate CHECK (refund_rate >= 0 AND refund_rate <= 1)
);

CREATE UNIQUE INDEX UX_reservation_cancel_active
    ON reservation_cancellation_requests(reservation_id)
    WHERE status = 'PENDING';

CREATE INDEX IX_reservation_cancel_status_requested
    ON reservation_cancellation_requests(status, requested_at DESC);

CREATE TABLE reservation_contact_logs (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    reservation_id bigint NOT NULL,
    staff_username varchar(80) NOT NULL,
    contact_type varchar(30) NOT NULL,
    result varchar(40) NOT NULL,
    contacted_at datetime2 NOT NULL,
    note nvarchar(1000) NULL,
    CONSTRAINT FK_reservation_contact_log_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);

CREATE INDEX IX_reservation_contact_log_reservation
    ON reservation_contact_logs(reservation_id, contacted_at DESC);
