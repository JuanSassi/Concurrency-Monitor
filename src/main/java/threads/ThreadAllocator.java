public class ThreadAllocator {
    private int[][] Pre;
    private int[][] Post;
    private int[] m0;
    private int[][] W;
    private Invariants invariants;
    private ClassificationOfPlaces analyzer;
    private ReachabilityTree tree;
    private Responsibilities responsibilities;

    public ThreadAllocator(){
        Pre = PetrinetLoader.getPreMatrix();
        Post = PetrinetLoader.getPostMatrix();
        m0 = PetrinetLoader.getInitialMarkingVector();
        W = MatrixUtils.subtract(Post, Pre);
        invariants = new Invariants(W);
        analyzer = new ClassificationOfPlaces(Pre, Post, m0, invariants);
        tree = new ReachabilityTree(analyzer.getActionPlaces());
        responsibilities = new Responsibilities(Pre, Post, invariants.getTInvariants(), analyzer.getActionPlaces());
        
        algorithm1();
        algorithm2();
        algorithm3();
    }

    public void algorithm1() {
        // Algorithm for determining maximum simultaneous active threads (4.1)
        System.out.println("╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  ALGORITHM FOR DETERMINING MAXIMUM SIMULTANEOUS ACTIVE THREADS (4.1)  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
        
        // 1. Obtain the transition invariants of the PN
        invariants.printTransitionsOfTI();

        // 2. Obtain the set of places associated with the T-invariant under analysis
        analyzer.printPIofIT();

        // 3. Determine the action-related places of each T-invariant
        analyzer.printPAofIT();

        // 4. 
        analyzer.printPA();
        tree.printMarkings();
    }

    public void algorithm2(){
        // Algorithm for determining thread responsibility (4.2)
        System.out.println("╔═════════════════════════════════════════════════════════╗");
        System.out.println("║  ALGORITHM FOR DETERMINING THREAD RESPONSIBILITY (4.2)  ║");
        System.out.println("╚═════════════════════════════════════════════════════════╝");

        
        responsibilities.printAnalysis();
    }

    public void algorithm3(){
        // Algorithm for determining maximum threads per segment (4.3)
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ALGORITHM FOR DETERMINING MAXIMOM THREADS PER SEGMENT (4.3)  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }
}