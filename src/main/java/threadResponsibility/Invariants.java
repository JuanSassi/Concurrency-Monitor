import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Computation of P-invariants and T-invariants for Petri nets.
 *
 * <p>This class calculates two fundamental structural properties of Petri nets:
 *
 * <ul>
 *   <li><b>T-invariants</b>: Non-negative vectors y such that W·y = 0. They represent firing
 *       sequences that return the net to its initial marking (cyclic behavior).
 *   <li><b>P-invariants</b>: Non-negative vectors x such that x^T·W = 0 (or equivalently W^T·x =
 *       0). They represent conservative components such as resources, idle places, or capacity
 *       restrictions where the weighted sum of tokens remains constant.
 * </ul>
 *
 * <p>Where W is the incidence matrix of the net (W = Post - Pre).
 *
 * <p>Rendering the computed invariants is the caller's responsibility; this class only exposes them
 * through {@link #getPInvariants()} and {@link #getTInvariants()}.
 *
 * @author Juan Ignacio Sassi
 */
public class Invariants {

  private static final int MAX_COMBINATION_COEFFICIENT = 4;

  /** List of minimal P-invariants */
  private final List<List<Integer>> pInvariant;

  /** List of minimal T-invariants */
  private final List<List<Integer>> tInvariant;

  /**
   * Constructs an invariant calculator and computes all minimal invariants.
   *
   * <p>The constructor automatically computes:
   *
   * <ul>
   *   <li>P-invariants by finding the nullspace of W^T
   *   <li>T-invariants by finding the nullspace of W
   * </ul>
   *
   * @param w incidence matrix (Post - Pre) of the Petri net
   */
  public Invariants(int[][] w) {
    int[][] wt = Matrix.transposed(w);
    this.pInvariant = computeInvariants(wt);
    this.tInvariant = computeInvariants(w);
  }

  /**
   * Computes all minimal invariants of the input matrix.
   *
   * <p>The algorithm consists of the following steps:
   *
   * <ol>
   *   <li>Compute the nullspace basis of the matrix using exact rational arithmetic
   *   <li>Generate all non-negative linear combinations of basis vectors with bounded coefficients
   *   <li>Filter to keep only minimal invariants (those whose support is not a proper superset of
   *       another invariant's support)
   *   <li>Sort results by component sum (simplest invariants first)
   * </ol>
   *
   * @param matrix matrix for which invariants are sought (W for T-invariants, W^T for P-invariants)
   * @return list of minimal invariant vectors as lists of integers
   */
  public List<List<Integer>> computeInvariants(int[][] matrix) {
    List<int[]> nullBasis = Nullspace.compute(matrix);
    Set<List<Integer>> all = new HashSet<>();

    // Generate all non-negative linear combinations
    generateCombinations(nullBasis, new int[nullBasis.size()], 0, MAX_COMBINATION_COEFFICIENT, all);
    if (all.isEmpty()) {
      System.out.println("\nWARNING: No invariants found.");
    }

    // Filter only minimal invariants
    List<List<Integer>> minimal = new ArrayList<>();
    for (List<Integer> inv : all) if (isMinimal(inv, all)) minimal.add(inv);

    // Sort by sum of components (simplest invariants first)
    minimal.sort(Comparator.comparingInt(v -> v.stream().mapToInt(Integer::intValue).sum()));
    return minimal;
  }

  /**
   * Recursively generates all non-negative linear combinations of basis vectors.
   *
   * <p>For each basis vector, tests coefficients from -maxCoeff to +maxCoeff. Only combinations
   * resulting in completely non-negative vectors with at least one positive component are kept. The
   * trivial zero vector is ignored.
   *
   * @param basis nullspace basis vectors
   * @param coeffs array of coefficients (filled recursively)
   * @param idx current index in the recursion
   * @param maxCoeff absolute maximum value for each coefficient
   * @param set set where valid combinations are stored (duplicates auto-removed)
   */
  private void generateCombinations(
      List<int[]> basis, int[] coeffs, int idx, int maxCoeff, Set<List<Integer>> set) {
    if (idx == basis.size()) {
      processLeaf(basis, coeffs, set);
      return;
    }
    for (int c = -maxCoeff; c <= maxCoeff; c++) {
      coeffs[idx] = c;
      generateCombinations(basis, coeffs, idx + 1, maxCoeff, set);
    }
  }

  /**
   * Processes a leaf node in the combination tree. Computes the linear combination and adds it to
   * the set if valid.
   *
   * @param basis nullspace basis vectors
   * @param coeffs array of coefficients for the current combination
   * @param set set where valid combinations are stored
   */
  private void processLeaf(List<int[]> basis, int[] coeffs, Set<List<Integer>> set) {
    int[] combo = computeCombo(basis, coeffs);
    if (combo == null) return;
    addIfValid(combo, set);
  }

  /**
   * Computes the linear combination of basis vectors with given coefficients.
   *
   * @param basis nullspace basis vectors
   * @param coeffs array of coefficients
   * @return the resulting combination array, or null if all coefficients are zero
   */
  private int[] computeCombo(List<int[]> basis, int[] coeffs) {
    int n = basis.get(0).length;
    int[] combo = new int[n];
    boolean allZero = true;
    for (int i = 0; i < basis.size(); i++) {
      if (coeffs[i] != 0) allZero = false;
      for (int j = 0; j < n; j++) combo[j] += coeffs[i] * basis.get(i)[j];
    }
    return allZero ? null : combo;
  }

  /**
   * Adds a combination vector to the set if it is non-negative and non-zero. Reduces the vector by
   * its GCD before adding.
   *
   * @param combo the combination vector to validate and add
   * @param set set where valid combinations are stored
   */
  private void addIfValid(int[] combo, Set<List<Integer>> set) {
    for (int v : combo) if (v < 0) return;
    boolean anyPos = false;
    for (int v : combo)
      if (v > 0) {
        anyPos = true;
        break;
      }
    if (!anyPos) return;
    int g = Matrix.gcdArray(combo);
    if (g > 0) for (int j = 0; j < combo.length; j++) combo[j] /= g;
    List<Integer> lst = new ArrayList<>();
    for (int v : combo) lst.add(v);
    set.add(lst);
  }

  /**
   * Checks whether an invariant is minimal.
   *
   * <p>An invariant is minimal if there is no other invariant whose support (set of indices with
   * positive values) is a proper subset of its own support.
   *
   * @param inv invariant vector to check
   * @param all set of all invariants found
   * @return true if the invariant is minimal, false otherwise
   */
  private boolean isMinimal(List<Integer> inv, Set<List<Integer>> all) {
    // Compute the support of the current invariant
    Set<Integer> supp = new HashSet<>();
    for (int i = 0; i < inv.size(); i++) if (inv.get(i) > 0) supp.add(i);

    // Check against all other invariants
    for (List<Integer> other : all) {
      if (other.equals(inv)) continue;

      Set<Integer> supp2 = new HashSet<>();
      for (int i = 0; i < other.size(); i++) if (other.get(i) > 0) supp2.add(i);

      // If another invariant has strictly contained support then it is not minimal
      if (supp.containsAll(supp2) && !supp.equals(supp2)) return false;
    }
    return true;
  }

  /**
   * Gets the list of computed P-invariants.
   *
   * @return list of minimal P-invariant vectors
   */
  public List<List<Integer>> getPInvariants() {
    return Collections.unmodifiableList(pInvariant);
  }

  /**
   * Gets the list of computed T-invariants.
   *
   * @return list of minimal T-invariant vectors
   */
  public List<List<Integer>> getTInvariants() {
    return Collections.unmodifiableList(tInvariant);
  }
}