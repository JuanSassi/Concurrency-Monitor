/*import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class Monitor implements MonitorInterface {
    private static final boolean FIFO = true;
    private static final int CLOSED = 0;
    private static final int ONE_PERMIT = 1;

    private static Monitor INSTANCE;

    private final int maxGlobal;
    private final int[] maxForSegments;
    private final int numSegments;

    private final PetriNet petriNet;

    private final Semaphore globalSlots;
    private final Semaphore[] segmentSlots;

    private final Semaphore mutexQueues = new Semaphore(ONE_PERMIT, FIFO);
    private final Semaphore mutexPetrinet = new Semaphore(ONE_PERMIT, FIFO);

    private final LinkedList<QueueNode> enterQueue = new LinkedList<>();
    private final LinkedList<QueueNode> waitQueue = new LinkedList<>();
    private final LinkedList<QueueNode> courtesyQueue = new LinkedList<>();

    private Monitor(int[] maxForSegments, int maxGlobal) {
        this.maxGlobal = maxGlobal;
        this.maxForSegments = maxForSegments;
        this.numSegments = maxForSegments.length;

        this.globalSlots = new Semaphore(maxGlobal, FIFO);
        this.segmentSlots = new Semaphore[numSegments];

        this.petriNet = PetriNet.getInstance();
        petriNet.reset();

        for (int s = 0; s < numSegments; s++) {
            segmentSlots[s] = new Semaphore(maxForSegments[s], FIFO);
        }
    }

    public static synchronized Monitor getInstance(int[] maxForSegments, int maxGlobal) {
        if (INSTANCE == null) {
            INSTANCE = new Monitor(maxForSegments, maxGlobal);
        }
        return INSTANCE;
    }

    public static Monitor getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                "Monitor no inicializado. Llamar primero a getInstance(maxForSegments, maxGlobal)."
            );
        }
        return INSTANCE;
    }

    public boolean fireTransition(int transition) {
        QueueNode node = new QueueNode(transition, ((MyThread) Thread.currentThread()).getSegment());
        mutexQueues.acquire();
        enterQueue.addLast(node);
        mutexQueues.release();
        node.getSemaphore().acquire();

        globalSlots.acquire();
        segmentSlots[node.getSegment()].acquire();

        mutexPetrinet.acquire();
        while (!petriNet.transitionEnabled(transition)) {
            mutexPetrinet.release();

            mutexQueues.acquire();
            waitQueue.addLast(node);
            mutexQueues.release();
            globalSlots.release();
            segmentSlots[node.getSegment()].release();
            node.getSemaphore().acquire();

            globalSlots.acquire();
            segmentSlots[node.getSegment()].acquire();
            mutexPetrinet.acquire();
        }

        if (petriNet.isTemporary(transition)) {
            petriNet.consumeTokens(transition);

            //dormir
            mutexPetrinet.release();
            globalSlots.release();
            segmentSlots[node.getSegment()].release();
            SleepUtilities.nap(transition);

            mutexQueues.acquire();
            courtesyQueue.addLast(node);
            mutexQueues.release();
            node.getSemaphore().acquire();

            mutexPetrinet.acquire();
            petriNet.produceTokens(transition);
        } else {
            petriNet.fire(transition);
        }
        mutexPetrinet.release();

        despertarHilo();

        return true;
    }

    private void despertarHilo () {
        mutexQueues.acquire();
        if(!courtesyQueue.isEmpty()) {
            QueueNode node = courtesyQueue.removeFirst();
            node.getSemaphore().release();
        } else if (!waitQueue.isEmpty()) {
            mutexPetrinet.acquire();
            QueueNode node = null;
            for (QueueNode n : waitQueue) {
                if (petriNet.transitionEnabled(n.getTransition())) {
                    node = n;
                    break;
                }
            }
            mutexPetrinet.release();
            if (node != null) {
                waitQueue.remove(node);
                node.getSemaphore().release();
            } else if (!enterQueue.isEmpty()) {
                enterQueue.removeFirst().getSemaphore().release();
            }
        } else {
            QueueNode node = enterQueue.removeFirst();
            node.getSemaphore().release();
        }
        mutexQueues.release();
    }

}*/
