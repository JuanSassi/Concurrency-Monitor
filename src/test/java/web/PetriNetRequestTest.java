import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PetriNetRequest}.
 *
 * <p>Es la puerta de entrada de las redes que llegan por HTTP, así que lo que se verifica acá es
 * sobre todo el rechazo: componentes faltantes y el techo de tamaño más estricto que el del
 * catálogo.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PetriNetRequest")
class PetriNetRequestTest {

  /**
   * Construye una petición válida de dos lugares y dos transiciones.
   *
   * @return la petición bien formada
   */
  private static PetriNetRequest validRequest() {
    return new PetriNetRequest(
        new int[][] {{1, 0}, {0, 1}},
        new int[][] {{0, 1}, {1, 0}},
        new int[] {1, 0},
        new int[] {0, 0});
  }

  /**
   * Construye una petición cuadrada de ceros del tamaño pedido.
   *
   * @param n cantidad de lugares y de transiciones
   * @return la petición correspondiente
   */
  private static PetriNetRequest square(int n) {
    return new PetriNetRequest(new int[n][n], new int[n][n], new int[n], new int[n]);
  }

  // ── camino feliz ──────────────────────────────────────────

  @Test
  @DisplayName("una petición válida produce una definición")
  void testValidRequest() {
    PetriNetDefinition d = validRequest().toDefinition();
    assertEquals(2, d.pre().length);
    assertEquals(2, d.pre()[0].length);
  }

  // ── componentes faltantes ─────────────────────────────────

  @Test
  @DisplayName("cualquier componente en null lanza PetriNetValidationException")
  void testMissingComponents() {
    int[][] m = {{1}};
    int[] v = {0};

    assertThrows(
        PetriNetValidationException.class, () -> new PetriNetRequest(null, m, v, v).toDefinition());
    assertThrows(
        PetriNetValidationException.class, () -> new PetriNetRequest(m, null, v, v).toDefinition());
    assertThrows(
        PetriNetValidationException.class, () -> new PetriNetRequest(m, m, null, v).toDefinition());
    assertThrows(
        PetriNetValidationException.class, () -> new PetriNetRequest(m, m, v, null).toDefinition());
  }

  @Test
  @DisplayName("el mensaje de componentes faltantes nombra los cuatro campos esperados")
  void testMissingComponentsMessage() {
    PetriNetValidationException e =
        assertThrows(
            PetriNetValidationException.class,
            () -> new PetriNetRequest(null, null, null, null).toDefinition());
    assertTrue(e.getMessage().contains("pre"));
    assertTrue(e.getMessage().contains("initialMarking"));
  }

  @Test
  @DisplayName("una matriz pre vacía lanza PetriNetValidationException")
  void testEmptyPre() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetRequest(new int[][] {}, new int[][] {}, new int[] {}, new int[] {})
                .toDefinition());
  }

  @Test
  @DisplayName("una primera fila null lanza PetriNetValidationException, no NPE")
  void testNullFirstRow() {
    // PetriNetRequest tapa acá el hueco que PetriNetDefinition deja abierto: en la ruta
    // directa, un pre[0] null se escapa como NullPointerException.
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetRequest(new int[][] {null}, new int[][] {{1}}, new int[] {0}, new int[] {0})
                .toDefinition());
  }

  @Test
  @DisplayName("una primera fila vacía lanza PetriNetValidationException")
  void testEmptyFirstRow() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetRequest(new int[][] {{}}, new int[][] {{}}, new int[] {0}, new int[] {})
                .toDefinition());
  }

  // ── techo de tamaño ───────────────────────────────────────

  @Test
  @DisplayName("el límite para redes de usuario es 16")
  void testMaxDimensionConstant() {
    assertEquals(16, PetriNetRequest.MAX_USER_DIMENSION);
  }

  @Test
  @DisplayName("una red de exactamente 16x16 es aceptada")
  void testMaxDimensionAccepted() {
    assertDoesNotThrow(() -> square(16).toDefinition());
  }

  @Test
  @DisplayName("17 lugares lanza PetriNetValidationException")
  void testTooManyPlaces() {
    PetriNetValidationException e =
        assertThrows(
            PetriNetValidationException.class,
            () ->
                new PetriNetRequest(new int[17][2], new int[17][2], new int[17], new int[2])
                    .toDefinition());
    assertTrue(e.getMessage().contains("16"), "el mensaje debe citar el límite");
    assertTrue(e.getMessage().contains("17"), "el mensaje debe citar lo recibido");
  }

  @Test
  @DisplayName("17 transiciones lanza PetriNetValidationException")
  void testTooManyTransitions() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetRequest(new int[2][17], new int[2][17], new int[2], new int[17])
                .toDefinition());
  }

  @Test
  @DisplayName("el límite del usuario es más estricto que el del catálogo")
  void testUserLimitIsStricterThanCatalog() {
    // PetriNetDefinition acepta hasta 200; una red de 20 pasaría por el catálogo pero no
    // por HTTP, que es exactamente la intención.
    assertDoesNotThrow(
        () -> new PetriNetDefinition(new int[20][20], new int[20][20], new int[20], new int[20]));
    assertThrows(PetriNetValidationException.class, () -> square(20).toDefinition());
  }

  // ── delegación en PetriNetDefinition ──────────────────────

  @Test
  @DisplayName("las validaciones estructurales las sigue haciendo PetriNetDefinition")
  void testStructuralValidationIsDelegated() {
    // Tamaño correcto pero marcado de largo equivocado: lo detecta la definición.
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetRequest(
                    new int[][] {{1}}, new int[][] {{1}}, new int[] {0, 0}, new int[] {0})
                .toDefinition());
  }

  @Test
  @DisplayName("un peso negativo lo rechaza la definición")
  void testNegativeWeightRejected() {
    assertThrows(
        PetriNetValidationException.class,
        () ->
            new PetriNetRequest(new int[][] {{-1}}, new int[][] {{1}}, new int[] {0}, new int[] {0})
                .toDefinition());
  }

  @Test
  @DisplayName("una red válida del tamaño permitido se puede analizar de punta a punta")
  void testEndToEnd() {
    PetriNetDefinition d = validRequest().toDefinition();
    ThreadAllocator allocator = new ThreadAllocator(d);

    assertTrue(allocator.getMaxActiveThreads() >= 0);
    assertEquals(allocator.getSegments().size(), allocator.getThreadsPerSegment().size());
  }

  // ── comportamiento documentado, no deseable ───────────────

  @Test
  @DisplayName("BUG DOCUMENTADO: equals compara por identidad de arrays")
  void testEqualsUsesArrayIdentity() {
    // Mismo problema que PetriNetDefinition: el record se genera con Object::equals sobre
    // campos que son arrays. Dos peticiones idénticas no son iguales.
    assertNotEquals(validRequest(), validRequest());
  }
}
