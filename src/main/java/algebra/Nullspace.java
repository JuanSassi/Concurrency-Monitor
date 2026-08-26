import java.util.ArrayList;
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
   * Builds one integer basis vector of the nullspace for a given free variable. This method has
   * been refactored to reduce cyclomatic complexity.
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
    Rational[] vec = initializeRationalVector(n, free);

    // Calculate values for pivot positions
    for (int j = 0; j < n; j++) {
      if (pivots[j] != -1) {
        vec[j] = Matrix.calculatePivotValue(a, pivots[j], freeVars, vec);
      }
    }

    return convertToReducedIntegerArray(vec);
  }

  /**
   * Helper to initialize a rational vector with a 1 in the free variable position.
   *
   * @param n the length of the vector
   * @param free the index of the free variable column
   * @return a Rational array initialized with zeros and a one at the free index
   */
  private static Rational[] initializeRationalVector(int n, int free) {
    Rational[] vec = new Rational[n];
    for (int j = 0; j < n; j++) {
      vec[j] = new Rational(0);
    }
    vec[free] = new Rational(1);
    return vec;
  }

  /**
   * Converts a vector of {@link Rational} numbers to its simplest equivalent integer form. *
   *
   * <p>The conversion process follows these steps:
   *
   * <ol>
   *   <li>Finds the Least Common Multiple (LCM) of all denominators to eliminate fractions.
   *   <li>Scales each rational number by the LCM to obtain an initial integer vector.
   *   <li>Reduces the resulting integer vector by dividing all elements by their Greatest Common
   *       Divisor (GCD) to ensure the simplest representation.
   * </ol>
   *
   * @param vec the array of {@link Rational} numbers to convert
   * @return a new integer array representing the simplified basis vector
   */
  private static int[] convertToReducedIntegerArray(Rational[] vec) {
    int n = vec.length;
    long lcmVal = 1;
    for (Rational r : vec) {
      lcmVal = Rational.lcm(lcmVal, r.den);
    }

    int[] intVec = new int[n];
    for (int j = 0; j < n; j++) {
      long factor = lcmVal / vec[j].den;
      long resultLong = vec[j].num * factor;
      intVec[j] = (int) resultLong;
    }

    int g = Matrix.gcdArray(intVec);
    if (g != 0) {
      for (int j = 0; j < n; j++) {
        intVec[j] /= g;
      }
    }
    // Si SpotBugs se queja de devolver el array directamente,
    // podrías devolver intVec.clone(), pero en utilidades suele aceptarse.
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
    Rational[][] a = Matrix.toRational(w);
    int[] pivots = new int[n];
    Matrix.gaussianElimination(a, pivots);

    List<Integer> freeVars = new ArrayList<>();
    for (int j = 0; j < n; j++) if (pivots[j] == -1) freeVars.add(j);

    List<int[]> basis = new ArrayList<>();
    for (int free : freeVars) basis.add(buildBasisVector(a, pivots, freeVars, free, n));
    return basis;
  }
}
