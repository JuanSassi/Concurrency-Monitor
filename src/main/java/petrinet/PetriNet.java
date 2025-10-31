/**
 * Singleton class representing a Petri net model for a travel agency system.
 * This class models the workflow of client reservations including resources like
 * doors, assistants, managers, and agents. It supports both immediate and temporary
 * transitions with token consumption, production and fire mechanisms.
 *
 * The Petri net consists of 15 places (P0-P14) and 12 transitions (T0-T11)
 * representing different states and operations in the reservation process.
 *
 * @author Sassi Juan Ignacio
 */
public class PetriNet { 
    /** Single static instance of PetriNet following Singleton pattern */
    private static PetriNet petriNet = new PetriNet();

    /** Current marking vector representing the current state of Petri net */
    private int[] marking;

    private final int[] temporalTransitions;

    /**
     * Input incidence matrix (Pre matrix).
     * Element [i][j] represents the number of tokens consumed from place Pi
     * when transition Tj fires. Rows represent places (Pi), columns represent transitions (Tj).
     */
    private final int[][] pre;

    /**
     * Output incidence matrix (Post matrix).
     * Element [i][j] represents the number of tokens produced in place Pi
     * when transition Tj fires. Rows represent places (Pi), columns represent transitions (Tj).
     */
    private final int[][] post;

    /**
     * Incidence matrix calculated as Post - Pre.
     * Element [i][j] represents the net change in tokens for place Pi
     * when transition Tj fires (positive = tokens added, negative = tokens removed).
     */
    private final int[][] incidence;

    private final int numP;

    private final int numT;

    /**
     * Private constructor implementing Singleton pattern.
     * Initializes the marking with initial values and calculates the incidence matrix.
     */
    private PetriNet() {
        this.numP = PetrinetLoader.getNumPlaces();
        this.numT = PetrinetLoader.getNumTransitions();
        this.pre = PetrinetLoader.getPreMatrix();
        this.post = PetrinetLoader.getPostMatrix();
        this.marking = PetrinetLoader.getInitialMarkingVector();
        this.temporalTransitions = PetrinetLoader.getTemporalTransitionsVector();

        // Dimension validation
        if (pre.length != post.length) {
            throw new RuntimeException("The Pre and Post matrices must have the same number of rows");
        }
        if (pre[0].length != post[0].length) {
            throw new RuntimeException("The Pre and Post matrices must have the same number of columns");
        }
        if (marking.length != numP) {
            throw new RuntimeException("The initial marking vector must have " + numP + " items");
        }
        if (temporalTransitions.length != numT) {
            throw new RuntimeException("The vector of temporal transitions must have " + numT + " items");
        }

        // Start incidence as post - pre
        incidence = new int[numP][numT];
        for (int i = 0; i < numP; i++) {
            for (int j = 0; j < numT; j++) {
                incidence[i][j] = post[i][j] - pre[i][j];
            }
        }
    }

    /**
     * Public method to get the unique instance following Singleton pattern.
     *
     * @return The single instance of PetriNet
     */
    public static PetriNet getInstance() {
        return petriNet;
    }

    /**
     * Checks if a transition is enabled (ready to be fired).
     * A transition is enabled when the current marking has enough tokens
     * in all input places to satisfy the pre-conditions.
     *
     * @param transition The index of the transition to check (0-based)
     * @return true if the transition can be fired, false otherwise
     */
    public boolean transitionEnabled(int transition) {
        if (transition < 0 || transition >= numT) {
            throw new IllegalArgumentException("Invalid transition index: " + transition);
        }
        for (int i = 0; i < numP; i++) {
            if (marking[i] < pre[i][transition]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Consumes tokens from places according to the pre-conditions of a temporary transition.
     * This method is used for temporary transitions that have a two-phase firing process:
     * first consume tokens, wait for a predetermined time, then produce tokens.
     *
     * @param transition The index of the transition that consumes tokens
     */
    public void consumeTokens(int transition){
        if (transition < 0 || transition >= numT) {
            throw new IllegalArgumentException("Invalid transition index: " + transition);
        }
        if (!transitionEnabled(transition)) {
            throw new IllegalStateException("The transition " + transition + " is not enabled");
        }
        for(int i = 0; i < numP; i++){
            marking[i] -= pre[i][transition];
        }
    }

    /**
     * Produces tokens in places according to the post-conditions of a temporary transition.
     * This method completes the firing process of temporary transitions by adding
     * tokens to the output places.
     *
     * @param transition The index of the transition that produces tokens
     */
    public void produceTokens(int transition){
        if (transition < 0 || transition >= numT) {
            throw new IllegalArgumentException("Invalid transition index: " + transition);
        }
        for(int i = 0; i < numP; i++){
            marking[i] += post[i][transition];
        }
    }

    /**
     * Fires an immediate transition by applying the incidence matrix.
     * This method atomically consumes and produces tokens in a single operation
     * for immediate transitions (non-temporary transitions).
     *
     * @param transition The index of the immediate transition to fire
     */
    public void fire(int transition){
        if (transition < 0 || transition >= numT) {
            throw new IllegalArgumentException("Invalid transition index: " + transition);
        }
        if (!transitionEnabled(transition)) {
            throw new IllegalStateException("The transition " + transition + " is not enabled");
        }
        for(int i = 0; i < incidence.length; i++){
            marking[i] += incidence[i][transition];
        }
    }

    /**
     * Returns a copy of the current marking vector representing the net status.
     * This method provides a snapshot of the current state without exposing
     * the internal marking array for modification.
     *
     * @return A copy of the current marking matrix where each row represents
     *         the token count in the corresponding place
     */
    public int[] getMarking() {
        int[] copy = new int[marking.length];
        System.arraycopy(marking, 0, copy, 0, marking.length);
        return copy;
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
    public boolean isTemporary(int transitionValue) {
        if (transitionValue < 0 || transitionValue >= temporalTransitions.length) {
            throw new IllegalArgumentException("Invalid transition index: " + transitionValue);
        }
        return temporalTransitions[transitionValue] != 0;
    }

    /**
     * Gets the number of places in the Petri net.
     * @return The number of places
     */
    public int getNumPlaces() {
        return numP;
    }

    /**
     * Gets the number of transitions in the Petri net.
     * @return The number of transitions
     */
    public int getNumTransitions() {
        return numT;
    }

    public void setMarking(int[] newMarking) {
        if (newMarking.length != marking.length) {
            throw new IllegalArgumentException("Invalid marking size");
        }
        System.arraycopy(newMarking, 0, marking, 0, marking.length);
    }
}