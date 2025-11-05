/**
 * Singleton class representing a Petri net model for a travel agency system.
 * This class models the workflow of client reservations including resources like
 * doors, assistants, managers, and agents. It supports both immediate and temporary
 * transitions with token consumption, production and fire mechanisms.
 *
 * The Petri net consists of 15 places (P0-P14) and 12 transitions (T0-T11)
 * representing different states and operations in the reservation process.
 *
 * @see PetrinetLoader
 * @author Sassi Juan Ignacio
 */
public class PetriNet { 
    /** Single static instance of PetriNet following Singleton pattern */
    private static PetriNet petriNet = new PetriNet();

    /** 
     * Current marking vector representing the current state of the Petri net.
     * Each element represents the number of tokens in the corresponding place.
     */
    private int[] marking;

    /**
     * Vector indicating which transitions are temporal.
     * A value of 1 indicates a temporal transition, 0 indicates an immediate transition.
     */
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

    /** The number of places in the Petri net */
    private final int numP;

    /** The number of transitions in the Petri net */
    private final int numT;

    /**
     * Private constructor implementing Singleton pattern.
     * Initializes the Petri net by loading configuration from PetrinetLoader,
     * validates dimensions, and calculates the incidence matrix.
     * 
     * @throws RuntimeException if matrix dimensions are inconsistent or if
     *         vectors don't match the expected sizes
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
        incidence = Matrix.subtract(post, pre);
    }

    /**
     * Gets the unique instance of PetriNet following the Singleton pattern.
     * This method ensures only one instance of PetriNet exists throughout
     * the application lifecycle.
     *
     * @return the single instance of PetriNet
     */
    public static PetriNet getInstance() {
        return petriNet;
    }

    /**
     * Checks if a transition is enabled (ready to be fired).
     * A transition is enabled when the current marking has enough tokens
     * in all input places to satisfy the pre-conditions defined in the
     * pre-incidence matrix.
     *
     * @param transition the index of the transition to check (0-based)
     * @return true if the transition can be fired, false otherwise
     * @throws IllegalArgumentException if the transition index is out of bounds
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
     * Consumes tokens from places according to the pre-conditions of a temporal transition.
     * This method is the first phase of firing a temporal transition. It removes tokens
     * from input places but does not yet add tokens to output places. The produceTokens
     * method should be called after a time delay to complete the transition firing.
     * 
     * <p>This two-phase approach models transitions that take time to complete,
     * such as a service operation that has a duration.</p>
     *
     * @param transition the index of the temporal transition that consumes tokens
     * @throws IllegalArgumentException if the transition index is out of bounds
     * @throws IllegalStateException if the transition is not currently enabled
     * @see #produceTokens(int)
     * @see #isTemporary(int)
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
     * Produces tokens in places according to the post-conditions of a temporal transition.
     * This method is the second phase of firing a temporal transition. It adds tokens
     * to output places as defined in the post-incidence matrix. This method should be
     * called after consumeTokens and after the appropriate time delay has elapsed.
     * 
     * <p>Note: This method does not validate if the transition was previously
     * enabled or if consumeTokens was called. The caller is responsible for
     * proper sequencing.</p>
     *
     * @param transition the index of the temporal transition that produces tokens
     * @throws IllegalArgumentException if the transition index is out of bounds
     * @see #consumeTokens(int)
     * @see #isTemporary(int)
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
     * This method atomically consumes and produces tokens in a single operation,
     * which is appropriate for immediate transitions that complete instantaneously.
     * The net effect is to add incidence[i][transition] tokens to place i for all places.
     * 
     * <p>For temporal transitions, use consumeTokens followed by produceTokens instead.</p>
     *
     * @param transition the index of the immediate transition to fire
     * @throws IllegalArgumentException if the transition index is out of bounds
     * @throws IllegalStateException if the transition is not currently enabled
     * @see #consumeTokens(int)
     * @see #produceTokens(int)
     * @see #isTemporary(int)
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
     * Returns a copy of the current marking vector representing the net state.
     * This method provides a snapshot of the current distribution of tokens
     * across all places without exposing the internal marking array for modification.
     * Each element in the returned array represents the number of tokens in the
     * corresponding place.
     *
     * @return a copy of the current marking vector where element [i] represents
     *         the token count in place i
     */
    public int[] getMarking() {
        int[] copy = new int[marking.length];
        System.arraycopy(marking, 0, copy, 0, marking.length);
        return copy;
    }

    /**
     * Checks if a given transition is a temporal (time-delayed) transition.
     * Temporal transitions use a two-phase firing mechanism: first consuming tokens,
     * waiting for a predetermined time, and finally producing tokens. Immediate
     * transitions fire instantaneously without delay.
     * 
     * <p>The temporal characteristic is defined in the configuration loaded by
     * PetrinetLoader.</p>
     *
     * @param transitionValue the index of the transition to check
     * @return true if the transition is temporal, false if it is immediate
     * @throws IllegalArgumentException if the transition index is out of bounds
     * @see #consumeTokens(int)
     * @see #produceTokens(int)
     */
    public boolean isTemporary(int transitionValue) {
        if (transitionValue < 0 || transitionValue >= temporalTransitions.length) {
            throw new IllegalArgumentException("Invalid transition index: " + transitionValue);
        }
        return temporalTransitions[transitionValue] != 0;
    }

    /**
     * Gets the number of places in the Petri net.
     * Places represent states or resources in the modeled system.
     * 
     * @return the total number of places
     */
    public int getNumPlaces() {
        return numP;
    }

    /**
     * Gets the number of transitions in the Petri net.
     * Transitions represent events or operations that change the system state.
     * 
     * @return the total number of transitions
     */
    public int getNumTransitions() {
        return numT;
    }

    /**
     * Sets the current marking to a new state.
     * This method allows external control of the Petri net state, which can be
     * useful for simulation, state restoration, or testing purposes.
     * 
     * <p>The new marking must have the same size as the current marking
     * (equal to the number of places).</p>
     * 
     * @param newMarking the new marking vector to set, where element [i] represents
     *                   the number of tokens to place in place i
     * @throws IllegalArgumentException if the new marking size doesn't match
     *         the number of places in the Petri net
     */
    public void setMarking(int[] newMarking) {
        if (newMarking.length != marking.length) {
            throw new IllegalArgumentException("Invalid marking size");
        }
        System.arraycopy(newMarking, 0, marking, 0, marking.length);
    }

    /**
     * Resets the Petri net to its initial state.
     * This method restores the marking to the initial marking vector defined
     * in the configuration file. All tokens are repositioned to their original
     * places, effectively resetting the Petri net to its starting configuration.
     * 
     * <p>This is useful for restarting simulations, running multiple test scenarios,
     * or recovering from an error state.</p>
     * 
     * @see PetrinetLoader#getInitialMarkingVector()
     */
    public void resetPetrinet(){
        this.marking = PetrinetLoader.getInitialMarkingVector();
    }
}