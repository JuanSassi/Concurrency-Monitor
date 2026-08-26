import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Specialized log for the results of the three thread allocation algorithms.
 *
 * @author Sassi Juan Ignacio
 */
public class AlgorithmsLog extends Log {

  /** Creates a new {@code AlgorithmsLog}. The log file will be named {@code algorithms.log}. */
  public AlgorithmsLog() {
    super("algorithms");
  }

  /**
   * Logs the results of Algorithm 4.1 (maximum simultaneous active threads).
   *
   * @param tInvariants list of T-invariant vectors
   * @param piOfIt all places associated with each T-invariant
   * @param paOfIt action places associated with each T-invariant
   * @param actionPlaces union of all action places
   * @param maxThreads maximum number of concurrent threads
   * @param numReachableMarkings total number of reachable markings
   */
  public void logAlgorithm1(
      List<List<Integer>> tInvariants,
      List<List<Integer>> piOfIt,
      List<List<Integer>> paOfIt,
      Set<Integer> actionPlaces,
      int maxThreads,
      int numReachableMarkings) {

    StringBuilder sb = new StringBuilder();
    sb.append("\n╔═════════════════════════════════════════════════════════════════════╗\n");
    sb.append("║  ALGORITHM FOR DETERMINING MAXIMUM SIMULTANEOUS ACTIVE THREADS (4.1)  ║\n");
    sb.append("╚═══════════════════════════════════════════════════════════════════════");

    sb.append("\n\n=================================");
    sb.append("\nT-Invariant Transitions\n");
    for (int i = 0; i < tInvariants.size(); i++) {
      sb.append("\ny").append(i + 1).append(": ").append(formatTransitions(tInvariants.get(i)));
    }

    sb.append("\n\n=================================");
    sb.append("\nSet of places associated with each T-invariant (PI of IT)\n");
    for (int i = 0; i < piOfIt.size(); i++) {
      sb.append("\nIT").append(i + 1).append(": ").append(formatPlaces(piOfIt.get(i)));
    }

    sb.append("\n\n=================================");
    sb.append("\nAction places associated with each T-invariant (PA of IT)\n");
    for (int i = 0; i < paOfIt.size(); i++) {
      sb.append("\nIT").append(i + 1).append(": ").append(formatPlaces(paOfIt.get(i)));
    }

    sb.append("\n\n=================================");
    sb.append("\nUnion of all action places (PA)\n");
    List<Integer> sorted = new ArrayList<>(actionPlaces);
    Collections.sort(sorted);
    sb.append("\nAction places: ").append(formatPlaces(sorted)).append("\n");

    sb.append("\n=== RESULTS ===");
    sb.append("\nTotal unique markings: ").append(numReachableMarkings);
    sb.append("\nMaximum active threads: ").append(maxThreads).append("\n");
    sb.append("\nFull reachability tree at reachabilityTree.log");

    write(sb.toString());
  }

  /**
   * Logs the results of Algorithm 4.2 (thread responsibility).
   *
   * @param segments execution segments identified
   * @param forkPlaces fork place indices
   * @param joinPlaces join place indices
   */
  public void logAlgorithm2(
      List<List<Integer>> segments, List<Integer> forkPlaces, List<Integer> joinPlaces) {

    StringBuilder sb = new StringBuilder();
    sb.append("\n╔══════════════════════════════════════════════════════════╗");
    sb.append("\n║  ALGORITHM FOR DETERMINING THREAD RESPONSIBILITY (4.2)   ║");
    sb.append("\n╚═══════════════════════════════════════════════════════════\n");

    sb.append("\n=================================");
    sb.append("\nTHREAD RESPONSIBILITY SEGMENTS");
    sb.append("\n=================================");
    for (int i = 0; i < segments.size(); i++) {
      sb.append("\nSegment ").append(i + 1).append(": ").append(formatTransitions(segments.get(i)));
    }

    sb.append("\n\n=================================\nFORKS");
    if (forkPlaces.isEmpty()) {
      sb.append("\nNo forks found.");
    } else {
      sb.append("\nFork places: ").append(formatPlaces(forkPlaces));
    }

    sb.append("\n\n=================================\nJOINS");
    if (joinPlaces.isEmpty()) {
      sb.append("\nNo joins found.");
    } else {
      sb.append("\nJoin places: ").append(formatPlaces(joinPlaces));
    }

    write(sb.toString());
  }

  /**
   * Logs the results of Algorithm 4.3 (maximum threads per segment).
   *
   * @param segments execution segments
   * @param segmentPlaces action places for each segment
   * @param maxThreadsPerSegment maximum thread count per segment
   */
  public void logAlgorithm3(
      List<List<Integer>> segments,
      List<List<Integer>> segmentPlaces,
      List<Integer> maxThreadsPerSegment) {

    StringBuilder sb = new StringBuilder();
    sb.append("\n╔══════════════════════════════════════════════════════════════════╗");
    sb.append("\n║  ALGORITHM FOR DETERMINING MAXIMUM THREADS PER SEGMENT (4.3)     ║");
    sb.append("\n╚═══════════════════════════════════════════════════════════════════");

    for (int i = 0; i < segments.size(); i++) {
      sb.append("\n\n=== Segment ")
          .append(i + 1)
          .append(" (transitions: ")
          .append(formatTransitions(segments.get(i)))
          .append(") ===");
      if (segmentPlaces.get(i).isEmpty()) {
        sb.append("\nNo action places (excluding forks/joins) in this segment.");
      } else {
        sb.append("\nAction places: ").append(formatPlaces(segmentPlaces.get(i)));
      }
      sb.append("\nMaximum threads: ").append(maxThreadsPerSegment.get(i)).append("\n");
    }

    write(sb.toString());
  }

  /**
   * Formats a list of transition indices as {@code [T0, T1, ...]}.
   *
   * @param transitions list of transition indices
   * @return formatted string
   */
  private static List<String> formatTransitions(List<Integer> transitions) {
    List<String> result = new ArrayList<>();
    for (Integer t : transitions) {
      result.add("T" + t);
    }
    return result;
  }

  /**
   * Formats a list of place indices as {@code [P0, P1, ...]}.
   *
   * @param places list of place indices
   * @return formatted string
   */
  private static List<String> formatPlaces(List<Integer> places) {
    List<String> result = new ArrayList<>();
    for (Integer p : places) {
      result.add("P" + p);
    }
    return result;
  }
}
