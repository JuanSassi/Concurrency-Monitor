import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Class for analyzing thread responsibilities and execution segments in a Petri net.
 * 
 * <p>This class implements Algorithm 4.2 for determining thread responsibility by:</p>
 * <ul>
 *   <li>Classifying T-invariants as sequential or parallel</li>
 *   <li>Identifying fork and join places in the net structure</li>
 *   <li>Segmenting T-invariants into execution segments based on synchronization points</li>
 * </ul>
 * 
 * <p>A <b>fork</b> is a place with multiple output transitions (parallel split).
 * A <b>join</b> is a place with multiple input transitions (parallel merge).
 * <b>Segments</b> are groups of transitions that should be managed by the same thread pool.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * Responsibilities resp = new Responsibilities(pre, post, tInvariants, actionPlaces);
 * List&lt;List&lt;Integer&gt;&gt; segments = resp.getSegments();
 * List&lt;Integer&gt; forks = resp.getForkPlaces();
 * String analysis = resp.logAnalysis();
 * </pre>
 * 
 * @author Sassi Juan Ignacio
 */
public class Responsibilities {
    /** Pre-incidence matrix (token consumption by transitions) */
    private int[][] pre;
    
    /** Post-incidence matrix (token production by transitions) */
    private int[][] post;

    /** Set of action places in the Petri net */
    private Set<Integer> actionPlaces;
    
    /** List of minimal T-invariants */
    private List<List<Integer>> tInvariants;

    /** T-invariants classified as sequential (no shared transitions) */
    private List<List<Integer>> sequences;
    
    /** T-invariants classified as parallel (shared transitions) */
    private List<List<Integer>> notSequences;
    
    /** Execution segments identified for thread allocation */
    private List<List<Integer>> segments;

    /** List of fork places (places with multiple output transitions) */
    private List<Integer> forks;
    
    /** List of join places (places with multiple input transitions) */
    private List<Integer> joins;

    /**
     * Constructs a Responsibilities analyzer and performs the complete analysis.
     * 
     * <p>The constructor automatically:</p>
     * <ol>
     *   <li>Classifies T-invariants as sequential or parallel</li>
     *   <li>Identifies all fork and join places</li>
     *   <li>Segments the T-invariants based on synchronization points</li>
     * </ol>
     * 
     * @param pre pre-incidence matrix of the Petri net
     * @param post post-incidence matrix of the Petri net
     * @param tInvariants list of minimal T-invariants
     * @param actionPlaces set of action places in the net
     */
    public Responsibilities(int[][] pre, int[][] post, List<List<Integer>> tInvariants, 
                            Set<Integer> actionPlaces) {
        this.actionPlaces = actionPlaces;
        this.tInvariants = tInvariants;
        this.pre = pre;
        this.post = post;

        forks = new ArrayList<>();
        joins = new ArrayList<>();
        segments = new ArrayList<>();
        sequences = new ArrayList<>();
        notSequences = new ArrayList<>();

        isSequential();
        analyzeForkOrJoin();
        segmentInvariants();
    }

    /**
     * Classifies T-invariants as sequential or parallel based on transition sharing.
     * 
     * <p>A T-invariant is classified as:</p>
     * <ul>
     *   <li><b>Sequential</b>: if it doesn't share any transitions with other T-invariants</li>
     *   <li><b>Parallel</b>: if it shares at least one transition with another T-invariant</li>
     * </ul>
     * 
     * <p>This classification helps determine which parts of the system can execute
     * independently and which require coordination.</p>
     */
    private void isSequential() {
        // For each T-invariant, check if it shares transitions with other T-invariants
        for (int i = 0; i < tInvariants.size(); i++) {
            List<Integer> currentTI = tInvariants.get(i);
            boolean sharesTransitions = false;
            
            // Compare with all other T-invariants
            for (int j = 0; j < tInvariants.size(); j++) {
                if (i == j) continue; // Skip the same row
                
                List<Integer> otherTI = tInvariants.get(j);
                
                // Check if they share any transitions (positions with values > 0)
                if (hasCommonTransitions(currentTI, otherTI)) {
                    sharesTransitions = true;
                    break;
                }
            }
            
            // If this T-invariant doesn't share transitions, it's sequential
            if (!sharesTransitions) {
                sequences.add(currentTI);
            } else {
                notSequences.add(currentTI);
            }
        }
    }

