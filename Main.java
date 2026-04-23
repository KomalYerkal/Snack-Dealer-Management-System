package snack;

import snack.auth.LoginFrame;
import javax.swing.*;

/**
 * Entry point — sets Look & Feel then launches the Login window.
 */
public class Main {
    public static void main(String[] args) {
        // Use system look-and-feel for native widgets; fallback to Nimbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }

        // Swing is not thread-safe — always start on the EDT
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}