package snack.util;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Centralised UI constants and factory helpers for a consistent look & feel.
 */
public final class UIConstants {

    // ── Palette ──────────────────────────────────────────────
    public static final Color PRIMARY      = new Color(26,  35, 126);   // deep indigo
    public static final Color ACCENT       = new Color(63,  81, 181);   // indigo
    public static final Color LIGHT_ACCENT = new Color(197,202,233);    // very light indigo
    public static final Color BG           = new Color(245,245,250);
    public static final Color SIDEBAR_BG   = new Color(21,  28, 100);
    public static final Color SIDEBAR_SEL  = new Color(48,  63, 159);
    public static final Color DANGER       = new Color(198,  40,  40);
    public static final Color SUCCESS      = new Color( 27, 127,  50);
    public static final Color WARNING      = new Color(230, 119,   0);
    public static final Color WHITE        = Color.WHITE;
    public static final Color TEXT         = new Color( 33,  33,  33);
    public static final Color MUTED        = new Color(117, 117, 117);

    // ── Fonts ─────────────────────────────────────────────────
    public static final Font TITLE_FONT   = new Font("Segoe UI", Font.BOLD,  24);
    public static final Font HEADER_FONT  = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font LABEL_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BUTTON_FONT  = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font TABLE_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font TABLE_HEADER = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font SMALL_FONT   = new Font("Segoe UI", Font.PLAIN, 11);

    private UIConstants() {}

    // ── Button Factory ────────────────────────────────────────
    public static JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(BUTTON_FONT);
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        return btn;
    }

    public static JButton btnAdd()     { return makeButton("＋ Add",    ACCENT);  }
    public static JButton btnEdit()    { return makeButton("✎ Edit",   WARNING); }
    public static JButton btnDelete()  { return makeButton("✕ Delete", DANGER);  }
    public static JButton btnRefresh() { return makeButton("↺ Refresh",new Color(69,90,100)); }
    public static JButton btnSave()    { return makeButton("✔ Save",   SUCCESS); }
    public static JButton btnCancel()  { return makeButton("Cancel",   MUTED);   }

    // ── Table Factory ─────────────────────────────────────────
    public static JTable makeTable(DefaultTableModel model) {
        JTable t = new JTable(model) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        t.setFont(TABLE_FONT);
        t.setRowHeight(30);
        t.setGridColor(LIGHT_ACCENT);
        t.setSelectionBackground(LIGHT_ACCENT);
        t.setSelectionForeground(PRIMARY);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0,1));
        styleHeader(t.getTableHeader());
        // Alternate row colours
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,col);
                if (!isSelected) setBackground(row % 2 == 0 ? WHITE : new Color(235,237,255));
                setBorder(new EmptyBorder(0,8,0,8));
                return this;
            }
        });
        return t;
    }

    public static void styleHeader(JTableHeader h) {
        h.setFont(TABLE_HEADER);
        h.setBackground(PRIMARY);
        h.setForeground(WHITE);
        h.setPreferredSize(new Dimension(h.getWidth(), 36));
        h.setReorderingAllowed(false);
    }

    // ── Panel / Border helpers ────────────────────────────────
    public static JPanel makeTitlePanel(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(WHITE);
        p.setBorder(new CompoundBorder(
                new MatteBorder(0,0,2,0,ACCENT),
                new EmptyBorder(14,20,14,20)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(TITLE_FONT);
        lbl.setForeground(PRIMARY);
        JLabel sub = new JLabel(subtitle);
        sub.setFont(SMALL_FONT);
        sub.setForeground(MUTED);
        p.add(lbl, BorderLayout.NORTH);
        p.add(sub, BorderLayout.SOUTH);
        return p;
    }

    public static JPanel makeButtonBar(JButton... btns) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(BG);
        for (JButton b : btns) p.add(b);
        return p;
    }

    public static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    public static JTextField makeField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(LABEL_FONT);
        f.setBorder(new CompoundBorder(
            new LineBorder(LIGHT_ACCENT),
            new EmptyBorder(4,6,4,6)));
        return f;
    }

    public static JComboBox<String> makeCombo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(LABEL_FONT);
        return c;
    }

    /** Lay out label/field pairs in a grid. labels & fields must match in length. */
    public static JPanel formGrid(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        p.setBorder(new EmptyBorder(16,20,16,20));
        GridBagConstraints gl = new GridBagConstraints();
        gl.anchor = GridBagConstraints.WEST;
        gl.insets = new Insets(6,4,6,10);
        GridBagConstraints gf = new GridBagConstraints();
        gf.fill = GridBagConstraints.HORIZONTAL;
        gf.weightx = 1;
        gf.insets = new Insets(6,0,6,4);
        for (int i = 0; i < labels.length; i++) {
            gl.gridx = 0; gl.gridy = i;
            gf.gridx = 1; gf.gridy = i;
            p.add(makeLabel(labels[i]), gl);
            p.add(fields[i], gf);
        }
        return p;
    }

    /** Standard modal dialog shell */
    public static JDialog makeDialog(JFrame owner, String title, int w, int h) {
        JDialog d = new JDialog(owner, title, true);
        d.setSize(w, h);
        d.setLocationRelativeTo(owner);
        d.setResizable(false);
        d.getContentPane().setBackground(WHITE);
        return d;
    }
}