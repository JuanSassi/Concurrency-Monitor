import java.util.*;

/**
 * Computation of P-invariants and T-invariants for Petri nets.
 * 
 * <p>This class calculates two fundamental structural properties of Petri nets:</p>
 * <ul>
 *   <li><b>T-invariants</b>: Non-negative vectors y such that W·y = 0. They represent
 *       firing sequences that return the net to its initial marking (cyclic behavior).</li>
 *   <li><b>P-invariants</b>: Non-negative vectors x such that x^T·W = 0 (or equivalently
 *       W^T·x = 0). They represent conservative components such as resources, idle places,
 *       or capacity restrictions where the weighted sum of tokens remains constant.</li>
 * </ul>
 * 
 * <p>Where W is the incidence matrix of the net (W = Post - Pre).</p>
 * 
 * @author Juan Ignacio Sassi
 */
public class Invariants {
    /** Incidence matrix of the Petri net (W = Post - Pre) */
    private final int[][] W;
    
    /** Transposed incidence matrix (W^T) */
    private final int[][] Wt;
    
    /** List of minimal P-invariants */
    private List<List<Integer>> pInvariant;
    
    /** List of minimal T-invariants */
    private List<List<Integer>> tInvariant;

    /**
     * Constructs an invariant calculator and computes all minimal invariants.
     * 
     * <p>The constructor automatically computes:</p>
     * <ul>
     *   <li>P-invariants by finding the nullspace of W^T</li>
     *   <li>T-invariants by finding the nullspace of W</li>
     * </ul>
     * 
     * @param W incidence matrix (Post - Pre) of the Petri net
     */
    public Invariants(int[][] W) {
        this.W = W;
        this.Wt = Matrix.transposed(W);
        this.pInvariant = computeInvariants(Wt);
        this.tInvariant = computeInvariants(W);
    }

    /**
     * Computes all minimal invariants of the input matrix.
     * 
     * <p>The algorithm consists of the following steps:</p>
     * <ol>
     *   <li>Compute the nullspace basis of the matrix using exact rational arithmetic</li>
     *   <li>Generate all non-negative linear combinations of basis vectors with
     *       bounded coefficients</li>
     *   <li>Filter to keep only minimal invariants (those whose support is not a
     *       proper superset of another invariant's support)</li>
     *   <li>Sort results by component sum (simplest invariants first)</li>
     * </ol>
     * 
     * @param matrix matrix for which invariants are sought (W for T-invariants,
     *               W^T for P-invariants)
     * @return list of minimal invariant vectors as lists of integers
     */
    public List<List<Integer>> computeInvariants(int[][] matrix) {
        List<int[]> nullBasis = Nullspace.compute(matrix);
        int maxCoeff = 4;
        Set<List<Integer>> all = new HashSet<>();

        // Generate all non-negative linear combinations
        generateCombinations(nullBasis, new int[nullBasis.size()], 0, maxCoeff, all);
        if (all.isEmpty()) {
            System.out.println("\nWARNING: No invariants found.");
        }

        // Filter only minimal invariants
        List<List<Integer>> minimal = new ArrayList<>();
        for (List<Integer> inv : all) 
            if (isMinimal(inv, all)) 
                minimal.add(inv);

        // Sort by sum of components (simplest invariants first)
        minimal.sort(Comparator.comparingInt(v -> v.stream().mapToInt(Integer::intValue).sum()));
        return minimal;
    }

