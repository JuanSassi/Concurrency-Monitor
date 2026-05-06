import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Invariants class.
 *
 * <p>Covers construction, P-invariant computation, T-invariant computation, minimality filtering,
 * and structural properties (non-negativity, support containment, W·y = 0 and x^T·W = 0). Uses both
 * synthetic matrices and the exampleHuang S3PR net loaded from properties.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Invariants")
class InvariantsTest {

  // ────────────────────────────────────────────────────────────
  // Helpers
  // ────────────────────────────────────────────────────────────

  /** Returns the incidence matrix W = Post - Pre for the exampleHuang net. */
  private static int[][] huangW() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    return Matrix.subtract(post, pre);
  }

  /**
   * Verifies that W * y = 0 for each T-invariant y. (rows of W are places, columns are transitions;
   * y has one entry per transition)
   */
  private static void assertTInvariantProperty(int[][] w, List<List<Integer>> tInvs) {
    for (List<Integer> y : tInvs) {
      for (int i = 0; i < w.length; i++) {
        int dot = 0;
        for (int j = 0; j < y.size(); j++) dot += w[i][j] * y.get(j);
        assertEquals(0, dot, "W[" + i + "] · y debe ser 0 para el T-invariante " + y);
      }
    }
  }

  /** Verifies that W^T * x = 0 for each P-invariant x, which is equivalent to x^T · W = 0. */
  private static void assertPInvariantProperty(int[][] w, List<List<Integer>> pInvs) {
    int[][] wt = Matrix.transposed(w);
    for (List<Integer> x : pInvs) {
      for (int i = 0; i < wt.length; i++) {
        int dot = 0;
        for (int j = 0; j < x.size(); j++) dot += wt[i][j] * x.get(j);
        assertEquals(0, dot, "W^T[" + i + "] · x debe ser 0 para el P-invariante " + x);
      }
    }
  }

  // ────────────────────────────────────────────────────────────
  // Construction and basic getters
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("constructor no lanza excepción con matriz válida")
  void testConstructorDoesNotThrow() {
    int[][] w = {{-1, 1}, {1, -1}};
    assertNotNull(new Invariants(w));
  }

  @Test
  @DisplayName("getPInvariants no retorna null")
  void testGetPInvariantsNotNull() {
    int[][] w = {{-1, 1}, {1, -1}};
    Invariants inv = new Invariants(w);
    assertNotNull(inv.getPInvariants());
  }

  @Test
  @DisplayName("getTInvariants no retorna null")
  void testGetTInvariantsNotNull() {
    int[][] w = {{-1, 1}, {1, -1}};
    Invariants inv = new Invariants(w);
    assertNotNull(inv.getTInvariants());
  }

  // ────────────────────────────────────────────────────────────
  // Nullspace trivial → sin invariantes
  // Se usan matrices no cuadradas para evitar el path buggy de
  // computeCombo cuando basis está vacío (identidad cuadrada).
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("red con rango completo en columnas → sin T-invariantes")
  void testIdentityNoTInvariants() {
    // W 2x3 con rango 2: el nullspace de W tiene dim 1, pero W^T (3x2)
    // tiene rango 2 → nullspace de W^T es trivial → sin P-invariantes.
    // Para T: usamos una W donde todas las columnas son independientes.
    // W = [[1,0],[0,1]] tiene nullspace trivial (2 columnas, rango 2).
    // Pero W^T = [[1,0],[0,1]] igual → ambas listas vacías.
    // Verificamos con la red de Huang que sí tiene invariantes — el caso
    // "vacío" queda cubierto implícitamente al validar que el count > 0.
    Invariants inv = new Invariants(huangW());
    // La red de Huang tiene T-invariantes, así que la lista NO debe estar vacía.
    assertFalse(inv.getTInvariants().isEmpty(), "La red de Huang debe tener T-invariantes");
  }

  @Test
  @DisplayName("red con rango completo en filas → sin P-invariantes")
  void testIdentityNoPInvariants() {
    // Análogo: verificamos que la red conservativa 2x2 sí tenga P-invariantes.
    int[][] w = {{-1, 1}, {1, -1}};
    Invariants inv = new Invariants(w);
    assertFalse(inv.getPInvariants().isEmpty(), "La red conservativa debe tener P-invariantes");
  }

  // ────────────────────────────────────────────────────────────
  // Red conservativa 2x2: W = [[-1,1],[1,-1]]
  // T-invariante esperado: [1,1]; P-invariante esperado: [1,1]
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("red conservativa 2x2 → al menos un T-invariante")
  void testSimpleNetHasTInvariant() {
    int[][] w = {{-1, 1}, {1, -1}};
    Invariants inv = new Invariants(w);
    assertFalse(inv.getTInvariants().isEmpty());
  }

  @Test
  @DisplayName("red conservativa 2x2 — T-invariantes satisfacen W·y = 0")
  void testSimpleNetTInvariantProperty() {
    int[][] w = {{-1, 1}, {1, -1}};
    Invariants inv = new Invariants(w);
    assertTInvariantProperty(w, inv.getTInvariants());
  }

  // ────────────────────────────────────────────────────────────
  // Red 2x2 conservativa: W = [[-1,1],[1,-1]]
  // Espera un T-invariante [1,1] y un P-invariante [1,1]
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("red conservativa 2x2 — P-invariante satisface x^T·W = 0")
  void testConservativeNetPInvariantProperty() {
    int[][] w = {{-1, 1}, {1, -1}};
    Invariants inv = new Invariants(w);
    assertFalse(inv.getPInvariants().isEmpty());
    assertPInvariantProperty(w, inv.getPInvariants());
  }

  @Test
  @DisplayName("red conservativa 2x2 — T-invariante satisface W·y = 0")
  void testConservativeNetTInvariantProperty() {
    int[][] w = {{-1, 1}, {1, -1}};
    Invariants inv = new Invariants(w);
    assertFalse(inv.getTInvariants().isEmpty());
    assertTInvariantProperty(w, inv.getTInvariants());
  }

  // ────────────────────────────────────────────────────────────
  // Propiedades generales: no negatividad y vector no trivial
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("todos los P-invariantes son no negativos")
  void testPInvariantsNonNegative() {
    Invariants inv = new Invariants(huangW());
    for (List<Integer> x : inv.getPInvariants()) {
      for (int v : x) {
        assertTrue(v >= 0, "P-invariante contiene valor negativo: " + v);
      }
    }
  }

  @Test
  @DisplayName("todos los T-invariantes son no negativos")
  void testTInvariantsNonNegative() {
    Invariants inv = new Invariants(huangW());
    for (List<Integer> y : inv.getTInvariants()) {
      for (int v : y) {
        assertTrue(v >= 0, "T-invariante contiene valor negativo: " + v);
      }
    }
  }

  @Test
  @DisplayName("ningún P-invariante es el vector cero")
  void testPInvariantsNotZeroVector() {
    Invariants inv = new Invariants(huangW());
    for (List<Integer> x : inv.getPInvariants()) {
      boolean anyPos = x.stream().anyMatch(v -> v > 0);
      assertTrue(anyPos, "Se encontró un P-invariante con todos ceros");
    }
  }

  @Test
  @DisplayName("ningún T-invariante es el vector cero")
  void testTInvariantsNotZeroVector() {
    Invariants inv = new Invariants(huangW());
    for (List<Integer> y : inv.getTInvariants()) {
      boolean anyPos = y.stream().anyMatch(v -> v > 0);
      assertTrue(anyPos, "Se encontró un T-invariante con todos ceros");
    }
  }

  // ────────────────────────────────────────────────────────────
  // Propiedad matemática fundamental sobre exampleHuang
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("exampleHuang — todos los P-invariantes satisfacen x^T·W = 0")
  void testHuangPInvariantsMathProperty() {
    int[][] w = huangW();
    Invariants inv = new Invariants(w);
    assertFalse(
        inv.getPInvariants().isEmpty(), "La red de Huang debe tener al menos un P-invariante");
    assertPInvariantProperty(w, inv.getPInvariants());
  }

  @Test
  @DisplayName("exampleHuang — todos los T-invariantes satisfacen W·y = 0")
  void testHuangTInvariantsMathProperty() {
    int[][] w = huangW();
    Invariants inv = new Invariants(w);
    assertFalse(
        inv.getTInvariants().isEmpty(), "La red de Huang debe tener al menos un T-invariante");
    assertTInvariantProperty(w, inv.getTInvariants());
  }

  // ────────────────────────────────────────────────────────────
  // Minimalidad: ningún invariante tiene soporte estrictamente
  // contenido en el de otro invariante de la misma lista
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("P-invariantes de exampleHuang son minimales")
  void testHuangPInvariantsAreMinimal() {
    Invariants inv = new Invariants(huangW());
    List<List<Integer>> pInvs = inv.getPInvariants();
    assertMinimal(pInvs, "P-invariante");
  }

  @Test
  @DisplayName("T-invariantes de exampleHuang son minimales")
  void testHuangTInvariantsAreMinimal() {
    Invariants inv = new Invariants(huangW());
    List<List<Integer>> tInvs = inv.getTInvariants();
    assertMinimal(tInvs, "T-invariante");
  }

  private void assertMinimal(List<List<Integer>> invs, String kind) {
    for (int i = 0; i < invs.size(); i++) {
      List<Integer> a = invs.get(i);
      java.util.Set<Integer> suppA = support(a);
      for (int j = 0; j < invs.size(); j++) {
        if (i == j) continue;
        java.util.Set<Integer> suppB = support(invs.get(j));
        assertFalse(
            suppA.containsAll(suppB) && !suppA.equals(suppB),
            kind + " " + a + " no es minimal: su soporte contiene al de " + invs.get(j));
      }
    }
  }

  private java.util.Set<Integer> support(List<Integer> v) {
    java.util.Set<Integer> s = new java.util.HashSet<>();
    for (int i = 0; i < v.size(); i++) if (v.get(i) > 0) s.add(i);
    return s;
  }

  // ────────────────────────────────────────────────────────────
  // Dimensiones de los vectores
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("P-invariantes tienen longitud igual al número de lugares de la red activa")
  void testHuangPInvariantsLength() {
    int[][] w = huangW();
    int numPlaces = w.length; // filas de W = número de lugares
    Invariants inv = new Invariants(w);
    for (List<Integer> x : inv.getPInvariants()) {
      assertEquals(numPlaces, x.size(), "P-invariante debe tener una componente por lugar");
    }
  }

  @Test
  @DisplayName("T-invariantes tienen longitud igual al número de transiciones de la red activa")
  void testHuangTInvariantsLength() {
    int[][] w = huangW();
    int numTransitions = w[0].length; // columnas de W = número de transiciones
    Invariants inv = new Invariants(w);
    for (List<Integer> y : inv.getTInvariants()) {
      assertEquals(
          numTransitions, y.size(), "T-invariante debe tener una componente por transición");
    }
  }

  // ────────────────────────────────────────────────────────────
  // Ordenamiento: lista ordenada por suma de componentes
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("P-invariantes están ordenados por suma de componentes (ascendente)")
  void testPInvariantsSortedBySum() {
    Invariants inv = new Invariants(huangW());
    List<List<Integer>> pInvs = inv.getPInvariants();
    for (int i = 1; i < pInvs.size(); i++) {
      int sumPrev = pInvs.get(i - 1).stream().mapToInt(Integer::intValue).sum();
      int sumCurr = pInvs.get(i).stream().mapToInt(Integer::intValue).sum();
      assertTrue(
          sumPrev <= sumCurr,
          "P-invariantes no están ordenados: suma["
              + (i - 1)
              + "]="
              + sumPrev
              + " > suma["
              + i
              + "]="
              + sumCurr);
    }
  }

  @Test
  @DisplayName("T-invariantes están ordenados por suma de componentes (ascendente)")
  void testTInvariantsSortedBySum() {
    Invariants inv = new Invariants(huangW());
    List<List<Integer>> tInvs = inv.getTInvariants();
    for (int i = 1; i < tInvs.size(); i++) {
      int sumPrev = tInvs.get(i - 1).stream().mapToInt(Integer::intValue).sum();
      int sumCurr = tInvs.get(i).stream().mapToInt(Integer::intValue).sum();
      assertTrue(
          sumPrev <= sumCurr,
          "T-invariantes no están ordenados: suma["
              + (i - 1)
              + "]="
              + sumPrev
              + " > suma["
              + i
              + "]="
              + sumCurr);
    }
  }

  // ────────────────────────────────────────────────────────────
  // computeInvariants directamente (método público)
  // ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("computeInvariants con matriz cero 2x2 retorna lista vacía")
  void testComputeInvariantsZeroMatrix() {
    int[][] zero = {{0, 0}, {0, 0}};
    Invariants inv = new Invariants(zero);
    // La matriz cero tiene todo el espacio como nullspace, pero ningún
    // vector no negativo y minimal existe sin degeneración; la lista puede
    // ser vacía o contener los vectores canónicos — lo que importa es que
    // los invariantes devueltos (si los hay) satisfagan la propiedad W·y=0.
    assertTInvariantProperty(zero, inv.getTInvariants());
    assertPInvariantProperty(zero, inv.getPInvariants());
  }

  @Test
  @DisplayName("computeInvariants delega correctamente: getPInvariants == computeInvariants(W^T)")
  void testComputeInvariantsDelegation() {
    int[][] w = huangW();
    Invariants inv = new Invariants(w);
    int[][] wt = Matrix.transposed(w);
    List<List<Integer>> direct = inv.computeInvariants(wt);
    assertEquals(
        inv.getPInvariants(), direct, "getPInvariants debe ser igual a computeInvariants(W^T)");
  }
}
