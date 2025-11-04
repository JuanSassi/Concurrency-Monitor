import java.util.*;

/**
 * Main class for the Concurrency Monitor simulation.
 * Provides a clean, simple interface for running the Petri net simulation.
 */
public class Main {
    public static void main(String[] args) {
        ThreadAllocator allocator = new ThreadAllocator();
        
        /*ThreadsHandler handler = new ThreadsHandler();
        
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
        }*/
    }
}