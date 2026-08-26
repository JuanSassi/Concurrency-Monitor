import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlaceClassifier}.
 *
 * <p>La clasificación depende de los P-invariantes, del marcado inicial y de la matriz de
 * incidencia. Los tests sobre redes reales reutilizan un cálculo cacheado, porque construir
 * {@link Invariants} para exampleHuang cuesta más de un segundo.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PlaceClassifier")
class PlaceClassifierTest {

  /** Clasificador de exampleHuang, calculado una sola vez. */
  private static PlaceClassifier huangClassifier;

  /**
   * Devuelve el clasificador de exampleHuang, construyéndolo la primera vez.
   *
   * @return el clasificador ya poblado
   */
  private static synchronized PlaceClassifier huang() {
    if (huangClassifier == null) {
      PetriNetDefinition d =
          PetriNetProperties.fromResource("exampleHuang.properties").toDefinition();
      int[][] pre = d.pre();
      int[][] post = d.post();
      Invariants invariants = new Invariants(Matrix.subtract(post, pre));
      huangClassifier = new PlaceClassifier(pre, post, d.initialMarking(), invariants);
    }
    return huangClassifier;
  }

  // ── invariantes de la clasificación ───────────────────────

  @Test
  @DisplayName("acción y recurso son conjuntos disjuntos")
  void testSetsAreDisjoint() {
    Set<Integer> intersection = new HashSet<>(huang().getActionPlaces());
    intersection.retainAll(huang().getResourcePlaces());
    assertTrue(intersection.isEmpty(), "plazas clasificadas dos veces: " + intersection);
  }

  @Test
  @DisplayName("en exampleHuang toda plaza queda clasificada")
  void testEveryPlaceIsClassified() {
    Set<Integer> all = new TreeSet<>(huang().getActionPlaces());
    all.addAll(huang().getResourcePlaces());
    assertEquals(14, all.size(), "quedaron plazas sin clasificar");
  }

  @Test
  @DisplayName("GOLDEN: clasificación de exampleHuang")
  void testHuangClassification() {
    assertEquals(Set.of(1, 2, 3, 4, 8, 9, 10), new TreeSet<>(huang().getActionPlaces()));
    assertEquals(Set.of(0, 5, 6, 7, 11, 12, 13), new TreeSet<>(huang().getResourcePlaces()));
  }

  @Test
  @DisplayName("la clasificación es determinista entre construcciones")
  void testClassificationIsDeterministic() {
    // Fase 2 recorre un HashSet de candidatos; si ese recorrido variara, la plaza idle
    // elegida podría cambiar de una corrida a otra y con ella toda la clasificación.
    PetriNetDefinition d =
        PetriNetProperties.fromResource("travelAgencySystem.properties").toDefinition();
    Invariants invariants = new Invariants(Matrix.subtract(d.post(), d.pre()));

    PlaceClassifier first = new PlaceClassifier(d.pre(), d.post(), d.initialMarking(), invariants);
    PlaceClassifier second = new PlaceClassifier(d.pre(), d.post(), d.initialMarking(), invariants);

    assertEquals(first.getActionPlaces(), second.getActionPlaces());
    assertEquals(first.getResourcePlaces(), second.getResourcePlaces());
  }

  // ── reglas de clasificación ───────────────────────────────

  @Test
  @DisplayName("una plaza en varios P-invariantes es plaza de acción")
  void testPlaceInManyInvariantsIsAction() {
    // P1 aparece en los dos P-invariantes; P0 y P2 en uno solo cada uno.
    int[][] pre = {{1, 0}, {0, 1}, {1, 0}};
    int[][] post = {{0, 1}, {1, 0}, {0, 1}};
    PlaceClassifier classifier =
        new PlaceClassifier(pre, post, new int[] {1, 0, 1}, fakeInvariants(pre, post));

    assertTrue(classifier.getActionPlaces().contains(1));
  }

