import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader using static methods to load and retrieve properties
 * from a config.properties file located in the resources directory.
 * This class provides type-safe access to configuration values including
 * primitives, arrays, and matrices. The configuration is loaded once during
 * class initialization through a static block.
 * 
 * @author Sassi Juan Ignacio
 */
class ConfigLoader {
    /** Properties object holding all configuration key-value pairs */
    private static Properties config;
    //private static String properties = "config.properties";
    private static String properties = "config2.properties";
    
    /**
     * Static initialization block that loads the configuration file
     * when the class is first loaded.
     */
    static {
        loadConfig();
    }
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ConfigLoader() {}
    
    /**
     * Loads the configuration from the config.properties file located in resources.
     * This method is called automatically during class initialization.
     * 
     * @throws RuntimeException if the config.properties file cannot be found or read
     */
    private static void loadConfig() {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(properties)) {
            if (input == null) {
                throw new RuntimeException("Could not find config.properties in resources");
            }
            
            config = new Properties();
            config.load(input);
            
            System.out.println("Configuration loaded from config.properties");
            
        } catch (IOException e) {
            throw new RuntimeException("Error loading configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a boolean value from the configuration.
     * Only accepts "true" or "false" (case-insensitive) as valid values.
     * 
     * @param key the configuration key to retrieve
     * @return the boolean value associated with the key
     * @throws RuntimeException if the key is not found or the value is not a valid boolean
     */
    public static boolean getBoolean(String key) {
        String value = config.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Key not found: " + key);
        }
        
        String trimmedValue = value.trim();
        
        // Opción 1: Validación estricta (solo acepta "true" o "false")
        if (!trimmedValue.equalsIgnoreCase("true") && !trimmedValue.equalsIgnoreCase("false")) {
            throw new RuntimeException("The value '" + value + "' is not a valid boolean for: " + key + 
                                    ". Valid values ​​are: 'true' o 'false'");
        }
        
        return Boolean.parseBoolean(trimmedValue);
    }

    /**
     * Retrieves an integer value from the configuration.
     * 
     * @param key the configuration key to retrieve
     * @return the integer value associated with the key
     * @throws RuntimeException if the key is not found or the value cannot be parsed as an integer
     */
    public static int getInt(String key) {
        String value = config.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Key not found: " + key);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("The value '" + value + "' is not a valid integer for: " + key);
        }
    }

