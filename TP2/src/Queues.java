import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Queues {
    Queue<ClientStates> inputBuffer = new LinkedList<>();
    Queue<ClientStates> waitingRoom1 = new LinkedList<>();
    Queue<ClientStates> waitingRoom2 = new LinkedList<>();
    Queue<ClientStates> outputBuffer = new LinkedList<>();

    private Thread[] inputBuffer = new Thread[5];
    private Thread[] waitingRoom1 = new Thread[5];
    private Thread[] waitingRoom2 = new Thread[5];
    private Thread[] outputBuffer = new Thread[5];

    public Queues(){
        for (int i = 0; i < 5; i++) {
            inputBuffer.add(ClientStates.PENDING_ENTRY);
        }
    }
}