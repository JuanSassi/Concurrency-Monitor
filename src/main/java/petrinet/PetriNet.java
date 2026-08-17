import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents a Petri net model: places, transitions, and the pre/post incidence matrices that
 * govern token flow.
 *
 * <p>This class models the workflow of a system including resources, states and operations. It
 * supports both immediate and temporal transitions with token consumption, production and fire
 * mechanisms.
 *
 * <p>Each {@code PetriNet} instance is self-contained: it owns its own marking and its own copy of
 * the incidence matrices. This makes it safe to create one instance per analysis run — no shared
 * global state between runs, and no risk of two concurrent runs interfering with each other's
 * marking.
 *
 * @author Sassi Juan Ignacio
 */
public final class PetriNet {

  /**
   * Current marking vector representing the state of the Petri net. Each element represents the
   * number of tokens in the corresponding place.
   */
  private int[] marking;

  /** Initial marking vector, kept to support {@link #reset()} without any external dependency. */
  private final int[] initialMarking;

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
   * Constructs a {@code PetriNet} from explicit structural data.
   *
   * <p>Defensive copies of {@code pre}, {@code post}, {@code initialMarking} and {@code
   * temporalTransitions} are made, so the caller's arrays can be freely reused or mutated
   * afterwards without affecting this instance.
   *
   * @param pre pre-incidence matrix, dimension [numPlaces][numTransitions]
   * @param post post-incidence matrix, dimension [numPlaces][numTransitions]
   * @param initialMarking initial marking vector, dimension [numPlaces]
   * @param temporalTransitions per-transition temporal flag vector, dimension [numTransitions]
   * @throws IllegalArgumentException if matrix dimensions are inconsistent or vectors have wrong
   *     sizes
   */
  public PetriNet(int[][] pre, int[][] post, int[] initialMarking, int[] temporalTransitions) {
    this.pre = deepCopy(pre);
    this.post = deepCopy(post);
    this.initialMarking = initialMarking.clone();
    this.marking = initialMarking.clone();
    this.temporalTransitions = temporalTransitions.clone();

    this.numPlaces = this.pre.length;
    this.numTransitions = this.pre.length > 0 ? this.pre[0].length : 0;

    validateDimensions();

    this.incidence = Matrix.subtract(this.post, this.pre);
  }

  /**
   * Convenience factory that builds a {@code PetriNet} from the properties file configured in
   * {@code config.properties} — equivalent to the console/catalog behaviour this project had before
   * supporting user-submitted nets.
   *
   * <p>Unlike the old singleton, calling this twice returns two independent instances, each with
   * its own marking.
   *
   * @return a new {@code PetriNet} built from the currently configured properties
   */
  public static PetriNet fromProperties() {
    return new PetriNet(
        PetrinetLoader.getPreMatrix(),
        PetrinetLoader.getPostMatrix(),
        PetrinetLoader.getInitialMarkingVector(),
        PetrinetLoader.getTemporalTransitionsVector());
  }

  /**
   * Returns a deep copy of a 2D integer matrix.
   *
   * @param matrix the matrix to copy
   * @return a new matrix with the same values
   */
  private static int[][] deepCopy(int[][] matrix) {
    int[][] copy = new int[matrix.length][];
    for (int i = 0; i < matrix.length; i++) {
      copy[i] = matrix[i].clone();
    }
    return copy;
  }

  private void validateDimensions() {
    validateRowsMatch();
    validateColumnsMatch();
    validateMarkingSize();
    validateTemporalTransitionsSize();
  }

  private void validateRowsMatch() {
    if (pre.length != post.length) {
      throw new IllegalArgumentException("Pre and Post matrices must have the same number of rows");
    }
  }

  private void validateColumnsMatch() {
    if (numTransitions > 0 && pre[0].length != post[0].length) {
      throw new IllegalArgumentException(
          "Pre and Post matrices must have the same number of columns");
    }
  }

  private void validateMarkingSize() {
    if (marking.length != numPlaces) {
      throw new IllegalArgumentException(
          "Initial marking vector must have " + numPlaces + " elements");
    }
  }

  private void validateTemporalTransitionsSize() {
    if (temporalTransitions.length != numTransitions) {
      throw new IllegalArgumentException(
          "Temporal transitions vector must have " + numTransitions + " elements");
    }
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
   * Resets this net to its own initial marking — the one it was constructed with, not whatever is
   * currently in {@code config.properties}. Each instance is independent.
   */
  public void reset() {
    this.marking = initialMarking.clone();
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
