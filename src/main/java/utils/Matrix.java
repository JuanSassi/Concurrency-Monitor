/**
 * Utility class for matrix operations.
 * Provides static methods for performing basic matrix arithmetic operations
 * such as subtraction of integer matrices.
 * 
 * @author Sassi Juan Ignacio
 */
class Matrix {
    /**
     * Subtracts two matrices element by element.
     * Both matrices must have the same dimensions. The operation is performed as C[i][j] = A[i][j] - B[i][j]
     * for all valid indices i and j.
     * 
     * @param A the first matrix (minuend)
     * @param B the second matrix (subtrahend), must have the same dimensions as A
     * @return a new matrix C = A - B
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
}