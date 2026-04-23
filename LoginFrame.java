package snack.auth;

import snack.db.DatabaseConnection;
import snack.dealer.DealerDashboard;
import snack.buyer.BuyerDashboard;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    private JTextField  tfUsername;
    private JPasswordField pfPassword;

    public LoginFrame() {
        super("Snack Dealer Management System — Login");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIConstants.BG);

        // ── Left branding panel ──────────────────────────────
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(UIConstants.PRIMARY);
        brand.setPreferredSize(new Dimension(320, 500));
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(UIConstants.PRIMARY);
        inner.setBorder(new EmptyBorder(0,30,0,30));

        JLabel logo = new JLabel("🍿");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
        logo.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = new JLabel("SnackDealer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Management System");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(UIConstants.LIGHT_ACCENT);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.ACCENT);
        sep.setMaximumSize(new Dimension(200, 2));

        JLabel desc = new JLabel("<html><center>Track orders, bills,<br>snacks & customers<br>all in one place.</center></html>");
        desc.setFont(UIConstants.LABEL_FONT);
        desc.setForeground(UIConstants.LIGHT_ACCENT);
        desc.setAlignmentX(CENTER_ALIGNMENT);

        inner.add(Box.createVerticalStrut(30));
        inner.add(logo);
        inner.add(Box.createVerticalStrut(16));
        inner.add(title);
        inner.add(sub);
        inner.add(Box.createVerticalStrut(24));
        inner.add(sep);
        inner.add(Box.createVerticalStrut(24));
        inner.add(desc);
        brand.add(inner);

        // ── Right login form ─────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIConstants.WHITE);
        form.setBorder(new EmptyBorder(0,40,0,40));
        form.setPreferredSize(new Dimension(360, 500));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8,0,8,0);
        gbc.gridx = 0; gbc.weightx = 1;

        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcome.setForeground(UIConstants.PRIMARY);
        gbc.gridy = 0; form.add(welcome, gbc);

        JLabel hint = new JLabel("Sign in to your account");
        hint.setFont(UIConstants.SMALL_FONT);
        hint.setForeground(UIConstants.MUTED);
        gbc.gridy = 1; form.add(hint, gbc);

        gbc.gridy = 2; form.add(styledLabel("Username"), gbc);
        tfUsername = UIConstants.makeField(20);
        gbc.gridy = 3; form.add(tfUsername, gbc);

        gbc.gridy = 4; form.add(styledLabel("Password"), gbc);
        pfPassword = new JPasswordField(20);
        pfPassword.setFont(UIConstants.LABEL_FONT);
        pfPassword.setBorder(new CompoundBorder(
                new LineBorder(UIConstants.LIGHT_ACCENT),
                new EmptyBorder(4,6,4,6)));
        gbc.gridy = 5; form.add(pfPassword, gbc);

        JButton btnLogin = UIConstants.makeButton("Login", UIConstants.PRIMARY);
        btnLogin.setPreferredSize(new Dimension(280,42));
        gbc.gridy = 6; gbc.insets = new Insets(18,0,8,0);
        form.add(btnLogin, gbc);

        gbc.insets = new Insets(8,0,8,0);
        JPanel signupRow = new JPanel(new FlowLayout(FlowLayout.CENTER,4,0));
        signupRow.setBackground(UIConstants.WHITE);
        JLabel noAcc = new JLabel("Don't have an account?");
        noAcc.setFont(UIConstants.SMALL_FONT);
        noAcc.setForeground(UIConstants.MUTED);
        JLabel signupLink = new JLabel("<html><u>Sign Up</u></html>");
        signupLink.setFont(UIConstants.SMALL_FONT);
        signupLink.setForeground(UIConstants.ACCENT);
        signupLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signupRow.add(noAcc);
        signupRow.add(signupLink);
        gbc.gridy = 7; form.add(signupRow, gbc);

        add(brand, BorderLayout.WEST);
        add(form,  BorderLayout.CENTER);
        pack();

        // ── Actions ──────────────────────────────────────────
        btnLogin.addActionListener(e -> doLogin());
        pfPassword.addActionListener(e -> doLogin());
        signupLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispose();
                new SignupFrame();
            }
        });
    }

    private void doLogin() {
        String user = tfUsername.getText().trim();
        String pass = new String(pfPassword.getPassword()).trim();
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            String sql = "SELECT user_id, role, ref_id FROM Users WHERE username=? AND password=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role  = rs.getString("role");
                int    refId = rs.getInt("ref_id");
                dispose();
                if ("dealer".equals(role)) new DealerDashboard(refId, user);
                else                       new BuyerDashboard(refId,  user);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Invalid username or password.", "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIConstants.TEXT);
        return l;
    }
}