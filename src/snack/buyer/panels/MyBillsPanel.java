package snack.buyer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.Box;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


public class MyBillsPanel extends JPanel {

    private final int custId;
    private JTable table;
    private DefaultTableModel model;

    // Summary labels
    private JPanel summaryPanel;
    private JLabel lblGrandTotal;

    private static final String[] COLS = {
        "Bill ID", "Order ID", "Date", "Dealer", "Snack",
        "Qty", "Unit Price (₹)", "Subtotal (₹)", "GST 18% (₹)", "Total (₹)", "Payment"
    };

    public MyBillsPanel(int custId) {
        this.custId = custId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("🧾  My Bills",
            "All purchases across dealers — Pay All at once"), BorderLayout.NORTH);

        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        table.getColumnModel().getColumn(9).setCellRenderer(new TotalRenderer());
        table.getColumnModel().getColumn(10).setCellRenderer(new PaymentRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);
        add(sp, BorderLayout.CENTER);

        // ── Bottom: summary + buttons ─────────────────────────────────────────
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(UIConstants.WHITE);
        bottom.setBorder(new MatteBorder(2, 0, 0, 0, UIConstants.ACCENT));

        summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(UIConstants.WHITE);
        summaryPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        lblGrandTotal = new JLabel("Grand Total: ₹ 0.00");
        lblGrandTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblGrandTotal.setForeground(UIConstants.SUCCESS);

        JButton btnRefresh = UIConstants.btnRefresh();
        JButton btnPayAll  = UIConstants.makeButton("💳  Pay All & Generate Bill", UIConstants.SUCCESS);
        JButton btnHistory = UIConstants.makeButton("📜  Paid History", UIConstants.ACCENT);

        JPanel btnBar = UIConstants.makeButtonBar(btnRefresh, btnPayAll, btnHistory);
        btnBar.setBackground(UIConstants.WHITE);

