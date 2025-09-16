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
    private static final double BALANCED_POLICY = 0.5;
    
    /** 
     * Prioritized policy configuration for reservation confirmation.
     * Ensures 80% of reservations are confirmed vs 20% cancelled.
     */
    private static final double RESERVATION_CONFIRMATION_PRIORITY = 0.8; 
    
    /** 
     * Prioritized policy configuration for agent selection.
     * Ensures 75% of reservations are processed by upper agent (P6).
     */
    private static final double AGENT_PRIORITY_P6 = 0.75; 
    
    /** 
     * Current policy mode flag.
     * true = balanced policy (50/50 distribution)
     * false = prioritized policy (75/25 agent, 80/20 confirmation)
     */
    private static boolean isBalancedPolicy = true;
    
    /**
     * Sets the active policy mode for the simulation.
     * This configuration affects both agent selection and reservation outcome decisions.
     *
     * @param isBalanced true for balanced policy, false for prioritized policy
     */
    public static void setPolicy(boolean isBalanced) {
        isBalancedPolicy = isBalanced;
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
            policy = BALANCED_POLICY;
        } else {
            policy = RESERVATION_CONFIRMATION_PRIORITY;
        }
        
        if (Math.random() < policy) {
            return Sequences.CONFIRMATION_PROCESS;
        } else {
            return Sequences.CANCELLATION_PROCESS;
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
            return BALANCED_POLICY;
        } else {
            return AGENT_PRIORITY_P6;
        }
    }
}