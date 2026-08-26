import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyzes thread responsibilities and execution segments in a Petri net.
 *
 * <p>This class implements Algorithm 4.2 for determining thread responsibility by:
 *
 * <ul>
 *   <li>Classifying T-invariants as sequential or parallel
 *   <li>Identifying fork and join places in the net structure
 *   <li>Segmenting T-invariants into execution segments based on synchronization points
 * </ul>
 *
 * <p>A <b>fork</b> is a place with multiple output transitions (parallel split). A <b>join</b> is a
 * place with multiple input transitions (parallel merge). <b>Segments</b> are groups of transitions
 * that should be managed by the same thread pool.
 *
 * @author Sassi Juan Ignacio
 */
public class Responsibilities {

  /** Pre-incidence matrix (token consumption by transitions) */
  private final int[][] pre;

  /** Post-incidence matrix (token production by transitions) */
  private final int[][] post;

  /** Set of action places in the Petri net */
  private final Set<Integer> actionPlaces;

  /** List of minimal T-invariants */
  private final List<List<Integer>> tInvariants;

  /** T-invariants classified as sequential (no shared transitions) */
  private final List<List<Integer>> sequences;

  /** T-invariants classified as parallel (shared transitions) */
  private final List<List<Integer>> notSequences;

  /** Execution segments identified for thread allocation */
  private final List<List<Integer>> segments;

  /** Fork places (places with multiple output transitions) */
  private final List<Integer> forks;

  /** Join places (places with multiple input transitions) */
  private final List<Integer> joins;

  /**
   * Constructs a Responsibilities analyzer and performs the complete analysis.
   *
   * <p>The constructor automatically:
   *
   * <ol>
   *   <li>Classifies T-invariants as sequential or parallel
   *   <li>Identifies all fork and join places
   *   <li>Segments the T-invariants based on synchronization points
   * </ol>
   *
   * <p>Defensive copies of {@code pre} and {@code post} are made to prevent external modification.
   * {@code tInvariants} and {@code actionPlaces} come from {@link Invariants} and {@link
   * PlaceClassifier} respectively, which already return unmodifiable views.
   *
   * @param pre pre-incidence matrix of the Petri net
   * @param post post-incidence matrix of the Petri net
   * @param tInvariants list of minimal T-invariants
   * @param actionPlaces set of action places in the net
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "tInvariants and actionPlaces come from unmodifiable views returned by Invariants"
              + " and PlaceClassifier. No defensive copy needed.")
  public Responsibilities(
      int[][] pre, int[][] post, List<List<Integer>> tInvariants, Set<Integer> actionPlaces) {
    this.pre = deepCopy(pre);
    this.post = deepCopy(post);
    this.tInvariants = tInvariants;
    this.actionPlaces = actionPlaces;

    this.forks = new ArrayList<>();
    this.joins = new ArrayList<>();
    this.segments = new ArrayList<>();
    this.sequences = new ArrayList<>();
    this.notSequences = new ArrayList<>();

    classifyInvariants();
    analyzeForkOrJoin();
    segmentInvariants();
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

  /**
   * Classifies T-invariants as sequential or parallel based on transition sharing.
   *
   * <p>A T-invariant is classified as:
   *
   * <ul>
   *   <li><b>Sequential</b>: if it shares no transitions with any other T-invariant
   *   <li><b>Parallel</b>: if it shares at least one transition with another T-invariant
   * </ul>
   */
  private void classifyInvariants() {
    for (int i = 0; i < tInvariants.size(); i++) {
      List<Integer> current = tInvariants.get(i);
      boolean sharesTransitions = false;

      for (int j = 0; j < tInvariants.size(); j++) {
        if (i == j) continue;
        if (hasCommonTransitions(current, tInvariants.get(j))) {
          sharesTransitions = true;
          break;
        }
      }

      if (sharesTransitions) {
        notSequences.add(current);
      } else {
        sequences.add(current);
      }
    }
  }

  /**
   * Checks if two T-invariants share at least one active transition.
   *
   * @param it1 first T-invariant vector
   * @param it2 second T-invariant vector
   * @return true if both invariants have a positive value at the same index
   */
  private boolean hasCommonTransitions(List<Integer> it1, List<Integer> it2) {
    for (int i = 0; i < it1.size(); i++) {
      if (it1.get(i) > 0 && it2.get(i) > 0) {
        return true;
      }
    }
    return false;
  }

