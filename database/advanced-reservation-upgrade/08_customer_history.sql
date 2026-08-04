IF OBJECT_ID('dbo.v_customer_reservation_history', 'V') IS NOT NULL
    DROP VIEW dbo.v_customer_reservation_history;
GO
CREATE VIEW dbo.v_customer_reservation_history AS
SELECT
    customer_phone,
    MAX(customer_name) AS latest_customer_name,
    COUNT(*) AS reservation_count,
    SUM(CASE WHEN reservation_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
    SUM(CASE WHEN reservation_status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_count,
    SUM(CASE WHEN reservation_status = 'NO_SHOW' THEN 1 ELSE 0 END) AS no_show_count,
    SUM(ISNULL(total_amount, 0)) AS total_amount,
    SUM(ISNULL(deposit_amount, 0)) AS total_deposit_amount,
    MAX(created_at) AS last_reservation_at
FROM dbo.reservations
GROUP BY customer_phone;
GO
