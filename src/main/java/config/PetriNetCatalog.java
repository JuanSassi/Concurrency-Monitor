import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Catalog of the Petri nets shipped with the application.
 *
 * <p>Reads the {@code petrinet.N} entries of {@code config.properties} and exposes them as a list
 * of selectable nets, each loadable on demand into a fresh {@link PetriNetDefinition}. Two calls to
 * {@link #load(String)} return two independent definitions — nothing is cached or shared, so
 * concurrent analyses cannot interfere with each other.
 *
 * <p><b>The map of entries is also a whitelist.</b> {@link #load(String)} resolves the caller's id
 * against it and never passes a caller-supplied string to the classloader. Reading a resource name
 * straight from an HTTP request would let a client walk the classpath and pull out arbitrary files.
 *
 * @see PetriNetProperties
 * @author Sassi Juan Ignacio
 */
public final class PetriNetCatalog {

  /** Main configuration file listing the available nets. */
  private static final String CONFIG_FILE = "config.properties";

  /** Key prefix of each catalog entry, e.g. {@code petrinet.0}. */
  private static final String ENTRY_PREFIX = "petrinet.";

  /** Key holding the id of the net selected by default. */
  private static final String DEFAULT_KEY = "petrinet.number";

  /** Suffix stripped from the file name to build the display name. */
  private static final String FILE_SUFFIX = ".properties";

  /** Catalog entries, keyed by id, in ascending id order. */
  private final Map<String, String> filesById;

  /** Id of the net loaded when the caller does not choose one. */
  private final String defaultId;

  /**
   * A net offered by the catalog.
   *
   * @param id stable identifier used to request this net, e.g. {@code "0"}
   * @param name display name, e.g. {@code "travelAgencySystem"}
   */
  public record CatalogEntry(String id, String name) {}

  /**
   * Reads the catalog from {@code config.properties}.
   *
   * @throws ConfigurationException if the file is missing, unreadable, or lists no nets
   */
  public PetriNetCatalog() {
    Properties config = loadConfig();
    this.filesById = readEntries(config);

    if (filesById.isEmpty()) {
      throw new ConfigurationException("No '" + ENTRY_PREFIX + "N' entries found in " + CONFIG_FILE);
    }

    String configured = config.getProperty(DEFAULT_KEY, "").trim();
    this.defaultId =
        filesById.containsKey(configured) ? configured : filesById.keySet().iterator().next();
  }

  /**
   * Loads {@code config.properties} from the classpath.
   *
   * @return the parsed configuration
   */
  private static Properties loadConfig() {
    try (InputStream input =
        PetriNetCatalog.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
      if (input == null) {
        throw new ConfigurationException("Could not find " + CONFIG_FILE + " in resources");
      }
      Properties config = new Properties();
      config.load(input);
      return config;
    } catch (IOException e) {
      throw new ConfigurationException("Error loading " + CONFIG_FILE + ": " + e.getMessage(), e);
    }
  }

  /**
   * Collects the {@code petrinet.N} entries, ignoring every other key.
   *
   * <p>Only keys whose suffix is entirely numeric are accepted, which is what keeps {@code
   * petrinet.number} — a setting, not a net — out of the catalog.
   *
   * @param config the parsed configuration
   * @return file name by id, in ascending numeric order
   */
  private static Map<String, String> readEntries(Properties config) {
    List<String> ids = new ArrayList<>();

    for (String key : config.stringPropertyNames()) {
      if (!key.startsWith(ENTRY_PREFIX)) {
        continue;
      }
      String id = key.substring(ENTRY_PREFIX.length());
      if (isNumeric(id)) {
        ids.add(id);
      }
    }

    ids.sort(Comparator.comparingInt(Integer::parseInt));

    Map<String, String> entries = new LinkedHashMap<>();
    for (String id : ids) {
      String file = config.getProperty(ENTRY_PREFIX + id);
      if (file != null && !file.trim().isEmpty()) {
        entries.put(id, file.trim());
      }
    }
    return entries;
  }

  /**
   * Checks whether a string is a non-empty run of digits.
   *
   * @param value the string to check
   * @return true if every character is a digit
   */
  private static boolean isNumeric(String value) {
    if (value.isEmpty()) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isDigit(value.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Lists the nets available for selection.
   *
   * @return the catalog entries, in ascending id order
   */
  public List<CatalogEntry> list() {
    List<CatalogEntry> entries = new ArrayList<>();
    for (Map.Entry<String, String> entry : filesById.entrySet()) {
      entries.add(new CatalogEntry(entry.getKey(), displayName(entry.getValue())));
    }
    return entries;
  }

  /**
   * Strips the file extension to build a display name.
   *
   * @param file the resource file name
   * @return the name without its {@code .properties} suffix
   */
  private static String displayName(String file) {
    return file.endsWith(FILE_SUFFIX)
        ? file.substring(0, file.length() - FILE_SUFFIX.length())
        : file;
  }

  /**
   * Loads a net from the catalog.
   *
   * @param id the id of the requested net
   * @return a fresh, validated definition
   * @throws PetriNetValidationException if the id is not in the catalog — this is a bad request,
   *     not a deployment problem
   * @throws ConfigurationException if the catalog file itself is missing or malformed
   */
  public PetriNetDefinition load(String id) {
    String file = filesById.get(id);
    if (file == null) {
      throw new PetriNetValidationException(
          "Red desconocida: '" + id + "'. Opciones válidas: " + filesById.keySet() + ".");
    }
    return PetriNetProperties.fromResource(file).toDefinition();
  }

  /**
   * Returns the id of the net selected by {@code petrinet.number}, falling back to the first entry
   * when that setting is missing or points nowhere.
   *
   * @return the default net id
   */
  public String getDefaultId() {
    return defaultId;
  }
}