/**
 * Utility class containing system-wide constants for the travel agency Petri net simulation.
 * Defines initial resource quantities, network dimensions, and execution parameters.
 *
 * @author Sassi Juan Ignacio
 */
class Constants {
    private Constants() {} // Avoidance of instantiation

    /** Number of places in the Petri net */
    public static final int NUM_P = 15;
    /** Number of transitions in the Petri net */
    public static final int NUM_T = 12;

    /** Multiple runs must be performed with 186 invariants completed for each run */
    public static final int MAX_INVARIANTS = 186;

    /** Idle place corresponding to the client input buffer */
    public static final int CLIENT = 5;         // P0
    /** Shared resource: door access control */
    public static final int DOOR = 1;           // P1
    /** Number of available assistants */
    public static final int ASSISTANTS = 5;     // P4
    /** Shared resource: manager1 availability */
    public static final int AGENT_P6 = 1;       // P6
    /** Shared resource: manager2 availability */
    public static final int AGENT_P7 = 1;       // P7
    /** Shared resource: travel agent availability */
    public static final int AGENT_P10 = 1;      // P10
}