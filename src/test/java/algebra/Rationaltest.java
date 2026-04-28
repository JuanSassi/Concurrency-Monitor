import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Rational class.
 *
 * <p>Covers construction, reduction to canonical form, arithmetic operations (add, sub, mul,
 * negate), utility methods (isZero, gcd, lcm), and exception handling.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Rational")
class RationalTest {

  // ── construcción y reducción ──────────────────────────────

  @Test
  @DisplayName("Rational(3, 6) se reduce a 1/2")
  void testReduction() {
    Rational r = new Rational(3, 6);
    assertEquals(1L, r.num);
    assertEquals(2L, r.den);
  }

  @Test
  @DisplayName("Rational(4, 2) se reduce a 2/1")
  void testReductionWhole() {
    Rational r = new Rational(4, 2);
    assertEquals(2L, r.num);
    assertEquals(1L, r.den);
  }

  @Test
  @DisplayName("Rational denominador negativo — signo pasa al numerador")
  void testNegativeDenominator() {
    Rational r = new Rational(3, -6);
    assertTrue(r.den > 0);
    assertEquals(-1L, r.num);
  }

  @Test
  @DisplayName("Rational(-3, -6) se reduce a 1/2 (ambos negativos → positivo)")
  void testBothNegative() {
    Rational r = new Rational(-3, -6);
    assertEquals(1L, r.num);
    assertEquals(2L, r.den);
  }

  @Test
  @DisplayName("Rational(0, 5) → num=0, den=1")
  void testZeroNumerator() {
    Rational r = new Rational(0, 5);
    assertEquals(0L, r.num);
    assertEquals(1L, r.den);
  }

  @Test
  @DisplayName("Rational(n) equivale a Rational(n, 1)")
  void testIntegerConstructor() {
    Rational r = new Rational(7);
    assertEquals(7L, r.num);
    assertEquals(1L, r.den);
  }

  @Test
  @DisplayName("Rational denominador cero lanza ArithmeticException")
  void testZeroDenominatorThrows() {
    assertThrows(ArithmeticException.class, () -> new Rational(1, 0));
  }

  // ── isZero ────────────────────────────────────────────────

  @Test
  @DisplayName("isZero con num=0 → true")
  void testIsZeroTrue() {
    assertTrue(new Rational(0).isZero());
  }

  @Test
  @DisplayName("isZero con num≠0 → false")
  void testIsZeroFalse() {
    assertFalse(new Rational(1).isZero());
  }

  // ── add ───────────────────────────────────────────────────

  @Test
  @DisplayName("add: 1/2 + 1/3 = 5/6")
  void testAdd() {
    Rational result = new Rational(1, 2).add(new Rational(1, 3));
    assertEquals(5L, result.num);
    assertEquals(6L, result.den);
  }

  @Test
  @DisplayName("add: enteros 2 + 3 = 5")
  void testAddIntegers() {
    Rational result = new Rational(2).add(new Rational(3));
    assertEquals(5L, result.num);
    assertEquals(1L, result.den);
  }

  @Test
  @DisplayName("add: x + 0 = x")
  void testAddZero() {
    Rational a = new Rational(3, 7);
    Rational result = a.add(new Rational(0));
    assertEquals(a.num, result.num);
    assertEquals(a.den, result.den);
  }

  @Test
  @DisplayName("add: x + (-x) = 0")
  void testAddNegative() {
    Rational a = new Rational(3, 7);
    assertTrue(a.add(a.negate()).isZero());
  }

  // ── sub ───────────────────────────────────────────────────

  @Test
  @DisplayName("sub: 3/4 - 1/4 = 1/2")
  void testSub() {
    Rational result = new Rational(3, 4).sub(new Rational(1, 4));
    assertEquals(1L, result.num);
    assertEquals(2L, result.den);
  }

  @Test
  @DisplayName("sub: x - x = 0")
  void testSubSelf() {
    assertTrue(new Rational(5, 7).sub(new Rational(5, 7)).isZero());
  }

  @Test
  @DisplayName("sub: x - 0 = x")
  void testSubZero() {
    Rational a = new Rational(5, 9);
    Rational result = a.sub(new Rational(0));
    assertEquals(a.num, result.num);
    assertEquals(a.den, result.den);
  }

  // ── mul ───────────────────────────────────────────────────

  @Test
  @DisplayName("mul: 2/3 * 3/4 = 1/2")
  void testMul() {
    Rational result = new Rational(2, 3).mul(new Rational(3, 4));
    assertEquals(1L, result.num);
    assertEquals(2L, result.den);
  }

  @Test
  @DisplayName("mul: x * 0 = 0")
  void testMulByZero() {
    assertTrue(new Rational(5, 7).mul(new Rational(0)).isZero());
  }

  @Test
  @DisplayName("mul: x * 1 = x")
  void testMulByOne() {
    Rational a = new Rational(5, 7);
    Rational result = a.mul(new Rational(1));
    assertEquals(a.num, result.num);
    assertEquals(a.den, result.den);
  }

  // ── negate ────────────────────────────────────────────────

  @Test
  @DisplayName("negate: -(1/2) = -1/2")
  void testNegate() {
    Rational r = new Rational(1, 2).negate();
    assertEquals(-1L, r.num);
    assertEquals(2L, r.den);
  }

  @Test
  @DisplayName("negate: -(-3/5) = 3/5")
  void testNegateNegative() {
    Rational r = new Rational(-3, 5).negate();
    assertEquals(3L, r.num);
    assertEquals(5L, r.den);
  }

  @Test
  @DisplayName("negate: -(0) = 0")
  void testNegateZero() {
    assertTrue(new Rational(0).negate().isZero());
  }

  // ── gcd ───────────────────────────────────────────────────

  @Test
  @DisplayName("gcd(12, 8) → 4")
  void testGcd() {
    assertEquals(4L, Rational.gcd(12L, 8L));
  }

  @Test
  @DisplayName("gcd(0, 7) → 7")
  void testGcdZero() {
    assertEquals(7L, Rational.gcd(0L, 7L));
  }

  @Test
  @DisplayName("gcd(7, 7) → 7")
  void testGcdSame() {
    assertEquals(7L, Rational.gcd(7L, 7L));
  }

  @Test
  @DisplayName("gcd(1, n) → 1 (coprimos)")
  void testGcdCoprime() {
    assertEquals(1L, Rational.gcd(1L, 13L));
  }

  // ── lcm ───────────────────────────────────────────────────

  @Test
  @DisplayName("lcm(4, 6) → 12")
  void testLcm() {
    assertEquals(12L, Rational.lcm(4L, 6L));
  }

  @Test
  @DisplayName("lcm(5, 5) → 5")
  void testLcmSame() {
    assertEquals(5L, Rational.lcm(5L, 5L));
  }

  @Test
  @DisplayName("lcm(1, n) → n")
  void testLcmOne() {
    assertEquals(13L, Rational.lcm(1L, 13L));
  }
}
