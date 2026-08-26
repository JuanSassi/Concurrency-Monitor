import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Responsibilities}.
 *
 * <p>El constructor recibe los T-invariantes y las plazas de acción ya calculados, así que casi
 * todo se puede testear con matrices y listas escritas a mano, sin construir {@link Invariants}.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Responsibilities")
class ResponsibilitiesTest {

  // ── forks y joins ─────────────────────────────────────────

  @Test
  @DisplayName("una plaza con dos transiciones de salida es un fork")
  void testFork() {
    // P0 alimenta a T0 y T1: split paralelo.
    int[][] pre = {{1, 1}, {0, 0}};
    int[][] post = {{0, 0}, {1, 0}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of(0, 1));

    assertEquals(List.of(0), r.getForkPlaces());
    assertTrue(r.getJoinPlaces().isEmpty());
  }

  @Test
  @DisplayName("una plaza con dos transiciones de entrada es un join")
  void testJoin() {
    int[][] pre = {{0, 0}, {1, 0}};
    int[][] post = {{1, 1}, {0, 0}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of(0, 1));

    assertEquals(List.of(0), r.getJoinPlaces());
    assertTrue(r.getForkPlaces().isEmpty());
  }

  @Test
  @DisplayName("una plaza puede ser fork y join a la vez")
  void testForkAndJoin() {
    // P0 recibe de T0 y T1, y alimenta a T0 y T1.
    int[][] pre = {{1, 1}};
    int[][] post = {{1, 1}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of(0));

    assertEquals(List.of(0), r.getForkPlaces());
    assertEquals(List.of(0), r.getJoinPlaces());
  }

  @Test
  @DisplayName("una plaza con una sola entrada y una sola salida no es ni fork ni join")
  void testPlainPlace() {
    int[][] pre = {{1, 0}};
    int[][] post = {{0, 1}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of(0));

    assertTrue(r.getForkPlaces().isEmpty());
    assertTrue(r.getJoinPlaces().isEmpty());
  }

  @Test
  @DisplayName("solo se analizan las plazas de acción")
  void testOnlyActionPlacesAreConsidered() {
    // P0 es estructuralmente un fork, pero no está en el conjunto de plazas de acción.
    int[][] pre = {{1, 1}, {1, 1}};
    int[][] post = {{0, 0}, {0, 0}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of(1));

    assertEquals(List.of(1), r.getForkPlaces());
    assertFalse(r.getForkPlaces().contains(0), "P0 no es plaza de acción, no debe reportarse");
  }

  @Test
  @DisplayName("sin plazas de acción no hay forks ni joins")
  void testNoActionPlaces() {
    int[][] pre = {{1, 1}};
    int[][] post = {{1, 1}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of());

    assertTrue(r.getForkPlaces().isEmpty());
    assertTrue(r.getJoinPlaces().isEmpty());
  }

  // ── segmentación ──────────────────────────────────────────

  @Test
  @DisplayName("un T-invariante secuencial forma un único segmento")
  void testSequentialInvariantIsOneSegment() {
    int[][] pre = {{1, 0}};
    int[][] post = {{0, 1}};
    Responsibilities r = new Responsibilities(pre, post, List.of(List.of(1, 1)), Set.of(0));

    assertEquals(List.of(List.of(0, 1)), r.getSegments());
  }

  @Test
  @DisplayName("dos T-invariantes disjuntos dan dos segmentos")
  void testTwoDisjointInvariants() {
    int[][] pre = {{1, 0, 0, 0}, {0, 0, 1, 0}};
    int[][] post = {{0, 1, 0, 0}, {0, 0, 0, 1}};
    Responsibilities r =
        new Responsibilities(
            pre, post, List.of(List.of(1, 1, 0, 0), List.of(0, 0, 1, 1)), Set.of(0, 1));

    assertEquals(List.of(List.of(0, 1), List.of(2, 3)), r.getSegments());
  }

  @Test
  @DisplayName("el segmento solo incluye las transiciones con coeficiente positivo")
  void testSegmentUsesActiveTransitionsOnly() {
    int[][] pre = {{1, 0, 0}};
    int[][] post = {{0, 1, 0}};
    Responsibilities r = new Responsibilities(pre, post, List.of(List.of(1, 1, 0)), Set.of(0));

    assertEquals(List.of(List.of(0, 1)), r.getSegments(), "T2 tiene coeficiente 0");
  }

  @Test
  @DisplayName("un T-invariante nulo no genera segmento")
  void testZeroInvariantProducesNoSegment() {
    int[][] pre = {{1, 0}};
    int[][] post = {{0, 1}};
    Responsibilities r = new Responsibilities(pre, post, List.of(List.of(0, 0)), Set.of(0));

    assertTrue(r.getSegments().isEmpty());
  }

  @Test
  @DisplayName("sin T-invariantes no hay segmentos")
  void testNoInvariantsNoSegments() {
    int[][] pre = {{1, 0}};
    int[][] post = {{0, 1}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of(0));

    assertTrue(r.getSegments().isEmpty());
  }

