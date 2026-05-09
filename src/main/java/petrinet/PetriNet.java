import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Singleton class representing a Petri net model loaded from configuration.
 *
 * <p>This class models the workflow of a system including resources, states and operations. It
 * supports both immediate and temporal transitions with token consumption, production and fire
 * mechanisms.
 *
 * <p>The Petri net structure is loaded from the properties file specified in {@code
 * config.properties} via {@link PetrinetLoader}.
 *
 * @see PetrinetLoader
 * @author Sassi Juan Ignacio
 */
public final class PetriNet {

  /** Single static instance following the Singleton pattern. */
  private static final PetriNet INSTANCE = new PetriNet();

  /**
   * Current marking vector representing the state of the Petri net. Each element represents the
   * number of tokens in the corresponding place.
   */
  private int[] marking;

  /**
   * Vector indicating which transitions are temporal. A value of 1 indicates a temporal transition;
   * 0 indicates an immediate transition.
   */
  private final int[] temporalTransitions;

  /**
   * Pre-incidence matrix. Element [i][j] represents the number of tokens consumed from place Pi
   * when transition Tj fires.
   */
  private final int[][] pre;

  /**
   * Post-incidence matrix. Element [i][j] represents the number of tokens produced in place Pi when
   * transition Tj fires.
   */
  private final int[][] post;

  /**
   * Incidence matrix (Post - Pre). Element [i][j] represents the net token change for place Pi when
   * transition Tj fires.
   */
  private final int[][] incidence;

  /** Number of places in the Petri net. */
  private final int numPlaces;

  /** Number of transitions in the Petri net. */
  private final int numTransitions;

  /**
   * Private constructor implementing the Singleton pattern. Loads the Petri net from configuration,
   * validates dimensions, and computes the incidence matrix.
   *
   * @throws RuntimeException if matrix dimensions are inconsistent or vectors have wrong sizes
   */
  private PetriNet() {
    this.numPlaces = PetrinetLoader.getNumPlaces();
    this.numTransitions = PetrinetLoader.getNumTransitions();
    this.pre = PetrinetLoader.getPreMatrix();
    this.post = PetrinetLoader.getPostMatrix();
    this.marking = PetrinetLoader.getInitialMarkingVector();
    this.temporalTransitions = PetrinetLoader.getTemporalTransitionsVector();

    validateDimensions();

    this.incidence = Matrix.subtract(post, pre);
  }

  /**
   * Validates that all loaded matrices and vectors have consistent dimensions.
   *
   * @throws RuntimeException if any dimension mismatch is found
   */
  private void validateDimensions() {
    if (pre.length != post.length) {
      throw new RuntimeException("Pre and Post matrices must have the same number of rows");
    }
    if (pre[0].length != post[0].length) {
      throw new RuntimeException("Pre and Post matrices must have the same number of columns");
    }
    if (marking.length != numPlaces) {
      throw new RuntimeException("Initial marking vector must have " + numPlaces + " elements");
    }
    if (temporalTransitions.length != numTransitions) {
      throw new RuntimeException(
          "Temporal transitions vector must have " + numTransitions + " elements");
    }
  }

  /**
   * Returns the unique instance of {@code PetriNet}.
   *
   * @return the singleton instance
   */
  @SuppressFBWarnings(
      value = "MS_EXPOSE_REP",
      justification = "Acceso intencional al Singleton; los cambios de estado están controlados.")
  public static PetriNet getInstance() {
    return INSTANCE;
  }

