public class MazeSolver {
    public static void main(String[] args) {
        String inputFile = "test.txt";
        boolean showNeighbors = true;

        try {
            Maze maze = Maze.importFromFile(inputFile);
            printMaze(maze);
            validateMazeNeighbors(maze);

            if (showNeighbors) {
                printNeighborTable(maze);
            }

            // Make all cells available so we can visualize paths.
            for (int r = 0; r < maze.getLength(); r++) {
                for (int c = 0; c < maze.getWidth(); c++) {
                    maze.createCell(r, c, 0);
                }
            }

            MazeRenderer.showMaze(maze);


        } catch (Exception ex) {
            System.err.println("Failed to load or print maze: " + ex.getMessage());
            ex.printStackTrace();
        }


    }


/*
* Helper method to print the maze details and layout.
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

    private static void printNeighborTable(Maze maze) {
        for (String line : maze.buildNeighborTableLines()) {
            System.out.println(line);
        }
    }
}