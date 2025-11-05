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
    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods are static and should be accessed through the class name.
     */
    private ConfigLoader() {
        super();
    }

    /**
     * Gets the maximum number of invariants allowed in execution.
     * This value is read from the "execution.max_invariants" property.
     * 
     * @return the maximum number of invariants
     * @throws RuntimeException if the property is not found or is not a valid integer
     */
    public static int getMaxInvariants() {
        return getInt("execution.max_invariants");
    }

    /**
     * Checks if the balanced policy is enabled.
     * This value is read from the "policies.is_balanced" property.
     * 
     * @return true if balanced policy is enabled, false otherwise
     * @throws RuntimeException if the property is not found or is not a valid boolean
     */
    public static boolean getIsBalancedPolicy(){
        return getBoolean("policies.is_balanced");
    }

    /**
     * Checks if full print mode is enabled for the tree visualization.
     * This value is read from the "tree.fullprint" property.
     * 
     * @return true if full print is enabled, false otherwise
     * @throws RuntimeException if the property is not found or is not a valid boolean
     */
    public static boolean getFullprint(){
        return getBoolean("tree.fullprint");
    }

    /**
     * Gets the balanced policy priority value.
     * This value is read from the "policies.balanced" property.
     * 
     * @return the balanced policy priority as a double
     * @throws RuntimeException if the property is not found or is not a valid double
     */
    public static double getBalancedPolicy() {
        return getDouble("policies.balanced");
    }

    /**
     * Gets the reservation confirmation priority value.
     * This value is read from the "policies.reservation_confirmation" property.
     * 
     * @return the reservation confirmation priority as a double
     * @throws RuntimeException if the property is not found or is not a valid double
     */
    public static double getReservationConfirmationPriority() {
        return getDouble("policies.reservation_confirmation");
    }

    /**
     * Gets the agent priority for P6.
     * This value is read from the "policies.agent_priority_p6" property.
     * 
     * @return the agent priority for P6 as a double
     * @throws RuntimeException if the property is not found or is not a valid double
     */
    public static double getAgentPriorityP6() {
        return getDouble("policies.agent_priority_p6");
    }

    /**
     * Gets the Petri net number identifier that determines which
     * Petri net configuration file should be loaded.
     * This value is read from the "petrinet.number" property.
     * 
     * @return the Petri net number identifier, or 0 if not specified
     */
    public static int getPetrinetNumber() {
        return Integer.parseInt(config.getProperty("petrinet.number", "0"));
    }

    /**
     * Gets the filename of the Petri net configuration file to be loaded.
     * The filename is determined by the petrinet number using the
     * "petrinet.{number}" property.
     * 
     * @return the Petri net configuration filename
     * @see #getPetrinetNumber()
     */
    public static String getPetrinetFile () {
        return config.getProperty("petrinet." + getPetrinetNumber());
    }
}