import java.util.stream.IntStream;
import java.util.Random;

public class Process implements Runnable{
    public ClientStates states = ClientStates.PENDING_ENTRY;
    public Monitor monitor;

    public boolean balancedPolicy;
    public boolean prioritizedProcessingPolicy;

    public int probabilityManager;
    public int probabilityConfirmedAndPay;

    private int[] transitionSet;
    private int[] transitionSetDoor = {0,1};
    private int[] transitionSetManager1 = {2,5};
    private int[] transitionSetManager2 = {3,4};
    private int[] transitionSetConfirmedAndPay = {6,9,10,11};
    private int[] transitionSetCanceled = {7,8,11};

    public Process(boolean balancedPolicy) {
        this.balancedPolicy = balancedPolicy;
        this.prioritizedProcessingPolicy = !balancedPolicy;
        if (this.balancedPolicy) {
            probabilityManager = 50;
            probabilityConfirmedAndPay = 50;
        }
        if (this.prioritizedProcessingPolicy) {
            probabilityManager = 75;
            probabilityConfirmedAndPay = 80;
        }
        createTransitionSet();

    }

    @Override
    public void run() {
        for (int i = 0; i < transitionSet.length; i++) {
            monitor.fireTransition(transitionSet[i]);
        }
    }

    private void createTransitionSet(){
        Random random = new Random();
        int randomNumber1 = random.nextInt(101);
        int randomNumber2 = random.nextInt(101);

        if (randomNumber1 < probabilityManager) {
            transitionSet = IntStream.concat(
                    IntStream.of(transitionSetDoor),
                    IntStream.of(transitionSetManager1)
            ).toArray();
        } else {
            transitionSet = IntStream.concat(
                    IntStream.of(transitionSetDoor),
                    IntStream.of(transitionSetManager2)
            ).toArray();
        }

        if (randomNumber2 < probabilityConfirmedAndPay) {
            transitionSet = IntStream.concat(
                    IntStream.of(transitionSet),
                    IntStream.of(transitionSetConfirmedAndPay)
            ).toArray();
        } else {
            transitionSet = IntStream.concat(
                    IntStream.of(transitionSet),
                    IntStream.of(transitionSetCanceled)
            ).toArray();
        }
    }
}