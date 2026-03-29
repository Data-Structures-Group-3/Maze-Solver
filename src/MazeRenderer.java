import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/**
 * Renders a {@link Maze} into a Swing window and exposes UI controls for
 * stepping solver execution.
 *
 * <p>The renderer owns:
 * <ul>
 *   <li>Grid drawing (cells + walls)</li>
 *   <li>Control panel (Start, Pause, Restart)</li>
 *   <li>Runtime controls (algorithm selector, step delay, traversed counter)</li>
 *   <li>Automatic restart trigger when algorithm selection changes</li>
 * </ul>
 */
public class MazeRenderer {
    private static final Color START_CELL_COLOR = Color.GREEN;
    private static final Color GOAL_CELL_COLOR = Color.BLUE;
    private static final Color BLOCKED_CELL_COLOR = Color.DARK_GRAY;
    private static final Color OPEN_CELL_COLOR = Color.WHITE;

    private static Maze renderedMaze;
    private static JLabel[][] renderedCells;
    private static JFrame mazeFrame;
    private static Runnable onStart;
    private static Runnable onPause;
    private static Runnable onRestart;
    private static JComboBox<String> algorithmSelector;
    private static JSpinner delaySpinner;
    private static JLabel traversedCountLabel;
    private static boolean applyingControlDefaults = false;
    private static String defaultAlgorithm = "DFS";
    private static int defaultStepDelayMs = 200;
    private static int defaultTraversedCount = 0;

    /**
     * Border implementation that supports independent thickness and color for
     * each side of a cell.
     */
    private static class MazeEdgeBorder extends AbstractBorder {
        private final int top, left, bottom, right;
        private final Color topColor, leftColor, bottomColor, rightColor;

        /**
         * Creates an edge-aware border with per-side thickness and color.
         *
         * @param top thickness of the top edge in pixels
         * @param left thickness of the left edge in pixels
         * @param bottom thickness of the bottom edge in pixels
         * @param right thickness of the right edge in pixels
         * @param topColor color used to paint the top edge
         * @param leftColor color used to paint the left edge
         * @param bottomColor color used to paint the bottom edge
         * @param rightColor color used to paint the right edge
         */
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

        /**
         * Returns border insets equal to configured side thickness values.
         *
         * @param c component this border is attached to
         * @return insets in top, left, bottom, right order
         */
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(top, left, bottom, right);
        }

        /**
         * Paints each border side with its configured color.
         *
         * @param c component currently being painted
         * @param g graphics context supplied by Swing
         * @param x left coordinate of border area
         * @param y top coordinate of border area
         * @param width border paint area width
         * @param height border paint area height
         */
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
     * Displays the maze in a GUI window.
     *
     * <p>Cell color mapping:
     * <ul>
     *   <li>Start: green with {@code S}</li>
     *   <li>Goal: blue with {@code G}</li>
     *   <li>Blocked/unoccupied: dark gray</li>
     *   <li>Traversable: white</li>
     * </ul>
     *
     * <p>Edge color mapping:
     * <ul>
     *   <li>Black edge: wall or outer maze boundary</li>
     *   <li>Light edge: passable adjacency</li>
     * </ul>
     *
    * @param maze maze to render; expected to have dimensions initialized and
    *             path cells represented as non-null nodes
     */
    public static void showMaze(Maze maze) {
        int rows = maze.getLength();
        int cols = maze.getWidth();

        if (mazeFrame != null) {
            mazeFrame.dispose();
        }

        renderedMaze = maze;
        renderedCells = new JLabel[rows][cols];

        JPanel gridPanel = new JPanel(new GridLayout(rows, cols));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JLabel cell = new JLabel();
                cell.setOpaque(true);

                Maze.Node node = maze.getNode(r, c);
                Maze.Node start = maze.getStart();
                Maze.Node goal = maze.getGoal();

                if (start != null && start.row == r && start.col == c) {
                    cell.setBackground(START_CELL_COLOR);
                    cell.setText("S");
                    cell.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (goal != null && goal.row == r && goal.col == c) {
                    cell.setBackground(GOAL_CELL_COLOR);
                    cell.setText("G");
                    cell.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (!maze.isOccupied(r, c) || node == null) {
                    cell.setBackground(BLOCKED_CELL_COLOR);
                } else {
                    cell.setBackground(OPEN_CELL_COLOR);
                }

                Color passableEdge = new Color(0xF9E8D6);
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
                renderedCells[r][c] = cell;
            }
        }

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton startButton = new JButton("Start");
        JButton pauseButton = new JButton("Pause");
        JButton restartButton = new JButton("Restart");
        JLabel algorithmLabel = new JLabel("Algorithm:");
        JLabel delayLabel = new JLabel("Step Delay (ms):");
        algorithmSelector = new JComboBox<>(new String[] { "DFS", "BFS", "A*" });
        delaySpinner = new JSpinner(new SpinnerNumberModel(defaultStepDelayMs, 10, 5000, 10));
        traversedCountLabel = new JLabel("Traversed: " + defaultTraversedCount);
        algorithmSelector.setSelectedItem(defaultAlgorithm);

        algorithmSelector.addActionListener(e -> {
            if (!applyingControlDefaults && onRestart != null) {
                onRestart.run();
            }
        });

        startButton.addActionListener(e -> {
            if (onStart != null) {
                onStart.run();
            }
        });
        pauseButton.addActionListener(e -> {
            if (onPause != null) {
                onPause.run();
            }
        });
        restartButton.addActionListener(e -> {
            if (onRestart != null) {
                onRestart.run();
            }
        });

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(restartButton);
        controlPanel.add(algorithmLabel);
        controlPanel.add(algorithmSelector);
        controlPanel.add(delayLabel);
        controlPanel.add(delaySpinner);
        controlPanel.add(traversedCountLabel);

        mazeFrame = new JFrame("Maze Map");
        mazeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mazeFrame.setLayout(new BorderLayout());
        mazeFrame.add(gridPanel, BorderLayout.CENTER);
        mazeFrame.add(controlPanel, BorderLayout.SOUTH);
        mazeFrame.pack();

        // Keep initial size large enough for both grid and control panel.
        Dimension preferredSize = mazeFrame.getPreferredSize();
        int controlsHeight = controlPanel.getPreferredSize().height;
        int verticalPadding = 36;
        int initialWidth = Math.max(cols * 35, preferredSize.width);
        int initialHeight = Math.max(rows * 35 + controlsHeight + verticalPadding, preferredSize.height);
        Dimension initialSize = new Dimension(initialWidth, initialHeight);

        mazeFrame.setMinimumSize(preferredSize);
        mazeFrame.setSize(initialSize);
        mazeFrame.setLocationRelativeTo(null);
        mazeFrame.setVisible(true);
    }

