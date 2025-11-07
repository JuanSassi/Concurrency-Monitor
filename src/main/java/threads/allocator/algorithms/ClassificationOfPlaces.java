import java.util.*;

/**
 * Automatic classification of Petri net places based on P-invariant analysis.
 * 
 * <p>This class implements an algorithm that categorizes places into two main types:</p>
 * <ul>
 *   <li><b>Action places</b>: Participate in multiple P-invariants, representing active
 *       system states or work-in-progress</li>
 *   <li><b>Resource places</b>: Appear in a single P-invariant, including:
 *       <ul>
 *         <li>Resource places (available resources)</li>
 *         <li>Idle places (idle state with tokens)</li>
 *         <li>Restriction places (capacity constraints)</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * @author Juan Ignacio Sassi
 */
public class ClassificationOfPlaces {
    /** Pre-incidence matrix (token consumption by transitions) */
    private int[][] pre;
    
    /** Post-incidence matrix (token production by transitions) */
    private int[][] post;
    
    /** Incidence matrix (W = Post - Pre) */
    private int[][] W;
    
    /** Initial marking vector of the net */
    private int[] m0;
    
    /** Total number of places in the net */
    private int numPlaces;
    
    /** Total number of transitions in the net */
    private int numTransitions;
    
    /** Net invariants calculator */
    private Invariants invariants;
    
    /** Set of places classified as resources (includes idle, resources and restrictions) */
    private Set<Integer> resourcePlaces;
    
    /** Set of places classified as action places */
    private Set<Integer> actionPlaces;
    
    /** Temporary set of candidate places to be resources */
    private Set<Integer> possibleRecourse;

    /** Action places associated with each T-invariant */
    private List<List<Integer>> PAofIT;

    /**
     * Constructs the classifier and performs automatic place classification.
     * 
     * @param pre  pre-incidence matrix of dimension [numPlaces][numTransitions]
     * @param post post-incidence matrix of dimension [numPlaces][numTransitions]
     * @param W    incidence matrix (W = Post - Pre)
     * @param m0   initial marking vector of dimension [numPlaces]
     * @param invariants invariants calculator containing P-invariants and T-invariants
     */
    public ClassificationOfPlaces(int[][] pre, int[][] post, int[][] W, int[] m0, Invariants invariants) {
        resourcePlaces = new HashSet<>();
        actionPlaces = new HashSet<>();
        possibleRecourse = new HashSet<>();
        PAofIT = new ArrayList<>();

        this.pre = pre;
        this.post = post;
        this.m0 = m0;
        this.W = W;
        this.numPlaces = pre.length;
        this.numTransitions = pre[0].length;
        this.invariants = invariants;

        classifyPlaces();
        this.PAofIT = computePAofIT();
    }

    /**
     * Classifies the net's places into action places and resource/restriction/idle places.
     * 
     * <p>The algorithm implements two phases:</p>
     * 
     * <p><b>Phase 1: Initial classification based on P-invariant participation</b></p>
     * <ul>
     *   <li>Places in a single P-invariant → candidates for resource/restriction/idle</li>
     *   <li>Places in multiple P-invariants → action places</li>
     * </ul>
     * 
     * <p><b>Phase 2: Refinement of candidate classification</b></p>
     * <ul>
     *   <li>If a P-invariant has a single candidate → confirmed as resource/restriction/idle</li>
     *   <li>If a P-invariant has multiple candidates → identify the idle place using
     *       topological criteria. Remaining candidates are reclassified as action places.</li>
     * </ul>
     */
    private void classifyPlaces() {
        List<List<Integer>> pInvariants = invariants.getPInvariants();

        // Phase 1: Classify places according to how many invariants they participate in
        for (int p = 0; p < numPlaces; p++) {
            int count = 0;
            for (List<Integer> inv : pInvariants) {
                if (inv.get(p) > 0) count++;
            }

            if (count == 1) {
                possibleRecourse.add(p);
            } else if (count > 1) {
                actionPlaces.add(p);
            }
        }

        // Phase 2: Determine resource, idle, or restriction places
        for (List<Integer> inv : pInvariants) {
            List<Integer> involved = possibleRecourse.stream()
                    .filter(r -> r < inv.size() && inv.get(r) > 0)
                    .toList();

            if (involved.size() == 1) {
                resourcePlaces.addAll(involved);
            } else if (involved.size() > 1) {
                boolean done = false;
                for (Integer i : involved) {
                    if (isIdlePlace(i) && !done) {
                        resourcePlaces.add(i);
                        done = true;
                    } else {
                        actionPlaces.add(i);
                    }
                }
            }
        }
    }

    /**
     * Determines if a place is an idle place according to topological criteria.
     * 
     * <p>A place is classified as idle if it satisfies ALL of the following:</p>
     * <ol>
     *   <li>Initial marking > 0 (contains tokens at start)</li>
     *   <li>Exactly 1 input transition (one way to enter)</li>
     *   <li>Exactly 1 output transition (one way to exit)</li>
     * </ol>
     * 
     * @param place the index of the place to check
     * @return true if the place satisfies all idle place criteria
     */
    private boolean isIdlePlace(int place) {
        if (m0[place] == 0) {
            return false;
        }
        
        int inputTransitions = 0;
        int outputTransitions = 0;
        
        for (int t = 0; t < numTransitions; t++) {
            if (W[place][t] > 0) {
                inputTransitions++;
            }
            if (W[place][t] < 0) {
                outputTransitions++;
            }
        }
        
        return inputTransitions == 1 && outputTransitions == 1;
    }

    /**
     * Prints the classification results to the console.
     */
    public void printClassification() {
        System.out.println("\n=================================");
        System.out.println("Place Classification \n");
        System.out.println("Resources, restrictions and idle places: " + resourcePlaces);
        System.out.println("Action places: " + actionPlaces + "\n");
    }

    /**
     * Gets the set of action places identified by the classification algorithm.
     * 
     * @return set of action place indices
     */
    public Set<Integer> getActionPlaces() {
        return actionPlaces;
    }

    /**
     * Computes the action places associated with each T-invariant.
     * 
     * <p>For each T-invariant, this method:</p>
     * <ol>
     *   <li>Identifies all places connected to transitions in the invariant</li>
     *   <li>Filters to keep only places classified as action places</li>
     * </ol>
     * 
     * @return list of lists, where each inner list contains action place indices
     *         for the corresponding T-invariant
     */
    public List<List<Integer>> getPAofIT() {
        return PAofIT;
    }

    /**
     * Internal method to compute action places per T-invariant.
     * Called during construction.
     * 
     * @return list of action places for each T-invariant
     */
    private List<List<Integer>> computePAofIT() {
        List<List<Integer>> result = new ArrayList<>();
        List<List<Integer>> tInvariants = invariants.getTInvariants();
        
        for (List<Integer> tInv : tInvariants) {
            Set<Integer> involvedPlaces = new TreeSet<>();
            
            // For each transition of the T-Invariant
            for (int t = 0; t < numTransitions; t++) {
                if (tInv.get(t) > 0) {
                    // Add all places connected to this transition
                    for (int p = 0; p < numPlaces; p++) {
                        if (pre[p][t] > 0 || post[p][t] > 0) {
                            involvedPlaces.add(p);
                        }
                    }
                }
            }
            
            // Filter only action places
            List<Integer> filteredActionPlaces = involvedPlaces.stream()
                .filter(p -> actionPlaces.contains(p))
                .toList();
            
            result.add(filteredActionPlaces);
        }
        
        return result;
    }
}