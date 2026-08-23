import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * Renders the results of the three thread allocation algorithms to the console.
 *
 * <p>This class holds every {@code System.out} call that used to be scattered across {@code
 * ThreadAllocator}, {@code PlaceClassifier} and {@code Invariants}. Those classes are now pure
 * analysis components: they compute and expose results, and whoever wants to display them decides
 * how and where. That separation is what lets the web layer reuse the same analysis without
 * polluting the server's standard output.
 *
 * <p>The strings themselves come from {@link AnalysisFormatter}, shared with {@code
 * AnalysisController}, so console and API cannot drift into showing the same data two different
 * ways.
 *
 * @see ThreadAllocator
 * @see AnalysisFormatter
 * @author Sassi Juan Ignacio
 */
public class AlgorithmsConsolePrinter {

  /** Analysis whose results are rendered. */
  private final ThreadAllocator allocator;

  /** Place classifier of the analysis. */
  private final PlaceClassifier classifier;

  /** Invariants calculator of the analysis. */
  private final Invariants invariants;

  /** Responsibility and segment analyzer of the analysis. */
  private final Responsibilities responsibilities;

  /**
   * Creates a printer bound to an already completed analysis.
   *
   * @param allocator the {@link ThreadAllocator} whose results will be printed
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "The printer is intentionally bound to the caller's analysis instance — it must render"
              + " that exact analysis, not a copy.")
  public AlgorithmsConsolePrinter(ThreadAllocator allocator) {
    this.allocator = allocator;
    this.classifier = allocator.getClassifier();
    this.invariants = allocator.getInvariants();
    this.responsibilities = allocator.getResponsibilities();
  }

  /** Prints the results of the three algorithms, in order. */
  public void printAll() {
    printAlgorithm1();
    printAlgorithm2();
    printAlgorithm3();
  }

  /**
   * Prints the results of Algorithm 4.1 — maximum number of simultaneously active threads.
   *
   * <p>Sequence of steps:
   *
   * <ol>
   *   <li>Resource, restriction and idle places
   *   <li>T-invariants of the net
   *   <li>Places associated with each T-invariant (PI of IT)
   *   <li>Action places associated with each T-invariant (PA of IT)
   *   <li>Action places of the net
   *   <li>Maximum active threads and number of reachable markings
   * </ol>
   */
  public void printAlgorithm1() {
    System.out.println("\n" + "=== Algorithm 4.1 ===");
    printResourcePlaces();
    printTInvariants();
    printPiOfIt();
    printPaOfIt();
    printActionPlaces();
    System.out.print("The reachability tree is located in a .log file in the ./log/ directory\n");
    printMaxActiveThreads();
  }

  /**
   * Prints the results of Algorithm 4.2 — execution segments, fork places and join places, plus the
   * places belonging to each segment.
   */
  public void printAlgorithm2() {
    System.out.println("\n=== Algorithm 4.2 ===");
    printSegments();
    System.out.println(
        "Forks:    " + AnalysisFormatter.formatIndices(responsibilities.getForkPlaces(), "P"));
    System.out.println(
        "Joins:    " + AnalysisFormatter.formatIndices(responsibilities.getJoinPlaces(), "P"));
    printSegmentPlaces();
  }

  /**
   * Prints the results of Algorithm 4.3 — maximum number of threads per segment and the resulting
   * total for the whole system.
   */
  public void printAlgorithm3() {
    System.out.println("\n=== Algorithm 4.3 ===");

    List<Integer> threadsPerSegment = allocator.getThreadsPerSegment();
    printLines(AnalysisFormatter.labelThreadsPerSegment(threadsPerSegment));

    int total = 0;
    for (Integer threads : threadsPerSegment) {
      total += threads;
    }
    System.out.println("\nMaximum number of threads in the system = " + total);
  }

  /** Prints all minimal P-invariants, showing each as a labeled set of places. */
  public void printPInvariants() {
    System.out.println("\nP-invariants");
    printLines(AnalysisFormatter.labelInvariants("IP", " = ", invariants.getPInvariants(), "P"));
  }

  /** Prints all minimal T-invariants, showing each as a labeled set of transitions. */
  public void printTInvariants() {
    System.out.println("\nT-invariants");
    printLines(AnalysisFormatter.labelInvariants("IT", " = ", invariants.getTInvariants(), "T"));
  }

  /** Prints the resource, restriction and idle places of the net. */
  public void printResourcePlaces() {
    System.out.println(
        "\n"
            + "Resources, restrictions and idle places: "
            + AnalysisFormatter.sortedLabels(classifier.getResourcePlaces(), "P"));
  }

  /** Prints the action places of the net. */
  public void printActionPlaces() {
    System.out.println(
        "\nAction places: "
            + AnalysisFormatter.sortedLabels(classifier.getActionPlaces(), "P")
            + "\n");
  }

  /**
   * Prints, for each T-invariant, the full set of places (input and output, mixed) connected to it
   * — e.g. {@code PI1: {P2, P3, P5, P6, P8, P14, P15}}.
   */
  public void printPiOfIt() {
    System.out.println("\nPI of IT (places connected to each T-invariant)");
    printLines(AnalysisFormatter.labelIndexGroups("PI", ": ", classifier.getPiOfIt(), "P"));
  }

  /**
   * Prints, for each T-invariant, its action places only — i.e. the places from {@link
   * #printPiOfIt()} with resource, idle and restriction places excluded. E.g. {@code PA1: {P3, P8,
   * P14}}.
   */
  public void printPaOfIt() {
    System.out.println("\nPA of IT (action places per T-invariant)");
    printLines(AnalysisFormatter.labelIndexGroups("PA", ": ", classifier.getPaOfIt(), "P"));
  }

  /** Prints the maximum number of active threads and the size of the reachability set. */
  public void printMaxActiveThreads() {
    System.out.println("\nMax active threads: " + allocator.getMaxActiveThreads());
    System.out.println("Reachable markings: " + allocator.getTree().getNumReachableMarkings());
  }

  /** Prints the execution segments as labeled sets of transitions. */
  public void printSegments() {
    printLines(AnalysisFormatter.labelIndexGroups("S", " = ", allocator.getSegments(), "T"));
  }

  /** Prints the places belonging to each execution segment, forks and joins included. */
  public void printSegmentPlaces() {
    printLines(
        AnalysisFormatter.labelIndexGroups("PS", " = ", allocator.computeAllSegmentPlaces(), "P"));
  }

  /**
   * Writes each line to standard output.
   *
   * @param lines the already formatted lines to print
   */
  private void printLines(List<String> lines) {
    for (String line : lines) {
      System.out.println(line);
    }
  }
}