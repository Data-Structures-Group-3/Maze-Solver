import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.awt.Point;

/**
 * Incremental maze solver that supports DFS, BFS, and A*.
 *
 * <p>The solver keeps internal state between calls to {@link #updateSolve()},
 * so callers can visualize or inspect each step one node at a time.
 *
 * <p>Typical usage: create a solver, set algorithm and maze, then call
 * {@link #updateSolve()} repeatedly until {@link #isFinished()} returns true.
 * Example:
 * @code
 * MazeSolver solver = new MazeSolver();
 * solver.setMaze(maze);
 * solver.setAlg("A*");
 * while (!solver.isFinished()) {
 *     solver.updateSolve();
 * }
 * if (solver.isSolved()) {
 *     List<Point> path = solver.getFinalPath();
 * }
 * @endcode
 */
public class MazeSolver {

    /**
     * Search strategies supported by this solver.
     */
    public enum Algorithm {
        DFS,
        BFS,
        A_STAR
    }

    private Maze maze;
    private Algorithm algorithm = Algorithm.DFS;

    private boolean initialized;
    private boolean finished;
    private boolean solved;
    private int visitedCount;
    private Maze.Node[][] parent;

    // DFS/BFS state
    private final Deque<Maze.Node> frontier = new ArrayDeque<>();
    private boolean[][] discovered;

    // A* state
    private final PriorityQueue<AStarEntry> aStarFrontier = new PriorityQueue<>();
    private int[][] gScore;
    private boolean[][] closed;

    /**
     * Sets which algorithm to run.
     *
     * @param alg algorithm label. Accepted values are DFS, BFS, A*, ASTAR, and A_STAR
     *            (case-insensitive)
     * @throws IllegalArgumentException if {@code alg} is null or unsupported
     */
    public void setAlg(String alg) {
        if (alg == null) {
            throw new IllegalArgumentException("Algorithm cannot be null.");
        }

        String normalized = alg.trim().toUpperCase();
        if ("DFS".equals(normalized)) {
            this.algorithm = Algorithm.DFS;
        } else if ("BFS".equals(normalized)) {
            this.algorithm = Algorithm.BFS;
        } else if ("A*".equals(normalized) || "ASTAR".equals(normalized) || "A_STAR".equals(normalized)) {
            this.algorithm = Algorithm.A_STAR;
        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + alg + ". Use DFS, BFS, or A*.");
        }

