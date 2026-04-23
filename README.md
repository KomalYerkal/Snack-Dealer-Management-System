# 🍿 Snack Dealer Management System

A **Java Swing + MySQL** desktop application for managing snack dealership operations. The system provides two dedicated dashboards — one for **Dealers** and one for **Buyers** — enabling end-to-end order management, billing, inventory control, and reporting through a clean, role-based GUI.

---

## 📌 Project Overview

| Field | Details |
|---|---|
| **Language** | Java (Core Java + Swing GUI) |
| **Database** | MySQL (via JDBC) |
| **Connector** | `mysql-connector-j.jar` |
| **Architecture** | Role-based dual dashboard (Dealer & Buyer) |
| **Build** | Shell script (`build.sh`) |

---

## 🗂️ Project Structure

```
dbms/
├── src/
│   └── snack/
│       ├── Main.java                  # Entry point
│       ├── auth/
│       │   ├── LoginFrame.java        # Role-based login screen
│       │   └── SignupFrame.java       # Registration for Dealers & Buyers
│       ├── dealer/
│       │   ├── DealerDashboard.java   # Dealer main dashboard
│       │   └── panels/
│       │       ├── SnacksPanel.java        # Manage snack inventory
│       │       ├── ShopsPanel.java         # Manage shops
│       │       ├── ShopkeepersPanel.java   # Manage shopkeepers
│       │       ├── IndustriesPanel.java    # Manage industry/supplier info
│       │       ├── CustomersPanel.java     # View registered buyers/customers
│       │       ├── DealerOrdersPanel.java  # View & manage incoming orders
│       │       ├── DealerBillsPanel.java   # View generated bills
│       │       └── ReportsPanel.java       # Analytics & reports
│       ├── buyer/
│       │   ├── BuyerDashboard.java    # Buyer main dashboard
│       │   └── panels/
│       │       ├── BrowseSnacksPanel.java  # Browse available snacks by dealer
│       │       ├── ShopOrderPanel.java     # Place / update / delete orders
│       │       ├── MyOrdersPanel.java      # View own orders
│       │       └── MyBillsPanel.java       # View own bills/receipts
│       ├── db/
│       │   └── DatabaseConnection.java    # JDBC MySQL connection handler
│       └── util/
│           └── UIConstants.java           # Shared UI styling constants
├── lib/
│   └── mysql-connector-j.jar          # MySQL JDBC driver
├── schema.sql                          # Database schema (DDL)
├── data.sql                            # Sample seed data (DML)
├── build.sh                            # Compile & run shell script
└── sources.txt                         # Java source file list for javac
```

---

## 🖥️ Dashboards

### 1. 🏪 Dealer Dashboard
The **Dealer** is the admin/supplier side of the system. After logging in as a Dealer, the user gets access to:

| Tab | Description |
|---|---|
| **Snacks** | Add, update, and delete snack products with price, quantity, and shopkeeper linkage |
| **Shops** | Manage shop records associated with the dealer |
| **Shopkeepers** | Manage shopkeeper profiles linked to shops |
| **Industries** | Track industry/supplier associations |
| **Customers** | View all registered buyers in the system |
| **Orders** | View, accept, or manage orders placed by buyers in real time |
| **Bills** | View all generated bills associated with fulfilled orders |
| **Reports** | Analytics panel showing revenue, order trends, and inventory summaries |

---

### 2. 🛒 Buyer Dashboard
The **Buyer** is the customer-facing side. After logging in as a Buyer, the user can:

| Tab | Description |
|---|---|
| **Browse Snacks** | Browse available snacks filtered by selected dealer/shop |
| **Shop & Order** | Place new orders, update quantities, or cancel (delete) existing orders |
| **My Orders** | View full order history with status tracking |
| **My Bills** | View and review past generated bills/receipts |

---

## 🔐 Authentication

- **Login**: Users select their role (Dealer or Buyer) and log in with registered credentials.
- **Signup**: New users register as either a **Dealer** or **Buyer** with name, contact, and password.
- Credentials are stored securely in the MySQL database.

---

## 🗄️ Database Schema

The MySQL database (`snack_db`) includes the following tables:

| Table | Description |
|---|---|
| `dealers` | Stores dealer account information |
| `buyers` | Stores buyer account information |
| `industries` | Industry/manufacturer records |
| `shops` | Shop records linked to dealers |
| `shopkeepers` | Shopkeeper details linked to shops |
| `snacks` | Snack products linked to shopkeepers |
| `orders` | Orders placed by buyers |
| `bills` | Bills auto-generated upon order fulfillment |

> Full schema with constraints and foreign keys is in [`schema.sql`](schema.sql).  
> Sample seed data is in [`data.sql`](data.sql).

---

## ⚙️ Setup & Running

### Prerequisites
- Java JDK 8 or above
- MySQL Server running locally
- `mysql-connector-j.jar` (already included in `lib/`)

### 1. Database Setup
```sql
-- In MySQL terminal:
CREATE DATABASE snack_db;
USE snack_db;
SOURCE schema.sql;
SOURCE data.sql;
```

### 2. Configure DB Connection
Edit `src/snack/db/DatabaseConnection.java` and update:
```java
private static final String URL = "jdbc:mysql://localhost:3306/snack_db";
private static final String USER = "root";        // your MySQL username
private static final String PASSWORD = "your_password";  // your MySQL password
```

### 3. Build & Run
```bash
chmod +x build.sh
./build.sh
```
Or compile manually:
```bash
javac -cp lib/mysql-connector-j.jar -d bin @sources.txt
java -cp bin:lib/mysql-connector-j.jar snack.Main
```

---

## 🔄 Key Features

- ✅ **Role-based login** — separate Dealer and Buyer flows
- ✅ **Full CRUD on snacks, shops, shopkeepers, industries** (Dealer side)
- ✅ **Order placement with dealer selection** (Buyer side)
- ✅ **Real-time order sync** — buyer orders appear instantly on dealer dashboard
- ✅ **Auto-generated bills** on order completion
- ✅ **Bill history** for both Dealers and Buyers
- ✅ **Reports & analytics** panel for Dealers
- ✅ **JDBC-based MySQL integration** with relational integrity (foreign keys)

---

## 👥 Team

Built as a **DBMS Mini Project** demonstrating SQL connectivity, relational database design, and Java Swing GUI development.

---

## 📄 License

This project is for academic/educational purposes.
