import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Matrix utility class.
 *
 * <p>Covers subtract and transposed operations with square, rectangular, and edge-case matrices.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Matrix")
class MatrixTest {

  // ── subtract ─────────────────────────────────────────────

  @Test
  @DisplayName("subtract 2x2 simples")
  void testSubtract2x2() {
    int[][] a = {{5, 3}, {2, 8}};
    int[][] b = {{1, 1}, {1, 1}};
    int[][] expected = {{4, 2}, {1, 7}};
    assertArrayEquals(expected, Matrix.subtract(a, b));
  }

  @Test
  @DisplayName("subtract produce negativos")
  void testSubtractNegativeResult() {
    int[][] a = {{1, 0}, {0, 1}};
    int[][] b = {{3, 2}, {2, 3}};
    int[][] expected = {{-2, -2}, {-2, -2}};
    assertArrayEquals(expected, Matrix.subtract(a, b));
  }

  @Test
  @DisplayName("subtract 1x1")
  void testSubtract1x1() {
    int[][] a = {{7}};
    int[][] b = {{3}};
    int[][] expected = {{4}};
    assertArrayEquals(expected, Matrix.subtract(a, b));
  }

  @Test
  @DisplayName("A - A = matriz de ceros")
  void testSubtractSameMatrix() {
    int[][] a = {{4, 7}, {2, 9}};
    int[][] expected = {{0, 0}, {0, 0}};
    assertArrayEquals(expected, Matrix.subtract(a, a));
  }

  @Test
  @DisplayName("A - zeros = A")
  void testSubtractZeroMatrix() {
    int[][] a = {{3, 5}, {1, 4}};
    int[][] zeros = {{0, 0}, {0, 0}};
    assertArrayEquals(a, Matrix.subtract(a, zeros));
  }

  @Test
  @DisplayName("subtract matriz rectangular 3x2")
  void testSubtractRectangular() {
    int[][] a = {{1, 2}, {3, 4}, {5, 6}};
    int[][] b = {{1, 1}, {1, 1}, {1, 1}};
    int[][] expected = {{0, 1}, {2, 3}, {4, 5}};
    assertArrayEquals(expected, Matrix.subtract(a, b));
  }

  // ── transposed ───────────────────────────────────────────

  @Test
  @DisplayName("transposed matriz cuadrada 3x3")
  void testTransposedSquare() {
    int[][] w = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
    int[][] expected = {{1, 4, 7}, {2, 5, 8}, {3, 6, 9}};
    assertArrayEquals(expected, Matrix.transposed(w));
  }

  @Test
  @DisplayName("transposed 2x3 → 3x2")
  void testTransposedRectangular() {
    int[][] w = {{1, 2, 3}, {4, 5, 6}};
    int[][] expected = {{1, 4}, {2, 5}, {3, 6}};
    int[][] result = Matrix.transposed(w);
    assertEquals(3, result.length);
    assertEquals(2, result[0].length);
    assertArrayEquals(expected, result);
  }

  @Test
  @DisplayName("transpuesta de transpuesta = original")
  void testTransposedTwiceIsOriginal() {
    int[][] w = {{1, 2, 3}, {4, 5, 6}};
    assertArrayEquals(w, Matrix.transposed(Matrix.transposed(w)));
  }
}
