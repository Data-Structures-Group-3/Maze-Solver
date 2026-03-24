public class Maze {

    /**
     * Enum for directions to improve readability.
     */
    public static final enum Dir {
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
        if (isOccupied(row, col)) {
            this.goal = grid[row][col];
            this.goal.isGoal = true;
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
}
