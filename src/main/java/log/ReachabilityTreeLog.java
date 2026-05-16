import java.util.List;

/**
 * Specialized log for the complete reachability tree projected onto action places.
 *
 * @author Sassi Juan Ignacio
 */
public class ReachabilityTreeLog extends Log {

  /**
   * Creates a new {@code ReachabilityTreeLog}. The log file will be named {@code
   * reachabilityTree.log}.
   */
  public ReachabilityTreeLog() {
    super("reachabilityTree");
  }

  /**
   * Logs all reachable markings projected onto action places.
   *
   * @param reachableMarkings list of action-place markings in BFS order
   * @param sortedActionPlaces sorted list of action place indices (column headers)
   * @param maxNumThreads maximum number of concurrent threads
   * @param fullPrint if {@code false}, only the first 20 markings are printed
   */
  public void logMarkings(
      List<int[]> reachableMarkings,
      List<Integer> sortedActionPlaces,
      int maxNumThreads,
      boolean fullPrint) {

    StringBuilder sb = new StringBuilder();
    sb.append("\n=== REACHABLE MARKINGS (action places) ===");
    sb.append("\nTotal unique markings: ").append(reachableMarkings.size());
    sb.append("\nMaximum active threads: ").append(maxNumThreads).append("\n");

    // Header row
    sb.append("\nM\t");
    for (Integer p : sortedActionPlaces) {
      sb.append("P").append(p).append("\t");
    }
    sb.append("SUM\n");

    // Separator
    for (int j = 0; j < sortedActionPlaces.size() + 1; j++) {
      sb.append("---\t");
    }
    sb.append("----\n");

    // Markings
    int limit = fullPrint ? reachableMarkings.size() : Math.min(20, reachableMarkings.size());
    for (int i = 0; i < limit; i++) {
      int[] marking = reachableMarkings.get(i);
      sb.append("M").append(i).append("\t");
      int sum = 0;
      for (int tokens : marking) {
        sb.append(tokens).append("\t");
        sum += tokens;
      }
      sb.append(sum).append("\n");
    }

    if (!fullPrint && reachableMarkings.size() > 20) {
      sb.append("\n... (showing first 20 markings only. Set fullprint=true to see all)");
    }

    write(sb.toString());
  }
}
