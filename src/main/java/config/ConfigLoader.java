public class ConfigLoader extends PropertiesLoader{
    public ConfigLoader() {
        super();
    }

    /**
     * Gets the maximum number of invariants allowed in execution.
     * 
     * @return the maximum number of invariants
     */
    public int getMaxInvariants() {
        return getInt("execution.max_invariants");
    }

    /**
     * Checks if the balanced policy is enabled.
     * 
     * @return true if balanced policy is enabled, false otherwise
     */
    public boolean getIsBalancedPolicy(){
        return getBoolean("policies.is_balanced");
    }

    /**
     * Gets the balanced policy priority value.
     * 
     * @return the balanced policy priority as a double
     */
    public double getBalancedPolicy() {
        return getDouble("policies.balanced");
    }

    /**
     * Gets the reservation confirmation priority value.
     * 
     * @return the reservation confirmation priority as a double
     */
    public double getReservationConfirmationPriority() {
        return getDouble("policies.reservation_confirmation");
    }

    /**
     * Gets the agent priority for P6.
     * 
     * @return the agent priority for P6 as a double
     */
    public double getAgentPriorityP6() {
        return getDouble("policies.agent_priority_p6");
    }

    public int getPetrinetNumber() {
        return Integer.parseInt(config.getProperty("petrinet.number", "0"));
    }

    public String getPetrinetFile () {
        return config.getProperty("petrinet." + getpetrinetNumber());
    }
}