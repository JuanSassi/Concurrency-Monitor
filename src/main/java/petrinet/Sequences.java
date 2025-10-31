/**
 * Configuration class that defines predefined sequences of transitions
 * for different processes in the travel agency workflow.
 * Each sequence represents a complete business process from start to finish.
 *
 * @author Sassi Juan Ignacio
 */
class Sequences {
    
    /** Sequence for client entry process: arrival and door access */
    public static final int[] entryProcess; //ENTRY_PROCESS = {0, 1};
    
    /** Sequence for reservation process handled by manager above */
    public static final int[] reservationProcessAbove; //RESERVATION_PROCESS_ABOVE = {2, 5};
    
    /** Sequence for reservation process handled by manager below */
    public static final int[] reservationProcessBelow; //RESERVATION_PROCESS_BELOW = {3, 4};
    
    /** Sequence for reservation cancellation process */
    public static final int[] cancellationProcess; //CANCELLATION_PROCESS = {7, 8, 11};
    
    /** Sequence for reservation confirmation and payment process */
    public static final int[] confirmationProcess; //CONFIRMATION_PROCESS = {6, 9, 10, 11};
    
    static {
        entryProcess = new int[] {0, 1};
        reservationProcessAbove = new int[] {2, 5};
        reservationProcessBelow = new int[] {3, 4};
        cancellationProcess = new int[] {7, 8, 11};
        confirmationProcess = new int[] {6, 9, 10, 11};
    }
}