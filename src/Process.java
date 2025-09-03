/**
 * Represents a process that executes a sequence of Petri net transitions in a separate thread.
 * It can operate in single-sequence mode or allow dynamic sequence selection based on the current 
 * policy (such as a travel agent process that approves or rejects reservations).
 * 
 * @author Sassi Juan Ignacio
 */
public class Process implements Runnable {
    
    /** Volatile flag to control thread execution in a thread-safe manner */
    private volatile boolean running;
    
    /** Indicates whether to run only the initial sequence or allow dynamic selection */
    private boolean isSingleSequence;
    
    /** Array of Petri net transitions to be executed in sequence */
    private int[] sequence;
    
    /** Singleton monitor instance for synchronization */
    private Monitor monitor;
    
    /** Flag indicating whether the current transition was triggered successfully */
    private boolean fired;

    /**
     * Constructor that initializes a process with the specified sequence of transitions.
     * 
     * @param sequence Array of Petri net transitions to be executed in sequence
     * @param isSingleSequence true if only this sequence should be executed, 
     *                        false if Policy can select dynamically
     */
    public Process(int[] sequence, boolean isSingleSequence) {
        this.isSingleSequence = isSingleSequence;
        this.running = true;
        this.sequence = sequence;
        this.monitor = Monitor.getInstance();
        this.fired = false;
    }

    /**
      * Main thread execution method. Executes transitions in sequence continuously until 
      * the main thread calls the stop() method. If isSingleSequence is false, allows Policy 
      * to select a new sequence on each iteration of the main loop (cancellation sequence 
      * or reservation confirmation sequence). For each transition in the sequence:
      *     - Attempts to trigger it via the Monitor
      *     - Retries until the transition triggers successfully
      *     - Prints a trigger confirmation
     */
    @Override
    public void run() {
        while (running) {
            if (!isSingleSequence) {
                Policy.selectSequence(sequence);
            }
            
            for (int i = 0; i < sequence.length; i++) {
                fired = false;
                
                while (!fired) {
                    fired = Monitor.fireTransition(sequence[i]);
                }
                
                System.out.println(Thread.currentThread().getName() + 
                                 " disparo la Transicion " + sequence[i] + "\n");
            }
        }
    }

    /**
     * Stops the process execution safely.
     * Sets the running flag to false, which will cause the main loop to terminate 
     * on the next iteration.
     */
    public void stop() {
        running = false;
    }
}