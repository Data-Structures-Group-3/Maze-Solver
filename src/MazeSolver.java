
import java.util.ArrayList;
import java.util.List;

public class MazeSolver {
    
    private Maze maze;
    
    public MazeSolver(Maze maze) {
        this.maze = maze;
    }
    
    /**
     * Get valid neighbors for a given node (non-null neighbors only).
     * Null pointers represent walls or boundaries.
     * Useful for BFS, DFS, A* algorithms.
     */
    public List<Maze.Node> getValidNeighbors(Maze.Node node) {
        List<Maze.Node> neighbors = new ArrayList<>();
        
        // Check all four directions using Direction enum
        for (Maze.Node.Direction direction : Maze.Node.Direction.values()) {
            Maze.Node neighbor = node.neighbors[direction.ordinal()];
            if (neighbor != null) {
                neighbors.add(neighbor);
            }
        }
        
        return neighbors;
    }
    
    // TODO: Implement BFS algorithm
    public List<Maze.Node> solveBFS() {
        return null;
    }
    
    // TODO: Implement DFS algorithm
    public List<Maze.Node> solveDFS() {
        return null;
    }
    
    // TODO: Implement A* algorithm
    public List<Maze.Node> solveAStar() {
        return null;
    }
    
    public static void main(String[] args) {
        // Example usage:
        // Maze maze = new Maze(10, 10);
        // maze.setStart(0, 0);
        // maze.setGoal(9, 9);
        // maze.createWall(1, 1, 1, 2);  // Create wall between (1,1) and (1,2)
        // 
        // MazeSolver solver = new MazeSolver(maze);
        // List<Maze.Node> path = solver.solveBFS();
    }
}