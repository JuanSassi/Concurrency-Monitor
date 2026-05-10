import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReachabilityTree using the Huang S3PR net (exampleHuang.properties).
 *
 * <p>Expected results validated against the paper (Ventre & Micolini, 2021):
 *
 * <ul>
 *   <li>Action places (sorted): [1, 2, 3, 4, 8, 9, 10]
 *   <li>Reachable markings: 26
 *   <li>Maximum active threads: 3
 *   <li>First marking (M0): [0, 1, 0, 0, 0, 0, 0]
 * </ul>
 *
 * <p>Segment thread counts (Algorithm 4.3):
 *
 * <ul>
 *   <li>SA → places [] → 1 (no action places)
 *   <li>SB → places [3] → 1
 *   <li>SC → places [2] → 1
 *   <li>SD → places [] → 1 (no action places)
 *   <li>SE → places [8, 9, 10] → 1
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("ReachabilityTree — exampleHuang")
class ReachabilityTreeTest {

  /** Shared tree instance built once per test. */
  private ReachabilityTree tree;

  /** Expected sorted action places for the Huang net. */
  private static final List<Integer> EXPECTED_ACTION_PLACES = List.of(1, 2, 3, 4, 8, 9, 10);

  /** Expected number of reachable markings. */
  private static final int EXPECTED_NUM_MARKINGS = 26;

  /** Expected maximum active threads (Algorithm 4.1 result). */
  private static final int EXPECTED_MAX_THREADS = 3;

  /** Expected first marking M0 projected onto action places. */
  private static final int[] EXPECTED_M0 = {0, 1, 0, 0, 0, 0, 0};

