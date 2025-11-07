import java.util.Set;
import java.util.HashSet;

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
     * Gets the value of the nth policy.
     * Example: for num = 1 → "policies.value.1"
     *
     * @param num the policy number
     * @return the policy value as a double
     * @throws RuntimeException if the policy index is invalid or value is not a valid double
     */
    public static double getValuePolicies(int num) {
        // Obtener todas las claves del archivo
        Set<String> keys = config.stringPropertyNames();

        // Contar cuántas policies.value.X hay
        int totalPolicies = 0;
        for (String key : keys) {
            if (key.startsWith("policies.value.")) {
                totalPolicies++;
            }
        }

        if (num < 1 || num > totalPolicies) {
            throw new RuntimeException("Invalid policy number: " + num +
                    ". Expected between 1 and " + totalPolicies);
        }

        return getDouble("policies.value." + num);
    }

    /**
     * Retrieves the standard policy value used as the base probability
     * or default weighting factor in decision-making processes.
     * <p>
     * This value is read from the <b>policies.standard</b> property
     * in the configuration file.
     * </p>
     *
     * @return the standard policy value as a double
     * @throws RuntimeException if the property is missing or not a valid double
     */
    public static double getStandardPolicies(){
        return getDouble("policies.standard");
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
}