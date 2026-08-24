import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads a Petri net out of a {@link Properties} object.
 *
 * <p>This is the instance-based counterpart of {@code PetrinetLoader}: the parsing rules are the
 * same, but the source is a {@code Properties} handed in by the caller instead of a static field
 * loaded once at class-initialization time. That distinction is what allows several different nets
 * to be loaded during the lifetime of one process — and, in a web context, during concurrent
 * requests — without any shared mutable state.
 *
 * <p>Expected keys:
 *
 * <ul>
 *   <li>{@code initial_marking.vector} — comma-separated token counts, one per place
 *   <li>{@code temporary_transitions.vector} — comma-separated flags, one per transition
 *   <li>{@code matrix.pre.0}, {@code matrix.pre.1}, … — one row per place
 *   <li>{@code matrix.post.0}, {@code matrix.post.1}, … — one row per place
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
public final class PetriNetProperties {

  /** Property key holding the initial marking vector. */
  private static final String KEY_MARKING = "initial_marking.vector";

  /** Property key holding the temporal transitions vector. */
  private static final String KEY_TEMPORAL = "temporary_transitions.vector";

  /** Key prefix of the pre-incidence matrix rows. */
  private static final String PREFIX_PRE = "matrix.pre";

  /** Key prefix of the post-incidence matrix rows. */
  private static final String PREFIX_POST = "matrix.post";

  /** The properties being read. */
  private final Properties properties;

  /** Human-readable origin of the properties, used in error messages. */
  private final String source;

  /**
   * Wraps an already loaded {@code Properties} object.
   *
   * @param properties the key-value pairs describing the net
   * @param source human-readable origin, quoted in error messages
   */
  public PetriNetProperties(Properties properties, String source) {
    this.properties = properties;
    this.source = source;
  }

  /**
   * Loads a properties file from the classpath resources.
   *
   * @param resource the resource name, e.g. {@code "exampleHuang.properties"}
   * @return a reader over that resource
   * @throws ConfigurationException if the resource is missing or unreadable
   */
  public static PetriNetProperties fromResource(String resource) {
    try (InputStream input =
        PetriNetProperties.class.getClassLoader().getResourceAsStream(resource)) {
      if (input == null) {
        throw new ConfigurationException("Could not find " + resource + " in resources");
      }
      Properties loaded = new Properties();
      loaded.load(input);
      return new PetriNetProperties(loaded, resource);
    } catch (IOException e) {
      throw new ConfigurationException("Error loading " + resource + ": " + e.getMessage(), e);
    }
  }

  /**
   * Builds a validated definition from the properties.
   *
   * @return the net described by these properties
   * @throws ConfigurationException if a key is missing or malformed
   * @throws PetriNetValidationException if the resulting net is structurally invalid
   */
  public PetriNetDefinition toDefinition() {
    return new PetriNetDefinition(
        getMatrix(PREFIX_PRE),
        getMatrix(PREFIX_POST),
        getIntArray(KEY_MARKING),
        getIntArray(KEY_TEMPORAL));
  }

  /**
   * Reads a comma-separated integer vector.
   *
   * @param key the property key
   * @return the parsed values
   * @throws ConfigurationException if the key is missing, empty, or holds a non-integer
   */
  public int[] getIntArray(String key) {
    String value = properties.getProperty(key);
    if (value == null || value.trim().isEmpty()) {
      throw new ConfigurationException("The key was not found or is empty: " + key + " (" + source + ")");
    }

    String[] parts = value.split(",");
    int[] result = new int[parts.length];

    for (int i = 0; i < parts.length; i++) {
      try {
        result[i] = Integer.parseInt(parts[i].trim());
      } catch (NumberFormatException e) {
        throw new ConfigurationException(
            "Invalid value in array '" + parts[i] + "' for: " + key + " (" + source + ")");
      }
    }

    return result;
  }

  /**
   * Reads a matrix stored as one property per row, e.g. {@code matrix.pre.0}, {@code matrix.pre.1}.
   *
   * <p>Rows are read until the first missing index, so numbering must be contiguous and start at
   * zero.
   *
   * @param prefix the key prefix without the row index
   * @return the parsed matrix, one row per place
   * @throws ConfigurationException if no rows exist or the rows have inconsistent widths
   */
  public int[][] getMatrix(String prefix) {
    int rows = 0;
    while (properties.getProperty(prefix + "." + rows) != null) {
      rows++;
    }

    if (rows == 0) {
      throw new ConfigurationException("No rows found for " + prefix + " (" + source + ")");
    }

    int[] firstRow = getIntArray(prefix + ".0");
    int columns = firstRow.length;

    int[][] matrix = new int[rows][];
    matrix[0] = firstRow;

    for (int i = 1; i < rows; i++) {
      int[] row = getIntArray(prefix + "." + i);
      if (row.length != columns) {
        throw new ConfigurationException(
            "Row " + i + " of " + prefix + " must have " + columns + " items (" + source + ")");
      }
      matrix[i] = row;
    }

    return matrix;
  }
}