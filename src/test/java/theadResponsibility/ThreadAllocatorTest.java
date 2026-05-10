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
 * Unit tests for ThreadAllocator using the Huang S3PR net (exampleHuang.properties).
 *
 * <p>Expected results validated against the paper (Ventre & Micolini, 2021):
 *
 * <ul>
 *   <li>Algorithm 4.1: max active threads = 3, reachable markings = 26
 *   <li>Algorithm 4.2: 5 segments, fork={1}, join={4}
 *   <li>Algorithm 4.3: all segments have max 1 thread
 *   <li>PI of IT1 → [0,1,3,4,6,12,13]
 *   <li>PI of IT2 → [0,1,2,4,5,12,13]
 *   <li>PI of IT3 → [7,8,9,10,11,12,13]
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("ThreadAllocator — exampleHuang")
class ThreadAllocatorTest {

  /** Shared allocator instance. */
  private ThreadAllocator allocator;

  /** Expected segments (order-independent). */
  private static final Set<List<Integer>> EXPECTED_SEGMENTS =
      Set.of(
          List.of(6, 7, 8, 9), // SE
          List.of(0), // SA
          List.of(2, 4), // SC
          List.of(5), // SD
          List.of(1, 3) // SB
          );

  @BeforeEach
  void setUp() {
    allocator = new ThreadAllocator();
  }

  // ── construcción ─────────────────────────────────────────

  @Test
  @DisplayName("constructor no lanza excepción con la red de Huang")
  void testConstructorDoesNotThrow() {
    assertNotNull(allocator);
  }

  @Test
  @DisplayName("getClassifier no retorna null")
  void testGetClassifierNotNull() {
    assertNotNull(allocator.getClassifier());
  }

  @Test
  @DisplayName("getTree no retorna null")
  void testGetTreeNotNull() {
    assertNotNull(allocator.getTree());
  }

  @Test
  @DisplayName("getResponsibilities no retorna null")
  void testGetResponsibilitiesNotNull() {
    assertNotNull(allocator.getResponsibilities());
  }

  @Test
  @DisplayName("getInvariants no retorna null")
  void testGetInvariantsNotNull() {
    assertNotNull(allocator.getInvariants());
  }

  // ── Algorithm 4.1 ─────────────────────────────────────────

  @Test
  @DisplayName("Algorithm 4.1 — getMaxActiveThreads → 3")
  void testMaxActiveThreads() {
    assertEquals(3, allocator.getMaxActiveThreads());
  }

  @Test
  @DisplayName("Algorithm 4.1 — reachable markings → 26")
  void testNumReachableMarkings() {
    assertEquals(26, allocator.getTree().getNumReachableMarkings());
  }

  @Test
  @DisplayName("Algorithm 4.1 — getMaxActiveThreads coincide con el árbol")
  void testMaxActiveThreadsConsistency() {
    assertEquals(allocator.getTree().getMaxNumThreads(), allocator.getMaxActiveThreads());
  }

  // ── Algorithm 4.2 ─────────────────────────────────────────

  @Test
  @DisplayName("Algorithm 4.2 — 5 segmentos identificados")
  void testSegmentsCount() {
    assertEquals(5, allocator.getSegments().size());
  }

  @Test
  @DisplayName("Algorithm 4.2 — segmentos coinciden con los del paper")
  void testSegmentsMatchPaper() {
    assertEquals(EXPECTED_SEGMENTS, Set.copyOf(allocator.getSegments()));
  }

  @Test
  @DisplayName("Algorithm 4.2 — fork place → [1] (paper P2)")
  void testForkPlaces() {
    assertEquals(List.of(1), allocator.getResponsibilities().getForkPlaces());
  }

  @Test
  @DisplayName("Algorithm 4.2 — join place → [4] (paper P5)")
  void testJoinPlaces() {
    assertEquals(List.of(4), allocator.getResponsibilities().getJoinPlaces());
  }

  @Test
  @DisplayName("Algorithm 4.2 — getSegments retorna vista no modificable")
  void testSegmentsIsUnmodifiable() {
    List<List<Integer>> segs = allocator.getSegments();
    assertThrows(UnsupportedOperationException.class, () -> segs.add(List.of(99)));
  }

  // ── Algorithm 4.3 ─────────────────────────────────────────

  @Test
  @DisplayName("Algorithm 4.3 — 5 valores de hilos por segmento")
  void testThreadsPerSegmentCount() {
    assertEquals(5, allocator.getThreadsPerSegment().size());
  }

  @Test
  @DisplayName("Algorithm 4.3 — todos los segmentos tienen máximo 1 hilo")
  void testThreadsPerSegmentAllOne() {
    for (int i = 0; i < allocator.getThreadsPerSegment().size(); i++) {
      assertEquals(
          1,
          allocator.getThreadsPerSegment().get(i),
          "Segmento S" + (i + 1) + " debería tener 1 hilo máximo");
    }
  }

  @Test
  @DisplayName("Algorithm 4.3 — SE [6,7,8,9] → places [8,9,10] → 1 hilo")
  void testThreadsSegmentSE() {
    List<List<Integer>> segPlaces = allocator.computeAllSegmentPlaces();
    List<List<Integer>> segs = allocator.getSegments();
    int idx = segs.indexOf(List.of(6, 7, 8, 9));
    assertTrue(idx >= 0, "Segmento SE no encontrado");
    assertEquals(List.of(8, 9, 10), segPlaces.get(idx));
    assertEquals(1, allocator.getThreadsPerSegment().get(idx));
  }

