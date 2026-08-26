import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests sobre los archivos de red reales del proyecto: {@code exampleHuang.properties} y {@code
 * travelAgencySystem.properties}.
 *
 * <p>Cada red se carga <b>por nombre</b> con {@link PetriNetProperties#fromResource(String)}, no a
 * través de {@code petrinet.number}. Así los tests siguen valiendo aunque se cambie qué red está
 * seleccionada en {@code config.properties}.
 *
 * <p>Las aserciones matemáticas son propiedades exactas — Wᵀ·x = 0, rango + nulidad = n, vectores
 * reducidos — y no dependen de qué base concreta devuelva el algoritmo.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Redes reales del catálogo")
class PetriNetFixturesTest {

  /** Recurso de la red S3PR de Huang. */
  private static final String HUANG = "exampleHuang.properties";

  /** Recurso de la red del sistema de agencia de viajes. */
  private static final String TRAVEL = "travelAgencySystem.properties";

  // ── exampleHuang: datos del archivo ───────────────────────

  @Test
  @DisplayName("exampleHuang tiene 14 lugares y 10 transiciones")
  void testHuangDimensions() {
    PetriNetDefinition d = load(HUANG);
    assertEquals(14, d.pre().length, "cantidad de lugares");
    assertEquals(10, d.pre()[0].length, "cantidad de transiciones");
    assertEquals(14, d.post().length);
    assertEquals(10, d.post()[0].length);
  }

  @Test
  @DisplayName("exampleHuang: marcado inicial con 9 tokens repartidos en 7 lugares")
  void testHuangInitialMarking() {
    int[] m0 = load(HUANG).initialMarking();
    assertArrayEquals(new int[] {2, 0, 1, 0, 0, 0, 1, 2, 0, 0, 0, 1, 1, 1}, m0);
    assertEquals(9, sum(m0), "total de tokens iniciales");
    assertEquals(7, countMarked(m0), "lugares con al menos un token");
  }

  @Test
  @DisplayName("exampleHuang no tiene transiciones temporizadas")
  void testHuangHasNoTimedTransitions() {
    for (int time : load(HUANG).temporalTransitions()) {
      assertEquals(0, time, "la red de Huang es puramente lógica, sin tiempos");
    }
  }

  @Test
  @DisplayName("exampleHuang: los lugares de recurso P12 y P13 aparecen en varias transiciones")
  void testHuangResourcePlaces() {
    int[][] pre = load(HUANG).pre();
    assertArrayEquals(new int[] {1, 0, 0, 1, 1, 0, 1, 0, 1, 0}, pre[12]);
    assertArrayEquals(new int[] {1, 0, 0, 0, 0, 0, 1, 0, 0, 0}, pre[13]);
  }

  // ── travelAgencySystem: datos del archivo ─────────────────

  @Test
  @DisplayName("travelAgencySystem tiene 15 lugares y 12 transiciones")
  void testTravelDimensions() {
    PetriNetDefinition d = load(TRAVEL);
    assertEquals(15, d.pre().length, "cantidad de lugares");
    assertEquals(12, d.pre()[0].length, "cantidad de transiciones");
    assertEquals(15, d.post().length);
    assertEquals(12, d.post()[0].length);
  }

  @Test
  @DisplayName("travelAgencySystem: marcado inicial exacto")
  void testTravelInitialMarking() {
    assertArrayEquals(
        new int[] {5, 1, 0, 0, 5, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0}, load(TRAVEL).initialMarking());
  }

  @Test
  @DisplayName("travelAgencySystem tiene exactamente 6 transiciones temporizadas")
  void testTravelTimedTransitions() {
    int[] times = load(TRAVEL).temporalTransitions();
    assertArrayEquals(new int[] {0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0}, times);

    int timed = 0;
    for (int t : times) {
      if (t > 0) {
        timed++;
      }
    }
    assertEquals(6, timed);
  }

  // ── invariantes de plaza (nullspace de Wᵀ) ────────────────

  @Test
  @DisplayName("exampleHuang tiene 7 invariantes de plaza")
  void testHuangPlaceInvariantCount() {
    assertEquals(7, placeInvariants(HUANG).size());
  }

  @Test
  @DisplayName("travelAgencySystem tiene 6 invariantes de plaza")
  void testTravelPlaceInvariantCount() {
    assertEquals(6, placeInvariants(TRAVEL).size());
  }

  @Test
  @DisplayName("todo invariante de plaza de exampleHuang cumple Wᵀ·x = 0")
  void testHuangPlaceInvariantsAreValid() {
    assertAnnihilates(transposedIncidence(HUANG), placeInvariants(HUANG));
  }

  @Test
  @DisplayName("todo invariante de plaza de travelAgencySystem cumple Wᵀ·x = 0")
  void testTravelPlaceInvariantsAreValid() {
    assertAnnihilates(transposedIncidence(TRAVEL), placeInvariants(TRAVEL));
  }

  @Test
  @DisplayName("los invariantes de plaza son linealmente independientes")
  void testPlaceInvariantsAreIndependent() {
    for (String net : new String[] {HUANG, TRAVEL}) {
      List<int[]> basis = placeInvariants(net);
      assertEquals(basis.size(), rank(toMatrix(basis)), "base con vectores redundantes en " + net);
    }
  }

  @Test
  @DisplayName("se cumple rango(Wᵀ) + cantidad de invariantes = cantidad de lugares")
  void testRankNullityTheorem() {
    for (String net : new String[] {HUANG, TRAVEL}) {
      int[][] wt = transposedIncidence(net);
      int places = wt[0].length;
      assertEquals(
          places,
          rank(wt) + placeInvariants(net).size(),
          "falla el teorema rango-nulidad en " + net);
    }
  }

  @Test
  @DisplayName("cada invariante está reducido: el GCD de sus componentes es 1")
  void testInvariantsAreInLowestTerms() {
    for (String net : new String[] {HUANG, TRAVEL}) {
      for (int[] v : placeInvariants(net)) {
        assertEquals(1, Matrix.gcdArray(v), "vector sin reducir en " + net + ": " + str(v));
      }
    }
  }

  @Test
  @DisplayName("cada invariante tiene un componente por lugar")
  void testInvariantLength() {
    assertEquals(14, placeInvariants(HUANG).get(0).length);
    assertEquals(15, placeInvariants(TRAVEL).get(0).length);
  }

  // ── invariantes de transición (nullspace de W) ────────────

  @Test
  @DisplayName("ambas redes tienen 3 invariantes de transición")
  void testTransitionInvariantCount() {
    assertEquals(3, Nullspace.compute(incidence(HUANG)).size());
    assertEquals(3, Nullspace.compute(incidence(TRAVEL)).size());
  }

  @Test
  @DisplayName("todo invariante de transición cumple W·y = 0")
  void testTransitionInvariantsAreValid() {
    for (String net : new String[] {HUANG, TRAVEL}) {
      int[][] w = incidence(net);
      assertAnnihilates(w, Nullspace.compute(w));
    }
  }

  // ── relación con la configuración ─────────────────────────

  @Test
  @DisplayName("ninguna red supera execution.max_invariants")
  void testInvariantCountFitsConfiguredLimit() {
    int limit = ConfigLoader.getMaxInvariants();
    assertTrue(placeInvariants(HUANG).size() <= limit, "exampleHuang supera el límite configurado");
    assertTrue(placeInvariants(TRAVEL).size() <= limit, "travelAgencySystem supera el límite");
  }

  @Test
  @DisplayName("el catálogo expone exactamente las dos redes del proyecto")
  void testCatalogListsBothNets() {
    List<PetriNetCatalog.CatalogEntry> entries = new PetriNetCatalog().list();
    assertEquals(2, entries.size());
    assertEquals("0", entries.get(0).id());
    assertEquals("exampleHuang", entries.get(0).name());
    assertEquals("1", entries.get(1).id());
    assertEquals("travelAgencySystem", entries.get(1).name());
  }

  // ── test de caracterización (golden) ──────────────────────

  @Test
  @DisplayName("GOLDEN: base de invariantes de exampleHuang, tal como se calcula hoy")
  void testHuangGoldenBasis() {
    // Fija la base exacta que produce el algoritmo actual, incluyendo su convención de
    // signo y orden. NO es una propiedad matemática: cualquier base del mismo subespacio
    // sería igual de correcta. Si este test falla y los demás pasan, no hay un bug —
    // cambió la convención, y hay que revisar y actualizar los valores esperados.
    int[][] expected = {
      {1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
      {0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
      {0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
      {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0},
      {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0},
      {-1, 0, -1, -1, 0, 0, 0, -1, 0, -1, 0, 0, 1, 0},
      {0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1},
    };
    assertArrayEquals(expected, toMatrix(placeInvariants(HUANG)));
  }

  @Test
  @DisplayName("OBSERVACIÓN: la base contiene invariantes con componentes negativos")
  void testBasisIsNotSemiPositive() {
    // Un invariante de plaza "clásico" es semipositivo: representa una suma de tokens que
    // se conserva. El nullspace devuelve una base cualquiera del subespacio, que puede
    // incluir vectores con signos mezclados — matemáticamente correctos, pero no
    // interpretables como conservación de tokens. Obtener la base semipositiva mínima
    // requiere el algoritmo de Farkas, no eliminación gaussiana.
    assertTrue(hasNegativeComponent(placeInvariants(HUANG)), "exampleHuang");
    assertTrue(hasNegativeComponent(placeInvariants(TRAVEL)), "travelAgencySystem");
  }

  // ── helpers ───────────────────────────────────────────────

  /**
   * Carga una red del classpath por nombre de archivo.
   *
   * @param resource el nombre del recurso
   * @return la definición validada
   */
  private static PetriNetDefinition load(String resource) {
    return PetriNetProperties.fromResource(resource).toDefinition();
  }

  /**
   * Calcula la matriz de incidencia W = post - pre.
   *
   * @param resource el nombre del recurso
   * @return W, con dimensión lugares x transiciones
   */
  private static int[][] incidence(String resource) {
    PetriNetDefinition d = load(resource);
    return Matrix.subtract(d.post(), d.pre());
  }

  /**
   * Calcula Wᵀ, cuya nullspace son los invariantes de plaza.
   *
   * @param resource el nombre del recurso
   * @return Wᵀ, con dimensión transiciones x lugares
   */
  private static int[][] transposedIncidence(String resource) {
    return Matrix.transposed(incidence(resource));
  }

  /**
   * Calcula los invariantes de plaza de una red.
   *
   * @param resource el nombre del recurso
   * @return la base entera de la nullspace de Wᵀ
   */
  private static List<int[]> placeInvariants(String resource) {
    return Nullspace.compute(transposedIncidence(resource));
  }

  /**
   * Verifica que cada vector de la base anule a la matriz dada.
   *
   * @param m la matriz
   * @param basis los vectores que deben cumplir m·v = 0
   */
  private static void assertAnnihilates(int[][] m, List<int[]> basis) {
    for (int[] v : basis) {
      for (int i = 0; i < m.length; i++) {
        long dot = 0;
        for (int j = 0; j < v.length; j++) {
          dot += (long) m[i][j] * v[j];
        }
        assertEquals(0L, dot, "fila " + i + " por " + str(v) + " debe dar 0");
      }
    }
  }

  /**
   * Calcula el rango de una matriz entera por eliminación gaussiana exacta.
   *
   * @param m la matriz
   * @return la cantidad de columnas pivote
   */
  private static int rank(int[][] m) {
    Rational[][] a = Matrix.toRational(m);
    int[] pivots = new int[m[0].length];
    Matrix.gaussianElimination(a, pivots);

    int rank = 0;
    for (int p : pivots) {
      if (p != -1) {
        rank++;
      }
    }
    return rank;
  }

  /**
   * Indica si alguno de los vectores tiene al menos un componente negativo.
   *
   * @param basis los vectores a inspeccionar
   * @return true si hay algún componente negativo
   */
  private static boolean hasNegativeComponent(List<int[]> basis) {
    for (int[] v : basis) {
      for (int x : v) {
        if (x < 0) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Convierte una lista de vectores en una matriz.
   *
   * @param basis los vectores
   * @return la matriz con un vector por fila
   */
  private static int[][] toMatrix(List<int[]> basis) {
    return basis.toArray(new int[0][]);
  }

  /**
   * Suma los elementos de un vector.
   *
   * @param v el vector
   * @return la suma de sus componentes
   */
  private static int sum(int[] v) {
    int total = 0;
    for (int x : v) {
      total += x;
    }
    return total;
  }

  /**
   * Cuenta cuántos lugares tienen al menos un token.
   *
   * @param v el vector de marcado
   * @return la cantidad de componentes mayores a cero
   */
  private static int countMarked(int[] v) {
    int marked = 0;
    for (int x : v) {
      if (x > 0) {
        marked++;
      }
    }
    return marked;
  }

  /**
   * Representación legible de un vector, para los mensajes de error.
   *
   * @param v el vector
   * @return el vector como texto
   */
  private static String str(int[] v) {
    return java.util.Arrays.toString(v);
  }
}
