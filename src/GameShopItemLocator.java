
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameShopItemLocator extends JFrame {

    private final JTextField txtItem;
    private final Map<String, StoreRow> stores = new LinkedHashMap<>(); // preserve order
    private Connection conn;

    public GameShopItemLocator() {
        setNimbusLookAndFeel();

        setTitle("Console Game Location Finder Database");
        setSize(680, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Root gradient background
        JPanel root = new GradientPanel(new Color(245, 248, 255), new Color(225, 235, 255));
        root.setLayout(null);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        connectDB();
        createTable();

        // Card container (white surface)
        JPanel card = new JPanel(null);
        card.setBounds(24, 24, 632, 552);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 240)),
                new EmptyBorder(18, 18, 18, 18)
        ));
        root.add(card);

        // Fonts
        Font titleFont  = new Font("Segoe UI", Font.BOLD, 20);
        Font labelFont  = new Font("Segoe UI", Font.PLAIN, 14);

        // Title
        JLabel title = new JLabel("Console Game Location Finder — Database");
        title.setFont(titleFont);
        title.setForeground(new Color(25, 60, 120));
        title.setBounds(18, 8, 560, 30);
        card.add(title);

        // Item field
        JLabel lblItem = new JLabel("Item:");
        lblItem.setFont(labelFont);
        lblItem.setForeground(new Color(50, 60, 75));
        lblItem.setBounds(18, 52, 60, 28);
        card.add(lblItem);

        txtItem = new JTextField();
        stylizeTextField(txtItem);
        txtItem.setBounds(78, 52, 520, 30);
        card.add(txtItem);

        // Section header
        JLabel lblStore = new JLabel("Stores with availability, quantity, and price");
        lblStore.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblStore.setForeground(new Color(20, 75, 160));
        lblStore.setBounds(18, 98, 400, 24);
        card.add(lblStore);

        // Column headers row
        JLabel hdrName  = makeHeaderLabel("Store");
        JLabel hdrQty   = makeHeaderLabel("Qty");
        JLabel hdrPrice = makeHeaderLabel("Price");

        hdrName.setBounds(60, 128, 200, 22);
        hdrQty.setBounds(300, 128, 80, 22);
        hdrPrice.setBounds(400, 128, 100, 22);

        card.add(hdrName);
        card.add(hdrQty);
        card.add(hdrPrice);

        // Store rows (checkbox + name + qty + price)
        int y = 156;
        for (String name : new String[]{"Store A","Store B","Store C","Store D","Store E","Store F"}) {
            addStoreRow(card, name, y);
            y += 38;
        }

        // Buttons
        JButton btnAdd  = new RoundedButton("Add Item",  new Color(0, 140, 255));
        JButton btnView = new RoundedButton("View Items", new Color(0, 180, 100));

        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnView.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnAdd.setBounds(180, 440, 140, 42);
        btnView.setBounds(340, 440, 140, 42);

        card.add(btnAdd);
        card.add(btnView);

        btnAdd.addActionListener(e -> saveItem());
        btnView.addActionListener(e -> openViewWindow());
    }

    // Database
    private void connectDB() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:gameshop.db");
            System.out.println("Database connected.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to database.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void createTable() {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        itemName TEXT,
                        storeName TEXT,
                        quantity INTEGER,
                        price REAL
                    );
                    """;
            conn.createStatement().execute(sql);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize database table.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addStoreRow(JPanel parent, String name, int y) {
        JCheckBox chk = new JCheckBox();
        chk.setBounds(18, y, 24, 24);
        parent.add(chk);

        JLabel lbl = new JLabel(name);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(35, 45, 60));
        lbl.setBounds(60, y, 220, 24);
        parent.add(lbl);

        JTextField qty = new JTextField();
        stylizeTextField(qty);
        qty.setHorizontalAlignment(SwingConstants.CENTER);
        qty.setBounds(300, y, 80, 26);
        parent.add(qty);

        JTextField price = new JTextField();
        stylizeTextField(price);
        price.setHorizontalAlignment(SwingConstants.RIGHT);
        price.setBounds(400, y, 120, 26);
        parent.add(price);

        stores.put(name, new StoreRow(chk, qty, price));
    }

    /* ---------- Actions ---------- */
    private void saveItem() {
        String itemName = txtItem.getText().trim();
        if (itemName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the item name.");
            txtItem.requestFocus();
            return;
        }

        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO items (itemName, storeName, quantity, price) VALUES (?, ?, ?, ?)"
            );
            boolean atLeastOne = false;

            for (Map.Entry<String, StoreRow> entry : stores.entrySet()) {
                String store = entry.getKey();
                StoreRow row = entry.getValue();

                if (row.chk.isSelected()) {
                    String qtyText = row.qty.getText().trim();
                    String priceText = row.price.getText().trim();

                    if (qtyText.isEmpty() || priceText.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Please fill quantity and price for " + store);
                        return;
                    }

                    int qty;
                    float price;
                    try {
                        qty = Integer.parseInt(qtyText);
                        price = Float.parseFloat(priceText);
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(this, "Invalid number for " + store + ". Use numeric values.");
                        return;
                    }

                    stmt.setString(1, itemName);
                    stmt.setString(2, store);
                    stmt.setInt(3, qty);
                    stmt.setFloat(4, price);
                    stmt.executeUpdate();
                    atLeastOne = true;
                }
            }

            if (!atLeastOne) {
                JOptionPane.showMessageDialog(this, "Please select at least one store.");
                return;
            }

            JOptionPane.showMessageDialog(this, "Item Saved Successfully!");
            // Clear inputs (keep item name for quick batch entry)
            for (StoreRow r : stores.values()) {
                r.chk.setSelected(false);
                r.qty.setText("");
                r.price.setText("");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving data.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void openViewWindow() {
        JFrame viewFrame = new JFrame("Saved Items");
        viewFrame.setSize(720, 420);
        viewFrame.setLocationRelativeTo(this);

        String[] columns = {"Item Name", "Store", "Quantity", "Price"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        try {
            String sql = "SELECT itemName, storeName, quantity, price FROM items";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String item = rs.getString("itemName");
                String store = rs.getString("storeName");
                int qty = rs.getInt("quantity");
                float price = rs.getFloat("price");
                model.addRow(new Object[]{item, store, qty, price});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading data.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }

        JTable table = new JTable(model) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stylizeTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245)));

        viewFrame.add(scroll);
        viewFrame.setVisible(true);
    }


    private JLabel makeHeaderLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(25, 75, 140));
        return l;
    }

    private void stylizeTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(new Color(35, 45, 60));
        tf.setBackground(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
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

        JTableHeader header = tbl.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(245, 248, 255));
        header.setForeground(new Color(25, 75, 140));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 230, 245)));

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
                if (column == 2) setHorizontalAlignment(SwingConstants.CENTER); // Qty
                if (column == 3) setHorizontalAlignment(SwingConstants.RIGHT);  // Price
                return c;
            }
        };
        for (int i = 0; i < tbl.getColumnCount(); i++) {
            tbl.getColumnModel().getColumn(i).setCellRenderer(renderer);
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

    private static class StoreRow {
        JCheckBox chk;
        JTextField qty, price;
        StoreRow(JCheckBox chk, JTextField qty, JTextField price) {
            this.chk = chk;
            this.qty = qty;
            this.price = price;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameShopItemLocator().setVisible(true));
    }
}
