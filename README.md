# Maze-Solver

Desktop Java Swing maze visualizer and solver with DFS, BFS, and A*.

## Run

1. Compile:
	`javac src/*.java`
2. Launch:
	`java -cp src MS`

The app opens a title screen first, then prompts for a maze text file.

## Maze File Format

The expected text format is:

1. Row count
2. Column count
3. `start: row,col`
4. `end: row,col`
5. One row per line of `1` (open) and `0` (blocked)

## Solver Controls

- `Start`: begins solving, or resumes if paused.
- `Pause`: pauses timer-based solving without resetting state.
- `Restart`: clears traversal/path colors and resets search state.
- `Algorithm`: `DFS`, `BFS`, or `A*`.
- `Step Delay (ms)`: timer delay for step-by-step solve; valid range is `1..5000` ms.
- `Instant`: when enabled, pressing `Start` runs the solver to completion in the background and updates the UI when done.
- `Zoom +/-`: resizes maze cells.
- `Ctrl + Mouse Wheel`: zoom in/out while hovering the maze pane.

## Instant Solve Behavior

- Instant mode and timer mode are mutually exclusive at runtime.
- The delay spinner is disabled while `Instant` is selected because step delay does not apply.
- The `Instant` checkbox is disabled while solving is active (both timer solve and instant solve) to prevent mode switching mid-run.
- Instant solve updates at completion with the same visual semantics as step mode: traversed cells are colored, then the final path is highlighted.

## Large Maze Handling

- Maze grid is hosted in a scroll pane with both horizontal and vertical scrolling.
- Window size is clamped to available screen bounds and centered.