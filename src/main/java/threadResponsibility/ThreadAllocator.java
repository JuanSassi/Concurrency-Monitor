import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates the three thread allocation algorithms for Petri net systems.
 *
 * <p>This class coordinates the full analysis pipeline:
 *
 * <ol>
 *   <li>Receives the Petri net structure via a {@link PetriNetDefinition}
 *   <li>Computes P-invariants and T-invariants ({@link Invariants})
 *   <li>Classifies places into action and resource places ({@link PlaceClassifier})
 *   <li>Builds the reachability tree ({@link ReachabilityTree})
 *   <li>Identifies fork/join places and execution segments ({@link Responsibilities})
 * </ol>
 *
 * <p>This class is a pure analysis component — it does not produce any log files or side effects.
 * Logging is the responsibility of the caller.
 *
 * <p>The {@code PetriNet} instance created here is exposed via {@link #getPetriNet()} so that
 * whoever runs the simulation afterwards (e.g. a {@code Monitor}) can reuse the exact same instance
 * — one analysis, one net, no shared global state.
 *
 * @see Invariants
 * @see PlaceClassifier
 * @see ReachabilityTree
 * @see Responsibilities
 * @author Sassi Juan Ignacio
 */
public class ThreadAllocator {

  /** Pre-incidence matrix of the Petri net. */
  private final int[][] pre;

  /** Post-incidence matrix of the Petri net. */
  private final int[][] post;

  /** Incidence matrix (W = Post - Pre). */
  private final int[][] w;

  /** Invariants calculator. */
  private final Invariants invariants;

  /** Place classifier. */
  private final PlaceClassifier classifier;

  /** Petri net instance built for this analysis run. */
  private final PetriNet petriNet;

  /** Reachability tree over action places. */
  private final ReachabilityTree tree;

  /** Responsibility and segment analyzer. */
  private final Responsibilities responsibilities;

  /** Execution segments identified by Algorithm 4.2. */
  private final List<List<Integer>> segments;

  /**
   * Constructs a {@code ThreadAllocator} using the Petri net configured in {@code
   * config.properties} — same behaviour as before. Kept for backward compatibility with the console
   * entry point ({@code Main}).
   */
  public ThreadAllocator() {
    this(PetriNetDefinition.fromProperties());
  }

  /**
   * Constructs a {@code ThreadAllocator} and runs the full analysis pipeline for the given net
   * definition. This is the constructor a web layer should use: one call, one definition (from the
   * catalog or from a user-submitted request), one isolated analysis — no shared static state
   * between runs.
   *
   * @param definition the validated Petri net definition to analyze
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "definition.pre()/post() already return defensive copies (PetriNetDefinition's own"
              + " accessors clone internally). No further copy needed here.")
  public ThreadAllocator(PetriNetDefinition definition) {
    this.pre = definition.pre();
    this.post = definition.post();
    int[] m0 = definition.initialMarking();
    this.w = Matrix.subtract(post, pre);

    this.invariants = new Invariants(w);
    this.classifier = new PlaceClassifier(pre, post, m0, invariants);

    this.petriNet = new PetriNet(pre, post, m0, definition.temporalTransitions());
    this.tree = new ReachabilityTree(classifier.getActionPlaces(), petriNet);

    this.responsibilities =
        new Responsibilities(pre, post, invariants.getTInvariants(), classifier.getActionPlaces());
    this.segments = responsibilities.getSegments();
  }

  /**
   * Computes the set of all places (action and resource) associated with each T-invariant.
   *
   * <p>A place is included if it is connected to any active transition of the invariant via the pre
   * or post incidence matrix. This differs from {@link PlaceClassifier#getPaOfIt()}, which returns
   * only action places.
   *
   * @return list of sorted place index lists, one per T-invariant
   */
  public List<List<Integer>> computePiOfIt() {
    List<List<Integer>> result = new ArrayList<>();
    int numPlaces = pre.length;
    int numTransitions = pre[0].length;

    for (List<Integer> tInv : invariants.getTInvariants()) {
      Set<Integer> involved = new HashSet<>();

      for (int t = 0; t < numTransitions; t++) {
        if (tInv.get(t) > 0) {
          for (int p = 0; p < numPlaces; p++) {
            if (pre[p][t] > 0 || post[p][t] > 0) {
              involved.add(p);
            }
          }
        }
      }

      List<Integer> sorted = new ArrayList<>(involved);
      Collections.sort(sorted);
      result.add(sorted);
    }

    return result;
  }

  /**
   * Computes the action places (excluding forks and joins) for each segment.
   *
   * @return list of sorted place index lists, one per segment
   */
  public List<List<Integer>> computeAllSegmentPlaces() {
    List<List<Integer>> result = new ArrayList<>();
    for (List<Integer> segment : segments) {
      result.add(computeSegmentPlaces(segment));
    }
    return result;
  }

  /**
   * Computes the action places for a single segment, excluding fork and join places.
   *
   * @param segment list of transition indices forming the segment
   * @return sorted list of action place indices in the segment
   */
  private List<Integer> computeSegmentPlaces(List<Integer> segment) {
    Set<Integer> places = new HashSet<>();
    Set<Integer> actionPlaces = classifier.getActionPlaces();
    List<Integer> forks = responsibilities.getForkPlaces();
    List<Integer> joins = responsibilities.getJoinPlaces();

    for (Integer t : segment) {
      for (int p = 0; p < post.length; p++) {
        if (post[p][t] > 0
            && actionPlaces.contains(p)
            && !forks.contains(p)
            && !joins.contains(p)) {
          places.add(p);
        }
      }
    }

    List<Integer> sorted = new ArrayList<>(places);
    Collections.sort(sorted);
    return sorted;
  }

  /**
   * Computes the maximum number of threads for each segment (Algorithm 4.3).
   *
   * @return list of maximum thread counts, one per segment
   */
  public List<Integer> getThreadsPerSegment() {
    List<Integer> result = new ArrayList<>();
    for (List<Integer> segmentPlaces : computeAllSegmentPlaces()) {
      result.add(tree.calculateMaxThreadsInSegment(segmentPlaces));
    }
    return result;
  }

  /**
   * Returns the maximum number of simultaneously active threads (Algorithm 4.1 result).
   *
   * @return maximum concurrent thread count
   */
  public int getMaxActiveThreads() {
    return tree.getMaxNumThreads();
  }

  /**
   * Returns the execution segments identified by Algorithm 4.2.
   *
   * @return unmodifiable list of segments, each a list of transition indices
   */
  public List<List<Integer>> getSegments() {
    return Collections.unmodifiableList(segments);
  }

  /**
   * Returns the place classifier used in the analysis.
   *
   * @return the {@link PlaceClassifier} instance
   */
  public PlaceClassifier getClassifier() {
    return classifier;
  }

  /**
   * Returns the {@code PetriNet} instance built for this analysis, already restored to the initial
   * marking. Pass this same instance to {@code Monitor} to run the simulation without creating a
   * second, disconnected net.
   *
   * @return the analyzed Petri net, ready to be handed to a {@code Monitor}
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification =
          "Intentional: the caller (typically code wiring up a Monitor) must reuse the exact"
              + " same PetriNet instance that was analyzed here, not a copy — that's the whole"
              + " point of exposing it. Returning a defensive copy would let the analysis and"
              + " the simulation drift onto two different nets.")
  public PetriNet getPetriNet() {
    return petriNet;
  }

  /**
   * Returns the reachability tree built during analysis.
   *
   * @return the {@link ReachabilityTree} instance
   */
  public ReachabilityTree getTree() {
    return tree;
  }

  /**
   * Returns the responsibilities analyzer used in the analysis.
   *
   * @return the {@link Responsibilities} instance
   */
  public Responsibilities getResponsibilities() {
    return responsibilities;
  }

  /**
   * Returns the invariants calculator used in the analysis.
   *
   * @return the {@link Invariants} instance
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification =
          "Invariants is effectively immutable: all fields are final and getters return"
              + " unmodifiable views.")
  public Invariants getInvariants() {
    return invariants;
  }

  /** 
  * === Algorithm 1 ===
  * 
  * 1 Obtener los IT de la RdP
  * 
  * 2 Para cada IT obtener el conjunto de plazas asociadas al IT en análisis
  * 
  * 3 Determinar las plazas relacionadas a acciones de cada IT
  * 
  * 4 Del árbol de alcanzabilidad de la RdP, se debe obtener MA
  * MA es el conjunto de todos los marcados posibles de todos los conjuntos de plazas.
  * 
  *  5 De cada marcado posible (estado) del conjunto MA se debe realizar la suma de las marcas. 
  * De todas estas sumas, se debe buscar la de mayor valor (marcado máximo). 
  * Esta será la cantidad máxima de hilos activos simultáneos en el sistema.
  * 
  */
  public void Algorithm1(){
    System.out.println("\n"+"=== Algorithm 4.1 ===");
    classifier.printResourcePlaces();
    Algorithm11();
    Algorithm12();
    Algorithm13();
    Algorithm14();
    Algorithm15();
  }

  public void Algorithm11(){
    invariants.printTInvariants();
  }

  public void Algorithm12(){
    classifier.printPiOfIt();
  }

  public void Algorithm13(){
    classifier.printPaOfIt();
  }

  public void Algorithm14(){
    classifier.printActionPlaces();
    System.out.print("The reachability tree is located in a .log file in the ./log/ directory\n");
  }

  public void Algorithm15(){
    System.out.println("\nMax active threads: " + getMaxActiveThreads());
    System.out.println("Reachable markings: " + getTree().getNumReachableMarkings());
  }
}
