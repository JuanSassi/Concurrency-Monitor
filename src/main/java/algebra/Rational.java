/**
 * Inner class representing rational numbers with exact precision. Uses long integer arithmetic to
 * avoid rounding errors that would occur with floating-point arithmetic. All rationals are
 * automatically reduced to canonical form (lowest terms with positive denominator).
 *
 * <p>This class is essential for exact Gaussian elimination, as it prevents the accumulation of
 * floating-point errors that would compromise the accuracy of the nullspace computation.
 */
public final class Rational {
  /** The numerator of the rational number */
  long num;

  /** The denominator of the rational number (always positive after construction) */
  long den;

  /**
   * Constructs a rational number and reduces it to canonical form. The rational is reduced to
   * lowest terms using GCD, and the sign is normalized to be in the numerator.
   *
   * @param n the numerator
   * @param d the denominator (must not be zero)
   * @throws ArithmeticException if the denominator is zero
   */
  Rational(long n, long d) {
    if (d == 0) throw new ArithmeticException("Denominator zero");
    long g = gcd(Math.abs(n), Math.abs(d));
    n /= g;
    d /= g;
    if (d < 0) {
      n = -n;
      d = -d;
    }
    this.num = n;
    this.den = d;
  }

  /**
   * Constructs a rational number from an integer. Equivalent to creating the rational n/1.
   *
   * @param n the integer value
   */
  Rational(long n) {
    this(n, 1);
  }

  /**
   * Adds this rational to another rational.
   *
   * @param r the rational to add
   * @return a new rational representing the sum
   */
  public Rational add(Rational r) {
    return new Rational(this.num * r.den + r.num * this.den, this.den * r.den);
  }

  /**
   * Subtracts another rational from this rational.
   *
   * @param r the rational to subtract
   * @return a new rational representing the difference
   */
  public Rational sub(Rational r) {
    return new Rational(this.num * r.den - r.num * this.den, this.den * r.den);
  }

  /**
   * Multiplies this rational by another rational.
   *
   * @param r the rational to multiply by
   * @return a new rational representing the product
   */
  public Rational mul(Rational r) {
    return new Rational(this.num * r.num, this.den * r.den);
  }

  /**
   * Returns the negation of this rational.
   *
   * @return a new rational representing -this
   */
  public Rational negate() {
    return new Rational(-this.num, this.den);
  }

  /**
   * Checks if this rational is zero.
   *
   * @return true if this rational equals zero, false otherwise
   */
  public boolean isZero() {
    return num == 0;
  }

  /**
   * Computes the least common multiple of two numbers.
   *
   * @param a the first number
   * @param b the second number
   * @return the LCM of a and b
   */
  public static long lcm(long a, long b) {
    return a / gcd(a, b) * b;
  }

  /**
   * Computes the greatest common divisor of two long numbers using Euclid's algorithm.
   *
   * @param a the first number
   * @param b the second number
   * @return the GCD of a and b
   */
  public static long gcd(long a, long b) {
    return b == 0 ? a : gcd(b, a % b);
  }
}
