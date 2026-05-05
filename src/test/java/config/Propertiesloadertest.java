import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PropertiesLoader protected methods.
 *
 * <p>Tests the error paths of getBoolean, getInt, getDouble and getIntArray by calling them through
 * ConfigLoader and PetrinetLoader (which extend PropertiesLoader). This covers the branches that
 * the happy-path tests in ConfigLoaderTest and PetrinetLoaderTest do not reach.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PropertiesLoader — error paths")
class Propertiesloadertest {

  // ── getBoolean ───────────────────────────────────────────

  @Test
  @DisplayName("getBoolean con key inexistente lanza RuntimeException")
  void testGetBooleanKeyNotFound() {
    assertThrows(RuntimeException.class, () -> ConfigLoader.getBoolean("key.that.does.not.exist"));
  }

  @Test
  @DisplayName("getBoolean con valor inválido lanza RuntimeException")
  void testGetBooleanInvalidValue() {
    // policies.standard = 0.5 → no es true/false
    assertThrows(RuntimeException.class, () -> ConfigLoader.getBoolean("policies.standard"));
  }

  @Test
  @DisplayName("getBoolean con valor true retorna true")
  void testGetBooleanTrue() {
    assertTrue(ConfigLoader.getBoolean("policies.isStandard"));
  }

  // ── getInt ───────────────────────────────────────────────

  @Test
  @DisplayName("getInt con key inexistente lanza RuntimeException")
  void testGetIntKeyNotFound() {
    assertThrows(RuntimeException.class, () -> ConfigLoader.getInt("key.that.does.not.exist"));
  }

  @Test
  @DisplayName("getInt con valor no numérico lanza RuntimeException")
  void testGetIntInvalidValue() {
    // policies.isStandard = true → no es entero
    assertThrows(RuntimeException.class, () -> ConfigLoader.getInt("policies.isStandard"));
  }

  @Test
  @DisplayName("getInt con valor válido retorna el entero")
  void testGetIntValid() {
    assertEquals(186, ConfigLoader.getInt("execution.max_invariants"));
  }

  // ── getDouble ────────────────────────────────────────────

  @Test
  @DisplayName("getDouble con key inexistente lanza RuntimeException")
  void testGetDoubleKeyNotFound() {
    assertThrows(RuntimeException.class, () -> ConfigLoader.getDouble("key.that.does.not.exist"));
  }

  @Test
  @DisplayName("getDouble con valor no numérico lanza RuntimeException")
  void testGetDoubleInvalidValue() {
    // policies.isStandard = true → no es double
    assertThrows(RuntimeException.class, () -> ConfigLoader.getDouble("policies.isStandard"));
  }

  @Test
  @DisplayName("getDouble con valor válido retorna el double")
  void testGetDoubleValid() {
    assertEquals(0.5, ConfigLoader.getDouble("policies.standard"), 1e-9);
  }

  // ── getIntArray ──────────────────────────────────────────

  @Test
  @DisplayName("getIntArray con key inexistente lanza RuntimeException")
  void testGetIntArrayKeyNotFound() {
    assertThrows(
        RuntimeException.class, () -> PetrinetLoader.getIntArray("key.that.does.not.exist"));
  }

  @Test
  @DisplayName("getIntArray con valor válido retorna el array")
  void testGetIntArrayValid() {
    int[] result = PetrinetLoader.getIntArray("initial_marking.vector");
    assertEquals(14, result.length);
  }
}
