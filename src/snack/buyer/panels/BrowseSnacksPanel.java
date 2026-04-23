package snack.buyer.panels;

import snack.db.DatabaseConnection;
import snack.util.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class BrowseSnacksPanel extends JPanel {

    private final int custId;
    private JTable table;
    private DefaultTableModel model;
    private JTextField tfSearch;

    private static final String[] COLS = {
        "Snack ID","Snack Name","Price (₹)","Mfg Date","Expiry","Manufacturer","Shop","Stock"
    };

    public BrowseSnacksPanel(int custId) {
        this.custId = custId;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG);
        buildUI();
        loadData("");
    }

    private void buildUI() {
        add(UIConstants.makeTitlePanel("Browse Snacks",
            "All available snacks across shops in your network"), BorderLayout.NORTH);

        // Search bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));
        searchBar.setBackground(UIConstants.BG);
        searchBar.add(UIConstants.makeLabel("🔍 Search:"));
        tfSearch = UIConstants.makeField(24);
        JButton btnSearch  = UIConstants.makeButton("Search", UIConstants.ACCENT);
        JButton btnRefresh = UIConstants.btnRefresh();
        searchBar.add(tfSearch);
        searchBar.add(btnSearch);
        searchBar.add(btnRefresh);

        model = new DefaultTableModel(COLS, 0);
        table = UIConstants.makeTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(UIConstants.WHITE);

        add(searchBar, BorderLayout.SOUTH);
        add(sp,        BorderLayout.CENTER);

        btnSearch.addActionListener(e  -> loadData(tfSearch.getText().trim()));
        btnRefresh.addActionListener(e -> { tfSearch.setText(""); loadData(""); });
        tfSearch.addActionListener(e   -> loadData(tfSearch.getText().trim()));
    }

    private void loadData(String search) {
        model.setRowCount(0);
        try {
            String sql =
                "SELECT sn.Snack_ID, sn.Snack_Name, sn.Price, sn.MFd, sn.Expiry, " +
                "  i.I_Name, sh.Shop_name, COALESCE(ss.stock_qty,0) AS stock " +
                "FROM Snacks sn " +
                "LEFT JOIN Industries i   ON sn.L_ID   = i.L_ID " +
                "LEFT JOIN Shop_Snacks ss ON sn.Snack_ID = ss.Snack_ID " +
                "LEFT JOIN Shops sh       ON ss.Shop_ID  = sh.Shop_ID " +
                (search.isEmpty() ? "" :
                 "WHERE sn.Snack_Name LIKE ? OR i.I_Name LIKE ? ") +
                "ORDER BY sn.Snack_Name";

            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                ps.setString(1, like); ps.setString(2, like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int stock = rs.getInt("stock");
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2),
                    String.format("₹ %.2f", rs.getDouble(3)),
                    rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getString(7),
                    stock > 0 ? stock : "Out of Stock"
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,"DB Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }
}