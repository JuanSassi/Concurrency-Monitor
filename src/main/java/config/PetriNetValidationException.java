/**
 * Thrown when Petri net structural data is invalid — mismatched matrix dimensions, negative values,
 * missing invariants, or a network too large to analyze safely.
 *
 * <p>Signals a problem with the <b>data itself</b>, regardless of whether it came from a catalog
 * file or from a user's request. In a web context it should map to HTTP 400 (Bad Request).
 *
 * @author Sassi Juan Ignacio
 */
public class PetriNetValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PetriNetValidationException(String message) {
    super(message);
  }

  public PetriNetValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