    /**
     * Registers callbacks used by the control buttons.
        *
        * <p>The restart callback is also used when a user changes the algorithm
        * selection in the runtime controls.
     *
     * @param startAction callback executed when Start is pressed
     * @param pauseAction callback executed when Pause is pressed
     * @param restartAction callback executed when Restart is pressed
     */
    public static void setControlActions(Runnable startAction, Runnable pauseAction, Runnable restartAction) {
        onStart = startAction;
        onPause = pauseAction;
        onRestart = restartAction;
    }

    /**
     * Configures default control values.
     *
     * <p>If controls are already visible, their values are updated immediately.
     *
     * @param algorithm default algorithm label (DFS, BFS, or A*)
     * @param stepDelayMs default solver step delay in milliseconds
     */
    public static void configureControls(String algorithm, int stepDelayMs) {
        if (algorithm != null && !algorithm.trim().isEmpty()) {
            defaultAlgorithm = normalizeAlgorithmLabel(algorithm);
        }
        defaultStepDelayMs = Math.max(10, stepDelayMs);

        if (algorithmSelector != null) {
            applyingControlDefaults = true;
            try {
                algorithmSelector.setSelectedItem(defaultAlgorithm);
            } finally {
                applyingControlDefaults = false;
            }
        }
        if (delaySpinner != null) {
            delaySpinner.setValue(defaultStepDelayMs);
        }
    }

    /**
     * Reads current algorithm selection from the GUI.
     *
     * @return selected algorithm label (DFS, BFS, or A*)
     */
    public static String getSelectedAlgorithm() {
        if (algorithmSelector == null) {
            return defaultAlgorithm;
        }
        Object selected = algorithmSelector.getSelectedItem();
        if (selected == null) {
            return defaultAlgorithm;
        }
        return normalizeAlgorithmLabel(selected.toString());
    }

    /**
     * Reads current step-delay value from the GUI.
     *
     * @return delay in milliseconds (minimum enforced at 10)
     */
    public static int getSelectedStepDelayMs() {
        if (delaySpinner == null) {
            return defaultStepDelayMs;
        }

        Object value = delaySpinner.getValue();
        if (value instanceof Number) {
            return Math.max(10, ((Number) value).intValue());
        }
        return defaultStepDelayMs;
    }

