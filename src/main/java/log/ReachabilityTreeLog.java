import java.util.*;

/**
 * Specialized log class for complete reachability tree output.
 * Handles formatting and writing of all reachable markings for action places.
 * 
 * @author Sassi Juan Ignacio
 */
public class ReachabilityTreeLog extends Log {
    
    /**
     * Creates a new ReachabilityTreeLog instance.
     * The log file will be named "reachabilityTree.log".
     */
    public ReachabilityTreeLog() {
        super("reachabilityTree");
    }
    
    /**
     * Logs the complete reachability tree with all markings.
     * 
     * @param reachableMarkings set of all reachable markings
     * @param actionPlaces set of action place indices being tracked
     * @param maxNumThreads maximum number of concurrent threads
     * @param fullPrint whether to print all markings or limit to first 20
     */
    public void logMarkings(Set<int[]> reachableMarkings,
                           Set<Integer> actionPlaces,
                           int maxNumThreads,
                           boolean fullPrint) {
        StringBuilder log = new StringBuilder();
        
        // Header
        log.append("\n=== ACHIEVABLE MARKS (Action places) ===");
        log.append("\nTotal unique marks: ").append(reachableMarkings.size());
        log.append("\nMaximum number of active threads: ").append(maxNumThreads).append("\n");
        
        // Create sorted list of action places for header
        List<Integer> sortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(sortedPlaces);
        
        // Print header
        log.append("\nM\t");
        for (Integer place : sortedPlaces) {
            log.append("P").append(place).append("\t");
        }
        log.append("SUM\n");
        
        // Print separator line
        for (int j = 0; j < sortedPlaces.size() + 1; j++) {
            log.append("---\t");
        }
        log.append("----\n");
        
        // Print markings
        int i = 0;
        for (int[] marking : reachableMarkings) {
            if (i >= 20 && !fullPrint) break;
            
            log.append("M").append(i).append("\t");
            
            int sum = 0;
            for (int token : marking) {
                log.append(token).append("\t");
                sum += token;
            }
            log.append(sum).append("\n");
            i++;
        }
        
        if (reachableMarkings.size() > 20 && !fullPrint) {
            log.append("\n... (showing first 20 markings only. Set fullprint=true in config to see all)");
        }
        
        write(log.toString());
    }
}