    /**
     * Retrieves a double value from the configuration.
     * 
     * @param key the configuration key to retrieve
     * @return the double value associated with the key
     * @throws RuntimeException if the key is not found or the value cannot be parsed as a double
     */
    public static double getDouble(String key) {
        String value = config.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Key not found: " + key);
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("The value '" + value + "' is not a valid double for: " + key);
        }
    }

    /**
     * Retrieves an integer array from the configuration.
     * Values should be comma-separated in the properties file.
     * 
     * @param key the configuration key to retrieve
     * @return an array of integers parsed from the comma-separated value
     * @throws RuntimeException if the key is not found, empty, or contains invalid integer values
     */
    public static int[] getIntArray(String key) {
        String value = config.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("The key was not found or is empty: " + key);
        }
        
        String[] parts = value.split(",");
        int[] result = new int[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid value in array '" + parts[i] + "' for: " + key);
            }
        }
        
        return result;
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
     * Gets the initial marking vector for the Petri net.
     * 
     * @return an integer array representing the initial marking
     */
    public static int[] getInitialMarkingVector() {
        return getIntArray("initial_marking.vector");
    }

    /**
     * Gets the temporal transitions vector.
     * 
     * @return an integer array representing which transitions are temporal
     */
    public static int[] getTemporalTransitionsVector() {
        return getIntArray("temporary_transitions.vector");
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

    /**
     * Gets the entry process sequence.
     * 
     * @return an integer array representing the entry process transition sequence
     */
    public static int[] getEntryProcess() {
        return getIntArray("sequences.entry_process");
    }

    /**
     * Gets the reservation process sequence for above threshold.
     * 
     * @return an integer array representing the reservation process above transition sequence
     */
    public static int[] getReservationProcessAbove() {
        return getIntArray("sequences.reservation_process_above");
    }

    /**
     * Gets the reservation process sequence for below threshold.
     * 
     * @return an integer array representing the reservation process below transition sequence
     */
    public static int[] getReservationProcessBelow() {
        return getIntArray("sequences.reservation_process_below");
    }

    /**
     * Gets the cancellation process sequence.
     * 
     * @return an integer array representing the cancellation process transition sequence
     */
    public static int[] getCancellationProcess() {
        return getIntArray("sequences.cancellation_process");
    }

    /**
     * Gets the confirmation process sequence.
     * 
     * @return an integer array representing the confirmation process transition sequence
     */
    public static int[] getConfirmationProcess() {
        return getIntArray("sequences.confirmation_process");
    }

    /**
     * Gets the pre-incidence matrix for the Petri net.
     * The matrix is loaded from properties with keys "matrix.pre.0", "matrix.pre.1", etc.
     * Each row represents a place, and each column represents a transition.
     * 
     * @return a 2D integer array representing the pre-incidence matrix
     * @throws RuntimeException if no rows are found or if rows have inconsistent dimensions
     */
    public static int[][] getPreMatrix() {
        // Primero necesitamos saber cuántas filas hay
        int places = 0;
        while (config.getProperty("matrix.pre." + places) != null) {
            places++;
        }
        
        if (places == 0) {
            throw new RuntimeException("No rows found for matrix pre");
        }
        
        // Obtener la primera fila para saber cuántas columnas hay
        int[] firstRow = getIntArray("matrix.pre.0");
        int transitions = firstRow.length;
        
        int[][] matrix = new int[places][transitions];
        matrix[0] = firstRow;
        
        for (int i = 1; i < places; i++) {
            int[] row = getIntArray("matrix.pre." + i);
            if (row.length != transitions) {
                throw new RuntimeException("The row " + i + " of the pre-matrix must have " + transitions + " items");
            }
            matrix[i] = row;
        }
        
        return matrix;
    }

    /**
     * Gets the post-incidence matrix for the Petri net.
     * The matrix is loaded from properties with keys "matrix.post.0", "matrix.post.1", etc.
     * Each row represents a place, and each column represents a transition.
     * 
     * @return a 2D integer array representing the post-incidence matrix
     * @throws RuntimeException if no rows are found or if rows have inconsistent dimensions
     */
    public static int[][] getPostMatrix() {
        // Primero necesitamos saber cuántas filas hay
        int places = 0;
        while (config.getProperty("matrix.post." + places) != null) {
            places++;
        }
        
        if (places == 0) {
            throw new RuntimeException("No rows found for the post matrix");
        }
        
        // Obtener la primera fila para saber cuántas columnas hay
        int[] firstRow = getIntArray("matrix.post.0");
        int transitions = firstRow.length;
        
        int[][] matrix = new int[places][transitions];
        matrix[0] = firstRow;
        
        for (int i = 1; i < places; i++) {
            int[] row = getIntArray("matrix.post." + i);
            if (row.length != transitions) {
                throw new RuntimeException("The row " + i + " of the post matrix must have " + transitions + " items");
            }
            matrix[i] = row;
        }
        
        return matrix;
    }

    /**
     * Gets the number of places in the Petri net.
     * This is determined by the number of rows in the pre-incidence matrix.
     * 
     * @return the number of places
     */
    public static int getNumPlaces() {
        return getPreMatrix().length;
    }

    /**
     * Gets the number of transitions in the Petri net.
     * This is determined by the number of columns in the pre-incidence matrix.
     * 
     * @return the number of transitions, or 0 if the matrix is empty
     */
    public static int getNumTransitions() {
        return getPreMatrix().length > 0 ? getPreMatrix()[0].length : 0;
    }
}