  @BeforeEach
  void setUp() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);
    Invariants inv = new Invariants(w);
    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, inv);
    tree = new ReachabilityTree(classifier.getActionPlaces());
  }

  // ── construcción ─────────────────────────────────────────

  @Test
  @DisplayName("constructor no lanza excepción con la red de Huang")
  void testConstructorDoesNotThrow() {
    assertNotNull(tree);
  }

  @Test
  @DisplayName("getReachableMarkings no retorna null")
  void testGetReachableMarkingsNotNull() {
    assertNotNull(tree.getReachableMarkings());
  }

  @Test
  @DisplayName("getSortedActionPlaces no retorna null")
  void testGetSortedActionPlacesNotNull() {
    assertNotNull(tree.getSortedActionPlaces());
  }

  // ── action places ─────────────────────────────────────────

  @Test
  @DisplayName("getSortedActionPlaces → [1, 2, 3, 4, 8, 9, 10]")
  void testSortedActionPlaces() {
    assertEquals(EXPECTED_ACTION_PLACES, tree.getSortedActionPlaces());
  }

  @Test
  @DisplayName("getSortedActionPlaces retorna lista no modificable")
  void testSortedActionPlacesIsUnmodifiable() {
    List<Integer> places = tree.getSortedActionPlaces();
    assertThrows(UnsupportedOperationException.class, () -> places.add(99));
  }

  @Test
  @DisplayName("getSortedActionPlaces está ordenado ascendentemente")
  void testSortedActionPlacesOrdered() {
    List<Integer> places = tree.getSortedActionPlaces();
    for (int i = 1; i < places.size(); i++) {
      assertTrue(places.get(i - 1) < places.get(i), "Lista no ordenada en posición " + i);
    }
  }

  // ── marcados alcanzables ──────────────────────────────────

  @Test
  @DisplayName("getNumReachableMarkings → 26")
  void testNumReachableMarkings() {
    assertEquals(EXPECTED_NUM_MARKINGS, tree.getNumReachableMarkings());
  }

  @Test
  @DisplayName("getReachableMarkings → lista de 26 elementos")
  void testReachableMarkingsSize() {
    assertEquals(EXPECTED_NUM_MARKINGS, tree.getReachableMarkings().size());
  }

  @Test
  @DisplayName("M0 → [0, 1, 0, 0, 0, 0, 0] (marcado inicial proyectado)")
  void testFirstMarking() {
    assertArrayEquals(EXPECTED_M0, tree.getReachableMarkings().get(0));
  }

  @Test
  @DisplayName("todos los marcados tienen longitud igual al número de action places (7)")
  void testMarkingLength() {
    for (int[] marking : tree.getReachableMarkings()) {
      assertEquals(
          EXPECTED_ACTION_PLACES.size(),
          marking.length,
          "Marcado con longitud incorrecta: " + Arrays.toString(marking));
    }
  }

  @Test
  @DisplayName("todos los tokens en todos los marcados son no negativos")
  void testMarkingsNonNegative() {
    for (int[] marking : tree.getReachableMarkings()) {
      for (int tokens : marking) {
        assertTrue(tokens >= 0, "Token negativo encontrado: " + Arrays.toString(marking));
      }
    }
  }

  @Test
  @DisplayName("no hay marcados duplicados (deduplicación por contenido)")
  void testNoduplicateMarkings() {
    List<int[]> markings = tree.getReachableMarkings();
    Set<String> seen = new HashSet<>();
    for (int[] marking : markings) {
      String key = Arrays.toString(marking);
      assertFalse(seen.contains(key), "Marcado duplicado encontrado: " + key);
      seen.add(key);
    }
  }

  @Test
  @DisplayName(
      "getReachableMarkings retorna copias defensivas — modificar no afecta el estado interno")
  void testReachableMarkingsDefensiveCopy() {
    List<int[]> markings = tree.getReachableMarkings();
    int[] first = markings.get(0);
    int originalValue = first[0];
    first[0] = 999;
    assertEquals(
        originalValue,
        tree.getReachableMarkings().get(0)[0],
        "Modificar la copia externa afectó el estado interno");
  }

  // ── Algorithm 4.1 — max threads ──────────────────────────

  @Test
  @DisplayName("getMaxNumThreads → 3 (resultado del paper)")
  void testMaxNumThreads() {
    assertEquals(EXPECTED_MAX_THREADS, tree.getMaxNumThreads());
  }

  @Test
  @DisplayName("getMaxNumThreads coincide con el máximo calculado manualmente")
  void testMaxNumThreadsConsistency() {
    int max = 0;
    for (int[] marking : tree.getReachableMarkings()) {
      int sum = 0;
      for (int t : marking) sum += t;
      if (sum > max) max = sum;
    }
    assertEquals(max, tree.getMaxNumThreads());
  }

  // ── Algorithm 4.3 — max threads per segment ───────────────

  @Test
  @DisplayName("calculateMaxThreadsInSegment con lista vacía → 1")
  void testMaxThreadsEmptySegment() {
    assertEquals(1, tree.calculateMaxThreadsInSegment(List.of()));
  }

  @Test
  @DisplayName("SE → places [8, 9, 10] → max 1 hilo")
  void testMaxThreadsSegmentSE() {
    assertEquals(1, tree.calculateMaxThreadsInSegment(List.of(8, 9, 10)));
  }

  @Test
  @DisplayName("SB → places [3] → max 1 hilo")
  void testMaxThreadsSegmentSB() {
    assertEquals(1, tree.calculateMaxThreadsInSegment(List.of(3)));
  }

  @Test
  @DisplayName("SC → places [2] → max 1 hilo")
  void testMaxThreadsSegmentSC() {
    assertEquals(1, tree.calculateMaxThreadsInSegment(List.of(2)));
  }

  @Test
  @DisplayName("todas las action places juntas → max igual a getMaxNumThreads")
  void testMaxThreadsAllActionPlaces() {
    assertEquals(
        tree.getMaxNumThreads(), tree.calculateMaxThreadsInSegment(EXPECTED_ACTION_PLACES));
  }

  @Test
  @DisplayName("calculateMaxThreadsInSegment nunca retorna valor negativo")
  void testMaxThreadsSegmentNonNegative() {
    assertTrue(tree.calculateMaxThreadsInSegment(List.of(1, 2, 3)) >= 0);
  }

  // ── estado del PetriNet después del BFS ──────────────────

  @Test
  @DisplayName("PetriNet queda en marcado inicial después de construir el árbol")
  void testPetriNetResetAfterBFS() {
    int[] expected = PetrinetLoader.getInitialMarkingVector();
    assertArrayEquals(
        expected,
        PetriNet.getInstance().getMarking(),
        "PetriNet no fue reseteado al marcado inicial tras el BFS");
  }
}
