import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TInvariantPlaces}.
 *
 * <p>Clase puramente funcional: recibe matrices y listas, calcula en el constructor y no toca
 * estado externo. Todos los tests usan matrices escritas a mano, sin recursos.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("TInvariantPlaces")
class TInvariantPlacesTest {

  /** Pre de una red de 3 lugares y 2 transiciones. */
  private static int[][] pre() {
    return new int[][] {{1, 0}, {0, 1}, {1, 0}};
  }

  /** Post correspondiente a {@link #pre()}. */
  private static int[][] post() {
    return new int[][] {{0, 1}, {1, 0}, {0, 1}};
  }

  // ── cálculo básico ────────────────────────────────────────

  @Test
  @DisplayName("piOfIt junta las plazas conectadas por pre o por post")
  void testPiOfIt() {
    // El T-invariante activa sólo T0. P0 y P2 lo consumen, P1 lo produce.
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(1, 0)), Set.of());

    assertEquals(List.of(List.of(0, 1, 2)), places.getPiOfIt());
  }

  @Test
  @DisplayName("paOfIt filtra piOfIt dejando solo plazas de acción")
  void testPaOfIt() {
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(1, 0)), Set.of(1));

    assertEquals(List.of(List.of(0, 1, 2)), places.getPiOfIt());
    assertEquals(List.of(List.of(1)), places.getPaOfIt());
  }

  @Test
  @DisplayName("paOfIt siempre es subconjunto de piOfIt")
  void testPaIsSubsetOfPi() {
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(1, 1), List.of(0, 1)), Set.of(0, 2));

    for (int i = 0; i < places.getPiOfIt().size(); i++) {
      assertTrue(
          places.getPiOfIt().get(i).containsAll(places.getPaOfIt().get(i)),
          "PA no está contenido en PI en el invariante " + i);
    }
  }

  @Test
  @DisplayName("las plazas salen en orden ascendente")
  void testPlacesAreSorted() {
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(1, 1)), Set.of(0, 1, 2));

    for (List<Integer> group : places.getPiOfIt()) {
      for (int i = 1; i < group.size(); i++) {
        assertTrue(group.get(i - 1) < group.get(i), "orden roto: " + group);
      }
    }
  }

  @Test
  @DisplayName("hay una entrada por T-invariante, en el mismo orden")
  void testOneEntryPerInvariant() {
    TInvariantPlaces places =
        new TInvariantPlaces(
            pre(), post(), List.of(List.of(1, 0), List.of(0, 1), List.of(1, 1)), Set.of());

    assertEquals(3, places.getPiOfIt().size());
    assertEquals(3, places.getPaOfIt().size());
    assertEquals(List.of(0, 1, 2), places.getPiOfIt().get(2), "el tercero activa ambas transiciones");
  }

  // ── coeficientes ──────────────────────────────────────────

  @Test
  @DisplayName("solo cuentan las transiciones con coeficiente positivo")
  void testOnlyPositiveCoefficientsCount() {
    // T1 conecta P1 nada más, pero su coeficiente es 0: no debe aportar plazas.
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(1, 0)), Set.of());

    assertTrue(places.getPiOfIt().get(0).containsAll(List.of(0, 2)));
  }

  @Test
  @DisplayName("un coeficiente negativo se ignora igual que un cero")
  void testNegativeCoefficientIgnored() {
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(-3, 0)), Set.of());

    assertEquals(List.of(List.of()), places.getPiOfIt());
  }

  @Test
  @DisplayName("un T-invariante nulo no aporta ninguna plaza")
  void testZeroInvariantGivesEmptyGroup() {
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(0, 0)), Set.of(0, 1, 2));

    assertEquals(List.of(List.of()), places.getPiOfIt());
    assertEquals(List.of(List.of()), places.getPaOfIt());
  }

  @Test
  @DisplayName("sin T-invariantes las dos listas quedan vacías")
  void testNoInvariants() {
    TInvariantPlaces places = new TInvariantPlaces(pre(), post(), List.of(), Set.of(0));

    assertTrue(places.getPiOfIt().isEmpty());
    assertTrue(places.getPaOfIt().isEmpty());
  }

  @Test
  @DisplayName("una plaza desconectada de la transición activa no aparece")
  void testDisconnectedPlaceExcluded() {
    // P2 no participa de ninguna transición.
    int[][] pre = {{1, 0}, {0, 1}, {0, 0}};
    int[][] post = {{0, 1}, {1, 0}, {0, 0}};
    TInvariantPlaces places = new TInvariantPlaces(pre, post, List.of(List.of(1, 1)), Set.of());

    assertEquals(List.of(0, 1), places.getPiOfIt().get(0));
  }

  // ── aislamiento del estado ────────────────────────────────

  @Test
  @DisplayName("mutar las matrices originales no cambia el resultado")
  void testDefensiveCopyOfMatrices() {
    int[][] pre = pre();
    int[][] post = post();
    TInvariantPlaces places = new TInvariantPlaces(pre, post, List.of(List.of(1, 0)), Set.of());
    List<List<Integer>> before = places.getPiOfIt();

    pre[0][0] = 0;
    post[0][0] = 99;

    assertEquals(before, places.getPiOfIt());
  }

  @Test
  @DisplayName("las listas devueltas son inmutables")
  void testReturnedListsAreUnmodifiable() {
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(1, 0)), Set.of(1));

    assertThrows(UnsupportedOperationException.class, () -> places.getPiOfIt().clear());
    assertThrows(UnsupportedOperationException.class, () -> places.getPaOfIt().clear());
    assertThrows(UnsupportedOperationException.class, () -> places.getPiOfIt().get(0).add(9));
  }

  @Test
  @DisplayName("mutar el conjunto de plazas de acción después no cambia paOfIt")
  void testActionPlacesAreFrozen() {
    Set<Integer> actionPlaces = new java.util.HashSet<>(Set.of(1));
    TInvariantPlaces places =
        new TInvariantPlaces(pre(), post(), List.of(List.of(1, 0)), actionPlaces);

    actionPlaces.add(0);
    actionPlaces.add(2);

    assertEquals(List.of(List.of(1)), places.getPaOfIt(), "la clasificación debe quedar congelada");
  }

  // ── red real ──────────────────────────────────────────────

  @Test
  @DisplayName("exampleHuang: PI y PA de cada T-invariante")
  void testRealNet() {
    PetriNetDefinition d =
        PetriNetProperties.fromResource("exampleHuang.properties").toDefinition();
    List<List<Integer>> tInvariants =
        List.of(
            List.of(1, 0, 1, 0, 1, 1, 0, 0, 0, 0),
            List.of(1, 1, 0, 1, 0, 1, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 1, 1, 1, 1));
    Set<Integer> actionPlaces = Set.of(1, 2, 3, 4, 8, 9, 10);

    TInvariantPlaces places = new TInvariantPlaces(d.pre(), d.post(), tInvariants, actionPlaces);

    assertEquals(
        List.of(
            List.of(0, 1, 3, 4, 6, 12, 13),
            List.of(0, 1, 2, 4, 5, 12, 13),
            List.of(7, 8, 9, 10, 11, 12, 13)),
        places.getPiOfIt());
    assertEquals(
        List.of(List.of(1, 3, 4), List.of(1, 2, 4), List.of(8, 9, 10)), places.getPaOfIt());
  }
}