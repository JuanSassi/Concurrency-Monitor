/**
 * Represents a process that executes a sequence of Petri net transitions in a separate thread.
 * It can operate in single-sequence mode or allow dynamic sequence selection based on the current 
 * policy (such as a travel agent process that approves or rejects reservations).
 * 
 * @author Sassi Juan Ignacio
 */
public class Process implements Runnable {    
    /** Indicates whether to run only the initial sequence or allow dynamic selection */
    private boolean isSingleSequence;
    
    /** Array of Petri net transitions to be executed in sequence */
    private int[] sequence;
    
    /** Singleton monitor instance for synchronization */
    private Monitor monitor;

    /**
     * Constructor that initializes a process with the specified sequence of transitions.
     * 
     * @param sequence Array of Petri net transitions to be executed in sequence
     * @param isSingleSequence true if only this sequence should be executed, 
     *                        false if Policy can select dynamically
     */
    public Process(int[] sequence, boolean isSingleSequence) {
        this.isSingleSequence = isSingleSequence;
        this.sequence = sequence;
        this.monitor = Monitor.getInstance();
    }

    /**
      * Run is the main thread's execution method. It executes transitions in sequence continuously 
      * until the maximum number of invariants is completed or it is interrupted by the main thread. 
      * If isSingleSequence is false, allows Policy to select a new sequence on each iteration of 
      * the main loop (cancellation sequence or reservation confirmation sequence). 
      * For each transition in the sequence:
      *     - Attempts to fired it via the Monitor
      *     - Retries until the transition fired successfully
      *     - Prints a fired confirmation
     */
    @Override
    public void run() {
        boolean shouldTerminate = false;
        try{
            while (!Log.endExecution() && !Thread.currentThread().isInterrupted()) {
                if (!isSingleSequence) {
                    sequence = Policy.selectSequence();
                }

                for (int i = 0; i < sequence.length && !shouldTerminate; i++) {
                    while (!monitor.fireTransition(sequence[i])) {
                        if (Log.endExecution() || Thread.currentThread().isInterrupted()) {
                            shouldTerminate = true;
                            break;
                        }
                    }
                }
                if (shouldTerminate) break;
            }
        } catch (Exception e) {
            // Handle any unexpected exceptions during execution
            System.err.println("Thread " + Thread.currentThread().getName() + 
                             " encountered exception: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            // Ensure proper cleanup and logging
            System.out.println("Thread " + Thread.currentThread().getName() + 
                             " finished execution cleanly.");
            
            // Restore interrupt status if it was set
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Thread " + Thread.currentThread().getName() + 
                                 " was interrupted during execution.");
            }
        }
    }
}