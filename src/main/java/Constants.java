/**
 * Utility class containing system-wide constants for the travel agency Petri net simulation.
 * Defines initial resource quantities, network dimensions, and execution parameters.
 *
 * @author Sassi Juan Ignacio
 */
class Constants {
    /** Prevents instantiation */
    private Constants() {}
    
    /** Number of places in the Petri Net */
    public static final int NUM_P = 15;

    /** Number of transitions in the Petri Net */
    public static final int NUM_T = 12;
    
    /** Maximum number of invariants to complete before ending simulation */
    public static final int MAX_INVARIANTS = 186;
    
    /** P0: Idle place corresponding to the agency's client input buffer */
    public static final int CLIENT = 5;

    /** P1: Door access control */
    public static final int DOOR = 1;

    /** P4: Number of available assistants */
    public static final int ASSISTANTS = 5;

    /** P6: Top reservation agent */
    public static final int AGENT_P6 = 1;

    /** P7: Bottom reservation agent */
    public static final int AGENT_P7 = 1;

    /** P10: Approval/Denial reservation agent */
    public static final int AGENT_P10 = 1;
}