import java.util.*;

/**
 * Specialized log class for thread allocation algorithms output.
 * Handles formatting and writing of results from Algorithms 4.1, 4.2, and 4.3.
 * 
 * @author Sassi Juan Ignacio
 */
public class AlgorithmsLog extends Log {
    
    /**
     * Creates a new AlgorithmsLog instance.
     * The log file will be named "algorithms.log".
     */
    public AlgorithmsLog() {
        super("algorithms");
    }
    
    /**
     * Logs the results of Algorithm 4.1 (Maximum simultaneous active threads).
     * 
     * @param tInvariants list of T-invariants with their transitions
     * @param piOfIT places associated with each T-invariant
     * @param paOfIT action places associated with each T-invariant
     * @param actionPlaces union of all action places
     * @param maxThreads maximum number of concurrent threads
     * @param numReachableMarkings total number of reachable markings found
     */
    public void logAlgorithm1(List<List<Integer>> tInvariants,
                              List<List<Integer>> piOfIT,
                              List<List<Integer>> paOfIT,
                              Set<Integer> actionPlaces,
                              int maxThreads,
                              int numReachableMarkings) {
        StringBuilder log = new StringBuilder();
        
        // Header
        log.append("\n╔═══════════════════════════════════════════════════════════════════════╗\n");
        log.append("║  ALGORITHM FOR DETERMINING MAXIMUM SIMULTANEOUS ACTIVE THREADS (4.1)  ║\n");
        log.append("╚═══════════════════════════════════════════════════════════════════════");
        
        // T-Invariant Transitions
        log.append("\n\n=================================");
        log.append("\nT-Invariant Transitions\n");
        for (int i = 0; i < tInvariants.size(); i++) {
            List<String> transitionNames = formatTransitionList(tInvariants.get(i));
            log.append("\ny").append(i + 1).append(": ").append(transitionNames);
        }
        
        // Places associated with each T-invariant
        log.append("\n\n=================================");
        log.append("\nSet of places associated with each Transition Invariant (PI of TI)\n");
        for (int i = 0; i < piOfIT.size(); i++) {
            List<String> placeNames = formatPlaceList(piOfIT.get(i));
            log.append("\nTI ").append(i + 1).append(": ").append(placeNames);
        }
        
        // Action places per T-invariant
        log.append("\n\n=================================");
        log.append("\nSet of action places associated with each Transition Invariant (PA of TI)\n");
        for (int i = 0; i < paOfIT.size(); i++) {
            List<String> actionPlaceNames = formatPlaceList(paOfIT.get(i));
            log.append("\nTI ").append(i + 1).append(": ").append(actionPlaceNames);
        }
        
        // Union of all action places
        log.append("\n\n=================================");
        log.append("\nSet of action places associated with all Transition Invariants (PA)\n");
        List<Integer> sortedActionPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(sortedActionPlaces);
        List<String> actionPlaceNames = formatPlaceList(sortedActionPlaces);
        log.append("\nAction places: ").append(actionPlaceNames).append("\n");
        
        // Results
        log.append("\n=== ACHIEVABLE MARKS (Action places) ===");
        log.append("\nTotal unique marks: ").append(numReachableMarkings);
        log.append("\nMaximum number of active threads: ").append(maxThreads).append("\n");
        log.append("\nThe Reachability tree is complete at reachabilityTree.log");
        
        write(log.toString());
    }
    
    /**
     * Logs the results of Algorithm 4.2 (Thread responsibility).
     * 
     * @param segments list of execution segments with their transitions
     * @param forkPlaces list of fork places
     * @param joinPlaces list of join places
     */
    public void logAlgorithm2(List<List<Integer>> segments,
                              List<Integer> forkPlaces,
                              List<Integer> joinPlaces) {
        StringBuilder log = new StringBuilder();
        
        // Header
        log.append("\n╔═══════════════════════════════════════════════════════════╗");
        log.append("\n║  ALGORITHM FOR DETERMINING THREAD RESPONSIBILITY (4.2)  ║");
        log.append("\n╚═══════════════════════════════════════════════════════════\n");
        
        // Segments
        log.append("\n=================================");
        log.append("\nTHREADS RESPONSIBILITY");
        log.append("\n=================================");
        for (int i = 0; i < segments.size(); i++) {
            // Format each segment with "T" before each number
            List<String> formattedSegment = new ArrayList<>();
            for (Integer t : segments.get(i)) {
                formattedSegment.add("T" + t);
            }
            log.append("\nSegment ").append(i + 1).append(": ").append(formattedSegment);
        }
        
        // Forks
        log.append("\n\n=================================");
        log.append("\nFORKS");
        if (forkPlaces.isEmpty()) {
            log.append("\nNo forks found.");
        } else {
            List<String> formattedForks = formatPlaceList(forkPlaces);
            log.append("\nFork places: ").append(formattedForks);
        }
        
        // Joins
        log.append("\n\n=================================");
        log.append("\nJOINS");
        if (joinPlaces.isEmpty()) {
            log.append("\nNo joins found.");
        } else {
            List<String> formattedJoins = formatPlaceList(joinPlaces);
            log.append("\nJoin places: ").append(formattedJoins);
        }
        
        write(log.toString());
    }
    
    /**
     * Logs the results of Algorithm 4.3 (Maximum threads per segment).
     * 
     * @param segments list of execution segments
     * @param segmentPlaces list of action places for each segment
     * @param maxThreadsPerSegment maximum threads for each segment
     */
    public void logAlgorithm3(List<List<Integer>> segments,
                              List<List<Integer>> segmentPlaces,
                              List<Integer> maxThreadsPerSegment) {
        StringBuilder log = new StringBuilder();
        
        // Header
        log.append("\n╔═══════════════════════════════════════════════════════════════════╗");
        log.append("\n║  ALGORITHM FOR DETERMINING MAXIMUM THREADS PER SEGMENT (4.3)  ║");
        log.append("\n╚═══════════════════════════════════════════════════════════════════");
        
        for (int i = 0; i < segments.size(); i++) {
            log.append("\n\n=== SEGMENT MARKS (Transitions: ").append(segments.get(i)).append(") ===");
            
            if (segmentPlaces.get(i).isEmpty()) {
                log.append("\nIt hasn't action place neither forks and joins in this segment");
            } else {
                log.append("\nAction places in segment (without forks and joins): ")
                   .append(segmentPlaces.get(i));
            }
            
            log.append("\nMaximum number of active threads in segment: ")
               .append(maxThreadsPerSegment.get(i)).append("\n");
        }
        
        write(log.toString());
    }
    
    /**
     * Formats a list of transition indices as "T0, T1, T2, ...".
     * 
     * @param transitions list of transition indices (may contain values or just indices)
     * @return formatted list of transition names
     */
    private List<String> formatTransitionList(List<Integer> transitions) {
        List<String> formatted = new ArrayList<>();
        for (int i = 0; i < transitions.size(); i++) {
            if (transitions.get(i) > 0) {
                formatted.add("T" + i);
            }
        }
        // If no positive values found, assume the list contains indices directly
        if (formatted.isEmpty()) {
            for (Integer t : transitions) {
                formatted.add("T" + t);
            }
        }
        return formatted;
    }
    
    /**
     * Formats a list of place indices as "P0, P1, P2, ...".
     * 
     * @param places list of place indices
     * @return formatted list of place names
     */
    private List<String> formatPlaceList(List<Integer> places) {
        List<String> formatted = new ArrayList<>();
        for (Integer p : places) {
            formatted.add("P" + p);
        }
        return formatted;
    }
}