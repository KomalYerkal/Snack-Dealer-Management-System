-- ============================================================
--  Snack Dealer Management System — Database Schema
-- ============================================================
CREATE DATABASE IF NOT EXISTS snack_db;
USE snack_db;

-- ─────────────────────────────────────────────────────────────
-- 1. USERS  (authentication — links to dealer or customer)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Users (
    user_id    INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('dealer','buyer') NOT NULL,
    ref_id     INT NOT NULL,                       -- Dealer_ID or cust_ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- 2. INDUSTRIES  (manufacturers / producers)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Industries (
    L_ID         INT AUTO_INCREMENT PRIMARY KEY,
    I_Name       VARCHAR(100) NOT NULL,
    I_Licence    VARCHAR(50),
    I_contact_no VARCHAR(15),
    I_address    VARCHAR(255)
);

-- ─────────────────────────────────────────────────────────────
-- 3. SNACK_DEALER
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Snack_Dealer (
    Dealer_ID   INT AUTO_INCREMENT PRIMARY KEY,
    Dealer_Name VARCHAR(100) NOT NULL,
    Contact_no  VARCHAR(15),
    email       VARCHAR(100),
    D_Address   VARCHAR(255)
);

-- ─────────────────────────────────────────────────────────────
-- 4. DEALS_WITH  (Snack_Dealer ↔ Industries)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Deals_With (
    Dealer_ID INT NOT NULL,
    L_ID      INT NOT NULL,
    deal_date DATE,
    PRIMARY KEY (Dealer_ID, L_ID),
    FOREIGN KEY (Dealer_ID) REFERENCES Snack_Dealer(Dealer_ID) ON DELETE CASCADE,
    FOREIGN KEY (L_ID)      REFERENCES Industries(L_ID)        ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────
-- 5. SHOPKEEPERS  (works under a dealer)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Shopkeepers (
    S_ID        INT AUTO_INCREMENT PRIMARY KEY,
    S_Name      VARCHAR(100) NOT NULL,
    S_Contact_no VARCHAR(15),
    Dealer_ID   INT,
    FOREIGN KEY (Dealer_ID) REFERENCES Snack_Dealer(Dealer_ID) ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────
-- 6. SHOPS  (owned by a shopkeeper)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Shops (
    Shop_ID      INT AUTO_INCREMENT PRIMARY KEY,
    Shop_name    VARCHAR(100) NOT NULL,
    Shop_Address VARCHAR(255),
    Shop_C_no    VARCHAR(15),
    S_ID         INT,
    FOREIGN KEY (S_ID) REFERENCES Shopkeepers(S_ID) ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────
-- 7. SNACKS  (manufactured by an industry)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Snacks (
    Snack_ID   INT AUTO_INCREMENT PRIMARY KEY,
    Snack_Name VARCHAR(100) NOT NULL,
    Expiry     DATE,
    MFd        DATE,
    Price      DECIMAL(10,2) NOT NULL,
    L_ID       INT,
    FOREIGN KEY (L_ID) REFERENCES Industries(L_ID) ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────
-- 8. SHOP_SNACKS  (shop inventory — contains relationship)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Shop_Snacks (
    Shop_ID   INT NOT NULL,
    Snack_ID  INT NOT NULL,
    stock_qty INT DEFAULT 0,
    PRIMARY KEY (Shop_ID, Snack_ID),
    FOREIGN KEY (Shop_ID)  REFERENCES Shops(Shop_ID)   ON DELETE CASCADE,
    FOREIGN KEY (Snack_ID) REFERENCES Snacks(Snack_ID) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────
-- 9. CUSTOMERS  (linked to a shop)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Customers (
    cust_ID     INT AUTO_INCREMENT PRIMARY KEY,
    F_name      VARCHAR(50)  NOT NULL,
    L_name      VARCHAR(50),
    C_Contact_no VARCHAR(15),
    C_Address   VARCHAR(255),
    Shop_ID     INT,
    FOREIGN KEY (Shop_ID) REFERENCES Shops(Shop_ID) ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────
-- 10. ORDERS  (placed by customer via shopkeeper for a snack)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Orders (
    Order_ID       INT AUTO_INCREMENT PRIMARY KEY,
    Order_date     DATE NOT NULL,
    Order_Quantity INT  NOT NULL,
    Total_price    DECIMAL(10,2),
    S_ID           INT,
    cust_ID        INT NOT NULL,
    Snack_ID       INT,
    status         ENUM('Pending','Confirmed','Delivered','Cancelled') DEFAULT 'Pending',
    FOREIGN KEY (S_ID)     REFERENCES Shopkeepers(S_ID)  ON DELETE SET NULL,
    FOREIGN KEY (cust_ID)  REFERENCES Customers(cust_ID) ON DELETE CASCADE,
    FOREIGN KEY (Snack_ID) REFERENCES Snacks(Snack_ID)   ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────
-- 11. BILL  (auto-generated when an order is created)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Bill (
    Bill_ID      INT AUTO_INCREMENT PRIMARY KEY,
    Order_ID     INT,
    Bill_date    DATE NOT NULL,
    Tax          DECIMAL(10,2) DEFAULT 0.00,
    Total_Amount DECIMAL(10,2),
    cust_ID      INT NOT NULL,
    FOREIGN KEY (Order_ID) REFERENCES Orders(Order_ID)   ON DELETE CASCADE,
    FOREIGN KEY (cust_ID)  REFERENCES Customers(cust_ID) ON DELETE CASCADE
);