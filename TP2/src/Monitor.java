import java.util.concurrent.Semaphore;

public class Monitor implements MonitorInterface {
    private static Monitor monitor = new Monitor();

    private Semaphore mutex = new Semaphore(1);
    private Semaphore door = new Semaphore(1);
    private Semaphore agent = new Semaphore(1);
    private Semaphore manager1 = new Semaphore(1);
    private Semaphore manager2 = new Semaphore(1);
    private Semaphore assistants = new Semaphore(5;

    private PetriNet petriNet;
    private Queues queues;

    private Monitor() {
    }

    public static Monitor getInstance() {
        return monitor;
    }

    public boolean fireTransition(int transition) {
        try{
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(petriNet.isEnabled(transition)){
            petriNet.inputFiring(transition);
            transitionAction(transition);
            petriNet.outputFiring(transition);
        } else {
            System.out.println("La transición " + transition + " no está habilitada.");
        }

        // enabled
        // whoAreThere
        // whichAwake

        mutex.release();
        return true;
    }

    private void transitionAction(int transition){
        switch (transition) {
            case 0:
                System.out.println("Running transition 0");

                break;
            case 1:
                System.out.println("Running transition 1");
                break;
            case 2:
                System.out.println("Running transition 2");
                break;
            case 3:
                System.out.println("Running transition 3");
                break;
            case 4:
                System.out.println("Running transition 4");
                break;
            case 5:
                System.out.println("Running transition 5");
                break;
            case 6:
                System.out.println("Running transition 6");
                break;
            case 7:
                System.out.println("Running transition 7");
                break;
            case 8:
                System.out.println("Running transition 8");
                break;
            case 9:
                System.out.println("Running transition 9");
                break;
            case 10:
                System.out.println("Running transition 10");
                break;
            case 11:
                System.out.println("Running transition 11");
                break;
            case 12:
                System.out.println("Running transition 12");
                break;
            case 13:
                System.out.println("Running transition 13");
                break;
            case 14:
                System.out.println("Running transition 14");
                break;
            default:
                System.out.println("Invalid option!");
                break;
        }
    }
}