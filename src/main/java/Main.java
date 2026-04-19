/**
 * Entry point of the Concurrency Monitor application.
 * Loads the Petri net and configuration, then prints a summary.
 *
 * @author Sassi Juan Ignacio
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("=== Concurrency Monitor ===");
        System.out.println();

        // ── Configuración general ────────────────────────────
        System.out.println("--- Configuration ---");
        System.out.println("Petri net file   : " + ConfigLoader.getPetrinetFile());
        System.out.println("Max invariants   : " + ConfigLoader.getMaxInvariants());
        System.out.println("Standard policy  : " + ConfigLoader.isStandardPolicies());
        System.out.println("Standard value   : " + ConfigLoader.getStandardPolicies());
        System.out.println("Full print       : " + ConfigLoader.getFullprint());

        int totalPolicies = ConfigLoader.getTotalPolicies();
        System.out.println("Policy values    : " + totalPolicies);
        for (int i = 1; i <= totalPolicies; i++) {
            System.out.println("  policy[" + i + "]     : " + ConfigLoader.getValuePolicies(i));
        }

        System.out.println();

        // ── Red de Petri ─────────────────────────────────────
        System.out.println("--- Petri Net ---");
        System.out.println("Places           : " + PetrinetLoader.getNumPlaces());
        System.out.println("Transitions      : " + PetrinetLoader.getNumTransitions());

        System.out.println("Initial marking  : " + arrayToString(PetrinetLoader.getInitialMarkingVector()));
        System.out.println("Temporal trans.  : " + arrayToString(PetrinetLoader.getTemporalTransitionsVector()));

        System.out.println();
        System.out.println("Pre matrix:");
        printMatrix(PetrinetLoader.getPreMatrix());

        System.out.println();
        System.out.println("Post matrix:");
        printMatrix(PetrinetLoader.getPostMatrix());

        System.out.println();
        System.out.println("=== Done ===");
    }

    private static String arrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            System.out.printf("  P%-2d %s%n", i, arrayToString(matrix[i]));
        }
    }
}