  @Test
  @DisplayName("Algorithm 4.3 — SA [0] → places [] → 1 hilo")
  void testThreadsSegmentSA() {
    List<List<Integer>> segs = allocator.getSegments();
    List<List<Integer>> segPlaces = allocator.computeAllSegmentPlaces();
    int idx = segs.indexOf(List.of(0));
    assertTrue(idx >= 0, "Segmento SA no encontrado");
    assertTrue(segPlaces.get(idx).isEmpty(), "SA no debería tener action places");
    assertEquals(1, allocator.getThreadsPerSegment().get(idx));
  }

  @Test
  @DisplayName("Algorithm 4.3 — SC [2,4] → places [3] → 1 hilo")
  void testThreadsSegmentSC() {
    List<List<Integer>> segs = allocator.getSegments();
    List<List<Integer>> segPlaces = allocator.computeAllSegmentPlaces();
    int idx = segs.indexOf(List.of(2, 4));
    assertTrue(idx >= 0, "Segmento SC no encontrado");
    assertEquals(List.of(3), segPlaces.get(idx));
    assertEquals(1, allocator.getThreadsPerSegment().get(idx));
  }

  @Test
  @DisplayName("Algorithm 4.3 — SB [1,3] → places [2] → 1 hilo")
  void testThreadsSegmentSB() {
    List<List<Integer>> segs = allocator.getSegments();
    List<List<Integer>> segPlaces = allocator.computeAllSegmentPlaces();
    int idx = segs.indexOf(List.of(1, 3));
    assertTrue(idx >= 0, "Segmento SB no encontrado");
    assertEquals(List.of(2), segPlaces.get(idx));
    assertEquals(1, allocator.getThreadsPerSegment().get(idx));
  }

  // ── computePiOfIt ─────────────────────────────────────────

  @Test
  @DisplayName("computePiOfIt → 3 entradas (una por T-invariante)")
  void testPiOfItSize() {
    assertEquals(3, allocator.computePiOfIt().size());
  }

  @Test
  @DisplayName("PI of IT1 → [0,1,3,4,6,12,13]")
  void testPiOfIt1() {
    assertEquals(List.of(0, 1, 3, 4, 6, 12, 13), allocator.computePiOfIt().get(0));
  }

  @Test
  @DisplayName("PI of IT2 → [0,1,2,4,5,12,13]")
  void testPiOfIt2() {
    assertEquals(List.of(0, 1, 2, 4, 5, 12, 13), allocator.computePiOfIt().get(1));
  }

  @Test
  @DisplayName("PI of IT3 → [7,8,9,10,11,12,13]")
  void testPiOfIt3() {
    assertEquals(List.of(7, 8, 9, 10, 11, 12, 13), allocator.computePiOfIt().get(2));
  }

  @Test
  @DisplayName("PI of IT contiene más places que PA of IT (incluye recursos)")
  void testPiOfItLargerThanPaOfIt() {
    List<List<Integer>> piOfIt = allocator.computePiOfIt();
    List<List<Integer>> paOfIt = allocator.getClassifier().getPaOfIt();
    for (int i = 0; i < piOfIt.size(); i++) {
      assertTrue(
          piOfIt.get(i).size() >= paOfIt.get(i).size(),
          "PI of IT" + (i + 1) + " debería tener al menos tantas places como PA of IT");
    }
  }

  @Test
  @DisplayName("PA of IT es subconjunto de PI of IT para cada invariante")
  void testPaOfItSubsetOfPiOfIt() {
    List<List<Integer>> piOfIt = allocator.computePiOfIt();
    List<List<Integer>> paOfIt = allocator.getClassifier().getPaOfIt();
    for (int i = 0; i < piOfIt.size(); i++) {
      assertTrue(
          piOfIt.get(i).containsAll(paOfIt.get(i)),
          "PA of IT" + (i + 1) + " no es subconjunto de PI of IT" + (i + 1));
    }
  }

  // ── computeAllSegmentPlaces ───────────────────────────────

  @Test
  @DisplayName("computeAllSegmentPlaces → 5 entradas (una por segmento)")
  void testAllSegmentPlacesSize() {
    assertEquals(5, allocator.computeAllSegmentPlaces().size());
  }

  @Test
  @DisplayName("todas las places de todos los segmentos son action places")
  void testSegmentPlacesAreActionPlaces() {
    Set<Integer> actionPlaces = allocator.getClassifier().getActionPlaces();
    for (List<Integer> segPlaces : allocator.computeAllSegmentPlaces()) {
      for (Integer p : segPlaces) {
        assertTrue(
            actionPlaces.contains(p), "La plaza " + p + " en un segmento no es action place");
      }
    }
  }

  @Test
  @DisplayName("ninguna place de segmento es fork o join")
  void testSegmentPlacesExcludeForksAndJoins() {
    List<Integer> forks = allocator.getResponsibilities().getForkPlaces();
    List<Integer> joins = allocator.getResponsibilities().getJoinPlaces();
    for (List<Integer> segPlaces : allocator.computeAllSegmentPlaces()) {
      for (Integer p : segPlaces) {
        assertFalse(forks.contains(p), "Plaza fork " + p + " encontrada en segmento");
        assertFalse(joins.contains(p), "Plaza join " + p + " encontrada en segmento");
      }
    }
  }
}
