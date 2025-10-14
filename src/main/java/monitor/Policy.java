/**
 * Policy management class for controlling resource allocation and decision-making
 * in the travel agency Petri net simulation. Implements two distinct policy modes
 * to resolve conflicts and manage system behavior according to specified requirements.
 *
 * Supports two independent policy configurations:
 * 1. Balanced Policy: Ensures equitable distribution of clients between reservation
 *    agents and equal proportions of confirmed vs cancelled reservations.
 * 2. Prioritized Processing Policy: Favors the upper reservation agent (P6) with 75%
 *    of reservations and prioritizes confirmation processes with 80% confirmation rate.
 *
 * @author Sassi Juan Ignacio
 */
class Policy {
    /** Probability value for balanced decisions (50% for all choices) */
    private static final double balancedPolicy;
    
    /** 
     * Prioritized policy configuration for reservation confirmation.
     * Ensures 80% of reservations are confirmed vs 20% cancelled.
     */
    private static final double reservationConfirmationPriority;
    
    /** 
     * Prioritized policy configuration for agent selection.
     * Ensures 75% of reservations are processed by upper agent (P6).
     */
    private static final double agentPriorityP6;
    
    /** 
     * Current policy mode flag.
     * true = balanced policy (50/50 distribution)
     * false = prioritized policy (75/25 agent, 80/20 confirmation)
     */
    private static final boolean isBalancedPolicy;

    static {
        balancedPolicy = ConfigLoader.getBalancedPolicy();
        reservationConfirmationPriority = ConfigLoader.getReservationConfirmationPriority();
        agentPriorityP6 = ConfigLoader.getAgentPriorityP6();
        isBalancedPolicy = ConfigLoader.getIsBalancedPolicy();
    }
    
    /**
     * Returns the current policy mode.
     *
     * @return true if balanced policy is active, false if prioritized policy is active
     */
    public static boolean isBalancedPolicy() {
        return isBalancedPolicy;
    }

    /**
     * Selects between confirmation and cancellation sequences based on active policy.
     * 
     * Balanced Policy: 50% confirmation, 50% cancellation
     * Prioritized Policy: 80% confirmation, 20% cancellation
     *
     * @return CONFIRMATION_PROCESS or CANCELLATION_PROCESS sequence array
     */
    public static int[] selectSequence() {
        double policy;
        if (isBalancedPolicy) {
            policy = balancedPolicy;
        } else {
            policy = reservationConfirmationPriority;
        }
        
        if (Math.random() < policy) {
            return Sequences.confirmationProcess;
        } else {
            return Sequences.cancellationProcess;
        }
    }

    /**
     * Returns the priority probability for upper reservation agent (P6) selection.
     * Used by the Monitor's scheduling algorithm to resolve conflicts between
     * transitions T2 (upper agent) and T3 (lower agent).
     *
     * Balanced Policy: 50% probability for upper agent (P6)
     * Prioritized Policy: 75% probability for upper agent (P6)
     *
     * @return probability value for selecting upper agent over lower agent
     */
    public static double getAgentPriority(){
        if(isBalancedPolicy){
            return balancedPolicy;
        } else {
            return agentPriorityP6;
        }
    }
}