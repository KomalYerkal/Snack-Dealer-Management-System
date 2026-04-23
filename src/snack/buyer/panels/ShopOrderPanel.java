package snack.buyer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Buyer's "Shop & Order" panel.
 *
 * Flow:
 *  1. Pick a Dealer from the dropdown.
 *  2. All snacks available in that dealer's shops load in the table.
 *  3. Buyer selects a snack and clicks Place Order → dialog to enter qty.
 *  4. Order + Bill are inserted → visible on the Dealer's dashboard immediately.
 *  5. Edit / Cancel buttons let the buyer modify or cancel a Pending order.
 */
public class ShopOrderPanel extends JPanel {

    private final int custId;

    // Dealer selector
    private JComboBox<String> cbDealer;
    private final Map<String, Integer> dealerMap = new LinkedHashMap<>();

    // Snacks table
    private JTable snackTable;
    private DefaultTableModel snackModel;
    private static final String[] SNACK_COLS =
        {"Snack ID", "Snack Name", "Price (₹)", "Manufacturer", "Shop", "Stock"};

    // My orders table (for this dealer)
    private JTable orderTable;
    private DefaultTableModel orderModel;
    private static final String[] ORDER_COLS =
        {"Order ID", "Date", "Snack", "Qty", "Total (₹)", "Shop", "Status"};

