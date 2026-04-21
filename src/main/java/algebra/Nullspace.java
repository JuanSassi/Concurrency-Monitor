import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for computing the nullspace (kernel) of integer matrices. The nullspace of a matrix
 * w consists of all vectors x such that w*x = 0. This class uses Gaussian elimination with exact
 * rational arithmetic to compute a minimal integer basis for the nullspace.
 *
 * <p>The algorithm guarantees exact results by using rational number arithmetic internally and
 * converting the final basis vectors to reduced integer form.
 *
 * @author Sassi Juan Ignacio
 */
class Nullspace {
  /** Private constructor to prevent instantiation of this utility class. */
  private Nullspace() {
    // Utility class, no instances allowed
  }

  /**
   * Inner class representing rational numbers with exact precision. Uses long integer arithmetic to
   * avoid rounding errors that would occur with floating-point arithmetic. All rationals are
   * automatically reduced to canonical form (lowest terms with positive denominator).
   *
   * <p>This class is essential for exact Gaussian elimination, as it prevents the accumulation of
   * floating-point errors that would compromise the accuracy of the nullspace computation.
   */
  static class Rational {
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
     * Computes the greatest common divisor using Euclid's algorithm.
     *
     * @param a the first number (non-negative)
     * @param b the second number (non-negative)
     * @return the GCD of a and b
     */
    private static long gcd(long a, long b) {
      while (b != 0) {
        long t = b;
        b = a % b;
        a = t;
      }
      return a;
    }
  }

  /**
   * Converts an integer matrix to a rational matrix.
   *
   * @param w the integer matrix (m x n)
   * @return a new Rational matrix with the same values
   */
  static Rational[][] toRational(int[][] w) {
    int m = w.length, n = w[0].length;
    Rational[][] a = new Rational[m][n];
    for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) a[i][j] = new Rational(w[i][j]);
    return a;
  }

  /**
   * Reduces a rational matrix to reduced row echelon form (RREF) in-place using Gaussian
   * elimination, and records the pivot column for each row.
   *
   * @param a the rational matrix to reduce (modified in-place)
   * @param pivots array of length n; pivots[col] is set to the row index of the pivot in that
   *     column, or -1 if the column is free
   */
  static void gaussianElimination(Rational[][] a, int[] pivots) {
    int m = a.length, n = a[0].length;
    Arrays.fill(pivots, -1);

    int row = 0;
    for (int col = 0; col < n && row < m; col++) {
      int sel = findPivot(a, row, col, m);
      if (sel == -1) continue;

      swapRows(a, row, sel);
      normalizeRow(a, row, col, n);
      eliminateColumn(a, row, col, m, n);

      pivots[col] = row;
      row++;
    }
  }

  private static int findPivot(Rational[][] a, int row, int col, int m) {
    for (int i = row; i < m; i++) {
      if (!a[i][col].isZero()) {
        return i;
      }
    }
    return -1;
  }

  private static void swapRows(Rational[][] a, int r1, int r2) {
    Rational[] tmp = a[r1];
    a[r1] = a[r2];
    a[r2] = tmp;
  }

  private static void normalizeRow(Rational[][] a, int row, int col, int n) {
    Rational inv = new Rational(a[row][col].den, a[row][col].num);
    for (int j = col; j < n; j++) {
      a[row][j] = a[row][j].mul(inv);
    }
  }

  private static void eliminateColumn(Rational[][] a, int row, int col, int m, int n) {
    for (int i = 0; i < m; i++) {
      if (i != row && !a[i][col].isZero()) {
        Rational factor = a[i][col];
        for (int j = col; j < n; j++) {
          a[i][j] = a[i][j].sub(factor.mul(a[row][j]));
        }
      }
    }
  }

  /**
   * Builds one integer basis vector of the nullspace for a given free variable.
   *
   * @param a the matrix already in RREF
   * @param pivots pivot map from gaussianElimination
   * @param freeVars list of free variable column indices
   * @param free the free variable column for which to build the vector
   * @param n number of columns
   * @return an integer array representing the basis vector
   */
  static int[] buildBasisVector(
      Rational[][] a, int[] pivots, List<Integer> freeVars, int free, int n) {
    Rational[] vec = new Rational[n];
    for (int j = 0; j < n; j++) vec[j] = new Rational(0);
    vec[free] = new Rational(1);

    for (int j = 0; j < n; j++) {
      if (pivots[j] != -1) {
        Rational sum = new Rational(0);
        for (int f : freeVars) sum = sum.add(a[pivots[j]][f].mul(vec[f]));
        vec[j] = sum.negate();
      }
    }

    long lcmVal = 1;
    for (Rational r : vec) lcmVal = lcm(lcmVal, r.den);

    int[] intVec = new int[n];
    for (int j = 0; j < n; j++) intVec[j] = (int) (vec[j].num * (lcmVal / vec[j].den));

    int g = gcdArray(intVec);
    if (g != 0) for (int j = 0; j < n; j++) intVec[j] /= g;

    return intVec;
  }

  /**
   * Computes the nullspace (kernel) of matrix w using Gaussian elimination. The nullspace contains
   * all vectors x such that w*x = 0.
   *
   * @param w the input matrix (m x n)
   * @return a list of basis vectors for the nullspace, each as an integer array of length n.
   *     Returns an empty list if the nullspace is trivial (only the zero vector).
   */
  public static List<int[]> compute(int[][] w) {
    int n = w[0].length;
    Rational[][] a = toRational(w);
    int[] pivots = new int[n];
    gaussianElimination(a, pivots);

    List<Integer> freeVars = new ArrayList<>();
    for (int j = 0; j < n; j++) if (pivots[j] == -1) freeVars.add(j);

    List<int[]> basis = new ArrayList<>();
    for (int free : freeVars) basis.add(buildBasisVector(a, pivots, freeVars, free, n));
    return basis;
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

  /**
   * Computes the GCD of all elements in an array.
   *
   * @param arr the array of integers
   * @return the GCD of all elements in the array
   */
  public static int gcdArray(int[] arr) {
    int g = 0;
    for (int x : arr) g = gcd(g, Math.abs(x));
    return g;
  }

  /**
   * Computes the greatest common divisor of two integers using Euclid's algorithm.
   *
   * @param a the first integer
   * @param b the second integer
   * @return the GCD of a and b
   */
  public static int gcd(int a, int b) {
    return b == 0 ? a : gcd(b, a % b);
  }
}
