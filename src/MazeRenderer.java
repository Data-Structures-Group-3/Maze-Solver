import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;

/**
 * @brief Renderer and UI controller for the maze application.
 *
 * <p>This class owns the full Swing-driven UX flow:
 * <ul>
 *   <li>Title screen presentation</li>
 *   <li>Maze file selection and load error reporting</li>
 *   <li>Maze grid rendering with wall-edge visualization</li>
 *   <li>Runtime solver controls (start/pause/restart, delay, instant mode)</li>
 *   <li>Zoom handling (buttons and Ctrl+mouse wheel)</li>
 * </ul>
 *
 * <p>Typical usage is through {@link #launchFromTitleScreen()} from the app entrypoint.
 * Example:
 * @code
 * SwingUtilities.invokeLater(MazeRenderer::launchFromTitleScreen);
 * @endcode
 */
public final class MazeRenderer {

    // ---------------------------------------------------------------------
    // Application defaults and theme
    // ---------------------------------------------------------------------

    private static final String TITLE_SCREEN_IMAGE_PATH = "images/TitleImage.jpg";
    private static final String DEFAULT_SESSION_ALGORITHM = "A*";
    private static final boolean DEFAULT_SHOW_NEIGHBORS = true;
    private static final int DEFAULT_SESSION_STEP_DELAY_MS = 200;
    private static final Color DEFAULT_VISITED_COLOR = new Color(0xF4C542);

    private static final Color START_CELL_COLOR = Color.GREEN;
    private static final Color GOAL_CELL_COLOR = Color.BLUE;
    private static final Color BLOCKED_CELL_COLOR = Color.DARK_GRAY;
    private static final Color OPEN_CELL_COLOR = Color.WHITE;
    private static final Color PASSABLE_EDGE_COLOR = new Color(0xF9E8D6);

    private static final int MIN_CELL_SIZE_PX = 6;
    private static final int MAX_CELL_SIZE_PX = 60;
    private static final int ZOOM_STEP_PX = 2;

    // ---------------------------------------------------------------------
    // UI components and runtime session state
    // ---------------------------------------------------------------------

    private static JFrame titleFrame;
    private static JFrame mazeFrame;

    private static Maze renderedMaze;
    private static JLabel[][] renderedCells;
    private static JPanel renderedGridPanel;
    private static JScrollPane renderedGridScrollPane;

    private static JComboBox<String> algorithmSelector;
    private static JSpinner delaySpinner;
    private static JLabel traversedCountLabel;
    private static JCheckBox instantSolveCheckBox;

    private static MazeSolver solver;
    private static Timer solveTimer;
    private static volatile boolean solvingInstantly;
    private static boolean sessionStarted;
    private static int traversedTiles;
    private static Color sessionVisitedColor = DEFAULT_VISITED_COLOR;

    private static String defaultAlgorithm = "DFS";
    private static int defaultStepDelayMs = DEFAULT_SESSION_STEP_DELAY_MS;
    private static int defaultTraversedCount;
    private static int cellSizePx = 18;
    private static boolean applyingControlDefaults;

    /**
     * @brief Utility class; not instantiable.
     */
    private MazeRenderer() {
    }

    // ---------------------------------------------------------------------
    // Public bootstrap API
    // ---------------------------------------------------------------------

    /**
     * @brief Starts the renderer-driven application flow from the title screen.
     *
     * <p>This is the recommended public entrypoint for GUI startup.
     */
    public static void launchFromTitleScreen() {
        showTitleScreen(MazeRenderer::promptForMazeAndRun);
    }

    // ---------------------------------------------------------------------
    // Title and file selection flow
    // ---------------------------------------------------------------------

