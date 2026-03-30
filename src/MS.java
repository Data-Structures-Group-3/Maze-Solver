import javax.swing.SwingUtilities;

/**
 * Demo application entry point.
 *
 * <p>Typical usage is to run this class directly from IDE or command line.
 * It delegates all GUI and session flow to {@link MazeRenderer}.
 */
public class MS {

    /**
     * Starts the maze demo.
     *
      * <p>Delegates app flow to MazeRenderer:
      * title screen, file selection, maze loading, and interactive solve UI.
     *
     * @param args command line arguments (currently unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MazeRenderer::launchFromTitleScreen);
    }
}
