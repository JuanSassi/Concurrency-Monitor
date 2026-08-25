import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Computes the sets of places associated with each T-invariant of a Petri net.
 *
 * <p>Two views of the same underlying computation are exposed:
 *
 * <ul>
 *   <li><b>PI of IT</b> — every place (input or output, mixed) touched by any active transition of
 *       the invariant, via either the pre or post incidence matrix, regardless of its
 *       classification.
 *   <li><b>PA of IT</b> — the same set filtered down to action places only, i.e. with resource,
 *       idle and restriction places excluded.
 * </ul>
 *
 * <p>Both sets are computed once, at construction time, by a single traversal per T-invariant. This
 * class exists so that the traversal lives in exactly one place: it used to be duplicated between
 * {@link PlaceClassifier} (twice, once per view) and {@code ThreadAllocator}.
 *
 * <p>Place indices are always returned in ascending order.
 *
 * @see PlaceClassifier
 * @author Sassi Juan Ignacio
 */
public class TInvariantPlaces {

  /** Pre-incidence matrix (token consumption by transitions). */
  private final int[][] pre;

  /** Post-incidence matrix (token production by transitions). */
  private final int[][] post;

  /** Total number of places in the net. */
  private final int numPlaces;

  /** Total number of transitions in the net. */
  private final int numTransitions;

  /** All places connected to each T-invariant, one entry per invariant. */
  private final List<List<Integer>> piOfIt;

  /** Action places connected to each T-invariant, one entry per invariant. */
  private final List<List<Integer>> paOfIt;

  /**
   * Constructs the analyzer and computes both place sets for every T-invariant.
   *
   * <p>Defensive copies of the incidence matrices are made. {@code actionPlaces} is copied into an
   * immutable set, so the classification result is frozen at construction time.
   *
   * @param pre pre-incidence matrix of dimension [numPlaces][numTransitions]
   * @param post post-incidence matrix of dimension [numPlaces][numTransitions]
   * @param tInvariants list of minimal T-invariants
   * @param actionPlaces set of place indices already classified as action places
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "pre/post are deep-copied below and actionPlaces is copied into an immutable set;"
              + " tInvariants is only read during construction and never retained.")
  public TInvariantPlaces(
      int[][] pre, int[][] post, List<List<Integer>> tInvariants, Set<Integer> actionPlaces) {
    this.pre = deepCopy(pre);
    this.post = deepCopy(post);
    this.numPlaces = pre.length;
    this.numTransitions = pre[0].length;

    Set<Integer> frozenActionPlaces = Set.copyOf(actionPlaces);

    List<List<Integer>> allPlaces = new ArrayList<>();
    List<List<Integer>> onlyActionPlaces = new ArrayList<>();

    for (List<Integer> tInv : tInvariants) {
      List<Integer> places = placesOf(tInv);
      allPlaces.add(places);
      onlyActionPlaces.add(places.stream().filter(frozenActionPlaces::contains).toList());
    }

    this.piOfIt = Collections.unmodifiableList(allPlaces);
    this.paOfIt = Collections.unmodifiableList(onlyActionPlaces);
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
   * Collects every place connected to an active transition of the given T-invariant.
   *
   * <p>A place is included when it is read from or written to by at least one transition with a
   * positive coefficient in the invariant, i.e. when {@code pre[p][t] > 0} or {@code post[p][t] >
   * 0}.
   *
   * @param tInvariant the T-invariant vector
   * @return sorted, immutable list of the place indices involved
   */
  private List<Integer> placesOf(List<Integer> tInvariant) {
    Set<Integer> involved = new TreeSet<>();

    for (int t = 0; t < numTransitions; t++) {
      if (tInvariant.get(t) <= 0) {
        continue;
      }
      for (int p = 0; p < numPlaces; p++) {
        if (pre[p][t] > 0 || post[p][t] > 0) {
          involved.add(p);
        }
      }
    }

    return List.copyOf(involved);
  }

  /**
   * Gets all places (input and output, mixed) connected to each T-invariant.
   *
   * @return unmodifiable list of sorted place index lists, one per T-invariant
   */
  public List<List<Integer>> getPiOfIt() {
    return piOfIt;
  }

  /**
   * Gets the action places associated with each T-invariant.
   *
   * @return unmodifiable list of sorted action place index lists, one per T-invariant
   */
  public List<List<Integer>> getPaOfIt() {
    return paOfIt;
  }
}
