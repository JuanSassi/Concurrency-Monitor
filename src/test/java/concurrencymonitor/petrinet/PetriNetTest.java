import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PetriNet}.
 *
 * <p>Red de referencia usada en la mayoría de los casos: dos lugares y dos transiciones formando un
 * ciclo. P0 --T0--&gt; P1 --T1--&gt; P0, con un token inicial en P0.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PetriNet")
class PetriNetTest {

  /** Pre de la red de referencia. */
  private static int[][] pre() {
    return new int[][] {{1, 0}, {0, 1}};
  }

  /** Post de la red de referencia. */
  private static int[][] post() {
    return new int[][] {{0, 1}, {1, 0}};
  }

  /**
   * Construye la red de referencia con un token en P0 y ambas transiciones inmediatas.
   *
   * @return la red lista para usar
   */
  private static PetriNet cycle() {
    return new PetriNet(pre(), post(), new int[] {1, 0}, new int[] {0, 0});
  }

  // ── construcción ──────────────────────────────────────────

  @Test
  @DisplayName("dimensiones tomadas de la matriz pre")
  void testDimensions() {
    PetriNet net = new PetriNet(new int[1][3], new int[1][3], new int[1], new int[3]);
    assertEquals(1, net.getNumPlaces());
    assertEquals(3, net.getNumTransitions());
  }

  @Test
  @DisplayName("el marcado inicial se refleja en getMarking")
  void testInitialMarking() {
    assertArrayEquals(new int[] {1, 0}, cycle().getMarking());
  }

  @Test
  @DisplayName("mutar los arrays del llamador no afecta a la red")
  void testDefensiveCopyOnConstruction() {
    int[][] pre = pre();
    int[] m0 = {1, 0};
    int[] temporal = {0, 0};
    PetriNet net = new PetriNet(pre, post(), m0, temporal);

    pre[0][0] = 99;
    m0[0] = 99;
    temporal[0] = 1;

    assertArrayEquals(new int[] {1, 0}, net.getMarking());
    assertFalse(net.isTemporary(0), "el vector temporal debe estar copiado");
  }

  @Test
  @DisplayName("dos instancias son independientes")
  void testInstancesAreIndependent() {
    PetriNet first = cycle();
    PetriNet second = cycle();

    first.fire(0);

    assertArrayEquals(new int[] {0, 1}, first.getMarking());
    assertArrayEquals(new int[] {1, 0}, second.getMarking(), "la segunda red no debe moverse");
  }

  // ── validación de dimensiones ─────────────────────────────

