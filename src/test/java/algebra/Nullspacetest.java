import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Nullspace utility class.
 *
 * <p>Covers compute(), toRational(), gaussianElimination(), buildBasisVector(), helper methods
 * (lcm, gcd, gcdArray) and the inner Rational class. The key mathematical property verified is W*x
 * = 0 for every basis vector x returned by compute().
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Nullspace")
class NullspaceTest {

  // ── helpers ──────────────────────────────────────────────

  @Test
  @DisplayName("gcd(12, 8) → 4")
  void testGcdLong() {
    assertEquals(4L, Nullspace.gcd(12L, 8L));
  }

  @Test
  @DisplayName("gcd(0, 5) → 5")
  void testGcdWithZero() {
    assertEquals(5L, Nullspace.gcd(0L, 5L));
  }

  @Test
  @DisplayName("lcm(6, 4) → 12")
  void testLcm() {
    assertEquals(12L, Nullspace.lcm(6L, 4L));
  }

  @Test
  @DisplayName("lcm(3, 3) → 3")
  void testLcmSame() {
    assertEquals(3L, Nullspace.lcm(3L, 3L));
  }

  @Test
  @DisplayName("gcdArray([6, 9, 3]) → 3")
  void testGcdArray() {
    assertEquals(3, Nullspace.gcdArray(new int[] {6, 9, 3}));
  }

  @Test
  @DisplayName("gcdArray([0, 0, 0]) → 0")
  void testGcdArrayAllZeros() {
    assertEquals(0, Nullspace.gcdArray(new int[] {0, 0, 0}));
  }

  @Test
  @DisplayName("gcdArray([5]) → 5")
  void testGcdArraySingle() {
    assertEquals(5, Nullspace.gcdArray(new int[] {5}));
  }

  // ── toRational ───────────────────────────────────────────

  @Test
  @DisplayName("toRational convierte correctamente los valores")
  void testToRational() {
    int[][] w = {{1, -2}, {3, 0}};
    Nullspace.Rational[][] r = Nullspace.toRational(w);
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
    Nullspace.Rational[][] r = Nullspace.toRational(w);
    for (Nullspace.Rational[] row : r)
      for (Nullspace.Rational cell : row) assertEquals(1L, cell.den);
  }

  // ── gaussianElimination ──────────────────────────────────

  @Test
  @DisplayName("gaussianElimination — identidad 2x2 → pivots [0,1]")
  void testGaussianEliminationIdentity() {
    Nullspace.Rational[][] a = Nullspace.toRational(new int[][] {{1, 0}, {0, 1}});
    int[] pivots = new int[2];
    Nullspace.gaussianElimination(a, pivots);
    assertEquals(0, pivots[0]);
    assertEquals(1, pivots[1]);
  }

  @Test
  @DisplayName("gaussianElimination — columna cero → pivots[0] = -1")
  void testGaussianEliminationFreeColumn() {
    Nullspace.Rational[][] a = Nullspace.toRational(new int[][] {{0, 1}, {0, 0}});
    int[] pivots = new int[2];
    Nullspace.gaussianElimination(a, pivots);
    assertEquals(-1, pivots[0]);
    assertEquals(0, pivots[1]);
  }

  // ── buildBasisVector ─────────────────────────────────────

  @Test
  @DisplayName("buildBasisVector para [1, -1] → v[0] == v[1]")
  void testBuildBasisVectorSimple() {
    int[][] w = {{1, -1}};
    Nullspace.Rational[][] a = Nullspace.toRational(w);
    int[] pivots = new int[2];
    Nullspace.gaussianElimination(a, pivots);
    List<Integer> freeVars = new java.util.ArrayList<>();
    for (int j = 0; j < 2; j++) if (pivots[j] == -1) freeVars.add(j);
    int[] vec = Nullspace.buildBasisVector(a, pivots, freeVars, freeVars.get(0), 2);
    assertEquals(vec[0], vec[1]);
    assertTrue(vec[0] != 0);
  }

  // ── Rational ─────────────────────────────────────────────

  @Test
  @DisplayName("Rational(3, 6) se reduce a 1/2")
  void testRationalReduction() {
    Nullspace.Rational r = new Nullspace.Rational(3, 6);
    assertEquals(1L, r.num);
    assertEquals(2L, r.den);
  }

  @Test
  @DisplayName("Rational denominador negativo — signo pasa al numerador")
  void testRationalNegativeDenominator() {
    Nullspace.Rational r = new Nullspace.Rational(3, -6);
    assertTrue(r.den > 0);
    assertEquals(-1L, r.num);
  }

  @Test
  @DisplayName("Rational denominador cero lanza ArithmeticException")
  void testRationalZeroDenominator() {
    assertThrows(ArithmeticException.class, () -> new Nullspace.Rational(1, 0));
  }

  @Test
  @DisplayName("Rational.isZero con num=0 → true")
  void testRationalIsZero() {
    assertTrue(new Nullspace.Rational(0).isZero());
  }

