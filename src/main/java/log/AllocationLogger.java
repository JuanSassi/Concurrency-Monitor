/**
 * Orchestrates writing of all thread allocation algorithm results to log files.
 *
 * <p>This class is the only component responsible for log file creation and writing. It receives a
 * fully constructed {@link ThreadAllocator} and delegates formatting to the specialized log
 * classes.
 *
 * <p>Three log files are produced:
 *
 * <ul>
 *   <li>{@code algorithms.log} — results of Algorithms 4.1, 4.2 and 4.3
 *   <li>{@code reachabilityTree.log} — complete reachability tree
 *   <li>{@code treePerSegment.log} — segment-specific reachability information
 * </ul>
 *
 * @see AlgorithmsLog
 * @see ReachabilityTreeLog
 * @see TreePerSegmentLog
 * @author Sassi Juan Ignacio
 */
public class AllocationLogger {

  /** The allocator whose results will be logged. */
  private final ThreadAllocator allocator;

  /**
   * Constructs an {@code AllocationLogger} for the given allocator.
   *
   * @param allocator a fully constructed {@link ThreadAllocator}
   */
  public AllocationLogger(ThreadAllocator allocator) {
    this.allocator = allocator;
  }

  /**
   * Writes all algorithm results to their respective log files.
   *
   * <p>Log files are created in the {@code log/} directory. If files with the same names already
   * exist, numbered suffixes are added automatically.
   */
  public void logAll() {
    boolean fullPrint = ConfigLoader.getFullprint();

    java.util.List<java.util.List<Integer>> segmentPlaces = allocator.computeAllSegmentPlaces();
    java.util.List<Integer> threadsPerSegment = allocator.getThreadsPerSegment();

    AlgorithmsLog algorithmsLog = new AlgorithmsLog();
    algorithmsLog.logAlgorithm1(
        allocator.getInvariants().getTInvariants(),
        allocator.computePiOfIt(),
        allocator.getClassifier().getPaOfIt(),
        allocator.getClassifier().getActionPlaces(),
        allocator.getMaxActiveThreads(),
        allocator.getTree().getNumReachableMarkings());
    algorithmsLog.logAlgorithm2(
        allocator.getSegments(),
        allocator.getResponsibilities().getForkPlaces(),
        allocator.getResponsibilities().getJoinPlaces());
    algorithmsLog.logAlgorithm3(allocator.getSegments(), segmentPlaces, threadsPerSegment);

    ReachabilityTreeLog treeLog = new ReachabilityTreeLog();
    treeLog.logMarkings(
        allocator.getTree().getReachableMarkings(),
        allocator.getTree().getSortedActionPlaces(),
        allocator.getMaxActiveThreads(),
        fullPrint);

    TreePerSegmentLog segmentLog = new TreePerSegmentLog();
    segmentLog.logAllSegments(
        allocator.getSegments(),
        segmentPlaces,
        allocator.getTree().getReachableMarkings(),
        allocator.getTree().getSortedActionPlaces(),
        threadsPerSegment,
        fullPrint);
  }
}
