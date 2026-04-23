package snack.dealer;

import snack.auth.LoginFrame;
import snack.dealer.panels.*;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main window shown after a dealer logs in.
 * Sidebar nav → swaps content panel on the right.
 */
public class DealerDashboard extends JFrame {

    private final int dealerId;
    private final String username;

    private JPanel contentArea;
    private CardLayout cards;

    // Menu item labels → panel IDs
    private static final String[][] MENU = {
        {"🏠", "Dashboard",    "dashboard"},
        {"👥", "Customers",    "customers"},
        {"🏪", "Shopkeepers",  "shopkeepers"},
        {"🛒", "Snacks",       "snacks"},
        {"🏬", "Shops",        "shops"},
        {"🏭", "Industries",   "industries"},
        {"📦", "Orders",       "orders"},
        {"🧾", "Bills",        "bills"},
        {"📊", "Reports",      "reports"},
    };

    public DealerDashboard(int dealerId, String username) {
        super("SnackDealer — Dealer Panel  [" + username + "]");
        this.dealerId = dealerId;
        this.username = username;
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1100, 680));
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // ── Top bar ────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIConstants.PRIMARY);
        topBar.setBorder(new EmptyBorder(10,20,10,20));
        topBar.setPreferredSize(new Dimension(0, 56));

        JLabel appName = new JLabel("🍿 SnackDealer Management");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appName.setForeground(UIConstants.WHITE);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        topRight.setBackground(UIConstants.PRIMARY);
        JLabel userLbl = new JLabel("👤 " + username + "  |  Dealer");
        userLbl.setFont(UIConstants.SMALL_FONT);
        userLbl.setForeground(UIConstants.LIGHT_ACCENT);
        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(UIConstants.SMALL_FONT);
        btnLogout.setForeground(UIConstants.WHITE);
        btnLogout.setBackground(UIConstants.DANGER);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> { dispose(); new LoginFrame(); });
        topRight.add(userLbl); topRight.add(btnLogout);

        topBar.add(appName, BorderLayout.WEST);
        topBar.add(topRight, BorderLayout.EAST);

        // ── Sidebar ────────────────────────────────────────────
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIConstants.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new EmptyBorder(12,0,12,0));

        ButtonGroup bg = new ButtonGroup();
        JToggleButton[] menuBtns = new JToggleButton[MENU.length];

        for (int i = 0; i < MENU.length; i++) {
            final String panelId = MENU[i][2];
            JToggleButton btn = makeSidebarBtn(MENU[i][0] + "  " + MENU[i][1]);
            menuBtns[i] = btn;
            bg.add(btn);
            sidebar.add(btn);
            if (i == 0) { btn.setSelected(true); }
            btn.addActionListener(e -> cards.show(contentArea, panelId));
        }

        // ── Content area ───────────────────────────────────────
        cards = new CardLayout();
        contentArea = new JPanel(cards);
        contentArea.setBackground(UIConstants.BG);

        contentArea.add(makeDashboard(),                         "dashboard");
        contentArea.add(new CustomersPanel(dealerId),            "customers");
        contentArea.add(new ShopkeepersPanel(dealerId),          "shopkeepers");
        contentArea.add(new SnacksPanel(dealerId),               "snacks");
        contentArea.add(new ShopsPanel(dealerId),                "shops");
        contentArea.add(new IndustriesPanel(dealerId),           "industries");
        contentArea.add(new DealerOrdersPanel(dealerId),         "orders");
        contentArea.add(new DealerBillsPanel(dealerId),          "bills");
        contentArea.add(new ReportsPanel(dealerId),              "reports");

        cards.show(contentArea, "dashboard");

        add(topBar,    BorderLayout.NORTH);
        add(sidebar,   BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);
    }

    // ── Home / Dashboard panel ─────────────────────────────────
    private JPanel makeDashboard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIConstants.BG);
        p.add(UIConstants.makeTitlePanel(
            "Dealer Dashboard",
            "Overview of your snack business"), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(3, 3, 16, 16));
        cards.setBackground(UIConstants.BG);
        cards.setBorder(new EmptyBorder(24,24,24,24));

        String[][] stats = {
            {"👥 Customers",    "SELECT COUNT(*) FROM Customers c JOIN Shops s ON c.Shop_ID=s.Shop_ID JOIN Shopkeepers sk ON s.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
            {"🏪 Shopkeepers",  "SELECT COUNT(*) FROM Shopkeepers WHERE Dealer_ID="+dealerId},
            {"🏬 Shops",        "SELECT COUNT(*) FROM Shops s JOIN Shopkeepers sk ON s.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
            {"🍿 Snacks",       "SELECT COUNT(*) FROM Snacks"},
            {"🏭 Industries",   "SELECT COUNT(*) FROM Industries"},
            {"📦 Total Orders", "SELECT COUNT(*) FROM Orders o JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
            {"🔔 Pending",      "SELECT COUNT(*) FROM Orders o JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId+" AND o.status='Pending'"},
            {"✅ Delivered",    "SELECT COUNT(*) FROM Orders o JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId+" AND o.status='Delivered'"},
            {"💰 Revenue (₹)",  "SELECT COALESCE(SUM(b.Total_Amount),0) FROM Bill b JOIN Orders o ON b.Order_ID=o.Order_ID JOIN Shopkeepers sk ON o.S_ID=sk.S_ID WHERE sk.Dealer_ID="+dealerId},
        };
        Color[] colours = {
            UIConstants.ACCENT, new Color(2,119,189), new Color(0,131,143),
            new Color(56,142,60), new Color(123,31,162), new Color(230,119,0),
            UIConstants.DANGER, UIConstants.SUCCESS, new Color(21,101,192),
        };

        for (int i = 0; i < stats.length; i++) {
            String val = "—";
            try {
                java.sql.Statement st = snack.db.DatabaseConnection.getConnection().createStatement();
                java.sql.ResultSet rs = st.executeQuery(stats[i][1]);
                if (rs.next()) val = rs.getString(1);
            } catch (Exception ex) { val = "err"; }
            cards.add(statCard(stats[i][0], val, colours[i]));
        }
        p.add(cards, BorderLayout.CENTER);
        return p;
    }

    private JPanel statCard(String label, String value, Color color) {
        JPanel c = new JPanel(new GridBagLayout());
        c.setBackground(UIConstants.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color, 2, true),
            new EmptyBorder(18,18,18,18)));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0; g.gridy=0; g.anchor=GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.LABEL_FONT);
        lbl.setForeground(UIConstants.MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 30));
        val.setForeground(color);
        c.add(lbl); g.gridy=1; c.add(val, g);
        return c;
    }

    private JToggleButton makeSidebarBtn(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setForeground(UIConstants.LIGHT_ACCENT);
        b.setBackground(UIConstants.SIDEBAR_BG);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(12, 20, 12, 20));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.addChangeListener(e -> {
            if (b.isSelected()) {
                b.setBackground(UIConstants.SIDEBAR_SEL);
                b.setForeground(UIConstants.WHITE);
            } else {
                b.setBackground(UIConstants.SIDEBAR_BG);
                b.setForeground(UIConstants.LIGHT_ACCENT);
            }
        });
        return b;
    }
}