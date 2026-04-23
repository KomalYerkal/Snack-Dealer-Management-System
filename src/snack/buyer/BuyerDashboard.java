package snack.buyer;

import snack.auth.LoginFrame;
import snack.buyer.panels.*;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Dashboard shown to a logged-in buyer (customer).
 */
public class BuyerDashboard extends JFrame {

    private final int custId;
    private final String username;

    public BuyerDashboard(int custId, String username) {
        super("SnackDealer — Buyer Panel  [" + username + "]");
        this.custId = custId;
        this.username = username;
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900, 620));
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // ── Top bar ────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(13, 71, 161)); // darker buyer blue
        topBar.setBorder(new EmptyBorder(10, 20, 10, 20));
        topBar.setPreferredSize(new Dimension(0, 56));

        JLabel appName = new JLabel("🛒 SnackDealer — Buyer Portal");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appName.setForeground(UIConstants.WHITE);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topRight.setBackground(new Color(13, 71, 161));
        JLabel userLbl = new JLabel("👤 " + username + "  |  Buyer  #" + custId);
        userLbl.setFont(UIConstants.SMALL_FONT);
        userLbl.setForeground(UIConstants.LIGHT_ACCENT);
        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(UIConstants.SMALL_FONT);
        btnLogout.setForeground(UIConstants.WHITE);
        btnLogout.setBackground(UIConstants.DANGER);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });
        topRight.add(userLbl);
        topRight.add(btnLogout);
        topBar.add(appName, BorderLayout.WEST);
        topBar.add(topRight, BorderLayout.EAST);

        // ── Tabbed content ─────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.setFont(UIConstants.LABEL_FONT);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        tabs.addTab("🏠  Home", homePanel());
        tabs.addTab("🛍  Shop & Order", new ShopOrderPanel(custId));
        tabs.addTab("🧾  My Bills", new MyBillsPanel(custId));

        add(topBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel homePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIConstants.BG);
        p.add(UIConstants.makeTitlePanel("Welcome, " + username + "!",
                "Browse snacks, track your orders and view bills"), BorderLayout.NORTH);

        // Summary cards
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 16));
        cards.setBackground(UIConstants.BG);
        cards.setBorder(new EmptyBorder(24, 24, 24, 24));

        String[][] stats = {
                { "📦 My Orders", "SELECT COUNT(*) FROM Orders WHERE cust_ID=" + custId },
                { "🧾 My Bills", "SELECT COUNT(*) FROM Bill WHERE cust_ID=" + custId },
                { "💰 Total Spent (₹)", "SELECT COALESCE(SUM(Total_Amount),0) FROM Bill WHERE cust_ID=" + custId },
        };
        Color[] colours = { UIConstants.ACCENT, new Color(0, 131, 143), UIConstants.SUCCESS };
        for (int i = 0; i < stats.length; i++) {
            String val = "—";
            try {
                java.sql.ResultSet rs = snack.db.DatabaseConnection.getConnection()
                        .createStatement().executeQuery(stats[i][1]);
                if (rs.next())
                    val = rs.getString(1);
                if (val != null && val.contains("."))
                    val = String.format("₹ %.2f", Double.parseDouble(val));
            } catch (Exception ignored) {
            }
            cards.add(buyerCard(stats[i][0], val, colours[i]));
        }
        p.add(cards, BorderLayout.CENTER);

        JLabel tip = new JLabel("<html><center>💡 Tip: Use <b>Browse Snacks</b> to see available items.<br>" +
                "Your dealer creates orders on your behalf — they appear in <b>My Orders</b> automatically.</center></html>");
        tip.setFont(UIConstants.SMALL_FONT);
        tip.setHorizontalAlignment(SwingConstants.CENTER);
        tip.setForeground(UIConstants.MUTED);
        tip.setBorder(new EmptyBorder(0, 0, 20, 0));
        p.add(tip, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buyerCard(String label, String val, Color c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIConstants.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(c, 2, true), new EmptyBorder(18, 18, 18, 18)));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(label);
        l.setFont(UIConstants.LABEL_FONT);
        l.setForeground(UIConstants.MUTED);
        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 30));
        v.setForeground(c);
        p.add(l, g);
        g.gridy = 1;
        p.add(v, g);
        return p;
    }
}