    /**
     * @brief Displays the title screen and invokes a callback on Start.
     *
     * @param onStartGame callback executed when the Start button is pressed
     */
    private static void showTitleScreen(Runnable onStartGame) {
        if (titleFrame != null) {
            titleFrame.dispose();
        }

        titleFrame = new JFrame("Maze Solver");
        titleFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        titleFrame.setSize(1280, 720);
        titleFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new BackgroundImagePanel(TITLE_SCREEN_IMAGE_PATH);
        mainPanel.setLayout(new GridBagLayout());

        JButton startButton = new JButton("START MAZE");
        styleTitleButton(startButton);
        startButton.addActionListener(e -> {
            titleFrame.dispose();
            titleFrame = null;
            if (onStartGame != null) {
                onStartGame.run();
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(300, 0, 0, 0);

        mainPanel.add(startButton, gbc);
        titleFrame.add(mainPanel);
        titleFrame.setVisible(true);
    }

    /**
     * @brief Opens a maze file, prints diagnostics, and starts an interactive session.
      *
      * <p>Flow:
      * <ol>
      *   <li>Prompt user for a maze text file</li>
      *   <li>Load maze via {@link Maze#importFromFile(String)}</li>
      *   <li>Print diagnostics to console</li>
      *   <li>Create a fresh interactive solve session</li>
      * </ol>
     */
    private static void promptForMazeAndRun() {
        try {
            String inputFile = chooseMazeFile();
            if (inputFile == null) {
                showTitleScreen(MazeRenderer::promptForMazeAndRun);
                return;
            }

            Maze maze = Maze.importFromFile(inputFile);
            printMaze(maze);
            validateMazeNeighbors(maze);
            if (DEFAULT_SHOW_NEIGHBORS) {
                printNeighborTable(maze);
            }

            runInteractiveSolveSession(
                    maze,
                    DEFAULT_SESSION_ALGORITHM,
                    DEFAULT_SESSION_STEP_DELAY_MS,
                    DEFAULT_VISITED_COLOR);
        } catch (Exception ex) {
            System.err.println("Failed to load or print maze: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to load maze: " + ex.getMessage(),
                    "Maze Load Error",
                    JOptionPane.ERROR_MESSAGE);
            showTitleScreen(MazeRenderer::promptForMazeAndRun);
        }
    }

    /**
     * @brief Prompts for a maze text file.
     *
     * @return selected absolute file path, or {@code null} when canceled
     */
    private static String chooseMazeFile() {
        FileDialog openDialog = new FileDialog((Frame) null, "Select Maze File", FileDialog.LOAD);
        openDialog.setDirectory(".");
        openDialog.setFile("*.txt");
        openDialog.setVisible(true);

        String fileName = openDialog.getFile();
        String directory = openDialog.getDirectory();
        if (fileName == null) {
            System.out.println("No File Chosen");
            return null;
        }
        return directory + fileName;
    }

    /**
     * @brief Applies the visual style used by the title Start button.
     *
     * @param button title Start button
     */
    private static void styleTitleButton(JButton button) {
        button.setPreferredSize(new Dimension(200, 60));
        button.setFont(button.getFont().deriveFont(20f));
        button.setBackground(new Color(65, 180, 220));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // ---------------------------------------------------------------------
    // Interactive solve session
    // ---------------------------------------------------------------------

    /**
     * @brief Starts a full interactive solve session for a maze.
      *
      * <p>This method resets renderer runtime state for a new maze file and prepares
      * the timer-driven incremental solver loop.
     *
     * @param maze maze to render and solve
     * @param initialAlgorithm initial algorithm label (DFS/BFS/A*)
     * @param initialStepDelayMs initial delay between solver steps in milliseconds
     * @param visitedColor color used for visited cells
     */
    private static void runInteractiveSolveSession(
            Maze maze,
            String initialAlgorithm,
            int initialStepDelayMs,
            Color visitedColor) {
        if (maze == null) {
            throw new IllegalArgumentException("Maze cannot be null.");
        }

        stopTimerIfRunning();

        sessionVisitedColor = visitedColor != null ? visitedColor : DEFAULT_VISITED_COLOR;
        buildMazeWindow(maze);
        applyControlDefaults(initialAlgorithm, initialStepDelayMs);
        resetTraversalCounter();

        solver = new MazeSolver();
        solver.setMaze(maze);
        solver.setAlg(getSelectedAlgorithmLabel());

        sessionStarted = false;
        solvingInstantly = false;
        solveTimer = new Timer(getSelectedStepDelayMsValue(), e -> onSolveTick());
        refreshRuntimeControlState();
    }

    /**
     * @brief Handles one timer tick of incremental solving.
      *
      * <p>Each tick performs exactly one call to {@link MazeSolver#updateSolve()} and
      * then updates UI state based on the returned node and terminal solver status.
     */
    private static void onSolveTick() {
        syncTimerDelayWithUi();

        Maze.Node visited = solver.updateSolve();
        if (visited != null && visited != renderedMaze.getGoal() && visited != renderedMaze.getStart()) {
            setCellColor(visited.row, visited.col, sessionVisitedColor);
            traversedTiles++;
            setTraversedCount(traversedTiles);
        }

        if (!solver.isFinished()) {
            return;
        }

        stopTimerIfRunning();
        refreshRuntimeControlState();
        if (solver.isSolved()) {
            setCellColors(solver.getFinalPath(), Color.CYAN);
            System.out.println("Solve result: goal reached using " + solver.getAlg()
                    + " after visiting " + solver.getVisitedCount() + " node(s).");
            return;
        }

        System.out.println("Solve result: no path found using " + solver.getAlg()
                + " after visiting " + solver.getVisitedCount() + " node(s).");
    }

    /**
     * @brief Handles Start button action.
        *
        * <p>On first start after a restart, this resets solver search state while keeping
        * current control selections (algorithm, delay, and instant mode).
        * If Instant mode is selected, Start executes a background solve-to-completion.
     */
    private static void handleStartRequested() {
        if (solver == null || solveTimer == null) {
            return;
        }

        if (solvingInstantly) {
            return;
        }

        syncTimerDelayWithUi();
        if (!sessionStarted) {
            solver.setAlg(getSelectedAlgorithmLabel());
            solver.resetSearch();
            resetAllCellColors();
            resetTraversalCounter();
            sessionStarted = true;
        }

        if (isInstantSolveSelected()) {
            runInstantSolve();
            return;
        }

        if (!solver.isFinished() && !solveTimer.isRunning()) {
            solveTimer.start();
            refreshRuntimeControlState();
        }
    }


    /**
     * @brief Handles Restart button action and algorithm changes.
        *
        * <p>This preserves the currently loaded maze while clearing traversal coloring and
        * rebuilding solver state from the current algorithm selection.
     */
    private static void handleRestartRequested() {
        if (solver == null || solveTimer == null) {
            return;
        }

        stopTimerIfRunning();
        solver.setAlg(getSelectedAlgorithmLabel());
        solver.resetSearch();
        resetAllCellColors();
        resetTraversalCounter();
        syncTimerDelayWithUi();
        sessionStarted = false;
        solvingInstantly = false;
        refreshRuntimeControlState();
    }

    /**
     * @brief Runs solve loop to completion without per-step UI updates.
     *
     * <p>The display is updated only when solving has finished, either with a final path
      * (solved) or with an unsolved result. During this run, mode toggling is disabled
      * via {@link #refreshRuntimeControlState()}. Traversed-cell coloring and traversed
      * count semantics match timer mode (start/goal excluded).
     */
    private static void runInstantSolve() {
        if (solver == null || solvingInstantly || !isInstantSolveSelected()) {
            return;
        }

        stopTimerIfRunning();
        solvingInstantly = true;
        refreshRuntimeControlState();

        Thread worker = new Thread(() -> {
            List<Point> traversedPoints = new ArrayList<>();
            while (!solver.isFinished()) {
                Maze.Node visited = solver.updateSolve();
                if (visited != null && visited != renderedMaze.getStart() && visited != renderedMaze.getGoal()) {
                    traversedPoints.add(new Point(visited.col, visited.row));
                }
            }

            boolean solved = solver.isSolved();
            List<Point> finalPath = solved ? solver.getFinalPath() : null;

            SwingUtilities.invokeLater(() -> {
                solvingInstantly = false;
                traversedTiles = setCellColors(traversedPoints, sessionVisitedColor);
                setTraversedCount(traversedTiles);

                if (solved && finalPath != null) {
                    setCellColors(finalPath, Color.CYAN);
                    System.out.println("Solve result: goal reached using " + solver.getAlg()
                            + " after visiting " + solver.getVisitedCount() + " node(s).");
                } else {
                    System.out.println("Solve result: no path found using " + solver.getAlg()
                            + " after visiting " + solver.getVisitedCount() + " node(s).");
                }

                refreshRuntimeControlState();
            });
        }, "maze-instant-solve");

        worker.setDaemon(true);
        worker.start();
    }

    /**
     * @brief Stops the session timer if it is currently running.
     */
    private static void stopTimerIfRunning() {
        if (solveTimer != null && solveTimer.isRunning()) {
            solveTimer.stop();
        }
    }

    /**
     * @brief Resets traversal counter state and UI text.
     */
    private static void resetTraversalCounter() {
        traversedTiles = 0;
        setTraversedCount(0);
    }

    // ---------------------------------------------------------------------
    // Maze window construction
    // ---------------------------------------------------------------------

    /**
     * @brief Builds and displays the maze window with controls.
      *
      * <p>Large mazes are displayed inside a scroll pane and the frame is clamped to
      * available screen bounds.
     *
     * @param maze maze to render
     */
    private static void buildMazeWindow(Maze maze) {
        int rows = maze.getLength();
        int cols = maze.getWidth();

        if (mazeFrame != null) {
            mazeFrame.dispose();
        }

        renderedMaze = maze;
        renderedCells = new JLabel[rows][cols];

        renderedGridPanel = new JPanel(new GridLayout(rows, cols));
        buildGridCells(rows, cols);

        JPanel controlPanel = buildControlPanel();

        renderedGridScrollPane = new JScrollPane(
                renderedGridPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        renderedGridScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        renderedGridScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        renderedGridScrollPane.addMouseWheelListener(e -> {
            if (!e.isControlDown()) {
                return;
            }
            if (e.getWheelRotation() < 0) {
                adjustZoom(1);
            } else if (e.getWheelRotation() > 0) {
                adjustZoom(-1);
            }
            e.consume();
        });

        mazeFrame = new JFrame("Maze Map");
        mazeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mazeFrame.setLayout(new BorderLayout());
        mazeFrame.add(renderedGridScrollPane, BorderLayout.CENTER);
        mazeFrame.add(controlPanel, BorderLayout.SOUTH);
        mazeFrame.pack();

        clampAndCenterMazeFrame(rows, cols, controlPanel);
        mazeFrame.setVisible(true);
    }

    /**
     * @brief Creates all maze cell labels and applies initial styling.
     *
     * @param rows maze row count
     * @param cols maze column count
     */
    private static void buildGridCells(int rows, int cols) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                JLabel cell = new JLabel();
                cell.setOpaque(true);
                cell.setPreferredSize(new Dimension(cellSizePx, cellSizePx));

                applyDefaultCellAppearance(cell, renderedMaze, row, col);
                applyCellBorder(cell, row, col, rows, cols);

                renderedCells[row][col] = cell;
                renderedGridPanel.add(cell);
            }
        }
    }

    /**
     * @brief Builds the bottom control panel and wires listeners.
         *
         * <p>Control semantics:
         * <ul>
         *   <li><b>Start</b>: begin or continue stepping</li>
         *   <li><b>Pause</b>: stop timer without resetting search</li>
         *   <li><b>Restart</b>: reset search and recolor cells</li>
         *   <li><b>Step Delay</b>: 1..5000 ms for timer-driven solving</li>
         *   <li><b>Instant</b>: solve to completion with timer-mode-equivalent final output</li>
         *   <li><b>Zoom +/-</b>: adjust cell size in fixed increments</li>
         * </ul>
     *
     * @return constructed control panel
     */
    private static JPanel buildControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        JButton startButton = new JButton("Start");
        JButton pauseButton = new JButton("Pause");
        JButton restartButton = new JButton("Restart");
        JButton zoomOutButton = new JButton("-");
        JButton zoomInButton = new JButton("+");

        JLabel algorithmLabel = new JLabel("Algorithm:");
        JLabel delayLabel = new JLabel("Step Delay (ms):");
        JLabel instantSolveLabel = new JLabel("Instant:");
        JLabel zoomLabel = new JLabel("Zoom:");

        algorithmSelector = new JComboBox<>(new String[] { "DFS", "BFS", "A*" });
        delaySpinner = new JSpinner(new SpinnerNumberModel(defaultStepDelayMs, 1, 5000, 1));
        instantSolveCheckBox = new JCheckBox();
        traversedCountLabel = new JLabel("Traversed: " + defaultTraversedCount);

        algorithmSelector.addActionListener(e -> {
            if (!applyingControlDefaults) {
                handleRestartRequested();
            }
        });
        instantSolveCheckBox.addActionListener(e -> {
            if (solveTimer != null && solveTimer.isRunning()) {
                instantSolveCheckBox.setSelected(false);
            }
            refreshRuntimeControlState();
        });

        startButton.addActionListener(e -> handleStartRequested());
        pauseButton.addActionListener(e -> {
            stopTimerIfRunning();
            refreshRuntimeControlState();
        });
        restartButton.addActionListener(e -> handleRestartRequested());
        zoomOutButton.addActionListener(e -> adjustZoom(-1));
        zoomInButton.addActionListener(e -> adjustZoom(1));

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(restartButton);
        controlPanel.add(algorithmLabel);
        controlPanel.add(algorithmSelector);
        controlPanel.add(delayLabel);
        controlPanel.add(delaySpinner);
        controlPanel.add(instantSolveLabel);
        controlPanel.add(instantSolveCheckBox);
        controlPanel.add(zoomLabel);
        controlPanel.add(zoomOutButton);
        controlPanel.add(zoomInButton);
        controlPanel.add(traversedCountLabel);

        refreshRuntimeControlState();

        return controlPanel;
    }

    /**
     * @brief Sizes maze window while keeping it within current screen bounds.
     *
     * @param rows maze row count
     * @param cols maze column count
     * @param controlPanel control panel used to estimate preferred height
     */
    private static void clampAndCenterMazeFrame(int rows, int cols, JPanel controlPanel) {
        Dimension preferredSize = mazeFrame.getPreferredSize();
        int controlsHeight = controlPanel.getPreferredSize().height;
        int verticalPadding = 36;

        int initialWidth = Math.max(cols * 35, preferredSize.width);
        int initialHeight = Math.max(rows * 35 + controlsHeight + verticalPadding, preferredSize.height);

        Rectangle maxWindowBounds = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();

        int clampedWidth = Math.min(initialWidth, maxWindowBounds.width);
        int clampedHeight = Math.min(initialHeight, maxWindowBounds.height);

        mazeFrame.setSize(new Dimension(clampedWidth, clampedHeight));
        mazeFrame.setLocation(
            maxWindowBounds.x + (maxWindowBounds.width - clampedWidth) / 2,
            maxWindowBounds.y + (maxWindowBounds.height - clampedHeight) / 2);
    }

    /**
     * @brief Applies edge border colors based on connectivity and boundaries.
     *
     * @param cell target cell label
     * @param row cell row
     * @param col cell column
     * @param rows total row count
     * @param cols total column count
     */
    private static void applyCellBorder(JLabel cell, int row, int col, int rows, int cols) {
        Maze.Node node = renderedMaze.getNode(row, col);

        Color topColor = PASSABLE_EDGE_COLOR;
        Color rightColor = PASSABLE_EDGE_COLOR;
        Color bottomColor = PASSABLE_EDGE_COLOR;
        Color leftColor = PASSABLE_EDGE_COLOR;
        int thickness = 1;

        if (node == null) {
            topColor = rightColor = bottomColor = leftColor = Color.BLACK;
        } else {
            if (row == 0 || node.getNeighbor(Maze.Dir.NORTH) == null) {
                topColor = Color.BLACK;
            }
            if (row == rows - 1 || node.getNeighbor(Maze.Dir.SOUTH) == null) {
                bottomColor = Color.BLACK;
            }
            if (col == 0 || node.getNeighbor(Maze.Dir.WEST) == null) {
                leftColor = Color.BLACK;
            }
            if (col == cols - 1 || node.getNeighbor(Maze.Dir.EAST) == null) {
                rightColor = Color.BLACK;
            }
        }

        cell.setBorder(new MazeEdgeBorder(
                thickness,
                thickness,
                thickness,
                thickness,
                topColor,
                leftColor,
                bottomColor,
                rightColor));
    }

    // ---------------------------------------------------------------------
    // Control values and normalization
    // ---------------------------------------------------------------------

    /**
     * @brief Applies initial algorithm and delay defaults to current controls.
     *
     * @param algorithm preferred algorithm label
     * @param stepDelayMs preferred delay in milliseconds
     */
    private static void applyControlDefaults(String algorithm, int stepDelayMs) {
        if (algorithm != null && !algorithm.trim().isEmpty()) {
            defaultAlgorithm = normalizeAlgorithmLabel(algorithm);
        }
        defaultStepDelayMs = Math.max(1, stepDelayMs);

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
        refreshRuntimeControlState();
    }

    /**
     * @brief Reads currently selected algorithm from controls.
     *
     * @return canonical algorithm label
     */
    private static String getSelectedAlgorithmLabel() {
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
     * @brief Reads currently selected delay from controls.
     *
        * @return delay in milliseconds (minimum 1)
     */
    private static int getSelectedStepDelayMsValue() {
        if (delaySpinner == null) {
            return defaultStepDelayMs;
        }

        Object value = delaySpinner.getValue();
        if (value instanceof Number) {
            return Math.max(1, ((Number) value).intValue());
        }
        return defaultStepDelayMs;
    }

    /**
     * @brief Indicates whether instant-solve mode is selected in the UI.
     *
     * @return true if instant-solve checkbox is selected
     */
    private static boolean isInstantSolveSelected() {
        return instantSolveCheckBox != null && instantSolveCheckBox.isSelected();
    }

    /**
     * @brief Refreshes runtime control enablement based on current solve state.
     *
     * <p>Rules:
     * <ul>
      *   <li>Instant solve cannot be toggled while a solve is actively running</li>
     *   <li>Step delay spinner is disabled whenever instant solve is selected</li>
      *   <li>Both rules apply for timer mode and instant worker mode</li>
     * </ul>
     */
    private static void refreshRuntimeControlState() {
        boolean timerRunning = solveTimer != null && solveTimer.isRunning();
        boolean activelySolving = timerRunning || solvingInstantly;

        if (instantSolveCheckBox != null) {
            instantSolveCheckBox.setEnabled(!activelySolving);
        }

        if (delaySpinner != null) {
            delaySpinner.setEnabled(!isInstantSolveSelected());
        }
    }

    /**
     * @brief Updates timer delay to match current UI value.
     */
    private static void syncTimerDelayWithUi() {
        if (solveTimer == null) {
            return;
        }

        int selectedDelay = getSelectedStepDelayMsValue();
        if (solveTimer.getDelay() == selectedDelay) {
            return;
        }

        solveTimer.setDelay(selectedDelay);
        solveTimer.setInitialDelay(selectedDelay);
    }

    /**
     * @brief Normalizes an algorithm label to DFS, BFS, or A*.
     *
     * @param algorithm input label
     * @return canonical label
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

    // ---------------------------------------------------------------------
    // Cell color and appearance helpers
    // ---------------------------------------------------------------------

    /**
     * @brief Sets traversed-count label text.
     *
     * @param traversedCount traversed tile count
     */
    private static void setTraversedCount(int traversedCount) {
        defaultTraversedCount = Math.max(0, traversedCount);
        if (traversedCountLabel == null) {
            return;
        }

        SwingUtilities.invokeLater(
                () -> traversedCountLabel.setText("Traversed: " + defaultTraversedCount));
    }

    /**
     * @brief Sets one rendered cell color.
     *
     * @param row row index
     * @param col column index
     * @param color target color
     * @return true when update was accepted
     */
    private static boolean setCellColor(int row, int col, Color color) {
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
     * @brief Sets multiple rendered cell colors.
     *
     * @param cells cells where {@code x=col, y=row}
     * @param color color to apply
     * @return number of accepted updates
     */
    private static int setCellColors(List<Point> cells, Color color) {
        if (cells == null || color == null) {
            return 0;
        }

        int updated = 0;
        for (Point point : cells) {
            if (point == null) {
                continue;
            }

            if (setCellColor(point.y, point.x, color)) {
                updated++;
            }
        }
        return updated;
    }

    /**
     * @brief Resets all rendered cells to their default maze-based style.
     */
    private static void resetAllCellColors() {
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
     * @brief Applies default cell appearance from maze semantics.
     *
     * @param cell target label
     * @param maze source maze
     * @param row row index
     * @param col column index
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

    /**
     * @brief Returns true when renderer cache has been initialized.
     *
     * @return true when maze and cell grid exist
     */
    private static boolean isRendererReady() {
        return renderedMaze != null && renderedCells != null;
    }

    // ---------------------------------------------------------------------
    // Zoom helpers
    // ---------------------------------------------------------------------

    /**
     * @brief Adjusts zoom by one configured step.
     *
     * @param direction +1 zoom in, -1 zoom out
     */
    private static void adjustZoom(int direction) {
        if (direction == 0) {
            return;
        }
        setCellSize(cellSizePx + (direction * ZOOM_STEP_PX));
    }

    /**
     * @brief Applies a new cell size while preserving viewport center.
      *
      * <p>The viewport keeps approximately the same visual center after zoom so users do
      * not lose context while navigating large mazes.
     *
     * @param requestedCellSizePx requested cell size in pixels
     */
    private static void setCellSize(int requestedCellSizePx) {
        if (!isRendererReady() || renderedGridPanel == null || renderedGridScrollPane == null) {
            cellSizePx = clampCellSize(requestedCellSizePx);
            return;
        }

        int clampedSize = clampCellSize(requestedCellSizePx);
        if (clampedSize == cellSizePx) {
            return;
        }

        int oldSize = cellSizePx;
        cellSizePx = clampedSize;

        JViewport viewport = renderedGridScrollPane.getViewport();
        Rectangle oldViewRect = viewport.getViewRect();
        double oldCenterX = oldViewRect.getCenterX();
        double oldCenterY = oldViewRect.getCenterY();

        Dimension newCellSize = new Dimension(cellSizePx, cellSizePx);
        for (int row = 0; row < renderedMaze.getLength(); row++) {
            for (int col = 0; col < renderedMaze.getWidth(); col++) {
                JLabel cell = renderedCells[row][col];
                if (cell != null) {
                    cell.setPreferredSize(newCellSize);
                }
            }
        }

        renderedGridPanel.revalidate();
        renderedGridPanel.repaint();

        double scale = ((double) cellSizePx) / oldSize;
        int targetCenterX = (int) Math.round(oldCenterX * scale);
        int targetCenterY = (int) Math.round(oldCenterY * scale);

        SwingUtilities.invokeLater(() -> {
            Dimension viewSize = viewport.getExtentSize();
            Dimension fullSize = viewport.getViewSize();

            int targetX = targetCenterX - (viewSize.width / 2);
            int targetY = targetCenterY - (viewSize.height / 2);
            targetX = Math.max(0, Math.min(targetX, Math.max(0, fullSize.width - viewSize.width)));
            targetY = Math.max(0, Math.min(targetY, Math.max(0, fullSize.height - viewSize.height)));

            viewport.setViewPosition(new Point(targetX, targetY));
        });
    }

    /**
     * @brief Clamps a requested cell size to supported bounds.
     *
     * @param requestedCellSizePx requested cell size
     * @return clamped size
     */
    private static int clampCellSize(int requestedCellSizePx) {
        return Math.max(MIN_CELL_SIZE_PX, Math.min(MAX_CELL_SIZE_PX, requestedCellSizePx));
    }

    // ---------------------------------------------------------------------
    // Diagnostics and debug output
    // ---------------------------------------------------------------------

    /**
     * @brief Prints maze dimensions, start/end coordinates, and occupancy grid.
      *
      * <p>Intended for debug/validation workflows to verify import correctness.
     *
     * @param maze maze to print
     */
    private static void printMaze(Maze maze) {
        System.out.println(maze.getLength());
        System.out.println(maze.getWidth());

        Maze.Node start = maze.getStart();
        Maze.Node goal = maze.getGoal();

        System.out.println(start == null ? "start: unset" : "start: " + start.row + "," + start.col);
        System.out.println(goal == null ? "end: unset" : "end: " + goal.row + "," + goal.col);

        for (int row = 0; row < maze.getLength(); row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < maze.getWidth(); col++) {
                line.append(maze.isOccupied(row, col) ? '1' : '0');
            }
            System.out.println(line);
        }
    }

    /**
     * @brief Validates maze neighbor consistency and prints issues.
     *
     * @param maze maze to validate
     */
    private static void validateMazeNeighbors(Maze maze) {
        final int maxPrintedIssues = 25;
        Maze.NeighborValidationResult result = maze.validateNeighborConnections(maxPrintedIssues);

        if (result.isValid()) {
            System.out.println("Neighbor validation: PASSED");
            return;
        }

        for (String issue : result.getIssues()) {
            System.out.println(issue);
        }

        System.out.println("Neighbor validation: FAILED (" + result.getIssueCount() + " issue(s))");
        if (result.getIssueCount() > maxPrintedIssues) {
            System.out.println("Only first " + maxPrintedIssues + " issue(s) shown.");
        }
    }

    /**
     * @brief Prints compact neighbor table lines for occupied cells.
     *
     * @param maze maze whose neighbor table is printed
     */
    private static void printNeighborTable(Maze maze) {
        for (String line : maze.buildNeighborTableLines()) {
            System.out.println(line);
        }
    }


    
    // ---------------------------------------------------------------------
    // Internal UI support classes
    // ---------------------------------------------------------------------

    /**
     * @brief Border implementation with independently colored sides.
     */
    private static class MazeEdgeBorder extends AbstractBorder {
        private final int top;
        private final int left;
        private final int bottom;
        private final int right;
        private final Color topColor;
        private final Color leftColor;
        private final Color bottomColor;
        private final Color rightColor;

        /**
         * @brief Creates a border with per-side thickness and color.
         *
         * @param top top edge thickness in pixels
         * @param left left edge thickness in pixels
         * @param bottom bottom edge thickness in pixels
         * @param right right edge thickness in pixels
         * @param topColor top edge color
         * @param leftColor left edge color
         * @param bottomColor bottom edge color
         * @param rightColor right edge color
         */
        MazeEdgeBorder(
                int top,
                int left,
                int bottom,
                int right,
                Color topColor,
                Color leftColor,
                Color bottomColor,
                Color rightColor) {
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
         * @brief Returns insets matching side thickness values.
         *
         * @param component component using this border
         * @return border insets
         */
        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(top, left, bottom, right);
        }

        /**
         * @brief Paints all border edges with their configured colors.
         */
        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
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
     * @brief JPanel that paints a stretched background image.
     */
    private static class BackgroundImagePanel extends JPanel {
        private final BufferedImage backgroundImage;

        /**
         * @brief Creates a panel backed by the image at the given path.
         *
         * @param imagePath path to image file
         */
        BackgroundImagePanel(String imagePath) {
            BufferedImage loaded = null;
            try {
                loaded = ImageIO.read(new File(imagePath));
            } catch (Exception ignored) {
                loaded = null;
            }
            this.backgroundImage = loaded;
        }

        /**
         * @brief Paints stretched image or fallback solid background.
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (backgroundImage != null) {
                graphics.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                return;
            }

            graphics.setColor(new Color(30, 30, 30));
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
