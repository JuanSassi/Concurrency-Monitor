/**
 * Configuration loader for accessing main application configuration values.
 * This class extends PropertiesLoader to provide convenient static methods
 * for retrieving specific configuration properties related to execution settings,
 * policies, and Petri net selection.
 * 
 * <p>All methods in this class are static and provide type-safe access to
 * configuration values defined in the config.properties file.</p>
 * 
 * @see PropertiesLoader
 * 
 * @author Sassi Juan Ignacio
 */
class ConfigLoader extends PropertiesLoader{
    private ConfigLoader() {
        super();
    }

    /**
     * Gets the maximum number of invariants allowed in execution.
     * 
     * @return the maximum number of invariants
     */
    public static int getMaxInvariants() {
        return getInt("execution.max_invariants");
    }

    /**
     * Checks if the balanced policy is enabled.
     * 
     * @return true if balanced policy is enabled, false otherwise
     */
    public static boolean getIsBalancedPolicy(){
        return getBoolean("policies.is_balanced");
    }

    /**
     * Gets the balanced policy priority value.
     * 
     * @return the balanced policy priority as a double
     */
    public static double getBalancedPolicy() {
        return getDouble("policies.balanced");
    }

    /**
     * Gets the reservation confirmation priority value.
     * 
     * @return the reservation confirmation priority as a double
     */
    public static double getReservationConfirmationPriority() {
        return getDouble("policies.reservation_confirmation");
    }

    /**
     * Gets the agent priority for P6.
     * 
     * @return the agent priority for P6 as a double
     */
    public static double getAgentPriorityP6() {
        return getDouble("policies.agent_priority_p6");
    }

    public static int getPetrinetNumber() {
        return Integer.parseInt(config.getProperty("petrinet.number", "0"));
    }

    public static String getPetrinetFile () {
        return config.getProperty("petrinet." + getPetrinetNumber());
    }
}