    /**
     * Recursively generates all non-negative linear combinations of basis vectors.
     * 
     * <p>For each basis vector, tests coefficients from -maxCoeff to +maxCoeff.
     * Only combinations resulting in completely non-negative vectors with at least
     * one positive component are kept. The trivial zero vector is ignored.</p>
     * 
     * @param basis nullspace basis vectors
     * @param coeffs array of coefficients (filled recursively)
     * @param idx current index in the recursion
     * @param maxCoeff absolute maximum value for each coefficient
     * @param set set where valid combinations are stored (duplicates auto-removed)
     */
    private void generateCombinations(List<int[]> basis, int[] coeffs, int idx, int maxCoeff, Set<List<Integer>> set) {
        if (idx == basis.size()) {
            int n = basis.get(0).length;
            int[] combo = new int[n];
            boolean allZero = true;
            
            // Calculate the linear combination
            for (int i = 0; i < basis.size(); i++) {
                if (coeffs[i] != 0) allZero = false;
                for (int j = 0; j < n; j++) {
                    combo[j] += coeffs[i] * basis.get(i)[j];
                }
            }
            
            if (allZero) return;
            
            // Verify that it is non-negative and has at least one positive component
            boolean nonNeg = true, anyPos = false;
            for (int v : combo) {
                if (v < 0) {
                    nonNeg = false;
                    break;
                }
                if (v > 0) anyPos = true;
            }
            
            if (nonNeg && anyPos) {
                // Reduce the vector to its minimal form by dividing by the GCD
                int g = Nullspace.gcdArray(combo);
                if (g > 0) {
                    for (int j = 0; j < combo.length; j++) {
                        combo[j] /= g;
                    }
                }
                
                // Add to set (duplicates are automatically removed)
                List<Integer> lst = new ArrayList<>();
                for (int v : combo) lst.add(v);
                set.add(lst);
            }
            return;
        }
        
        // Recursive case: test all possible coefficients for the current vector
        for (int c = -maxCoeff; c <= maxCoeff; c++) {
            coeffs[idx] = c;
            generateCombinations(basis, coeffs, idx + 1, maxCoeff, set);
        }
    }

    /**
     * Checks whether an invariant is minimal.
     * 
     * <p>An invariant is minimal if there is no other invariant whose support
     * (set of indices with positive values) is a proper subset of its own support.</p>
     * 
     * @param inv invariant vector to check
     * @param all set of all invariants found
     * @return true if the invariant is minimal, false otherwise
     */
    private boolean isMinimal(List<Integer> inv, Set<List<Integer>> all) {
        // Compute the support of the current invariant
        Set<Integer> supp = new HashSet<>();
        for (int i = 0; i < inv.size(); i++) 
            if (inv.get(i) > 0) 
                supp.add(i);
        
        // Check against all other invariants
        for (List<Integer> other : all) {
            if (other.equals(inv)) continue;
            
            Set<Integer> supp2 = new HashSet<>();
            for (int i = 0; i < other.size(); i++) 
                if (other.get(i) > 0) 
                    supp2.add(i);
            
            // If another invariant has strictly contained support then it is not minimal
            if (supp.containsAll(supp2) && !supp.equals(supp2)) 
                return false;
        }
        return true;
    }

    /**
     * Prints all minimal P-invariants to the console.
     */
    public void printPInvariants() {
        System.out.println("\n=================================");
        System.out.println("P-invariants (minimal invariants found): " + pInvariant.size() + "\n");
        for (int i = 0; i < pInvariant.size(); i++) {
            System.out.println("x" + (i + 1) + " = " + pInvariant.get(i));
        }
    }

    /**
     * Prints all minimal T-invariants to the console.
     */
    public void printTInvariants() {
        System.out.println("\n=================================");
        System.out.println("T-invariants (minimal invariants found): " + tInvariant.size() + "\n");
        for (int i = 0; i < tInvariant.size(); i++) {
            System.out.println("y" + (i + 1) + " = " + tInvariant.get(i));
        }
    }

    /**
     * Gets the list of computed P-invariants.
     * 
     * @return list of minimal P-invariant vectors
     */
    public List<List<Integer>> getPInvariants() {
        return pInvariant;
    }

    /**
     * Gets the list of computed T-invariants.
     * 
     * @return list of minimal T-invariant vectors
     */
    public List<List<Integer>> getTInvariants() {
        return tInvariant;
    }
}