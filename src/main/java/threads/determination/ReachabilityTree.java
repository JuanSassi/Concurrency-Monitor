import java.util.*;

public class ReachabilityTree {
    /** Pre matrix (token consumption by transitions) */
    private int[][] pre;
    /** Post matrix (token production by transitions) */
    private int[][] post;
    /** Incidence matrix (W = Post - Pre) */
    private int[][] W;
    /** Initial marking vector of the net */
    private int[] m0;
    /** Total number of places in the net */
    private int numPlaces;
    /** Total number of transitions in the net */
    private int numTransitions;
    /** Set of places classified as action places */
    private Set<Integer> actionPlaces;

    public ReachabilityTree(int[][] pre, int[][] post, int[] m0, Set<Integer> actionPlaces) {
        this.actionPlaces = actionPlaces;
        this.pre = pre;
        this.post = post;
        this.m0 = m0;
        this.W = MatrixUtils.subtract(post, pre);
        this.numPlaces = pre.length;
        this.numTransitions = pre[0].length;
    }

    /**
     * Verifica si una transición está habilitada para un marcado dado
     * Según el PDF: Tj está habilitada si m(Pi) >= Pre(Pi, Tj) para todo Pi en °Tj
     */
    private boolean isEnabled(int[] marking, int transition) {
        for (int p = 0; p < numPlaces; p++) {
            if (marking[p] < pre[p][transition]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Dispara una transición y retorna el nuevo marcado
     * Ecuación fundamental: mk = mi + W * s
     */
    private int[] fireTransition(int[] marking, int transition) {
        int[] newMarking = new int[numPlaces];
        for (int p = 0; p < numPlaces; p++) {
            newMarking[p] = marking[p] + W[p][transition];
        }
        return newMarking;
    }


    /**
     * Clase que representa un nodo del grafo (un marcado alcanzable)
     */
    public static class Node {

    }

}