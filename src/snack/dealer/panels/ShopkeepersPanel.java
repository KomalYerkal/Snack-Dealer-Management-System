package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class ShopkeepersPanel extends JPanel {

    private final int dealerId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {"S_ID","Name","Contact No","Dealer"};

    public ShopkeepersPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Shopkeepers","Manage shopkeepers under your dealership"), BorderLayout.NORTH);
        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);

        JButton btnAdd     = UIConstants.btnAdd();
        JButton btnEdit    = UIConstants.btnEdit();
        JButton btnDel     = UIConstants.btnDelete();
        JButton btnRefresh = UIConstants.btnRefresh();
        add(UIConstants.makeButtonBar(btnAdd, btnEdit, btnDel, btnRefresh), BorderLayout.SOUTH);
        add(sp, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadData());
        btnAdd.addActionListener(e    -> showDialog(null));
        btnEdit.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { warn("Select a row."); return; }
            showDialog(r);
        });
        btnDel.addActionListener(e -> deleteSelected());
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT sk.S_ID, sk.S_Name, sk.S_Contact_no, sd.Dealer_Name " +
                "FROM Shopkeepers sk JOIN Snack_Dealer sd ON sk.Dealer_ID=sd.Dealer_ID " +
                "WHERE sk.Dealer_ID=? ORDER BY sk.S_ID");
            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4)});
        } catch (SQLException ex) { showErr(ex); }
    }

    private void showDialog(Integer row) {
        boolean editing = (row != null);
        int sId = editing ? (int)model.getValueAt(row,0) : -1;

        JTextField tfName    = UIConstants.makeField(20);
        JTextField tfContact = UIConstants.makeField(20);
        if (editing) {
            tfName.setText((String)model.getValueAt(row,1));
            tfContact.setText((String)model.getValueAt(row,2));
        }

        JDialog dlg = UIConstants.makeDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            editing ? "Edit Shopkeeper" : "Add Shopkeeper", 380, 260);
        JPanel form = UIConstants.formGrid(
            new String[]{"Name *","Contact No"},
            new JComponent[]{tfName, tfContact});
        JButton save = UIConstants.btnSave(), cancel = UIConstants.btnCancel();
        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(UIConstants.makeButtonBar(save,cancel), BorderLayout.SOUTH);

        save.addActionListener(e -> {
            if (tfName.getText().trim().isEmpty()) { warn("Name required."); return; }
            try {
                Connection con = DatabaseConnection.getConnection();
                if (editing) {
                    PreparedStatement ps = con.prepareStatement(
                        "UPDATE Shopkeepers SET S_Name=?,S_Contact_no=? WHERE S_ID=?");
                    ps.setString(1,tfName.getText().trim());
                    ps.setString(2,tfContact.getText().trim());
                    ps.setInt(3,sId); ps.executeUpdate();
                } else {
                    PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Shopkeepers(S_Name,S_Contact_no,Dealer_ID) VALUES(?,?,?)");
                    ps.setString(1,tfName.getText().trim());
                    ps.setString(2,tfContact.getText().trim());
                    ps.setInt(3,dealerId); ps.executeUpdate();
                }
                dlg.dispose(); loadData();
            } catch (SQLException ex) { showErr(ex); }
        });
        cancel.addActionListener(e -> dlg.dispose());
        dlg.setVisible(true);
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) { warn("Select a row."); return; }
        int id = (int)model.getValueAt(r,0);
        if (JOptionPane.showConfirmDialog(this,"Delete shopkeeper #"+id+"?",
            "Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("DELETE FROM Shopkeepers WHERE S_ID=?");
            ps.setInt(1,id); ps.executeUpdate(); loadData();
        } catch (SQLException ex) { showErr(ex); }
    }

    private void warn(String m){JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.WARNING_MESSAGE);}
    private void showErr(Exception e){JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
}