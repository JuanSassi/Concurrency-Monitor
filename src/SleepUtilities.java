/**
 * Utility class that provides sleep functionality for temporary transitions in the Petri net.
 * Each temporary transition has a predefined maximum duration that simulates the real-world
 * processing time of different operations in the travel agency workflow.
 * 
 * The actual sleep time is randomized between 0 and the maximum duration to simulate
 * variable processing times in real scenarios.
 *
 * @author Sassi Juan Ignacio
 */
class SleepUtilities {
    /** Maximum time for T1: door opening process (in seconds) */
    private static final int NAP_TIME_T1 = 3;
    /** Maximum time for T4: manager 1's reservation processing (in seconds) */
    private static final int NAP_TIME_T4 = 3;
    /** Maximum time for T5: manager 2's reservation processing (in seconds) */
    private static final int NAP_TIME_T5 = 3;
    /** Maximum time for T8: reservation confirmation process (in seconds) */
    private static final int NAP_TIME_T8 = 3;
    /** Maximum time for T9: payment processing (in seconds) */
    private static final int NAP_TIME_T9 = 3;
    /** Maximum time for T10: cancellation process (in seconds) */
    private static final int NAP_TIME_T10 = 3;

    /**
     * Executes a randomized sleep for the specified temporary transition.
     * This method maps temporary transition numbers to their corresponding
     * processing times and simulates the time delay that occurs in real-world
     * operations.
     *
     * @param temporaryTransition The numeric identifier of the temporary transition (1, 4, 5, 8, 9, or 10)
     */
    public static void switchNap(int temporaryTransition) {
        switch (temporaryTransition) {
            case 1: 
                nap(NAP_TIME_T1); 
                break;
            case 4: 
                nap(NAP_TIME_T4); 
                break;
            case 5: 
                nap(NAP_TIME_T5); 
                break;
            case 8: 
                nap(NAP_TIME_T8); 
                break;
            case 9: 
                nap(NAP_TIME_T9); 
                break;
            case 10: 
                nap(NAP_TIME_T10); 
                break;
            default:
                System.out.println("No nap defined for transition " + temporaryTransition);
                break;
        }
    }

    /**
     * Causes the current thread to sleep for a randomized duration.
     * The actual sleep time is a random value between 0.0 and the specified duration,
     * simulating variable processing times in real-world scenarios.
     * 
     * Properly handles InterruptedException by restoring the thread's interrupt status,
     * which is important for clean thread termination in concurrent environments.
     *
     * @param duration The maximum sleep duration in seconds
     */
    public static void nap(int duration) {
        double sleeptime = duration * Math.random(); // between 0.0 and duration
        System.out.printf("Nap for %.2f seconds%n", sleeptime);
        try {
            Thread.sleep((long) (sleeptime * 1000)); // conversion to milliseconds
        } catch (InterruptedException e) {
            System.out.println("Nap interrupted: " + e.getMessage()); // Restore interrupt status
            Thread.currentThread().interrupt();
        }
    }
}