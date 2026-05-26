# Sơ đồ Cơ Sở Dữ Liệu (ERD) - FPOLY RESTAURANT

Dưới đây là sơ đồ thực thể mối quan hệ (ERD) mô phỏng cấu trúc database của hệ thống quản lý nhà hàng FPOLY, dựa trên các Entity của backend Spring Boot.

```mermaid
erDiagram
    Accounts {
        varchar username PK
        varchar password
        nvarchar fullname
        varchar email
        varchar photo
        double total_spent
        int loyalty_points
        varchar tier
    }

    Roles {
        int id PK
        varchar name
    }

    Authorities {
        int id PK
        varchar username FK
        int role_id FK
    }

    Categories {
        int id PK
        nvarchar name
        nvarchar description
    }

    Products {
        int id PK
        nvarchar name
        double price
        varchar image
        nvarchar description
        date create_date
        bit available
        bit status
        int category_id FK
    }

    Ingredients {
        bigint id PK
        nvarchar name
        double quantity
        nvarchar unit
        double min_stock
    }

    Recipes {
        bigint id PK
        double amount_required
        int product_id FK
        bigint ingredient_id FK
    }

    Orders {
        int id PK
        datetime create_date
        nvarchar address
        int status
        nvarchar note
        varchar voucher_code
        varchar username FK
    }

    OrderDetails {
        int id PK
        double price
        int quantity
        int product_id FK
        int order_id FK
    }

    RestaurantTable {
        int id PK
        nvarchar name
        nvarchar floor
        int is_occupied
        bit has_view
        varchar reserved_time
        int capacity
        nvarchar view_type
    }

    Reviews {
        int id PK
        int rating
        nvarchar comment
        datetime create_date
        varchar username FK
        int product_id FK
    }

    Posts {
        int id PK
        nvarchar title
        nvarchar content
        varchar image
        varchar type
        int likes
        bit active
        datetime create_date
    }

    Applications {
        int id PK
        nvarchar fullname
        varchar phone
        varchar email
        varchar cv_url
        nvarchar status
        datetime create_date
        int post_id FK
    }

    Vouchers {
        bigint id PK
        varchar code
        int discount_percent
        bit is_used
        datetime create_date
        varchar account_username FK
    }

    %% Relationships
    Accounts ||--o{ Authorities : "has"
    Roles ||--o{ Authorities : "assigned to"
    
    Accounts ||--o{ Orders : "places"
    Accounts ||--o{ Reviews : "writes"
    Accounts ||--o{ Vouchers : "owns"

    Categories ||--o{ Products : "contains"
    
    Products ||--o{ OrderDetails : "part of"
    Products ||--o{ Reviews : "receives"
    Products ||--o{ Recipes : "made of"
    
    Ingredients ||--o{ Recipes : "used in"
    
    Orders ||--o{ OrderDetails : "has"
    
    Posts ||--o{ Applications : "receives"
```

## Các Bảng Chính
- **Hệ thống Tài khoản & Quyền**: `Accounts`, `Roles`, `Authorities`.
- **Thực đơn & Kho**: `Categories`, `Products`, `Ingredients`, `Recipes`.
- **Giao dịch & Đơn hàng**: `Orders`, `OrderDetails`.
- **Tương tác Khách hàng**: `Reviews`, `Vouchers`.
- **Marketing & Tuyển dụng**: `Posts`, `Applications`.
- **Vận hành Tiền sảnh**: `RestaurantTable`.
