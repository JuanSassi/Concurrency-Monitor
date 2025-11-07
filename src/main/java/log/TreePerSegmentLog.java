import java.util.*;

/**
 * Specialized log class for segment-specific reachability tree output.
 * Handles formatting and writing of reachable markings filtered by segment.
 * 
 * @author Sassi Juan Ignacio
 */
public class TreePerSegmentLog extends Log {
    
    /**
     * Creates a new TreePerSegmentLog instance.
     * The log file will be named "treePerSegment.log".
     */
    public TreePerSegmentLog() {
        super("treePerSegment");
    }
    
    /**
     * Logs the reachable markings for a specific segment.
     * 
     * @param segment list of transition indices in the segment
     * @param segmentPlaces list of action places in the segment
     * @param reachableMarkings set of all reachable markings (full network)
     * @param actionPlaces set of all action places in the network
     * @param maxThreadsSegment maximum threads for this segment
     * @param fullPrint whether to print all markings or limit to first 20
     */
    public void logSegment(List<Integer> segment,
                          List<Integer> segmentPlaces,
                          Set<int[]> reachableMarkings,
                          Set<Integer> actionPlaces,
                          int maxThreadsSegment,
                          boolean fullPrint) {
        StringBuilder log = new StringBuilder();
        
        log.append("\n=== SEGMENT MARKS (Transitions: ").append(segment).append(") ===");
        
        if (segmentPlaces.isEmpty()) {
            log.append("\nIt hasn't action place neither forks and joins in this segment");
            log.append("\nMaximum number of active threads in segment: 1\n");
            write(log.toString());
            return;
        }
        
        log.append("\nAction places in segment (without forks and joins): ").append(segmentPlaces);
        log.append("\nMaximum number of active threads in segment: ").append(maxThreadsSegment).append("\n");
        
        // Sort segment places
        Collections.sort(segmentPlaces);
        
        // Sort all action places for indexing
        List<Integer> allSortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(allSortedPlaces);
        
        // Print header
        log.append("\nM\t");
        for (Integer place : segmentPlaces) {
            log.append("P").append(place).append("\t");
        }
        log.append("SUM\n");
        
        // Print separator line
        log.append("---\t");
        for (int j = 0; j < segmentPlaces.size(); j++) {
            log.append("---\t");
        }
        log.append("----\n");
        
        // Print filtered markings
        int i = 0;
        for (int[] marking : reachableMarkings) {
            if (i >= 20 && !fullPrint) break;
            
            log.append("M").append(i).append("\t");
            int segmentSum = 0;
            
            // Print only tokens from segment places
            for (Integer segmentPlace : segmentPlaces) {
                // Find the index of this place in the marking
                int markingIndex = allSortedPlaces.indexOf(segmentPlace);
                if (markingIndex >= 0 && markingIndex < marking.length) {
                    int tokens = marking[markingIndex];
                    log.append(tokens).append("\t");
                    segmentSum += tokens;
                } else {
                    log.append("0\t");
                }
            }
            
            log.append(segmentSum).append("\n");
            i++;
        }
        
        if (reachableMarkings.size() > 20 && !fullPrint) {
            log.append("\n... (showing first 20 markings only. Set fullprint=true in config to see all)");
        }
        
        write(log.toString());
    }
    
    /**
     * Logs all segments in sequence.
     * 
     * @param segments list of all execution segments
     * @param allSegmentPlaces list of action places for each segment
     * @param reachableMarkings set of all reachable markings
     * @param actionPlaces set of all action places
     * @param maxThreadsPerSegment maximum threads for each segment
     * @param fullPrint whether to print all markings or limit to first 20
     */
    public void logAllSegments(List<List<Integer>> segments,
                               List<List<Integer>> allSegmentPlaces,
                               Set<int[]> reachableMarkings,
                               Set<Integer> actionPlaces,
                               List<Integer> maxThreadsPerSegment,
                               boolean fullPrint) {
        for (int i = 0; i < segments.size(); i++) {
            logSegment(segments.get(i),
                      allSegmentPlaces.get(i),
                      reachableMarkings,
                      actionPlaces,
                      maxThreadsPerSegment.get(i),
                      fullPrint);
        }
    }
}