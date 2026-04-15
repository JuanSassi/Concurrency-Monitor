import java.util.Set;
import java.util.HashSet;

/**
 * Policy management class for controlling resource allocation and decision-making
 * in the travel agency Petri net simulation. Implements two distinct policy modes
 * to resolve conflicts and manage system behavior according to specified requirements.
 *
 * Supports two independent policy configurations:
 * 1. Balanced Policy: Ensures equitable distribution of clients between reservation
 *    agents and equal proportions of confirmed vs cancelled reservations.
 * 2. Prioritized Processing Policy: Favors the upper reservation agent (P6) with 75%
 *    of reservations and prioritizes confirmation processes with 80% confirmation rate.
 *
 * @author Sassi Juan Ignacio
 */
class Policy {
    private static final boolean isStandard;
    private static final double standardPolicy;
    private static final double[] valuePolicies;

    static {
        isStandard = ConfigLoader.isStandardPolicies();
        
        standardPolicy = ConfigLoader.getStandardPolicies();

        if (isStandard) {
            // Balanced policy → use a single standard value
            System.out.println("[Policy] Using STANDARD (balanced) policy → " + standardValue);

        } else {
            // Prioritized policy → load all existing policies.value.n
            int totalPolicies = ConfigLoader.getTotalPolicies();
            valuePolicies = new double[totalPolicies];
            for (int i = 0; i < totalPolicies; i++) {
                valuePolicies[i] = ConfigLoader.getValuePolicies(i + 1);
            }

            System.out.print("[Policy] Using PRIORITIZED policy → ");
            for (int i = 0; i < totalPolicies; i++) {
                System.out.print(valuePolicies[i] + (i < totalPolicies - 1 ? ", " : ""));
            }
            System.out.println();
        }
    }

    /**
     * Returns the loaded policy values, either a single standard value
     * or the prioritized set defined in the configuration file.
     *
     * @return array of active policy values
     */
    public static double[] getValuePolicies() {
        return valuePolicies;
    }

    public static double getStandardPolicy() {
        return standardPolicy;
    }

    /**
     * Indicates whether the standard (balanced) policy is active.
     *
     * @return true if using standard policy, false if prioritized
     */
    public static boolean isStandard() {
        return isStandard;
    }

}