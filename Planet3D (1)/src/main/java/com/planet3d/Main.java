package com.planet3d;

import com.planet3d.ui.MainWindow;

import javax.swing.*;

/**
 * Entry point của ứng dụng Planet3D.
 */
public class Main {

    public static void main(String[] args) {
        // Set Look and Feel to FlatLaf Dark Theme
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Apply dark colors globally
        UIManager.put("Panel.background", new java.awt.Color(18, 22, 36));
        UIManager.put("OptionPane.background", new java.awt.Color(22, 28, 48));
        UIManager.put("ScrollPane.background", new java.awt.Color(18, 22, 36));
        UIManager.put("TabbedPane.background", new java.awt.Color(22, 28, 48));
        UIManager.put("TabbedPane.selected", new java.awt.Color(35, 45, 80));
        UIManager.put("SplitPane.background", new java.awt.Color(18, 22, 36));

        // Run on Swing EDT
        SwingUtilities.invokeLater(() -> {
            System.out.println("==============================================");
            System.out.println("  Planet3D - Ứng dụng Mô phỏng Hành tinh 3D");
            System.out.println("  Version 1.0 | Java + JOGL + SQLite");
            System.out.println("==============================================");

            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
