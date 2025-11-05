/**
 * Utility class for matrix operations.
 * Provides static methods for performing basic matrix arithmetic operations
 * such as subtraction of integer matrices.
 * 
 * @author Sassi Juan Ignacio
 */
class Matrix {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Matrix() {
        // Utility class, no instances allowed
    }

    /**
     * Subtracts two matrices element by element.
     * Both matrices must have the same dimensions. The operation is performed as 
     * C[i][j] = A[i][j] - B[i][j] for all valid indices i and j.
     * 
     * <p>Note: This method assumes both matrices have at least one row and one column.
     * The behavior is undefined if either matrix is empty.</p>
     * 
     * @param A the first matrix (minuend)
     * @param B the second matrix (subtrahend), must have the same dimensions as A
     * @return a new matrix C = A - B with the same dimensions as the input matrices
     * @throws ArrayIndexOutOfBoundsException if matrices have different dimensions
     */
    public static int[][] subtract(int[][] A, int[][] B) {
        int rows = A.length, cols = A[0].length;
        int[][] C = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    /**
     * Computes the transpose of a matrix.
     * The transpose operation swaps rows and columns, such that the element
     * at position [i][j] in the original matrix appears at position [j][i]
     * in the transposed matrix.
     * 
     * <p>If the input matrix has dimensions m×n, the output matrix will have
     * dimensions n×m.</p>
     * 
     * <p>Note: This method assumes the matrix has at least one row and one column.
     * The behavior is undefined if the matrix is empty.</p>
     * 
     * @param W the matrix to transpose (m × n)
     * @return a new matrix that is the transpose of W (n × m), where
     *         result[j][i] = W[i][j] for all valid indices
     */
    public static int[][] transposed(int[][] W) {
        int rows = W.length;
        int cols = W[0].length;
        int[][] Wtransposed = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Wtransposed[j][i] = W[i][j];
            }
        }
        return Wtransposed;
    }
}