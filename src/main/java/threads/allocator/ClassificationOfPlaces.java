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
 * <p>The classification is based on structural analysis of the net using P-invariants
 * (place invariants) and topological criteria. This information is essential for
 * thread allocation analysis.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * ClassificationOfPlaces classifier = new ClassificationOfPlaces(pre, post, W, m0, invariants);
 * Set&lt;Integer&gt; actionPlaces = classifier.getActionPlaces();
 * String report = classifier.logPA();
 * </pre>
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
    
    /** Temporary set of candidate places to be resources (appear in a single invariant) */
    private Set<Integer> possibleRecourse;

    /** Action places associated with each T-invariant */
    private List<List<Integer>> PAofIT;

    /**
     * Constructs the classifier and performs automatic place classification.
     * 
     * <p>The constructor automatically executes the two-phase classification algorithm
     * and computes the action places for each T-invariant.</p>
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
        this.PAofIT = getPAofIT();
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
     *       topological criteria (initial marking > 0, exactly 1 input and 1 output transition).
     *       Remaining candidates are reclassified as action places.</li>
     * </ul>
     * 
     * <p>This two-phase approach ensures accurate classification even in complex
     * net structures with multiple interacting P-invariants.</p>
     */
    private void classifyPlaces() {
        List<List<Integer>> pInvariants = invariants.getPInvariants();
        int numInvariants = pInvariants.size();

        // Phase 1: Classify places according to how many invariants they participate in
        for (int p = 0; p < numPlaces; p++) {
            int count = 0;
            for (List<Integer> inv : pInvariants) {
                if (inv.get(p) > 0) count++;
            }

            if (count == 1) {
                possibleRecourse.add(p); // Possible resource, idle or restriction
            } else if (count > 1) {
                actionPlaces.add(p); // Action place (participates in several invariants)
            }
        }

        // Phase 2: Determine resource, idle, or restriction places within potential candidates
        for (List<Integer> inv : pInvariants) {
            // Filter only the possible ones that appear in this invariant
            List<Integer> involved = possibleRecourse.stream()
                    .filter(r -> r < inv.size() && inv.get(r) > 0)
                    .toList();

            if (involved.size() == 1) {
                // If there is only one candidate in this invariant, 
                // it is either a resource, constraint, or idle.
                resourcePlaces.addAll(involved);
            } else if (involved.size() > 1) {
                // Multiple candidates: identifying the idle place
                boolean done = false;
                for (Integer i : involved) {
                    if(isIdlePlace(i) && !done) {
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
     * <p>These criteria identify places that represent an idle or waiting state
     * in a cyclic process, typically holding tokens when the corresponding
     * resource or entity is not actively working.</p>
     * 
     * @param place the index of the place to check
     * @return true if the place satisfies all idle place criteria
     */
    private boolean isIdlePlace(int place) {
        // Criterion 1: Initial marking > 0
        if (m0[place] == 0) {
            return false;
        }
        
        // Criterion 2 and 3: Count input and output transitions using W
        int inputTransitions = 0;
        int outputTransitions = 0;
        
        for (int t = 0; t < numTransitions; t++) {
            if (W[place][t] > 0) {
                inputTransitions++;  // The transition produces tokens in this place
            }
            if (W[place][t] < 0) {
                outputTransitions++; // The transition consumes tokens from this place
            }
        }
        
        // It is idle if it has exactly 1 input and 1 output
        return inputTransitions == 1 && outputTransitions == 1;
    }

    /**
     * Prints the classification results to the console.
     * Displays resource/restriction/idle places and action places separately.
     */
    public void printClassification() {
        System.out.println("\n=================================");
        System.out.println("Place Classification \n");
        System.out.println("Resources, restrictions and idle places: " + resourcePlaces);
        System.out.println("Action places: " + actionPlaces +"\n");
    }

    /**
     * Generates a log of places associated with each T-invariant.
     * 
     * <p>For each T-invariant, this method identifies all places that are
     * connected to at least one transition in the invariant (either as input
     * or output). This shows the "sphere of influence" of each T-invariant.</p>
     * 
     * @return formatted string showing places (PI) for each T-invariant
     */
    public String logPIofIT() {
        String log = "";
        List<List<Integer>> tInvariants = invariants.getTInvariants();
        
        log += "\n=================================";
        log += "\nSet of places associated with each Transition Invariant (PI of TI) \n";
        
        for (int i = 0; i < tInvariants.size(); i++) {
            List<Integer> tInv = tInvariants.get(i);
            
            Set<Integer> involvedPlaces = new TreeSet<>(); // TreeSet to maintain order
            
            // For each transition of the T-Invariant
            for (int t = 0; t < numTransitions; t++) {
                if (tInv.get(t) > 0) {  // If the transition belongs to the invariant
                    
                    // Add all places connected to this transition
                    for (int p = 0; p < numPlaces; p++) {
                        // If the transition consumes or produces in this place
                        if (pre[p][t] > 0 || post[p][t] > 0) {
                            involvedPlaces.add(p);
                        }
                    }
                }
            }
            
            // Convert indices to "P0, P1, P2, ..." format
            List<String> placeNames = involvedPlaces.stream()
                    .map(p -> "P" + p)
                    .toList();
            
            log += "\nTI " + (i + 1) + ": " + placeNames;
        }
        return log;
    }

    /**
     * Generates a log of action places associated with each T-invariant.
     * 
     * <p>This is similar to logPIofIT(), but filters to show only action places,
     * excluding resource, idle, and restriction places. This focuses on the
     * places that represent active work or state transitions.</p>
     * 
     * @return formatted string showing action places (PA) for each T-invariant
     */
    public String logPAofIT() {
        String log = "\n=================================";
        log += "\nSet of action places associated with each Transition Invariant (PA of TI) \n";
        
        // Validate that PAofIT is calculated
        if (PAofIT == null || PAofIT.isEmpty()) {
            log += "\nNo action places calculated yet. Please run classifyPlaces() first.\n";
            return log;
        }
        
        for (int i = 0; i < PAofIT.size(); i++) {
            // Convert indices to "P0, P1, P2, ..." format
            List<String> actionPlaceNames = PAofIT.get(i).stream()
                    .map(p -> "P" + p)
                    .toList();
            
            log += "\nTI " + (i + 1) + ": " + actionPlaceNames;
        }
        return log;
    }

    /** 
     * Generates a log of all action places in the Petri net.
     * This is the union of action places across all T-invariants.
     * 
     * @return formatted string showing the complete set of action places (PA)
     */
    public String logPA() {
        String log = "\n=================================";
        log += "\nSet of action places associated with all Transition Invariants (PA) \n";
            // Convert indices to "P0, P1, P2, ..." format
        List<String> actionPlaceNames = actionPlaces.stream()
                .sorted()  // Sort numerically
                .map(p -> "P" + p)
                .toList();
        
        log += "\nAction places: " + actionPlaceNames + "\n";
        return log;
    }

    /**
     * Gets the set of action places identified by the classification algorithm.
     * 
     * @return unmodifiable set of action place indices
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
     *   <li>Returns the filtered list</li>
     * </ol>
     * 
     * <p>This method should be called AFTER classifyPlaces() has been executed,
     * as it depends on the actionPlaces set being populated.</p>
     * 
     * @return list of lists, where each inner list contains action place indices
     *         for the corresponding T-invariant
     */
    public List<List<Integer>> getPAofIT() {
        List<List<Integer>> result = new ArrayList<>();
        List<List<Integer>> tInvariants = invariants.getTInvariants();
        
        for (int i = 0; i < tInvariants.size(); i++) {
            List<Integer> tInv = tInvariants.get(i);
            Set<Integer> involvedPlaces = new TreeSet<>();
            
            // For each transition of the T-Invariant
            for (int t = 0; t < numTransitions; t++) {
                if (tInv.get(t) > 0) { // If the transition belongs to the invariant
                    // Add all places connected to this transition
                    for (int p = 0; p < numPlaces; p++) {
                        // If the transition consumes or produces in this place
                        if (pre[p][t] > 0 || post[p][t] > 0) {
                            involvedPlaces.add(p);
                        }
                    }
                }
            }
            
            // Filter only action places (exclude resources/idle/restrictions)
            List<Integer> filteredActionPlaces = involvedPlaces.stream()
                .filter(p -> actionPlaces.contains(p)) // Only action places
                .toList();
            
            // Add the filtered list to the result
            result.add(filteredActionPlaces);
        }
        
        return result;
    }
}