import java.util.*;

/**
 * The algorithm automatically classifies the places of a Petri Net into:
 *   - Action places: participate in multiple place invariants
 *   - Resource places: appear in a single invariant (includes idle, resources and restrictions)
 * 
 * @author Juan Ignacio Sassi
 */
public class ClassificationOfPlaces {
    /** Pre matrix (token consumption by transitions) */
    private int[][] pre;
    
    /** Post matrix (token production by transitions) */
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

    private List<List<Integer>> PAofIT;

    /**
     * Constructor that initializes the algorithm with the Petri Net matrices.
     * 
     * Automatically calculates the incidence matrix W and classifies the places
     * according to their participation in place invariants.
     * 
     * @param pre  Pre matrix of dimension [numPlaces][numTransitions] indicating
     *             tokens consumed by each transition
     * @param post Post matrix of dimension [numPlaces][numTransitions] indicating
     *             tokens produced by each transition
     * @param m0   Initial marking vector of dimension [numPlaces]
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
     * Classifies the net's places into action places and resource, restriction or idle places
     * The algorithm implements two phases:
     * 
     * - Phase 1: Classifies places according to their participation in P-invariants:
     *       Places appearing in a single invariant → candidates for resource, restriction or idle
     *       Places appearing in multiple invariants → action places
     * 
     * - Phase 2: Refines the classification of candidate places:
     *       If an invariant has a single candidate → it is a resource, restriction or idle
     *       If an invariant has multiple candidates → identifies the idle place
     *       using topological and initial marking criteria, the rest are action places
     * 
     */
    private void classifyPlaces() {
        List<List<Integer>> pInvariants = invariants.getPInvariants();
        int numInvariants = pInvariants.size();

        // Classify squares according to how many invariants they contain
        for (int p = 0; p < numPlaces; p++) {
            int count = 0;
            for (List<Integer> inv : pInvariants) {
                if (inv.get(p) > 0) count++;
            }

            if (count == 1) {
                possibleRecourse.add(p); // possible resource, idle or restriction
            } else if (count > 1) {
                actionPlaces.add(p); // action square (participates in several invariants)
            }
        }

        // Determine resource, idle, or restriction places within potential candidates
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
                // Multiple candidates: identifying the idle square
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
     * Determines if a place is idle according to the criteria:
     * 1. Initial marking > 0
     * 2. Exactly 1 input transition
     * 3. Exactly 1 output transition
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
     * Useful methods for printing results of place classification
     */
    public void printClassification() {
        System.out.println("\n=================================");
        System.out.println("Place Classification \n");
        System.out.println("Resources, restrictions and idle places: " + resourcePlaces);
        System.out.println("Action places: " + actionPlaces +"\n");
    }

    /**
     * Prints the places that are part of each T-Invariant.
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
     * Prints the action places of each T-Invariant.
     */
    public String logPAofIT() {
        String log = "\n=================================";
        log += "\nSet of action places associated with each Transition Invariant (PA of TI) \n";
        
        // Validar que PAofIT esté calculado
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
     * Print sets of action places associated with all Transition Invariants
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

    public Set<Integer> getActionPlaces() {
        return actionPlaces;
    }

    /**
     * Calculates the action places for each T-Invariant.
     * This method should be called AFTER classifyPlaces() has been executed.
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