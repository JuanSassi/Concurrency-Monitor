import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Automatic classification of Petri net places based on P-invariant analysis.
 *
 * <p>This class implements an algorithm that categorizes places into two main types:
 *
 * <ul>
 *   <li><b>Action places</b>: Participate in multiple P-invariants, representing active system
 *       states or work-in-progress.
 *   <li><b>Resource places</b>: Appear in a single P-invariant, including:
 *       <ul>
 *         <li>Resource places (available resources)
 *         <li>Idle places (idle state with tokens)
 *         <li>Restriction places (capacity constraints)
 *       </ul>
 * </ul>
 *
 * <p>The classification itself depends only on the P-invariants, the initial marking and the
 * incidence matrix. The T-invariant based sets (PI of IT and PA of IT) are computed by {@link
 * TInvariantPlaces} and merely re-exposed here for backward compatibility with existing callers.
 *
 * <p>This class performs no console output: rendering the results is the caller's responsibility.
 *
 * @see TInvariantPlaces
 * @author Juan Ignacio Sassi
 */
public class PlaceClassifier {

  /** Incidence matrix (W = Post - Pre), computed internally */
  private final int[][] w;

  /** Initial marking vector of the net */
  private final int[] m0;

  /** Total number of places in the net */
  private final int numPlaces;

  /** Total number of transitions in the net */
  private final int numTransitions;

  /** Net invariants calculator */
  private final Invariants invariants;

  /** Set of places classified as resources (includes idle, resources and restrictions) */
  private final Set<Integer> resourcePlaces;

  /** Set of places classified as action places */
  private final Set<Integer> actionPlaces;

  /** Candidate places to be resources, refined in phase 2 */
  private final Set<Integer> possibleResource;

  /** Place sets associated with each T-invariant, derived from the classification above */
  private final TInvariantPlaces tInvariantPlaces;

  /**
   * Constructs the classifier and performs automatic place classification.
   *
   * <p>The incidence matrix W is computed internally as Post - Pre. A defensive copy of the marking
   * vector is made to prevent external modification; the incidence matrices are only read here and
   * handed to {@link TInvariantPlaces}, which makes its own copies.
   *
   * @param pre pre-incidence matrix of dimension [numPlaces][numTransitions]
   * @param post post-incidence matrix of dimension [numPlaces][numTransitions]
   * @param m0 initial marking vector of dimension [numPlaces]
   * @param invariants invariants calculator containing P-invariants and T-invariants
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Invariants is effectively immutable: all fields are final and getters return"
              + " unmodifiable views. No defensive copy is needed.")
  public PlaceClassifier(int[][] pre, int[][] post, int[] m0, Invariants invariants) {
    this.m0 = m0.clone();
    this.w = Matrix.subtract(post, pre);
    this.numPlaces = pre.length;
    this.numTransitions = pre[0].length;
    this.invariants = invariants;

    this.resourcePlaces = new HashSet<>();
    this.actionPlaces = new HashSet<>();
    this.possibleResource = new HashSet<>();

    classifyPlaces();

    this.tInvariantPlaces =
        new TInvariantPlaces(pre, post, invariants.getTInvariants(), actionPlaces);
  }

  /**
   * Classifies the net's places into action places and resource/restriction/idle places.
   *
   * <p>The algorithm implements two phases:
   *
   * <p><b>Phase 1: Initial classification based on P-invariant participation</b>
   *
   * <ul>
   *   <li>Places in a single P-invariant → candidates for resource/restriction/idle
   *   <li>Places in multiple P-invariants → action places
   * </ul>
   *
   * <p><b>Phase 2: Refinement of candidate classification</b>
   *
   * <ul>
   *   <li>If a P-invariant has a single candidate → confirmed as resource/restriction/idle
   *   <li>If a P-invariant has multiple candidates → identify the idle place using topological
   *       criteria. Remaining candidates are reclassified as action places.
   * </ul>
   */
  private void classifyPlaces() {
    List<List<Integer>> pInvariants = invariants.getPInvariants();

    // Phase 1: classify places by how many P-invariants they participate in
    for (int p = 0; p < numPlaces; p++) {
      int count = 0;
      for (List<Integer> inv : pInvariants) {
        if (inv.get(p) > 0) count++;
      }
      if (count == 1) {
        possibleResource.add(p);
      } else if (count > 1) {
        actionPlaces.add(p);
      }
    }

    // Phase 2: determine resource, idle, or restriction places
    for (List<Integer> inv : pInvariants) {
      List<Integer> involved =
          possibleResource.stream().filter(r -> r < inv.size() && inv.get(r) > 0).toList();

      if (involved.size() == 1) {
        resourcePlaces.addAll(involved);
      } else if (involved.size() > 1) {
        classifyMultipleCandidates(involved);
      }
    }
  }

  /**
   * Classifies a group of candidate resource places when more than one belongs to the same
   * P-invariant. The first idle place found is confirmed as a resource; the rest are reclassified
   * as action places.
   *
   * @param candidates list of candidate place indices sharing the same P-invariant
   */
  private void classifyMultipleCandidates(List<Integer> candidates) {
    boolean idleFound = false;
    for (Integer place : candidates) {
      if (!idleFound && isIdlePlace(place)) {
        resourcePlaces.add(place);
        idleFound = true;
      } else {
        actionPlaces.add(place);
      }
    }
  }

  /**
   * Determines if a place is an idle place according to topological criteria.
   *
   * <p>A place is classified as idle if it satisfies ALL of the following:
   *
   * <ol>
   *   <li>Initial marking &gt; 0 (contains tokens at start)
   *   <li>Exactly 1 input transition (one way to enter)
   *   <li>Exactly 1 output transition (one way to exit)
   * </ol>
   *
   * @param place the index of the place to check
   * @return true if the place satisfies all idle place criteria
   */
  private boolean isIdlePlace(int place) {
    if (m0[place] == 0) {
      return false;
    }

    int inputTransitions = 0;
    int outputTransitions = 0;

    for (int t = 0; t < numTransitions; t++) {
      if (w[place][t] > 0) inputTransitions++;
      if (w[place][t] < 0) outputTransitions++;
    }

    return inputTransitions == 1 && outputTransitions == 1;
  }

  /**
   * Gets the set of action places identified by the classification algorithm.
   *
   * @return unmodifiable set of action place indices
   */
  public Set<Integer> getActionPlaces() {
    return Collections.unmodifiableSet(actionPlaces);
  }

  /**
   * Gets the set of resource, restriction, and idle places identified by the classification
   * algorithm.
   *
   * @return unmodifiable set of resource place indices
   */
  public Set<Integer> getResourcePlaces() {
    return Collections.unmodifiableSet(resourcePlaces);
  }

  /**
   * Gets the action places associated with each T-invariant.
   *
   * @return unmodifiable list of action place index lists, one per T-invariant
   */
  public List<List<Integer>> getPaOfIt() {
    return tInvariantPlaces.getPaOfIt();
  }

  /**
   * Gets all places (input and output, mixed) connected to each T-invariant.
   *
   * @return unmodifiable list of place index lists, one per T-invariant
   */
  public List<List<Integer>> getPiOfIt() {
    return tInvariantPlaces.getPiOfIt();
  }
}
