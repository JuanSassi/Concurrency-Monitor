import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Computes and stores all reachable markings of the action places in a Petri net.
 *
 * <p>The reachability set is built using breadth-first search (BFS) starting from the initial
 * marking. Only the token counts of action places are stored per marking, reducing memory usage and
 * focusing on the states relevant to thread allocation.
 *
 * <p>This class supports Algorithms 4.1 and 4.3 from Ventre &amp; Micolini (2021):
 *
 * <ul>
 *   <li>Algorithm 4.1 — maximum number of simultaneously active threads
 *   <li>Algorithm 4.3 — maximum threads per execution segment
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
public class ReachabilityTree {

  /**
   * Ceiling on the number of distinct markings explored before giving up.
   *
   * <p>An unbounded net has an infinite reachability set — a single transition that produces tokens
   * without consuming any is enough. The BFS below would never terminate and would exhaust the heap
   * on the way. Since boundedness is not checked anywhere upstream, and nets can now arrive from a
   * client, the search needs a hard stop.
   */
  private static final int MAX_REACHABLE_MARKINGS = 50_000;

  /** Petri net instance used for state-space exploration. */
  private final PetriNet petriNet;

  /** Sorted list of action place indices to track. */
  private final List<Integer> sortedActionPlaces;

  /**
   * All reachable markings projected onto action places, in BFS discovery order. Each entry is an
   * array of token counts indexed by position in {@code sortedActionPlaces}.
   */
  private final List<int[]> reachableMarkings;

  /** Maximum total tokens across all action places in any reachable marking. */
  private final int maxNumThreads;

  /**
   * Constructs the reachability tree for the given action places over the given Petri net. The
   * complete reachability set is built immediately during construction.
   *
   * <p>{@code actionPlaces} comes from {@link PlaceClassifier#getActionPlaces()}, which already
   * returns an unmodifiable view — no defensive copy is needed. {@code petriNet} is owned by the
   * caller (typically {@code ThreadAllocator}) for the lifetime of one analysis run — it is
   * intentionally shared, not defensively copied, so that a {@code Monitor} built afterwards can
   * reuse the exact same net instance.
   *
   * @param actionPlaces set of place indices classified as action places
   * @param petriNet the Petri net to explore; restored to its initial marking when this constructor
   *     returns
   * @throws PetriNetValidationException if the net appears to be unbounded
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "actionPlaces comes from PlaceClassifier.getActionPlaces() (unmodifiable view)."
              + " petriNet is intentionally shared with the caller for the lifetime of one"
              + " analysis run — not a defensive-copy candidate.")
  public ReachabilityTree(Set<Integer> actionPlaces, PetriNet petriNet) {
    this.petriNet = petriNet;
    this.sortedActionPlaces =
        Collections.unmodifiableList(new ArrayList<>(new TreeSet<>(actionPlaces)));
    this.reachableMarkings = new ArrayList<>();

    buildReachabilitySet();
    this.maxNumThreads = calculateMaxNumThreads();
  }

  /**
   * Explores the full reachability set using BFS.
   *
   * <p>Duplicate detection is performed via a {@link HashMap} keyed on the string representation of
   * the full marking. This is necessary because {@code int[]} does not implement {@code
   * equals}/{@code hashCode} by content.
   *
   * <p>After each transition firing the Petri net is restored to the current BFS marking so that
   * all enabled transitions can be tried independently.
   *
   * @throws PetriNetValidationException if the reachability set exceeds {@link
   *     #MAX_REACHABLE_MARKINGS}
   */
  private void buildReachabilitySet() {
    Map<String, Boolean> visited = new HashMap<>();
    Queue<int[]> queue = new LinkedList<>();

    int[] initialMarking = petriNet.getMarking();
    String initialKey = Arrays.toString(initialMarking);

    visited.put(initialKey, Boolean.TRUE);
    reachableMarkings.add(extractActionMarking(initialMarking));
    queue.add(initialMarking);

    while (!queue.isEmpty()) {
      int[] current = queue.poll();
      petriNet.setMarking(current);

      for (int t = 0; t < petriNet.getNumTransitions(); t++) {
        if (!petriNet.transitionEnabled(t)) continue;

        fireTransition(t);

        int[] next = petriNet.getMarking();
        String key = Arrays.toString(next);

        if (!visited.containsKey(key)) {
          visited.put(key, Boolean.TRUE);
          reachableMarkings.add(extractActionMarking(next));
          checkBounded();
          queue.add(next);
        }

        petriNet.setMarking(current);
      }
    }

    petriNet.reset();
  }