    /**
     * Checks if two T-invariants share common transitions.
     * Two T-invariants share transitions if they both have positive values
     * at the same transition indices.
     * 
     * @param it1 first T-invariant vector
     * @param it2 second T-invariant vector
     * @return true if the invariants share at least one transition, false otherwise
     */
    private boolean hasCommonTransitions(List<Integer> it1, List<Integer> it2) {
        // Search for positions where both T-invariants have values > 0
        for (int i = 0; i < it1.size(); i++) {
            if (it1.get(i) > 0 && it2.get(i) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyzes all action places to identify forks and joins.
     * 
     * <p>This method populates the forks and joins lists by testing
     * each action place with the isFork() and isJoin() criteria.</p>
     */
    private void analyzeForkOrJoin () {
        for(Integer p : actionPlaces){
            if(isJoin(p)) joins.add(p);
            if(isFork(p)) forks.add(p);
        }
    }

    /**
     * Segments T-invariants into execution segments based on synchronization points.
     * 
     * <p>Segmentation rules:</p>
     * <ul>
     *   <li>Sequential T-invariants form single segments</li>
     *   <li>Parallel T-invariants are split at transitions that:</li>
     *   <ul>
     *     <li>Produce tokens into a fork place, OR</li>
     *     <li>Produce tokens into a join place</li>
     *   </ul>
     * </ul>
     * 
     * <p>Each segment represents a unit of work that can be assigned to
     * a separate thread pool or managed independently.</p>
     */
    private void segmentInvariants() {
        Set<List<Integer>> uniqueSegments = new HashSet<>();
        
        // First, convert sequences to index format and add them as segments
        for (List<Integer> seq : sequences) {
            List<Integer> sequenceIndices = new ArrayList<>();
            for (int idx = 0; idx < seq.size(); idx++) {
                if (seq.get(idx) > 0) {
                    sequenceIndices.add(idx);
                }
            }
            if (!sequenceIndices.isEmpty()) {
                uniqueSegments.add(sequenceIndices);
            }
        }
        
        // Now process parallel T-invariants
        for (List<Integer> sublist : notSequences) {
            // First, extract the indices of active transitions (value > 0)
            List<Integer> activeTransitions = new ArrayList<>();
            for (int idx = 0; idx < sublist.size(); idx++) {
                if (sublist.get(idx) > 0) {
                    activeTransitions.add(idx);
                }
            }
            
            // Now segment these active transitions
            List<Integer> newSegment = new ArrayList<>();
            
            for (int t : activeTransitions) {
                // Add current transition to segment
                newSegment.add(t);
                
                boolean isCutPoint = false;
                
                // Check if this transition touches a fork or join
                for (int p = 0; p < pre.length; p++) {
                    // If it produces to a fork OR produces to a join → cut point
                    if ((post[p][t] > 0 && forks.contains(p)) ||
                        (post[p][t] > 0 && joins.contains(p))) {
                        isCutPoint = true;
                        break;
                    }
                }
                
                // If we found a cut point, save the segment and start a new one
                if (isCutPoint) {
                    uniqueSegments.add(new ArrayList<>(newSegment));
                    newSegment.clear(); // Start new EMPTY segment
                }
            }
            
            // Save the last segment if anything is pending
            if (!newSegment.isEmpty()) {
                uniqueSegments.add(new ArrayList<>(newSegment));
            }
        }
        
        // Convert Set to List to store in segments
        segments.addAll(uniqueSegments);
    }

    /**
     * Generates a formatted log of the responsibility analysis results.
     * 
     * <p>The output includes:</p>
     * <ul>
     *   <li>List of all identified segments with their transitions</li>
     *   <li>List of all fork places</li>
     *   <li>List of all join places</li>
     * </ul>
     * 
     * @return formatted string with complete responsibility analysis
     */
    public String logAnalysis() {
        String log ="\n=================================";
        log +="\nTHREADS RESPONSIBILITY";
        log +="\n=================================";
        for (int i = 0; i < segments.size(); i++) {
            // Format each segment with "T" before each number
            List<String> formattedSegment = new ArrayList<>();
            for (Integer t : segments.get(i)) {
                formattedSegment.add("T" + t);
            }
            log +="\nSegment " + (i + 1) + ": " + formattedSegment;
        }
        
        log +="\n=================================";
        log +="\nFORKS";
        if (forks.isEmpty()) {
            log +="\nNo forks found.";
        } else {
            // Format forks with "P" before each number
            List<String> formattedForks = new ArrayList<>();
            for (Integer p : forks) {
                formattedForks.add("P" + p);
            }
            log +="\nFork places: " + formattedForks;
        }
        
        log +="\n=================================";
        log +="\nJOINS";
        if (joins.isEmpty()) {
            log +="\nNo joins found.";
        } else {
            // Format joins with "P" before each number
            List<String> formattedJoins = new ArrayList<>();
            for (Integer p : joins) {
                formattedJoins.add("P" + p);
            }
            log +="\nJoin places: " + formattedJoins;
        }
        return log;
    }

    /**
     * Gets the list of identified execution segments.
     * 
     * @return list where each element is a segment (list of transition indices)
     */
    public List<List<Integer>> getSegments(){
        return segments;
    }

    /**
     * Determines if a place is a fork (parallel split point).
     * 
     * <p>A place is a fork if it has multiple output transitions, meaning
     * multiple transitions can consume tokens from it. This indicates a
     * point where execution can split into parallel paths.</p>
     * 
     * <p>Implementation: counts transitions in the pre-matrix that consume
     * from this place (pre[place][transition] > 0).</p>
     * 
     * @param place the index of the place to check
     * @return true if the place is a fork (has multiple output transitions)
     */
    private boolean isFork(int place) {
        // A fork occurs when a place has multiple output transitions
        // We check the PRE matrix: if the place consumes tokens in more than one transition
        
        int outputTransitions = 0;
        
        // Iterate over all transitions (columns) for this place (row)
        for (int transition = 0; transition < pre[place].length; transition++) {
            if (pre[place][transition] > 0) {
                outputTransitions++;
            }
        }
        
        return outputTransitions > 1;
    }

    /**
     * Determines if a place is a join (parallel merge point).
     * 
     * <p>A place is a join if it has multiple input transitions, meaning
     * multiple transitions can produce tokens into it. This indicates a
     * point where parallel execution paths converge.</p>
     * 
     * <p>Implementation: counts transitions in the post-matrix that produce
     * into this place (post[place][transition] > 0).</p>
     * 
     * @param place the index of the place to check
     * @return true if the place is a join (has multiple input transitions)
     */
    private boolean isJoin(int place) {
        // A join occurs when a place has multiple input transitions
        // We check the POST matrix: if the place produces tokens from more than one transition
        
        int inputTransitions = 0;
        
        // Iterate over all transitions (columns) for this place (row)
        for (int transition = 0; transition < post[place].length; transition++) {
            if (post[place][transition] > 0) {
                inputTransitions++;
            }
        }
        
        return inputTransitions > 1;
    }

    /**
     * Gets the list of fork places identified in the analysis.
     * 
     * @return list of place indices that are forks (parallel split points)
     */
    public List<Integer> getForkPlaces(){
        return forks;
    }

    /**
     * Gets the list of join places identified in the analysis.
     * 
     * @return list of place indices that are joins (parallel merge points)
     */
    public List<Integer> getJoinPlaces(){
        return joins;
    }

}