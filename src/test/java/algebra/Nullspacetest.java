import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Nullspace utility class.
 *
 * <p>Covers compute(), helper methods (lcm, gcd, gcdArray) and the inner Rational class. The key
 * mathematical property verified is W*x = 0 for every basis vector x returned by compute().
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
    // el vector debe ser [0, 1] o un múltiplo
    int[] v = basis.get(0);
    assertEquals(0, v[0]);
    assertTrue(v[1] != 0);
  }

  @Test
  @DisplayName("todo vector del nullspace satisface W*x = 0")
  void testComputeWxEqualsZero() {
    // W = Post - Pre de la red exampleHuang (matriz de incidencia 14x10)
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[][] w = Matrix.subtract(post, pre);
    int[][] wt = Matrix.transposed(w);

    List<int[]> basis = Nullspace.compute(wt);
    assertTrue(basis.size() > 0, "La red debe tener invariantes de plaza");

    for (int[] x : basis) {
      // Verificar wt * x = 0
      for (int i = 0; i < wt.length; i++) {
        int dot = 0;
        for (int j = 0; j < x.length; j++) {
          dot += wt[i][j] * x[j];
        }
        assertEquals(0, dot, "wt[" + i + "] · x debe ser 0");
      }
    }
  }

  @Test
  @DisplayName("nullspace de matrix 1x2 [1, -1] → [1, 1]")
  void testComputeSimpleKernel() {
    int[][] w = {{1, -1}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(1, basis.size());
    int[] v = basis.get(0);
    // v[0] y v[1] deben ser iguales y no cero
    assertEquals(v[0], v[1]);
    assertTrue(v[0] != 0);
  }
}
