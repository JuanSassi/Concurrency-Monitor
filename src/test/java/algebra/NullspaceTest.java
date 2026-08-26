import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Nullspace utility class.
 *
 * <p>Covers compute(), Matrix.toRational(), Matrix.gaussianElimination(), and buildBasisVector().
 * The key mathematical property verified is W*x = 0 for every basis vector x returned by compute().
 *
 * <p>Los casos que dependen de las redes reales del proyecto viven en {@code PetriNetFixturesTest};
 * acá todo es autocontenido, con matrices escritas en el propio test.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Nullspace")
class NullspaceTest {

  // ── Matrix.toRational ────────────────────────────────────

  @Test
  @DisplayName("toRational convierte correctamente los valores")
  void testToRational() {
    int[][] w = {{1, -2}, {3, 0}};
    Rational[][] r = Matrix.toRational(w);
    assertEquals(2, r.length);
    assertEquals(2, r[0].length);
    assertEquals(1L, r[0][0].num);
    assertEquals(-2L, r[0][1].num);
    assertEquals(3L, r[1][0].num);
    assertEquals(0L, r[1][1].num);
  }

  @Test
  @DisplayName("toRational — todos los denominadores son 1")
  void testToRationalDenominators() {
    int[][] w = {{5, 7}, {2, 4}};
    Rational[][] r = Matrix.toRational(w);
    for (Rational[] row : r) {
      for (Rational cell : row) {
        assertEquals(1L, cell.den);
      }
    }
  }

  // ── Matrix.gaussianElimination ───────────────────────────

  @Test
  @DisplayName("gaussianElimination — identidad 2x2 → pivots [0,1]")
  void testGaussianEliminationIdentity() {
    Rational[][] a = Matrix.toRational(new int[][] {{1, 0}, {0, 1}});
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    assertEquals(0, pivots[0]);
    assertEquals(1, pivots[1]);
  }

  @Test
  @DisplayName("gaussianElimination — columna cero → pivots[0] = -1")
  void testGaussianEliminationFreeColumn() {
    Rational[][] a = Matrix.toRational(new int[][] {{0, 1}, {0, 0}});
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    assertEquals(-1, pivots[0]);
    assertEquals(0, pivots[1]);
  }

  @Test
  @DisplayName("gaussianElimination — requiere intercambio de filas")
  void testGaussianEliminationNeedsRowSwap() {
    // El pivote de la columna 0 está en la fila 1: ejercita swapRows, que ningún
    // test anterior alcanzaba.
    Rational[][] a = Matrix.toRational(new int[][] {{0, 1}, {1, 0}});
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    assertEquals(0, pivots[0]);
    assertEquals(1, pivots[1]);
    assertRref(a, new long[][] {{1, 0}, {0, 1}});
  }

  @Test
  @DisplayName("gaussianElimination — pivote distinto de 1 se normaliza")
  void testGaussianEliminationNormalizesPivot() {
    // Ejercita normalizeRow con aritmética fraccionaria real: 2x + 4y = 0 → x + 2y = 0.
    Rational[][] a = Matrix.toRational(new int[][] {{2, 4}, {3, 6}});
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    assertEquals(0, pivots[0]);
    assertEquals(-1, pivots[1]);
    assertRref(a, new long[][] {{1, 2}, {0, 0}});
  }

  @Test
  @DisplayName("gaussianElimination — matriz nula no tiene ningún pivote")
  void testGaussianEliminationZeroMatrix() {
    Rational[][] a = Matrix.toRational(new int[][] {{0, 0}, {0, 0}});
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    assertEquals(-1, pivots[0]);
    assertEquals(-1, pivots[1]);
  }

  // ── Nullspace.buildBasisVector ───────────────────────────

  @Test
  @DisplayName("buildBasisVector para [1, -1] → vector reducido [1, 1]")
  void testBuildBasisVectorSimple() {
    int[][] w = {{1, -1}};
    Rational[][] a = Matrix.toRational(w);
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);

    List<Integer> freeVars = freeVariables(pivots);
    int[] vec = Nullspace.buildBasisVector(a, pivots, freeVars, freeVars.get(0), 2);

