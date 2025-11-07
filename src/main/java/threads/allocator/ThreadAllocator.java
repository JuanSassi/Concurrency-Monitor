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
 * separate files for detailed examination.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * ThreadAllocator allocator = new ThreadAllocator();
 * allocator.logAllocation();
 * int maxThreads = allocator.maxActiveThreads();
 * List&lt;Integer&gt; threadsPerSegment = allocator.getThreadsPerSegment();
 * </pre>
 * 
 * @see Invariants
 * @see ClassificationOfPlaces
 * @see ReachabilityTree
 * @see Responsibilities
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
    
    /** Log file for algorithm results */
    private Log algorithmsLog;
    
    /** Log file for complete reachability tree */
    private Log treeLog;
    
    /** Log file for segment-specific reachability information */
    private Log treePerSegmentLog;
    
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
    public ThreadAllocator(){
        this.Pre = PetrinetLoader.getPreMatrix();
        this.Post = PetrinetLoader.getPostMatrix();
        this.m0 = PetrinetLoader.getInitialMarkingVector();
        this.W = Matrix.subtract(Post, Pre);
        
        this.invariants = new Invariants(W);
        this.analyzer = new ClassificationOfPlaces(Pre, Post, W, m0, invariants);
        this.tree = new ReachabilityTree(analyzer.getActionPlaces());
        this.responsibilities = new Responsibilities(Pre, Post, invariants.getTInvariants(), analyzer.getActionPlaces());

        this.algorithmsLog = new Log("algorithms");
        this.treeLog = new Log("reachabilityTree");
        this.treePerSegmentLog = new Log("treePerSegment");
        this.segments = new ArrayList<>();
    }

    /**
     * Executes Algorithm 4.1 for determining maximum simultaneous active threads.
     * 
     * <p>This algorithm performs the following steps:</p>
     * <ol>
     *   <li>Obtains the transition invariants of the Petri net</li>
     *   <li>Obtains the set of places associated with each T-invariant</li>
     *   <li>Determines the action-related places of each T-invariant</li>
     *   <li>Computes the union of all action places</li>
     *   <li>Analyzes the reachability tree to find the maximum concurrent tokens</li>
     * </ol>
     * 
     * @return a formatted string containing the algorithm execution log
     */
    private String algorithm1() {
        // Algorithm for determining maximum simultaneous active threads (4.1)
        String log = "\n╔═══════════════════════════════════════════════════════════════════════╗\n" +
                       "║  ALGORITHM FOR DETERMINING MAXIMUM SIMULTANEOUS ACTIVE THREADS (4.1)  ║\n" +
                       "╚═══════════════════════════════════════════════════════════════════════╝";

        // 1. Obtain the transition invariants of the PN
        log += invariants.logTransitionsOfTI();
        
        // 2. Obtain the set of places associated with the T-invariant under analysis
        log += analyzer.logPIofIT();

        // 3. Determine the action-related places of each T-invariant
        log += analyzer.logPAofIT();

        // 4. Compute the union of action places and analyze maximum threads
        log += analyzer.logPA();
        log += tree.logMaxThreads();
        return log;
    }

    /**
     * Executes Algorithm 4.2 for determining thread responsibility and segmentation.
     * 
     * <p>This algorithm analyzes the Petri net structure to identify:</p>
     * <ul>
     *   <li>Sequential and parallel execution segments</li>
     *   <li>Fork places (where parallel execution begins)</li>
     *   <li>Join places (where parallel execution converges)</li>
     *   <li>Responsibility boundaries for thread management</li>
     * </ul>
     * 
     * @return a formatted string containing the algorithm execution log with
     *         identified segments, forks, and joins
     */
    private String algorithm2(){
        // Algorithm for determining thread responsibility (4.2)
        String log = "\n╔═════════════════════════════════════════════════════════╗" +
                     "\n║  ALGORITHM FOR DETERMINING THREAD RESPONSIBILITY (4.2)  ║" + 
                     "\n╚═════════════════════════════════════════════════════════╝\n";

        log += responsibilities.logAnalysis();
        return log;
    }

    /**
     * Executes Algorithm 4.3 for determining maximum threads per segment.
     * 
     * <p>For each segment identified in Algorithm 4.2, this algorithm:</p>
     * <ol>
     *   <li>Identifies the action places within the segment</li>
     *   <li>Analyzes the reachability tree for those specific places</li>
     *   <li>Computes the maximum concurrent tokens in the segment</li>
     * </ol>
     * 
     * <p>This provides fine-grained thread allocation information for each
     * execution segment of the system.</p>
     * 
     * @return a formatted string containing the algorithm execution log with
     *         maximum threads for each segment
     */
    private String algorithm3() {
        // Algorithm for determining maximum threads per segment (4.3)
        String log = "\n╔═════════════════════════════════════════════════════════════════╗" +
                     "\n║  ALGORITHM FOR DETERMINING MAXIMOM THREADS PER SEGMENT (4.3)  ║" +
                     "\n╚═════════════════════════════════════════════════════════════════╝";
        
        segments = responsibilities.getSegments();
        for(List<Integer> segment : segments){
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            log += tree.logThreadsPerSegment(segment, segmentPlaces);
        }      
        return log;
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
     * <p>This filtering ensures that only internal action places of the segment
     * are considered when computing thread requirements.</p>
     * 
     * @param segment list of transition indices that form the segment
     * @return sorted list of place indices that are action places within the segment,
     *         excluding forks and joins
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
     * Retrieves the complete reachability tree log showing all reachable markings.
     * 
     * @return formatted string with the complete marking table for action places
     */
    private String getLogMarkings(){
        return tree.logMarkings();
    }

    /**
     * Retrieves detailed segment logs showing reachable markings for each segment.
     * 
     * <p>For each segment, generates a filtered view of the reachability tree
     * showing only the places relevant to that segment.</p>
     * 
     * @return formatted string with marking tables for all segments
     */
    private String getLogSegments(){
        String log = "";
        for(List<Integer> segment : segments){
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            log += tree.logSegment(segment, segmentPlaces);
        }      
        return log;
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
    public void logAllocation(){
        algorithmsLog.write(algorithm1());
        algorithmsLog.write(algorithm2());
        algorithmsLog.write(algorithm3());
        treeLog.write(getLogMarkings());
        treePerSegmentLog.write(getLogSegments());
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
     * <p>This information is useful for allocating thread pools or semaphores
     * specific to each segment of the system.</p>
     * 
     * @return list where each element is the maximum thread count for the
     *         corresponding segment
     */
    public List<Integer> getThreadsPerSegment() {
        List<Integer> maxThreadsPerSegment = new ArrayList<>();
        for(List<Integer> segment : segments){
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            maxThreadsPerSegment.add(tree.calculateMaxThreadsInSegment(segmentPlaces));
        }
        return maxThreadsPerSegment;
    }
}