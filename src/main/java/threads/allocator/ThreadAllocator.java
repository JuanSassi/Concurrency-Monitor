import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * Main class for thread allocation analysis in Petri nets.
 * This class implements three algorithms for determining thread allocation requirements:
 * 
 * <ul>
 *   <li>Algorithm 4.1: Maximum simultaneous active threads</li>
 *   <li>Algorithm 4.2: Thread responsibility (segmentation)</li>
 *   <li>Algorithm 4.3: Maximum threads per segment</li>
 * </ul>
 * 
 * <p>The class orchestrates the analysis by utilizing invariants, place classification,
 * reachability tree construction, and responsibility analysis. Results are logged to
 * separate files for detailed examination using specialized log classes.</p>
 * 
 * @see Invariants
 * @see ClassificationOfPlaces
 * @see ReachabilityTree
 * @see Responsibilities
 * @see AlgorithmsLog
 * @see ReachabilityTreeLog
 * @see TreePerSegmentLog
 * 
 * @author Sassi Juan Ignacio
 */
public class ThreadAllocator {
    /** Pre-incidence matrix of the Petri net */
    private int[][] Pre;
    
    /** Post-incidence matrix of the Petri net */
    private int[][] Post;
    
    /** Initial marking vector */
    private int[] m0;
    
    /** Incidence matrix (W = Post - Pre) */
    private int[][] W;
    
    /** Invariants calculator for the Petri net */
    private Invariants invariants;
    
    /** Place classification analyzer */
    private ClassificationOfPlaces analyzer;
    
    /** Reachability tree for action places */
    private ReachabilityTree tree;
    
    /** Responsibility analyzer for thread segmentation */
    private Responsibilities responsibilities;
    
    /** Specialized log for algorithm results */
    private AlgorithmsLog algorithmsLog;
    
    /** Specialized log for complete reachability tree */
    private ReachabilityTreeLog treeLog;
    
    /** Specialized log for segment-specific reachability information */
    private TreePerSegmentLog treePerSegmentLog;
    
    /** List of transition segments identified by responsibility analysis */
    private List<List<Integer>> segments;

    /**
     * Constructs a ThreadAllocator and initializes all analysis components.
     * Loads the Petri net structure from configuration, computes invariants,
     * classifies places, builds the reachability tree, and analyzes responsibilities.
     * 
     * <p>This constructor performs all necessary preprocessing for the three
     * allocation algorithms.</p>
     */
    public ThreadAllocator() {
        this.Pre = PetrinetLoader.getPreMatrix();
        this.Post = PetrinetLoader.getPostMatrix();
        this.m0 = PetrinetLoader.getInitialMarkingVector();
        this.W = Matrix.subtract(Post, Pre);
        
        this.invariants = new Invariants(W);
        this.analyzer = new ClassificationOfPlaces(Pre, Post, W, m0, invariants);
        this.tree = new ReachabilityTree(analyzer.getActionPlaces());
        this.responsibilities = new Responsibilities(Pre, Post, invariants.getTInvariants(), analyzer.getActionPlaces());

        this.algorithmsLog = new AlgorithmsLog();
        this.treeLog = new ReachabilityTreeLog();
        this.treePerSegmentLog = new TreePerSegmentLog();
        this.segments = responsibilities.getSegments();
    }

    /**
     * Executes all three allocation algorithms and writes results to log files.
     * 
     * <p>Generates three log files:</p>
     * <ul>
     *   <li>algorithms.log - Results of all three algorithms</li>
     *   <li>reachabilityTree.log - Complete reachability tree</li>
     *   <li>treePerSegment.log - Segment-specific reachability information</li>
     * </ul>
     * 
     * <p>This method should be called after constructing the ThreadAllocator
     * to perform the complete analysis and generate output files.</p>
     */
    public void logAllocation() {
        boolean fullPrint = ConfigLoader.getFullprint();
        
        // Algorithm 4.1: Maximum simultaneous active threads
        algorithmsLog.logAlgorithm1(
            invariants.getTInvariants(),
            getPIofIT(),
            analyzer.getPAofIT(),
            analyzer.getActionPlaces(),
            tree.getMaxNumThreads(),
            tree.getNumReachableMarkings()
        );
        
        // Algorithm 4.2: Thread responsibility
        algorithmsLog.logAlgorithm2(
            segments,
            responsibilities.getForkPlaces(),
            responsibilities.getJoinPlaces()
        );
        
        // Algorithm 4.3: Maximum threads per segment
        List<List<Integer>> allSegmentPlaces = new ArrayList<>();
        List<Integer> maxThreadsPerSegment = new ArrayList<>();
        
        for (List<Integer> segment : segments) {
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            allSegmentPlaces.add(segmentPlaces);
            maxThreadsPerSegment.add(tree.calculateMaxThreadsInSegment(segmentPlaces));
        }
        
        algorithmsLog.logAlgorithm3(segments, allSegmentPlaces, maxThreadsPerSegment);
        
        // Complete reachability tree
        treeLog.logMarkings(
            tree.getReachableMarkings(),
            analyzer.getActionPlaces(),
            tree.getMaxNumThreads(),
            fullPrint
        );
        
        // Segment-specific reachability trees
        treePerSegmentLog.logAllSegments(
            segments,
            allSegmentPlaces,
            tree.getReachableMarkings(),
            analyzer.getActionPlaces(),
            maxThreadsPerSegment,
            fullPrint
        );
    }

