package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class SnacksPanel extends JPanel {

    private final int dealerId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {
        "Snack ID","Snack Name","Mfg Date","Expiry Date","Price (₹)","Manufacturer"
    };

    public SnacksPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Snacks","All snacks with manufacturer info"), BorderLayout.NORTH);
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
            if (r < 0) { warn("Select a snack."); return; }
            showDialog(r);
        });
        btnDel.addActionListener(e -> deleteSelected());
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            Statement st = DatabaseConnection.getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT s.Snack_ID,s.Snack_Name,s.MFd,s.Expiry,s.Price,i.I_Name " +
                "FROM Snacks s LEFT JOIN Industries i ON s.L_ID=i.L_ID ORDER BY s.Snack_ID");
            while (rs.next())
                model.addRow(new Object[]{
                    rs.getInt(1),rs.getString(2),rs.getString(3),
                    rs.getString(4),rs.getDouble(5),rs.getString(6)
                });
        } catch (SQLException ex) { showErr(ex); }
    }

    private void showDialog(Integer row) {
        boolean editing = (row != null);
        int snackId = editing ? (int)model.getValueAt(row,0) : -1;

        JTextField tfName   = UIConstants.makeField(20);
        JTextField tfMfd    = UIConstants.makeField(10);
        JTextField tfExpiry = UIConstants.makeField(10);
        JTextField tfPrice  = UIConstants.makeField(10);
        JComboBox<String> cbInd = new JComboBox<>();
        cbInd.setFont(UIConstants.LABEL_FONT);

        Map<String,Integer> indMap = new LinkedHashMap<>();
        indMap.put("-- None --", 0);
        try {
            ResultSet rs = DatabaseConnection.getConnection().createStatement()
                .executeQuery("SELECT L_ID,I_Name FROM Industries ORDER BY I_Name");
            while (rs.next()) indMap.put(rs.getString("I_Name"), rs.getInt("L_ID"));
        } catch (SQLException ex) { showErr(ex); return; }
        indMap.keySet().forEach(cbInd::addItem);

        if (editing) {
            tfName.setText((String)model.getValueAt(row,1));
            tfMfd.setText((String)model.getValueAt(row,2));
            tfExpiry.setText((String)model.getValueAt(row,3));
            tfPrice.setText(String.valueOf(model.getValueAt(row,4)));
            String mfr = (String)model.getValueAt(row,5);
            if (mfr!=null) cbInd.setSelectedItem(mfr);
        }

        JDialog dlg = UIConstants.makeDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            editing ? "Edit Snack" : "Add Snack", 420, 340);
        JPanel form = UIConstants.formGrid(
            new String[]{"Name *","Mfg Date (yyyy-MM-dd)","Expiry (yyyy-MM-dd)","Price (₹) *","Manufacturer"},
            new JComponent[]{tfName,tfMfd,tfExpiry,tfPrice,cbInd});
        JButton save = UIConstants.btnSave(), cancel = UIConstants.btnCancel();
        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(UIConstants.makeButtonBar(save,cancel), BorderLayout.SOUTH);

        save.addActionListener(e -> {
            try {
                String name = tfName.getText().trim();
                if (name.isEmpty()) { warn("Name required."); return; }
                double price = Double.parseDouble(tfPrice.getText().trim());
                int lId = indMap.getOrDefault((String)cbInd.getSelectedItem(), 0);
                Connection con = DatabaseConnection.getConnection();
                if (editing) {
                    PreparedStatement ps = con.prepareStatement(
                        "UPDATE Snacks SET Snack_Name=?,MFd=?,Expiry=?,Price=?,L_ID=? WHERE Snack_ID=?");
                    ps.setString(1,name);
                    ps.setString(2,tfMfd.getText().trim().isEmpty()?null:tfMfd.getText().trim());
                    ps.setString(3,tfExpiry.getText().trim().isEmpty()?null:tfExpiry.getText().trim());
                    ps.setDouble(4,price);
                    if(lId>0)ps.setInt(5,lId);else ps.setNull(5,Types.INTEGER);
                    ps.setInt(6,snackId); ps.executeUpdate();
                } else {
                    PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Snacks(Snack_Name,MFd,Expiry,Price,L_ID) VALUES(?,?,?,?,?)");
                    ps.setString(1,name);
                    ps.setString(2,tfMfd.getText().trim().isEmpty()?null:tfMfd.getText().trim());
                    ps.setString(3,tfExpiry.getText().trim().isEmpty()?null:tfExpiry.getText().trim());
                    ps.setDouble(4,price);
                    if(lId>0)ps.setInt(5,lId);else ps.setNull(5,Types.INTEGER);
                    ps.executeUpdate();
                }
                dlg.dispose(); loadData();
            } catch (NumberFormatException nfe) { warn("Price must be a number."); }
            catch (SQLException ex) { showErr(ex); }
        });
        cancel.addActionListener(e -> dlg.dispose());
        dlg.setVisible(true);
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r<0){warn("Select a snack.");return;}
        int id=(int)model.getValueAt(r,0);
        if(JOptionPane.showConfirmDialog(this,"Delete snack #"+id+"?",
            "Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{
            PreparedStatement ps=DatabaseConnection.getConnection()
                .prepareStatement("DELETE FROM Snacks WHERE Snack_ID=?");
            ps.setInt(1,id);ps.executeUpdate();loadData();
        }catch(SQLException ex){showErr(ex);}
    }

    private void warn(String m){JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.WARNING_MESSAGE);}
    private void showErr(Exception e){JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
}