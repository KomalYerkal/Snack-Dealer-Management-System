package snack.buyer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class MyOrdersPanel extends JPanel {

    private final int custId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {
        "Order ID","Date","Snack","Qty","Total (₹)","Shopkeeper","Status"
    };

    public MyOrdersPanel(int custId) {
        this.custId = custId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("My Orders",
            "Orders placed on your behalf by your dealer"), BorderLayout.NORTH);

        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        // Colour-code status column
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);

        JButton btnRefresh = UIConstants.btnRefresh();
        JButton btnDetail  = UIConstants.makeButton("📄 Details", UIConstants.ACCENT);
        add(UIConstants.makeButtonBar(btnRefresh, btnDetail), BorderLayout.SOUTH);
        add(sp, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadData());
        btnDetail.addActionListener(e -> showDetail());
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT o.Order_ID, o.Order_date, sn.Snack_Name, " +
                "  o.Order_Quantity, o.Total_price, sk.S_Name, o.status " +
                "FROM Orders o " +
                "JOIN Snacks      sn ON o.Snack_ID = sn.Snack_ID " +
                "JOIN Shopkeepers sk ON o.S_ID      = sk.S_ID " +
                "WHERE o.cust_ID=? ORDER BY o.Order_date DESC");
            ps.setInt(1, custId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getInt(4), String.format("₹ %.2f", rs.getDouble(5)),
                    rs.getString(6), rs.getString(7)
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,"DB Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDetail() {
        int r = table.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this,"Select an order.","Info",JOptionPane.WARNING_MESSAGE); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════\n");
        sb.append("       ORDER DETAILS\n");
        sb.append("═══════════════════════════\n");
        for (int c = 0; c < COLS.length; c++)
            sb.append(String.format("%-14s: %s%n", COLS[c], model.getValueAt(r,c)));
        sb.append("───────────────────────────\n");
        sb.append("Bill is available in My Bills\n");
        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced",Font.PLAIN,13)); ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
            "Order #"+model.getValueAt(r,0), JOptionPane.INFORMATION_MESSAGE);
    }

    /** Colours the Status cell. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t,v,sel,foc,row,col);
            String s = v == null ? "" : v.toString();
            if (!sel) {
                switch (s) {
                    case "Delivered"  -> setBackground(new Color(200,240,200));
                    case "Pending"    -> setBackground(new Color(255,243,200));
                    case "Confirmed"  -> setBackground(new Color(200,220,255));
                    case "Cancelled"  -> setBackground(new Color(255,200,200));
                    default           -> setBackground(UIConstants.WHITE);
                }
            }
            setBorder(new EmptyBorder(0,8,0,8));
            return this;
        }
    }
}