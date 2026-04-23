package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class IndustriesPanel extends JPanel {

    private final int dealerId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {"L_ID","Name","Licence","Contact","Address"};

    public IndustriesPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Industries","Manufacturing partners & licenced producers"), BorderLayout.NORTH);
        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);

        JButton btnAdd=UIConstants.btnAdd(),btnEdit=UIConstants.btnEdit(),
                btnDel=UIConstants.btnDelete(),btnRefresh=UIConstants.btnRefresh();
        add(UIConstants.makeButtonBar(btnAdd,btnEdit,btnDel,btnRefresh),BorderLayout.SOUTH);
        add(sp,BorderLayout.CENTER);

        btnRefresh.addActionListener(e->loadData());
        btnAdd.addActionListener(e->showDialog(null));
        btnEdit.addActionListener(e->{int r=table.getSelectedRow();if(r<0){warn("Select a row.");return;}showDialog(r);});
        btnDel.addActionListener(e->deleteSelected());
    }

    private void loadData() {
        model.setRowCount(0);
        try{
            ResultSet rs=DatabaseConnection.getConnection().createStatement()
                .executeQuery("SELECT L_ID,I_Name,I_Licence,I_contact_no,I_address FROM Industries ORDER BY L_ID");
            while(rs.next())
                model.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)});
        }catch(SQLException ex){showErr(ex);}
    }

    private void showDialog(Integer row){
        boolean editing=row!=null;int lId=editing?(int)model.getValueAt(row,0):-1;
        JTextField tfName=UIConstants.makeField(20),tfLic=UIConstants.makeField(20),
                   tfCno=UIConstants.makeField(20),tfAddr=UIConstants.makeField(20);
        if(editing){tfName.setText((String)model.getValueAt(row,1));tfLic.setText((String)model.getValueAt(row,2));
            tfCno.setText((String)model.getValueAt(row,3));tfAddr.setText((String)model.getValueAt(row,4));}

        JDialog dlg=UIConstants.makeDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            editing?"Edit Industry":"Add Industry",420,300);
        JPanel form=UIConstants.formGrid(
            new String[]{"Name *","Licence No","Contact No","Address"},
            new JComponent[]{tfName,tfLic,tfCno,tfAddr});
        JButton save=UIConstants.btnSave(),cancel=UIConstants.btnCancel();
        dlg.setLayout(new BorderLayout());
        dlg.add(form,BorderLayout.CENTER);
        dlg.add(UIConstants.makeButtonBar(save,cancel),BorderLayout.SOUTH);

        save.addActionListener(e->{
            if(tfName.getText().trim().isEmpty()){warn("Name required.");return;}
            try{
                Connection con=DatabaseConnection.getConnection();
                if(editing){PreparedStatement ps=con.prepareStatement(
                    "UPDATE Industries SET I_Name=?,I_Licence=?,I_contact_no=?,I_address=? WHERE L_ID=?");
                    ps.setString(1,tfName.getText().trim());ps.setString(2,tfLic.getText().trim());
                    ps.setString(3,tfCno.getText().trim());ps.setString(4,tfAddr.getText().trim());
                    ps.setInt(5,lId);ps.executeUpdate();
                }else{PreparedStatement ps=con.prepareStatement(
                    "INSERT INTO Industries(I_Name,I_Licence,I_contact_no,I_address) VALUES(?,?,?,?)");
                    ps.setString(1,tfName.getText().trim());ps.setString(2,tfLic.getText().trim());
                    ps.setString(3,tfCno.getText().trim());ps.setString(4,tfAddr.getText().trim());
                    ps.executeUpdate();}
                dlg.dispose();loadData();
            }catch(SQLException ex){showErr(ex);}
        });
        cancel.addActionListener(e->dlg.dispose());
        dlg.setVisible(true);
    }

    private void deleteSelected(){
        int r=table.getSelectedRow();if(r<0){warn("Select a row.");return;}
        int id=(int)model.getValueAt(r,0);
        if(JOptionPane.showConfirmDialog(this,"Delete industry #"+id+"?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{PreparedStatement ps=DatabaseConnection.getConnection().prepareStatement("DELETE FROM Industries WHERE L_ID=?");
            ps.setInt(1,id);ps.executeUpdate();loadData();}catch(SQLException ex){showErr(ex);}
    }

    private void warn(String m){JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.WARNING_MESSAGE);}
    private void showErr(Exception e){JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
}