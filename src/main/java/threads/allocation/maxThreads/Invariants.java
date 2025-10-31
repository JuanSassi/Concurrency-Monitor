import java.util.*;

/**
 * Calculation of P-invariants and T-invariants of a Petri Net.
 * 
 * - T-invariants: non-negative vector y such that W*y = 0 (firing sequences that return to the initial marking)
 * - P-invariants: non-negative vector x such that x^T*W = 0 or equivalently W^T*x = 0 (conservative components such as idle places, resources or restrictions)
 * Where W is the network incidence matrix (Post - Pre)
 * 
 * @author Juan Ignacio Sassi
 */
public class Invariants {
    private final int[][] W;
    private final int[][] Wt;
    private List<List<Integer>> pInvariant;
    private List<List<Integer>> tInvariant;

    /**
     * Constructor that initializes the invariant calculator.
     * 
     * @param W Incidence matrix (Post - Pre) of the Petri Net
     */
    public Invariants(int[][] W) {
        this.W = W;
        int rows = W.length;
        int cols = W[0].length;
        int[][] Wtransposed = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Wtransposed[j][i] = W[i][j];
            }
        }
        this.Wt = Wtransposed;

        this.pInvariant = computeInvariants(Wt);
        this.tInvariant = computeInvariants(W);
    }

    /**
     * Computes all minimal invariants of the input matrix.
     * 
     * Process:
     * 1. Computes the nullspace basis of the matrix (W for T-invariants or W^T for P-invariants)
     * 2. Generates all non-negative linear combinations of the basis vectors
     * 3. Filters only minimal invariants (without redundant components)
     * 4. Sort the results
     * 
     * @param matrix matrix in which the invariants are sought
     * @return List of vectors that make up the invariants 
     * (either T-invariants in the case where matrix = W 
     * or P-invariants in the case where matrix = W^T)
     */
    public List<List<Integer>> computeInvariants(int[][] matrix) {
        List<int[]> nullBasis=MatrixUtils.computeNullspace(matrix);
        // Limit for each coefficient in the linear combinations
        int maxCoeff = 4 ;
        Set<List<Integer>> all = new HashSet<>();

        // Generate all non-negative linear combinations
        generateCombinations(nullBasis, new int[nullBasis.size()], 0, maxCoeff, all);
        if (all.isEmpty()) {
            System.out.println("\nWARNING: No invariants found.");
        }

        // Filter only minimal invariants
        List<List<Integer>> minimal=new ArrayList<>();
        for(List<Integer> inv:all) 
            if(isMinimal(inv,all)) 
                minimal.add(inv);

        // Sort by sum of components (simplest invariants first)
        minimal.sort(Comparator.comparingInt(v->v.stream().mapToInt(Integer::intValue).sum()));
        return minimal;
    }

    /**
     * Computes and prints all minimal P-invariants to the console.
     * 
     * Displays the total number of P-invariants found and lists each one
     * in the format "x1 = [vector]", "x2 = [vector]", etc.
     * P-invariants represent conservative components of the Petri Net.
     */
    public void printPInvariants() {
        pInvariant = computeInvariants(Wt);
        System.out.println("\n=================================");
        System.out.println(("P") + "- (minimal invariants found): " + pInvariant.size() + "\n");
        for(int i=0;i<pInvariant.size();i++) {
            System.out.println("x"+(i+1)+" = "+pInvariant.get(i));
        }
    }

    /**
     * Computes and prints all minimal T-invariants to the console.
     * 
     * Displays the total number of T-invariants found and lists each one
     * in the format "y1 = [vector]", "y2 = [vector]", etc.
     * T-invariants represent firing sequences that return to the initial marking.
     */
    public void printTInvariants() {
        tInvariant = computeInvariants(W);
        System.out.println("\n=================================");
        System.out.println(("T") + "- (minimal invariants found): " + tInvariant.size() + "\n");
        for(int i=0;i<tInvariant.size();i++) {
            System.out.println("y"+(i+1)+" = "+tInvariant.get(i));
        }
    }

    /**
     * Recursively generates all non-negative linear combinations of the basis vectors.
     * 
     * For each basis vector, test coefficients from -maxCoeff to +maxCoeff.
     * Only add combinations that result in completely non-negative vectors.
     * 
     * @param basis Nullspace basis vectors
     * @param coeffs Array of coefficients (filled recursively)
     * @param idx Current index in the recursion
     * @param maxCoeff Absolute maximum value for each coefficient
     * @param set Set where valid combinations are stored
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
            
            if (allZero) return; // Ignore the trivial zero vector
            
            // Verify that it is non-negative and has at least one positive component.
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
                int g = MatrixUtils.gcdArray(combo);
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
     * An invariant is minimal if there is no other invariant whose support 
     * (set of indices with positive values) is a proper subset of its own.
     * 
     * @param inv Invariant to be verified
     * @param all Set of all invariants found
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
            
            // If another invariant has strictly contained support then it is not minimal.
            if (supp.containsAll(supp2) && !supp.equals(supp2)) 
                return false;
        }
        return true;
    }

    /**
     * Returns the list of computed P-invariants.
     * 
     * P-invariants are non-negative vectors x such that x^T*W = 0, representing
     * conservative components of the Petri Net (e.g., resources, places that
     * maintain constant token counts across firing sequences).
     * 
     * @return List of minimal P-invariant vectors
     */
    public List<List<Integer>> getPInvariants(){
        return pInvariant;
    }

    /**
     * Returns the list of computed T-invariants.
     * 
     * T-invariants are non-negative vectors y such that W*y = 0, representing
     * firing sequences that return the net to its initial marking (cyclic behavior).
     * 
     * @return List of minimal T-invariant vectors
     */
    public List<List<Integer>> getTInvariants(){
        return tInvariant;
    }

    /**
     * Prints the transitions that are part of each T-Invariant.
     */
    public void printTransitionsOfTI() {
        
        System.out.println("\n=================================");
        System.out.println("T-Invariant Transitions\n");
        
        for (int i = 0; i < tInvariant.size(); i++) {
            List<Integer> tInv = tInvariant.get(i);
            
            // Filter transitions that belong to the invariant (value > 0)
            List<String> transitionNames = new ArrayList<>();
            for (int t = 0; t < tInv.size(); t++) {
                if (tInv.get(t) > 0) {
                    transitionNames.add("T" + t);
                }
            }
            
            System.out.println("y" + (i + 1) + ": " + transitionNames);
        }
    }
}