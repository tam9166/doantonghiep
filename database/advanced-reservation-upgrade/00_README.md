# Advanced Reservation Upgrade

Chạy trên SQL Server database `RestaurantDB`.

Thứ tự đề xuất:

1. `01_backup_database.sql`
2. `02_realtime_and_status.sql`
3. `03_payment_webhook.sql`
4. `04_table_layout.sql`
5. `05_smart_table_suggestion.sql`
6. `06_deposit_policies.sql`
7. `07_reservation_vouchers.sql`
8. `08_customer_history.sql`
9. `09_reviews.sql`
10. `10_notification_channels.sql`
11. `11_audit_logs.sql`
12. `12_utf8_detection.sql`
13. `13_utf8_repair.sql`
14. `14_indexes_constraints.sql`
15. `15_seed_test_data.sql`
16. `18_seed_admin_analytics_demo_data.sql`
17. `21_seed_admin_analytics_demo_ascii.sql` - optional but recommended for clean Admin Analytics demo data.
18. `16_verify_upgrade.sql`

`17_rollback_upgrade.sql` chỉ rollback các bảng/cột mới của gói này và không xóa dữ liệu cũ của hệ thống.