    public ShopOrderPanel(int custId) {
        this.custId = custId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadDealers();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        add(UIConstants.makeTitlePanel(
            "🛍  Shop & Order",
            "Select a dealer, browse their snacks and place your orders"),
            BorderLayout.NORTH);

        // Dealer picker strip
        JPanel dealerStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        dealerStrip.setBackground(UIConstants.WHITE);
        dealerStrip.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIConstants.LIGHT_ACCENT),
            new EmptyBorder(6, 14, 6, 14)));

        cbDealer = new JComboBox<>();
        cbDealer.setFont(UIConstants.LABEL_FONT);
        cbDealer.setPreferredSize(new Dimension(260, 30));

        JButton btnLoad = UIConstants.makeButton("Load Snacks →", UIConstants.ACCENT);

        dealerStrip.add(UIConstants.makeLabel("🏢  Select Dealer:"));
        dealerStrip.add(cbDealer);
        dealerStrip.add(btnLoad);

        // Split: top = snacks, bottom = my orders for this dealer
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.55);
        split.setDividerSize(6);

        // -- Top: Snacks --
        snackModel = new DefaultTableModel(SNACK_COLS, 0);
        snackTable = UIConstants.makeTable(snackModel);
        JScrollPane snackScroll = new JScrollPane(snackTable);
        snackScroll.getViewport().setBackground(UIConstants.WHITE);

        JButton btnOrder  = UIConstants.makeButton("🛒 Place Order", UIConstants.SUCCESS);
        JPanel snackBar = UIConstants.makeButtonBar(btnOrder);
        snackBar.setBorder(new MatteBorder(1, 0, 0, 0, UIConstants.LIGHT_ACCENT));

        JPanel snackPanel = new JPanel(new BorderLayout());
        snackPanel.add(sectionHeader("Available Snacks"), BorderLayout.NORTH);
        snackPanel.add(snackScroll,  BorderLayout.CENTER);
        snackPanel.add(snackBar,     BorderLayout.SOUTH);

        // -- Bottom: My Orders --
        orderModel = new DefaultTableModel(ORDER_COLS, 0);
        orderTable = UIConstants.makeTable(orderModel);
        orderTable.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.getViewport().setBackground(UIConstants.WHITE);

        JButton btnEdit   = UIConstants.makeButton("✎ Edit Qty",    UIConstants.WARNING);
        JButton btnCancel = UIConstants.makeButton("✕ Cancel Order", UIConstants.DANGER);
        JButton btnRefresh= UIConstants.btnRefresh();
        JPanel orderBar = UIConstants.makeButtonBar(btnRefresh, btnEdit, btnCancel);
        orderBar.setBorder(new MatteBorder(1, 0, 0, 0, UIConstants.LIGHT_ACCENT));

        JPanel orderPanel = new JPanel(new BorderLayout());
        orderPanel.add(sectionHeader("My Orders (from this dealer)"), BorderLayout.NORTH);
        orderPanel.add(orderScroll, BorderLayout.CENTER);
        orderPanel.add(orderBar,    BorderLayout.SOUTH);

        split.setTopComponent(snackPanel);
        split.setBottomComponent(orderPanel);

        JPanel center = new JPanel(new BorderLayout());
        center.add(dealerStrip, BorderLayout.NORTH);
        center.add(split,       BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // ── Listeners ─────────────────────────────────────────────────────────
        btnLoad.addActionListener(e -> loadSnacks());
        btnOrder.addActionListener(e -> placeOrder());
        btnEdit.addActionListener(e -> editOrder());
        btnCancel.addActionListener(e -> cancelOrder());
        btnRefresh.addActionListener(e -> refreshOrders());
    }

    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        p.setBackground(new Color(232, 234, 246));
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.HEADER_FONT);
        l.setForeground(UIConstants.PRIMARY);
        p.add(l);
        return p;
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    private void loadDealers() {
        dealerMap.clear();
        cbDealer.removeAllItems();
        try {
            ResultSet rs = DatabaseConnection.getConnection()
                .createStatement()
                .executeQuery("SELECT Dealer_ID, Dealer_Name FROM Snack_Dealer ORDER BY Dealer_Name");
            while (rs.next()) {
                String name = rs.getString("Dealer_Name");
                dealerMap.put(name, rs.getInt("Dealer_ID"));
                cbDealer.addItem(name);
            }
        } catch (SQLException ex) {
            err("Cannot load dealers: " + ex.getMessage());
        }
    }

    private int selectedDealerId() {
        String chosen = (String) cbDealer.getSelectedItem();
        return chosen == null ? -1 : dealerMap.getOrDefault(chosen, -1);
    }

    private void loadSnacks() {
        int dealerId = selectedDealerId();
        if (dealerId < 0) { warn("Please select a dealer first."); return; }
        snackModel.setRowCount(0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT sn.Snack_ID, sn.Snack_Name, sn.Price, " +
                "  i.I_Name, sh.Shop_name, COALESCE(ss.stock_qty, 0) " +
                "FROM Snacks sn " +
                "LEFT JOIN Industries  i  ON sn.L_ID    = i.L_ID " +
                "LEFT JOIN Shop_Snacks ss ON sn.Snack_ID = ss.Snack_ID " +
                "LEFT JOIN Shops       sh ON ss.Shop_ID  = sh.Shop_ID " +
                "LEFT JOIN Shopkeepers sk ON sh.S_ID     = sk.S_ID " +
                "WHERE sk.Dealer_ID = ? " +
                "ORDER BY sn.Snack_Name");
            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int stock = rs.getInt(6);
                snackModel.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    String.format("₹ %.2f", rs.getDouble(3)),
                    rs.getString(4),
                    rs.getString(5),
                    stock > 0 ? stock : "Out of Stock"
                });
            }
            refreshOrders();
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
        }
    }

    private void refreshOrders() {
        int dealerId = selectedDealerId();
        if (dealerId < 0) return;
        orderModel.setRowCount(0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT o.Order_ID, o.Order_date, sn.Snack_Name, " +
                "  o.Order_Quantity, o.Total_price, sh.Shop_name, o.status " +
                "FROM Orders o " +
                "JOIN Snacks      sn ON o.Snack_ID = sn.Snack_ID " +
                "JOIN Shopkeepers sk ON o.S_ID     = sk.S_ID " +
                "JOIN Shops       sh ON sk.S_ID    = sh.S_ID " +
                "WHERE o.cust_ID = ? AND sk.Dealer_ID = ? " +
                "ORDER BY o.Order_date DESC");
            ps.setInt(1, custId);
            ps.setInt(2, dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                orderModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getInt(4),
                    String.format("₹ %.2f", rs.getDouble(5)),
                    rs.getString(6), rs.getString(7)
                });
            }
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
        }
    }

    // ── Order operations ─────────────────────────────────────────────────────

    private void placeOrder() {
        int row = snackTable.getSelectedRow();
        if (row < 0) { warn("Select a snack to order."); return; }

        Object stockVal = snackModel.getValueAt(row, 5);
        if ("Out of Stock".equals(stockVal)) { warn("This snack is out of stock."); return; }

        int snackId   = (int) snackModel.getValueAt(row, 0);
        String snackName = (String) snackModel.getValueAt(row, 1);
        double price  = parsePrice(snackModel.getValueAt(row, 2).toString());
        int    stock  = (int) stockVal;

        // Find shopkeeper for this snack+dealer
        int shopkeeperId = getShopkeeperForSnack(snackId, selectedDealerId());
        if (shopkeeperId < 0) { err("Could not find a shopkeeper for this snack."); return; }

        // Qty dialog
        String qtyStr = JOptionPane.showInputDialog(this,
            "<html><b>Ordering:</b> " + snackName +
            "<br>Price: ₹ " + String.format("%.2f", price) +
            "<br>Available Stock: " + stock +
            "<br><br>Enter quantity:</html>",
            "Place Order", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr == null || qtyStr.isBlank()) return;

        int qty;
        try { qty = Integer.parseInt(qtyStr.trim()); }
        catch (NumberFormatException e) { warn("Invalid quantity."); return; }
        if (qty <= 0)       { warn("Quantity must be > 0."); return; }
        if (qty > stock)    { warn("Not enough stock. Available: " + stock); return; }

        double total = price * qty;
        double tax   = total * 0.18;
        double grand = total + tax;

        try {
            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try {
                // Insert Order
                PreparedStatement psO = conn.prepareStatement(
                    "INSERT INTO Orders (Order_date, Order_Quantity, Total_price, S_ID, cust_ID, Snack_ID, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'Pending')",
                    Statement.RETURN_GENERATED_KEYS);
                psO.setString(1, LocalDate.now().toString());
                psO.setInt(2, qty);
                psO.setDouble(3, total);
                psO.setInt(4, shopkeeperId);
                psO.setInt(5, custId);
                psO.setInt(6, snackId);
                psO.executeUpdate();
                ResultSet keys = psO.getGeneratedKeys();
                keys.next();
                int orderId = keys.getInt(1);

                // Insert Bill
                PreparedStatement psB = conn.prepareStatement(
                    "INSERT INTO Bill (Order_ID, Bill_date, Tax, Total_Amount, cust_ID) VALUES (?,?,?,?,?)");
                psB.setInt(1, orderId);
                psB.setString(2, LocalDate.now().toString());
                psB.setDouble(3, Math.round(tax * 100.0) / 100.0);
                psB.setDouble(4, Math.round(grand * 100.0) / 100.0);
                psB.setInt(5, custId);
                psB.executeUpdate();

                // Reduce stock
                PreparedStatement psS = conn.prepareStatement(
                    "UPDATE Shop_Snacks ss " +
                    "JOIN Shops sh ON ss.Shop_ID = sh.Shop_ID " +
                    "JOIN Shopkeepers sk ON sh.S_ID = sk.S_ID " +
                    "SET ss.stock_qty = ss.stock_qty - ? " +
                    "WHERE ss.Snack_ID = ? AND sk.S_ID = ?");
                psS.setInt(1, qty);
                psS.setInt(2, snackId);
                psS.setInt(3, shopkeeperId);
                psS.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this,
                    String.format("<html><b>Order placed successfully!</b><br>" +
                        "Snack : %s × %d<br>" +
                        "Subtotal : ₹ %.2f<br>" +
                        "Tax (18%%) : ₹ %.2f<br>" +
                        "<b>Total : ₹ %.2f</b><br>" +
                        "<br>Bill generated on dealer dashboard.</html>",
                        snackName, qty, total, tax, grand),
                    "Order Placed", JOptionPane.INFORMATION_MESSAGE);

                loadSnacks(); // refresh both tables
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
        }
    }

    private void editOrder() {
        int row = orderTable.getSelectedRow();
        if (row < 0) { warn("Select an order to edit."); return; }

        String status = (String) orderModel.getValueAt(row, 6);
        if (!"Pending".equals(status)) {
            warn("Only Pending orders can be edited.");
            return;
        }

        int orderId   = (int) orderModel.getValueAt(row, 0);
        int currentQty= (int) orderModel.getValueAt(row, 3);

        String qtyStr = JOptionPane.showInputDialog(this,
            "<html>Edit quantity for Order #" + orderId +
            "<br>Current Qty: " + currentQty +
            "<br><br>New quantity:</html>",
            "Edit Order", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr == null || qtyStr.isBlank()) return;

        int newQty;
        try { newQty = Integer.parseInt(qtyStr.trim()); }
        catch (NumberFormatException e) { warn("Invalid quantity."); return; }
        if (newQty <= 0) { warn("Quantity must be > 0."); return; }

        try {
            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try {
                // Get price per unit
                PreparedStatement psP = conn.prepareStatement(
                    "SELECT sn.Price, o.S_ID, o.Snack_ID FROM Orders o " +
                    "JOIN Snacks sn ON o.Snack_ID = sn.Snack_ID WHERE o.Order_ID=?");
                psP.setInt(1, orderId);
                ResultSet rs = psP.executeQuery();
                rs.next();
                double unitPrice  = rs.getDouble(1);
                int    skId       = rs.getInt(2);
                int    snackId    = rs.getInt(3);

                int    diff       = newQty - currentQty;   // positive = need more stock
                double newTotal   = unitPrice * newQty;
                double newTax     = newTotal * 0.18;
                double newGrand   = newTotal + newTax;

                // Update Order
                PreparedStatement psO = conn.prepareStatement(
                    "UPDATE Orders SET Order_Quantity=?, Total_price=? WHERE Order_ID=?");
                psO.setInt(1, newQty);
                psO.setDouble(2, newTotal);
                psO.setInt(3, orderId);
                psO.executeUpdate();

                // Update Bill
                PreparedStatement psB = conn.prepareStatement(
                    "UPDATE Bill SET Tax=?, Total_Amount=? WHERE Order_ID=?");
                psB.setDouble(1, Math.round(newTax * 100.0) / 100.0);
                psB.setDouble(2, Math.round(newGrand * 100.0) / 100.0);
                psB.setInt(3, orderId);
                psB.executeUpdate();

                // Adjust stock (subtract diff — negative diff restores stock)
                PreparedStatement psS = conn.prepareStatement(
                    "UPDATE Shop_Snacks ss " +
                    "JOIN Shops sh ON ss.Shop_ID = sh.Shop_ID " +
                    "JOIN Shopkeepers sk ON sh.S_ID = sk.S_ID " +
                    "SET ss.stock_qty = ss.stock_qty - ? " +
                    "WHERE ss.Snack_ID = ? AND sk.S_ID = ?");
                psS.setInt(1, diff);
                psS.setInt(2, snackId);
                psS.setInt(3, skId);
                psS.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this,
                    "Order #" + orderId + " updated to qty " + newQty,
                    "Updated", JOptionPane.INFORMATION_MESSAGE);
                loadSnacks();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
        }
    }

    private void cancelOrder() {
        int row = orderTable.getSelectedRow();
        if (row < 0) { warn("Select an order to cancel."); return; }

        String status = (String) orderModel.getValueAt(row, 6);
        if (!"Pending".equals(status)) {
            warn("Only Pending orders can be cancelled.");
            return;
        }

        int orderId    = (int) orderModel.getValueAt(row, 0);
        int qty        = (int) orderModel.getValueAt(row, 3);
        int confirm    = JOptionPane.showConfirmDialog(this,
            "Cancel Order #" + orderId + "? This will restore stock.",
            "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try {
                // Get snack + shopkeeper before delete
                PreparedStatement psI = conn.prepareStatement(
                    "SELECT o.Snack_ID, o.S_ID FROM Orders o WHERE o.Order_ID=?");
                psI.setInt(1, orderId);
                ResultSet rs = psI.executeQuery();
                rs.next();
                int snackId = rs.getInt(1);
                int skId    = rs.getInt(2);

                // Delete Bill first (FK)
                PreparedStatement psDB = conn.prepareStatement(
                    "DELETE FROM Bill WHERE Order_ID=?");
                psDB.setInt(1, orderId);
                psDB.executeUpdate();

                // Delete Order
                PreparedStatement psDO = conn.prepareStatement(
                    "DELETE FROM Orders WHERE Order_ID=?");
                psDO.setInt(1, orderId);
                psDO.executeUpdate();

                // Restore stock
                PreparedStatement psS = conn.prepareStatement(
                    "UPDATE Shop_Snacks ss " +
                    "JOIN Shops sh ON ss.Shop_ID = sh.Shop_ID " +
                    "JOIN Shopkeepers sk ON sh.S_ID = sk.S_ID " +
                    "SET ss.stock_qty = ss.stock_qty + ? " +
                    "WHERE ss.Snack_ID = ? AND sk.S_ID = ?");
                psS.setInt(1, qty);
                psS.setInt(2, snackId);
                psS.setInt(3, skId);
                psS.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this,
                    "Order #" + orderId + " cancelled and stock restored.",
                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                loadSnacks();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the S_ID of a shopkeeper who sells this snack under the given dealer. */
    private int getShopkeeperForSnack(int snackId, int dealerId) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT sk.S_ID FROM Shopkeepers sk " +
                "JOIN Shops       sh ON sk.S_ID    = sh.S_ID " +
                "JOIN Shop_Snacks ss ON sh.Shop_ID  = ss.Shop_ID " +
                "WHERE ss.Snack_ID = ? AND sk.Dealer_ID = ? LIMIT 1");
            ps.setInt(1, snackId);
            ps.setInt(2, dealerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
        }
        return -1;
    }

    private double parsePrice(String s) {
        return Double.parseDouble(s.replace("₹", "").replace(",", "").trim());
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Status cell renderer ──────────────────────────────────────────────────

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            String s = v == null ? "" : v.toString();
            if (!sel) {
                switch (s) {
                    case "Delivered" -> setBackground(new Color(200, 240, 200));
                    case "Pending"   -> setBackground(new Color(255, 243, 200));
                    case "Confirmed" -> setBackground(new Color(200, 220, 255));
                    case "Cancelled" -> setBackground(new Color(255, 200, 200));
                    default          -> setBackground(UIConstants.WHITE);
                }
            }
            setBorder(new EmptyBorder(0, 8, 0, 8));
            return this;
        }
    }
}