        bottom.add(summaryPanel, BorderLayout.CENTER);
        bottom.add(btnBar, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadData());
        btnPayAll.addActionListener(e -> payAll());
        btnHistory.addActionListener(e -> showPaidHistory());
    }

    // ── Load UNPAID bills only ────────────────────────────────────────────────

    public void loadData() {
        model.setRowCount(0);
        summaryPanel.removeAll();

        Map<String, Double> dealerTotals = new LinkedHashMap<>();
        double grandSubtotal = 0;

        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT b.Bill_ID, b.Order_ID, b.Bill_date, " +
                "  sd.Dealer_Name, sn.Snack_Name, o.Order_Quantity, " +
                "  sn.Price, o.Total_price, b.Tax, b.Total_Amount " +
                "FROM Bill b " +
                "JOIN Orders      o  ON b.Order_ID  = o.Order_ID " +
                "JOIN Snacks      sn ON o.Snack_ID  = sn.Snack_ID " +
                "JOIN Shopkeepers sk ON o.S_ID      = sk.S_ID " +
                "JOIN Snack_Dealer sd ON sk.Dealer_ID = sd.Dealer_ID " +
                "WHERE b.cust_ID = ? AND (b.payment_mode IS NULL OR b.payment_mode = '') " +
                "ORDER BY sd.Dealer_Name, b.Bill_date DESC");
            ps.setInt(1, custId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dealer   = rs.getString("Dealer_Name");
                double subtotal = rs.getDouble("Total_price");
                double gst      = rs.getDouble("Tax");
                double total    = rs.getDouble("Total_Amount");

                model.addRow(new Object[]{
                    rs.getInt("Bill_ID"),
                    rs.getInt("Order_ID"),
                    rs.getString("Bill_date"),
                    dealer,
                    rs.getString("Snack_Name"),
                    rs.getInt("Order_Quantity"),
                    fmt(rs.getDouble("Price")),
                    fmt(subtotal),
                    fmt(gst),
                    fmt(total),
                    "Pending"
                });

                dealerTotals.merge(dealer, subtotal, Double::sum);
                grandSubtotal += subtotal;
            }
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
        }

        // Build summary labels
        for (Map.Entry<String, Double> e : dealerTotals.entrySet()) {
            JLabel l = new JLabel("🏢  " + e.getKey() + "  →  Subtotal: " + fmt(e.getValue()));
            l.setFont(UIConstants.LABEL_FONT);
            l.setForeground(UIConstants.PRIMARY);
            summaryPanel.add(l);
        }
        if (!dealerTotals.isEmpty()) {
            summaryPanel.add(Box.createVerticalStrut(6));
            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            summaryPanel.add(sep);
            summaryPanel.add(Box.createVerticalStrut(4));
        }
        double gstOnTotal = round(grandSubtotal * 0.18);
        double grandTotal = round(grandSubtotal + gstOnTotal);
        lblGrandTotal.setText("Grand Total (incl. GST 18%):  " + fmt(grandTotal) +
            "   [Subtotal: " + fmt(grandSubtotal) + "  +  GST: " + fmt(gstOnTotal) + "]");
        summaryPanel.add(lblGrandTotal);
        summaryPanel.revalidate();
        summaryPanel.repaint();
    }

    // ── Pay All ───────────────────────────────────────────────────────────────

    private void payAll() {
        if (model.getRowCount() == 0) {
            warn("No unpaid bills found.");
            return;
        }

        // 1. Payment mode
        String[] modes = {"💵  Cash", "📱  UPI", "💳  Card"};
        String modeChoice = (String) JOptionPane.showInputDialog(this,
            "<html><b>Select Payment Mode for all items:</b></html>",
            "Payment Method", JOptionPane.QUESTION_MESSAGE, null, modes, modes[0]);
        if (modeChoice == null) return;
        String modeLabel = modeChoice.replaceAll("[^a-zA-Z]", "").trim();

        // 2. Discount
        String discStr = JOptionPane.showInputDialog(this,
            "<html>Enter <b>Discount %</b> (0 for no discount):</html>",
            "Discount", JOptionPane.QUESTION_MESSAGE);
        if (discStr == null) return;
        double discPct = 0;
        try { discPct = Math.min(100, Math.max(0, Double.parseDouble(discStr.trim()))); }
        catch (NumberFormatException ignored) {}

        // 3. Collect all unpaid bill IDs + order IDs + subtotals
        java.util.List<int[]>    billOrders = new ArrayList<>();   // [billId, orderId]
        java.util.List<Double>   subtotals  = new ArrayList<>();
        double totalSubtotal = 0;
        for (int r = 0; r < model.getRowCount(); r++) {
            int billId  = (int) model.getValueAt(r, 0);
            int orderId = (int) model.getValueAt(r, 1);
            double sub  = parseAmt(model.getValueAt(r, 7).toString());
            billOrders.add(new int[]{billId, orderId});
            subtotals.add(sub);
            totalSubtotal += sub;
        }

        double totalDiscAmt = round(totalSubtotal * discPct / 100.0);
        double afterDisc    = round(totalSubtotal - totalDiscAmt);
        double totalGST     = round(afterDisc * 0.18);
        double grandTotal   = round(afterDisc + totalGST);

        // 4. Update DB — proportional discount per bill
        try {
            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < billOrders.size(); i++) {
                    int    billId  = billOrders.get(i)[0];
                    int    orderId = billOrders.get(i)[1];
                    double sub     = subtotals.get(i);
                    double share   = totalSubtotal > 0 ? sub / totalSubtotal : 1.0 / billOrders.size();
                    double disc    = round(totalDiscAmt * share);
                    double ad      = round(sub - disc);
                    double gst     = round(ad * 0.18);
                    double tot     = round(ad + gst);

                    PreparedStatement psB = conn.prepareStatement(
                        "UPDATE Bill SET payment_mode=?, discount=?, Tax=?, Total_Amount=? WHERE Bill_ID=?");
                    psB.setString(1, modeLabel);
                    psB.setDouble(2, disc);
                    psB.setDouble(3, gst);
                    psB.setDouble(4, tot);
                    psB.setInt(5, billId);
                    psB.executeUpdate();

                    PreparedStatement psO = conn.prepareStatement(
                        "UPDATE Orders SET status='Delivered' WHERE Order_ID=?");
                    psO.setInt(1, orderId);
                    psO.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
            return;
        }

        // 5. Build combined receipt grouped by dealer
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════╗\n");
        sb.append("║      SNACK DEALER — COMBINED RECEIPT     ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(String.format("║  Payment Mode : %-25s║%n", modeLabel));
        sb.append("╠══════════════════════════════════════════╣\n");

        // Group rows by dealer
        Map<String, java.util.List<Object[]>> grouped = new LinkedHashMap<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            String dealer = (String) model.getValueAt(r, 3);
            grouped.computeIfAbsent(dealer, k -> new ArrayList<>())
                   .add(new Object[]{
                       model.getValueAt(r, 4), // snack
                       model.getValueAt(r, 5), // qty
                       model.getValueAt(r, 6), // unit price
                       model.getValueAt(r, 7)  // subtotal
                   });
        }

        for (Map.Entry<String, java.util.List<Object[]>> entry : grouped.entrySet()) {
            sb.append(String.format("║  🏢 %-37s║%n", entry.getKey()));
            double dealerSub = 0;
            for (Object[] row : entry.getValue()) {
                sb.append(String.format("║    %-20s x%-3s @ %-8s║%n",
                    row[0].toString().substring(0, Math.min(row[0].toString().length(), 20)),
                    row[1], row[2]));
                dealerSub += parseAmt(row[3].toString());
            }
            sb.append(String.format("║    Dealer Subtotal : %-21s║%n", fmt(dealerSub)));
            sb.append("╠══════════════════════════════════════════╣\n");
        }

        sb.append(String.format("║  Overall Subtotal : %-22s║%n", fmt(totalSubtotal)));
        sb.append(String.format("║  Discount  (%4.1f%%): %-22s║%n", discPct, "- " + fmt(totalDiscAmt)));
        sb.append(String.format("║  After Discount   : %-22s║%n", fmt(afterDisc)));
        sb.append(String.format("║  GST (18%%)        : %-22s║%n", fmt(totalGST)));
        sb.append(String.format("║  ★ GRAND TOTAL    : %-22s║%n", fmt(grandTotal)));
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append("║  Status           : PAID ✅               ║\n");
        sb.append("╚══════════════════════════════════════════╝\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setBackground(new Color(250, 250, 250));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
            "Combined Bill — PAID", JOptionPane.PLAIN_MESSAGE);

        loadData(); // refresh (unpaid list clears, moves to history)
    }

    // ── Paid History ──────────────────────────────────────────────────────────

    private void showPaidHistory() {
        String[] histCols = {"Bill ID", "Date", "Dealer", "Snack", "Qty",
                             "Total (₹)", "Discount (₹)", "GST (₹)", "Grand (₹)", "Payment"};
        DefaultTableModel hm = new DefaultTableModel(histCols, 0);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT b.Bill_ID, b.Bill_date, sd.Dealer_Name, sn.Snack_Name, " +
                "  o.Order_Quantity, o.Total_price, " +
                "  COALESCE(b.discount,0), b.Tax, b.Total_Amount, b.payment_mode " +
                "FROM Bill b " +
                "JOIN Orders       o  ON b.Order_ID   = o.Order_ID " +
                "JOIN Snacks       sn ON o.Snack_ID   = sn.Snack_ID " +
                "JOIN Shopkeepers  sk ON o.S_ID        = sk.S_ID " +
                "JOIN Snack_Dealer sd ON sk.Dealer_ID  = sd.Dealer_ID " +
                "WHERE b.cust_ID = ? AND b.payment_mode IS NOT NULL AND b.payment_mode != '' " +
                "ORDER BY b.Bill_date DESC");
            ps.setInt(1, custId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                hm.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getInt(5),
                    fmt(rs.getDouble(6)), fmt(rs.getDouble(7)),
                    fmt(rs.getDouble(8)), fmt(rs.getDouble(9)),
                    rs.getString(10)
                });
            }
        } catch (SQLException ex) {
            err("DB Error: " + ex.getMessage());
            return;
        }

        JTable ht = UIConstants.makeTable(hm);
        JScrollPane sp = new JScrollPane(ht);
        sp.setPreferredSize(new Dimension(860, 320));
        JOptionPane.showMessageDialog(this, sp, "Paid Bill History", JOptionPane.PLAIN_MESSAGE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String fmt(double v)   { return String.format("₹ %.2f", v); }
    private double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private double parseAmt(String s) {
        return Double.parseDouble(s.replace("₹", "").replace(",", "").trim());
    }
    private void warn(String m) {
        JOptionPane.showMessageDialog(this, m, "Notice", JOptionPane.WARNING_MESSAGE);
    }
    private void err(String m) {
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Cell renderers ────────────────────────────────────────────────────────

    private static class TotalRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            if (!sel) { setBackground(new Color(232,245,232)); setForeground(new Color(27,127,50)); }
            setBorder(new EmptyBorder(0,8,0,8));
            return this;
        }
    }

    private static class PaymentRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (!sel) {
                String s = v == null ? "" : v.toString();
                if ("Pending".equals(s)) {
                    setBackground(new Color(255, 243, 200));
                    setForeground(new Color(180, 100, 0));
                } else {
                    setBackground(new Color(200, 240, 200));
                    setForeground(new Color(27, 127, 50));
                }
            }
            setBorder(new EmptyBorder(0,8,0,8));
            return this;
        }
    }
}