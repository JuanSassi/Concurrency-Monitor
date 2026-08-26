import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Base utility class for loading and managing configuration properties from files. This class
 * provides the foundation for loading both main configuration and Petri net-specific configurations
 * from properties files located in the resources folder.
 *
 * <p>The class automatically loads the main configuration file (config.properties) and the
 * appropriate Petri net configuration file during class initialization.
 *
 * <p>Subclasses can extend this class to provide specialized access to specific configuration
 * values through type-safe getter methods.
 *
 * <p>Configuration files must be located in the classpath resources directory.
 *
 * @see ConfigLoader
 * @see PetrinetLoader
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

  static {
    configFile = "config.properties";
    loadConfig();
    loadPetrinet();
  }

  protected PropertiesLoader() {}

  private static void loadConfig() {
    try (InputStream input =
        PropertiesLoader.class.getClassLoader().getResourceAsStream(configFile)) {
      if (input == null) {
        throw new ConfigurationException("Could not find " + configFile + " in resources");
      }

      config = new Properties();
      config.load(input);

      System.out.println("Configuration loaded from " + configFile);

    } catch (IOException e) {
      throw new ConfigurationException("Error loading configuration: " + e.getMessage(), e);
    }
  }

  private static void loadPetrinet() {
    try {
      int petrinetNumber = Integer.parseInt(config.getProperty("petrinet.number", "0"));
      petrinetFile = config.getProperty("petrinet." + petrinetNumber);

      if (petrinetFile == null) {
        throw new ConfigurationException("Petri net file not found for petrinet." + petrinetNumber);
      }

      InputStream input = PropertiesLoader.class.getClassLoader().getResourceAsStream(petrinetFile);
      if (input == null) {
        throw new ConfigurationException("Could not find " + petrinetFile + " in resources");
      }

      petrinet = new Properties();
      petrinet.load(input);
      input.close();

      System.out.println("Petri net configuration loaded from " + petrinetFile);

    } catch (IOException e) {
      throw new ConfigurationException(
          "Error loading petri net configuration: " + e.getMessage(), e);
    } catch (NumberFormatException e) {
      throw new ConfigurationException("Invalid petrinet.number value in config.properties");
    }
  }

  protected static boolean getBoolean(String key) {
    String value = config.getProperty(key);
    if (value == null) {
      throw new ConfigurationException("Key not found: " + key);
    }

    String trimmedValue = value.trim();

    if (!trimmedValue.equalsIgnoreCase("true") && !trimmedValue.equalsIgnoreCase("false")) {
      throw new ConfigurationException(
          "The value '"
              + value
              + "' is not a valid boolean for: "
              + key
              + ". Valid values are: 'true' or 'false'");
    }

    return Boolean.parseBoolean(trimmedValue);
  }

  protected static int getInt(String key) {
    String value = config.getProperty(key);
    if (value == null) {
      throw new ConfigurationException("Key not found: " + key);
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new ConfigurationException(
          "The value '" + value + "' is not a valid integer for: " + key);
    }
  }

  protected static double getDouble(String key) {
    String value = config.getProperty(key);
    if (value == null) {
      throw new ConfigurationException("Key not found: " + key);
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      throw new ConfigurationException(
          "The value '" + value + "' is not a valid double for: " + key);
    }
  }

  protected static int[] getIntArray(String key) {
    String value = petrinet.getProperty(key);
    if (value == null || value.trim().isEmpty()) {
      throw new ConfigurationException("The key was not found or is empty: " + key);
    }

    String[] parts = value.split(",");
    int[] result = new int[parts.length];

    for (int i = 0; i < parts.length; i++) {
      try {
        result[i] = Integer.parseInt(parts[i].trim());
      } catch (NumberFormatException e) {
        throw new ConfigurationException("Invalid value in array '" + parts[i] + "' for: " + key);
      }
    }

    return result;
  }
}
