import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Responsibilities using the Huang S3PR net (exampleHuang.properties).
 *
 * <p>Index mapping (0-based vs paper 1-based):
 *
 * <ul>
 *   <li>Transitions: T0-T9 → paper T1-T10
 *   <li>Places: P0-P13 → paper P1-P14
 * </ul>
 *
 * <p>Expected results validated against the paper (Ventre & Micolini, 2021):
 *
 * <ul>
 *   <li>Fork places: {P1} → index {1} (paper P2)
 *   <li>Join places: {P4} → index {4} (paper P5)
 *   <li>Segments (SA-SE):
 *       <ul>
 *         <li>SA → [0] (paper T1)
 *         <li>SB → [1, 3] (paper T2, T4)
 *         <li>SC → [2, 4] (paper T3, T5)
 *         <li>SD → [5] (paper T6)
 *         <li>SE → [6, 7, 8, 9] (paper T7, T8, T9, T10)
 *       </ul>
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Responsibilities — exampleHuang")
class ResponsibilitiesTest {

  /** Shared instance built once per test. */
  private Responsibilities resp;

  /** Expected fork place indices. */
  private static final List<Integer> EXPECTED_FORKS = List.of(1);

  /** Expected join place indices. */
  private static final List<Integer> EXPECTED_JOINS = List.of(4);

  /** Expected segments (order-independent set of transition index lists). */
  private static final Set<List<Integer>> EXPECTED_SEGMENTS =
      Set.of(
          List.of(0), // SA
          List.of(1, 3), // SB
          List.of(2, 4), // SC
          List.of(5), // SD
          List.of(6, 7, 8, 9) // SE
          );