  /**
   * Aborts the search once the reachability set grows past the allowed ceiling.
   *
   * @throws PetriNetValidationException if the limit has been exceeded
   */
  private void checkBounded() {
    if (reachableMarkings.size() > MAX_REACHABLE_MARKINGS) {
      throw new PetriNetValidationException(
          "La red parece no acotada: se superaron los "
              + MAX_REACHABLE_MARKINGS
              + " marcados alcanzables. Revisá que cada transición consuma tokens y que las"
              + " plazas de recurso limiten la cantidad de hilos.");
    }
  }

  /**
   * Fires transition {@code t}, handling both immediate and temporal transitions.
   *
   * @param t transition index
   */
  private void fireTransition(int t) {
    if (petriNet.isTemporary(t)) {
      petriNet.consumeTokens(t);
      petriNet.produceTokens(t);
    } else {
      petriNet.fire(t);
    }
  }

  /**
   * Projects a full marking onto the action places in sorted order.
   *
   * @param fullMarking complete marking vector
   * @return array of token counts for each action place, in index order
   */
  private int[] extractActionMarking(int[] fullMarking) {
    int[] actionMarking = new int[sortedActionPlaces.size()];
    for (int i = 0; i < sortedActionPlaces.size(); i++) {
      int placeIdx = sortedActionPlaces.get(i);
      if (placeIdx >= 0 && placeIdx < fullMarking.length) {
        actionMarking[i] = fullMarking[placeIdx];
      }
    }
    return actionMarking;
  }

  /**
   * Computes the maximum total tokens across all reachable action-place markings.
   *
   * @return maximum sum of tokens in any single reachable marking
   */
  private int calculateMaxNumThreads() {
    int max = 0;
    for (int[] marking : reachableMarkings) {
      int sum = 0;
      for (int tokens : marking) sum += tokens;
      if (sum > max) max = sum;
    }
    return max;
  }

  /**
   * Returns all reachable action-place markings in BFS discovery order.
   *
   * <p>Each element is a defensive copy of the internal array to prevent external modification.
   *
   * @return list of action-place marking snapshots
   */
  public List<int[]> getReachableMarkings() {
    List<int[]> copy = new ArrayList<>(reachableMarkings.size());
    for (int[] marking : reachableMarkings) {
      copy.add(marking.clone());
    }
    return copy;
  }

  /**
   * Returns the sorted list of action place indices tracked by this tree.
   *
   * @return unmodifiable sorted list of action place indices
   */
  public List<Integer> getSortedActionPlaces() {
    return sortedActionPlaces;
  }

  /**
   * Returns the total number of distinct reachable markings found.
   *
   * @return count of unique reachable states
   */
  public int getNumReachableMarkings() {
    return reachableMarkings.size();
  }

  /**
   * Returns the maximum number of simultaneously active threads. This is the maximum total token
   * count across all action places in any reachable marking (Algorithm 4.1 result).
   *
   * @return maximum concurrent thread count
   */
  public int getMaxNumThreads() {
    return maxNumThreads;
  }

  /**
   * Calculates the maximum number of active threads within a specific segment (Algorithm 4.3).
   *
   * <p>For each reachable marking, sums only the tokens in places belonging to the segment and
   * returns the maximum such sum found.
   *
   * @param segmentPlaces action place indices belonging to the segment (excluding forks/joins)
   * @return maximum concurrent threads in this segment; returns 1 if the segment has no action
   *     places
   */
  public int calculateMaxThreadsInSegment(List<Integer> segmentPlaces) {
    if (segmentPlaces.isEmpty()) {
      return 1;
    }

    int max = 0;
    for (int[] marking : reachableMarkings) {
      int sum = 0;
      for (Integer place : segmentPlaces) {
        int idx = sortedActionPlaces.indexOf(place);
        if (idx >= 0 && idx < marking.length) {
          sum += marking[idx];
        }
      }
      if (sum > max) max = sum;
    }
    return max;
  }
}