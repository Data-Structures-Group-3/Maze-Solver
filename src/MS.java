import java.awt.Color;
import javax.swing.Timer;

/**
 * Demo application entry point for loading, visualizing, and step-solving a maze.
 *
 * <p>This class wires:
 * <ul>
 *   <li>Maze file import</li>
 *   <li>Optional debug output</li>
 *   <li>GUI rendering and controls</li>
 *   <li>Incremental solver stepping through a Swing timer</li>
 * </ul>
 */
public class MS {

    /**
     * Starts the maze demo.
     *
     * <p>Runtime behavior:
     * <ol>
     *   <li>Loads maze from file</li>
     *   <li>Displays maze + control panel</li>
     *   <li>Runs selected solver algorithm one step per timer tick</li>
     *   <li>Restarts solver view/state when algorithm selection changes</li>
     * </ol>
     *
     * @param args command line arguments (currently unused)
     */
    public static void main(String[] args) {
        String inputFile = "test.txt";
        String algorithm = "A*";
        boolean showNeighbors = true;
        int stepDelayMs = 200;
        Color visitedColor = new Color(0xF4C542);

        try {
            Maze maze = Maze.importFromFile(inputFile);
            printMaze(maze);
            validateMazeNeighbors(maze);

            if (showNeighbors) {
                printNeighborTable(maze);
            }

            MazeRenderer.showMaze(maze);
            MazeRenderer.configureControls(algorithm, stepDelayMs);
            MazeRenderer.setTraversedCount(0);

            MazeSolver solver = new MazeSolver();
            solver.setAlg(algorithm);
            solver.setMaze(maze);

            final boolean[] hasStarted = new boolean[] { false };
            final int[] traversedTiles = new int[] { 0 };
            final Timer[] solveTimer = new Timer[1];
            solveTimer[0] = new Timer(stepDelayMs, e -> {
                Maze.Node visited = solver.updateSolve();
                if (visited != null && visited != maze.getGoal() && visited != maze.getStart()) {
                    MazeRenderer.setCellColor(visited.row, visited.col, visitedColor);
                    traversedTiles[0]++;
                    MazeRenderer.setTraversedCount(traversedTiles[0]);
                }

                if (solver.isFinished()) {
                    solveTimer[0].stop();
                    if (solver.isSolved()) {
                        System.out.println("Solve result: goal reached using " + solver.getAlg() +
                                " after visiting " + solver.getVisitedCount() + " node(s).");
                    } else {
                        System.out.println("Solve result: no path found using " + solver.getAlg() +
                                " after visiting " + solver.getVisitedCount() + " node(s).");
                    }
                }
            });

            MazeRenderer.setControlActions(
                    () -> {
                        int selectedDelay = MazeRenderer.getSelectedStepDelayMs();
                        solveTimer[0].setDelay(selectedDelay);
                        solveTimer[0].setInitialDelay(selectedDelay);

                        if (!hasStarted[0]) {
                            solver.setAlg(MazeRenderer.getSelectedAlgorithm());
                            solver.resetSearch();
                            MazeRenderer.resetAllCellColors();
                            traversedTiles[0] = 0;
                            MazeRenderer.setTraversedCount(traversedTiles[0]);
                            hasStarted[0] = true;
                        }

                        if (!solver.isFinished() && !solveTimer[0].isRunning()) {
                            solveTimer[0].start();
                        }
                    },
                    () -> {
                        if (solveTimer[0].isRunning()) {
                            solveTimer[0].stop();
                        }
                    },
                    () -> {
                        solveTimer[0].stop();
                        solver.setAlg(MazeRenderer.getSelectedAlgorithm());
                        solver.resetSearch();
                        MazeRenderer.resetAllCellColors();
                        traversedTiles[0] = 0;
                        MazeRenderer.setTraversedCount(traversedTiles[0]);
                        int selectedDelay = MazeRenderer.getSelectedStepDelayMs();
                        solveTimer[0].setDelay(selectedDelay);
                        solveTimer[0].setInitialDelay(selectedDelay);
                        hasStarted[0] = false;
                    }
            );

        } catch (Exception ex) {
            System.err.println("Failed to load or print maze: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Prints maze dimensions, start/goal coordinates, and occupancy rows.
     *
     * @param maze maze instance whose content is printed
     */
    private static void printMaze(Maze maze) {
        System.out.println(maze.getLength());
        System.out.println(maze.getWidth());

        Maze.Node start = maze.getStart();
        Maze.Node goal = maze.getGoal();

        if (start == null) {
            System.out.println("start: unset");
        } else {
            System.out.println("start: " + start.row + "," + start.col);
        }

        if (goal == null) {
            System.out.println("end: unset");
        } else {
            System.out.println("end: " + goal.row + "," + goal.col);
        }

        for (int row = 0; row < maze.getLength(); row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < maze.getWidth(); col++) {
                line.append(maze.isOccupied(row, col) ? '1' : '0');
            }
            System.out.println(line);
        }
    }

    /**
     * Validates bidirectional and adjacency consistency of maze neighbor links.
     *
     * @param maze maze instance to validate
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
     * Prints a compact per-cell neighbor table for occupied maze cells.
     *
     * @param maze maze instance used to build the table
     */
    private static void printNeighborTable(Maze maze) {
        for (String line : maze.buildNeighborTableLines()) {
            System.out.println(line);
        }
    }
}