  @BeforeEach
  void setUp() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);
    Invariants inv = new Invariants(w);
    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, inv);

    resp = new Responsibilities(pre, post, inv.getTInvariants(), classifier.getActionPlaces());
  }

  // ── construcción ─────────────────────────────────────────

  @Test
  @DisplayName("constructor no lanza excepción con la red de Huang")
  void testConstructorDoesNotThrow() {
    assertNotNull(resp);
  }

  @Test
  @DisplayName("getSegments no retorna null")
  void testGetSegmentsNotNull() {
    assertNotNull(resp.getSegments());
  }

  @Test
  @DisplayName("getForkPlaces no retorna null")
  void testGetForkPlacesNotNull() {
    assertNotNull(resp.getForkPlaces());
  }

  @Test
  @DisplayName("getJoinPlaces no retorna null")
  void testGetJoinPlacesNotNull() {
    assertNotNull(resp.getJoinPlaces());
  }

  // ── forks ─────────────────────────────────────────────────

  @Test
  @DisplayName("fork places → {1} (paper P2)")
  void testForkPlacesValues() {
    assertEquals(EXPECTED_FORKS, resp.getForkPlaces());
  }

  @Test
  @DisplayName("fork places → cantidad 1")
  void testForkPlacesCount() {
    assertEquals(1, resp.getForkPlaces().size());
  }

  @Test
  @DisplayName("fork places son subconjunto de action places")
  void testForkPlacesAreActionPlaces() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);
    Invariants inv = new Invariants(w);
    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, inv);
    Set<Integer> actionPlaces = classifier.getActionPlaces();

    for (Integer fork : resp.getForkPlaces()) {
      assertTrue(actionPlaces.contains(fork), "Fork place " + fork + " no es una action place");
    }
  }

  // ── joins ─────────────────────────────────────────────────

  @Test
  @DisplayName("join places → {4} (paper P5)")
  void testJoinPlacesValues() {
    assertEquals(EXPECTED_JOINS, resp.getJoinPlaces());
  }

  @Test
  @DisplayName("join places → cantidad 1")
  void testJoinPlacesCount() {
    assertEquals(1, resp.getJoinPlaces().size());
  }

  @Test
  @DisplayName("join places son subconjunto de action places")
  void testJoinPlacesAreActionPlaces() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);
    Invariants inv = new Invariants(w);
    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, inv);
    Set<Integer> actionPlaces = classifier.getActionPlaces();

    for (Integer join : resp.getJoinPlaces()) {
      assertTrue(actionPlaces.contains(join), "Join place " + join + " no es una action place");
    }
  }

  @Test
  @DisplayName("forks y joins son disjuntos")
  void testForksAndJoinsDisjoint() {
    for (Integer fork : resp.getForkPlaces()) {
      assertFalse(
          resp.getJoinPlaces().contains(fork),
          "La plaza " + fork + " aparece como fork y join simultáneamente");
    }
  }

  // ── segments ──────────────────────────────────────────────

  @Test
  @DisplayName("segments → cantidad 5 (SA, SB, SC, SD, SE)")
  void testSegmentsCount() {
    assertEquals(5, resp.getSegments().size());
  }

  @Test
  @DisplayName("segments contienen exactamente los 5 segmentos esperados del paper")
  void testSegmentsMatchPaper() {
    Set<List<Integer>> actual = Set.copyOf(resp.getSegments());
    assertEquals(EXPECTED_SEGMENTS, actual);
  }

  @Test
  @DisplayName("SA → [0] (transición previa al fork, paper T1)")
  void testSegmentSA() {
    assertTrue(resp.getSegments().contains(List.of(0)), "Segmento SA [0] no encontrado");
  }

  @Test
  @DisplayName("SB → [1, 3] (rama izquierda, paper T2 T4)")
  void testSegmentSB() {
    assertTrue(resp.getSegments().contains(List.of(1, 3)), "Segmento SB [1, 3] no encontrado");
  }

  @Test
  @DisplayName("SC → [2, 4] (rama derecha, paper T3 T5)")
  void testSegmentSC() {
    assertTrue(resp.getSegments().contains(List.of(2, 4)), "Segmento SC [2, 4] no encontrado");
  }

  @Test
  @DisplayName("SD → [5] (transición posterior al join, paper T6)")
  void testSegmentSD() {
    assertTrue(resp.getSegments().contains(List.of(5)), "Segmento SD [5] no encontrado");
  }

  @Test
  @DisplayName("SE → [6, 7, 8, 9] (IT3 secuencial, paper T7 T8 T9 T10)")
  void testSegmentSE() {
    assertTrue(
        resp.getSegments().contains(List.of(6, 7, 8, 9)), "Segmento SE [6, 7, 8, 9] no encontrado");
  }

  @Test
  @DisplayName("ningún segmento está vacío")
  void testNoEmptySegments() {
    for (int i = 0; i < resp.getSegments().size(); i++) {
      assertFalse(resp.getSegments().get(i).isEmpty(), "El segmento S" + (i + 1) + " está vacío");
    }
  }

  @Test
  @DisplayName("todos los índices de transición en segmentos son válidos (0 a numTransitions-1)")
  void testSegmentTransitionIndicesValid() {
    int numTransitions = PetrinetLoader.getNumTransitions();
    for (List<Integer> segment : resp.getSegments()) {
      for (Integer t : segment) {
        assertTrue(t >= 0 && t < numTransitions, "Índice de transición inválido: " + t);
      }
    }
  }

  @Test
  @DisplayName("la unión de todos los segmentos cubre exactamente las 10 transiciones")
  void testSegmentsUnionCoversAllTransitions() {
    int numTransitions = PetrinetLoader.getNumTransitions();
    Set<Integer> covered = new java.util.HashSet<>();
    for (List<Integer> segment : resp.getSegments()) {
      covered.addAll(segment);
    }
    assertEquals(
        numTransitions, covered.size(), "La unión de segmentos no cubre todas las transiciones");
  }

  // ── inmutabilidad (EI_EXPOSE_REP) ────────────────────────

  @Test
  @DisplayName("getSegments retorna vista no modificable")
  void testGetSegmentsIsUnmodifiable() {
    List<List<Integer>> segs = resp.getSegments();
    assertThrows(UnsupportedOperationException.class, () -> segs.add(List.of(99)));
  }

  @Test
  @DisplayName("getForkPlaces retorna vista no modificable")
  void testGetForkPlacesIsUnmodifiable() {
    List<Integer> forks = resp.getForkPlaces();
    assertThrows(UnsupportedOperationException.class, () -> forks.add(99));
  }

  @Test
  @DisplayName("getJoinPlaces retorna vista no modificable")
  void testGetJoinPlacesIsUnmodifiable() {
    List<Integer> joins = resp.getJoinPlaces();
    assertThrows(UnsupportedOperationException.class, () -> joins.add(99));
  }
}