    /**
     * Updates the traversed tiles counter text in the GUI.
     *
     * @param traversedCount number of traversed tiles to display
     */
    public static void setTraversedCount(int traversedCount) {
        defaultTraversedCount = Math.max(0, traversedCount);
        if (traversedCountLabel == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> traversedCountLabel.setText("Traversed: " + defaultTraversedCount));
    }

    /**
     * Updates one rendered cell color.
     *
     * @param row row index of the target cell
     * @param col column index of the target cell
     * @param color new background color
     * @return true if update was accepted; false if renderer is not ready or coordinates are invalid
     */
    public static boolean setCellColor(int row, int col, Color color) {
        if (!isRendererReady() || color == null || !renderedMaze.isInBounds(row, col)) {
            return false;
        }

        JLabel cell = renderedCells[row][col];
        if (cell == null) {
            return false;
        }

        SwingUtilities.invokeLater(() -> {
            cell.setBackground(color);
            cell.repaint();
        });
        return true;
    }

    /**
     * Updates multiple rendered cell colors.
     *
     * @param cells points to recolor where {@code x=col} and {@code y=row}
     * @param color color to apply to each valid point
     * @return number of cells scheduled for update
     */
    public static int setCellColors(List<Point> cells, Color color) {
        if (cells == null || color == null) {
            return 0;
        }

        int updated = 0;
        for (Point p : cells) {
            if (p == null) {
                continue;
            }

            if (setCellColor(p.y, p.x, color)) {
                updated++;
            }
        }
        return updated;
    }

    /**
     * Resets one rendered cell to its default maze-driven appearance.
     *
     * @param row row index of the cell to reset
     * @param col column index of the cell to reset
     * @return true if reset was accepted; false if renderer is not ready or coordinates are invalid
     */
    public static boolean resetCellColor(int row, int col) {
        if (!isRendererReady() || !renderedMaze.isInBounds(row, col)) {
            return false;
        }

        JLabel cell = renderedCells[row][col];
        if (cell == null) {
            return false;
        }

        SwingUtilities.invokeLater(() -> {
            applyDefaultCellAppearance(cell, renderedMaze, row, col);
            cell.repaint();
        });
        return true;
    }

    /**
     * Resets all rendered cells to default maze-driven appearance.
     */
    public static void resetAllCellColors() {
        if (!isRendererReady()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < renderedMaze.getLength(); row++) {
                for (int col = 0; col < renderedMaze.getWidth(); col++) {
                    JLabel cell = renderedCells[row][col];
                    if (cell != null) {
                        applyDefaultCellAppearance(cell, renderedMaze, row, col);
                    }
                }
            }
            if (mazeFrame != null) {
                mazeFrame.repaint();
            }
        });
    }

    /**
     * Checks whether renderer state has been initialized by {@link #showMaze(Maze)}.
     *
     * @return true if maze and cell cache are available
     */
    private static boolean isRendererReady() {
        return renderedMaze != null && renderedCells != null;
    }

    /**
     * Normalizes algorithm labels used by UI and solver configuration.
     *
     * @param algorithm input algorithm label
     * @return canonical label (DFS, BFS, or A*)
     */
    private static String normalizeAlgorithmLabel(String algorithm) {
        String normalized = algorithm.trim().toUpperCase();
        if ("A_STAR".equals(normalized) || "ASTAR".equals(normalized)) {
            return "A*";
        }
        if ("A*".equals(normalized) || "BFS".equals(normalized) || "DFS".equals(normalized)) {
            return normalized;
        }
        return "DFS";
    }

    /**
     * Applies default visual styling for one cell based on maze semantics.
     *
     * @param cell label component to style
     * @param maze source maze for occupancy/start/goal lookup
     * @param row row index of the target cell
     * @param col column index of the target cell
     */
    private static void applyDefaultCellAppearance(JLabel cell, Maze maze, int row, int col) {
        Maze.Node start = maze.getStart();
        Maze.Node goal = maze.getGoal();
        Maze.Node node = maze.getNode(row, col);

        if (start != null && start.row == row && start.col == col) {
            cell.setBackground(START_CELL_COLOR);
            cell.setText("S");
            cell.setHorizontalAlignment(SwingConstants.CENTER);
        } else if (goal != null && goal.row == row && goal.col == col) {
            cell.setBackground(GOAL_CELL_COLOR);
            cell.setText("G");
            cell.setHorizontalAlignment(SwingConstants.CENTER);
        } else if (!maze.isOccupied(row, col) || node == null) {
            cell.setBackground(BLOCKED_CELL_COLOR);
            cell.setText("");
        } else {
            cell.setBackground(OPEN_CELL_COLOR);
            cell.setText("");
        }
    }
}
