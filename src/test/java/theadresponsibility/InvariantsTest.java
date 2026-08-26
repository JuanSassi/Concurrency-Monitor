import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Invariants}.
 *
 * <p>A diferencia de {@code Nullspace}, que devuelve una base cualquiera del espacio nulo, esta
 * clase busca los invariantes <b>minimales y semipositivos</b>: los que se interpretan como
 * conservación de tokens o secuencias de disparo cíclicas.
 *
 * <p>El cálculo sobre las redes reales es caro (la búsqueda visita 9^k hojas, ~1,7 s para
 * exampleHuang), así que se hace una sola vez y se reutiliza.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Invariants")
class InvariantsTest {

  /** Invariantes de exampleHuang, calculados una sola vez para toda la clase. */
  private static Invariants huang;

  /**
   * Devuelve los invariantes de exampleHuang, calculándolos la primera vez.
   *
   * @return el calculador ya poblado
   */
  private static synchronized Invariants huang() {
    if (huang == null) {
      huang = new Invariants(incidenceOf("exampleHuang.properties"));
    }
    return huang;
  }

  // ── casos mínimos ─────────────────────────────────────────

  @Test
  @DisplayName("ciclo de dos plazas: un P-invariante y un T-invariante")
  void testSimpleCycle() {
    // P0 -T0-> P1 -T1-> P0. Se conserva la suma de tokens y disparar T0,T1 vuelve al inicio.
    Invariants inv =
        new Invariants(incidence(new int[][] {{1, 0}, {0, 1}}, new int[][] {{0, 1}, {1, 0}}));

    assertEquals(List.of(List.of(1, 1)), inv.getPInvariants());
    assertEquals(List.of(List.of(1, 1)), inv.getTInvariants());
  }

