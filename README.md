# Maze-Solver

Maze-Solver is a Java Swing desktop app for loading, visualizing, and solving grid mazes using DFS, BFS, and A*.

It is designed for both interactive exploration and large-map performance testing, with a paint-based renderer that supports very large mazes efficiently.

## Table of Contents

1. Overview
2. Features
3. Requirements
4. Quick Start
5. How to Use
6. Maze File Format
7. Included Maze Files
8. Algorithms
9. Rendering and Performance
10. Project Structure
11. API and Extension Notes
12. Troubleshooting
13. Development Notes
14. License

## Overview

Maze-Solver has four main responsibilities:

1. Load maze files from disk.
2. Validate and visualize the maze graph.
3. Run DFS, BFS, or A* incrementally or instantly.
4. Show traversal progress and final path with clear visual semantics.

## Features

- Title-screen startup flow with import-or-generate selection.
- Random maze generation using traditional corridor carving.
- Interactive solver controls for start, pause, restart, algorithm, and delay.
- Instant solve mode that runs to completion in a background thread.
- In-session map controls for loading a different map, saving the current map, and generating a new random map.
- Paint-based grid rendering for all maze sizes.
- Viewport clipping so only visible cells are painted.
- Zoom controls (+/- and Ctrl + mouse wheel).
- Gridline overlay for easier node-by-node visibility.
- Start/goal color preservation after final path draw.
- Debug diagnostics for maze import and neighbor consistency checks.

## Requirements

- Java JDK 17+ (11+ may work, but 17+ is recommended).
- Windows, macOS, or Linux with desktop GUI support.
- No external dependencies required.

## Quick Start

From the repository root:

1. Compile

```bash
javac src/*.java
```

2. Run

```bash
java -cp src MS
```

Startup sequence:

1. Title screen appears.
2. Choose Import Maze or Generate Random.
3. If importing, select a maze text file; if generating, enter rows/columns.
4. Solve interactively.

## How to Use

After loading or generating a maze:

- Start: begin or resume timer-based solving.
- Pause: stop solving without clearing current search state.
- Restart: clear colors and reset the active algorithm state.
- Load Map: replace the current session maze with another file.
- Save Map: export the currently loaded maze to a text file.
- Generate Map: create and load a new random maze without leaving the solver window.
- Algorithm: choose DFS, BFS, or A*.
- Step Delay (ms): set timer delay for step-by-step mode (1..5000).
- Instant: solve to completion immediately (async worker).
- Zoom +/-: adjust cell size.
- Ctrl + Mouse Wheel: zoom while cursor is over the maze viewport.

Random generation details:

- Uses a depth-first backtracking corridor-carving strategy for a classic maze look.
- Keeps the outer border blocked.
- Ensures the configured goal cell is connected to the carved maze.
- Extends carving into final interior row/column on even dimensions to avoid persistent right/bottom wall strips.

## Maze File Format

Expected format:

1. Row count
2. Column count
3. start: row,col
4. end: row,col
5. One row per maze row using:
	- 1 for traversable/open
	- 0 for blocked/wall

Example:

```text
4
5
start: 3,0
end: 0,4
11111
10001
10101
11111
```

Validation rules enforced by import:

- Dimensions must be positive.
- Layout row count must match declared row count.
- Each layout row width must match declared column count.
- Only 0 and 1 are valid cell values.
- Start and end must be in bounds and on traversable cells.

## Included Maze Files

Located in src:

- test.txt: small sample map for quick checks.
- test_100x100.txt: medium map for standard performance checks.
- test_346x346.txt: larger map for stress testing.
- test_1000x1000.txt: very large map for renderer scalability tests.

## Algorithms

The solver supports:

- DFS: depth-first exploration, memory-light but not shortest-path guaranteed.
- BFS: breadth-first exploration, shortest path in unweighted grids.
- A*: informed search using Manhattan distance heuristic.

A* heuristic used:

$$h(n) = |r_n - r_g| + |c_n - c_g|$$

where $r_n,c_n$ is current node and $r_g,c_g$ is goal.

## Rendering and Performance

Current renderer behavior:

- One paint-based path for every maze size.
- Cell colors are cached in a backing array.
- Only visible clip-region cells are painted each frame.
- Gridlines are rendered with a subtle alpha stroke.
- Start and goal labels (S/G) are drawn at larger zoom levels.

Color semantics:

- Start: green
- Goal: blue
- Open: white
- Blocked: dark gray
- Visited: configurable (default yellow)
- Final path: cyan

After a solved run, start and goal are restored to semantic colors.

## Project Structure

```text
Maze-Solver/
├─ README.md
├─ LICENSE
├─ src/
│  ├─ MS.java
│  ├─ Maze.java
│  ├─ MazeSolver.java
│  ├─ MazeRenderer.java
│  ├─ test.txt
│  ├─ test_100x100.txt
│  ├─ test_346x346.txt
│  ├─ test_1000x1000.txt
│  └─ images/
└─ doc/
```

Core classes:

- MS: application entry point.
- Maze: model for occupancy, neighbors, start, goal, import/export.
- MazeSolver: incremental solver engine and path reconstruction.
- MazeRenderer: UI flow, controls, rendering, and solve orchestration.

## API and Extension Notes

Useful extension points:

```java
Maze.Node start = maze.getStart();
MazeRenderer.setNodeColor(start, java.awt.Color.MAGENTA);
MazeRenderer.resetNodeColor(start);
```

Typical programmatic solve usage:

```java
Maze maze = Maze.importFromFile("src/test_346x346.txt");
MazeSolver solver = new MazeSolver();
solver.setMaze(maze);
solver.setAlg("A*");

while (!solver.isFinished()) {
	 solver.updateSolve();
}

if (solver.isSolved()) {
	 java.util.List<java.awt.Point> path = solver.getFinalPath();
	 System.out.println("Path nodes: " + path.size());
}
```

## Troubleshooting

1. App does not start

- Confirm javac and java are on PATH.
- Rebuild with javac src/*.java from repo root.

2. Maze fails to load

- Check file format and dimensions.
- Ensure start/end are on traversable cells (1).

3. Slow behavior on huge maps

- Use lower zoom to reduce overdraw.
- Prefer Instant mode for completion-only runs.

4. Unexpected debug output volume

- The renderer prints diagnostics during load.
- This is useful for validation but verbose on large maps.

## Development Notes

Useful commands from repo root:

```bash
javac src/*.java
java -cp src MS
```

Optional cleanup of compiled artifacts:

```bash
del src\*.class
```

Optional docs generation can be done later from the doc tooling already included in this repo.

## License

This project is licensed under the terms in LICENSE.