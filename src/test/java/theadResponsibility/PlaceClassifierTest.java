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
 * Unit tests for PlaceClassifier using the Huang S3PR net (exampleHuang.properties).
 *
 * <p>Expected results validated against the paper (Ventre & Micolini, 2021):
 *
 * <ul>
 *   <li>Action places: {P1, P2, P3, P4, P8, P9, P10} → indices {1, 2, 3, 4, 8, 9, 10}
 *   <li>Resource/idle/restriction places: {P0, P5, P6, P7, P11, P12, P13} → indices {0, 5, 6, 7,
 *       11, 12, 13}
 *   <li>PA of IT1 → [1, 3, 4]
 *   <li>PA of IT2 → [1, 2, 4]
 *   <li>PA of IT3 → [8, 9, 10]
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PlaceClassifier — exampleHuang")
class PlaceClassifierTest {

  /** Shared classifier instance built once per test class. */
  private PlaceClassifier classifier;

  /** Expected action place indices for the Huang net. */
  private static final Set<Integer> EXPECTED_ACTION_PLACES = Set.of(1, 2, 3, 4, 8, 9, 10);

  /** Expected resource/idle/restriction place indices for the Huang net. */
  private static final Set<Integer> EXPECTED_RESOURCE_PLACES = Set.of(0, 5, 6, 7, 11, 12, 13);

  @BeforeEach
  void setUp() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);
    Invariants inv = new Invariants(w);
    classifier = new PlaceClassifier(pre, post, m0, inv);
  }

  // ── construcción ─────────────────────────────────────────

  @Test
  @DisplayName("constructor no lanza excepción con la red de Huang")
  void testConstructorDoesNotThrow() {
    assertNotNull(classifier);
  }

  @Test
  @DisplayName("getActionPlaces no retorna null")
  void testGetActionPlacesNotNull() {
    assertNotNull(classifier.getActionPlaces());
  }

  @Test
  @DisplayName("getPaOfIt no retorna null")
  void testGetPaOfItNotNull() {
    assertNotNull(classifier.getPaOfIt());
  }

  // ── action places ─────────────────────────────────────────

  @Test
  @DisplayName("action places → {1, 2, 3, 4, 8, 9, 10}")
  void testActionPlacesValues() {
    assertEquals(EXPECTED_ACTION_PLACES, classifier.getActionPlaces());
  }

  @Test
  @DisplayName("action places → cantidad 7")
  void testActionPlacesCount() {
    assertEquals(7, classifier.getActionPlaces().size());
  }

  @Test
  @DisplayName("action places no contiene plazas de recursos")
  void testActionPlacesDisjointFromResources() {
    Set<Integer> action = classifier.getActionPlaces();
    for (Integer resource : EXPECTED_RESOURCE_PLACES) {
      assertFalse(
          action.contains(resource),
          "La plaza de recurso " + resource + " no debe estar en action places");
    }
  }

  @Test
  @DisplayName("action places cubre exactamente las plazas esperadas del paper")
  void testActionPlacesMatchPaper() {
    assertTrue(classifier.getActionPlaces().containsAll(EXPECTED_ACTION_PLACES));
    assertTrue(EXPECTED_ACTION_PLACES.containsAll(classifier.getActionPlaces()));
  }

  // ── resource places (indirecta via partición) ─────────────

  @Test
  @DisplayName("action places + resource places cubren todas las plazas (partición completa)")
  void testPartitionCoversAllPlaces() {
    int numPlaces = PetrinetLoader.getNumPlaces();
    Set<Integer> action = classifier.getActionPlaces();

    for (int p = 0; p < numPlaces; p++) {
      boolean inAction = action.contains(p);
      boolean inResource = EXPECTED_RESOURCE_PLACES.contains(p);
      assertTrue(inAction || inResource, "La plaza " + p + " no está en ninguna categoría");
    }
  }

  @Test
  @DisplayName("action places y resource places son disjuntos")
  void testPartitionIsDisjoint() {
    Set<Integer> action = classifier.getActionPlaces();
    for (Integer p : action) {
      assertFalse(
          EXPECTED_RESOURCE_PLACES.contains(p), "La plaza " + p + " aparece en ambas categorías");
    }
  }

  // ── PA of IT ─────────────────────────────────────────────

  @Test
  @DisplayName("getPaOfIt → 3 entradas (una por T-invariante)")
  void testPaOfItSize() {
    assertEquals(3, classifier.getPaOfIt().size());
  }

  @Test
  @DisplayName("PA of IT1 → [1, 3, 4]")
  void testPaOfIt1() {
    assertEquals(List.of(1, 3, 4), classifier.getPaOfIt().get(0));
  }

  @Test
  @DisplayName("PA of IT2 → [1, 2, 4]")
  void testPaOfIt2() {
    assertEquals(List.of(1, 2, 4), classifier.getPaOfIt().get(1));
  }

  @Test
  @DisplayName("PA of IT3 → [8, 9, 10]")
  void testPaOfIt3() {
    assertEquals(List.of(8, 9, 10), classifier.getPaOfIt().get(2));
  }

  @Test
  @DisplayName("todas las plazas en PA of IT son action places")
  void testPaOfItContainsOnlyActionPlaces() {
    Set<Integer> action = classifier.getActionPlaces();
    for (List<Integer> pa : classifier.getPaOfIt()) {
      for (Integer p : pa) {
        assertTrue(action.contains(p), "La plaza " + p + " en PA of IT no es una action place");
      }
    }
  }

  @Test
  @DisplayName("ninguna lista de PA of IT está vacía")
  void testPaOfItNoEmptyEntries() {
    for (int i = 0; i < classifier.getPaOfIt().size(); i++) {
      assertFalse(
          classifier.getPaOfIt().get(i).isEmpty(), "PA of IT" + (i + 1) + " no debe estar vacío");
    }
  }

  @Test
  @DisplayName("PA of IT están ordenados ascendentemente por índice de plaza")
  void testPaOfItSorted() {
    for (List<Integer> pa : classifier.getPaOfIt()) {
      for (int i = 1; i < pa.size(); i++) {
        assertTrue(
            pa.get(i - 1) < pa.get(i),
            "PA of IT no está ordenado: " + pa.get(i - 1) + " >= " + pa.get(i));
      }
    }
  }

  // ── inmutabilidad (EI_EXPOSE_REP) ────────────────────────

  @Test
  @DisplayName("getActionPlaces retorna vista no modificable")
  void testGetActionPlacesIsUnmodifiable() {
    Set<Integer> action = classifier.getActionPlaces();
    assertThrows(UnsupportedOperationException.class, () -> action.add(99));
  }

  @Test
  @DisplayName("getPaOfIt retorna vista no modificable")
  void testGetPaOfItIsUnmodifiable() {
    List<List<Integer>> paOfIt = classifier.getPaOfIt();
    assertThrows(UnsupportedOperationException.class, () -> paOfIt.add(List.of(99)));
  }
}
