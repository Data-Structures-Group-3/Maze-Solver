import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class TitleScreen extends JFrame {
    /**
     * Title Screen frame to which the MS can be ran from by pressing the Start button
     */
    public TitleScreen() {
        setTitle("Maze Solver");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Use a background image for the Panel
        BackgroundPanel mainPanel = new BackgroundPanel("images/TitleImage.jpg");
        mainPanel.setLayout(new GridBagLayout());

        // Create and style the button
        JButton startButton = new JButton("START MAZE");
        styleButton(startButton);

        // Call the main of MS if the solve maze button is pressed
        startButton.addActionListener(e -> {
            this.dispose();
            MS.main(new String[0]);
        });

        
        // Create constraints to position the button
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        // This adds 300 pixels of space above the button
        // for reference parameters are (top, left, bottom, right)
        gbc.insets = new Insets(300, 0, 0, 0); 
        
        // Add button to the center
        mainPanel.add(startButton, gbc);

        add(mainPanel);
    }

    /**
     * This method takes a normal JButton and applies some styling
     * 
     * @param btn The button toS be stylized
     */
    private void styleButton(JButton btn) {
        btn.setPreferredSize(new Dimension(200, 60));
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setBackground(new Color(65, 180, 220)); 
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // Used to display the background image
    class BackgroundPanel extends JPanel {
        private BufferedImage img;

        public BackgroundPanel(String path) {
            // Read the image
            try {
                img = ImageIO.read(new File(path));
            } catch (Exception e) {
                System.out.println("Background image not found");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                // Draws the image
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            } else {
                // Solid Color if no image
                g.setColor(new Color(30, 30, 30));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    public static void main(String[] args) {
        // Run the Title Screen
        SwingUtilities.invokeLater(() -> new TitleScreen().setVisible(true));
    }
}