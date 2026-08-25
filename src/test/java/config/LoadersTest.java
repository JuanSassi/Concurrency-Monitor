import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConfigLoader} and {@link PetrinetLoader}.
 *
 * <p>Ambas clases heredan de {@link PropertiesLoader}, que carga {@code config.properties} y el
 * archivo de red en un bloque {@code static} al inicializarse la clase. No hay punto de inyección,
 * así que estos <b>no son tests unitarios</b>: corren contra los recursos reales del proyecto.
 *
 * <p>Por eso las aserciones son invariantes estructurales — relaciones que deben valer sea cual sea
 * la red configurada — y no valores concretos, que cambiarían al tocar {@code petrinet.number}.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("ConfigLoader y PetrinetLoader")
class LoadersTest {

  // ── PetrinetLoader: matrices ──────────────────────────────

  @Test
  @DisplayName("pre y post tienen las mismas dimensiones")
  void testMatricesHaveSameShape() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();

    assertEquals(pre.length, post.length, "distinta cantidad de lugares");
    for (int i = 0; i < pre.length; i++) {
      assertEquals(pre[i].length, post[i].length, "distinta cantidad de transiciones en la fila " + i);
    }
  }

  @Test
  @DisplayName("todas las filas de pre tienen el mismo ancho")
  void testPreRowsAreRectangular() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int columns = pre[0].length;
    for (int i = 1; i < pre.length; i++) {
      assertEquals(columns, pre[i].length, "fila " + i + " con ancho distinto");
    }
  }

  @Test
  @DisplayName("los pesos de las matrices no son negativos")
  void testWeightsAreNonNegative() {
    for (int[] row : PetrinetLoader.getPreMatrix()) {
      for (int v : row) {
        assertTrue(v >= 0, "peso negativo en pre: " + v);
      }
    }
    for (int[] row : PetrinetLoader.getPostMatrix()) {
      for (int v : row) {
        assertTrue(v >= 0, "peso negativo en post: " + v);
      }
    }
  }

  @Test
  @DisplayName("cada llamada devuelve una matriz nueva, no una referencia compartida")
  void testMatricesAreNotShared() {
    int[][] first = PetrinetLoader.getPreMatrix();
    int original = first[0][0];
    first[0][0] = original + 1000;

    assertEquals(original, PetrinetLoader.getPreMatrix()[0][0], "el loader cachea estado mutable");
  }

  // ── PetrinetLoader: dimensiones y vectores ────────────────

  @Test
  @DisplayName("getNumPlaces coincide con las filas de pre")
  void testNumPlaces() {
    assertEquals(PetrinetLoader.getPreMatrix().length, PetrinetLoader.getNumPlaces());
  }

  @Test
  @DisplayName("getNumTransitions coincide con las columnas de pre")
  void testNumTransitions() {
    assertEquals(PetrinetLoader.getPreMatrix()[0].length, PetrinetLoader.getNumTransitions());
  }

  @Test
  @DisplayName("el marcado inicial tiene un elemento por lugar")
  void testMarkingLength() {
    assertEquals(
        PetrinetLoader.getNumPlaces(), PetrinetLoader.getInitialMarkingVector().length);
  }

  @Test
  @DisplayName("el marcado inicial no tiene valores negativos")
  void testMarkingNonNegative() {
    for (int tokens : PetrinetLoader.getInitialMarkingVector()) {
      assertTrue(tokens >= 0, "cantidad de tokens negativa: " + tokens);
    }
  }

  @Test
  @DisplayName("el vector temporal tiene un elemento por transición")
  void testTemporalLength() {
    assertEquals(
        PetrinetLoader.getNumTransitions(), PetrinetLoader.getTemporalTransitionsVector().length);
  }

  @Test
  @DisplayName("getTransitionTimes es un alias de getTemporalTransitionsVector")
  void testTransitionTimesIsAlias() {
    assertArrayEquals(
        PetrinetLoader.getTemporalTransitionsVector(), PetrinetLoader.getTransitionTimes());
  }

  // ── coherencia entre el loader legacy y PetriNetProperties ─

  @Test
  @DisplayName("PetrinetLoader y PetriNetProperties parsean el mismo archivo igual")
  void testLegacyAndNewReaderAgree() {
    // PetrinetLoader.getPreMatrix y PetriNetProperties.getMatrix son código duplicado.
    // Este test es la red de seguridad para migrar: si divergen, salta acá.
    PetriNetDefinition viaProperties =
        PetriNetProperties.fromResource(ConfigLoader.getPetrinetFile()).toDefinition();

    assertArrayEquals(PetrinetLoader.getPreMatrix(), viaProperties.pre());
    assertArrayEquals(PetrinetLoader.getPostMatrix(), viaProperties.post());
    assertArrayEquals(
        PetrinetLoader.getInitialMarkingVector(), viaProperties.initialMarking());
    assertArrayEquals(
        PetrinetLoader.getTemporalTransitionsVector(), viaProperties.temporalTransitions());
  }

  @Test
  @DisplayName("PetriNetDefinition.fromProperties produce una red válida")
  void testFromProperties() {
    PetriNetDefinition d = PetriNetDefinition.fromProperties();
    assertNotNull(d);
    assertEquals(d.pre().length, d.initialMarking().length);
  }

  // ── ConfigLoader ──────────────────────────────────────────

  @Test
  @DisplayName("getPetrinetFile apunta al archivo del petrinet.number configurado")
  void testPetrinetFileMatchesNumber() {
    String file = ConfigLoader.getPetrinetFile();
    assertNotNull(file, "petrinet." + ConfigLoader.getPetrinetNumber() + " no está definido");
    assertFalse(file.isBlank());
  }

  @Test
  @DisplayName("getPetrinetNumber no es negativo")
  void testPetrinetNumber() {
    assertTrue(ConfigLoader.getPetrinetNumber() >= 0);
  }

  @Test
  @DisplayName("getMaxInvariants es positivo")
  void testMaxInvariants() {
    assertTrue(ConfigLoader.getMaxInvariants() > 0, "un máximo de 0 invariantes no tiene sentido");
  }

  @Test
  @DisplayName("las banderas booleanas se leen sin error")
  void testBooleanFlags() {
    assertDoesNotThrow(ConfigLoader::isStandardPolicies);
    assertDoesNotThrow(ConfigLoader::getFullprint);
  }

  @Test
  @DisplayName("la política estándar es un número finito y no negativo")
  void testStandardPolicyIsSane() {
    // Deliberadamente laxo: el rango concreto depende de config.properties y ajustarlo
    // no debería romper el test. Lo que se fija acá es que la clave existe y parsea.
    double value = ConfigLoader.getStandardPolicies();
    assertTrue(Double.isFinite(value) && value >= 0.0, "valor inválido: " + value);
  }

  @Test
  @DisplayName("hay al menos una política definida")
  void testTotalPolicies() {
    assertTrue(ConfigLoader.getTotalPolicies() >= 1);
  }

  @Test
  @DisplayName("getValuePolicies acepta todo el rango 1..total")
  void testAllPolicyIndicesAreReadable() {
    int total = ConfigLoader.getTotalPolicies();
    for (int i = 1; i <= total; i++) {
      final int index = i;
      assertDoesNotThrow(() -> ConfigLoader.getValuePolicies(index), "falló la política " + index);
    }
  }

  @Test
  @DisplayName("getValuePolicies(0) lanza excepción")
  void testPolicyIndexZero() {
    assertThrows(RuntimeException.class, () -> ConfigLoader.getValuePolicies(0));
  }

  @Test
  @DisplayName("getValuePolicies con índice negativo lanza excepción")
  void testPolicyIndexNegative() {
    assertThrows(RuntimeException.class, () -> ConfigLoader.getValuePolicies(-1));
  }

  @Test
  @DisplayName("getValuePolicies por encima del total lanza excepción")
  void testPolicyIndexTooHigh() {
    int beyond = ConfigLoader.getTotalPolicies() + 1;
    assertThrows(RuntimeException.class, () -> ConfigLoader.getValuePolicies(beyond));
  }
}