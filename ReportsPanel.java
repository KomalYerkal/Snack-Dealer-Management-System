package snack.dealer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class ReportsPanel extends JPanel {

    private final int dealerId;

    public ReportsPanel(int dealerId) {
        this.dealerId = dealerId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Reports & Analytics",
            "Business insights for your dealership"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIConstants.LABEL_FONT);
        tabs.setBackground(UIConstants.BG);

        tabs.addTab("📊 Revenue Summary",    revenuePanel());
        tabs.addTab("🍿 Top Snacks",         topSnacksPanel());
        tabs.addTab("📦 Orders by Status",   orderStatusPanel());
        tabs.addTab("👥 Customer Activity",  customerActivityPanel());
        tabs.addTab("📅 Monthly Revenue",    monthlyPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // ── 1. Revenue Summary ───────────────────────────────────
    private JPanel revenuePanel() {
        JPanel p = new JPanel(new GridLayout(2,3,16,16));
        p.setBackground(UIConstants.BG);
        p.setBorder(new EmptyBorder(20,20,20,20));

        String[][] metrics = {
            {"Total Revenue (₹)",
             "SELECT COALESCE(SUM(b.Total_Amount),0) FROM Bill b " +
             "JOIN Orders o ON b.Order_ID=o.Order_ID JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
            {"Total Tax Collected (₹)",
             "SELECT COALESCE(SUM(b.Tax),0) FROM Bill b " +
             "JOIN Orders o ON b.Order_ID=o.Order_ID JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
            {"Avg Order Value (₹)",
             "SELECT COALESCE(AVG(o.Total_price),0) FROM Orders o " +
             "JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
            {"Total Orders",
             "SELECT COUNT(*) FROM Orders o JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
            {"Delivered Orders",
             "SELECT COUNT(*) FROM Orders o JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId+" AND o.status='Delivered'"},
            {"Pending Orders",
             "SELECT COUNT(*) FROM Orders o JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId+" AND o.status='Pending'"},
        };
        Color[] cols = {UIConstants.SUCCESS, new Color(0,131,143), new Color(123,31,162),
                        UIConstants.ACCENT, new Color(56,142,60), UIConstants.DANGER};

        for (int i = 0; i < metrics.length; i++) {
            String val = "—";
            try {
                ResultSet rs = DatabaseConnection.getConnection().createStatement()
                    .executeQuery(metrics[i][1]);
                if (rs.next()) val = rs.getString(1);
                if (val != null && val.contains(".")) val = String.format("%.2f", Double.parseDouble(val));
            } catch (Exception ignored) {}
            p.add(metricCard(metrics[i][0], val, cols[i]));
        }
        return p;
    }

    // ── 2. Top Snacks by Revenue ─────────────────────────────
    private JPanel topSnacksPanel() {
        String[] cols = {"Snack","Orders","Qty Sold","Revenue (₹)"};
        DefaultTableModel tm = new DefaultTableModel(cols, 0);
        JTable t = UIConstants.makeTable(tm);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT sn.Snack_Name, COUNT(o.Order_ID), SUM(o.Order_Quantity), " +
                "  SUM(o.Total_price) " +
                "FROM Orders o " +
                "JOIN Snacks sn      ON o.Snack_ID = sn.Snack_ID " +
                "JOIN Shopkeepers sk ON o.S_ID     = sk.S_ID " +
                "WHERE sk.Dealer_ID=? " +
                "GROUP BY sn.Snack_ID, sn.Snack_Name " +
                "ORDER BY SUM(o.Total_price) DESC LIMIT 10");
            ps.setInt(1,dealerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                tm.addRow(new Object[]{rs.getString(1),rs.getInt(2),rs.getInt(3),
                    String.format("₹ %.2f",rs.getDouble(4))});
        } catch (SQLException ex) { /* skip */ }
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(UIConstants.BG);
        p.setBorder(new EmptyBorder(16,16,16,16));
        p.add(new JScrollPane(t));
        return p;
    }

    // ── 3. Orders by Status ──────────────────────────────────
    private JPanel orderStatusPanel() {
        String[] cols = {"Status","Count","Total Revenue (₹)"};
        DefaultTableModel tm = new DefaultTableModel(cols,0);
        JTable t = UIConstants.makeTable(tm);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT o.status, COUNT(*), COALESCE(SUM(o.Total_price),0) " +
                "FROM Orders o JOIN Shopkeepers sk ON o.S_ID=sk.S_ID " +
                "WHERE sk.Dealer_ID=? GROUP BY o.status ORDER BY COUNT(*) DESC");
            ps.setInt(1,dealerId);
            ResultSet rs=ps.executeQuery();
            while(rs.next())
                tm.addRow(new Object[]{rs.getString(1),rs.getInt(2),
                    String.format("₹ %.2f",rs.getDouble(3))});
        }catch(SQLException ex){}
        // Simple bar chart panel below the table
        JPanel bar = buildBarChart(tm);
        JPanel p = new JPanel(new BorderLayout(0,12)); p.setBackground(UIConstants.BG);
        p.setBorder(new EmptyBorder(16,16,16,16));
        p.add(new JScrollPane(t), BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        return p;
    }

    // ── 4. Customer Activity ─────────────────────────────────
    private JPanel customerActivityPanel() {
        String[] cols = {"Customer","Orders","Total Spent (₹)","Last Order"};
        DefaultTableModel tm = new DefaultTableModel(cols,0);
        JTable t = UIConstants.makeTable(tm);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT CONCAT(c.F_name,' ',COALESCE(c.L_name,'')) AS name, " +
                "  COUNT(o.Order_ID), COALESCE(SUM(b.Total_Amount),0), MAX(o.Order_date) " +
                "FROM Customers c " +
                "LEFT JOIN Orders o ON c.cust_ID=o.cust_ID " +
                "LEFT JOIN Bill   b ON o.Order_ID=b.Order_ID " +
                "JOIN Shops sh ON c.Shop_ID=sh.Shop_ID " +
                "JOIN Shopkeepers sk ON sh.S_ID=sk.S_ID " +
                "WHERE sk.Dealer_ID=? " +
                "GROUP BY c.cust_ID ORDER BY SUM(b.Total_Amount) DESC");
            ps.setInt(1,dealerId);
            ResultSet rs=ps.executeQuery();
            while(rs.next())
                tm.addRow(new Object[]{rs.getString(1),rs.getInt(2),
                    String.format("₹ %.2f",rs.getDouble(3)),rs.getString(4)});
        }catch(SQLException ex){}
        JPanel p=new JPanel(new BorderLayout());p.setBackground(UIConstants.BG);
        p.setBorder(new EmptyBorder(16,16,16,16));p.add(new JScrollPane(t));return p;
    }

    // ── 5. Monthly Revenue ───────────────────────────────────
    private JPanel monthlyPanel() {
        String[] cols = {"Month","Orders","Revenue (₹)","Tax (₹)"};
        DefaultTableModel tm = new DefaultTableModel(cols,0);
        JTable t = UIConstants.makeTable(tm);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT DATE_FORMAT(o.Order_date,'%Y-%m') AS mon, " +
                "  COUNT(o.Order_ID), SUM(o.Total_price), SUM(b.Tax) " +
                "FROM Orders o " +
                "JOIN Bill b ON o.Order_ID=b.Order_ID " +
                "JOIN Shopkeepers sk ON o.S_ID=sk.S_ID " +
                "WHERE sk.Dealer_ID=? " +
                "GROUP BY mon ORDER BY mon DESC");
            ps.setInt(1,dealerId);
            ResultSet rs=ps.executeQuery();
            while(rs.next())
                tm.addRow(new Object[]{rs.getString(1),rs.getInt(2),
                    String.format("₹ %.2f",rs.getDouble(3)),
                    String.format("₹ %.2f",rs.getDouble(4))});
        }catch(SQLException ex){}
        JPanel chart = buildMonthlyChart(tm);
        JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(UIConstants.BG);
        p.setBorder(new EmptyBorder(16,16,16,16));
        p.add(new JScrollPane(t),BorderLayout.NORTH);
        p.add(chart,BorderLayout.CENTER);
        return p;
    }

    // ── Helpers: simple bar charts ───────────────────────────
    private JPanel buildBarChart(DefaultTableModel tm) {
        return new JPanel() {
            {setBackground(UIConstants.WHITE);setPreferredSize(new Dimension(0,160));
             setBorder(new TitledBorder("Orders by Status"));}
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(tm.getRowCount()==0)return;
                Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int n=tm.getRowCount(),w=getWidth()-60,h=getHeight()-40,barW=Math.min(80,w/n-10);
                int maxVal=1;
                for(int i=0;i<n;i++)try{maxVal=Math.max(maxVal,Integer.parseInt(tm.getValueAt(i,1).toString()));}catch(Exception ignored){}
                Color[]bc={UIConstants.ACCENT,UIConstants.SUCCESS,UIConstants.WARNING,UIConstants.DANGER};
                for(int i=0;i<n;i++){
                    int bh=(int)((double)Integer.parseInt(tm.getValueAt(i,1).toString())/maxVal*(h-20));
                    int x=30+i*(barW+10),y=h-bh;
                    g2.setColor(bc[i%bc.length]);g2.fillRoundRect(x,y,barW,bh,6,6);
                    g2.setColor(UIConstants.TEXT);g2.setFont(UIConstants.SMALL_FONT);
                    g2.drawString(tm.getValueAt(i,0).toString(),x,h+14);
                    g2.drawString(tm.getValueAt(i,1).toString(),x+barW/2-6,y-4);
                }
            }
        };
    }

    private JPanel buildMonthlyChart(DefaultTableModel tm) {
        return new JPanel() {
            {setBackground(UIConstants.WHITE);setPreferredSize(new Dimension(0,200));
             setBorder(new TitledBorder("Monthly Revenue (₹)"));}
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(tm.getRowCount()==0)return;
                Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int n=tm.getRowCount(),w=getWidth()-80,h=getHeight()-50;
                double maxRev=0.01;
                for(int i=0;i<n;i++)try{maxRev=Math.max(maxRev,Double.parseDouble(tm.getValueAt(i,2).toString().replace("₹ ","")));}catch(Exception ignored){}
                int barW=Math.min(60,w/n-8);
                for(int i=0;i<n;i++){
                    try{
                        double rev=Double.parseDouble(tm.getValueAt(i,2).toString().replace("₹ ",""));
                        int bh=(int)(rev/maxRev*(h-30));
                        int x=40+i*(barW+8),y=h-bh;
                        g2.setColor(UIConstants.ACCENT);g2.fillRoundRect(x,y,barW,bh,6,6);
                        g2.setColor(UIConstants.TEXT);g2.setFont(UIConstants.SMALL_FONT);
                        String mon=tm.getValueAt(i,0).toString();
                        g2.drawString(mon.length()>7?mon.substring(5):mon,x,h+14);
                        g2.drawString("₹"+(int)rev,x,y-4);
                    }catch(Exception ignored){}
                }
            }
        };
    }

    private JPanel metricCard(String label, String val, Color c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIConstants.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(c,2,true), new EmptyBorder(14,14,14,14)));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0;g.gridy=0;g.anchor=GridBagConstraints.WEST;
        JLabel l=new JLabel(label);l.setFont(UIConstants.SMALL_FONT);l.setForeground(UIConstants.MUTED);
        JLabel v=new JLabel(val);v.setFont(new Font("Segoe UI",Font.BOLD,26));v.setForeground(c);
        p.add(l,g);g.gridy=1;p.add(v,g);return p;
    }
}