package tvgameboy.games.template;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import tvgameboy.shared.Game;

public final class TemplateGame implements Game {
    @Override
    public JComponent getView(Runnable returnToMenu) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(10, 12, 14));

        JButton menuButton = new JButton("Menu");
        menuButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        menuButton.setBackground(new Color(0, 100, 0));
        menuButton.setForeground(new Color(245, 246, 248));
        menuButton.setFocusPainted(false);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(10, 12, 14));
        topBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        topBar.add(menuButton, BorderLayout.WEST);

        JLabel dayLabel = new JLabel("Day", SwingConstants.CENTER);
        dayLabel.setForeground(new Color(245, 246, 248));
        dayLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topBar.add(dayLabel, BorderLayout.CENTER);

        JLabel timerLabel = new JLabel("60", SwingConstants.RIGHT);
        timerLabel.setForeground(new Color(245, 246, 248));
        timerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topBar.add(timerLabel, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        JLabel statusLabel = new JLabel("Click 'Cast' to fish.", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(245, 246, 248));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        JButton castButton = new JButton("Cast");
        castButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        castButton.setFocusPainted(false);

        JLabel scoreLabel = new JLabel("Fish caught: 0", SwingConstants.CENTER);
        scoreLabel.setForeground(new Color(245, 246, 248));
        scoreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        center.add(statusLabel, BorderLayout.CENTER);
        center.add(castButton, BorderLayout.SOUTH);
        center.add(scoreLabel, BorderLayout.NORTH);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);

        // Game state
        final int[] score = {0};
        final boolean[] isCasting = {false};
        final boolean[] fishPresent = {false};

        // Day/night cycle: 60s each
        final boolean[] isDay = {true};
        final int[] secondsLeft = {60};

        // Day/night timer (ticks every second)
        Timer dayNightTimer = new Timer(1000, null);
        dayNightTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                secondsLeft[0]--;
                if (secondsLeft[0] <= 0) {
                    isDay[0] = !isDay[0];
                    secondsLeft[0] = 60;
                    dayLabel.setText(isDay[0] ? "Day" : "Night");
                    panel.setBackground(isDay[0] ? new Color(120, 168, 255) : new Color(10, 12, 40));
                    topBar.setBackground(panel.getBackground());
                    center.setBackground(panel.getBackground());
                }
                timerLabel.setText(Integer.toString(secondsLeft[0]));
            }
        });
        dayNightTimer.setInitialDelay(0);
        dayNightTimer.start();

        // Helper to stop timers when returning to menu
        Runnable stopAll = () -> {
            dayNightTimer.stop();
        };

        menuButton.addActionListener(event -> {
            stopAll.run();
            returnToMenu.run();
        });

        // Casting / catching logic
        castButton.addActionListener(event -> {
            if (isCasting[0]) {
                // If fish is present, catch it
                if (fishPresent[0]) {
                    // 50% chance to catch the fish
                    Random rnd = new Random();
                    boolean caught = rnd.nextBoolean();
                    if (caught) {
                        score[0]++;
                        scoreLabel.setText("Fish caught: " + score[0]);
                        statusLabel.setText("You caught a fish!");
                    } else {
                        statusLabel.setText("The fish got away!");
                    }
                    fishPresent[0] = false;
                    isCasting[0] = false;
                    castButton.setText("Cast");
                }
                return;
            }

            // Start casting
            isCasting[0] = true;
            castButton.setText("Waiting...");
            statusLabel.setText("Casting...");

            // After 2 seconds, a fish will appear
            Timer fishTimer = new Timer(2000, null);
            fishTimer.setRepeats(false);
            fishTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fishPresent[0] = true;
                    statusLabel.setText("Fish biting! Click 'Cast' to catch.");
                    castButton.setText("Catch");
                }
            });
            fishTimer.start();
        });

        return panel;
    }
}
