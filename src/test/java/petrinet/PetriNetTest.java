import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PetriNet using the Huang S3PR net (exampleHuang.properties).
 *
 * <p>Expected values for the Huang net:
 *
 * <ul>
 *   <li>14 places (P0-P13), 10 transitions (T0-T9)
 *   <li>Initial marking: [2,0,1,0,0,0,1,2,0,0,0,1,1,1]
 *   <li>All transitions immediate: [0,0,0,0,0,0,0,0,0,0]
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PetriNet — exampleHuang")
class PetriNetTest {

  /** Initial marking of the Huang net. */
  private static final int[] INITIAL_MARKING = {2, 0, 1, 0, 0, 0, 1, 2, 0, 0, 0, 1, 1, 1};

  /** Fresh PetriNet instance built before every test — no shared global state between tests. */
  private PetriNet net;

  @BeforeEach
  void setUp() {
    net =
        new PetriNet(
            PetrinetLoader.getPreMatrix(),
            PetrinetLoader.getPostMatrix(),
            PetrinetLoader.getInitialMarkingVector(),
            PetrinetLoader.getTemporalTransitionsVector());
  }

  // ── construcción ──────────────────────────────────────────

  @Test
  @DisplayName("el constructor produce una instancia no nula")
  void testConstructorProducesNonNullInstance() {
    assertNotNull(net);
  }

  @Test
  @DisplayName("dos instancias construidas por separado son independientes, no un singleton")
  void testInstancesAreIndependent() {
    PetriNet other =
        new PetriNet(
            PetrinetLoader.getPreMatrix(),
            PetrinetLoader.getPostMatrix(),
            PetrinetLoader.getInitialMarkingVector(),
            PetrinetLoader.getTemporalTransitionsVector());

    assertNotSame(
        net, other, "Dos instancias construidas por separado no deben ser el mismo objeto");

    net.fire(0);
    assertArrayEquals(
        INITIAL_MARKING,
        other.getMarking(),
        "Disparar una transición en una instancia no debe afectar a la otra");
  }

  // ── dimensiones ───────────────────────────────────────────

  @Test
  @DisplayName("getNumPlaces → 14")
  void testGetNumPlaces() {
    assertEquals(14, net.getNumPlaces());
  }

  @Test
  @DisplayName("getNumTransitions → 10")
  void testGetNumTransitions() {
    assertEquals(10, net.getNumTransitions());
  }

  // ── marcado inicial ───────────────────────────────────────

  @Test
  @DisplayName("getMarking inicial → [2,0,1,0,0,0,1,2,0,0,0,1,1,1]")
  void testInitialMarking() {
    assertArrayEquals(INITIAL_MARKING, net.getMarking());
  }

  @Test
  @DisplayName("getMarking retorna copia defensiva — modificarla no afecta el estado interno")
  void testGetMarkingIsDefensiveCopy() {
    int[] marking = net.getMarking();
    marking[0] = 999;
    assertArrayEquals(INITIAL_MARKING, net.getMarking());
  }

  // ── transiciones temporales ───────────────────────────────

  @Test
  @DisplayName("isTemporary → false para todas las transiciones (red Huang es inmediata)")
  void testAllTransitionsImmediate() {
    for (int t = 0; t < net.getNumTransitions(); t++) {
      assertFalse(net.isTemporary(t), "T" + t + " no debería ser temporal");
    }
  }

  @Test
  @DisplayName("isTemporary con índice negativo lanza IllegalArgumentException")
  void testIsTemporaryNegativeIndex() {
    assertThrows(IllegalArgumentException.class, () -> net.isTemporary(-1));
  }

  @Test
  @DisplayName("isTemporary con índice fuera de rango lanza IllegalArgumentException")
  void testIsTemporaryOutOfRange() {
    assertThrows(IllegalArgumentException.class, () -> net.isTemporary(net.getNumTransitions()));
  }

  // ── transitionEnabled ─────────────────────────────────────

  @Test
  @DisplayName("T0 habilitada en marcado inicial (P0 tiene 2 tokens, Pre[P0][T0]=1)")
  void testT0EnabledInInitialMarking() {
    assertTrue(net.transitionEnabled(0));
  }

  @Test
  @DisplayName("T1 no habilitada en marcado inicial (P1 tiene 0 tokens)")
  void testT1NotEnabledInInitialMarking() {
    assertFalse(net.transitionEnabled(1));
  }

