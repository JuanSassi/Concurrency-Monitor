/**
 * Utility class for matrix operations. Provides static methods for performing basic matrix
 * arithmetic operations such as subtraction of integer matrices.
 *
 * @author Sassi Juan Ignacio
 */
import java.util.Arrays;
import java.util.List;

class Matrix {
  /** Private constructor to prevent instantiation of this utility class. */
  private Matrix() {
    // Utility class, no instances allowed
  }

  /**
   * Subtracts two matrices element by element. Both matrices must have the same dimensions. The
   * operation is performed as c[i][j] = a[i][j] - b[i][j] for all valid indices i and j.
   *
   * <p>Note: This method assumes both matrices have at least one row and one column. The behavior
   * is undefined if either matrix is empty.
   *
   * @param a the first matrix (minuend)
   * @param b the second matrix (subtrahend), must have the same dimensions as a
   * @return a new matrix c = a - b with the same dimensions as the input matrices
   * @throws ArrayIndexOutOfBoundsException if matrices have different dimensions
   */
  public static int[][] subtract(int[][] a, int[][] b) {
    int rows = a.length, cols = a[0].length;
    int[][] c = new int[rows][cols];
    for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) c[i][j] = a[i][j] - b[i][j];
    return c;
  }

  /**
   * Computes the transpose of a matrix. The transpose operation swaps rows and columns, such that
   * the element at position [i][j] in the original matrix appears at position [j][i] in the
   * transposed matrix.
   *
   * <p>If the input matrix has dimensions m×n, the output matrix will have dimensions n×m.
   *
   * <p>Note: This method assumes the matrix has at least one row and one column. The behavior is
   * undefined if the matrix is empty.
   *
   * @param w the matrix to transpose (m × n)
   * @return a new matrix that is the transpose of w (n × m), where result[j][i] = w[i][j] for all
   *     valid indices
   */
  public static int[][] transposed(int[][] w) {
    int rows = w.length;
    int cols = w[0].length;
    int[][] wtransposed = new int[cols][rows];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        wtransposed[j][i] = w[i][j];
      }
    }
    return wtransposed;
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

  /**
   * Scans a column to find a non-zero pivot element starting from a specific row.
   *
   * @param a the rational matrix
   * @param row the starting row index
   * @param col the column index to scan
   * @param m total number of rows
   * @return the row index of the pivot, or -1 if no pivot is found
   */
  private static int findPivot(Rational[][] a, int row, int col, int m) {
    for (int i = row; i < m; i++) {
      if (!a[i][col].isZero()) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Swaps two rows in a rational matrix.
   *
   * @param a the matrix where rows will be swapped
   * @param r1 the index of the first row
   * @param r2 the index of the second row
   */
  private static void swapRows(Rational[][] a, int r1, int r2) {
    Rational[] tmp = a[r1];
    a[r1] = a[r2];
    a[r2] = tmp;
  }

  /**
   * Normalizes a row so that the pivot element becomes one.
   *
   * @param a the rational matrix
   * @param row the index of the row to normalize
   * @param col the column index of the pivot
   * @param n total number of columns
   */
  private static void normalizeRow(Rational[][] a, int row, int col, int n) {
    Rational inv = new Rational(a[row][col].den, a[row][col].num);
    for (int j = col; j < n; j++) {
      a[row][j] = a[row][j].mul(inv);
    }
  }

  /**
   * Eliminates all other entries in a column using the pivot row (standard RREF step).
   *
   * @param a the rational matrix
   * @param row the current pivot row index
   * @param col the current pivot column index
   * @param m total number of rows
   * @param n total number of columns
   */
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
   * * Helper to calculate the value of a pivot variable based on free variables.
   *
   * @param a the RREF matrix
   * @param pivotRow the row index where the pivot is located
   * @param freeVars the list of indices for free variables
   * @param vec the current basis vector being constructed
   * @return the calculated rational value for the pivot variable
   */
  public static Rational calculatePivotValue(
      Rational[][] a, int pivotRow, List<Integer> freeVars, Rational[] vec) {
    Rational sum = new Rational(0);
    for (int f : freeVars) {
      sum = sum.add(a[pivotRow][f].mul(vec[f]));
    }
    return sum.negate();
  }

  /**
   * Computes the GCD of all elements in an array.
   *
   * @param arr the array of integers
   * @return the GCD of all elements in the array
   */
  public static int gcdArray(int[] arr) {
    int g = 0;
    for (int x : arr) g = gcdInt(g, x);
    return g;
  }

  /**
   * Computes the greatest common divisor of two integers using Euclid's algorithm.
   *
   * @param a the first integer
   * @param b the second integer
   * @return the GCD of a and b
   */
  public static int gcdInt(int a, int b) {
    return b == 0 ? Math.abs(a) : gcdInt(b, a % b);
  }
}