  /** Analyzes all action places to identify forks and joins. */
  private void analyzeForkOrJoin() {
    for (Integer p : actionPlaces) {
      if (isJoin(p)) joins.add(p);
      if (isFork(p)) forks.add(p);
    }
  }

  /**
   * Segments T-invariants into execution segments based on synchronization points.
   *
   * <p>Segmentation rules:
   *
   * <ul>
   *   <li>Sequential T-invariants form a single segment each
   *   <li>Parallel T-invariants are split at transitions that produce tokens into fork or join
   *       places
   * </ul>
   *
   * <p>A {@link LinkedHashSet} is used to preserve insertion order while eliminating duplicate
   * segments.
   */
  private void segmentInvariants() {
    Set<List<Integer>> uniqueSegments = new LinkedHashSet<>();

    for (List<Integer> seq : sequences) {
      List<Integer> indices = activeTransitionIndices(seq);
      if (!indices.isEmpty()) {
        uniqueSegments.add(indices);
      }
    }

    for (List<Integer> parallel : notSequences) {
      segmentParallelInvariant(activeTransitionIndices(parallel), uniqueSegments);
    }

    segments.addAll(uniqueSegments);
  }

  /**
   * Extracts the indices of active transitions (value &gt; 0) from a T-invariant vector.
   *
   * @param invariant the T-invariant vector
   * @return list of transition indices with positive values
   */
  private List<Integer> activeTransitionIndices(List<Integer> invariant) {
    List<Integer> indices = new ArrayList<>();
    for (int idx = 0; idx < invariant.size(); idx++) {
      if (invariant.get(idx) > 0) {
        indices.add(idx);
      }
    }
    return indices;
  }

  /**
   * Splits a parallel T-invariant into segments at fork and join cut points.
   *
   * @param activeTransitions ordered list of active transition indices
   * @param uniqueSegments set where the resulting segments are added
   */
  private void segmentParallelInvariant(
      List<Integer> activeTransitions, Set<List<Integer>> uniqueSegments) {
    List<Integer> currentSegment = new ArrayList<>();

    for (int t : activeTransitions) {
      currentSegment.add(t);

      if (producesToForkOrJoin(t)) {
        uniqueSegments.add(new ArrayList<>(currentSegment));
        currentSegment.clear();
      }
    }

    if (!currentSegment.isEmpty()) {
      uniqueSegments.add(new ArrayList<>(currentSegment));
    }
  }

  /**
   * Checks whether a transition produces tokens into any fork or join place.
   *
   * @param transition the transition index to check
   * @return true if the transition outputs to at least one fork or join place
   */
  private boolean producesToForkOrJoin(int transition) {
    for (int p = 0; p < post.length; p++) {
      if (post[p][transition] > 0 && (forks.contains(p) || joins.contains(p))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines if a place is a fork (parallel split point).
   *
   * <p>A place is a fork if more than one transition consumes tokens from it (multiple output arcs
   * in the pre-incidence matrix).
   *
   * @param place the index of the place to check
   * @return true if the place has more than one output transition
   */
  private boolean isFork(int place) {
    int outputTransitions = 0;
    for (int t = 0; t < pre[place].length; t++) {
      if (pre[place][t] > 0) outputTransitions++;
    }
    return outputTransitions > 1;
  }

  /**
   * Determines if a place is a join (parallel merge point).
   *
   * <p>A place is a join if more than one transition produces tokens into it (multiple input arcs
   * in the post-incidence matrix).
   *
   * @param place the index of the place to check
   * @return true if the place has more than one input transition
   */
  private boolean isJoin(int place) {
    int inputTransitions = 0;
    for (int t = 0; t < post[place].length; t++) {
      if (post[place][t] > 0) inputTransitions++;
    }
    return inputTransitions > 1;
  }

  /**
   * Gets the list of identified execution segments.
   *
   * @return unmodifiable list where each element is a segment (list of transition indices)
   */
  public List<List<Integer>> getSegments() {
    return Collections.unmodifiableList(segments);
  }

  /**
   * Gets the list of fork places identified in the analysis.
   *
   * @return unmodifiable list of place indices that are forks
   */
  public List<Integer> getForkPlaces() {
    return Collections.unmodifiableList(forks);
  }

  /**
   * Gets the list of join places identified in the analysis.
   *
   * @return unmodifiable list of place indices that are joins
   */
  public List<Integer> getJoinPlaces() {
    return Collections.unmodifiableList(joins);
  }
}
