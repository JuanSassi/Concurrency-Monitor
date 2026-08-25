import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConfigurationException} and {@link PetriNetValidationException}.
 *
 * <p>Son clases chicas, pero la distinción entre ambas es la que decide si un fallo se mapea a HTTP
 * 500 (problema del despliegue) o 400 (dato inválido del usuario). Vale la pena fijarla con tests.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Excepciones del dominio")
class ExceptionsTest {

  // ── ConfigurationException ────────────────────────────────

  @Test
  @DisplayName("ConfigurationException conserva el mensaje")
  void testConfigurationExceptionMessage() {
    ConfigurationException e = new ConfigurationException("archivo faltante");
    assertEquals("archivo faltante", e.getMessage());
    assertNull(e.getCause());
  }

  @Test
  @DisplayName("ConfigurationException conserva la causa original")
  void testConfigurationExceptionCause() {
    Throwable cause = new java.io.IOException("disco");
    ConfigurationException e = new ConfigurationException("error de lectura", cause);
    assertSame(cause, e.getCause());
    assertEquals("error de lectura", e.getMessage());
  }

  @Test
  @DisplayName("ConfigurationException es unchecked")
  void testConfigurationExceptionIsUnchecked() {
    assertTrue(RuntimeException.class.isAssignableFrom(ConfigurationException.class));
  }

  // ── PetriNetValidationException ───────────────────────────

  @Test
  @DisplayName("PetriNetValidationException conserva el mensaje")
  void testValidationExceptionMessage() {
    PetriNetValidationException e = new PetriNetValidationException("dimensiones inválidas");
    assertEquals("dimensiones inválidas", e.getMessage());
    assertNull(e.getCause());
  }

  @Test
  @DisplayName("PetriNetValidationException conserva la causa original")
  void testValidationExceptionCause() {
    Throwable cause = new IllegalStateException("raíz");
    PetriNetValidationException e = new PetriNetValidationException("dato inválido", cause);
    assertSame(cause, e.getCause());
  }

  @Test
  @DisplayName("PetriNetValidationException es unchecked")
  void testValidationExceptionIsUnchecked() {
    assertTrue(RuntimeException.class.isAssignableFrom(PetriNetValidationException.class));
  }

  // ── separación de jerarquías ──────────────────────────────

  @Test
  @DisplayName("las dos excepciones son jerarquías separadas")
  void testExceptionsAreUnrelated() {
    // Si una heredara de la otra, un catch de la más general capturaría ambas y el mapeo
    // 400/500 se rompería en silencio.
    assertTrue(
        !ConfigurationException.class.isAssignableFrom(PetriNetValidationException.class),
        "PetriNetValidationException no debe heredar de ConfigurationException");
    assertTrue(
        !PetriNetValidationException.class.isAssignableFrom(ConfigurationException.class),
        "ConfigurationException no debe heredar de PetriNetValidationException");
  }
}