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
     */
    private void isSequential() {
        for (int i = 0; i < tInvariants.size(); i++) {
            List<Integer> currentTI = tInvariants.get(i);
            boolean sharesTransitions = false;
            
            for (int j = 0; j < tInvariants.size(); j++) {
                if (i == j) continue;
                
                List<Integer> otherTI = tInvariants.get(j);
                
                if (hasCommonTransitions(currentTI, otherTI)) {
                    sharesTransitions = true;
                    break;
                }
            }
            
            if (!sharesTransitions) {
                sequences.add(currentTI);
            } else {
                notSequences.add(currentTI);
            }
        }
    }

    /**
     * Checks if two T-invariants share common transitions.
     * 
     * @param it1 first T-invariant vector
     * @param it2 second T-invariant vector
     * @return true if the invariants share at least one transition
     */
    private boolean hasCommonTransitions(List<Integer> it1, List<Integer> it2) {
        for (int i = 0; i < it1.size(); i++) {
            if (it1.get(i) > 0 && it2.get(i) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyzes all action places to identify forks and joins.
     */
    private void analyzeForkOrJoin() {
        for (Integer p : actionPlaces) {
            if (isJoin(p)) joins.add(p);
            if (isFork(p)) forks.add(p);
        }
    }

    /**
     * Segments T-invariants into execution segments based on synchronization points.
     * 
     * <p>Segmentation rules:</p>
     * <ul>
     *   <li>Sequential T-invariants form single segments</li>
     *   <li>Parallel T-invariants are split at transitions that produce tokens
     *       into fork or join places</li>
     * </ul>
     */
    private void segmentInvariants() {
        Set<List<Integer>> uniqueSegments = new HashSet<>();
        
        // Convert sequences to index format and add as segments
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
        
        // Process parallel T-invariants
        for (List<Integer> sublist : notSequences) {
            List<Integer> activeTransitions = new ArrayList<>();
            for (int idx = 0; idx < sublist.size(); idx++) {
                if (sublist.get(idx) > 0) {
                    activeTransitions.add(idx);
                }
            }
            
            List<Integer> newSegment = new ArrayList<>();
            
            for (int t : activeTransitions) {
                newSegment.add(t);
                
                boolean isCutPoint = false;
                
                // Check if this transition produces to a fork or join
                for (int p = 0; p < pre.length; p++) {
                    if ((post[p][t] > 0 && forks.contains(p)) ||
                        (post[p][t] > 0 && joins.contains(p))) {
                        isCutPoint = true;
                        break;
                    }
                }
                
                // If cut point found, save segment and start new one
                if (isCutPoint) {
                    uniqueSegments.add(new ArrayList<>(newSegment));
                    newSegment.clear();
                }
            }
            
            // Save the last segment if not empty
            if (!newSegment.isEmpty()) {
                uniqueSegments.add(new ArrayList<>(newSegment));
            }
        }
        
        segments.addAll(uniqueSegments);
    }

    /**
     * Gets the list of identified execution segments.
     * 
     * @return list where each element is a segment (list of transition indices)
     */
    public List<List<Integer>> getSegments() {
        return segments;
    }

    /**
     * Determines if a place is a fork (parallel split point).
     * 
     * @param place the index of the place to check
     * @return true if the place has multiple output transitions
     */
    private boolean isFork(int place) {
        int outputTransitions = 0;
        
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
     * @param place the index of the place to check
     * @return true if the place has multiple input transitions
     */
    private boolean isJoin(int place) {
        int inputTransitions = 0;
        
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
     * @return list of place indices that are forks
     */
    public List<Integer> getForkPlaces() {
        return forks;
    }

    /**
     * Gets the list of join places identified in the analysis.
     * 
     * @return list of place indices that are joins
     */
    public List<Integer> getJoinPlaces() {
        return joins;
    }
}