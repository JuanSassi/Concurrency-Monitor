/**
 * Thrown when the application's own configuration cannot be loaded or is malformed — a missing
 * properties file, an unreadable resource, or an invalid internal setting.
 *
 * <p>Unlike {@link PetriNetValidationException}, this signals a problem with the deployment/catalog
 * itself, not with data a user submitted. In a web context it should map to HTTP 500.
 *
 * @author Sassi Juan Ignacio
 */
public class ConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
