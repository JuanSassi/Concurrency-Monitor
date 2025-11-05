import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Base utility class for loading and managing configuration properties from files.
 * This class provides the foundation for loading both main configuration and
 * Petri net-specific configurations from properties files located in the resources folder.
 * 
 * <p>The class automatically loads the main configuration file (config.properties)
 * and the appropriate Petri net configuration file during class initialization.</p>
 * 
 * <p>Subclasses can extend this class to provide specialized access to specific
 * configuration values through type-safe getter methods.</p>
 * 
 * <p>Configuration files must be located in the classpath resources directory.</p>
 * 
 * @see ConfigLoader
 * @see PetrinetLoader
 * 
 * @author Sassi Juan Ignacio
 */
class PropertiesLoader {
    /** Properties object holding configuration key-value pairs */
    protected static Properties config;

    /** Properties object holding petri net specific configurations */
    protected static Properties petrinet;

    /** Name of the petri net properties file to load */
    private static String petrinetFile;

    /** Name of the main configuration file */
    private static String configFile;

    /**
     * Static initialization block that loads the configuration files
     * when the class is first loaded. This ensures configurations are
     * available before any subclass methods are called.
     */
    static {
        configFile = "config.properties";
        loadConfig();
        loadPetrinet();
    }

    /**
     * Protected constructor to prevent direct instantiation of this utility class.
     * Subclasses can call this constructor, but the class is primarily designed
     * for static method access.
     */
    protected PropertiesLoader() {}

    /**
     * Loads the main configuration from config.properties located in resources.
     * This method is called automatically during class initialization.
     * 
     * @throws RuntimeException if the config.properties file cannot be found or read
     */
    private static void loadConfig() {
        try (InputStream input = PropertiesLoader.class.getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                throw new RuntimeException("Could not find " + configFile + " in resources");
            }
            
            config = new Properties();
            config.load(input);
            
            System.out.println("Configuration loaded from " + configFile);
            
        } catch (IOException e) {
            throw new RuntimeException("Error loading configuration: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loads the Petri net configuration based on the petrinet.number property.
     * This method determines which Petri net file to load and loads it into
     * the petrinet Properties object.
     * 
     * <p>The method reads "petrinet.number" from the main config to determine
     * which Petri net configuration to load, then loads the file specified
     * by the "petrinet.{number}" property.</p>
     * 
     * @throws RuntimeException if the petri net file cannot be found, if the
     *         petrinet.number property is invalid, or if the file cannot be read
     */
    private static void loadPetrinet() {
        try {
            // Get the petri net number to load
            int petrinetNumber = Integer.parseInt(config.getProperty("petrinet.number", "0"));
            petrinetFile = config.getProperty("petrinet." + petrinetNumber);
            
            if (petrinetFile == null) {
                throw new RuntimeException("Petri net file not found for petrinet." + petrinetNumber);
            }
            
            InputStream input = PropertiesLoader.class.getClassLoader().getResourceAsStream(petrinetFile);
            if (input == null) {
                throw new RuntimeException("Could not find " + petrinetFile + " in resources");
            }
            
            petrinet = new Properties();
            petrinet.load(input);
            input.close();
            
            System.out.println("Petri net configuration loaded from " + petrinetFile);
            
        } catch (IOException e) {
            throw new RuntimeException("Error loading petri net configuration: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid petrinet.number value in config.properties");
        }
    }

    /**
     * Retrieves a boolean value from the main configuration.
     * Only accepts "true" or "false" (case-insensitive) as valid values.
     * 
     * @param key the configuration key to retrieve
     * @return the boolean value associated with the key
     * @throws RuntimeException if the key is not found or the value is not a valid boolean
     */
    protected static boolean getBoolean(String key) {
        String value = config.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Key not found: " + key);
        }
        
        String trimmedValue = value.trim();
        
        if (!trimmedValue.equalsIgnoreCase("true") && !trimmedValue.equalsIgnoreCase("false")) {
            throw new RuntimeException("The value '" + value + "' is not a valid boolean for: " + key + 
                                    ". Valid values are: 'true' or 'false'");
        }
        
        return Boolean.parseBoolean(trimmedValue);
    }

    /**
     * Retrieves an integer value from the main configuration.
     * Whitespace in the property value is automatically trimmed.
     * 
     * @param key the configuration key to retrieve
     * @return the integer value associated with the key
     * @throws RuntimeException if the key is not found or the value cannot be parsed as an integer
     */
    protected static int getInt(String key) {
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
     * Retrieves a double value from the main configuration.
     * Whitespace in the property value is automatically trimmed.
     * 
     * @param key the configuration key to retrieve
     * @return the double value associated with the key
     * @throws RuntimeException if the key is not found or the value cannot be parsed as a double
     */
    protected static double getDouble(String key) {
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
     * Retrieves an integer array from the Petri net configuration.
     * Values should be comma-separated in the properties file.
     * Whitespace around values is automatically trimmed.
     * 
     * @param key the configuration key to retrieve
     * @return an array of integers parsed from the comma-separated value
     * @throws RuntimeException if the key is not found, empty, or contains invalid integer values
     */
    protected static int[] getIntArray(String key) {
        String value = petrinet.getProperty(key);
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
}