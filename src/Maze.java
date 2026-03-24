public class Maze {
    
    /**
     * Represents a single cell in the maze grid.
     * Null pointer in neighbors array indicates a wall or boundary.
     * 
     */

     public static enum Direction {
            NORTH, EAST, SOUTH, WEST
        };

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
    }
    
    private Node[][] grid;
    private int height;
    private int width;
    private Node start;
    private Node goal;
    
    /**
     * Initialize maze with given dimensions.
     * All cells start as non-wall paths.
     */
    public Maze(int height, int width) {
        this.height = height;
        this.width = width;
        this.grid = new Node[height][width];
        initializeGrid();
    }
    
    /**
     * Create nodes and establish neighbor links.
     */
    private void initializeGrid() {
        // Create all nodes
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid[r][c] = new Node(r, c);
            }
        }
        
        // Link neighbors [N, E, S, W]
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Node current = grid[r][c];
                
                // NORTH
                if (r > 0) current.neighbors[Direction.NORTH.ordinal()] = grid[r - 1][c];
                // EAST
                if (c < width - 1) current.neighbors[Direction.EAST.ordinal()] = grid[r][c + 1];
                // SOUTH
                if (r < height - 1) current.neighbors[Direction.SOUTH.ordinal()] = grid[r + 1][c];
                // WEST
                if (c > 0) current.neighbors[Direction.WEST.ordinal()] = grid[r][c - 1];
            }
        }
    }
    
    /**
     * Create a wall between two nodes by setting their respective pointers to null.
     */
    public void createWall(int row1, int col1, int row2, int col2) {
        if (!isInBounds(row1, col1) || !isInBounds(row2, col2)) {
            return;
        }
        
        Node node1 = grid[row1][col1];
        Node node2 = grid[row2][col2];
        
        // Determine direction from node1 to node2 and vice versa
        int dRow = row2 - row1;
        int dCol = col2 - col1;
        
        // Only allow adjacent cells
        if (Math.abs(dRow) + Math.abs(dCol) != 1) {
            return;
        }
        
        if (dRow == -1) { // node2 is NORTH of node1
            node1.neighbors[Direction.NORTH.ordinal()] = null;
            node2.neighbors[Direction.SOUTH.ordinal()] = null;
        } else if (dRow == 1) { // node2 is SOUTH of node1
            node1.neighbors[Direction.SOUTH.ordinal()] = null;
            node2.neighbors[Direction.NORTH.ordinal()] = null;
        } else if (dCol == -1) { // node2 is WEST of node1
            node1.neighbors[Direction.WEST.ordinal()] = null;
            node2.neighbors[Direction.EAST.ordinal()] = null;
        } else if (dCol == 1) { // node2 is EAST of node1
            node1.neighbors[Direction.EAST.ordinal()] = null;
            node2.neighbors[Direction.WEST.ordinal()] = null;
        }
    }
    
    /**
     * Set the start position.
     */
    public void setStart(int row, int col) {
        if (isInBounds(row, col)) {
            this.start = grid[row][col];
        }
    }
    
    /**
     * Set the goal position.
     */
    public void setGoal(int row, int col) {
        if (isInBounds(row, col)) {
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
        if (isInBounds(row, col)) {
            return grid[row][col];
        }
        return null;
    }
    
    /**
     * Check if coordinates are within maze bounds.
     */
    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < height && col >= 0 && col < width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getWidth() {
        return width;
    }
}
