
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.*;

public class HomePage extends JFrame {
    private final JComboBox<String> itemDropdown;
    private final JComboBox<String> locationDropdown;
    private final JTable table;
    private Connection conn;

    // predefined distances for each store per location
    private final Map<String, Integer> annexDistance = Map.of(
            "Store A", 10,
            "Store B", 12,
            "Store C", 18,
            "Store D", 22,
            "Store E", 30,
            "Store F", 8
    );
    private final Map<String, Integer> cpagDistance = Map.of(
            "Store A", 15,
            "Store B", 9,
            "Store C", 20,
            "Store D", 14,
            "Store E", 27,
            "Store F", 11
    );
    private final Map<String, Integer> maysanDistance = Map.of(
            "Store A", 22,
            "Store B", 7,
            "Store C", 13,
            "Store D", 25,
            "Store E", 19,
            "Store F", 16
    );

    public HomePage() {
        // Prefer Nimbus L&F for a modern base (ships with Java)
        setNimbusLookAndFeel();

        setTitle("Console Game Location Finder");
        setSize(760, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Gradient background panel
        JPanel root = new GradientPanel(new Color(245, 248, 255), new Color(225, 235, 255));
        root.setLayout(null);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        connectDB();

        // Shared font palette
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font headerFont = new Font("Segoe UI", Font.BOLD, 16);
        Font controlFont = new Font("Segoe UI", Font.PLAIN, 14);

        // Card container for content
        JPanel card = new JPanel(null);
        card.setBounds(24, 24, 700, 540);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 240)),
                new EmptyBorder(18, 18, 18, 18)
        ));
        root.add(card);

        // Title
        JLabel title = new JLabel("Console Game Location Finder");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(25, 60, 120));
        title.setBounds(18, 10, 400, 30);
        card.add(title);

        // Item label
        JLabel lblItem = new JLabel("Item:");
        lblItem.setFont(labelFont);
        lblItem.setForeground(new Color(50, 60, 75));
        lblItem.setBounds(18, 60, 100, 28);
        card.add(lblItem);

        // Item dropdown
        itemDropdown = new JComboBox<>(loadUniqueItems());
        stylizeCombo(itemDropdown, controlFont);
        itemDropdown.setBounds(118, 60, 320, 30);
        card.add(itemDropdown);

        // Location label
        JLabel lblLoc = new JLabel("Location:");
        lblLoc.setFont(labelFont);
        lblLoc.setForeground(new Color(50, 60, 75));
        lblLoc.setBounds(18, 100, 100, 28);
        card.add(lblLoc);

        // Location dropdown
        locationDropdown = new JComboBox<>(new String[]{
                "PLV Annex", "PLV CPAG", "PLV Maysan"
        });
        stylizeCombo(locationDropdown, controlFont);
        locationDropdown.setBounds(118, 100, 320, 30);
        card.add(locationDropdown);

        // Section header
        JLabel lblAvail = new JLabel("AVAILABLE STORES WITH THAT ITEM");
        lblAvail.setFont(headerFont);
        lblAvail.setForeground(new Color(20, 75, 160));
        lblAvail.setBounds(18, 148, 400, 30);
        card.add(lblAvail);

        // Table
        String[] cols = {"Store Name", "Distance", "Price", "Quantity"};
        table = new JTable(new DefaultTableModel(cols, 0)) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        stylizeTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(18, 186, 664, 280);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245)));
        card.add(scroll);

        // Check Location Button (rounded with hover)
        JButton btnCheck = new RoundedButton("Check Location", new Color(0, 140, 255));
        btnCheck.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCheck.setForeground(Color.WHITE);
        btnCheck.setBounds(260, 480, 180, 42);
        card.add(btnCheck);

        // Interactions
        itemDropdown.addActionListener(e -> loadStores());
        locationDropdown.addActionListener(e -> loadStores());

        btnCheck.addActionListener(e -> {
            java.util.List<Object[]> storeData = new ArrayList<>();
            for (int i = 0; i < table.getRowCount(); i++) {
                storeData.add(new Object[]{
                        String.valueOf(table.getValueAt(i, 0)), // Store
                        String.valueOf(table.getValueAt(i, 1)), // Distance
                        String.valueOf(table.getValueAt(i, 2)), // Price
                        Integer.parseInt(String.valueOf(table.getValueAt(i, 3))) // Qty
                });
            }
            new LocationWindow(
                    (String) itemDropdown.getSelectedItem(),
                    (String) locationDropdown.getSelectedItem(),
                    storeData
            ).setVisible(true);
        });

        // Initial load
        loadStores();
    }

    // Database
    private void connectDB() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:gameshop.db");
            System.out.println("Homepage Connected to DB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] loadUniqueItems() {
        LinkedHashSet<String> items = new LinkedHashSet<>();
        try {
            String sql = "SELECT DISTINCT itemName FROM items";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(rs.getString("itemName"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items.toArray(new String[0]);
    }

    private void loadStores() {
        String item = (String) itemDropdown.getSelectedItem();
        String location = (String) locationDropdown.getSelectedItem();
        if (item == null || location == null) return;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        Map<String, Integer> selectedMap =
                location.equals("PLV Annex") ? annexDistance :
                        location.equals("PLV CPAG") ? cpagDistance :
                                maysanDistance;

        try {
            String sql = "SELECT storeName, quantity, price FROM items WHERE itemName = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, item);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String store = rs.getString("storeName");
                int qty = rs.getInt("quantity");
                float price = rs.getFloat("price");
                int distance = selectedMap.getOrDefault(store, 0);

                model.addRow(new Object[]{
                        store,
                        distance + " m",
                        "₱" + price,
                        qty
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void stylizeCombo(JComboBox<?> combo, Font font) {
        combo.setFont(font);
        combo.setBackground(Color.WHITE);
        combo.setForeground(new Color(35, 45, 60));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 225, 240)),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void stylizeTable(JTable tbl) {
        tbl.setRowHeight(28);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbl.setForeground(new Color(30, 40, 60));
        tbl.setGridColor(new Color(235, 240, 248));
        tbl.setShowGrid(true);

        // Header styling
        JTableHeader header = tbl.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(245, 248, 255));
        header.setForeground(new Color(25, 75, 140));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 230, 245)));

        // Striped rows + alignment
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    Color even = new Color(252, 253, 255);
                    Color odd  = new Color(244, 248, 255);
                    c.setBackground(row % 2 == 0 ? even : odd);
                } else {
                    c.setBackground(new Color(200, 225, 255));
                }
                setBorder(new EmptyBorder(4, 8, 4, 8));
                if (column == 1 || column == 3) setHorizontalAlignment(SwingConstants.CENTER);
                if (column == 2) setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        };
        for (int i = 0; i < tbl.getColumnCount(); i++) {
            tbl.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    // Soft vertical gradient background
    static class GradientPanel extends JPanel {
        private final Color top;
        private final Color bottom;
        GradientPanel(Color top, Color bottom) {
            this.top = top;
            this.bottom = bottom;
            setOpaque(true);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    static class RoundedButton extends JButton {
        private final Color baseColor;
        private boolean hover = false;

        RoundedButton(String text, Color baseColor) {
            super(text);
            this.baseColor = baseColor;

            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(Color.WHITE);
            setMargin(new Insets(8, 16, 8, 16));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = hover ? baseColor.brighter() : baseColor;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HomePage().setVisible(true));
    }
}
