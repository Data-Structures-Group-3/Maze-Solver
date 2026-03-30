import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core maze model storing occupancy, neighbor links, and start/goal nodes.
 *
 * <p>The maze uses a rectangular grid of {@link Node} objects. Traversable
 * cells contain a Node reference and blocked cells are {@code null}. Neighbor
 * links represent valid movement between adjacent traversable cells.
 *
 * <p>Example import format for {@link #importFromFile(String)} is:
 * row count, column count, {@code start: row,col}, {@code end: row,col},
 * then one layout row per maze row using {@code 1} (path) and {@code 0} (wall).
 * Example:
 * @code
 * 4
 * 5
 * start: 3,0
 * end: 0,4
 * 11111
 * 10001
 * 10101
 * 11111
 * @endcode
 */
public class Maze {

    /**
     * Cardinal directions used for indexing node neighbors.
     */
    public static enum Dir {
        NORTH(0), EAST(1), SOUTH(2), WEST(3);
        private final int idx;

        /**
         * Creates a direction enum value with a fixed neighbor-array index.
         *
         * @param idx index used in a node's neighbor array
         */
        Dir(int idx) { this.idx = idx; }

        /**
         * Gets the neighbor-array index associated with this direction.
         *
         * @return zero-based index for this direction in Node.neighbors
         */
        public int idx() { return idx; }
    }

    /**
     * Represents a single cell in the maze grid.
     * A null value in the neighbors array indicates either a wall
     * or an out-of-bounds direction.
     */

    public static class Node {
        public int row;
        public int col;
        //public int layer; // Optional: can be used for 3D mazes or multi-level mazes
        public boolean isGoal;
        
        // Neighbor references: [N, E, S, W] (null if wall or out of bounds)
        public Node[] neighbors;
    
        /**
         * Creates a non-goal node at the given position.
         *
         * @param row node row coordinate
         * @param col node column coordinate
         */
        public Node(int row, int col) {
            this.row = row;
            this.col = col;
            this.isGoal = false;
            this.neighbors = new Node[4]; // [N, E, S, W]
        }

        /**
         * Creates a node at the given position with an explicit goal flag.
         *
         * @param row node row coordinate
         * @param col node column coordinate
         * @param isGoal true if this node is a goal node, false otherwise
         */
        public Node(int row, int col, boolean isGoal) {
            this.row = row;
            this.col = col;
            this.isGoal = isGoal;
            this.neighbors = new Node[4]; // [N, E, S, W]
        }

        /**
         * Retrieves the neighboring node in the given direction.
         *
         * @param d direction to inspect
         * @return adjacent node, or null if blocked/out of bounds
         */
        public Node getNeighbor(Dir d) { return neighbors[d.idx()]; }

        /**
         * Assigns the neighboring node in the given direction.
         *
         * @param d direction to assign
         * @param n adjacent node to store, or null
         */
        public void setNeighbor(Dir d, Node n) { neighbors[d.idx()] = n; }

    }
    

    // Maze grid and dimensions
    private Node[][] grid;
    private int length;
    private int width;
    private Node start;
    private Node goal;
    
    /**
     * Initializes a maze with fixed dimensions.
        *
        * <p>A new maze starts empty: all cells are {@code null} (blocked) until
        * paths are created explicitly via {@link #createCell(int, int, int)} or
        * populated through {@link #importFromFile(String)}.
     *
     * @param length total number of rows in the maze
     * @param width total number of columns in the maze
     */
    public Maze(int length, int width) {
        this.length = length;
        this.width = width;
        this.grid = new Node[length][width];
    }
    

    /**
     * Marks a cell as occupied and creates a node at that coordinate.
        *
        * <p>Establishes bidirectional neighbor links immediately: this node is linked
     * to existing occupied neighbors, and those neighbors are updated to point back.
     * The {@code direction} parameter is currently reserved and ignored.
     *
     * @param row row index of the cell
     * @param col column index of the cell
     * @param direction reserved parameter for future directional creation logic
     */
    public void createCell(int row, int col, int direction) {
        if (isInBounds(row, col)) {
            Node node = new Node(row, col);
            grid[row][col] = node;
            
            // Link to existing neighbors and update their reverse links
            linkToNeighbor(node, row - 1, col, Dir.NORTH, Dir.SOUTH);
            linkToNeighbor(node, row, col + 1, Dir.EAST, Dir.WEST);
            linkToNeighbor(node, row + 1, col, Dir.SOUTH, Dir.NORTH);
            linkToNeighbor(node, row, col - 1, Dir.WEST, Dir.EAST);
        }
    }
    
    /**
     * Helper method to establish bidirectional neighbor link.
     * Links the new node in a given direction and updates the neighbor's reverse link.
     *
     * @param newNode the newly created node
     * @param neighborRow row of the potential neighbor
     * @param neighborCol column of the potential neighbor
     * @param forwardDir direction from new node to neighbor
     * @param reverseDir direction from neighbor back to new node
     */
    private void linkToNeighbor(Node newNode, int neighborRow, int neighborCol, Dir forwardDir, Dir reverseDir) {
        if (isInBounds(neighborRow, neighborCol) && grid[neighborRow][neighborCol] != null) {
            Node neighbor = grid[neighborRow][neighborCol];
            newNode.setNeighbor(forwardDir, neighbor);
            neighbor.setNeighbor(reverseDir, newNode);
        }
    }   

    /**
     * Import a maze from text using this format:
     * line 1: row count
     * line 2: column count
     * line 3: start: row,col
     * line 4: end: row,col
    * line 5+: layout rows of 1s and 0s
    *
    * <p>Cells marked {@code 1} are created through
    * {@link #createCell(int, int, int)} so neighbor links are built incrementally
    * and remain bidirectionally consistent.
    *
    * @param filePath path to the maze text file
    * @return newly created Maze instance populated from file data
    * @throws IOException if file reading fails
    * @throws IllegalArgumentException if file contents are invalid
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
                    maze.createCell(row, col, 0);
                } else if (cell == '0') {
                    maze.grid[row][col] = null;
                } else {
                    throw new IllegalArgumentException("Invalid cell character '" + cell + "' at row " + row + ", col " + col + ". Use only 0 or 1.");
                }
            }
        }

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
      *
      * <p>The output follows the same schema accepted by {@link #importFromFile(String)}.
      *
      * @param filePath destination file path
      * @throws IOException if writing fails
      * @throws IllegalStateException if start or end is not set
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
                output.append(grid[row][col] != null ? '1' : '0');
            }
            output.append(System.lineSeparator());
        }

        Files.writeString(Paths.get(filePath), output.toString());
    }


    /**
     * Parses a coordinate line with the format label: row,col.
     *
     * @param line full line text to parse
     * @param label expected label prefix (for example start or end)
     * @return two-element array containing row at index 0 and col at index 1
     * @throws IllegalArgumentException if the line is missing or malformed
     */
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
     * Sets the start node if the given coordinate is occupied.
     *
     * @param row start row coordinate
     * @param col start column coordinate
     */
    public void setStart(int row, int col) {
        if (isOccupied(row, col)) {
            this.start = grid[row][col];
        }
    }
    
    /**
     * Sets the goal node and updates the goal flag on nodes accordingly.
     *
     * @param row goal row coordinate
     * @param col goal column coordinate
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
     * Returns the current start node.
     *
     * @return start node, or null if not set
     */
    public Node getStart() {
        return this.start;
    }
    
    /**
     * Returns the current goal node.
     *
     * @return goal node, or null if not set
     */
    public Node getGoal() {
        return this.goal;
    }
    
    /**
     * Returns the node at a coordinate if that cell is occupied.
     *
     * @param row row coordinate to query
     * @param col column coordinate to query
     * @return node at the coordinate, or null if not occupied
     */
    public Node getNode(int row, int col) {
        if (isOccupied(row, col)) {
            return grid[row][col];
        }
        return null;
    }
    
    /**
     * Checks whether coordinates are inside maze boundaries.
     *
     * @param row row coordinate to test
     * @param col column coordinate to test
     * @return true if in range, otherwise false
     */
    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < length && col >= 0 && col < width;
    }

    /**
     * Checks whether a cell is a traversable/occupied cell.
     *
     * @param row row coordinate to test
     * @param col column coordinate to test
     * @return true if in bounds and occupied, otherwise false
     */
    public boolean isOccupied(int row, int col) {
        if (isInBounds(row, col)) {
            return grid[row][col] != null;
        }
        return false;
    }
    
    /**
     * Gets the maze row count.
     *
     * @return total number of rows
     */
    public int getLength() {
        return length;
    }
    
    /**
     * Gets the maze column count.
     *
     * @return total number of columns
     */
    public int getWidth() {
        return width;
    }



    /**
     * Validation and debugging utilities.
     */


    

    /**
     * Validation result for neighbor consistency checks.
     */
    public static class NeighborValidationResult {
        private final boolean valid;
        private final int issueCount;
        private final List<String> issues;

        /**
         * Creates an immutable validation result object.
         *
         * @param valid true if no issues were found
         * @param issueCount total number of issues detected
         * @param issues captured issue messages (possibly truncated)
         */
        private NeighborValidationResult(boolean valid, int issueCount, List<String> issues) {
            this.valid = valid;
            this.issueCount = issueCount;
            this.issues = issues;
        }

        /**
         * Indicates whether neighbor validation succeeded.
         *
         * @return true when zero issues were found
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Gets the total number of detected issues.
         *
         * @return issue count
         */
        public int getIssueCount() {
            return issueCount;
        }

        /**
        * Gets the collected issue messages.
        *
        * @return immutable list of issue descriptions
        */
        public List<String> getIssues() {
            return issues;
        }
    }

    /**
     * Validate all node-to-neighbor links against occupied adjacency.
     *
     * @param maxIssuesToCollect maximum number of issue messages to store.
    * @return validation result containing status, issue count, and messages
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
        *
        * @return list of formatted lines describing neighbors for debugging
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

    /**
     * Adds an issue message to the collection while respecting a hard cap.
     *
     * @param issues destination list of issue messages
     * @param maxIssuesToCollect maximum number of messages to store
     * @param message issue message text
     */
    private static void addIssue(List<String> issues, int maxIssuesToCollect, String message) {
        if (issues.size() < maxIssuesToCollect) {
            issues.add(message);
        }
    }

    /**
     * Returns row delta for a movement direction.
     *
     * @param dir direction to convert
     * @return -1, 0, or 1 depending on vertical movement
     */
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

    /**
     * Returns column delta for a movement direction.
     *
     * @param dir direction to convert
     * @return -1, 0, or 1 depending on horizontal movement
     */
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

    /**
     * Returns the opposite of a given direction.
     *
     * @param dir input direction
     * @return opposite direction
     */
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

    /**
     * Formats a node as coordinate text for debug output.
     *
     * @param node node to format
     * @return coordinate string or null text
     */
    private static String formatNode(Node node) {
        if (node == null) {
            return "null";
        }
        return "(" + node.row + "," + node.col + ")";
    }

}
