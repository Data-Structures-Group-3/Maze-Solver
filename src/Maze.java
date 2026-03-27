import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Maze {

    /**
     * Enum for directions to improve readability.
     */
    public static enum Dir {
        NORTH(0), EAST(1), SOUTH(2), WEST(3);
        private final int idx;
        Dir(int idx) { this.idx = idx; }
        public int idx() { return idx; }
    }
    /**
     * Represents a single cell in the maze grid.
     * Null pointer in neighbors array indicates a wall or boundary.
     * 
     */

    public static class Node {
        public int row;
        public int col;
        //public int layer; // Optional: can be used for 3D mazes or multi-level mazes
        public boolean isGoal;
        
        // Neighbor references: [N, E, S, W] (null if wall or out of bounds)
        public Node[] neighbors;
    
        
        public Node(int row, int col) {
            this.row = row;
            this.col = col;
            this.isGoal = false;
            this.neighbors = new Node[4]; // [N, E, S, W]
        }

        public Node(int row, int col, boolean isGoal) {
            this.row = row;
            this.col = col;
            this.isGoal = isGoal;
            this.neighbors = new Node[4]; // [N, E, S, W]
        }

        public Node getNeighbor(Dir d) { return neighbors[d.idx()]; }
        public void setNeighbor(Dir d, Node n) { neighbors[d.idx()] = n; }

    }
    

    // Maze grid and dimensions
    private Node[][] grid;
    private boolean[][] occupied;
    private int length;
    private int width;
    private Node start;
    private Node goal;
    
    /**
     * Initialize maze with given dimensions.
     * All cells start as non-wall paths.
     */
    public Maze(int length, int width) {
        this.length = length;
        this.width = width;
        this.grid = new Node[length][width];
        this.occupied = new boolean[length][width];
        initializeGrid();
    }
    
    /**
     * Create nodes and establish neighbor links.
     */
    private void initializeGrid() {
        // Create all nodes
        for (int l = 0; l < length; l++) {
            for (int w = 0; w < width; w++) {
                grid[l][w] = new Node(l, w);
                occupied[l][w] = false; // Initially, all cells are unoccupied, no paths
            }
        }
        
    }
    

    /**
     * Create a wall between two adjacent nodes.
     * This removes the neighbor reference in both nodes.
     */
    public void createWall(int row1, int col1, int row2, int col2) {
        if (isInBounds(row1, col1) && isInBounds(row2, col2)) {
            Node node1 = grid[row1][col1];
            Node node2 = grid[row2][col2];
            
            // Determine direction from node1 to node2
            if (row1 == row2) {
                // Same row: EAST or WEST
                if (col1 < col2) {
                    // node1 is WEST of node2
                    node1.neighbors[Maze.Dir.EAST.ordinal()] = null;
                    node2.neighbors[Maze.Dir.WEST.ordinal()] = null;
                } else {
                    // node1 is EAST of node2
                    node1.neighbors[Maze.Dir.WEST.ordinal()] = null;
                    node2.neighbors[Maze.Dir.EAST.ordinal()] = null;
                }
            } else if (col1 == col2) {
                // Same column: NORTH or SOUTH
                if (row1 < row2) {
                    // node1 is NORTH of node2
                    node1.neighbors[Maze.Dir.SOUTH.ordinal()] = null;
                    node2.neighbors[Maze.Dir.NORTH.ordinal()] = null;
                } else {
                    // node1 is SOUTH of node2
                    node1.neighbors[Maze.Dir.NORTH.ordinal()] = null;
                    node2.neighbors[Maze.Dir.SOUTH.ordinal()] = null;
                }
            }
        }
    }

    public void createCell(int row, int col, int direction) {
        if (isInBounds(row, col)) {
            occupied[row][col] = true;
            grid[row][col] = new Node(row, col);
        }
    }   

    /**
     * Rebuild all neighbor links using the occupied layout.
     * Occupied adjacent cells are connected; walls and boundaries are null.
     */
    private void rebuildNeighborsFromOccupied() {
        for (int row = 0; row < length; row++) {
            for (int col = 0; col < width; col++) {
                if (!occupied[row][col] || grid[row][col] == null) {
                    continue;
                }

                Node node = grid[row][col];
                node.setNeighbor(Dir.NORTH, getNodeIfOccupied(row - 1, col));
                node.setNeighbor(Dir.EAST, getNodeIfOccupied(row, col + 1));
                node.setNeighbor(Dir.SOUTH, getNodeIfOccupied(row + 1, col));
                node.setNeighbor(Dir.WEST, getNodeIfOccupied(row, col - 1));
            }
        }
    }

    private Node getNodeIfOccupied(int row, int col) {
        if (!isInBounds(row, col) || !occupied[row][col]) {
            return null;
        }
        return grid[row][col];
    }

    /**
     * Import a maze from text using this format:
     * line 1: row count
     * line 2: column count
     * line 3: start: row,col
     * line 4: end: row,col
     * line 5+: layout rows of 1s and 0s
     */
    public static Maze importFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        List<String> lines = Files.readAllLines(path);

        if (lines.size() < 4) {
            throw new IllegalArgumentException("Maze file must contain at least 4 header lines.");
        }

        int length = Integer.parseInt(lines.get(0).trim());
        int width = Integer.parseInt(lines.get(1).trim());

        if (length <= 0 || width <= 0) {
            throw new IllegalArgumentException("Maze dimensions must be positive.");
        }

        if (lines.size() < 4 + length) {
            throw new IllegalArgumentException("Maze file does not contain the expected number of layout rows.");
        }

        int[] startPos = parseCoordinateLine(lines.get(2), "start");
        int[] endPos = parseCoordinateLine(lines.get(3), "end");

        Maze maze = new Maze(length, width);

        for (int row = 0; row < length; row++) {
            String layoutRow = lines.get(4 + row).trim();
            if (layoutRow.length() != width) {
                throw new IllegalArgumentException("Layout row " + row + " has width " + layoutRow.length() + " but expected " + width + ".");
            }

            for (int col = 0; col < width; col++) {
                char cell = layoutRow.charAt(col);
                if (cell == '1') {
                    maze.occupied[row][col] = true;
                    maze.grid[row][col] = new Node(row, col);
                } else if (cell == '0') {
                    maze.occupied[row][col] = false;
                    maze.grid[row][col] = null;
                } else {
                    throw new IllegalArgumentException("Invalid cell character '" + cell + "' at row " + row + ", col " + col + ". Use only 0 or 1.");
                }
            }
        }

        maze.rebuildNeighborsFromOccupied();

        if (!maze.isInBounds(startPos[0], startPos[1]) || !maze.isOccupied(startPos[0], startPos[1])) {
            throw new IllegalArgumentException("Start position must be in bounds and on a path cell (1).");
        }
        if (!maze.isInBounds(endPos[0], endPos[1]) || !maze.isOccupied(endPos[0], endPos[1])) {
            throw new IllegalArgumentException("End position must be in bounds and on a path cell (1).");
        }

        maze.setStart(startPos[0], startPos[1]);
        maze.setGoal(endPos[0], endPos[1]);
        return maze;
    }

    /**
     * Export the current maze to text format for import/export.
     */
    public void exportToFile(String filePath) throws IOException {
        if (start == null) {
            throw new IllegalStateException("Start position is not set.");
        }
        if (goal == null) {
            throw new IllegalStateException("End position is not set.");
        }

        StringBuilder output = new StringBuilder();
        output.append(length).append(System.lineSeparator());
        output.append(width).append(System.lineSeparator());
        output.append("start: ").append(start.row).append(",").append(start.col).append(System.lineSeparator());
        output.append("end: ").append(goal.row).append(",").append(goal.col).append(System.lineSeparator());

        for (int row = 0; row < length; row++) {
            for (int col = 0; col < width; col++) {
                output.append(occupied[row][col] ? '1' : '0');
            }
            output.append(System.lineSeparator());
        }

        Files.writeString(Paths.get(filePath), output.toString());
    }


    private static int[] parseCoordinateLine(String line, String label) {
        if (line == null) {
            throw new IllegalArgumentException("Missing line for " + label + " coordinates.");
        }

        String trimmed = line.trim();
        String prefix = label + ":";
        if (!trimmed.toLowerCase().startsWith(prefix)) {
            throw new IllegalArgumentException("Expected '" + prefix + " row,col' but got: " + line);
        }

        String coordinateText = trimmed.substring(prefix.length()).trim();
        String[] parts = coordinateText.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected coordinates as row,col for " + label + ".");
        }

        int row = Integer.parseInt(parts[0].trim());
        int col = Integer.parseInt(parts[1].trim());
        return new int[] { row, col };
    }


    /**
     * Set the start position.
     */
    public void setStart(int row, int col) {
        if (isOccupied(row, col)) {
            this.start = grid[row][col];
        }
    }
    
    /**
     * Set the goal position.
     */
    public void setGoal(int row, int col) {
        if (this.goal != null) {
            this.goal.isGoal = false;
        }

        if (isOccupied(row, col)) {
            this.goal = grid[row][col];
            this.goal.isGoal = true;
        } else {
            this.goal = null;
        }
    }
    
    /**
     * Get the starting node.
     */
    public Node getStart() {
        return this.start;
    }
    
    /**
     * Get the goal node.
     */
    public Node getGoal() {
        return this.goal;
    }
    
    /**
     * Get a specific node by coordinates.
     */
    public Node getNode(int row, int col) {
        if (isOccupied(row, col)) {
            return grid[row][col];
        }
        return null;
    }
    
    /**
     * Check if coordinates are within maze bounds.
     */
    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < length && col >= 0 && col < width;
    }

    public boolean isOccupied(int row, int col) {
        if (isInBounds(row, col)) {
            return occupied[row][col];
        }
        return false;
    }
    
    public int getLength() {
        return length;
    }
    
    public int getWidth() {
        return width;
    }



    /**
     * 
     * Validation and debugging utilities:
     * 
     */


    

    /**
     * Validation result for neighbor consistency checks.
     */
    public static class NeighborValidationResult {
        private final boolean valid;
        private final int issueCount;
        private final List<String> issues;

        private NeighborValidationResult(boolean valid, int issueCount, List<String> issues) {
            this.valid = valid;
            this.issueCount = issueCount;
            this.issues = issues;
        }

        public boolean isValid() {
            return valid;
        }

        public int getIssueCount() {
            return issueCount;
        }

        public List<String> getIssues() {
            return issues;
        }
    }

    /**
     * Validate all node-to-neighbor links against occupied adjacency.
     *
     * @param maxIssuesToCollect maximum number of issue messages to store.
     */
    public NeighborValidationResult validateNeighborConnections(int maxIssuesToCollect) {
        int issues = 0;
        int limit = Math.max(0, maxIssuesToCollect);
        List<String> issueMessages = new ArrayList<>();

        for (int row = 0; row < length; row++) {
            for (int col = 0; col < width; col++) {
                if (!isOccupied(row, col)) {
                    continue;
                }

                Node node = getNode(row, col);
                if (node == null) {
                    issues++;
                    addIssue(issueMessages, limit,
                        "Neighbor mismatch: occupied cell has null node at (" + row + "," + col + ")");
                    continue;
                }

                for (Dir dir : Dir.values()) {
                    int nRow = row + rowDelta(dir);
                    int nCol = col + colDelta(dir);
                    boolean expectedConnected = isInBounds(nRow, nCol) && isOccupied(nRow, nCol);

                    Node actualNeighbor = node.getNeighbor(dir);

                    if (!expectedConnected) {
                        if (actualNeighbor != null) {
                            issues++;
                            addIssue(issueMessages, limit,
                                "Neighbor mismatch: expected null " + dir + " neighbor at (" + row + "," + col + ") but got ("
                                    + actualNeighbor.row + "," + actualNeighbor.col + ")");
                        }
                        continue;
                    }

                    Node expectedNeighbor = getNode(nRow, nCol);
                    if (actualNeighbor == null) {
                        issues++;
                        addIssue(issueMessages, limit,
                            "Neighbor mismatch: missing " + dir + " neighbor at (" + row + "," + col + "), expected ("
                                + nRow + "," + nCol + ")");
                        continue;
                    }

                    if (actualNeighbor != expectedNeighbor) {
                        issues++;
                        addIssue(issueMessages, limit,
                            "Neighbor mismatch: wrong " + dir + " neighbor at (" + row + "," + col + "), expected ("
                                + nRow + "," + nCol + ") but got (" + actualNeighbor.row + "," + actualNeighbor.col + ")");
                    }

                    Node reciprocal = actualNeighbor.getNeighbor(opposite(dir));
                    if (reciprocal != node) {
                        issues++;
                        addIssue(issueMessages, limit,
                            "Neighbor mismatch: reciprocal link broken between (" + row + "," + col + ") and ("
                                + actualNeighbor.row + "," + actualNeighbor.col + ") for " + dir);
                    }
                }
            }
        }

        return new NeighborValidationResult(issues == 0, issues, Collections.unmodifiableList(issueMessages));
    }

    /**
     * Build a compact, line-by-line neighbor table for occupied cells.
     */
    public List<String> buildNeighborTableLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Neighbor table (occupied cells only):");

        for (int row = 0; row < length; row++) {
            for (int col = 0; col < width; col++) {
                if (!isOccupied(row, col)) {
                    continue;
                }

                Node node = getNode(row, col);
                if (node == null) {
                    lines.add("(" + row + "," + col + ") N=null E=null S=null W=null");
                    continue;
                }

                String north = formatNode(node.getNeighbor(Dir.NORTH));
                String east = formatNode(node.getNeighbor(Dir.EAST));
                String south = formatNode(node.getNeighbor(Dir.SOUTH));
                String west = formatNode(node.getNeighbor(Dir.WEST));
                lines.add("(" + row + "," + col + ") N=" + north + " E=" + east + " S=" + south + " W=" + west);
            }
        }

        return lines;
    }

    private static void addIssue(List<String> issues, int maxIssuesToCollect, String message) {
        if (issues.size() < maxIssuesToCollect) {
            issues.add(message);
        }
    }

    private static int rowDelta(Dir dir) {
        switch (dir) {
            case NORTH:
                return -1;
            case SOUTH:
                return 1;
            default:
                return 0;
        }
    }

    private static int colDelta(Dir dir) {
        switch (dir) {
            case EAST:
                return 1;
            case WEST:
                return -1;
            default:
                return 0;
        }
    }

    private static Dir opposite(Dir dir) {
        switch (dir) {
            case NORTH:
                return Dir.SOUTH;
            case EAST:
                return Dir.WEST;
            case SOUTH:
                return Dir.NORTH;
            case WEST:
                return Dir.EAST;
            default:
                throw new IllegalArgumentException("Unknown direction: " + dir);
        }
    }

    private static String formatNode(Node node) {
        if (node == null) {
            return "null";
        }
        return "(" + node.row + "," + node.col + ")";
    }

}
