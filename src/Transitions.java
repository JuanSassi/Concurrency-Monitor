/**
 * Enumeration representing temporary transitions in the Petri net.
 * These transitions are used to identify which transitions are considered
 * temporary within the system's workflow (such as intermediate steps in
 * reservation processing that can be confirmed or cancelled).
 *
 * @author Sassi Juan Ignacio
 */
public enum Transition {
    /** Transition T1 with value 1 */
    T1(1),
    /** Transition T4 with value 4 */
    T4(4),
    /** Transition T5 with value 5 */
    T5(5),
    /** Transition T8 with value 8 */
    T8(8),
    /** Transition T9 with value 9 */
    T9(9),
    /** Transition T10 with value 10 */
    T10(10);

    /** The numeric value associated with this transition */
    public final int value;

    /**
     * Constructor that associates a numeric value with each transition.
     *
     * @param value The numeric identifier for this transition
     */
    Transition(int value) {
        this.value = value;
    }

    /**
     * Checks if a given transition value corresponds to a temporary transition.
     * Temporary transitions are those defined in this enumeration.
     * The transition consumes tokens, waits for a predetermined time and finally 
     * produces tokens.
     *
     * @param transitionValue The numeric value of the transition to check
     * @return true if the transition is temporary (defined in this enum), false otherwise
     */
    public static boolean isTemporary(int transitionValue) {
        for (Transition t : Transition.values()) {
            if (t.value == transitionValue) {
                return true;
            }
        }
        return false;
    }
}