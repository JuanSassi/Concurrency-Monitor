import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Nullspace utility class.
 *
 * <p>Covers compute(), Matrix.toRational(), Matrix.gaussianElimination(), and buildBasisVector().
 * The key mathematical property verified is W*x = 0 for every basis vector x returned by compute().
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Nullspace")
class NullspaceTest {

  // ── Matrix.toRational ────────────────────────────────────

  @Test
  @DisplayName("toRational convierte correctamente los valores")
  void testToRational() {
    int[][] w = {{1, -2}, {3, 0}};
    Rational[][] r = Matrix.toRational(w);
    assertEquals(2, r.length);
    assertEquals(2, r[0].length);
    assertEquals(1L, r[0][0].num);
    assertEquals(-2L, r[0][1].num);
    assertEquals(3L, r[1][0].num);
    assertEquals(0L, r[1][1].num);
  }

  @Test
  @DisplayName("toRational — todos los denominadores son 1")
  void testToRationalDenominators() {
    int[][] w = {{5, 7}, {2, 4}};
    Rational[][] r = Matrix.toRational(w);
    for (Rational[] row : r) for (Rational cell : row) assertEquals(1L, cell.den);
  }

  // ── Matrix.gaussianElimination ───────────────────────────

  @Test
  @DisplayName("gaussianElimination — identidad 2x2 → pivots [0,1]")
  void testGaussianEliminationIdentity() {
    Rational[][] a = Matrix.toRational(new int[][] {{1, 0}, {0, 1}});
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    assertEquals(0, pivots[0]);
    assertEquals(1, pivots[1]);
  }

  @Test
  @DisplayName("gaussianElimination — columna cero → pivots[0] = -1")
  void testGaussianEliminationFreeColumn() {
    Rational[][] a = Matrix.toRational(new int[][] {{0, 1}, {0, 0}});
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    assertEquals(-1, pivots[0]);
    assertEquals(0, pivots[1]);
  }

  // ── Nullspace.buildBasisVector ───────────────────────────

  @Test
  @DisplayName("buildBasisVector para [1, -1] → v[0] == v[1]")
  void testBuildBasisVectorSimple() {
    int[][] w = {{1, -1}};
    Rational[][] a = Matrix.toRational(w);
    int[] pivots = new int[2];
    Matrix.gaussianElimination(a, pivots);
    List<Integer> freeVars = new java.util.ArrayList<>();
    for (int j = 0; j < 2; j++) if (pivots[j] == -1) freeVars.add(j);
    int[] vec = Nullspace.buildBasisVector(a, pivots, freeVars, freeVars.get(0), 2);
    assertEquals(vec[0], vec[1]);
    assertTrue(vec[0] != 0);
  }

  // ── Nullspace.compute ────────────────────────────────────

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
    int[] v = basis.get(0);
    assertEquals(0, v[0]);
    assertTrue(v[1] != 0);
  }

  @Test
  @DisplayName("nullspace de [1, -1] → v[0] == v[1]")
  void testComputeSimpleKernel() {
    int[][] w = {{1, -1}};
    List<int[]> basis = Nullspace.compute(w);
    assertEquals(1, basis.size());
    int[] v = basis.get(0);
    assertEquals(v[0], v[1]);
    assertTrue(v[0] != 0);
  }

  @Test
  @DisplayName("todo vector del nullspace satisface W*x = 0 — red exampleHuang")
  void testComputeWxEqualsZero() {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[][] w = Matrix.subtract(post, pre);
    int[][] wt = Matrix.transposed(w);

    List<int[]> basis = Nullspace.compute(wt);
    assertTrue(basis.size() > 0, "La red debe tener invariantes de plaza");

    for (int[] x : basis) {
      for (int i = 0; i < wt.length; i++) {
        int dot = 0;
        for (int j = 0; j < x.length; j++) dot += wt[i][j] * x[j];
        assertEquals(0, dot, "wt[" + i + "] · x debe ser 0");
      }
    }
  }
}
