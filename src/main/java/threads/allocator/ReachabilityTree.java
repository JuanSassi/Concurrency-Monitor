import java.util.*;
import java.util.ArrayList;

/**
 * Class representing a reachability tree for action places in a Petri net.
 * This class computes and stores all reachable markings considering only
 * the action places of the net, without storing transition information.
 * 
 * <p>The reachability tree is constructed using breadth-first search (BFS)
 * to explore all possible markings from the initial state. Only markings
 * of action places are stored to reduce memory usage and focus on relevant
 * system states.</p>
 * 
 * <p>This class is used to determine:</p>
 * <ul>
 *   <li>Maximum number of concurrent active threads</li>
 *   <li>Maximum threads per execution segment</li>
 *   <li>All reachable states of the system</li>
 * </ul>
 * 
 * @author Sassi Juan Ignacio
 */
public class ReachabilityTree {
    /** Reference to the Petri net instance */
    private PetriNet petriNet;
    
    /** Set of indices of action places to track */
    private Set<Integer> actionPlaces;
    
    /** Set of all reachable markings for action places */
    private Set<int[]> reachableMarkings;
    
    /** Sum of tokens for each reachable marking */
    private int[] markSum;
    
    /** Maximum number of concurrent threads across all markings */
    private int maxNumThreads;
    
    /**
     * Constructs a reachability tree for the specified action places.
     * Automatically builds the complete reachability set and computes
     * maximum thread statistics.
     * 
     * @param actionPlaces set of place indices to track in the reachability analysis
     */
    public ReachabilityTree(Set<Integer> actionPlaces) {
        this.petriNet = PetriNet.getInstance();
        this.actionPlaces = actionPlaces;
        this.reachableMarkings = new LinkedHashSet<>();

        buildReachabilitySet();
        this.markSum = getMarkSum();
        this.maxNumThreads = getMaxNumThreads();
    }
    
    /**
     * Builds the complete set of reachable markings using breadth-first search.
     * 
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Start from the initial marking</li>
     *   <li>For each marking, try firing all enabled transitions</li>
     *   <li>Add new markings to the queue and visited set</li>
     *   <li>Continue until no new markings are discovered</li>
     * </ol>
     * 
     * <p>Both temporal and immediate transitions are handled correctly,
     * ensuring accurate state space exploration.</p>
     */
    private void buildReachabilitySet() {
        Map<String, int[]> visited = new HashMap<>();
        Queue<int[]> queue = new LinkedList<>();
        
        // Initial marking
        int[] initialMarking = petriNet.getMarking();
        int[] initialActionMarking = extractActionPlaces(initialMarking);
        
        String key = markingToString(initialMarking);
        visited.put(key, initialActionMarking);
        reachableMarkings.add(initialActionMarking);
        queue.add(initialMarking);
        
        // BFS exploration
        while (!queue.isEmpty()) {
            int[] currentMarking = queue.poll();
            setMarking(currentMarking);
            
            // Try all enabled transitions
            for (int t = 0; t < petriNet.getNumTransitions(); t++) {
                if (petriNet.transitionEnabled(t)) {
                    // Fire transition
                    if (petriNet.isTemporary(t)) {
                        petriNet.consumeTokens(t);
                        petriNet.produceTokens(t);
                    } else {
                        petriNet.fire(t);
                    }
                    
                    // Get new marking
                    int[] newMarking = petriNet.getMarking();
                    String newKey = markingToString(newMarking);
                    
                    // If it's a new marking, add it
                    if (!visited.containsKey(newKey)) {
                        int[] newActionMarking = extractActionPlaces(newMarking);
                        visited.put(newKey, newActionMarking);
                        reachableMarkings.add(newActionMarking);
                        queue.add(newMarking);
                    }
                    
                    // Restore marking to try other transitions
                    setMarking(currentMarking);
                }
            }
        }
    }

    /**
     * Computes the sum of tokens for each reachable marking.
     * The token sum represents the potential number of concurrent threads
     * in that particular state.
     * 
     * @return array where each element is the sum of tokens in the corresponding marking
     */
    private int[] getMarkSum() {
        int[] marks = new int[reachableMarkings.size()];
        int i = 0;
        for (int[] marking : reachableMarkings) {
            int sum = 0;
            for (int tokens : marking) {
                sum += tokens;
            }
            marks[i] = sum;
            i++;
        }
        return marks;
    }

    /**
     * Gets the maximum value of token sum across all reachable markings.
     * This represents the maximum number of threads that can be simultaneously
     * active in the system.
     * 
     * @return the maximum number of concurrent threads
     */
    public int getMaxNumThreads() {
        int max = 0;
        for (int sum : markSum) {
            if (sum > max) {
                max = sum;
            }
        }
        return max;
    }
    