  @Test
  @DisplayName("transitionEnabled con índice negativo lanza IllegalArgumentException")
  void testTransitionEnabledNegativeIndex() {
    assertThrows(IllegalArgumentException.class, () -> net.transitionEnabled(-1));
  }

  @Test
  @DisplayName("transitionEnabled con índice fuera de rango lanza IllegalArgumentException")
  void testTransitionEnabledOutOfRange() {
    assertThrows(
        IllegalArgumentException.class, () -> net.transitionEnabled(net.getNumTransitions()));
  }

  // ── fire ──────────────────────────────────────────────────

  @Test
  @DisplayName("fire T0 cambia el marcado correctamente")
  void testFireT0ChangesMarking() {
    net.fire(0);
    int[] marking = net.getMarking();
    // T0: consume P0 (2→1), produce P1 (0→1) según la red de Huang
    assertEquals(1, marking[0], "P0 debe pasar de 2 a 1");
    assertEquals(1, marking[1], "P1 debe pasar de 0 a 1");
  }

  @Test
  @DisplayName("fire transición no habilitada lanza IllegalStateException")
  void testFireNotEnabledThrows() {
    assertThrows(IllegalStateException.class, () -> net.fire(1));
  }

  @Test
  @DisplayName("fire con índice negativo lanza IllegalArgumentException")
  void testFireNegativeIndex() {
    assertThrows(IllegalArgumentException.class, () -> net.fire(-1));
  }

  @Test
  @DisplayName("fire con índice fuera de rango lanza IllegalArgumentException")
  void testFireOutOfRange() {
    assertThrows(IllegalArgumentException.class, () -> net.fire(net.getNumTransitions()));
  }

  // ── consumeTokens / produceTokens ─────────────────────────

  @Test
  @DisplayName("consumeTokens T0 reduce tokens de P0")
  void testConsumeTokensT0() {
    net.consumeTokens(0);
    assertEquals(1, net.getMarking()[0], "P0 debe pasar de 2 a 1 tras consumir");
  }

  @Test
  @DisplayName("produceTokens T0 tras consumeTokens produce tokens en P1")
  void testProduceTokensT0() {
    net.consumeTokens(0);
    net.produceTokens(0);
    int[] marking = net.getMarking();
    assertEquals(1, marking[0], "P0 debe tener 1 token");
    assertEquals(1, marking[1], "P1 debe tener 1 token");
  }

  @Test
  @DisplayName("consumeTokens transición no habilitada lanza IllegalStateException")
  void testConsumeTokensNotEnabledThrows() {
    assertThrows(IllegalStateException.class, () -> net.consumeTokens(1));
  }

  @Test
  @DisplayName("consumeTokens con índice negativo lanza IllegalArgumentException")
  void testConsumeTokensNegativeIndex() {
    assertThrows(IllegalArgumentException.class, () -> net.consumeTokens(-1));
  }

  @Test
  @DisplayName("produceTokens con índice negativo lanza IllegalArgumentException")
  void testProduceTokensNegativeIndex() {
    assertThrows(IllegalArgumentException.class, () -> net.produceTokens(-1));
  }

  // ── setMarking ────────────────────────────────────────────

  @Test
  @DisplayName("setMarking actualiza el marcado correctamente")
  void testSetMarking() {
    int[] newMarking = new int[14];
    newMarking[0] = 5;
    net.setMarking(newMarking);
    assertEquals(5, net.getMarking()[0]);
  }

  @Test
  @DisplayName("setMarking hace copia defensiva — modificar el array original no afecta el estado")
  void testSetMarkingIsDefensiveCopy() {
    int[] newMarking = new int[14];
    newMarking[0] = 3;
    net.setMarking(newMarking);
    newMarking[0] = 999;
    assertEquals(3, net.getMarking()[0]);
  }

  @Test
  @DisplayName("setMarking con tamaño incorrecto lanza IllegalArgumentException")
  void testSetMarkingWrongSizeThrows() {
    assertThrows(IllegalArgumentException.class, () -> net.setMarking(new int[5]));
  }

  // ── reset ─────────────────────────────────────────────────

  @Test
  @DisplayName("reset restaura el marcado inicial después de disparar")
  void testResetRestoresInitialMarking() {
    net.fire(0);
    net.reset();
    assertArrayEquals(INITIAL_MARKING, net.getMarking());
  }

  @Test
  @DisplayName("reset permite volver a habilitar T0")
  void testResetReenablesTransitions() {
    net.fire(0);
    net.reset();
    assertTrue(net.transitionEnabled(0));
  }
}