        resetSearch();
    }

    /**
     * Gets the active algorithm label.
     *
     * @return display label for the current algorithm
     */
    public String getAlg() {
        if (algorithm == Algorithm.A_STAR) {
            return "A*";
        }
        return algorithm.name();
    }

    /**
     * Assigns the maze instance to solve.
     *
     * @param maze maze object to solve; start/goal are validated when solving begins
     */
    public void setMaze(Maze maze) {
        this.maze = maze;
        resetSearch();
    }

    /**
     * Advances solving by exactly one visited node.
     *
     * @return node visited in this step, or {@code null} if solving is complete or no work remains
     * @throws IllegalStateException if maze/start/goal are not configured
     */
    public Maze.Node updateSolve() {
        if (finished) {
            return null;
        }

        initializeIfNeeded();

        switch (algorithm) {
            case DFS:
                return dfsStep();
            case BFS:
                return bfsStep();
            case A_STAR:
                return aStarStep();
            default:
                throw new IllegalStateException("Unsupported algorithm state: " + algorithm);
        }
    }

    /**
     * Indicates whether this run has terminated.
     *
     * @return true when goal was found or search space was exhausted
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Indicates whether the current run found the goal.
     *
     * @return true when goal node has been reached
     */
    public boolean isSolved() {
        return solved;
    }

    /**
     * Gets count of nodes visited in the current run.
     *
     * @return number of visited nodes since last reset
     */
    public int getVisitedCount() {
        return visitedCount;
    }

    
    /**
     * Gets the final path after a successful solve.
     *
     * <p>The path is traced backward using the internal parent table, so the returned
     * list begins at the goal and ends at the start.
     *
     * @return list of path points where {@code x=col, y=row}; empty if unsolved
     */
    public List<Point> getFinalPath() {
        List<Point> path = new java.util.ArrayList<>();
        if (parent == null || !solved) {
            return path;
        }
        Maze.Node curr = maze.getGoal();
        // Trace back using the parent map
        while (curr != null) {
            path.add(new Point(curr.col, curr.row));
            curr = parent[curr.row][curr.col];
        }
        return path;
    }

    /**
     * Resets transient search state so solving can start fresh.
     *
     * <p>This keeps the currently assigned maze and algorithm.
     */
    public void resetSearch() {
        frontier.clear();
        aStarFrontier.clear();
        parent = null;
        discovered = null;
        gScore = null;
        closed = null;
        initialized = false;
        finished = false;
        solved = false;
        visitedCount = 0;
    }

    /**
     * Performs lazy one-time initialization before first solve step.
     *
     * @throws IllegalStateException if maze, start, or goal are not set
     */
    private void initializeIfNeeded() {
        if (initialized) {
            return;
        }

        if (maze == null) {
            throw new IllegalStateException("Maze must be assigned before solving.");
        }
        if (maze.getStart() == null) {
            throw new IllegalStateException("Maze start is not set.");
        }
        if (maze.getGoal() == null) {
            throw new IllegalStateException("Maze goal is not set.");
        }

        if (algorithm == Algorithm.A_STAR) {
            initializeAStar();
        } else {
            initializeUninformedSearch();
        }

        initialized = true;
        finished = false;
        solved = false;
        visitedCount = 0;
    }

    /**
     * Initializes DFS/BFS state by seeding the frontier with the start node.
     *
     * <p>Also allocates the parent table used for final path reconstruction.
     */
    private void initializeUninformedSearch() {
        discovered = new boolean[maze.getLength()][maze.getWidth()];
        parent = new Maze.Node[maze.getLength()][maze.getWidth()];
        Maze.Node start = maze.getStart();
        discovered[start.row][start.col] = true;
        frontier.addLast(start);
    }

    /**
     * Executes one BFS step (FIFO frontier).
     *
     * @return node visited this step, or {@code null} if no node remains
     */
    private Maze.Node bfsStep() {
        if (frontier.isEmpty()) {
            finished = true;
            solved = false;
            return null;
        }

        Maze.Node current = frontier.removeFirst();
        visitedCount++;

        if (current == maze.getGoal()) {
            finished = true;
            solved = true;
            return current;
        }

        queueUndiscoveredNeighbors(current);
        if (frontier.isEmpty()) {
            finished = true;
        }
        return current;
    }

    /**
     * Executes one DFS step (LIFO frontier).
     *
     * @return node visited this step, or {@code null} if no node remains
     */
    private Maze.Node dfsStep() {
        if (frontier.isEmpty()) {
            finished = true;
            solved = false;
            return null;
        }

        Maze.Node current = frontier.removeLast();
        visitedCount++;

        if (current == maze.getGoal()) {
            finished = true;
            solved = true;
            return current;
        }

        queueUndiscoveredNeighbors(current);
        if (frontier.isEmpty()) {
            finished = true;
        }
        return current;
    }

    /**
     * Enqueues all currently undiscovered neighbors of a node.
     * Sets the parent of all potential nodes to the current node
     *
     * @param current node whose neighbor links are inspected
     */
    private void queueUndiscoveredNeighbors(Maze.Node current) {
        for (Maze.Dir dir : Maze.Dir.values()) {
            Maze.Node neighbor = current.getNeighbor(dir);
            if (neighbor == null) {
                continue;
            }

            if (!discovered[neighbor.row][neighbor.col]) {
                discovered[neighbor.row][neighbor.col] = true;
                parent[neighbor.row][neighbor.col] = current;
                frontier.addLast(neighbor);
            }
        }
    }

    /**
     * Initializes A* frontier and cost tables.
     */
    private void initializeAStar() {
        int rows = maze.getLength();
        int cols = maze.getWidth();
        gScore = new int[rows][cols];
        closed = new boolean[rows][cols];
        parent = new Maze.Node[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                gScore[r][c] = Integer.MAX_VALUE;
            }
        }

        Maze.Node start = maze.getStart();
        gScore[start.row][start.col] = 0;
        aStarFrontier.add(new AStarEntry(start, 0, heuristic(start, maze.getGoal())));
    }

    /**
     * Priority entry used by A* frontier.
     *
     * <p>Entries are ordered by ascending f-cost; ties use g-cost.
     */
    private static class AStarEntry implements Comparable<AStarEntry> {
        private final Maze.Node node;
        private final int gCost;
        private final int fCost;

        /**
         * Builds one A* frontier record.
         *
         * @param node node represented by this entry
         * @param gCost best-known path cost from start to {@code node}
         * @param fCost priority value used by queue ({@code gCost + heuristic})
         */
        private AStarEntry(Maze.Node node, int gCost, int fCost) {
            this.node = node;
            this.gCost = gCost;
            this.fCost = fCost;
        }

        /**
         * Compares entries by A* priority ordering.
         *
         * @param other entry to compare against
         * @return negative if this has higher priority, positive if lower, zero if tied
         */
        @Override
        public int compareTo(AStarEntry other) {
            int byF = Integer.compare(this.fCost, other.fCost);
            if (byF != 0) {
                return byF;
            }
            return Integer.compare(this.gCost, other.gCost);
        }
    }

    /**
     * Executes one A* step.
     *
     * <p>Stale queue entries are skipped; the first valid popped node becomes
     * the visited node for this update.
     *
     * @return node visited this step, or {@code null} if no node remains
     */
    private Maze.Node aStarStep() {
        while (!aStarFrontier.isEmpty()) {
            AStarEntry entry = aStarFrontier.remove();
            Maze.Node current = entry.node;

            if (closed[current.row][current.col]) {
                continue;
            }
            if (entry.gCost != gScore[current.row][current.col]) {
                continue;
            }

            closed[current.row][current.col] = true;
            visitedCount++;

            if (current == maze.getGoal()) {
                finished = true;
                solved = true;
                return current;
            }

            for (Maze.Dir dir : Maze.Dir.values()) {
                Maze.Node neighbor = current.getNeighbor(dir);
                if (neighbor == null || closed[neighbor.row][neighbor.col]) {
                    continue;
                }

                int tentativeG = gScore[current.row][current.col] + 1;
                if (tentativeG < gScore[neighbor.row][neighbor.col]) {
                    gScore[neighbor.row][neighbor.col] = tentativeG;

                    parent[neighbor.row][neighbor.col] = current;

                    int h = heuristic(neighbor, maze.getGoal());
                    aStarFrontier.add(new AStarEntry(neighbor, tentativeG, tentativeG + h));
                }
            }

            if (aStarFrontier.isEmpty()) {
                finished = true;
            }
            return current;
        }

        finished = true;
        solved = false;
        return null;
    }

    /**
     * Computes the Manhattan-distance heuristic used by A*.
     *
     * <p>The value estimates remaining path length by summing horizontal and
     * vertical offsets between two grid coordinates:
     * {@code |from.row - to.row| + |from.col - to.col|}.
     *
     * <p>Example: from {@code (2,3)} to {@code (5,7)} gives
     * {@code |2-5| + |3-7| = 3 + 4 = 7}.
     *
     * @param from current node being evaluated
     * @param to target node (typically maze goal)
     * @return Manhattan distance between the two nodes
     */
    private int heuristic(Maze.Node from, Maze.Node to) {
        return Math.abs(from.row - to.row) + Math.abs(from.col - to.col);
    }
}
