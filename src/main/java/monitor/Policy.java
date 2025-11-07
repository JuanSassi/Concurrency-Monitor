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
/*class Policy {
    private static final double balancedPolicy;

    private static final double reservationConfirmationPriority;
    
    private static final double agentPriorityP6;
    
    private static final boolean isBalancedPolicy;

    static {
        balancedPolicy = ConfigLoader.getBalancedPolicy();
        reservationConfirmationPriority = ConfigLoader.getReservationConfirmationPriority();
        agentPriorityP6 = ConfigLoader.getAgentPriorityP6();
        isBalancedPolicy = ConfigLoader.getIsBalancedPolicy();
    }
    
    public static boolean isBalancedPolicy() {
        return isBalancedPolicy;
    }

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

    public static double getAgentPriority(){
        if(isBalancedPolicy){
            return balancedPolicy;
        } else {
            return agentPriorityP6;
        }
    }
}*/