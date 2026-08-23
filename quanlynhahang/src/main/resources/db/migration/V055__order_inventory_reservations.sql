CREATE TABLE inventory_reservations (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version bigint NOT NULL CONSTRAINT DF_inventory_reservation_version DEFAULT 0,
    order_id bigint NOT NULL,
    ingredient_id bigint NOT NULL,
    quantity decimal(19,4) NOT NULL,
    status varchar(20) NOT NULL,
    expires_at datetime2 NOT NULL,
    created_at datetime2 NOT NULL CONSTRAINT DF_inventory_reservation_created DEFAULT GETDATE(),
    finalized_at datetime2 NULL,
    CONSTRAINT UQ_inventory_reservation_order_ingredient UNIQUE (order_id, ingredient_id),
    CONSTRAINT FK_inventory_reservation_order FOREIGN KEY (order_id) REFERENCES Orders(id),
    CONSTRAINT FK_inventory_reservation_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
    CONSTRAINT CK_inventory_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT CK_inventory_reservation_status CHECK (status IN ('RESERVED','CONSUMED','RELEASED','EXPIRED'))
);

CREATE INDEX IX_inventory_reservation_active
    ON inventory_reservations(ingredient_id, status, expires_at)
    INCLUDE (quantity);

CREATE INDEX IX_inventory_reservation_order
    ON inventory_reservations(order_id, status);
