package snack.auth;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class SignupFrame extends JFrame {

    private JTextField  tfUsername, tfExtra1, tfExtra2, tfExtra3;
    private JPasswordField pfPass, pfConfirm;
    private JComboBox<String> cbRole;
    private JLabel lblExtra1, lblExtra2, lblExtra3;

    public SignupFrame() {
        super("Snack Dealer Management System — Sign Up");
        buildUI();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.WHITE);

        // Top header bar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(new EmptyBorder(20,30,20,30));
        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UIConstants.WHITE);
        JLabel sub = new JLabel("Join the Snack Dealer platform");
        sub.setFont(UIConstants.SMALL_FONT);
        sub.setForeground(UIConstants.LIGHT_ACCENT);
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);

        // Form area
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIConstants.WHITE);
        form.setBorder(new EmptyBorder(20,40,20,40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(7,0,7,0);
        gc.gridx = 0; gc.weightx = 1;

        tfUsername = UIConstants.makeField(24);
        pfPass     = styledPwd();
        pfConfirm  = styledPwd();
        cbRole     = new JComboBox<>(new String[]{"buyer","dealer"});
        cbRole.setFont(UIConstants.LABEL_FONT);
        tfExtra1   = UIConstants.makeField(24);
        tfExtra2   = UIConstants.makeField(24);
        tfExtra3   = UIConstants.makeField(24);

        lblExtra1 = bold("Name");
        lblExtra2 = bold("Contact No");
        lblExtra3 = bold("Address / Email");

        int row = 0;
        addRow(form, gc, row++, bold("Username"),      tfUsername);
        addRow(form, gc, row++, bold("Password"),      pfPass);
        addRow(form, gc, row++, bold("Confirm Pass"),  pfConfirm);
        addRow(form, gc, row++, bold("Register As"),   cbRole);
        addRow(form, gc, row++, lblExtra1,  tfExtra1);
        addRow(form, gc, row++, lblExtra2,  tfExtra2);
        addRow(form, gc, row++, lblExtra3,  tfExtra3);

        // Buttons
        JButton btnRegister = UIConstants.makeButton("Register", UIConstants.PRIMARY);
        JButton btnBack     = UIConstants.makeButton("← Back to Login", UIConstants.MUTED);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER,12,0));
        btns.setBackground(UIConstants.WHITE);
        btns.setBorder(new EmptyBorder(8,0,0,0));
        btns.add(btnBack); btns.add(btnRegister);
        gc.gridy = row; gc.insets = new Insets(14,0,0,0);
        form.add(btns, gc);

        root.add(header, BorderLayout.NORTH);
        root.add(form,   BorderLayout.CENTER);
        add(root);
        pack();

        // Update extra labels when role changes
        cbRole.addActionListener(e -> updateLabels());
        updateLabels();

        btnRegister.addActionListener(e -> doRegister());
        btnBack.addActionListener(e -> { dispose(); new LoginFrame(); });
    }

    private void updateLabels() {
        boolean isDealer = "dealer".equals(cbRole.getSelectedItem());
        lblExtra1.setText(isDealer ? "Dealer Name" : "First Name");
        lblExtra2.setText(isDealer ? "Contact No"  : "Contact No");
        lblExtra3.setText(isDealer ? "Email"        : "Address");
        tfExtra3.setToolTipText(isDealer ? "e.g. dealer@email.com" : "e.g. 123 Main St, City");
    }

    private void doRegister() {
        String uname   = tfUsername.getText().trim();
        String pass    = new String(pfPass.getPassword()).trim();
        String confirm = new String(pfConfirm.getPassword()).trim();
        String role    = (String) cbRole.getSelectedItem();
        String ex1     = tfExtra1.getText().trim();
        String ex2     = tfExtra2.getText().trim();
        String ex3     = tfExtra3.getText().trim();

        if (uname.isEmpty() || pass.isEmpty() || ex1.isEmpty()) {
            warn("Please fill in all required fields."); return;
        }
        if (!pass.equals(confirm)) {
            warn("Passwords do not match."); return;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            // Check username uniqueness
            PreparedStatement chk = con.prepareStatement("SELECT user_id FROM Users WHERE username=?");
            chk.setString(1, uname);
            if (chk.executeQuery().next()) { warn("Username already taken."); return; }

            int refId;
            if ("dealer".equals(role)) {
                PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO Snack_Dealer(Dealer_Name,Contact_no,email,D_Address) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ins.setString(1, ex1); ins.setString(2, ex2);
                ins.setString(3, ex3); ins.setString(4, "");
                ins.executeUpdate();
                refId = getGenKey(ins);
            } else {
                // buyer: split name into first/last
                String[] parts = ex1.split(" ", 2);
                String fname = parts[0], lname = parts.length > 1 ? parts[1] : "";
                PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO Customers(F_name,L_name,C_Contact_no,C_Address) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ins.setString(1,fname); ins.setString(2,lname);
                ins.setString(3,ex2);   ins.setString(4,ex3);
                ins.executeUpdate();
                refId = getGenKey(ins);
            }

            PreparedStatement uins = con.prepareStatement(
                "INSERT INTO Users(username,password,role,ref_id) VALUES(?,?,?,?)");
            uins.setString(1,uname); uins.setString(2,pass);
            uins.setString(3,role);  uins.setInt(4,refId);
            uins.executeUpdate();

            JOptionPane.showMessageDialog(this,
                "Account created! You can now log in.", "Success",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginFrame();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"DB Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getGenKey(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.getGeneratedKeys();
        rs.next(); return rs.getInt(1);
    }

    private void addRow(JPanel p, GridBagConstraints gc, int row, JLabel lbl, JComponent fld) {
        GridBagConstraints gl = (GridBagConstraints)gc.clone();
        gl.gridy = row; gl.gridx = 0; gl.weightx = 0; gl.fill = GridBagConstraints.NONE;
        gl.insets = new Insets(7,0,2,0);
        p.add(lbl, gl);
        GridBagConstraints gf = (GridBagConstraints)gc.clone();
        gf.gridy = row; gf.gridx = 0; gf.weightx = 1; gf.fill = GridBagConstraints.HORIZONTAL;
        gf.insets = new Insets(0,0,4,0);
        p.add(fld, new GridBagConstraints(0,row,1,1,1,0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0,0,4,0),0,0));
        // Re-add label above field properly
        p.remove(p.getComponentCount()-1);
        p.add(lbl, new GridBagConstraints(0,row*2,1,1,1,0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(7,0,0,0),0,0));
        p.add(fld, new GridBagConstraints(0,row*2+1,1,1,1,0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0,0,4,0),0,0));
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    private JLabel bold(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIConstants.TEXT);
        return l;
    }

    private JPasswordField styledPwd() {
        JPasswordField f = new JPasswordField(24);
        f.setFont(UIConstants.LABEL_FONT);
        f.setBorder(new CompoundBorder(
            new LineBorder(UIConstants.LIGHT_ACCENT),
            new EmptyBorder(4,6,4,6)));
        return f;
    }
}