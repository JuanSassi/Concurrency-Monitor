/**
 * Main class for the Concurrency Monitor simulation.
 * Provides a clean, simple interface for running the Petri net simulation.
 */
/*public class Main {
    public static void main(String[] args) {
        ThreadsHandler handler = new ThreadsHandler();
        
        try {
            // Start simulation
            handler.start();
            
            // Wait for completion
            handler.waitForCompletion();
            
            // Stop threads cleanly
            handler.stop();
            
            // Print final statistics
            handler.printStatistics();
            
        } catch (Exception e) {
            System.err.println("Error during simulation execution: " + e.getMessage());
            e.printStackTrace();
            
            // Ensure threads are stopped even on error
            handler.stop();
        }
    }
}*/

import java.util.*;

/**
 * Ejemplo completo de uso del análisis de hilos en Redes de Petri
 * Implementa los algoritmos del paper para la red de Huang (Fig. 2)
 */
public class Main {
    
    public static void main(String[] args) {
        int[][] Pre = {
           //T1 T2 T3 T4 T5 T6 T7 T8 T9 T10
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // P1
            {0, 1, 1, 0, 0, 0, 0, 0, 0, 0},  // P2
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},  // P3
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},  // P4
            {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},  // P5
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},  // P6 (recurso)
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},  // P7 (recurso)
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},  // P8 (idle)
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},  // P9
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},  // P10
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},  // P11
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},  // P12 (recurso)
            {1, 0, 0, 1, 1, 0, 1, 0, 1, 0},  // P13 (recurso compartido)
            {1, 0, 0, 0, 0, 0, 1, 0, 0, 0}   // P14 (restricción)
        };
        int[][] Post = {
         // T1 T2 T3 T4 T5 T6 T7 T8 T9 T10
            {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},  // P1
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // P2
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},  // P3
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},  // P4
            {0, 0, 0, 1, 1, 0, 0, 0, 0, 0},  // P5
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},  // P6 (recurso)
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},  // P7 (recurso)
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},  // P8 (idle)
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},  // P9
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},  // P10
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},  // P11
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},  // P12 (recurso)
            {0, 1, 1, 0, 0, 1, 0, 1, 0, 1},  // P13 (recurso compartido)
            {0, 1, 1, 0, 0, 0, 0, 0, 1, 0}   // P14 (restricción)
        };
        int[] m0 = new int[]{2, 0, 1, 0, 0, 0, 1, 2, 0, 0, 0, 1, 1, 1}; //*/

        /*int[][] Pre = ConfigLoader.getPreMatrix();
        int[][] Post = ConfigLoader.getPostMatrix();
        int[] m0 = ConfigLoader.getInitialMarkingVector(); //*/
        int[][] W = MatrixUtils.subtract(Post, Pre);

        // Algorithm for determining maximum simultaneous active threads (4.1)
        System.out.println("╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  ALGORITHM FOR DETERMINING MAXIMUM SIMULTANEOUS ACTIVE THREADS (4.1)  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
        
        // 1. Obtain the transition invariants of the PN
        Invariants invariants = new Invariants(W);
        invariants.printTransitionsOfTI();

        // 2. Obtain the set of places associated with the T-invariant under analysis
        ClassificationOfPlaces analyzer = new ClassificationOfPlaces(Pre, Post, m0, invariants);
        analyzer.printPIofIT();

        // 3. Determine the action-related places of each T-invariant
        analyzer.classifyPlaces();
        analyzer.printPAofIT();

        // 4. 
        analyzer.printPA();

        /*ReachabilityTree tree = new ReachabilityTree(Pre, Post, m0, analyzer.getActionPlaces());
        ReachabilityTree.MarkingNode root = tree.buildReachabilityGraph();
        tree.printGraph(root);*/
        

        // 5. 

        // Algorithm for determining thread responsibility (4.2)
        System.out.println("╔═════════════════════════════════════════════════════════╗");
        System.out.println("║  ALGORITHM FOR DETERMINING THREAD RESPONSIBILITY (4.2)  ║");
        System.out.println("╚═════════════════════════════════════════════════════════╝");

        // Algorithm for determining maximum threads per segment (4.3)
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ALGORITHM FOR DETERMINING MAXIMOM THREADS PER SEGMENT (4.3)  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        

    }
}