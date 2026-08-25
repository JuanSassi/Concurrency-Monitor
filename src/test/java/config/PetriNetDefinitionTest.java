import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PetriNetDefinition}.
 *
 * <p>Cubre la validación estructural en el constructor compacto, las copias defensivas de entrada y
 * de salida, y los límites de tamaño.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PetriNetDefinition")
class PetriNetDefinitionTest {

  /** Matriz pre de una red mínima válida de 2 lugares y 2 transiciones. */
  private static int[][] pre() {
    return new int[][] {{1, 0}, {0, 1}};
  }

  /** Matriz post correspondiente a {@link #pre()}. */
  private static int[][] post() {
    return new int[][] {{0, 1}, {1, 0}};
  }

  /**
   * Construye la red mínima válida usada como base en la mayoría de los tests.
   *
   * @return una definición 2x2 bien formada
   */
  private static PetriNetDefinition validDefinition() {
    return new PetriNetDefinition(pre(), post(), new int[] {1, 0}, new int[] {0, 0});
  }

  // ── construcción válida ───────────────────────────────────

  @Test
  @DisplayName("una red bien formada se construye sin errores")
  void testValidConstruction() {
    PetriNetDefinition d = validDefinition();
    assertArrayEquals(pre(), d.pre());
    assertArrayEquals(post(), d.post());
    assertArrayEquals(new int[] {1, 0}, d.initialMarking());
    assertArrayEquals(new int[] {0, 0}, d.temporalTransitions());
  }

  @Test
  @DisplayName("una red 1x1 es válida")
  void testMinimalNet() {
    assertDoesNotThrow(
        () ->
            new PetriNetDefinition(
                new int[][] {{1}}, new int[][] {{1}}, new int[] {0}, new int[] {0}));
  }

  @Test
  @DisplayName("una red rectangular (más transiciones que lugares) es válida")
  void testRectangularNet() {
    PetriNetDefinition d =
        new PetriNetDefinition(
            new int[][] {{1, 0, 1}}, new int[][] {{0, 1, 0}}, new int[] {2}, new int[] {0, 0, 0});
    assertEquals(1, d.pre().length);
    assertEquals(3, d.pre()[0].length);
  }

  @Test
  @DisplayName("el marcado inicial puede ser todo ceros")
  void testZeroMarkingAllowed() {
    assertDoesNotThrow(
        () -> new PetriNetDefinition(pre(), post(), new int[] {0, 0}, new int[] {0, 0}));
  }

  // ── copias defensivas ─────────────────────────────────────

  @Test
  @DisplayName("mutar el array original no afecta a la definición")
  void testDefensiveCopyOnConstruction() {
    int[][] originalPre = pre();
    int[] originalMarking = {1, 0};
    PetriNetDefinition d = new PetriNetDefinition(originalPre, post(), originalMarking, new int[] {0, 0});

    originalPre[0][0] = 99;
    originalMarking[0] = 99;

    assertEquals(1, d.pre()[0][0], "pre debe estar aislada del array del llamador");
    assertEquals(1, d.initialMarking()[0], "el marcado debe estar aislado del array del llamador");
  }

  @Test
  @DisplayName("mutar lo devuelto por los accesores no afecta a la definición")
  void testDefensiveCopyOnAccess() {
    PetriNetDefinition d = validDefinition();

    d.pre()[0][0] = 99;
    d.post()[0][0] = 99;
    d.initialMarking()[0] = 99;
    d.temporalTransitions()[0] = 99;

    assertEquals(1, d.pre()[0][0]);
    assertEquals(0, d.post()[0][0]);
    assertEquals(1, d.initialMarking()[0]);
    assertEquals(0, d.temporalTransitions()[0]);
  }

  @Test
  @DisplayName("cada llamada a un accesor devuelve una copia nueva")
  void testAccessorsReturnFreshCopies() {
    PetriNetDefinition d = validDefinition();
    assertNotSame(d.pre(), d.pre());
    assertNotSame(d.initialMarking(), d.initialMarking());
  }

  // ── validación: nulls ─────────────────────────────────────

