import java.util.List;

/**
 * Specialized log for segment-specific reachability tree output.
 *
 * @author Sassi Juan Ignacio
 */
public class TreePerSegmentLog extends Log {

  /**
   * Creates a new {@code TreePerSegmentLog}. The log file will be named {@code treePerSegment.log}.
   */
  public TreePerSegmentLog() {
    super("treePerSegment");
  }

  /**
   * Logs the reachable markings filtered to the places of a single segment.
   *
   * @param segment transition indices that form the segment
   * @param segmentPlaces action place indices in this segment (excluding forks/joins)
   * @param reachableMarkings all reachable markings projected onto action places
   * @param sortedActionPlaces sorted list of all action place indices (for index lookup)
   * @param maxThreadsSegment maximum threads for this segment
   * @param fullPrint if {@code false}, only the first 20 markings are printed
   */
  public void logSegment(
      List<Integer> segment,
      List<Integer> segmentPlaces,
      List<int[]> reachableMarkings,
      List<Integer> sortedActionPlaces,
      int maxThreadsSegment,
      boolean fullPrint) {

    StringBuilder sb = new StringBuilder();
    sb.append("\n=== SEGMENT MARKINGS (transitions: ").append(segment).append(") ===");

    if (segmentPlaces.isEmpty()) {
      appendEmptySegment(sb);
    } else {
      appendSegmentContent(
          sb, segmentPlaces, reachableMarkings, sortedActionPlaces, maxThreadsSegment, fullPrint);
    }

    write(sb.toString());
  }

  /**
   * Appends the message for a segment with no action places.
   *
   * @param sb the string builder to append to
   */
  private void appendEmptySegment(StringBuilder sb) {
    sb.append("\nNo action places (excluding forks/joins) in this segment.");
    sb.append("\nMaximum threads in segment: 1\n");
  }

  /**
   * Appends the full marking table for a segment that has action places.
   *
   * @param sb the string builder to append to
   * @param segmentPlaces action place indices for this segment
   * @param reachableMarkings all reachable markings
   * @param sortedActionPlaces sorted list of all action place indices
   * @param maxThreadsSegment maximum threads for this segment
   * @param fullPrint if {@code false}, only the first 20 markings are printed
   */
  private void appendSegmentContent(
      StringBuilder sb,
      List<Integer> segmentPlaces,
      List<int[]> reachableMarkings,
      List<Integer> sortedActionPlaces,
      int maxThreadsSegment,
      boolean fullPrint) {

    sb.append("\nAction places in segment: ").append(segmentPlaces);
    sb.append("\nMaximum threads in segment: ").append(maxThreadsSegment).append("\n");

    appendHeader(sb, segmentPlaces);
    appendMarkings(sb, segmentPlaces, reachableMarkings, sortedActionPlaces, fullPrint);

    if (!fullPrint && reachableMarkings.size() > 20) {
      sb.append("\n... (showing first 20 markings only. Set fullprint=true to see all)");
    }
  }

  /**
   * Appends the column header row and separator.
   *
   * @param sb the string builder to append to
   * @param segmentPlaces action place indices for this segment
   */
  private void appendHeader(StringBuilder sb, List<Integer> segmentPlaces) {
    sb.append("\nM\t");
    for (Integer p : segmentPlaces) {
      sb.append("P").append(p).append("\t");
    }
    sb.append("SUM\n");
    sb.append("---\t");
    for (int j = 0; j < segmentPlaces.size(); j++) {
      sb.append("---\t");
    }
    sb.append("----\n");
  }

  /**
   * Appends each marking row filtered to the segment places.
   *
   * @param sb the string builder to append to
   * @param segmentPlaces action place indices for this segment
   * @param reachableMarkings all reachable markings
   * @param sortedActionPlaces sorted list of all action place indices
   * @param fullPrint if {@code false}, only the first 20 markings are printed
   */
  private void appendMarkings(
      StringBuilder sb,
      List<Integer> segmentPlaces,
      List<int[]> reachableMarkings,
      List<Integer> sortedActionPlaces,
      boolean fullPrint) {

    int limit = fullPrint ? reachableMarkings.size() : Math.min(20, reachableMarkings.size());
    for (int i = 0; i < limit; i++) {
      appendMarkingRow(sb, i, reachableMarkings.get(i), segmentPlaces, sortedActionPlaces);
    }
  }

  /**
   * Appends a single marking row.
   *
   * @param sb the string builder to append to
   * @param index the marking index (for the M column)
   * @param marking the full action-place marking array
   * @param segmentPlaces action place indices for this segment
   * @param sortedActionPlaces sorted list of all action place indices
   */
  private void appendMarkingRow(
      StringBuilder sb,
      int index,
      int[] marking,
      List<Integer> segmentPlaces,
      List<Integer> sortedActionPlaces) {

    sb.append("M").append(index).append("\t");
    int sum = 0;
    for (Integer p : segmentPlaces) {
      int idx = sortedActionPlaces.indexOf(p);
      int tokens = (idx >= 0 && idx < marking.length) ? marking[idx] : 0;
      sb.append(tokens).append("\t");
      sum += tokens;
    }
    sb.append(sum).append("\n");
  }

  /**
   * Logs all segments in sequence.
   *
   * @param segments all execution segments
   * @param allSegmentPlaces action places for each segment
   * @param reachableMarkings all reachable markings
   * @param sortedActionPlaces sorted list of all action place indices
   * @param maxThreadsPerSegment maximum threads per segment
   * @param fullPrint if {@code false}, only the first 20 markings are printed per segment
   */
  public void logAllSegments(
      List<List<Integer>> segments,
      List<List<Integer>> allSegmentPlaces,
      List<int[]> reachableMarkings,
      List<Integer> sortedActionPlaces,
      List<Integer> maxThreadsPerSegment,
      boolean fullPrint) {

    for (int i = 0; i < segments.size(); i++) {
      logSegment(
          segments.get(i),
          allSegmentPlaces.get(i),
          reachableMarkings,
          sortedActionPlaces,
          maxThreadsPerSegment.get(i),
          fullPrint);
    }
  }
}
