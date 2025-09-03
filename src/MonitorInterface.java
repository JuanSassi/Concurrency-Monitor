/**
 * Interface defining the contract for monitoring and firing transitions
 * in the Petri net system. Implementations should handle the synchronization
 * and coordination of transition firing between multiple concurrent processes.
 *
 * @author Sassi Juan Ignacio
 */
public interface MonitorInterface {
    /**
     * Attempts to fire a transition in the Petri net.
     * This method should handle the synchronization logic to ensure
     * thread-safe access to shared resources and proper transition firing.
     *
     * @param transition The index of the transition to fire
     * @return true if the transition was successfully fired, false otherwise
     */
    public abstract boolean fireTransition(int transition);
}