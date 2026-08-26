import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PetriNetProperties}.
 *
 * <p>Estos tests son unitarios puros: construyen un {@link Properties} en memoria, sin tocar el
 * classpath ni el sistema de archivos. Solo {@code fromResource} necesita recursos reales.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PetriNetProperties")
class PetriNetPropertiesTest {

  /** Nombre de fuente usado en los tests, para verificar que aparece en los mensajes de error. */
  private static final String SOURCE = "fuente-de-prueba";

  /**
   * Construye un lector sobre pares clave-valor dados en línea.
   *
   * @param keyValues pares alternados clave, valor
   * @return el lector listo para usar
   */
  private static PetriNetProperties props(String... keyValues) {
    Properties p = new Properties();
    for (int i = 0; i < keyValues.length; i += 2) {
      p.setProperty(keyValues[i], keyValues[i + 1]);
    }
    return new PetriNetProperties(p, SOURCE);
  }

  /**
   * Construye un lector sobre una red 2x2 completa y válida.
   *
   * @return el lector de una red mínima bien formada
   */
  private static PetriNetProperties validNet() {
    return props(
        "initial_marking.vector", "1,0",
        "temporary_transitions.vector", "0,0",
        "matrix.pre.0", "1,0",
        "matrix.pre.1", "0,1",
        "matrix.post.0", "0,1",
        "matrix.post.1", "1,0");
  }

  // ── getIntArray: casos válidos ────────────────────────────

