import java.util.*;

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
        this.maxNumThreads = calculateMaxNumThreads();
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
     * Calculates the maximum number of threads across all reachable markings.
     * This represents the maximum number of threads that can be simultaneously
     * active in the system.
     * 
     * @return the maximum number of concurrent threads
     */
    private int calculateMaxNumThreads() {
        int max = 0;
        for (int[] marking : reachableMarkings) {
            int sum = 0;
            for (int tokens : marking) {
                sum += tokens;
            }
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
     * Gets the maximum number of threads that can be simultaneously active.
     * 
     * @return the maximum number of concurrent threads
     */
    public int getMaxNumThreads() {
        return maxNumThreads;
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
}