  @Test
  @DisplayName("los T-invariantes que comparten transiciones se cortan en el fork")
  void testParallelInvariantsAreSplitAtFork() {
    // P1 es fork (lo consumen T1 y T2) y T0 produce en él, así que T0 cierra segmento.
    int[][] pre = {{1, 0, 0}, {0, 1, 1}};
    int[][] post = {{0, 1, 1}, {1, 0, 0}};
    List<List<Integer>> tInvariants = List.of(List.of(1, 1, 0), List.of(1, 0, 1));
    Responsibilities r = new Responsibilities(pre, post, tInvariants, Set.of(0, 1));

    assertTrue(r.getForkPlaces().contains(1), "precondición: P1 debe ser fork");
    assertEquals(List.of(List.of(0), List.of(1), List.of(2)), r.getSegments());
  }

  @Test
  @DisplayName("los segmentos no se repiten aunque dos invariantes generen el mismo")
  void testSegmentsAreDeduplicated() {
    int[][] pre = {{1, 0, 0}, {0, 1, 1}};
    int[][] post = {{0, 1, 1}, {1, 0, 0}};
    // Ambos invariantes empiezan con T0, que cierra segmento en el fork: [0] aparece dos veces.
    Responsibilities r =
        new Responsibilities(pre, post, List.of(List.of(1, 1, 0), List.of(1, 0, 1)), Set.of(0, 1));

    assertEquals(
        r.getSegments().size(), new java.util.HashSet<>(r.getSegments()).size(), "hay duplicados");
  }

  // ── aislamiento del estado ────────────────────────────────

  @Test
  @DisplayName("mutar las matrices originales no cambia el resultado")
  void testDefensiveCopyOfMatrices() {
    int[][] pre = {{1, 1}, {0, 0}};
    int[][] post = {{0, 0}, {1, 0}};
    Responsibilities r = new Responsibilities(pre, post, List.of(), Set.of(0, 1));
    List<Integer> before = List.copyOf(r.getForkPlaces());

    pre[0][1] = 0;
    post[0][0] = 99;

    assertEquals(before, r.getForkPlaces());
  }

  @Test
  @DisplayName("las listas devueltas son inmutables")
  void testReturnedListsAreUnmodifiable() {
    int[][] pre = {{1, 1}};
    int[][] post = {{1, 1}};
    Responsibilities r = new Responsibilities(pre, post, List.of(List.of(1, 1)), Set.of(0));

    assertThrows(UnsupportedOperationException.class, () -> r.getSegments().clear());
    assertThrows(UnsupportedOperationException.class, () -> r.getForkPlaces().add(9));
    assertThrows(UnsupportedOperationException.class, () -> r.getJoinPlaces().clear());
  }

  // ── redes reales ──────────────────────────────────────────

  @Test
  @DisplayName("GOLDEN: exampleHuang tiene un fork en P1 y un join en P4")
  void testHuangForksAndJoins() {
    Responsibilities r = huang();
    assertEquals(Set.of(1), new TreeSet<>(r.getForkPlaces()));
    assertEquals(Set.of(4), new TreeSet<>(r.getJoinPlaces()));
  }

  @Test
  @DisplayName("GOLDEN: segmentos de exampleHuang")
  void testHuangSegments() {
    assertEquals(
        List.of(List.of(6, 7, 8, 9), List.of(0), List.of(2, 4), List.of(5), List.of(1, 3)),
        huang().getSegments());
  }

  @Test
  @DisplayName("todo segmento es no vacío y sin transiciones repetidas")
  void testSegmentsAreWellFormed() {
    for (List<Integer> segment : huang().getSegments()) {
      assertFalse(segment.isEmpty(), "segmento vacío");
      assertEquals(
          segment.size(), new TreeSet<>(segment).size(), "transición repetida en " + segment);
    }
  }

  @Test
  @DisplayName("travelAgencySystem: P9 es fork y join a la vez")
  void testTravelHasPlaceThatIsBothForkAndJoin() {
    // Este caso es el que ThreadAllocator.computeSegmentPlaces excluye a propósito:
    // una plaza con varios productores y varios consumidores no tiene lado asignable.
    Responsibilities r =
        responsibilitiesOf("travelAgencySystem.properties", Set.of(2, 3, 5, 8, 9, 11, 12, 13, 14));

    assertTrue(r.getForkPlaces().contains(9));
    assertTrue(r.getJoinPlaces().contains(9));
  }

  /**
   * Construye el analizador de exampleHuang con su clasificación conocida.
   *
   * @return el analizador de responsabilidades de exampleHuang
   */
  private static Responsibilities huang() {
    return responsibilitiesOf("exampleHuang.properties", Set.of(1, 2, 3, 4, 8, 9, 10));
  }

  /**
   * Construye el analizador para una red del classpath.
   *
   * @param resource el nombre del recurso
   * @param actionPlaces las plazas de acción ya clasificadas
   * @return el analizador de responsabilidades
   */
  private static Responsibilities responsibilitiesOf(String resource, Set<Integer> actionPlaces) {
    PetriNetDefinition d = PetriNetProperties.fromResource(resource).toDefinition();
    Invariants invariants = new Invariants(Matrix.subtract(d.post(), d.pre()));
    return new Responsibilities(d.pre(), d.post(), invariants.getTInvariants(), actionPlaces);
  }
}