    /**
     * Gets the set of places associated with each T-invariant.
     * A place is associated with a T-invariant if it's connected to any
     * transition in that invariant.
     * 
     * @return list of place sets, one per T-invariant
     */
    private List<List<Integer>> getPIofIT() {
        List<List<Integer>> result = new ArrayList<>();
        List<List<Integer>> tInvariants = invariants.getTInvariants();
        int numPlaces = Pre.length;
        int numTransitions = Pre[0].length;
        
        for (List<Integer> tInv : tInvariants) {
            Set<Integer> involvedPlaces = new HashSet<>();
            
            // For each transition in the T-invariant
            for (int t = 0; t < numTransitions; t++) {
                if (tInv.get(t) > 0) {
                    // Add all places connected to this transition
                    for (int p = 0; p < numPlaces; p++) {
                        if (Pre[p][t] > 0 || Post[p][t] > 0) {
                            involvedPlaces.add(p);
                        }
                    }
                }
            }
            
            List<Integer> sortedPlaces = new ArrayList<>(involvedPlaces);
            Collections.sort(sortedPlaces);
            result.add(sortedPlaces);
        }
        
        return result;
    }

    /**
     * Extracts action places associated with a given segment of transitions.
     * 
     * <p>The method identifies places that:</p>
     * <ul>
     *   <li>Receive tokens from transitions in the segment (output places)</li>
     *   <li>Are classified as action places</li>
     *   <li>Are NOT fork or join places (to avoid boundary effects)</li>
     * </ul>
     * 
     * @param segment list of transition indices that form the segment
     * @return sorted list of place indices that are action places within the segment
     */
    private List<Integer> getPlacesFromSegment(List<Integer> segment) {
        Set<Integer> places = new HashSet<>();
        Set<Integer> actionPlaces = analyzer.getActionPlaces();
        List<Integer> forkPlaces = responsibilities.getForkPlaces();
        List<Integer> joinPlaces = responsibilities.getJoinPlaces();

        for (Integer transition : segment) {
            // Only add OUTPUT places (Post) that are NOT fork or join
            for (int p = 0; p < Post.length; p++) {
                if (Post[p][transition] > 0 && 
                    actionPlaces.contains(p) &&
                    !forkPlaces.contains(p) &&
                    !joinPlaces.contains(p)) {
                    places.add(p);
                }
            }
        }
        
        List<Integer> segmentPlaces = new ArrayList<>(places);
        Collections.sort(segmentPlaces);
        return segmentPlaces;
    }

    /**
     * Gets the maximum number of threads that can be simultaneously active.
     * This value is computed by Algorithm 4.1.
     * 
     * @return the maximum number of concurrent threads across all action places
     */
    public int maxActiveThreads() {
        return tree.getMaxNumThreads();
    }

    /**
     * Gets the list of identified execution segments.
     * Each segment is a list of transition indices that should be managed together.
     * 
     * @return list of segments, where each segment is a list of transition indices
     */
    public List<List<Integer>> getSegments() {
        return segments;
    }

    /**
     * Gets the maximum number of threads required for each segment.
     * The list order corresponds to the order of segments returned by getSegments().
     * 
     * @return list where each element is the maximum thread count for the corresponding segment
     */
    public List<Integer> getThreadsPerSegment() {
        List<Integer> maxThreadsPerSegment = new ArrayList<>();
        for (List<Integer> segment : segments) {
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            maxThreadsPerSegment.add(tree.calculateMaxThreadsInSegment(segmentPlaces));
        }
        return maxThreadsPerSegment;
    }
}