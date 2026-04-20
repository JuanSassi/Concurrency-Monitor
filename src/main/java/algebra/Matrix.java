/**
 * Utility class for matrix operations. Provides static methods for performing basic matrix
 * arithmetic operations such as subtraction of integer matrices.
 *
 * @author Sassi Juan Ignacio
 */
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
}