    /**
     * Extracts only the values of action places from a full marking vector.
     * The resulting array contains only the token counts for places that
     * are classified as action places, in sorted order by place index.
     * 
     * @param fullMarking complete marking vector including all places
     * @return array containing only action place token counts
     */
    private int[] extractActionPlaces(int[] fullMarking) {
        int[] actionMarking = new int[actionPlaces.size()];
        int index = 0;
        List<Integer> sortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(sortedPlaces);
        for (Integer placeIndex : sortedPlaces) {
            if (placeIndex >= 0 && placeIndex < fullMarking.length) {
                actionMarking[index++] = fullMarking[placeIndex];
            }
        }
        return actionMarking;
    }
    
    /**
     * Converts a marking array to a string representation for use as a hash key.
     * This enables efficient duplicate detection during reachability analysis.
     * 
     * @param marking the marking vector to convert
     * @return string representation of the marking
     */
    private String markingToString(int[] marking) {
        return Arrays.toString(marking);
    }
    
    /**
     * Sets a marking in the Petri net using reflection.
     * This is a workaround to access the setMarking method which may be private.
     * 
     * @param marking the marking vector to set in the Petri net
     * @throws RuntimeException if the setMarking method cannot be accessed
     */
    private void setMarking(int[] marking) {
        try {
            java.lang.reflect.Method method = petriNet.getClass()
                .getDeclaredMethod("setMarking", int[].class);
            method.setAccessible(true);
            method.invoke(petriNet, (Object) marking);
        } catch (Exception e) {
            throw new RuntimeException("Add setMarking(int[]) method in PetriNet", e);
        }
    }
    
    /**
     * Gets all reachable markings of the action places.
     * Returns a copy to prevent external modification of the internal state.
     * 
     * @return set of all reachable action place markings
     */
    public Set<int[]> getReachableMarkings() {
        return new LinkedHashSet<>(reachableMarkings);
    }
    
    /**
     * Gets the total number of distinct reachable markings.
     * 
     * @return count of unique reachable states
     */
    public int getNumReachableMarkings() {
        return reachableMarkings.size();
    }

    /**
     * Generates a summary log of maximum thread analysis.
     * Includes total markings found and maximum concurrent threads,
     * with a reference to the detailed log file.
     * 
     * @return formatted string with analysis summary
     */
    public String logMaxThreads() {
        String log = "\n=== ACHIEVABLE MARKS (Action places) ===";
        log += "\nTotal unique marks: " + reachableMarkings.size();
        log += "\nMaximum number of active threads: " + maxNumThreads +"\n";
        log += "\nThe Reachability tree is complete at reachabilityTree.log";
        return log;
    }
    
    /**
     * Generates a detailed log of all reachable markings.
     * 
     * <p>The output includes:</p>
     * <ul>
     *   <li>Header with action place indices</li>
     *   <li>Each reachable marking as a row</li>
     *   <li>Token sum for each marking</li>
     *   <li>Statistics about maximum threads</li>
     * </ul>
     * 
     * <p>By default, only the first 20 markings are printed unless
     * full print mode is enabled in the configuration.</p>
     * 
     * @return formatted string with complete marking table
     */
    public String logMarkings() {
        String log = "\n=== ACHIEVABLE MARKS (Action places) ===";
        log += "\nTotal unique marks: " + reachableMarkings.size();
        log += "\nMaximum number of active threads: " + maxNumThreads +"\n";
        
        // Create sorted list of action places for header
        List<Integer> sortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(sortedPlaces);
        
        // Print header
        log += "\nM\t";
        for (Integer place : sortedPlaces) {
            log += "P" + place + "\t";
        }
        log += "SUM\n";
        
        // Print separator line
        for (int j = 0; j < sortedPlaces.size()+1; j++) {
            log += "---\t";
        }
        log += "----\n";
        
        // Print markings
        int i = 0;
        boolean fullPrint = ConfigLoader.getFullprint();
        for (int[] marking : reachableMarkings) {
            if (i >= 20 && !fullPrint) break;
            log += "M" + i + "\t";
            for (int token : marking) {
                log += token + "\t";
            }
            log += markSum[i]+"\n";
            i++;
        }
        return log;
    }

