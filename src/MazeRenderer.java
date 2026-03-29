import java.awt.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/**
 * Responsible for rendering and displaying the maze visually.
 */
public class MazeRenderer {

    private static class MazeEdgeBorder extends AbstractBorder {
        private final int top, left, bottom, right;
        private final Color topColor, leftColor, bottomColor, rightColor;

        MazeEdgeBorder(int top, int left, int bottom, int right,
                       Color topColor, Color leftColor, Color bottomColor, Color rightColor) {
            this.top = top;
            this.left = left;
            this.bottom = bottom;
            this.right = right;
            this.topColor = topColor;
            this.leftColor = leftColor;
            this.bottomColor = bottomColor;
            this.rightColor = rightColor;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(top, left, bottom, right);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                if (top > 0) {
                    g2.setColor(topColor);
                    g2.fillRect(x, y, width, top);
                }
                if (left > 0) {
                    g2.setColor(leftColor);
                    g2.fillRect(x, y, left, height);
                }
                if (bottom > 0) {
                    g2.setColor(bottomColor);
                    g2.fillRect(x, y + height - bottom, width, bottom);
                }
                if (right > 0) {
                    g2.setColor(rightColor);
                    g2.fillRect(x + width - right, y, right, height);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Display the maze in a GUI window.
     */
    public static void showMaze(Maze maze) {
        int rows = maze.getLength();
        int cols = maze.getWidth();

        JPanel gridPanel = new JPanel(new GridLayout(rows, cols));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JLabel cell = new JLabel();
                cell.setOpaque(true);

                Maze.Node node = maze.getNode(r, c);
                Maze.Node start = maze.getStart();
                Maze.Node goal = maze.getGoal();

                if (start != null && start.row == r && start.col == c) {
                    cell.setBackground(Color.GREEN);
                    cell.setText("S");
                    cell.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (goal != null && goal.row == r && goal.col == c) {
                    cell.setBackground(Color.BLUE);
                    cell.setText("G");
                    cell.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (!maze.isOccupied(r, c) || node == null) {
                    cell.setBackground(Color.DARK_GRAY);
                } else {
                    cell.setBackground(Color.WHITE);
                }

                Color passableEdge = new Color(0xF9E8D6); // very light brown
                Color topColor = passableEdge;
                Color rightColor = passableEdge;
                Color bottomColor = passableEdge;
                Color leftColor = passableEdge;
                int thickness = 1;

                if (node == null) {
                    topColor = rightColor = bottomColor = leftColor = Color.BLACK;
                } else {
                    if (r == 0 || node.getNeighbor(Maze.Dir.NORTH) == null) {
                        topColor = Color.BLACK;
                    }
                    if (r == rows - 1 || node.getNeighbor(Maze.Dir.SOUTH) == null) {
                        bottomColor = Color.BLACK;
                    }
                    if (c == 0 || node.getNeighbor(Maze.Dir.WEST) == null) {
                        leftColor = Color.BLACK;
                    }
                    if (c == cols - 1 || node.getNeighbor(Maze.Dir.EAST) == null) {
                        rightColor = Color.BLACK;
                    }
                }

                cell.setBorder(new MazeEdgeBorder(thickness, thickness, thickness, thickness,
                        topColor, leftColor, bottomColor, rightColor));
                gridPanel.add(cell);
            }
        }

        JFrame frame = new JFrame("Maze Map");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(gridPanel);
        frame.pack();
        frame.setSize(new Dimension(cols * 35, rows * 35));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}