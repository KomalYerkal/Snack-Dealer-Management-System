-- ============================================================
--  Snack Dealer Management System — Sample Data
-- ============================================================
USE snack_db;

-- 1. Industries
INSERT INTO Industries (L_ID, I_Name, I_Licence, I_contact_no, I_address) VALUES
(1, 'Haldirams Foods Pvt Ltd',  'LIC-IND-001', '9876543200', 'Sector 14, Delhi'),
(2, 'PepsiCo India Holdings',   'LIC-IND-002', '9876543201', 'Lower Parel, Mumbai'),
(3, 'ITC Foods Division',       'LIC-IND-003', '9876543202', 'Park Street, Kolkata'),
(4, 'Bikanervala Foods Pvt Ltd','LIC-IND-004', '9876543203', 'Bikaner, Rajasthan'),
(5, 'Parle Products Pvt Ltd',   'LIC-IND-005', '9876543204', 'Vile Parle, Mumbai');

-- 2. Snack Dealers
INSERT INTO Snack_Dealer (Dealer_ID, Dealer_Name, Contact_no, email, D_Address) VALUES
(1, 'Rajesh Kumar Traders',  '9812345670', 'rajesh@snackdealer.com',  'Deccan, Pune, Maharashtra'),
(2, 'Sunita Wholesale Hub',  '9812345671', 'sunita@snackdealer.com',  'Dadar, Mumbai, Maharashtra');

-- 3. Deals_With (dealer ↔ industry)
INSERT INTO Deals_With VALUES
(1,1,'2024-01-15'),(1,2,'2024-02-20'),(1,3,'2024-03-10'),(1,5,'2024-04-05'),
(2,2,'2024-01-01'),(2,4,'2024-04-01'),(2,5,'2024-05-15');

-- 4. Shopkeepers
INSERT INTO Shopkeepers (S_ID, S_Name, S_Contact_no, Dealer_ID) VALUES
(1, 'Anil Sharma',  '9823456780', 1),
(2, 'Priya Patel',  '9823456781', 1),
(3, 'Mohan Singh',  '9823456782', 2),
(4, 'Geeta Joshi',  '9823456783', 1),
(5, 'Suresh Nair',  '9823456784', 2);

-- 5. Shops
INSERT INTO Shops (Shop_ID, Shop_name, Shop_Address, Shop_C_no, S_ID) VALUES
(1, 'Anil General Store',       'MG Road, Pune',          '9823456780', 1),
(2, 'Priya Snack Corner',       'FC Road, Pune',           '9823456781', 2),
(3, 'Mohan Kirana',             'Andheri West, Mumbai',    '9823456782', 3),
(4, 'Geeta Sweets & Snacks',    'Kothrud, Pune',           '9823456783', 4),
(5, 'Suresh Quick Bites',       'Bandra East, Mumbai',     '9823456784', 5);

-- 6. Snacks
INSERT INTO Snacks (Snack_ID, Snack_Name, Expiry, MFd, Price, L_ID) VALUES
(1, 'Aloo Bhujia 200g',   '2025-12-01', '2024-06-01',  50.00, 1),
(2, 'Lays Classic 26g',   '2025-08-15', '2024-08-15',  20.00, 2),
(3, 'Kurkure Masala 90g', '2025-09-01', '2024-09-01',  20.00, 2),
(4, 'Sunfeast Biscuit',   '2025-11-30', '2024-11-30',  30.00, 3),
(5, 'Haldirams Mixture',  '2025-10-01', '2024-10-01',  80.00, 1),
(6, 'Monaco Biscuit',     '2025-12-31', '2024-12-31',  25.00, 5),
(7, 'Parle-G 250g',       '2026-01-01', '2025-01-01',  15.00, 5),
(8, 'Choco Pie 6pc',      '2025-07-15', '2024-07-15',  60.00, 3),
(9, 'Bikaneri Bhujia',    '2025-11-01', '2024-11-01',  90.00, 4),
(10,'Nimkeen Mix',        '2025-10-15', '2024-10-15',  45.00, 4);

-- 7. Shop_Snacks (inventory)
INSERT INTO Shop_Snacks VALUES
(1,1,100),(1,2,200),(1,5,50),(1,6,80),
(2,3,150),(2,4,100),(2,7,250),(2,8,60),
(3,7,300),(3,8,80),(3,9,40),
(4,1,75),(4,3,120),(4,5,40),(4,10,90),
(5,2,180),(5,6,70),(5,9,55),(5,10,110);

-- 8. Customers
INSERT INTO Customers (cust_ID, F_name, L_name, C_Contact_no, C_Address, Shop_ID) VALUES
(1, 'Amit',   'Verma',    '9934567890', 'Baner, Pune',          1),
(2, 'Sneha',  'Kulkarni', '9934567891', 'Wakad, Pune',          2),
(3, 'Rohan',  'Mehta',    '9934567892', 'Viman Nagar, Pune',    1),
(4, 'Pooja',  'Rane',     '9934567893', 'Hinjewadi, Pune',      3),
(5, 'Vikram', 'Desai',    '9934567894', 'Kothrud, Pune',        4),
(6, 'Nisha',  'Patil',    '9934567895', 'Bandra, Mumbai',       5),
(7, 'Arjun',  'Shah',     '9934567896', 'Andheri, Mumbai',      3);

-- 9. Orders  (Total_price = Price × Qty)
INSERT INTO Orders (Order_ID,Order_date,Order_Quantity,Total_price,S_ID,cust_ID,Snack_ID,status) VALUES
(1,'2024-11-01',5, 250.00,1,1,1,'Delivered'),
(2,'2024-11-05',10,200.00,2,2,3,'Delivered'),
(3,'2024-11-10',3, 240.00,1,3,5,'Confirmed'),
(4,'2024-11-15',2,  30.00,3,4,7,'Pending'),
(5,'2024-11-20',4, 240.00,4,5,8,'Delivered'),
(6,'2024-11-22',6, 300.00,5,6,1,'Delivered'),
(7,'2024-11-25',8, 160.00,2,7,2,'Confirmed'),
(8,'2024-11-28',2, 180.00,1,1,9,'Pending');

-- 10. Bills  (Tax = 18% GST)
INSERT INTO Bill (Bill_ID,Order_ID,Bill_date,Tax,Total_Amount,cust_ID) VALUES
(1,1,'2024-11-01', 45.00, 295.00,1),
(2,2,'2024-11-05', 36.00, 236.00,2),
(3,3,'2024-11-10', 43.20, 283.20,3),
(4,4,'2024-11-15',  5.40,  35.40,4),
(5,5,'2024-11-20', 43.20, 283.20,5),
(6,6,'2024-11-22', 54.00, 354.00,6),
(7,7,'2024-11-25', 28.80, 188.80,7),
(8,8,'2024-11-28', 32.40, 212.40,1);

-- 11. Users
INSERT INTO Users (username,password,role,ref_id) VALUES
('dealer1',     'dealer123', 'dealer', 1),
('dealer2',     'dealer456', 'dealer', 2),
('amit',        'buyer123',  'buyer',  1),
('sneha',       'buyer456',  'buyer',  2),
('rohan',       'buyer789',  'buyer',  3),
('pooja',       'buyer321',  'buyer',  4),
('vikram',      'buyer654',  'buyer',  5),
('nisha',       'buyer111',  'buyer',  6),
('arjun',       'buyer222',  'buyer',  7);