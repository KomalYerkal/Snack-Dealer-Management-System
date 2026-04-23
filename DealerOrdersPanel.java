package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Orders panel — dealer view.
 * Creating an order AUTOMATICALLY generates a Bill (18% GST).
 * Both are immediately visible to the buyer.
 */
public class DealerOrdersPanel extends JPanel {

    private final int dealerId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {
        "Order ID","Date","Customer","Snack","Qty","Total (₹)","Status","Shopkeeper"
    };

    public DealerOrdersPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Orders",
            "Create & manage orders — bills are auto-generated for buyers"), BorderLayout.NORTH);

        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);

        JButton btnNew     = UIConstants.btnAdd();   btnNew.setText("＋ New Order");
        JButton btnStatus  = UIConstants.makeButton("✎ Update Status", UIConstants.WARNING);
        JButton btnDel     = UIConstants.btnDelete();
        JButton btnRefresh = UIConstants.btnRefresh();

        add(UIConstants.makeButtonBar(btnNew, btnStatus, btnDel, btnRefresh), BorderLayout.SOUTH);
        add(sp, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadData());
        btnNew.addActionListener(e    -> showNewOrderDialog());
        btnStatus.addActionListener(e -> updateStatus());
        btnDel.addActionListener(e    -> deleteSelected());
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT o.Order_ID, o.Order_date, " +
                "  CONCAT(c.F_name,' ',COALESCE(c.L_name,'')) AS cname, " +
                "  sn.Snack_Name, o.Order_Quantity, o.Total_price, o.status, sk.S_Name " +
                "FROM Orders o " +
                "JOIN Customers   c  ON o.cust_ID  = c.cust_ID " +
                "JOIN Snacks      sn ON o.Snack_ID = sn.Snack_ID " +
                "JOIN Shopkeepers sk ON o.S_ID     = sk.S_ID " +
                "WHERE sk.Dealer_ID = ? " +
                "ORDER BY o.Order_date DESC, o.Order_ID DESC");
            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getInt(5),
                    String.format("₹ %.2f", rs.getDouble(6)),
                    rs.getString(7), rs.getString(8)
                });
            }
        } catch (SQLException ex) { showErr(ex); }
    }

    // ── New Order Dialog ─────────────────────────────────────
    private void showNewOrderDialog() {
        // Load customers
        Map<String,Integer> custMap = new LinkedHashMap<>();
        Map<String,int[]>   snackMap = new LinkedHashMap<>(); // name → [id, price*100]
        Map<String,Integer> skMap   = new LinkedHashMap<>();

        try {
            Connection con = DatabaseConnection.getConnection();
            // Customers via dealer chain
            PreparedStatement psc = con.prepareStatement(
                "SELECT c.cust_ID, CONCAT(c.F_name,' ',COALESCE(c.L_name,'')) " +
                "FROM Customers c JOIN Shops sh ON c.Shop_ID=sh.Shop_ID " +
                "JOIN Shopkeepers sk ON sh.S_ID=sk.S_ID WHERE sk.Dealer_ID=? ORDER BY c.F_name");
            psc.setInt(1,dealerId);
            ResultSet rs = psc.executeQuery();
            while (rs.next()) custMap.put(rs.getString(2), rs.getInt(1));

            // Snacks
            rs = con.createStatement().executeQuery(
                "SELECT Snack_ID,Snack_Name,Price FROM Snacks ORDER BY Snack_Name");
            while (rs.next()) snackMap.put(rs.getString(2), new int[]{rs.getInt(1),(int)(rs.getDouble(3)*100)});

            // Shopkeepers
            PreparedStatement psk = con.prepareStatement(
                "SELECT S_ID,S_Name FROM Shopkeepers WHERE Dealer_ID=? ORDER BY S_Name");
            psk.setInt(1,dealerId);
            rs = psk.executeQuery();
            while (rs.next()) skMap.put(rs.getString(2), rs.getInt(1));
        } catch (SQLException ex) { showErr(ex); return; }

        if (custMap.isEmpty()) { warn("No customers linked to your shops. Add customers first."); return; }
        if (snackMap.isEmpty()) { warn("No snacks available."); return; }
        if (skMap.isEmpty())   { warn("No shopkeepers under your dealership."); return; }

        JComboBox<String> cbCust  = new JComboBox<>(custMap.keySet().toArray(new String[0]));
        JComboBox<String> cbSnack = new JComboBox<>(snackMap.keySet().toArray(new String[0]));
        JComboBox<String> cbSK    = new JComboBox<>(skMap.keySet().toArray(new String[0]));
        JTextField tfQty   = UIConstants.makeField(6);
        JTextField tfPrice = UIConstants.makeField(10);
        JTextField tfDate  = UIConstants.makeField(12);
        tfDate.setText(LocalDate.now().toString());
        tfPrice.setEditable(false);
        tfPrice.setBackground(new Color(240,240,240));
        cbCust.setFont(UIConstants.LABEL_FONT);
        cbSnack.setFont(UIConstants.LABEL_FONT);
        cbSK.setFont(UIConstants.LABEL_FONT);

        // Auto-calc price
        Runnable calcPrice = () -> {
            try {
                String sname = (String)cbSnack.getSelectedItem();
                int qty = Integer.parseInt(tfQty.getText().trim());
                if (sname != null && qty > 0) {
                    double unit = snackMap.get(sname)[1] / 100.0;
                    tfPrice.setText(String.format("%.2f", unit * qty));
                }
            } catch (NumberFormatException ignored) { tfPrice.setText(""); }
        };
        cbSnack.addActionListener(e -> calcPrice.run());
        tfQty.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e){calcPrice.run();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){calcPrice.run();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        JDialog dlg = UIConstants.makeDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            "Create New Order", 450, 370);
        JPanel form = UIConstants.formGrid(
            new String[]{"Customer *","Snack *","Shopkeeper *","Quantity *","Date (yyyy-MM-dd) *","Total Price (₹)"},
            new JComponent[]{cbCust,cbSnack,cbSK,tfQty,tfDate,tfPrice});
        JButton save = UIConstants.btnSave(), cancel = UIConstants.btnCancel();
        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(UIConstants.makeButtonBar(save,cancel), BorderLayout.SOUTH);

        save.addActionListener(e -> {
            try {
                int qty = Integer.parseInt(tfQty.getText().trim());
                if (qty <= 0) { warn("Qty must be > 0"); return; }
                String dateStr = tfDate.getText().trim();
                if (dateStr.isEmpty()) { warn("Date required."); return; }

                int custId  = custMap.get((String)cbCust.getSelectedItem());
                int snackId = snackMap.get((String)cbSnack.getSelectedItem())[0];
                int skId    = skMap.get((String)cbSK.getSelectedItem());
                double unitPrice = snackMap.get((String)cbSnack.getSelectedItem())[1] / 100.0;
                double total     = unitPrice * qty;

                Connection con = DatabaseConnection.getConnection();
                con.setAutoCommit(false);
                try {
                    // 1. Insert Order
                    PreparedStatement psO = con.prepareStatement(
                        "INSERT INTO Orders(Order_date,Order_Quantity,Total_price,S_ID,cust_ID,Snack_ID,status) " +
                        "VALUES(?,?,?,?,?,?,'Pending')", Statement.RETURN_GENERATED_KEYS);
                    psO.setString(1, dateStr);
                    psO.setInt(2, qty);
                    psO.setDouble(3, total);
                    psO.setInt(4, skId);
                    psO.setInt(5, custId);
                    psO.setInt(6, snackId);
                    psO.executeUpdate();
                    ResultSet gk = psO.getGeneratedKeys(); gk.next();
                    int orderId = gk.getInt(1);

                    // 2. Auto-generate Bill (18% GST)
                    double tax       = Math.round(total * 0.18 * 100) / 100.0;
                    double totalAmt  = Math.round((total + tax) * 100) / 100.0;
                    PreparedStatement psB = con.prepareStatement(
                        "INSERT INTO Bill(Order_ID,Bill_date,Tax,Total_Amount,cust_ID) VALUES(?,?,?,?,?)");
                    psB.setInt(1, orderId);
                    psB.setString(2, dateStr);
                    psB.setDouble(3, tax);
                    psB.setDouble(4, totalAmt);
                    psB.setInt(5, custId);
                    psB.executeUpdate();

                    con.commit();
                    dlg.dispose();
                    loadData();
                    JOptionPane.showMessageDialog(this,
                        "<html>Order created!<br>Bill auto-generated for customer.<br>" +
                        "Subtotal: ₹"+String.format("%.2f",total)+
                        " | Tax(18%): ₹"+String.format("%.2f",tax)+
                        " | Total: ₹"+String.format("%.2f",totalAmt)+"</html>",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                } finally { con.setAutoCommit(true); }
            } catch (NumberFormatException nfe) { warn("Quantity must be a number."); }
            catch (SQLException ex) { showErr(ex); }
        });
        cancel.addActionListener(e -> dlg.dispose());
        dlg.setVisible(true);
    }

    // ── Update order status ──────────────────────────────────
    private void updateStatus() {
        int r = table.getSelectedRow();
        if (r < 0) { warn("Select an order."); return; }
        int orderId = (int)model.getValueAt(r, 0);
        String current = (String)model.getValueAt(r, 6);
        String[] statuses = {"Pending","Confirmed","Delivered","Cancelled"};
        String chosen = (String)JOptionPane.showInputDialog(this,
            "Select new status for Order #"+orderId, "Update Status",
            JOptionPane.QUESTION_MESSAGE, null, statuses, current);
        if (chosen == null) return;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "UPDATE Orders SET status=? WHERE Order_ID=?");
            ps.setString(1,chosen); ps.setInt(2,orderId); ps.executeUpdate();
            loadData();
        } catch (SQLException ex) { showErr(ex); }
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r<0){warn("Select an order.");return;}
        int id=(int)model.getValueAt(r,0);
        if(JOptionPane.showConfirmDialog(this,
            "Delete order #"+id+"? The associated bill will also be deleted.",
            "Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{
            PreparedStatement ps=DatabaseConnection.getConnection().prepareStatement(
                "DELETE FROM Orders WHERE Order_ID=?");
            ps.setInt(1,id);ps.executeUpdate();loadData();
        }catch(SQLException ex){showErr(ex);}
    }

    private void warn(String m){JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.WARNING_MESSAGE);}
    private void showErr(Exception e){JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
}