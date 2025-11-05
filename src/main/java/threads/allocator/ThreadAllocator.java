import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class ThreadAllocator {
    private int[][] Pre;
    private int[][] Post;
    private int[] m0;
    private int[][] W;
    private Invariants invariants;
    private ClassificationOfPlaces analyzer;
    private ReachabilityTree tree;
    private Responsibilities responsibilities;
    private Log algorithmsLog;
    private Log treeLog;
    private Log treePerSegmentLog;
    private List<List<Integer>> segments;

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

        // 4. 
        log += analyzer.logPA();
        log += tree.logMaxThreads();
        return log;
    }

    private String algorithm2(){
        // Algorithm for determining thread responsibility (4.2)
        String log = "\n╔═════════════════════════════════════════════════════════╗" +
                     "\n║  ALGORITHM FOR DETERMINING THREAD RESPONSIBILITY (4.2)  ║" + 
                     "\n╚═════════════════════════════════════════════════════════╝\n";

        log += responsibilities.logAnalysis();
        return log;
    }

    private String algorithm3() {
        // Algorithm for determining maximum threads per segment (4.3)
        String log = "\n╔═══════════════════════════════════════════════════════════════╗" +
                     "\n║  ALGORITHM FOR DETERMINING MAXIMOM THREADS PER SEGMENT (4.3)  ║" +
                     "\n╚═══════════════════════════════════════════════════════════════╝";
        
        segments = responsibilities.getSegments();
        for(List<Integer> segment : segments){
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            log += tree.logThreadsPerSegment(segment, segmentPlaces);
        }      
        return log;
    }

    private List<Integer> getPlacesFromSegment(List<Integer> segment) {
        Set<Integer> places = new HashSet<>();
        Set<Integer> actionPlaces = analyzer.getActionPlaces();
        List<Integer> forkPlaces = responsibilities.getForkPlaces(); // Necesitas esto
        List<Integer> joinPlaces = responsibilities.getJoinPlaces(); // Y esto

        for (Integer transition : segment) {
            // Solo agregar plazas de SALIDA (Post) que NO sean fork ni join
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

    private String getLogMarkings(){
        return tree.logMarkings();
    }

    private String getLogSegments(){
        // Algorithm for determining maximum threads per segment (4.3)
        String log = "";
        for(List<Integer> segment : segments){
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            log += tree.logSegment(segment, segmentPlaces);
        }      
        return log;
    }

    public void logAllocation(){
        algorithmsLog.write(algorithm1());
        algorithmsLog.write(algorithm2());
        algorithmsLog.write(algorithm3());
        treeLog.write(getLogMarkings());
        treePerSegmentLog.write(getLogSegments());
    }

    public int maxActiveThreads() {
        return tree.getMaxNumThreads();
    }

    public List<List<Integer>> getSegments() {
        return segments;
    }

    public List<Integer> getThreadsPerSegment() {
        List<Integer> maxThreadsPerSegment = new ArrayList<>();
        for(List<Integer> segment : segments){
            List<Integer> segmentPlaces = getPlacesFromSegment(segment);
            maxThreadsPerSegment.add(tree.calculateMaxThreadsInSegment(segmentPlaces));
        }
        return maxThreadsPerSegment;
    }
}