  @Test
  @DisplayName("pre null lanza PetriNetValidationException")
  void testNullPre() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(null, post(), new int[] {1, 0}, new int[] {0, 0}));
  }

  @Test
  @DisplayName("post null lanza PetriNetValidationException")
  void testNullPost() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(pre(), null, new int[] {1, 0}, new int[] {0, 0}));
  }

  @Test
  @DisplayName("marcado inicial null lanza PetriNetValidationException")
  void testNullMarking() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(pre(), post(), null, new int[] {0, 0}));
  }

  @Test
  @DisplayName("vector temporal null lanza PetriNetValidationException")
  void testNullTemporal() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(pre(), post(), new int[] {1, 0}, null));
  }

  @Test
  @DisplayName("una fila null en post lanza PetriNetValidationException")
  void testNullRowInPost() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetDefinition(
                pre(), new int[][] {{0, 1}, null}, new int[] {1, 0}, new int[] {0, 0}));
  }

  // ── validación: dimensiones ───────────────────────────────

  @Test
  @DisplayName("una red sin lugares lanza PetriNetValidationException")
  void testEmptyPre() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(new int[][] {}, new int[][] {}, new int[] {}, new int[] {}));
  }

  @Test
  @DisplayName("una red sin transiciones lanza PetriNetValidationException")
  void testZeroTransitions() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(new int[][] {{}}, new int[][] {{}}, new int[] {0}, new int[] {}));
  }

  @Test
  @DisplayName("post con distinta cantidad de filas que pre lanza PetriNetValidationException")
  void testPostRowCountMismatch() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(pre(), new int[][] {{0, 1}}, new int[] {1, 0}, new int[] {0, 0}));
  }

  @Test
  @DisplayName("una fila de pre con ancho distinto lanza PetriNetValidationException")
  void testPreRowWidthMismatch() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetDefinition(
                new int[][] {{1, 0}, {0}}, post(), new int[] {1, 0}, new int[] {0, 0}));
  }

  @Test
  @DisplayName("marcado inicial con largo incorrecto lanza PetriNetValidationException")
  void testMarkingLengthMismatch() {
    PetriNetValidationException e =
        assertThrows(
            PetriNetValidationException.class,
            () -> new PetriNetDefinition(pre(), post(), new int[] {1, 0, 0}, new int[] {0, 0}));
    assertTrue(e.getMessage().contains("2"), "el mensaje debe indicar el largo esperado");
  }

  @Test
  @DisplayName("vector temporal con largo incorrecto lanza PetriNetValidationException")
  void testTemporalLengthMismatch() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(pre(), post(), new int[] {1, 0}, new int[] {0}));
  }

  // ── validación: valores negativos ─────────────────────────

  @Test
  @DisplayName("pre con valores negativos lanza PetriNetValidationException")
  void testNegativePre() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetDefinition(
                new int[][] {{-1, 0}, {0, 1}}, post(), new int[] {1, 0}, new int[] {0, 0}));
  }

  @Test
  @DisplayName("post con valores negativos lanza PetriNetValidationException")
  void testNegativePost() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetDefinition(
                pre(), new int[][] {{0, -1}, {1, 0}}, new int[] {1, 0}, new int[] {0, 0}));
  }

  @Test
  @DisplayName("marcado inicial negativo lanza PetriNetValidationException")
  void testNegativeMarking() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(pre(), post(), new int[] {-1, 0}, new int[] {0, 0}));
  }

  // ── límite MAX_DIMENSION ──────────────────────────────────

  @Test
  @DisplayName("una red de exactamente 200x200 es aceptada")
  void testMaxDimensionAccepted() {
    assertDoesNotThrow(() -> square(200));
  }

  @Test
  @DisplayName("201 lugares lanza PetriNetValidationException")
  void testTooManyPlaces() {
    PetriNetValidationException e =
        assertThrows(
            PetriNetValidationException.class,
            () ->
                new PetriNetDefinition(
                    new int[201][1], new int[201][1], new int[201], new int[1]));
    assertTrue(e.getMessage().contains("200"), "el mensaje debe mencionar el límite");
  }

  @Test
  @DisplayName("201 transiciones lanza PetriNetValidationException")
  void testTooManyTransitions() {
    assertThrows(
        PetriNetValidationException.class,
        () -> new PetriNetDefinition(new int[1][201], new int[1][201], new int[1], new int[201]));
  }

  /**
   * Construye una red cuadrada de ceros del tamaño pedido.
   *
   * @param n cantidad de lugares y de transiciones
   * @return una definición n x n válida
   */
  private static PetriNetDefinition square(int n) {
    return new PetriNetDefinition(new int[n][n], new int[n][n], new int[n], new int[n]);
  }

  // ── comportamiento documentado, no deseable ───────────────

  @Test
  @DisplayName("BUG DOCUMENTADO: un tiempo de transición negativo pasa la validación")
  void testNegativeTemporalIsNotRejected() {
    // checkNonNegative se aplica a pre, post y al marcado, pero NO al vector temporal,
    // aunque el javadoc lo documenta como "sleep time in seconds".
    assertDoesNotThrow(
        () -> new PetriNetDefinition(pre(), post(), new int[] {1, 0}, new int[] {-5, 0}));
  }

  @Test
  @DisplayName("BUG DOCUMENTADO: pre[0] null lanza NPE en vez de PetriNetValidationException")
  void testNullFirstRowThrowsNpe() {
    // validateBaseDimensions accede a pre[0].length antes de que checkRowsConsistent
    // pueda detectar la fila null. Se escapa una NullPointerException cruda,
    // que en un contexto web se mapearía a 500 en lugar de 400.
    assertThrows(
        NullPointerException.class,
        () ->
            new PetriNetDefinition(
                new int[][] {null, {0, 1}}, post(), new int[] {1, 0}, new int[] {0, 0}));
  }

  @Test
  @DisplayName("BUG DOCUMENTADO: equals compara por identidad de arrays, no por contenido")
  void testEqualsUsesArrayIdentity() {
    // El equals autogenerado del record usa Object::equals sobre los campos, y los arrays
    // no lo redefinen. Dos redes idénticas no son iguales, y no se pueden usar en Set/Map.
    assertNotEquals(validDefinition(), validDefinition());
  }
}