  @Test
  @DisplayName("sin P-invariantes no se clasifica ninguna plaza")
  void testNoInvariantsClassifiesNothing() {
    // La fase 1 cuenta apariciones en P-invariantes: con la lista vacía todos los
    // contadores quedan en 0 y ninguna plaza entra en ningún conjunto.
    int[][] pre = {{0, 0}, {0, 0}};
    int[][] post = {{1, 1}, {1, 1}};
    Invariants empty = new Invariants(Matrix.subtract(post, pre));
    assertTrue(empty.getPInvariants().isEmpty(), "precondición del test");

    PlaceClassifier classifier = new PlaceClassifier(pre, post, new int[] {0, 0}, empty);

    assertTrue(classifier.getActionPlaces().isEmpty());
    assertTrue(classifier.getResourcePlaces().isEmpty());
  }

  @Test
  @DisplayName("una plaza sin tokens iniciales no puede ser idle")
  void testIdleRequiresInitialTokens() {
    // Ambas plazas están en un único P-invariante, así que compiten por ser la idle;
    // la que arranca vacía no puede serlo y cae en plazas de acción.
    int[][] pre = {{1, 0}, {0, 1}};
    int[][] post = {{0, 1}, {1, 0}};
    PlaceClassifier classifier =
        new PlaceClassifier(pre, post, new int[] {0, 1}, fakeInvariants(pre, post));

    assertFalse(classifier.getResourcePlaces().contains(0), "P0 arranca vacía, no es idle");
  }

  // ── aislamiento del estado ────────────────────────────────

  @Test
  @DisplayName("mutar el marcado original no cambia la clasificación")
  void testDefensiveCopyOfMarking() {
    int[][] pre = {{1, 0}, {0, 1}};
    int[][] post = {{0, 1}, {1, 0}};
    int[] m0 = {1, 0};
    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, fakeInvariants(pre, post));
    Set<Integer> before = new TreeSet<>(classifier.getResourcePlaces());

    m0[0] = 0;
    m0[1] = 99;

    assertEquals(before, new TreeSet<>(classifier.getResourcePlaces()));
  }

  @Test
  @DisplayName("los conjuntos devueltos son inmutables")
  void testReturnedSetsAreUnmodifiable() {
    assertThrows(UnsupportedOperationException.class, () -> huang().getActionPlaces().add(99));
    assertThrows(UnsupportedOperationException.class, () -> huang().getResourcePlaces().clear());
  }

  // ── delegación en TInvariantPlaces ────────────────────────

  @Test
  @DisplayName("getPaOfIt y getPiOfIt delegan en TInvariantPlaces")
  void testDelegationToTInvariantPlaces() {
    assertEquals(3, huang().getPiOfIt().size(), "una entrada por T-invariante");
    assertEquals(3, huang().getPaOfIt().size());
    assertEquals(
        java.util.List.of(
            java.util.List.of(1, 3, 4), java.util.List.of(1, 2, 4), java.util.List.of(8, 9, 10)),
        huang().getPaOfIt());
  }

  @Test
  @DisplayName("paOfIt solo contiene plazas clasificadas como de acción")
  void testPaOfItOnlyHasActionPlaces() {
    for (java.util.List<Integer> group : huang().getPaOfIt()) {
      assertTrue(
          huang().getActionPlaces().containsAll(group), "PA contiene una plaza de recurso: " + group);
    }
  }

  @Test
  @DisplayName("piOfIt contiene plazas de acción y de recurso mezcladas")
  void testPiOfItMixesBothKinds() {
    boolean hasResource = false;
    for (java.util.List<Integer> group : huang().getPiOfIt()) {
      for (Integer place : group) {
        hasResource |= huang().getResourcePlaces().contains(place);
      }
    }
    assertTrue(hasResource, "PI debería incluir también plazas de recurso");
  }

  /**
   * Construye un calculador de invariantes para la red dada.
   *
   * @param pre matriz de pre-incidencia
   * @param post matriz de post-incidencia
   * @return los invariantes de esa red
   */
  private static Invariants fakeInvariants(int[][] pre, int[][] post) {
    return new Invariants(Matrix.subtract(post, pre));
  }
}