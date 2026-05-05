import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Invariants using the exampleHuang Petri net (S3PR net by Huang).
 *
 * <p>Net has 14 places and 10 transitions. W = Post - Pre.
 *
 * <p>Known minimal invariants for this net:
 *
 * <ul>
 *   <li>T-invariants: [0,0,0,0,0,0,1,1,1,1] and [1,0,1,0,1,1,0,0,0,0] and [1,1,0,1,0,1,0,0,0,0]
 *   <li>P-invariants: [0,0,0,0,0,0,0,1,1,1,1,0,0,0] and [0,0,0,1,0,0,1,0,0,0,0,0,0,0] and others
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Invariants — exampleHuang")
class Invariantstest {

  // Pre matrix (14 places x 10 transitions)
  private static final int[][] PRE = {
    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 1, 1, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
    {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
    {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
    {1, 0, 0, 1, 1, 0, 1, 0, 1, 0},
    {1, 0, 0, 0, 0, 0, 1, 0, 0, 0},
  };

  // Post matrix (14 places x 10 transitions)
  private static final int[][] POST = {
    {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
    {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 1, 1, 0, 0, 0, 0, 0},
    {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
    {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
    {0, 1, 1, 0, 0, 1, 0, 1, 0, 1},
    {0, 1, 1, 0, 0, 0, 0, 0, 1, 0},
  };

  private Invariants invariants;

  @BeforeEach
  void setUp() {
    int[][] w = Matrix.subtract(POST, PRE);
    invariants = new Invariants(w);
  }

  // ── cantidad de invariantes ───────────────────────────────

  @Test
  @DisplayName("T-invariants: al menos 3 invariantes encontrados")
  void testTInvariantsCount() {
    assertTrue(invariants.getTInvariants().size() >= 3);
  }

  @Test
  @DisplayName("P-invariants: al menos 4 invariantes encontrados")
  void testPInvariantsCount() {
    assertTrue(invariants.getPInvariants().size() >= 4);
  }

  // ── T-invariants conocidos ────────────────────────────────

  @Test
  @DisplayName("T-invariant [0,0,0,0,0,0,1,1,1,1] está presente (ciclo T6-T7-T8-T9)")
  void testTInvariantCycleT6toT9() {
    List<Integer> expected = Arrays.asList(0, 0, 0, 0, 0, 0, 1, 1, 1, 1);
    assertTrue(
        containsInvariant(invariants.getTInvariants(), expected),
        "Debe contener el T-invariante del ciclo T6-T9");
  }

  @Test
  @DisplayName("T-invariant [1,0,1,0,1,1,0,0,0,0] está presente (ciclo T0-T2-T4-T5)")
  void testTInvariantCycleT0T2T4T5() {
    List<Integer> expected = Arrays.asList(1, 0, 1, 0, 1, 1, 0, 0, 0, 0);
    assertTrue(
        containsInvariant(invariants.getTInvariants(), expected),
        "Debe contener el T-invariante del ciclo T0-T2-T4-T5");
  }

  @Test
  @DisplayName("T-invariant [1,1,0,1,0,1,0,0,0,0] está presente (ciclo T0-T1-T3-T5)")
  void testTInvariantCycleT0T1T3T5() {
    List<Integer> expected = Arrays.asList(1, 1, 0, 1, 0, 1, 0, 0, 0, 0);
    assertTrue(
        containsInvariant(invariants.getTInvariants(), expected),
        "Debe contener el T-invariante del ciclo T0-T1-T3-T5");
  }

  // ── P-invariants conocidos ────────────────────────────────

  @Test
  @DisplayName("P-invariant [0,0,0,0,0,0,0,1,1,1,1,0,0,0] está presente (ciclo P7-P10)")
  void testPInvariantCycleP7toP10() {
    List<Integer> expected = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0);
    assertTrue(
        containsInvariant(invariants.getPInvariants(), expected),
        "Debe contener el P-invariante P7+P8+P9+P10 = cte");
  }

  @Test
  @DisplayName("P-invariant [0,0,0,1,0,0,1,0,0,0,0,0,0,0] está presente (P3+P6 = cte)")
  void testPInvariantP3P6() {
    List<Integer> expected = Arrays.asList(0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0);
    assertTrue(
        containsInvariant(invariants.getPInvariants(), expected),
        "Debe contener el P-invariante P3+P6 = cte");
  }

  @Test
  @DisplayName("P-invariant [0,0,1,0,0,1,0,0,0,0,0,0,0,0] está presente (P2+P5 = cte)")
  void testPInvariantP2P5() {
    List<Integer> expected = Arrays.asList(0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0);
    assertTrue(
        containsInvariant(invariants.getPInvariants(), expected),
        "Debe contener el P-invariante P2+P5 = cte");
  }

  // ── verificación matemática: W·y = 0 ─────────────────────

  @Test
  @DisplayName("Todo T-invariant satisface W·y = 0")
  void testTInvariantsSatisfyWy0() {
    int[][] w = Matrix.subtract(POST, PRE);
    for (List<Integer> tInv : invariants.getTInvariants()) {
      for (int i = 0; i < w.length; i++) {
        int dot = 0;
        for (int j = 0; j < tInv.size(); j++) dot += w[i][j] * tInv.get(j);
        assertEquals(0, dot, "W[" + i + "] · y debe ser 0 para T-invariant " + tInv);
      }
    }
  }

  @Test
  @DisplayName("Todo P-invariant satisface x^T·W = 0")
  void testPInvariantsSatisfyXtW0() {
    int[][] w = Matrix.subtract(POST, PRE);
    int numTransitions = w[0].length;
    for (List<Integer> pInv : invariants.getPInvariants()) {
      for (int j = 0; j < numTransitions; j++) {
        int dot = 0;
        for (int i = 0; i < pInv.size(); i++) dot += pInv.get(i) * w[i][j];
        assertEquals(0, dot, "x^T · W[col " + j + "] debe ser 0 para P-invariant " + pInv);
      }
    }
  }

  // ── todos los invariantes son no-negativos ────────────────

  @Test
  @DisplayName("Todos los T-invariants son no-negativos")
  void testTInvariantsNonNegative() {
    for (List<Integer> tInv : invariants.getTInvariants())
      for (int v : tInv) assertTrue(v >= 0, "T-invariant contiene valor negativo: " + tInv);
  }

  @Test
  @DisplayName("Todos los P-invariants son no-negativos")
  void testPInvariantsNonNegative() {
    for (List<Integer> pInv : invariants.getPInvariants())
      for (int v : pInv) assertTrue(v >= 0, "P-invariant contiene valor negativo: " + pInv);
  }

  // ── no hay invariantes vacíos ─────────────────────────────

  @Test
  @DisplayName("Ningún T-invariant es el vector cero")
  void testNoZeroTInvariant() {
    for (List<Integer> tInv : invariants.getTInvariants()) {
      boolean anyPos = tInv.stream().anyMatch(v -> v > 0);
      assertTrue(anyPos, "T-invariant no debe ser el vector cero");
    }
  }

  @Test
  @DisplayName("Ningún P-invariant es el vector cero")
  void testNoZeroPInvariant() {
    for (List<Integer> pInv : invariants.getPInvariants()) {
      boolean anyPos = pInv.stream().anyMatch(v -> v > 0);
      assertTrue(anyPos, "P-invariant no debe ser el vector cero");
    }
  }

  // ── tamaño correcto de los vectores ──────────────────────

  @Test
  @DisplayName("T-invariants tienen longitud igual al número de transiciones (10)")
  void testTInvariantsLength() {
    for (List<Integer> tInv : invariants.getTInvariants())
      assertEquals(10, tInv.size(), "T-invariant debe tener 10 componentes");
  }

  @Test
  @DisplayName("P-invariants tienen longitud igual al número de plazas (14)")
  void testPInvariantsLength() {
    for (List<Integer> pInv : invariants.getPInvariants())
      assertEquals(14, pInv.size(), "P-invariant debe tener 14 componentes");
  }

  // ── helper ───────────────────────────────────────────────

  /**
   * Checks if a list of invariants contains a specific invariant vector.
   *
   * @param invariants the list of invariants to search
   * @param expected the invariant vector to look for
   * @return true if the invariant is found
   */
  private boolean containsInvariant(List<List<Integer>> invariants, List<Integer> expected) {
    return invariants.stream().anyMatch(inv -> inv.equals(expected));
  }
}
