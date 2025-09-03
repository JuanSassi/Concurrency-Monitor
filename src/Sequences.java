/**
 * Configuration class that defines predefined sequences of transitions
 * for different processes in the travel agency workflow.
 * Each sequence represents a complete business process from start to finish.
 *
 * @author Sassi Juan Ignacio
 */
public class Sequences {
    
    /** Sequence for client entry process: arrival and door access */
    public static final int[] ENTRY_PROCESS = {0, 1};
    
    /** Sequence for reservation process handled by manager above */
    public static final int[] RESERVATION_PROCESS_ABOVE = {2, 5};
    
    /** Sequence for reservation process handled by manager below */
    public static final int[] RESERVATION_PROCESS_BELOW = {3, 4};
    
    /** Sequence for reservation cancellation process */
    public static final int[] CANCELLATION_PROCESS = {7, 8, 11};
    
    /** Sequence for reservation confirmation and payment process */
    public static final int[] CONFIRMATION_PROCESS = {6, 9, 10, 11};
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods and constants.
     */
    private Sequences() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}