  @Test
  @DisplayName("pre y post con distinta cantidad de filas lanza IllegalArgumentException")
  void testRowMismatch() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PetriNet(new int[2][2], new int[3][2], new int[2], new int[2]));
  }

  @Test
  @DisplayName("pre y post con distinta cantidad de columnas lanza IllegalArgumentException")
  void testColumnMismatch() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PetriNet(new int[2][2], new int[2][3], new int[2], new int[2]));
  }

  @Test
  @DisplayName("marcado de largo incorrecto lanza IllegalArgumentException")
  void testMarkingSizeMismatch() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PetriNet(pre(), post(), new int[] {1, 0, 0}, new int[] {0, 0}));
    assertTrue(e.getMessage().contains("2"), "el mensaje debe indicar el largo esperado");
  }

  @Test
  @DisplayName("vector temporal de largo incorrecto lanza IllegalArgumentException")
  void testTemporalSizeMismatch() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PetriNet(pre(), post(), new int[] {1, 0}, new int[] {0}));
  }

  // ── transitionEnabled ─────────────────────────────────────

  @Test
  @DisplayName("una transición con tokens suficientes está habilitada")
  void testTransitionEnabled() {
    assertTrue(cycle().transitionEnabled(0));
  }

  @Test
  @DisplayName("una transición sin tokens suficientes no está habilitada")
  void testTransitionNotEnabled() {
    assertFalse(cycle().transitionEnabled(1), "P1 arranca vacía");
  }

  @Test
  @DisplayName("un arco de peso mayor a 1 exige esa cantidad de tokens")
  void testWeightedArc() {
    PetriNet net = new PetriNet(new int[][] {{2}}, new int[][] {{0}}, new int[] {1}, new int[] {0});
    assertFalse(net.transitionEnabled(0), "hace falta 2 y hay 1");

    net.setMarking(new int[] {2});
    assertTrue(net.transitionEnabled(0));
  }

  @Test
  @DisplayName("índice de transición fuera de rango lanza IllegalArgumentException")
  void testInvalidTransitionIndex() {
    PetriNet net = cycle();
    assertThrows(IllegalArgumentException.class, () -> net.transitionEnabled(-1));
    assertThrows(IllegalArgumentException.class, () -> net.transitionEnabled(2));
    assertThrows(IllegalArgumentException.class, () -> net.fire(99));
    assertThrows(IllegalArgumentException.class, () -> net.isTemporary(2));
  }

  // ── fire ──────────────────────────────────────────────────

  @Test
  @DisplayName("fire mueve el token según la matriz de incidencia")
  void testFire() {
    PetriNet net = cycle();
    net.fire(0);
    assertArrayEquals(new int[] {0, 1}, net.getMarking());
    net.fire(1);
    assertArrayEquals(new int[] {1, 0}, net.getMarking(), "el ciclo vuelve al inicio");
  }

  @Test
  @DisplayName("fire sobre una transición deshabilitada lanza IllegalStateException")
  void testFireDisabled() {
    PetriNet net = cycle();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> net.fire(1));
    assertTrue(e.getMessage().contains("1"));
    assertArrayEquals(new int[] {1, 0}, net.getMarking(), "el marcado no debe cambiar");
  }

  @Test
  @DisplayName("fire conserva el total de tokens en una red conservativa")
  void testFireConservesTokens() {
    PetriNet net = cycle();
    for (int i = 0; i < 6; i++) {
      net.fire(i % 2);
      assertEquals(1, net.getMarking()[0] + net.getMarking()[1]);
    }
  }

  // ── consumeTokens / produceTokens ─────────────────────────

  @Test
  @DisplayName("consume y produce equivalen a un fire en dos pasos")
  void testTwoPhaseFiring() {
    PetriNet net = cycle();

    net.consumeTokens(0);
    assertArrayEquals(new int[] {0, 0}, net.getMarking(), "el token está en tránsito");

    net.produceTokens(0);
    assertArrayEquals(new int[] {0, 1}, net.getMarking());
  }

  @Test
  @DisplayName("consumeTokens sobre una transición deshabilitada lanza IllegalStateException")
  void testConsumeDisabled() {
    PetriNet net = cycle();
    assertThrows(IllegalStateException.class, () -> net.consumeTokens(1));
  }

  @Test
  @DisplayName("produceTokens no verifica habilitación: crea tokens de la nada")
  void testProduceDoesNotCheckEnabled() {
    // Es la fase 2 de un disparo temporal, así que por diseño no re-verifica. La contracara
    // es que llamarla sin su consumeTokens previo inventa tokens sin ningún aviso.
    PetriNet net = cycle();
    net.produceTokens(0);
    assertArrayEquals(new int[] {1, 1}, net.getMarking(), "apareció un token extra");
  }

  // ── marcado ───────────────────────────────────────────────

  @Test
  @DisplayName("getMarking devuelve una copia")
  void testGetMarkingIsCopy() {
    PetriNet net = cycle();
    int[] marking = net.getMarking();
    marking[0] = 99;

    assertArrayEquals(new int[] {1, 0}, net.getMarking());
    assertNotSame(net.getMarking(), net.getMarking());
  }

  @Test
  @DisplayName("setMarking copia el array recibido")
  void testSetMarkingIsCopy() {
    PetriNet net = cycle();
    int[] newMarking = {0, 1};
    net.setMarking(newMarking);

    newMarking[0] = 99;

    assertArrayEquals(new int[] {0, 1}, net.getMarking());
  }

  @Test
  @DisplayName("setMarking con largo incorrecto lanza IllegalArgumentException")
  void testSetMarkingWrongSize() {
    PetriNet net = cycle();
    assertThrows(IllegalArgumentException.class, () -> net.setMarking(new int[] {1}));
  }

  @Test
  @DisplayName("setMarking no rechaza marcados negativos")
  void testSetMarkingAcceptsNegative() {
    // Sólo valida el largo. Un marcado negativo entra sin protestar y deja la red en un
    // estado que ninguna secuencia de disparos podría producir.
    PetriNet net = cycle();
    assertDoesNotThrow(() -> net.setMarking(new int[] {-5, 0}));
    assertEquals(-5, net.getMarking()[0]);
  }

  @Test
  @DisplayName("reset vuelve al marcado con el que se construyó la red")
  void testReset() {
    PetriNet net = cycle();
    net.fire(0);
    net.setMarking(new int[] {7, 7});

    net.reset();

    assertArrayEquals(new int[] {1, 0}, net.getMarking());
  }

  @Test
  @DisplayName("reset usa el marcado propio, no el de config.properties")
  void testResetIsInstanceLocal() {
    PetriNet net = new PetriNet(pre(), post(), new int[] {0, 1}, new int[] {0, 0});
    net.fire(1);
    net.reset();
    assertArrayEquals(new int[] {0, 1}, net.getMarking());
  }

  // ── transiciones temporales ───────────────────────────────

  @Test
  @DisplayName("isTemporary distingue transiciones temporizadas de inmediatas")
  void testIsTemporary() {
    PetriNet net = new PetriNet(pre(), post(), new int[] {1, 0}, new int[] {0, 3});
    assertFalse(net.isTemporary(0));
    assertTrue(net.isTemporary(1), "cualquier valor distinto de 0 marca la transición como temporal");
  }

  // ── integración con la configuración ──────────────────────

  @Test
  @DisplayName("fromProperties construye la red configurada")
  void testFromProperties() {
    PetriNet net = PetriNet.fromProperties();
    assertEquals(PetrinetLoader.getNumPlaces(), net.getNumPlaces());
    assertEquals(PetrinetLoader.getNumTransitions(), net.getNumTransitions());
    assertArrayEquals(PetrinetLoader.getInitialMarkingVector(), net.getMarking());
  }

  @Test
  @DisplayName("fromProperties devuelve instancias independientes")
  void testFromPropertiesIsNotSingleton() {
    PetriNet first = PetriNet.fromProperties();
    PetriNet second = PetriNet.fromProperties();

    assertNotSame(first, second);
    first.setMarking(new int[first.getNumPlaces()]);
    assertArrayEquals(
        PetrinetLoader.getInitialMarkingVector(), second.getMarking(), "no comparten marcado");
  }

  // ── comportamiento documentado, no deseable ───────────────

  @Test
  @DisplayName("BUG DOCUMENTADO: una red vacía revienta con ArrayIndexOutOfBoundsException")
  void testEmptyNetCrashes() {
    // numPlaces y numTransitions quedan en 0, todas las validaciones pasan, y recién
    // Matrix.subtract falla al hacer a[0].length. El javadoc promete
    // IllegalArgumentException para dimensiones inconsistentes.
    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> new PetriNet(new int[][] {}, new int[][] {}, new int[] {}, new int[] {}));
  }

  @Test
  @DisplayName("BUG DOCUMENTADO: filas de distinto ancho no se validan")
  void testRaggedMatrixCrashes() {
    // validateColumnsMatch sólo compara pre[0] con post[0]; las filas siguientes no se
    // miran. Una matriz irregular revienta después, con una excepción sin contexto.
    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () ->
            new PetriNet(
                new int[][] {{1, 0}, {1}}, post(), new int[] {1, 0}, new int[] {0, 0}));
  }
}