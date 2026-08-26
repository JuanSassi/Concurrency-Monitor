import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ReachabilityTree}.
 *
 * <p>El árbol se construye entero en el constructor mediante BFS, así que cada test arma una red
 * chica cuyo espacio de estados se puede enumerar a mano y comparar.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("ReachabilityTree")
class ReachabilityTreeTest {

  /**
   * Construye una red de dos lugares en ciclo con el marcado indicado.
   *
   * @param tokens tokens iniciales en P0
   * @return la red lista para explorar
   */
  private static PetriNet cycle(int tokens) {
    return new PetriNet(
        new int[][] {{1, 0}, {0, 1}},
        new int[][] {{0, 1}, {1, 0}},
        new int[] {tokens, 0},
        new int[] {0, 0});
  }

  // ── exploración básica ────────────────────────────────────

  @Test
  @DisplayName("un ciclo con un token tiene dos marcados alcanzables")
  void testSimpleCycle() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(1));

    assertEquals(2, tree.getNumReachableMarkings(), "el token está en P0 o en P1");
    assertEquals(1, tree.getMaxNumThreads());
  }

  @Test
  @DisplayName("con dos tokens el máximo de hilos es dos")
  void testTwoTokens() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(2));

    assertEquals(3, tree.getNumReachableMarkings(), "(2,0), (1,1) y (0,2)");
    assertEquals(2, tree.getMaxNumThreads());
  }

  @Test
  @DisplayName("una red sin transiciones habilitadas tiene un solo marcado")
  void testDeadNet() {
    PetriNet dead =
        new PetriNet(new int[][] {{1}}, new int[][] {{0}}, new int[] {0}, new int[] {0});
    ReachabilityTree tree = new ReachabilityTree(Set.of(0), dead);

    assertEquals(1, tree.getNumReachableMarkings(), "sólo el marcado inicial");
    assertEquals(0, tree.getMaxNumThreads());
  }

  @Test
  @DisplayName("el marcado inicial siempre es el primero descubierto")
  void testInitialMarkingIsFirst() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(1));
    assertArrayEquals(new int[] {1, 0}, tree.getReachableMarkings().get(0));
  }

  @Test
  @DisplayName("no se repiten marcados en el conjunto alcanzable")
  void testNoDuplicateMarkings() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(3));

    Set<String> seen = new java.util.HashSet<>();
    for (int[] marking : tree.getReachableMarkings()) {
      assertTrue(seen.add(java.util.Arrays.toString(marking)), "marcado duplicado");
    }
  }

  // ── proyección sobre plazas de acción ─────────────────────

  @Test
  @DisplayName("cada marcado se proyecta solo sobre las plazas de acción")
  void testProjectionOntoActionPlaces() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(1), cycle(1));

    assertEquals(List.of(1), tree.getSortedActionPlaces());
    for (int[] marking : tree.getReachableMarkings()) {
      assertEquals(1, marking.length, "sólo se guarda P1");
    }
  }

  @Test
  @DisplayName("las plazas de acción quedan ordenadas de forma ascendente")
  void testActionPlacesAreSorted() {
    ReachabilityTree tree = new ReachabilityTree(new java.util.HashSet<>(List.of(1, 0)), cycle(1));
    assertEquals(List.of(0, 1), tree.getSortedActionPlaces());
  }

  @Test
  @DisplayName("sin plazas de acción el máximo de hilos es cero")
  void testNoActionPlaces() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(), cycle(1));

    assertEquals(0, tree.getMaxNumThreads());
    assertEquals(2, tree.getNumReachableMarkings(), "el espacio de estados igual se explora");
  }

  @Test
  @DisplayName("un índice de plaza fuera de rango se ignora en vez de romper")
  void testOutOfRangePlaceIsIgnored() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 99), cycle(1));
    assertEquals(1, tree.getMaxNumThreads(), "P99 no existe y aporta 0");
  }

  // ── calculateMaxThreadsInSegment ──────────────────────────

  @Test
  @DisplayName("un segmento vacío devuelve 1 por convención")
  void testEmptySegmentReturnsOne() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(2));
    assertEquals(1, tree.calculateMaxThreadsInSegment(List.of()));
  }

  @Test
  @DisplayName("el máximo de un segmento suma solo sus plazas")
  void testMaxThreadsInSegment() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(2));

    assertEquals(2, tree.calculateMaxThreadsInSegment(List.of(0)), "P0 llega a tener 2 tokens");
    assertEquals(2, tree.calculateMaxThreadsInSegment(List.of(0, 1)));
  }

  @Test
  @DisplayName("una plaza que no es de acción no aporta al máximo del segmento")
  void testSegmentWithNonActionPlace() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0), cycle(2));
    assertEquals(0, tree.calculateMaxThreadsInSegment(List.of(1)), "P1 no se registró");
  }

  // ── estado del PetriNet ───────────────────────────────────

  @Test
  @DisplayName("la red queda restaurada al marcado inicial después de construir el árbol")
  void testNetIsResetAfterConstruction() {
    // Sin esto, quien reutilice la instancia (por ejemplo un Monitor) arrancaría desde
    // un marcado arbitrario del recorrido BFS.
    PetriNet net = cycle(1);
    new ReachabilityTree(Set.of(0, 1), net);

    assertArrayEquals(new int[] {1, 0}, net.getMarking());
  }

  @Test
  @DisplayName("getReachableMarkings devuelve copias defensivas")
  void testMarkingsAreDefensiveCopies() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(1));

    tree.getReachableMarkings().get(0)[0] = 99;

    assertArrayEquals(new int[] {1, 0}, tree.getReachableMarkings().get(0));
    assertNotSame(tree.getReachableMarkings().get(0), tree.getReachableMarkings().get(0));
  }

  @Test
  @DisplayName("getSortedActionPlaces es inmutable")
  void testSortedActionPlacesIsUnmodifiable() {
    ReachabilityTree tree = new ReachabilityTree(Set.of(0, 1), cycle(1));
    assertThrows(UnsupportedOperationException.class, () -> tree.getSortedActionPlaces().add(9));
  }

  // ── transiciones temporales ───────────────────────────────

  @Test
  @DisplayName("una transición temporal se explora igual que una inmediata")
  void testTemporalTransitionIsExplored() {
    // fireTransition usa consumeTokens + produceTokens para las temporales, lo que da el
    // mismo marcado resultante que un fire atómico.
    PetriNet immediate = cycle(1);
    PetriNet temporal =
        new PetriNet(
            new int[][] {{1, 0}, {0, 1}},
            new int[][] {{0, 1}, {1, 0}},
            new int[] {1, 0},
            new int[] {1, 1});

    ReachabilityTree fromImmediate = new ReachabilityTree(Set.of(0, 1), immediate);
    ReachabilityTree fromTemporal = new ReachabilityTree(Set.of(0, 1), temporal);

    assertEquals(fromImmediate.getNumReachableMarkings(), fromTemporal.getNumReachableMarkings());
    assertEquals(fromImmediate.getMaxNumThreads(), fromTemporal.getMaxNumThreads());
  }

  // ── protección contra redes no acotadas ───────────────────

  @Test
  @DisplayName("una red no acotada se corta con PetriNetValidationException")
  void testUnboundedNetIsRejected() {
    // T0 produce sin consumir: el conjunto alcanzable es infinito. Sin el tope, el BFS
    // no terminaría nunca y se comería el heap por el camino.
    PetriNet unbounded =
        new PetriNet(new int[][] {{0}}, new int[][] {{1}}, new int[] {0}, new int[] {0});

    PetriNetValidationException e =
        assertThrows(
            PetriNetValidationException.class, () -> new ReachabilityTree(Set.of(0), unbounded));
    assertTrue(e.getMessage().contains("50000"), "el mensaje debe citar el tope");
  }

  // ── redes reales ──────────────────────────────────────────

  @Test
  @DisplayName("GOLDEN: exampleHuang tiene 26 marcados alcanzables y hasta 3 hilos activos")
  void testHuang() {
    ReachabilityTree tree = treeOf("exampleHuang.properties", Set.of(1, 2, 3, 4, 8, 9, 10));

    assertEquals(26, tree.getNumReachableMarkings());
    assertEquals(3, tree.getMaxNumThreads());
    assertEquals(List.of(1, 2, 3, 4, 8, 9, 10), tree.getSortedActionPlaces());
  }

  @Test
  @DisplayName("GOLDEN: travelAgencySystem tiene 618 marcados alcanzables y hasta 5 hilos")
  void testTravelAgency() {
    ReachabilityTree tree =
        treeOf("travelAgencySystem.properties", Set.of(2, 3, 5, 8, 9, 11, 12, 13, 14));

    assertEquals(618, tree.getNumReachableMarkings());
    assertEquals(5, tree.getMaxNumThreads());
  }

  @Test
  @DisplayName("el máximo global es mayor o igual al de cualquier segmento")
  void testGlobalMaxDominatesSegments() {
    ReachabilityTree tree = treeOf("exampleHuang.properties", Set.of(1, 2, 3, 4, 8, 9, 10));

    for (Integer place : tree.getSortedActionPlaces()) {
      assertTrue(
          tree.calculateMaxThreadsInSegment(List.of(place)) <= tree.getMaxNumThreads(),
          "un segmento no puede superar el máximo global");
    }
  }

  /**
   * Construye el árbol de alcanzabilidad de una red del classpath.
   *
   * @param resource el nombre del recurso
   * @param actionPlaces las plazas de acción a seguir
   * @return el árbol ya construido
   */
  private static ReachabilityTree treeOf(String resource, Set<Integer> actionPlaces) {
    PetriNetDefinition d = PetriNetProperties.fromResource(resource).toDefinition();
    PetriNet net = new PetriNet(d.pre(), d.post(), d.initialMarking(), d.temporalTransitions());
    return new ReachabilityTree(actionPlaces, net);
  }
}