    assertArrayEquals(new int[] {1, 1}, vec);
  }

  @Test
  @DisplayName("buildBasisVector elimina denominadores: [2, 3] → [-3, 2]")
  void testBuildBasisVectorClearsDenominators() {
    // La solución racional es (-3/2, 1); el resultado debe ser el entero reducido.
    int[][] w = {{2, 3}};
    Rational[][] a = Matrix.toRational(w);
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);

    List<Integer> freeVars = freeVariables(pivots);
    int[] vec = Nullspace.buildBasisVector(a, pivots, freeVars, freeVars.get(0), 2);

    assertArrayEquals(new int[] {-3, 2}, vec);
    assertEquals(1, Matrix.gcdArray(vec), "el vector debe quedar en términos mínimos");
  }

  // ── Nullspace.compute ────────────────────────────────────

  @Test
  @DisplayName("matriz identidad 3x3 → nullspace vacío")
  void testComputeIdentityEmptyNullspace() {
    int[][] identity = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    assertTrue(Nullspace.compute(identity).isEmpty());
  }

  @Test
  @DisplayName("fila de ceros → nullspace no vacío")
  void testComputeZeroRowHasNullspace() {
    int[][] w = {{1, 0, 0}, {0, 1, 0}, {0, 0, 0}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(1, basis.size());
    assertArrayEquals(new int[] {0, 0, 1}, basis.get(0));
  }

  @Test
  @DisplayName("columna cero → vector canónico en nullspace")
  void testComputeZeroColumn() {
    int[][] w = {{1, 0}, {0, 0}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(1, basis.size());
    assertArrayEquals(new int[] {0, 1}, basis.get(0));
  }

  @Test
  @DisplayName("nullspace de [1, -1] → [1, 1]")
  void testComputeSimpleKernel() {
    List<int[]> basis = Nullspace.compute(new int[][] {{1, -1}});
    assertEquals(1, basis.size());
    assertArrayEquals(new int[] {1, 1}, basis.get(0));
  }

  @Test
  @DisplayName("matriz nula 3x3 → base canónica completa")
  void testComputeZeroMatrix() {
    List<int[]> basis = Nullspace.compute(new int[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
    assertEquals(3, basis.size());
    assertArrayEquals(new int[] {1, 0, 0}, basis.get(0));
    assertArrayEquals(new int[] {0, 1, 0}, basis.get(1));
    assertArrayEquals(new int[] {0, 0, 1}, basis.get(2));
  }

  @Test
  @DisplayName("filas dependientes → dos variables libres")
  void testComputeMultipleFreeVariables() {
    // rango 1 sobre 3 columnas ⇒ nulidad 2.
    int[][] w = {{1, 2, -1}, {2, 4, -2}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(2, basis.size());
    assertAnnihilates(w, basis);
  }

  @Test
  @DisplayName("una columna libre antes de una columna pivote")
  void testComputeFreeColumnBeforePivot() {
    // La columna 0 es libre y las columnas 1 y 2 tienen pivote: verifica que
    // normalizeRow y eliminateColumn saltear las columnas previas es correcto.
    int[][] w = {{0, 1, 2}, {0, 0, 0}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(2, basis.size());
    assertAnnihilates(w, basis);
  }

  @Test
  @DisplayName("con pivotes fraccionarios el resultado sigue siendo exacto")
  void testComputeWithFractionalPivots() {
    int[][] w = {{2, 3, 4}, {4, 6, 8}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(2, basis.size());
    assertAnnihilates(w, basis);
    for (int[] v : basis) {
      assertEquals(1, Matrix.gcdArray(v), "vector sin reducir");
    }
  }

  @Test
  @DisplayName("todos los vectores devueltos tienen largo n")
  void testComputeVectorLength() {
    for (int[] v : Nullspace.compute(new int[][] {{1, 1, 1, 1}})) {
      assertEquals(4, v.length);
    }
  }

  @Test
  @DisplayName("todo vector del nullspace satisface W*x = 0 — red de incidencia")
  void testComputeWxEqualsZero() {
    // Matriz de incidencia autocontenida (no depende de config.properties ni de qué red
    // esté seleccionada). El caso equivalente sobre las redes reales del proyecto está
    // en PetriNetFixturesTest.
    int[][] pre = {{1, 0, 0}, {0, 1, 0}, {1, 0, 0}, {0, 0, 1}};
    int[][] post = {{0, 1, 0}, {0, 0, 1}, {0, 0, 1}, {1, 0, 0}};

    int[][] w = Matrix.subtract(post, pre);
    int[][] wt = Matrix.transposed(w);

    List<int[]> basis = Nullspace.compute(wt);
    assertFalse(basis.isEmpty(), "la red debe tener invariantes de plaza");
    assertAnnihilates(wt, basis);
  }

  // ── helpers ──────────────────────────────────────────────

  /**
   * Devuelve los índices de columna sin pivote.
   *
   * @param pivots el mapa de pivotes de gaussianElimination
   * @return las columnas correspondientes a variables libres
   */
  private static List<Integer> freeVariables(int[] pivots) {
    List<Integer> free = new ArrayList<>();
    for (int j = 0; j < pivots.length; j++) {
      if (pivots[j] == -1) {
        free.add(j);
      }
    }
    return free;
  }

  /**
   * Verifica que cada vector de la base anule a la matriz.
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
        assertEquals(0L, dot, "la fila " + i + " no se anula");
      }
    }
  }

  /**
   * Verifica que la matriz reducida coincida con los enteros esperados.
   *
   * @param a la matriz racional ya reducida
   * @param expected los numeradores esperados, todos con denominador 1
   */
  private static void assertRref(Rational[][] a, long[][] expected) {
    for (int i = 0; i < expected.length; i++) {
      for (int j = 0; j < expected[i].length; j++) {
        assertEquals(expected[i][j], a[i][j].num, "celda [" + i + "][" + j + "] numerador");
        assertEquals(1L, a[i][j].den, "celda [" + i + "][" + j + "] denominador");
      }
    }
  }
}