  @Test
  @DisplayName("Rational.add: 1/2 + 1/3 = 5/6")
  void testRationalAdd() {
    Nullspace.Rational a = new Nullspace.Rational(1, 2);
    Nullspace.Rational b = new Nullspace.Rational(1, 3);
    Nullspace.Rational result = a.add(b);
    assertEquals(5L, result.num);
    assertEquals(6L, result.den);
  }

  @Test
  @DisplayName("Rational.sub: 3/4 - 1/4 = 1/2")
  void testRationalSub() {
    Nullspace.Rational a = new Nullspace.Rational(3, 4);
    Nullspace.Rational b = new Nullspace.Rational(1, 4);
    Nullspace.Rational result = a.sub(b);
    assertEquals(1L, result.num);
    assertEquals(2L, result.den);
  }

  @Test
  @DisplayName("Rational.mul: 2/3 * 3/4 = 1/2")
  void testRationalMul() {
    Nullspace.Rational a = new Nullspace.Rational(2, 3);
    Nullspace.Rational b = new Nullspace.Rational(3, 4);
    Nullspace.Rational result = a.mul(b);
    assertEquals(1L, result.num);
    assertEquals(2L, result.den);
  }

  @Test
  @DisplayName("Rational.negate: -(1/2) = -1/2")
  void testRationalNegate() {
    Nullspace.Rational r = new Nullspace.Rational(1, 2).negate();
    assertEquals(-1L, r.num);
    assertEquals(2L, r.den);
  }

  // ── compute ──────────────────────────────────────────────

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
  }

  @Test
  @DisplayName("columna cero → vector canónico en nullspace")
  void testComputeZeroColumn() {
    int[][] w = {{1, 0}, {0, 0}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(1, basis.size());
    int[] v = basis.get(0);
    assertEquals(0, v[0]);
    assertTrue(v[1] != 0);
  }

  @Test
  @DisplayName("nullspace de [1, -1] → v[0] == v[1]")
  void testComputeSimpleKernel() {
    int[][] w = {{1, -1}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(1, basis.size());
    int[] v = basis.get(0);
    assertEquals(v[0], v[1]);
    assertTrue(v[0] != 0);
  }

  @Test
  @DisplayName("todo vector del nullspace satisface W*x = 0 — red exampleHuang")
  void testComputeWxEqualsZero() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[][] w = Matrix.subtract(post, pre);
    int[][] wt = Matrix.transposed(w);

    List<int[]> basis = Nullspace.compute(wt);
    assertTrue(basis.size() > 0, "La red debe tener invariantes de plaza");

    for (int[] x : basis) {
      for (int i = 0; i < wt.length; i++) {
        int dot = 0;
        for (int j = 0; j < x.length; j++) dot += wt[i][j] * x[j];
        assertEquals(0, dot, "wt[" + i + "] · x debe ser 0");
      }
    }
  }

  @Test
  @DisplayName("matriz completamente cero → nullspace base completa")
  void testComputeAllZeroMatrix() {
    int[][] w = {{0, 0}, {0, 0}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(2, basis.size());
  }

  @Test
  @DisplayName("matriz 1x1 cero → nullspace trivial no vacío")
  void testComputeSingleZero() {
    int[][] w = {{0}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(1, basis.size());
  }

  @Test
  @DisplayName("gaussianElimination genera fila nula correctamente")
  void testGaussianEliminationZeroRow() {
    int[][] input = {{1, 2}, {2, 4}};
    var a = Nullspace.toRational(input);
    int[] pivots = new int[2];

    Nullspace.gaussianElimination(a, pivots);

    assertTrue(a[1][0].isZero());
    assertTrue(a[1][1].isZero());
  }

  @Test
  @DisplayName("buildBasisVector cumple W·x = 0")
  void testBuildBasisVectorWxZero() {
    int[][] w = {{1, -1}};
    var a = Nullspace.toRational(w);
    int[] pivots = new int[2];
    Nullspace.gaussianElimination(a, pivots);

    List<Integer> freeVars = new java.util.ArrayList<>();
    for (int j = 0; j < 2; j++) if (pivots[j] == -1) freeVars.add(j);

    int[] x = Nullspace.buildBasisVector(a, pivots, freeVars, freeVars.get(0), 2);

    int dot = w[0][0] * x[0] + w[0][1] * x[1];
    assertEquals(0, dot);
  }

  @Test
  @DisplayName("Rational maneja números grandes correctamente")
  void testRationalLargeNumbers() {
    Nullspace.Rational r = new Nullspace.Rational(1_000_000, 2_000_000);
    assertEquals(1, r.num);
    assertEquals(2, r.den);
  }

  @Test
  @DisplayName("compute con matriz rectangular")
  void testComputeRectangularMatrix() {
    int[][] w = {
      {1, 2, 3},
      {2, 4, 6}
    };

    List<int[]> basis = Nullspace.compute(w);
    assertTrue(basis.size() >= 1);
  }
}
