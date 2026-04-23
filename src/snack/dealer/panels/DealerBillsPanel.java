package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class DealerBillsPanel extends JPanel {

    private final int dealerId;
    private JTable table;
    private DefaultTableModel model;

    private static final String[] COLS = {
        "Bill ID","Order ID","Bill Date","Customer","Snack","Qty","Subtotal (₹)","Tax (₹)","Total (₹)","Status"
    };

    public DealerBillsPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Bills",
            "Bills auto-generated from orders — visible to customers"), BorderLayout.NORTH);
        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);

        // Read-only view; only allow Refresh and View Detail
        JButton btnRefresh = UIConstants.btnRefresh();
        JButton btnDetail  = UIConstants.makeButton("📄 View Detail", UIConstants.ACCENT);
        add(UIConstants.makeButtonBar(btnRefresh, btnDetail), BorderLayout.SOUTH);
        add(sp, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadData());
        btnDetail.addActionListener(e -> viewDetail());
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT b.Bill_ID, b.Order_ID, b.Bill_date, " +
                "  CONCAT(c.F_name,' ',COALESCE(c.L_name,'')) AS cname, " +
                "  sn.Snack_Name, o.Order_Quantity, " +
                "  o.Total_price, b.Tax, b.Total_Amount, o.status " +
                "FROM Bill b " +
                "JOIN Orders      o  ON b.Order_ID  = o.Order_ID " +
                "JOIN Customers   c  ON b.cust_ID   = c.cust_ID " +
                "JOIN Snacks      sn ON o.Snack_ID  = sn.Snack_ID " +
                "JOIN Shopkeepers sk ON o.S_ID      = sk.S_ID " +
                "WHERE sk.Dealer_ID = ? " +
                "ORDER BY b.Bill_date DESC, b.Bill_ID DESC");
            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getInt(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getInt(6),
                    String.format("₹ %.2f", rs.getDouble(7)),
                    String.format("₹ %.2f", rs.getDouble(8)),
                    String.format("₹ %.2f", rs.getDouble(9)),
                    rs.getString(10)
                });
            }
        } catch (SQLException ex) { showErr(ex); }
    }

    private void viewDetail() {
        int r = table.getSelectedRow();
        if (r < 0) { warn("Select a bill."); return; }
        int billId  = (int) model.getValueAt(r, 0);
        int orderId = (int) model.getValueAt(r, 1);
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("        SNACK DEALER BILL\n");
        sb.append("═══════════════════════════════\n");
        for (int col = 0; col < COLS.length; col++) {
            sb.append(String.format("%-14s : %s%n", COLS[col], model.getValueAt(r, col)));
        }
        sb.append("───────────────────────────────\n");
        sb.append("  Tax Rate : 18% GST\n");
        sb.append("═══════════════════════════════\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        ta.setEditable(false);
        ta.setBackground(UIConstants.BG);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
            "Bill #" + billId + " — Order #" + orderId,
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String m)  { JOptionPane.showMessageDialog(this,m,"Info",JOptionPane.WARNING_MESSAGE); }
    private void showErr(Exception e) { JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
}