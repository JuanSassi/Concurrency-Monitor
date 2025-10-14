/**
 * Handles the lifecycle of all simulation threads.
 * Provides clean separation between thread management and main application logic.
 */
public class ThreadsHandler {
    private final Thread[] entryProcesses;
    private final Thread reservationProcess1;
    private final Thread reservationProcess2;
    private final Thread confirmationOrCancelationProcess;
    private final int SLEEP_INTERVAL_MS = 1000;
    
    public ThreadsHandler() {
        // Initialize all threads
        final int assistantsPosition = 4; // COMO DEDUZCO CUANTOS HILOS??
        this.entryProcesses = new Thread[ConfigLoader.getInitialMarkingVector()[assistantsPosition]];
        
        // Create entry processes
        for (int i = 0; i < entryProcesses.length; i++) {
            entryProcesses[i] = new Thread(new Process(Sequences.entryProcess, true));
        }
        
        // Create reservation and confirmation processes
        this.reservationProcess1 = new Thread(new Process(Sequences.reservationProcessAbove, true));
        this.reservationProcess2 = new Thread(new Process(Sequences.reservationProcessBelow, true));
        this.confirmationOrCancelationProcess = new Thread(new Process(Sequences.confirmationProcess, false));
    }
    
    /**
     * Starts all simulation threads
     */
    public void start() {
        System.out.println("Starting simulation threads...");
        
        // Start all entry processes
        for (Thread thread : entryProcesses) {
            thread.start();
        }
        
        // Start reservation and confirmation processes
        reservationProcess1.start();
        reservationProcess2.start();
        confirmationOrCancelationProcess.start();
    }
    
    /**
     * Monitors execution until completion condition is met
     */
    public void waitForCompletion() {
        while (!Log.endExecution()) {
            try {
                Thread.sleep(SLEEP_INTERVAL_MS);
            } catch (InterruptedException e) {
                System.out.println("Main thread was interrupted!");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Stops all threads and performs cleanup
     */
    public void stop() {
        System.out.println("Stopping simulation threads...");
        
        // Interrupt all threads
        for (Thread thread : entryProcesses) {
            thread.interrupt();
        }
        reservationProcess1.interrupt();
        reservationProcess2.interrupt();
        confirmationOrCancelationProcess.interrupt();
        
        // Wait for threads to finish cleanly
        waitForThreadsToFinish();
        
        // Report thread status
        reportThreadStatus();
    }
    
    /**
     * Waits for threads to finish cleanly
     */
    private void waitForThreadsToFinish() {
        System.out.println("Waiting for threads to finish...");
        try {
            Thread.sleep(SLEEP_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Reports the status of all threads
     */
    private void reportThreadStatus() {
        System.out.println("Thread status report:");
        
        // Check entry processes
        for (Thread thread : entryProcesses) {
            reportSingleThreadStatus(thread);
        }
        
        // Check reservation and confirmation processes
        reportSingleThreadStatus(reservationProcess1);
        reportSingleThreadStatus(reservationProcess2);
        reportSingleThreadStatus(confirmationOrCancelationProcess);
    }
    
    /**
     * Reports status of a single thread
     */
    private void reportSingleThreadStatus(Thread thread) {
        if (thread.isAlive()) {
            System.out.println("Hilo vivo: " + thread.getName());
        } else {
            System.out.println("Hilo finalizo: " + thread.getName());
        }
    }
    
    /**
     * Prints simulation statistics
     */
    public void printStatistics() {
        System.out.println("\n" + "=".repeat(50)); // POR QUE 50??
        System.out.println("SIMULATION FINISHED - GENERATING REPORTS");
        System.out.println("=".repeat(50));
        Log.printDetailedStatistics();
        Log.exportDataForAnalysis();
        System.out.println("\nSimulation ended successfully!");
    }
}