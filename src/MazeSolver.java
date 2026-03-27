public class MazeSolver {
    public static void main(String[] args) {
        String inputFile = "test.txt";
        boolean showNeighbors = true;

        for (String arg : args) {
            if ("--show-neighbors".equalsIgnoreCase(arg) || "-n".equalsIgnoreCase(arg)) {
                showNeighbors = true;
            } else {
                inputFile = arg;
            }
        }

        try {
            Maze maze = Maze.importFromFile(inputFile);
            printMaze(maze);
            validateNeighborConnections(maze);
            if (showNeighbors) {
                printNeighborTable(maze);
            }
        } catch (Exception ex) {
            System.err.println("Failed to load or print maze: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

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

    private static void validateNeighborConnections(Maze maze) {
        int issues = 0;
        int printedIssues = 0;
        final int maxPrintedIssues = 25;

        for (int row = 0; row < maze.getLength(); row++) {
            for (int col = 0; col < maze.getWidth(); col++) {
                if (!maze.isOccupied(row, col)) {
                    continue;
                }

                Maze.Node node = maze.getNode(row, col);
                if (node == null) {
                    issues++;
                    if (printedIssues < maxPrintedIssues) {
                        System.out.println("Neighbor mismatch: occupied cell has null node at (" + row + "," + col + ")");
                        printedIssues++;
                    }
                    continue;
                }

                for (Maze.Dir dir : Maze.Dir.values()) {
                    int nRow = row + rowDelta(dir);
                    int nCol = col + colDelta(dir);
                    boolean expectedConnected = maze.isInBounds(nRow, nCol) && maze.isOccupied(nRow, nCol);

                    Maze.Node actualNeighbor = node.getNeighbor(dir);

                    if (!expectedConnected) {
                        if (actualNeighbor != null) {
                            issues++;
                            if (printedIssues < maxPrintedIssues) {
                                System.out.println("Neighbor mismatch: expected null " + dir + " neighbor at (" + row + "," + col + ") but got (" + actualNeighbor.row + "," + actualNeighbor.col + ")");
                                printedIssues++;
                            }
                        }
                        continue;
                    }

                    Maze.Node expectedNeighbor = maze.getNode(nRow, nCol);
                    if (actualNeighbor == null) {
                        issues++;
                        if (printedIssues < maxPrintedIssues) {
                            System.out.println("Neighbor mismatch: missing " + dir + " neighbor at (" + row + "," + col + "), expected (" + nRow + "," + nCol + ")");
                            printedIssues++;
                        }
                        continue;
                    }

                    if (actualNeighbor != expectedNeighbor) {
                        issues++;
                        if (printedIssues < maxPrintedIssues) {
                            System.out.println("Neighbor mismatch: wrong " + dir + " neighbor at (" + row + "," + col + "), expected (" + nRow + "," + nCol + ") but got (" + actualNeighbor.row + "," + actualNeighbor.col + ")");
                            printedIssues++;
                        }
                    }

                    Maze.Node reciprocal = actualNeighbor.getNeighbor(opposite(dir));
                    if (reciprocal != node) {
                        issues++;
                        if (printedIssues < maxPrintedIssues) {
                            System.out.println("Neighbor mismatch: reciprocal link broken between (" + row + "," + col + ") and (" + actualNeighbor.row + "," + actualNeighbor.col + ") for " + dir);
                            printedIssues++;
                        }
                    }
                }
            }
        }

        if (issues == 0) {
            System.out.println("Neighbor validation: PASSED");
        } else {
            System.out.println("Neighbor validation: FAILED (" + issues + " issue(s))");
            if (issues > maxPrintedIssues) {
                System.out.println("Only first " + maxPrintedIssues + " issue(s) shown.");
            }
        }
    }

    private static int rowDelta(Maze.Dir dir) {
        switch (dir) {
            case NORTH:
                return -1;
            case SOUTH:
                return 1;
            default:
                return 0;
        }
    }

    private static int colDelta(Maze.Dir dir) {
        switch (dir) {
            case EAST:
                return 1;
            case WEST:
                return -1;
            default:
                return 0;
        }
    }

    private static Maze.Dir opposite(Maze.Dir dir) {
        switch (dir) {
            case NORTH:
                return Maze.Dir.SOUTH;
            case EAST:
                return Maze.Dir.WEST;
            case SOUTH:
                return Maze.Dir.NORTH;
            case WEST:
                return Maze.Dir.EAST;
            default:
                throw new IllegalArgumentException("Unknown direction: " + dir);
        }
    }

    private static void printNeighborTable(Maze maze) {
        System.out.println("Neighbor table (occupied cells only):");

        for (int row = 0; row < maze.getLength(); row++) {
            for (int col = 0; col < maze.getWidth(); col++) {
                if (!maze.isOccupied(row, col)) {
                    continue;
                }

                Maze.Node node = maze.getNode(row, col);
                if (node == null) {
                    System.out.println("(" + row + "," + col + ") N=null E=null S=null W=null");
                    continue;
                }

                String north = formatNode(node.getNeighbor(Maze.Dir.NORTH));
                String east = formatNode(node.getNeighbor(Maze.Dir.EAST));
                String south = formatNode(node.getNeighbor(Maze.Dir.SOUTH));
                String west = formatNode(node.getNeighbor(Maze.Dir.WEST));

                System.out.println("(" + row + "," + col + ") N=" + north + " E=" + east + " S=" + south + " W=" + west);
            }
        }
    }

    private static String formatNode(Maze.Node node) {
        if (node == null) {
            return "null";
        }
        return "(" + node.row + "," + node.col + ")";
    }
}