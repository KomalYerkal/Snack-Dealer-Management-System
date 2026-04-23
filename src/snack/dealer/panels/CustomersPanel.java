package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class CustomersPanel extends JPanel {

    private final int dealerId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {
        "Cust ID","First Name","Last Name","Contact","Address","Shop","Shopkeeper"
    };

    public CustomersPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Customers",
            "Customers linked to your dealer network (via Shops → Shopkeepers)"), BorderLayout.NORTH);

        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new EmptyBorder(0,0,0,0));
        sp.getViewport().setBackground(UIConstants.WHITE);

        JButton btnAdd     = UIConstants.btnAdd();
        JButton btnEdit    = UIConstants.btnEdit();
        JButton btnDel     = UIConstants.btnDelete();
        JButton btnRefresh = UIConstants.btnRefresh();
        JPanel toolbar = UIConstants.makeButtonBar(btnAdd, btnEdit, btnDel, btnRefresh);

        add(toolbar, BorderLayout.SOUTH);
        add(sp,      BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadData());
        btnAdd.addActionListener(e     -> showDialog(null));
        btnEdit.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { warn("Select a customer to edit."); return; }
            showDialog(r);
        });
        btnDel.addActionListener(e -> deleteSelected());
    }

    private void loadData() {
        model.setRowCount(0);
        String sql = """
            SELECT c.cust_ID, c.F_name, c.L_name, c.C_Contact_no, c.C_Address,
                   sh.Shop_name, sk.S_Name
            FROM Customers c
            LEFT JOIN Shops       sh ON c.Shop_ID  = sh.Shop_ID
            LEFT JOIN Shopkeepers sk ON sh.S_ID     = sk.S_ID
            WHERE sk.Dealer_ID = ? OR c.Shop_ID IS NULL
            ORDER BY c.cust_ID
            """;
        // also show customers with no shop if they belong to this dealer indirectly
        String sql2 = """
            SELECT c.cust_ID, c.F_name, c.L_name, c.C_Contact_no, c.C_Address,
                   sh.Shop_name, sk.S_Name
            FROM Customers c
            LEFT JOIN Shops       sh ON c.Shop_ID  = sh.Shop_ID
            LEFT JOIN Shopkeepers sk ON sh.S_ID     = sk.S_ID
            WHERE sk.Dealer_ID = ?
            ORDER BY c.cust_ID
            """;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql2);
            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("cust_ID"),
                    rs.getString("F_name"),
                    rs.getString("L_name"),
                    rs.getString("C_Contact_no"),
                    rs.getString("C_Address"),
                    rs.getString("Shop_name"),
                    rs.getString("S_Name")
                });
            }
        } catch (SQLException ex) { showErr(ex); }
    }

    private void showDialog(Integer row) {
        boolean editing = (row != null);
        int custId = editing ? (int)model.getValueAt(row, 0) : -1;

        JTextField tfFirst    = UIConstants.makeField(20);
        JTextField tfLast     = UIConstants.makeField(20);
        JTextField tfContact  = UIConstants.makeField(20);
        JTextField tfAddress  = UIConstants.makeField(20);
        JComboBox<String> cbShop = new JComboBox<>();
        cbShop.setFont(UIConstants.LABEL_FONT);

        // Populate shops belonging to this dealer
        Map<String,Integer> shopMap = new LinkedHashMap<>();
        shopMap.put("-- None --", 0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT sh.Shop_ID, sh.Shop_name FROM Shops sh " +
                "JOIN Shopkeepers sk ON sh.S_ID=sk.S_ID WHERE sk.Dealer_ID=? ORDER BY sh.Shop_name");
            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) shopMap.put(rs.getString("Shop_name"), rs.getInt("Shop_ID"));
        } catch (SQLException ex) { showErr(ex); return; }
        shopMap.keySet().forEach(cbShop::addItem);

        if (editing) {
            tfFirst.setText((String)model.getValueAt(row, 1));
            tfLast.setText((String)model.getValueAt(row, 2));
            tfContact.setText((String)model.getValueAt(row, 3));
            tfAddress.setText((String)model.getValueAt(row, 4));
            String shopName = (String)model.getValueAt(row, 5);
            if (shopName != null) cbShop.setSelectedItem(shopName);
        }

        JDialog dlg = UIConstants.makeDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            editing ? "Edit Customer" : "Add Customer", 420, 360);

        JPanel form = UIConstants.formGrid(
            new String[]{"First Name *","Last Name","Contact No","Address","Shop"},
            new JComponent[]{tfFirst, tfLast, tfContact, tfAddress, cbShop}
        );
        JButton save   = UIConstants.btnSave();
        JButton cancel = UIConstants.btnCancel();
        JPanel btns = UIConstants.makeButtonBar(save, cancel);
        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            String fn = tfFirst.getText().trim();
            if (fn.isEmpty()) { warn("First name required."); return; }
            int shopId = shopMap.getOrDefault((String)cbShop.getSelectedItem(), 0);
            try {
                Connection con = DatabaseConnection.getConnection();
                if (editing) {
                    PreparedStatement ps = con.prepareStatement(
                        "UPDATE Customers SET F_name=?,L_name=?,C_Contact_no=?,C_Address=?,Shop_ID=? WHERE cust_ID=?");
                    ps.setString(1,fn); ps.setString(2,tfLast.getText().trim());
                    ps.setString(3,tfContact.getText().trim()); ps.setString(4,tfAddress.getText().trim());
                    if (shopId>0) ps.setInt(5,shopId); else ps.setNull(5,Types.INTEGER);
                    ps.setInt(6,custId); ps.executeUpdate();
                } else {
                    PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Customers(F_name,L_name,C_Contact_no,C_Address,Shop_ID) VALUES(?,?,?,?,?)");
                    ps.setString(1,fn); ps.setString(2,tfLast.getText().trim());
                    ps.setString(3,tfContact.getText().trim()); ps.setString(4,tfAddress.getText().trim());
                    if (shopId>0) ps.setInt(5,shopId); else ps.setNull(5,Types.INTEGER);
                    ps.executeUpdate();
                }
                dlg.dispose(); loadData();
            } catch (SQLException ex) { showErr(ex); }
        });
        cancel.addActionListener(e -> dlg.dispose());
        dlg.setVisible(true);
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) { warn("Select a customer to delete."); return; }
        int id = (int)model.getValueAt(r, 0);
        if (JOptionPane.showConfirmDialog(this,
            "Delete customer #"+id+"? Related orders/bills will also be removed.",
            "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "DELETE FROM Customers WHERE cust_ID=?");
            ps.setInt(1, id); ps.executeUpdate(); loadData();
        } catch (SQLException ex) { showErr(ex); }
    }

    private void warn(String m) { JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.WARNING_MESSAGE); }
    private void showErr(Exception e) { JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
}