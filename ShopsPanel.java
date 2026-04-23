package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class ShopsPanel extends JPanel {

    private final int dealerId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {"Shop ID","Shop Name","Address","Contact","Shopkeeper"};

    public ShopsPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Shops","Shops managed by your shopkeepers"), BorderLayout.NORTH);
        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);

        JButton btnAdd=UIConstants.btnAdd(), btnEdit=UIConstants.btnEdit(),
                btnDel=UIConstants.btnDelete(), btnRefresh=UIConstants.btnRefresh();
        add(UIConstants.makeButtonBar(btnAdd,btnEdit,btnDel,btnRefresh), BorderLayout.SOUTH);
        add(sp, BorderLayout.CENTER);

        btnRefresh.addActionListener(e->loadData());
        btnAdd.addActionListener(e->showDialog(null));
        btnEdit.addActionListener(e->{int r=table.getSelectedRow();if(r<0){warn("Select a row.");return;}showDialog(r);});
        btnDel.addActionListener(e->deleteSelected());
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT sh.Shop_ID,sh.Shop_name,sh.Shop_Address,sh.Shop_C_no,sk.S_Name " +
                "FROM Shops sh JOIN Shopkeepers sk ON sh.S_ID=sk.S_ID " +
                "WHERE sk.Dealer_ID=? ORDER BY sh.Shop_ID");
            ps.setInt(1,dealerId);
            ResultSet rs=ps.executeQuery();
            while(rs.next())
                model.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)});
        }catch(SQLException ex){showErr(ex);}
    }

    private void showDialog(Integer row) {
        boolean editing=row!=null;
        int shopId=editing?(int)model.getValueAt(row,0):-1;

        JTextField tfName=UIConstants.makeField(20),tfAddr=UIConstants.makeField(20),tfCno=UIConstants.makeField(20);
        JComboBox<String> cbSK=new JComboBox<>();cbSK.setFont(UIConstants.LABEL_FONT);

        Map<String,Integer> skMap=new LinkedHashMap<>();
        try{
            PreparedStatement ps=DatabaseConnection.getConnection().prepareStatement(
                "SELECT S_ID,S_Name FROM Shopkeepers WHERE Dealer_ID=? ORDER BY S_Name");
            ps.setInt(1,dealerId);ResultSet rs=ps.executeQuery();
            while(rs.next())skMap.put(rs.getString("S_Name"),rs.getInt("S_ID"));
        }catch(SQLException ex){showErr(ex);return;}
        skMap.keySet().forEach(cbSK::addItem);

        if(editing){
            tfName.setText((String)model.getValueAt(row,1));
            tfAddr.setText((String)model.getValueAt(row,2));
            tfCno.setText((String)model.getValueAt(row,3));
            cbSK.setSelectedItem(model.getValueAt(row,4));
        }

        JDialog dlg=UIConstants.makeDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            editing?"Edit Shop":"Add Shop",420,300);
        JPanel form=UIConstants.formGrid(
            new String[]{"Shop Name *","Address","Contact No","Shopkeeper *"},
            new JComponent[]{tfName,tfAddr,tfCno,cbSK});
        JButton save=UIConstants.btnSave(),cancel=UIConstants.btnCancel();
        dlg.setLayout(new BorderLayout());
        dlg.add(form,BorderLayout.CENTER);
        dlg.add(UIConstants.makeButtonBar(save,cancel),BorderLayout.SOUTH);

        save.addActionListener(e->{
            if(tfName.getText().trim().isEmpty()){warn("Name required.");return;}
            int skId=skMap.getOrDefault((String)cbSK.getSelectedItem(),0);
            if(skId==0){warn("Select a shopkeeper.");return;}
            try{
                Connection con=DatabaseConnection.getConnection();
                if(editing){
                    PreparedStatement ps=con.prepareStatement(
                        "UPDATE Shops SET Shop_name=?,Shop_Address=?,Shop_C_no=?,S_ID=? WHERE Shop_ID=?");
                    ps.setString(1,tfName.getText().trim());ps.setString(2,tfAddr.getText().trim());
                    ps.setString(3,tfCno.getText().trim());ps.setInt(4,skId);ps.setInt(5,shopId);ps.executeUpdate();
                }else{
                    PreparedStatement ps=con.prepareStatement(
                        "INSERT INTO Shops(Shop_name,Shop_Address,Shop_C_no,S_ID) VALUES(?,?,?,?)");
                    ps.setString(1,tfName.getText().trim());ps.setString(2,tfAddr.getText().trim());
                    ps.setString(3,tfCno.getText().trim());ps.setInt(4,skId);ps.executeUpdate();
                }
                dlg.dispose();loadData();
            }catch(SQLException ex){showErr(ex);}
        });
        cancel.addActionListener(e->dlg.dispose());
        dlg.setVisible(true);
    }

    private void deleteSelected(){
        int r=table.getSelectedRow();if(r<0){warn("Select a row.");return;}
        int id=(int)model.getValueAt(r,0);
        if(JOptionPane.showConfirmDialog(this,"Delete shop #"+id+"?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{PreparedStatement ps=DatabaseConnection.getConnection().prepareStatement("DELETE FROM Shops WHERE Shop_ID=?");
            ps.setInt(1,id);ps.executeUpdate();loadData();}catch(SQLException ex){showErr(ex);}
    }

    private void warn(String m){JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.WARNING_MESSAGE);}
    private void showErr(Exception e){JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
}