    /**
     * Calculates the maximum number of threads in a specific segment.
     * 
     * <p>For each reachable marking, sums the tokens only in the places
     * belonging to the segment and returns the maximum sum found.</p>
     * 
     * @param segmentPlaces list of place indices that belong to the segment
     * @return maximum concurrent threads possible in this segment, or 1 if
     *         the segment has no action places
     */
    public int calculateMaxThreadsInSegment(List<Integer> segmentPlaces) {
        if (segmentPlaces.isEmpty()) {
            return 1;
        }
        
        int maxThreadsSegment = 0;
        List<Integer> allSortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(allSortedPlaces);
        
        for (int[] marking : reachableMarkings) {
            int segmentSum = 0;
            for (Integer segmentPlace : segmentPlaces) {
                int markingIndex = allSortedPlaces.indexOf(segmentPlace);
                if (markingIndex >= 0 && markingIndex < marking.length) {
                    segmentSum += marking[markingIndex];
                }
            }
            if (segmentSum > maxThreadsSegment) {
                maxThreadsSegment = segmentSum;
            }
        }
        
        return maxThreadsSegment;
    }

    /**
     * Generates a summary log for a specific segment showing maximum threads.
     * 
     * @param segment list of transition indices in the segment
     * @param segmentPlaces list of action places in the segment (excluding forks/joins)
     * @return formatted string with segment analysis summary
     */
    public String logThreadsPerSegment(List<Integer> segment, List<Integer> segmentPlaces) {
        String log = "";
        int maxThreadsSegment = calculateMaxThreadsInSegment(segmentPlaces);

        log += "\n=== SEGMENT MARKS (Transitions: " + segment + ") ===";
        if (segmentPlaces.isEmpty()) {
            log += "\nIt hasn't action place neither forks and joins in this segment";
        } else {
            log += "\nAction places in segment (without forks and joins): " + segmentPlaces;
        }
        log += "\nMaximum number of active threads in segment: " + maxThreadsSegment + "\n";
        
        return log;
    }

    /**
     * Generates a detailed log of reachable markings for a specific segment.
     * 
     * <p>Similar to logMarkings(), but filters the output to show only
     * the places relevant to the specified segment. This provides a focused
     * view of the state space for segment-specific analysis.</p>
     * 
     * <p>The output includes:</p>
     * <ul>
     *   <li>Segment identification (transitions)</li>
     *   <li>Action places in the segment</li>
     *   <li>Marking table showing only segment places</li>
     *   <li>Token sums for the segment places only</li>
     * </ul>
     * 
     * @param segment list of transition indices that form the segment
     * @param segmentPlaces list of action places in the segment (excluding forks/joins)
     * @return formatted string with segment-specific marking table
     */
    public String logSegment(List<Integer> segment, List<Integer> segmentPlaces) {
        String log = "";
        if (segmentPlaces.isEmpty()) {
            log += "\n=== SEGMENT MARKS (Transitions: " + segment + ") ===";
            log += "\nIt hasn't action place neither forks and joins in this segment";
            log += "\nMaximum number of active threads in segment: " + 1 +"\n";
            return log;
        }
        
        // Calculate maximum threads for this segment
        int maxThreadsSegment = 0;
        List<Integer> allSortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(allSortedPlaces);
        
        for (int[] marking : reachableMarkings) {
            int segmentSum = 0;
            for (Integer segmentPlace : segmentPlaces) {
                int markingIndex = allSortedPlaces.indexOf(segmentPlace);
                if (markingIndex >= 0 && markingIndex < marking.length) {
                    segmentSum += marking[markingIndex];
                }
            }
            if (segmentSum > maxThreadsSegment) {
                maxThreadsSegment = segmentSum;
            }
        }
        
        log += "\n=== SEGMENT MARKS (Transitions: " + segment + ") ===";
        log += "\nAction places in segment (without forks and joins): " + segmentPlaces;
        log += "\nMaximum number of active threads in segment: " + maxThreadsSegment + "\n";
        
        // Sort segment places
        Collections.sort(segmentPlaces);
        
        // Print header
        log += "\nM\t";
        for (Integer place : segmentPlaces) {
            log += "P" + place + "\t";
        }
        log += "SUM\n";
        
        // Print separator line
        log += "---\t";
        for (int j = 0; j < segmentPlaces.size(); j++) {
            log += "---\t";
        }
        log += "----\n";
        
        // Print filtered markings
        int i = 0;
        boolean fullPrint = ConfigLoader.getFullprint();
        for (int[] marking : reachableMarkings) {
            if (i >= 20 && !fullPrint) break;
            
            log += "M" + i + "\t";
            int segmentSum = 0;
            
            // Print only tokens from segment places
            for (Integer segmentPlace : segmentPlaces) {
                // Find the index of this place in the marking
                int markingIndex = allSortedPlaces.indexOf(segmentPlace);
                if (markingIndex >= 0 && markingIndex < marking.length) {
                    int tokens = marking[markingIndex];
                    log += tokens + "\t";
                    segmentSum += tokens;
                } else {
                    log += "0\t";
                }
            }
            
            log += segmentSum + "\n";
            i++;
        }
        return log;
    }
}