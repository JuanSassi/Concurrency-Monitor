import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Monitor implements MonitorInterface {
    // Singleton pattern
    private static final Monitor monitor = new Monitor();
    private final PetriNet petriNet;

    // Lock principal
    private final Lock lock = new ReentrantLock(false);

    private final Condition[] conditionTransition;

    // Private constructor - Singleton pattern
    private Monitor() {
        this.petriNet = PetriNet.getInstance();
        this.conditionTransition = new Condition[petriNet.getNumTransitions()];
        for (int i = 0; i < petriNet.getNumTransitions(); i++) {
            this.conditionTransition[i] = lock.newCondition();
        }
    }

    // Public method to get the unique instance - Singleton pattern
    public static Monitor getInstance() {
        return monitor;
    }

    @Override
    public boolean fireTransition(int transition) {
        lock.lock();
        try {
            // Wait until the transition becomes enabled
            while (!petriNet.transitionEnabled(transition)) {
                try {
                    conditionTransition[transition].await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            long startTime = System.nanoTime(); // new

            // Now the transition is enabled - fire it
            if(petriNet.isTemporary(transition)){
                petriNet.consumeTokens(transition);
                //Log.addTransitionFiring(transition); //new
                lock.unlock();

                long sleepStart = System.nanoTime(); //new

                try {
                    SleepUtilities.switchNap(transition);
                } finally {
                    lock.lock();
                    long sleepEnd = System.nanoTime(); //new
                    long sleepDuration = (sleepEnd - sleepStart) / 1_000_000; //new
                    //Log.addTemporaryTransitionDuration(transition, sleepDuration); //new

                    petriNet.produceTokens(transition);
                }
            } else {
                petriNet.fire(transition);
                //Log.addTransitionFiring(transition);
            }
            System.out.print("Fired transition " + transition + " - Marcación actual: [ ");
            int[] marking = petriNet.getMarking();
            for (int i = 0; i < marking.length; i++) {
                System.out.print(marking[i] + " ");
            }
            System.out.println("]");

            // After firing, notify waiting threads based on priority
            schedule();
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void schedule() {
        /*if (Log.endExecution()) {
            endSimulation();
            return;
        }*/
        if(petriNet.transitionEnabled(2) && petriNet.transitionEnabled(3)) {
            double p;
            p = Policy.getAgentPriority();
            if (Math.random() < p) {
                conditionTransition[2].signal();
            } else {
                conditionTransition[3].signal();
            }
        } else {
            for (int i = 0; i < petriNet.getNumTransitions(); i++) {
                if(petriNet.transitionEnabled(i)) {
                    conditionTransition[i].signal();
                }
            }
        }
    }

    /**
     * Método para señalar el fin de la simulación y despertar todos los hilos
     */
    private void endSimulation() {
        // Despertar todos los hilos que están esperando
        for (Condition condition : conditionTransition) {
            condition.signalAll();
        }
    }
}