  /**
   * Checks whether a transition is enabled (can be fired). A transition is enabled when all input
   * places have enough tokens to satisfy the pre-conditions.
   *
   * @param transition the index of the transition to check (0-based)
   * @return true if the transition can be fired, false otherwise
   * @throws IllegalArgumentException if the transition index is out of bounds
   */
  public boolean transitionEnabled(int transition) {
    validateTransition(transition);
    for (int i = 0; i < numPlaces; i++) {
      if (marking[i] < pre[i][transition]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Consumes tokens from input places for a temporal transition (phase 1 of 2).
   *
   * <p>Call {@link #produceTokens(int)} after the time delay to complete the firing.
   *
   * @param transition the index of the temporal transition
   * @throws IllegalArgumentException if the transition index is out of bounds
   * @throws IllegalStateException if the transition is not currently enabled
   * @see #produceTokens(int)
   */
  public void consumeTokens(int transition) {
    validateTransition(transition);
    if (!transitionEnabled(transition)) {
      throw new IllegalStateException("Transition " + transition + " is not enabled");
    }
    for (int i = 0; i < numPlaces; i++) {
      marking[i] -= pre[i][transition];
    }
  }

  /**
   * Produces tokens in output places for a temporal transition (phase 2 of 2).
   *
   * <p>Must be called after {@link #consumeTokens(int)} once the time delay has elapsed.
   *
   * @param transition the index of the temporal transition
   * @throws IllegalArgumentException if the transition index is out of bounds
   * @see #consumeTokens(int)
   */
  public void produceTokens(int transition) {
    validateTransition(transition);
    for (int i = 0; i < numPlaces; i++) {
      marking[i] += post[i][transition];
    }
  }

  /**
   * Fires an immediate transition atomically (consume + produce in one step).
   *
   * <p>For temporal transitions use {@link #consumeTokens(int)} and {@link #produceTokens(int)}.
   *
   * @param transition the index of the immediate transition to fire
   * @throws IllegalArgumentException if the transition index is out of bounds
   * @throws IllegalStateException if the transition is not currently enabled
   */
  public void fire(int transition) {
    validateTransition(transition);
    if (!transitionEnabled(transition)) {
      throw new IllegalStateException("Transition " + transition + " is not enabled");
    }
    for (int i = 0; i < incidence.length; i++) {
      marking[i] += incidence[i][transition];
    }
  }

  /**
   * Returns a copy of the current marking vector.
   *
   * @return a new array with the current token counts for each place
   */
  public int[] getMarking() {
    return marking.clone();
  }

  /**
   * Sets the current marking to a new state. A defensive copy is made to prevent external
   * modification.
   *
   * @param newMarking the new marking vector
   * @throws IllegalArgumentException if the new marking size does not match the number of places
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "A defensive copy is made via clone() — internal state is not exposed.")
  public void setMarking(int[] newMarking) {
    if (newMarking.length != marking.length) {
      throw new IllegalArgumentException(
          "Marking size must be " + marking.length + ", got " + newMarking.length);
    }
    this.marking = newMarking.clone();
  }

  /**
   * Resets the Petri net to its initial marking as defined in the configuration.
   *
   * @see PetrinetLoader#getInitialMarkingVector()
   */
  public void reset() {
    this.marking = PetrinetLoader.getInitialMarkingVector();
  }

  /**
   * Checks whether a transition is temporal (time-delayed).
   *
   * @param transition the index of the transition to check
   * @return true if the transition is temporal, false if immediate
   * @throws IllegalArgumentException if the transition index is out of bounds
   */
  public boolean isTemporary(int transition) {
    validateTransition(transition);
    return temporalTransitions[transition] != 0;
  }

  /**
   * Returns the number of places in the Petri net.
   *
   * @return total number of places
   */
  public int getNumPlaces() {
    return numPlaces;
  }

  /**
   * Returns the number of transitions in the Petri net.
   *
   * @return total number of transitions
   */
  public int getNumTransitions() {
    return numTransitions;
  }

  /**
   * Validates that a transition index is within bounds.
   *
   * @param transition the transition index to validate
   * @throws IllegalArgumentException if the index is out of bounds
   */
  private void validateTransition(int transition) {
    if (transition < 0 || transition >= numTransitions) {
      throw new IllegalArgumentException(
          "Invalid transition index: "
              + transition
              + ". Must be between 0 and "
              + (numTransitions - 1));
    }
  }
}