  @Test
  @DisplayName("getIntArray parsea un vector separado por comas")
  void testGetIntArraySimple() {
    assertArrayEquals(new int[] {1, 2, 3}, props("k", "1,2,3").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray ignora espacios alrededor de cada valor")
  void testGetIntArrayTrimsWhitespace() {
    assertArrayEquals(new int[] {1, 2, 3}, props("k", " 1 ,  2,3 ").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray acepta valores negativos")
  void testGetIntArrayNegatives() {
    assertArrayEquals(new int[] {-1, 0, -25}, props("k", "-1,0,-25").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray con un solo valor devuelve un array de largo 1")
  void testGetIntArraySingleValue() {
    assertArrayEquals(new int[] {7}, props("k", "7").getIntArray("k"));
  }

  // ── getIntArray: casos de error ───────────────────────────

  @Test
  @DisplayName("getIntArray con clave ausente lanza ConfigurationException")
  void testGetIntArrayMissingKey() {
    assertThrows(ConfigurationException.class, () -> props("otra", "1").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray con valor vacío lanza ConfigurationException")
  void testGetIntArrayEmptyValue() {
    assertThrows(ConfigurationException.class, () -> props("k", "").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray con valor de solo espacios lanza ConfigurationException")
  void testGetIntArrayBlankValue() {
    assertThrows(ConfigurationException.class, () -> props("k", "    ").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray con un valor no numérico lanza ConfigurationException")
  void testGetIntArrayNonNumeric() {
    assertThrows(ConfigurationException.class, () -> props("k", "1,dos,3").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray con comas consecutivas lanza ConfigurationException")
  void testGetIntArrayDoubleComma() {
    assertThrows(ConfigurationException.class, () -> props("k", "1,,2").getIntArray("k"));
  }

  @Test
  @DisplayName("getIntArray con desbordamiento de int lanza ConfigurationException")
  void testGetIntArrayOverflow() {
    assertThrows(ConfigurationException.class, () -> props("k", "2147483648").getIntArray("k"));
  }

  @Test
  @DisplayName("el mensaje de error nombra la clave y la fuente")
  void testErrorMessageMentionsKeyAndSource() {
    ConfigurationException e =
        assertThrows(
            ConfigurationException.class, () -> props("otra", "1").getIntArray("faltante"));
    assertTrue(e.getMessage().contains("faltante"), "debe nombrar la clave");
    assertTrue(e.getMessage().contains(SOURCE), "debe nombrar la fuente");
  }

  // ── getIntArray: comportamiento documentado, no deseable ──

  @Test
  @DisplayName("BUG DOCUMENTADO: una coma final se descarta en silencio")
  void testGetIntArrayTrailingCommaIsSilentlyIgnored() {
    // String.split descarta los campos vacíos finales, así que "1,2," parsea como "1,2".
    // Un vector de largo incorrecto pasa desapercibido hasta la validación de dimensiones.
    assertArrayEquals(new int[] {1, 2}, props("k", "1,2,").getIntArray("k"));
  }

  // ── getMatrix: casos válidos ──────────────────────────────

  @Test
  @DisplayName("getMatrix lee filas consecutivas desde el índice 0")
  void testGetMatrixReadsRows() {
    int[][] m = props("m.0", "1,2,3", "m.1", "4,5,6").getMatrix("m");
    assertArrayEquals(new int[][] {{1, 2, 3}, {4, 5, 6}}, m);
  }

  @Test
  @DisplayName("getMatrix con una sola fila")
  void testGetMatrixSingleRow() {
    assertArrayEquals(new int[][] {{9}}, props("m.0", "9").getMatrix("m"));
  }

  @Test
  @DisplayName("getMatrix no confunde prefijos distintos")
  void testGetMatrixPrefixIsolation() {
    PetriNetProperties p = props("matrix.pre.0", "1,1", "matrix.post.0", "2,2");
    assertArrayEquals(new int[][] {{1, 1}}, p.getMatrix("matrix.pre"));
    assertArrayEquals(new int[][] {{2, 2}}, p.getMatrix("matrix.post"));
  }

  // ── getMatrix: casos de error ─────────────────────────────

  @Test
  @DisplayName("getMatrix sin ninguna fila lanza ConfigurationException")
  void testGetMatrixNoRows() {
    assertThrows(ConfigurationException.class, () -> props("otra", "1").getMatrix("m"));
  }

  @Test
  @DisplayName("getMatrix con filas de ancho distinto lanza ConfigurationException")
  void testGetMatrixInconsistentWidth() {
    ConfigurationException e =
        assertThrows(
            ConfigurationException.class, () -> props("m.0", "1,2,3", "m.1", "4,5").getMatrix("m"));
    assertTrue(e.getMessage().contains("1"), "debe indicar qué fila falla");
  }

  @Test
  @DisplayName("getMatrix que empieza en el índice 1 se trata como matriz vacía")
  void testGetMatrixNotStartingAtZero() {
    assertThrows(ConfigurationException.class, () -> props("m.1", "1,2").getMatrix("m"));
  }

  @Test
  @DisplayName("BUG DOCUMENTADO: una fila con índice salteado se descarta en silencio")
  void testGetMatrixSkippedIndexIsSilentlyDropped() {
    // m.1 no existe, así que el conteo se detiene en 1 y m.2 nunca se lee.
    // La matriz queda con menos filas de las que el usuario escribió, sin ningún aviso.
    int[][] m = props("m.0", "1,2", "m.2", "5,6").getMatrix("m");
    assertEquals(1, m.length);
  }

  // ── toDefinition ──────────────────────────────────────────

  @Test
  @DisplayName("toDefinition arma una definición con las dimensiones correctas")
  void testToDefinition() {
    PetriNetDefinition d = validNet().toDefinition();
    assertEquals(2, d.pre().length);
    assertEquals(2, d.pre()[0].length);
    assertArrayEquals(new int[] {1, 0}, d.initialMarking());
    assertArrayEquals(new int[] {0, 0}, d.temporalTransitions());
  }

  @Test
  @DisplayName("toDefinition sin marcado inicial lanza ConfigurationException")
  void testToDefinitionMissingMarking() {
    PetriNetProperties p =
        props(
            "temporary_transitions.vector", "0,0",
            "matrix.pre.0", "1,0",
            "matrix.post.0", "0,1");
    assertThrows(ConfigurationException.class, p::toDefinition);
  }

  @Test
  @DisplayName("toDefinition sin la matriz post lanza ConfigurationException")
  void testToDefinitionMissingPostMatrix() {
    PetriNetProperties p =
        props(
            "initial_marking.vector", "1",
            "temporary_transitions.vector", "0",
            "matrix.pre.0", "1");
    assertThrows(ConfigurationException.class, p::toDefinition);
  }

  @Test
  @DisplayName("toDefinition con dimensiones incoherentes lanza PetriNetValidationException")
  void testToDefinitionInconsistentDimensions() {
    // El marcado tiene 3 elementos pero la red tiene 2 lugares: lo detecta la validación,
    // no el parseo. Distinguir ambas excepciones es lo que permite mapear 500 vs 400.
    PetriNetProperties p =
        props(
            "initial_marking.vector", "1,0,0",
            "temporary_transitions.vector", "0,0",
            "matrix.pre.0", "1,0",
            "matrix.pre.1", "0,1",
            "matrix.post.0", "0,1",
            "matrix.post.1", "1,0");
    assertThrows(PetriNetValidationException.class, p::toDefinition);
  }

  @Test
  @DisplayName("toDefinition con valores negativos lanza PetriNetValidationException")
  void testToDefinitionNegativeWeights() {
    PetriNetProperties p =
        props(
            "initial_marking.vector", "1",
            "temporary_transitions.vector", "0",
            "matrix.pre.0", "-1",
            "matrix.post.0", "1");
    assertThrows(PetriNetValidationException.class, p::toDefinition);
  }

  @Test
  @DisplayName("dos llamadas a toDefinition devuelven instancias independientes")
  void testToDefinitionReturnsFreshInstances() {
    PetriNetProperties p = validNet();
    assertNotSame(p.toDefinition(), p.toDefinition());
  }

  // ── fromResource ──────────────────────────────────────────

  @Test
  @DisplayName("fromResource con recurso inexistente lanza ConfigurationException")
  void testFromResourceMissing() {
    ConfigurationException e =
        assertThrows(
            ConfigurationException.class,
            () -> PetriNetProperties.fromResource("no-existe.properties"));
    assertTrue(e.getMessage().contains("no-existe.properties"));
  }

  @Test
  @DisplayName("fromResource carga un recurso real del classpath")
  void testFromResourceLoadsRealNet() {
    // Usa un recurso que ya existe en el proyecto, así el test no depende de ningún
    // archivo extra. Lo que se verifica acá es la resolución del classpath; el contenido
    // concreto de cada red se testea en PetriNetFixturesTest.
    PetriNetDefinition d =
        PetriNetProperties.fromResource("exampleHuang.properties").toDefinition();
    assertNotNull(d);
    assertEquals(14, d.pre().length);
    assertEquals(10, d.pre()[0].length);
  }
}
