
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;

public class LocationWindow extends JFrame {
    private String userLocation;
    private java.util.List<Object[]> storeList; // storeName, distance, price, qty
    private String selectedItem;

    private Image mapImage;
    private BufferedImage userPointer;
    private BufferedImage greenPointer;
    private BufferedImage grayPointer;

    private static final int POINTER_SIZE = 30;

    public LocationWindow(String selectedItem, String location, java.util.List<Object[]> stores) {
        this.userLocation = location;
        this.storeList = stores;
        this.selectedItem = selectedItem;

        setNimbusLookAndFeel();

        setTitle("Location Viewer");
        setSize(900, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Root gradient background
        JPanel root = new GradientPanel(new Color(245, 248, 255), new Color(225, 235, 255));
        root.setLayout(null);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        // Card container (white surface)
        JPanel card = new JPanel(null);
        card.setBounds(24, 24, 852, 640);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 240)),
                new EmptyBorder(18, 18, 18, 18)
        ));
        root.add(card);

        // Fonts
        Font titleFont  = new Font("Segoe UI", Font.BOLD, 20);
        Font labelFont  = new Font("Segoe UI", Font.PLAIN, 14);
        Font headerFont = new Font("Segoe UI", Font.BOLD, 13);

        // Title
        JLabel title = new JLabel("Location — " + selectedItem + " near " + userLocation);
        title.setFont(titleFont);
        title.setForeground(new Color(25, 60, 120));
        title.setBounds(18, 8, 620, 30);
        card.add(title);

        // Map panel (BIGGER)
        loadImages();
        MapPanel mapPanel = new MapPanel();
        mapPanel.setBounds(18, 48, 600, 420); // <- increased size
        mapPanel.setBackground(Color.WHITE);
        mapPanel.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245)));
        card.add(mapPanel);

        // Store list table (right side)
        String[] cols = {"Store", "Distance", "Price", "Qty"};
        JTable storeTable = new JTable(new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        stylizeTable(storeTable, headerFont);

        // Populate table from storeList
        DefaultTableModel model = (DefaultTableModel) storeTable.getModel();
        for (Object[] row : storeList) {
            String store = (String) row[0];
            String distance = (String) row[1];
            String price = (String) row[2];
            int qty = (int) row[3];
            model.addRow(new Object[]{store, distance, price, qty});
        }

        JScrollPane storeScroll = new JScrollPane(storeTable);
        storeScroll.setBounds(630, 48, 204, 420); // aligns with taller map area
        storeScroll.getViewport().setBackground(Color.WHITE);
        storeScroll.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245)));
        card.add(storeScroll);

        // Compact legend (SMALLER, single line)
        JLabel legendCompact = new JLabel(
                "Legend: Green pin = nearest · Gray pin = farthest · Red pin = your location"
        );
        legendCompact.setFont(new Font("Segoe UI", Font.PLAIN, 11));           // smaller font
        legendCompact.setForeground(new Color(90, 100, 120));
        legendCompact.setBounds(18, 480, 816, 18);                             // tighter height
        card.add(legendCompact);

        // Back button (rounded with hover), moved slightly down
        JButton btnBack = new RoundedButton("Go Back Home", new Color(0, 140, 255));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnBack.setForeground(Color.WHITE);
        btnBack.setBounds(654, 560, 180, 42);
        btnBack.addActionListener(e -> dispose());
        card.add(btnBack);
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

    private void loadImages() {
        // Load the map image
        mapImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/map.png"))).getImage();
        try {
            Image originalRed   = new ImageIcon(Objects.requireNonNull(getClass().getResource("/pointer_red.png"))).getImage();
            Image originalGreen = new ImageIcon(Objects.requireNonNull(getClass().getResource("/pointer_green.png"))).getImage();
            Image originalGray  = new ImageIcon(Objects.requireNonNull(getClass().getResource("/pointer_gray.png"))).getImage();

            userPointer  = getScaledImage(originalRed, 40);
            greenPointer = getScaledImage(originalGreen, 32);
            grayPointer  = getScaledImage(originalGray, 32);
        } catch (Exception e) {
            System.err.println("Error loading/scaling pointer images: " + e.getMessage());
            userPointer  = new BufferedImage(POINTER_SIZE, POINTER_SIZE, BufferedImage.TYPE_INT_ARGB);
            greenPointer = new BufferedImage(POINTER_SIZE, POINTER_SIZE, BufferedImage.TYPE_INT_ARGB);
            grayPointer  = new BufferedImage(POINTER_SIZE, POINTER_SIZE, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static BufferedImage getScaledImage(Image img, int size) {
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(img, 0, 0, size, size, null);
        g2.dispose();
        return resized;
    }

    private class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw map scaled to panel
            g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);

            // Draw user pointer
            Point userPoint = getUserLocationPoint();
            g.drawImage(userPointer, userPoint.x, userPoint.y, null);

            // Draw store pointers
            drawStorePointers(g);
        }
    }

    private Point getUserLocationPoint() {
        switch (userLocation) {
            case "PLV Annex":  return new Point(240, 195);
            case "PLV CPAG":   return new Point(280, 220);
            case "PLV Maysan": return new Point(630,  90);
            default:           return new Point(300, 200);
        }
    }

    private void drawStorePointers(Graphics g) {
        int minDist = Integer.MAX_VALUE;
        int maxDist = Integer.MIN_VALUE;

        // Determine min/max distances
        for (Object[] row : storeList) {
            String d = (String) row[1];
            int meters = parseMeters(d);
            if (meters < minDist) minDist = meters;
            if (meters > maxDist) maxDist = meters;
        }

        // Draw pointers
        for (Object[] row : storeList) {
            String store    = (String) row[0];
            String distance = (String) row[1];
            int meters = parseMeters(distance);
            Point p = getStoreLocationPoint(store);

            Image icon;
            if (meters == minDist) {
                icon = greenPointer;
            } else if (meters == maxDist) {
                icon = grayPointer;
            } else {
                icon = grayPointer; // mid values use gray for now
            }
            g.drawImage(icon, p.x, p.y, null);
        }
    }

    private int parseMeters(String d) {
        try {
            return Integer.parseInt(d.replace(" m", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private Point getStoreLocationPoint(String store) {
        switch (store) {
            case "Store A": return new Point(200, 120);
            case "Store B": return new Point(250, 160);
            case "Store C": return new Point(300, 200);
            case "Store D": return new Point(350, 240);
            case "Store E": return new Point(400, 180);
            case "Store F": return new Point(450, 150);
            default:        return new Point(300, 200);
        }
    }

    private void stylizeTable(JTable tbl, Font headerFont) {
        tbl.setRowHeight(28);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbl.setForeground(new Color(30, 40, 60));
        tbl.setGridColor(new Color(235, 240, 248));
        tbl.setShowGrid(true);

        JTableHeader header = tbl.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
        header.setFont(headerFont);
        header.setBackground(new Color(245, 248, 255));
        header.setForeground(new Color(25, 75, 140));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 230, 245)));

        // Center Qty and Distance
        javax.swing.table.DefaultTableCellRenderer renderer = new javax.swing.table.DefaultTableCellRenderer() {
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
}
