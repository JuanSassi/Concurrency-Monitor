/**
 * Singleton class representing a Petri net model for a travel agency system.
 * This class models the workflow of client reservations including resources like
 * doors, assistants, managers, and agents. It supports both immediate and temporary
 * transitions with token consumption, production and fire mechanisms.
 *
 * The Petri net consists of 15 places (P0-P14) and 12 transitions (T0-T11)
 * representing different states and operations in the reservation process.
 *
 * @author Sassi Juan Ignacio
 */
public class PetriNet {
    /** Single static instance of PetriNet following Singleton pattern */
    private static PetriNet petriNet = new PetriNet();

    /** Number of places in the Petri net */
    private final int numP = 15;
    /** Number of transitions in the Petri net */
    private final int numT = 12;

    /** Idle place corresponding to the client input buffer */
    private final int client = 5;       // P0

    /** Shared resource: door access control */
    private final int door = 1;         // P1
    /** Shared resource: number of available assistants */
    private final int assistants = 5;   // P4
    /** Shared resource: manager1 availability */
    private final int manager1 = 1;     // P6
    /** Shared resource: manager2 availability */
    private final int manager2 = 1;     // P7
    /** Shared resource: travel agent availability */
    private final int agent = 1;        // P10

    // Initial marking vector
    /**
     * Initial marking vector defining the starting state of all places.
     * The column vector represents a location (P0-P14) with its initial token count.
     */
    private final int[][] initialMarking = {
            {client},       // P0
            {door},         // P1
            {0},            // P2
            {0},            // P3
            {assistants},   // P4
            {0},            // P5
            {manager1},     // P6
            {manager2},     // P7
            {0},            // P8
            {0},            // P9
            {agent},        // P10
            {0},            // P11
            {0},            // P12
            {0},            // P13
            {0}             // P14
    };

    /** Current marking vector representing the current state of Petri net */
    private int[][] marking;

    /**
     * Input incidence matrix (Pre matrix).
     * Element [i][j] represents the number of tokens consumed from place Pi
     * when transition Tj fires. Rows represent places (Pi), columns represent transitions (Tj).
     */
    private final int[][] pre = {
        // row Pi & column Tj
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    /**
     * Output incidence matrix (Post matrix).
     * Element [i][j] represents the number of tokens produced in place Pi
     * when transition Tj fires. Rows represent places (Pi), columns represent transitions (Tj).
     */
    private final int[][] post = {
        // row Pi & column Tj
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0}
    };

    /**
     * Incidence matrix calculated as Post - Pre.
     * Element [i][j] represents the net change in tokens for place Pi
     * when transition Tj fires (positive = tokens added, negative = tokens removed).
     */
    private final int[][] incidence;

    /**
     * Private constructor implementing Singleton pattern.
     * Initializes the marking with initial values and calculates the incidence matrix.
     */
    private PetriNet() {
        // Start marking as a copy of initialMarking
        marking = new int[numP][1];
        for (int i = 0; i < numP; i++) {
            marking[i][0] = initialMarking[i][0];
        }

        // Start incidence as post - pre
        incidence = new int[numP][numT];
        for (int i = 0; i < numP; i++) {
            for (int j = 0; j < numT; j++) {
                incidence[i][j] = post[i][j] - pre[i][j];
            }
        }
    }

    /**
     * Public method to get the unique instance following Singleton pattern.
     *
     * @return The single instance of PetriNet
     */
    public static PetriNet getInstance() {
        return petriNet;
    }

    /**
     * Checks if a transition is enabled (ready to be fired).
     * A transition is enabled when the current marking has enough tokens
     * in all input places to satisfy the pre-conditions.
     *
     * @param transition The index of the transition to check (0-based)
     * @return true if the transition can be fired, false otherwise
     */
    public boolean transitionEnabled(int transition) {
        for (int i = 0; i < numP; i++) {
            if (marking[i][0] < pre[i][transition]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Consumes tokens from places according to the pre-conditions of a temporary transition.
     * This method is used for temporary transitions that have a two-phase firing process:
     * first consume tokens, wait for a predetermined time, then produce tokens.
     *
     * @param transition The index of the transition that consumes tokens
     */
    public void consumeTokens(int transition){
        for(int i = 0; i < numP; i++){
            marking[i][0] -= pre[i][transition];
        }
    }

    /**
     * Produces tokens in places according to the post-conditions of a temporary transition.
     * This method completes the firing process of temporary transitions by adding
     * tokens to the output places.
     *
     * @param transition The index of the transition that produces tokens
     */
    public void produceTokens(int transition){
        for(int i = 0; i < numP; i++){
            marking[i][0] += post[i][transition];
        }
    }

    /**
     * Fires an immediate transition by applying the incidence matrix.
     * This method atomically consumes and produces tokens in a single operation
     * for immediate transitions (non-temporary transitions).
     *
     * @param transition The index of the immediate transition to fire
     */
    public void fire(int transition){
        for(int i = 0; i < incidence.length; i++){
            marking[i][0] += incidence[i][transition];
        }
    }

    /**
     * Returns a copy of the current marking vector representing the net status.
     * This method provides a snapshot of the current state without exposing
     * the internal marking array for modification.
     *
     * @return A copy of the current marking matrix where each row represents
     *         the token count in the corresponding place
     */
    public int[][] getMarking() {
        int[][] copy = new int[marking.length][1];
        for (int i = 0; i < marking.length; i++) {
            copy[i][0] = marking[i][0];
        }
        return copy;
    }
}