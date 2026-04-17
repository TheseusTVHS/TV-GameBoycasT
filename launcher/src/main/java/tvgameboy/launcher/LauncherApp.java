package tvgameboy.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Random;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import tvgameboy.shared.Game;

public final class LauncherApp {
    private static final int TILE_ROWS = 2;
    private static final int TILE_COLUMNS = 3;
    private static final int TILE_COUNT = TILE_ROWS * TILE_COLUMNS;

    private final JFrame frame;
    private final JPanel menuPanel;
    private final JPanel contentPanel;
    private Point dragOffset;
    private JButton maximizeButton;
    private Rectangle normalBounds;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LauncherApp::new);
    }

    public LauncherApp() {
        applyTheme();

        frame = new JFrame("TV GameBoy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setMinimumSize(new Dimension(900, 600));
        applyWindowIcon();

        menuPanel = buildMenuPanel();
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(10, 12, 14));

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(contentPanel, BorderLayout.CENTER);
        frame.setContentPane(root);

        showPanel(menuPanel);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildMenuPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        outer.setBackground(new Color(10, 12, 14));

        JPanel tiles = new JPanel(new GridLayout(TILE_ROWS, TILE_COLUMNS, 16, 16));
        tiles.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        tiles.setBackground(new Color(10, 12, 14));

        List<GameEntry> games = GameRegistry.getGames();
        GameEntry[] entries = new GameEntry[TILE_COUNT];
        for (int i = 0; i < TILE_COUNT; i++) {
            entries[i] = i < games.size() ? games.get(i) : null;
        }

        // Move everything from top-left (index 0) into the CAST slot (index 2)
        GameEntry tmp = entries[2];
        entries[2] = entries[0];
        entries[0] = tmp;

        for (int i = 0; i < TILE_COUNT; i++) {
            JButton button = createTileButton(i, entries[i]);
            tiles.add(button);
        }

        outer.add(tiles, BorderLayout.CENTER);
        return outer;
    }

    private JButton createTileButton(int index, GameEntry entry) {
        Color tileBackground = new Color(0, 100, 0);
        Color tileBorder = new Color(0, 128, 0);
        Color tileText = new Color(245, 246, 248);
        Color tileHover = new Color(0, 114, 0);
        Font tileFont = new Font("Segoe UI", Font.BOLD, 18);

        if (index == 2 && entry == null) {
            JButton button = new JButton("CAST");
            button.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(false);
            button.setBackground(tileBackground);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tileBorder),
                    BorderFactory.createEmptyBorder(20, 16, 20, 16)
            ));
            button.setFocusPainted(false);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    button.setBackground(tileHover);
                    button.repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    button.setBackground(tileBackground);
                    button.repaint();
                }
            });
            button.addActionListener(event -> showPanel(makeWhiteScreen(() -> showMenu())));
            return button;
        }

        if (entry == null) {
            JButton button = new JButton("Empty Slot");
            button.setFont(tileFont);
            button.setOpaque(true);
            button.setBackground(tileBackground);
            button.setForeground(new Color(190, 195, 202));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tileBorder),
                    BorderFactory.createEmptyBorder(20, 16, 20, 16)
            ));
            button.setEnabled(false);
            return button;
        }

        JButton button = new JButton(entry.getDisplayName(), entry.getIcon());
        button.setFont(tileFont);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBackground(tileBackground);
        button.setForeground(tileText);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tileBorder),
                BorderFactory.createEmptyBorder(20, 16, 20, 16)
        ));
        button.setFocusPainted(false);
        button.setHorizontalTextPosition(JButton.CENTER);
        button.setVerticalTextPosition(JButton.BOTTOM);
        button.setIconTextGap(12);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(tileHover);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(tileBackground);
                button.repaint();
            }
        });
        button.addActionListener(event -> openGame(entry));
        return button;
    }

    private JComponent makeWhiteScreen(Runnable returnToMenu) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = Math.max(1, getWidth());
                int h = Math.max(1, getHeight());
                Graphics2D g2 = (Graphics2D) g.create();

                // Try to load the provided image and render a pixelated, first-person raft view.
                BufferedImage img = null;
                try {
                    URL url = new URL("https://images.stockcake.com/public/b/d/0/bd0c0ea7-48f6-4e80-8c4c-89e074cc5cf9_large/serene-ocean-raft-stockcake.jpg");
                    img = ImageIO.read(url);
                } catch (IOException ex) {
                    img = null;
                }

                if (img != null) {
                    // Pixelate by scaling down then up using nearest-neighbor.
                    int scale = Math.max(6, Math.min(24, Math.min(w, h) / 40));
                    int sw = Math.max(1, w / scale);
                    int sh = Math.max(1, h / scale);

                    BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
                    Graphics2D gSmall = small.createGraphics();
                    gSmall.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    gSmall.drawImage(img, 0, 0, sw, sh, null);
                    gSmall.dispose();

                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.drawImage(small, 0, 0, w, h, null);

                    // Simple first-person raft overlay: draw wooden plank across bottom center
                    int plankH = Math.max(40, h / 8);
                    int plankW = Math.max(200, w / 2);
                    int px = (w - plankW) / 2;
                    int py = h - plankH - 20;
                    g2.setColor(new Color(102, 66, 40));
                    g2.fillRect(px, py, plankW, plankH);
                    g2.setColor(new Color(80, 48, 28));
                    for (int i = 0; i < 8; i++) {
                        int x = px + i * (plankW / 8);
                        g2.fillRect(x, py, 2, plankH);
                    }

                    // Simple rope/edge in front
                    g2.setColor(new Color(60, 40, 20));
                    g2.fillOval(px + plankW/2 - 30, py - 10, 60, 20);

                } else {
                    // Fallback to original gradient ocean if image fails to load
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Sky gradient (top half)
                    GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235), 0, h / 2, new Color(70, 130, 180));
                    g2.setPaint(sky);
                    g2.fillRect(0, 0, w, h / 2);

                    // Ocean gradient (bottom half)
                    GradientPaint sea = new GradientPaint(0, h / 2, new Color(28, 107, 160), 0, h, new Color(0, 51, 102));
                    g2.setPaint(sea);
                    g2.fillRect(0, h / 2, w, h / 2);

                    // Soft, organic clouds (overlapping translucent ovals)
                    Random rnd = new Random(42);
                    int cloudCount = 3 + (w / 500);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    for (int c = 0; c < cloudCount; c++) {
                        int cx = 40 + c * (w / Math.max(3, cloudCount)) + rnd.nextInt(120) - 60;
                        int cy = 30 + rnd.nextInt(h / 8);
                        int blobs = 5 + rnd.nextInt(6);
                        for (int b = 0; b < blobs; b++) {
                            int sx = cx + rnd.nextInt(140) - 70;
                            int sy = cy + rnd.nextInt(60) - 30;
                            int rw = 60 + rnd.nextInt(100);
                            int rh = 24 + rnd.nextInt(40);
                            int alpha = 120 + rnd.nextInt(100);
                            g2.setColor(new Color(255, 255, 255, Math.min(255, alpha)));
                            g2.fillOval(sx - rw / 2, sy - rh / 2, rw, rh);
                        }
                        // brighter core
                        for (int core = 0; core < 2; core++) {
                            int sx = cx + rnd.nextInt(60) - 30;
                            int sy = cy + rnd.nextInt(30) - 15;
                            int rw = 40 + rnd.nextInt(40);
                            int rh = 18 + rnd.nextInt(20);
                            g2.setColor(new Color(255, 255, 255, 200));
                            g2.fillOval(sx - rw / 2, sy - rh / 2, rw, rh);
                        }
                    }

                    // Multi-hue ocean bands for richer texture
                    Color[] oceanBands = new Color[] {
                            new Color(28, 107, 160),
                            new Color(20, 90, 150),
                            new Color(15, 75, 140),
                            new Color(10, 60, 120),
                            new Color(6, 45, 100)
                    };
                    int bands = oceanBands.length;
                    for (int i = 0; i < bands; i++) {
                        int y0 = h / 2 + (i * (h / 2)) / bands;
                        int y1 = h / 2 + ((i + 1) * (h / 2)) / bands;
                        GradientPaint gp = new GradientPaint(0, y0, oceanBands[i], 0, y1,
                                oceanBands[Math.min(i + 1, bands - 1)]);
                        g2.setPaint(gp);
                        g2.fillRect(0, y0, w, Math.max(1, y1 - y0));
                    }

                    // Subtle highlights and foam: many small translucent ovals
                    int highlights = Math.max(200, (w * h) / 8000);
                    for (int i = 0; i < highlights; i++) {
                        int xx = rnd.nextInt(w);
                        int yy = h / 2 + rnd.nextInt(h / 2);
                        int rw = 1 + rnd.nextInt(6);
                        int rh = 1 + rnd.nextInt(3);
                        int a = 20 + rnd.nextInt(120);
                        g2.setColor(new Color(255, 255, 255, Math.min(200, a)));
                        g2.fillOval(xx - rw, yy - rh, rw * 2, rh * 2);
                    }

                    // More detailed waves using multiple sine layers
                    for (int layer = 0; layer < 8; layer++) {
                        float alpha = 60 - layer * 6;
                        g2.setColor(new Color(255, 255, 255, Math.max(20, (int) alpha)));
                        int rows = 3 + layer;
                        for (int r = 0; r < rows; r++) {
                            int y = h / 2 + 10 + r * 14 + layer * 6;
                            Path2D.Double path = new Path2D.Double();
                            path.moveTo(0, y);
                            double freq = 0.01 + layer * 0.002;
                            double ampBase = 4 + layer;
                            for (int x = 0; x <= w; x += 6) {
                                double phase = (x * freq) + (r * 0.5) + (layer * 0.3);
                                double offset = Math.sin(phase) * (ampBase + Math.sin(x * 0.005) * 2);
                                path.lineTo(x, y + offset);
                            }
                            g2.draw(path);
                        }
                    }

                    // Bubbles: small translucent circles rising near the surface
                    for (int i = 0; i < 30; i++) {
                        int bx = rnd.nextInt(w);
                        int by = h / 2 + rnd.nextInt(h / 3);
                        int radius = 2 + rnd.nextInt(6);
                        int a = 80 + rnd.nextInt(120);
                        g2.setColor(new Color(255, 255, 255, a));
                        g2.fillOval(bx - radius, by - radius, radius * 2, radius * 2);
                        g2.setColor(new Color(255, 255, 255, Math.max(40, a - 60)));
                        g2.drawOval(bx - radius, by - radius, radius * 2, radius * 2);
                    }

                    g2.dispose();
                }
            }
        };

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton back = new JButton("X");
        back.setFont(new Font("Segoe UI", Font.BOLD, 14));
        back.addActionListener(e -> returnToMenu.run());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        right.add(back);

        top.add(right, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        return panel;
    }

    private void openGame(GameEntry entry) {
        Game game = entry.getFactory().get();
        showPanel(game.getView(this::showMenu));
        SwingUtilities.invokeLater(this::bringToFront);
    }

    private void showMenu() {
        showPanel(menuPanel);
    }

    private void showPanel(JComponent panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private static void applyTheme() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            return;
        }

        UIManager.put("control", new Color(10, 12, 14));
        UIManager.put("info", new Color(10, 12, 14));
        UIManager.put("nimbusBase", new Color(0, 100, 0));
        UIManager.put("nimbusAlertYellow", new Color(245, 246, 248));
        UIManager.put("nimbusDisabledText", new Color(112, 160, 84));
        UIManager.put("nimbusFocus", new Color(56, 176, 0));
        UIManager.put("nimbusLightBackground", new Color(10, 12, 14));
        UIManager.put("nimbusSelectionBackground", new Color(0, 128, 0));
        UIManager.put("nimbusSelectedText", new Color(245, 246, 248));
        UIManager.put("text", new Color(245, 246, 248));
        UIManager.put("Panel.background", new Color(10, 12, 14));
        UIManager.put("Button.background", new Color(0, 100, 0));
        UIManager.put("Button.foreground", new Color(245, 246, 248));
        UIManager.put("Label.foreground", new Color(245, 246, 248));
    }

    private JPanel buildTitleBar() {
        Color barBackground = new Color(10, 12, 14);
        Color barBorder = new Color(0, 75, 35);
        Color titleText = new Color(245, 246, 248);
        Font titleFont = new Font("Segoe UI", Font.BOLD, 14);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(barBackground);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, barBorder));

        JLabel title = new JLabel("TV GameBoy");
        title.setForeground(titleText);
        title.setFont(titleFont);
        title.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel iconLabel = new JLabel();
        iconLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 6));
        ImageIcon appIcon = loadAppIcon();
        if (appIcon != null) {
            iconLabel.setIcon(scaleIcon(appIcon, 18, 18));
        }

        JPanel titleArea = new JPanel(new BorderLayout());
        titleArea.setBackground(barBackground);
        titleArea.add(iconLabel, BorderLayout.WEST);
        titleArea.add(title, BorderLayout.CENTER);

        JButton minimizeButton = createTitleBarButton("_", 18, 12);
        minimizeButton.addActionListener(event -> frame.setState(Frame.ICONIFIED));

        maximizeButton = createTitleBarButton("MAX", 12, 10);
        maximizeButton.addActionListener(event -> toggleMaximize());

        JButton closeButton = createTitleBarButton("X", 12, 10);
        closeButton.addActionListener(event -> frame.dispose());

        JPanel controls = new JPanel(new GridLayout(1, 3, 6, 0));
        controls.setBackground(barBackground);
        controls.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        controls.add(minimizeButton);
        controls.add(maximizeButton);
        controls.add(closeButton);

        titleBar.add(titleArea, BorderLayout.WEST);
        titleBar.add(controls, BorderLayout.EAST);

        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                dragOffset = event.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragOffset == null) {
                    return;
                }
                Point screen = event.getLocationOnScreen();
                frame.setLocation(screen.x - dragOffset.x, screen.y - dragOffset.y);
            }
        };

        titleBar.addMouseListener(dragHandler);
        titleBar.addMouseMotionListener(dragHandler);
        title.addMouseListener(dragHandler);
        title.addMouseMotionListener(dragHandler);
        iconLabel.addMouseListener(dragHandler);
        iconLabel.addMouseMotionListener(dragHandler);

        return titleBar;
    }

    private JButton createTitleBarButton(String label, int fontSize, int horizontalPadding) {
        Color base = new Color(0, 114, 0);
        Color hover = new Color(0, 128, 0);
        JButton button = new JButton(label);
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setBackground(base);
        button.setForeground(new Color(245, 246, 248));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, horizontalPadding, 4, horizontalPadding));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(hover);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(base);
                button.repaint();
            }
        });
        return button;
    }

    private void toggleMaximize() {
        if (normalBounds != null) {
            setWindowBounds(normalBounds);
            normalBounds = null;
            maximizeButton.setText("MAX");
            return;
        }

        normalBounds = frame.getBounds();
        Rectangle usable = getUsableScreenBounds();
        setWindowBounds(usable);
        maximizeButton.setText("RESTORE");
    }

    private void setWindowBounds(Rectangle bounds) {
        frame.getContentPane().setVisible(false);
        frame.setBounds(bounds);
        frame.getContentPane().setVisible(true);
        frame.revalidate();
        frame.repaint();
    }

    private void bringToFront() {
        frame.setExtendedState(Frame.NORMAL);
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();

        Timer retry = new Timer(150, new ActionListener() {
            private int attempts = 0;

            @Override
            public void actionPerformed(ActionEvent event) {
                if (frame.isFocused() || attempts >= 5) {
                    ((Timer) event.getSource()).stop();
                    frame.setAlwaysOnTop(false);
                    return;
                }
                frame.setAlwaysOnTop(true);
                frame.toFront();
                frame.requestFocus();
                attempts++;
            }
        });
        retry.setRepeats(true);
        retry.start();
    }

    private Rectangle getUsableScreenBounds() {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
        );
        return new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom
        );
    }

    private void applyWindowIcon() {
        ImageIcon appIcon = loadAppIcon();
        if (appIcon != null) {
            frame.setIconImage(appIcon.getImage());
        }
    }

    private ImageIcon loadAppIcon() {
        try (InputStream stream = LauncherApp.class.getResourceAsStream("/icons/app-logo.png")) {
            if (stream == null) {
                return null;
            }
            Image image = ImageIO.read(stream);
            return new ImageIcon(image);
        } catch (IOException ignored) {
            return null;
        }
    }

    private ImageIcon scaleIcon(ImageIcon icon, int width, int height) {
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}