  @Test
  @DisplayName("BUG DOCUMENTADO: espacio nulo vacío hace crashear con IndexOutOfBoundsException")
  void testEmptyNullspaceCrashes() {
    // computeCombo hace basis.get(0).length sin comprobar que la base tenga elementos.
    // Cualquier red cuya matriz de incidencia tenga rango columna completo — es decir,
    // sin ningún invariante — revienta con una excepción cruda en vez de devolver una
    // lista vacía. En el servidor web eso es un 500 por una red perfectamente válida.
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> new Invariants(incidence(new int[][] {{1}}, new int[][] {{0}})));
  }

  @Test
  @DisplayName("sin combinación semipositiva devuelve listas vacías")
  void testNoSemiPositiveCombination() {
    // Acá la base del espacio nulo SÍ tiene elementos, pero ninguna combinación con
    // coeficientes en [-4, 4] resulta semipositiva. Éste es el camino que sí termina bien.
    Invariants inv = new Invariants(new int[][] {{1, 1}, {1, 1}});

    assertTrue(inv.getPInvariants().isEmpty());
    assertTrue(inv.getTInvariants().isEmpty());
  }

  @Test
  @DisplayName("dos ciclos independientes dan dos invariantes disjuntos")
  void testTwoIndependentCycles() {
    int[][] pre = {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
    int[][] post = {{0, 1, 0, 0}, {1, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 1, 0}};
    Invariants inv = new Invariants(incidence(pre, post));

    // Los dos invariantes tienen la misma suma de componentes, así que el orden entre
    // ellos no está definido: computeInvariants ordena por suma y el desempate depende
    // del recorrido de un HashSet. Se comparan como conjunto.
    assertEquals(2, inv.getPInvariants().size());
    assertEquals(
        Set.of(List.of(1, 1, 0, 0), List.of(0, 0, 1, 1)), new HashSet<>(inv.getPInvariants()));
  }

  // ── propiedades matemáticas ───────────────────────────────

  @Test
  @DisplayName("todo P-invariante cumple Wᵀ·x = 0")
  void testPInvariantsAnnihilateTransposed() {
    int[][] w = incidenceOf("exampleHuang.properties");
    assertAnnihilates(Matrix.transposed(w), huang().getPInvariants());
  }

  @Test
  @DisplayName("todo T-invariante cumple W·y = 0")
  void testTInvariantsAnnihilate() {
    assertAnnihilates(incidenceOf("exampleHuang.properties"), huang().getTInvariants());
  }

  @Test
  @DisplayName("todo invariante es semipositivo y no nulo")
  void testInvariantsAreSemiPositive() {
    // Ésta es la diferencia clave con Nullspace.compute, que sí devuelve componentes negativos.
    for (List<Integer> inv : allOf(huang())) {
      boolean anyPositive = false;
      for (int v : inv) {
        assertTrue(v >= 0, "componente negativo en " + inv);
        anyPositive |= v > 0;
      }
      assertTrue(anyPositive, "el vector nulo no es un invariante válido: " + inv);
    }
  }

  @Test
  @DisplayName("todo invariante está reducido: el GCD de sus componentes es 1")
  void testInvariantsAreReduced() {
    for (List<Integer> inv : allOf(huang())) {
      assertEquals(1, Matrix.gcdArray(toArray(inv)), "vector sin reducir: " + inv);
    }
  }

  @Test
  @DisplayName("los invariantes salen ordenados por suma de componentes creciente")
  void testInvariantsAreSortedBySum() {
    assertSortedBySum(huang().getPInvariants());
    assertSortedBySum(huang().getTInvariants());
  }

  @Test
  @DisplayName("ningún invariante tiene soporte que contenga estrictamente al de otro")
  void testInvariantsAreMinimal() {
    List<List<Integer>> invariants = huang().getPInvariants();
    for (List<Integer> a : invariants) {
      for (List<Integer> b : invariants) {
        if (a.equals(b)) {
          continue;
        }
        Set<Integer> supportA = support(a);
        Set<Integer> supportB = support(b);
        assertFalse(
            supportA.containsAll(supportB) && !supportA.equals(supportB),
            "el soporte de " + a + " contiene estrictamente al de " + b);
      }
    }
  }

  @Test
  @DisplayName("no hay invariantes duplicados")
  void testNoDuplicates() {
    assertEquals(new HashSet<>(huang().getPInvariants()).size(), huang().getPInvariants().size());
    assertEquals(new HashSet<>(huang().getTInvariants()).size(), huang().getTInvariants().size());
  }

  // ── redes reales ──────────────────────────────────────────

  @Test
  @DisplayName("exampleHuang: 7 P-invariantes y 3 T-invariantes")
  void testHuangCounts() {
    assertEquals(7, huang().getPInvariants().size());
    assertEquals(3, huang().getTInvariants().size());
  }

  @Test
  @DisplayName("GOLDEN: T-invariantes exactos de exampleHuang")
  void testHuangTInvariants() {
    assertEquals(
        List.of(
            List.of(1, 0, 1, 0, 1, 1, 0, 0, 0, 0),
            List.of(1, 1, 0, 1, 0, 1, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 1, 1, 1, 1)),
        huang().getTInvariants());
  }

  @Test
  @DisplayName("GOLDEN: P-invariantes exactos de exampleHuang")
  void testHuangPInvariants() {
    assertEquals(
        List.of(
            List.of(0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0),
            List.of(0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1),
            List.of(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0),
            List.of(0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0),
            List.of(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
        huang().getPInvariants());
  }

  @Test
  @DisplayName("los P-invariantes de exampleHuang conservan tokens desde M₀")
  void testHuangConservesTokens() {
    // Un P-invariante x cumple M·x = M₀·x en todo marcado alcanzable. Acá se verifica que
    // el valor conservado sea positivo: un invariante que arranca en 0 no restringe nada.
    int[] m0 =
        PetriNetProperties.fromResource("exampleHuang.properties").toDefinition().initialMarking();
    for (List<Integer> inv : huang().getPInvariants()) {
      int tokens = 0;
      for (int p = 0; p < inv.size(); p++) {
        tokens += inv.get(p) * m0[p];
      }
      assertTrue(tokens > 0, "invariante sin tokens iniciales: " + inv);
    }
  }

  // ── límite de complejidad ─────────────────────────────────

  @Test
  @DisplayName("un espacio nulo demasiado grande lanza PetriNetValidationException")
  void testSearchSpaceLimit() {
    // 9^8 = 43.046.721 supera el tope de 10.000.000 combinaciones.
    PetriNetValidationException e =
        assertThrows(PetriNetValidationException.class, () -> new Invariants(new int[1][8]));
    assertTrue(
        e.getMessage().contains("8"), "el mensaje debe indicar la dimensión del espacio nulo");
  }

  @Test
  @DisplayName("un espacio nulo de dimensión 7 sigue siendo aceptado")
  void testSearchSpaceLimitBoundary() {
    // 9^7 = 4.782.969, justo por debajo del tope. Es la dimensión de exampleHuang,
    // así que este límite no debe moverse sin revisar las redes del catálogo.
    assertEquals(7, huang().getPInvariants().size());
  }

  // ── inmutabilidad ─────────────────────────────────────────

  @Test
  @DisplayName("las listas devueltas son inmutables")
  void testReturnedListsAreUnmodifiable() {
    assertThrows(
        UnsupportedOperationException.class, () -> huang().getPInvariants().add(List.of(1)));
    assertThrows(UnsupportedOperationException.class, () -> huang().getTInvariants().clear());
  }

  // ── comportamiento documentado, no deseable ───────────────

  @Test
  @DisplayName("BUG DOCUMENTADO: computeInvariants escribe en System.out")
  void testWarningGoesToStdout() {
    // El javadoc de la clase dice que renderizar es responsabilidad del llamador, pero
    // cuando no encuentra invariantes imprime un WARNING por consola. En el servidor web
    // eso ensucia la salida estándar del proceso.
    java.io.PrintStream original = System.out;
    java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
    try {
      System.setOut(
          new java.io.PrintStream(captured, true, java.nio.charset.StandardCharsets.UTF_8));
      new Invariants(new int[][] {{1, 1}, {1, 1}});
    } finally {
      System.setOut(original);
    }
    assertTrue(
        captured.toString(java.nio.charset.StandardCharsets.UTF_8).contains("WARNING"),
        "se esperaba la salida por consola que hoy produce la clase");
  }

  // ── helpers ───────────────────────────────────────────────

  /**
   * Calcula la matriz de incidencia W = post - pre.
   *
   * @param pre matriz de pre-incidencia
   * @param post matriz de post-incidencia
   * @return la matriz de incidencia
   */
  private static int[][] incidence(int[][] pre, int[][] post) {
    return Matrix.subtract(post, pre);
  }

  /**
   * Calcula la matriz de incidencia de una red del classpath.
   *
   * @param resource el nombre del recurso
   * @return la matriz de incidencia de esa red
   */
  private static int[][] incidenceOf(String resource) {
    PetriNetDefinition d = PetriNetProperties.fromResource(resource).toDefinition();
    return Matrix.subtract(d.post(), d.pre());
  }

  /**
   * Junta P-invariantes y T-invariantes en una sola lista.
   *
   * @param inv el calculador
   * @return todos los invariantes
   */
  private static List<List<Integer>> allOf(Invariants inv) {
    List<List<Integer>> all = new ArrayList<>(inv.getPInvariants());
    all.addAll(inv.getTInvariants());
    return all;
  }

  /**
   * Verifica que cada invariante anule a la matriz dada.
   *
   * @param m la matriz
   * @param invariants los vectores que deben cumplir m·v = 0
   */
  private static void assertAnnihilates(int[][] m, List<List<Integer>> invariants) {
    for (List<Integer> v : invariants) {
      for (int i = 0; i < m.length; i++) {
        long dot = 0;
        for (int j = 0; j < v.size(); j++) {
          dot += (long) m[i][j] * v.get(j);
        }
        assertEquals(0L, dot, "la fila " + i + " no se anula con " + v);
      }
    }
  }

  /**
   * Verifica que la lista esté ordenada por suma de componentes creciente.
   *
   * @param invariants los invariantes a inspeccionar
   */
  private static void assertSortedBySum(List<List<Integer>> invariants) {
    int previous = Integer.MIN_VALUE;
    for (List<Integer> inv : invariants) {
      int sum = inv.stream().mapToInt(Integer::intValue).sum();
      assertTrue(sum >= previous, "orden roto en " + inv);
      previous = sum;
    }
  }

  /**
   * Calcula el soporte de un invariante.
   *
   * @param inv el vector
   * @return los índices con valor positivo
   */
  private static Set<Integer> support(List<Integer> inv) {
    Set<Integer> supp = new HashSet<>();
    for (int i = 0; i < inv.size(); i++) {
      if (inv.get(i) > 0) {
        supp.add(i);
      }
    }
    return supp;
  }

  /**
   * Convierte un invariante a array de enteros.
   *
   * @param inv el vector
   * @return el mismo vector como array
   */
  private static int[] toArray(List<Integer> inv) {
    int[] result = new int[inv.size()];
    for (int i = 0; i < inv.size(); i++) {
      result[i] = inv.get(i);
    }
    return result;
  }
}
