/**
 * Configuration loader for accessing Petri net specific configuration values.
 * This class extends PropertiesLoader to provide convenient static methods
 * for retrieving Petri net components such as marking vectors, transition vectors,
 * incidence matrices, and structural dimensions.
 * 
 * <p>The Petri net configuration is loaded from a properties file specified
 * in the main configuration. This class provides methods to access:</p>
 * <ul>
 *   <li>Initial marking vectors</li>
 *   <li>Temporal transitions vectors</li>
 *   <li>Pre and post incidence matrices</li>
 *   <li>Structural dimensions (number of places and transitions)</li>
 * </ul>
 * 
 * @see PropertiesLoader
 * 
 * @author Sassi Juan Ignacio
 */
class PetrinetLoader extends PropertiesLoader {        
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PetrinetLoader() {
       super();
    }

    /**
     * Gets the initial marking vector for the Petri net.
     * 
     * @return an integer array representing the initial marking
     */
    public static int[] getInitialMarkingVector() {
        return getIntArray("initial_marking.vector");
    }

    /**
     * Gets the temporal transitions vector.
     * 
     * @return an integer array representing which transitions are temporal
     */
    public static int[] getTemporalTransitionsVector() {
        return getIntArray("temporary_transitions.vector");
    }

    /**
     * Gets the pre-incidence matrix for the Petri net.
     * The matrix is loaded from properties with keys "matrix.pre.0", "matrix.pre.1", etc.
     * Each row represents a place, and each column represents a transition.
     * 
     * @return a 2D integer array representing the pre-incidence matrix
     * @throws RuntimeException if no rows are found or if rows have inconsistent dimensions
     */
    public static int[][] getPreMatrix() {
        int places = 0;
        while (petrinet.getProperty("matrix.pre." + places) != null) {
            places++;
        }
        
        if (places == 0) {
            throw new RuntimeException("No rows found for matrix pre");
        }
        
        int[] firstRow = getIntArray("matrix.pre.0");
        int transitions = firstRow.length;
        
        int[][] matrix = new int[places][transitions];
        matrix[0] = firstRow;
        
        for (int i = 1; i < places; i++) {
            int[] row = getIntArray("matrix.pre." + i);
            if (row.length != transitions) {
                throw new RuntimeException("The row " + i + " of the pre-matrix must have " + transitions + " items");
            }
            matrix[i] = row;
        }
        
        return matrix;
    }

    /**
     * Gets the post-incidence matrix for the Petri net.
     * The matrix is loaded from properties with keys "matrix.post.0", "matrix.post.1", etc.
     * Each row represents a place, and each column represents a transition.
     * 
     * @return a 2D integer array representing the post-incidence matrix
     * @throws RuntimeException if no rows are found or if rows have inconsistent dimensions
     */
    public static int[][] getPostMatrix() {
        int places = 0;
        while (petrinet.getProperty("matrix.post." + places) != null) {
            places++;
        }
        
        if (places == 0) {
            throw new RuntimeException("No rows found for the post matrix");
        }
        
        int[] firstRow = getIntArray("matrix.post.0");
        int transitions = firstRow.length;
        
        int[][] matrix = new int[places][transitions];
        matrix[0] = firstRow;
        
        for (int i = 1; i < places; i++) {
            int[] row = getIntArray("matrix.post." + i);
            if (row.length != transitions) {
                throw new RuntimeException("The row " + i + " of the post matrix must have " + transitions + " items");
            }
            matrix[i] = row;
        }
        
        return matrix;
    }

    /**
     * Gets the number of places in the Petri net.
     * This is determined by the number of rows in the pre-incidence matrix.
     * 
     * @return the number of places
     */
    public static int getNumPlaces() {
        return getPreMatrix().length;
    }

    /**
     * Gets the number of transitions in the Petri net.
     * This is determined by the number of columns in the pre-incidence matrix.
     * 
     * @return the number of transitions, or 0 if the matrix is empty
     */
    public static int getNumTransitions() {
        return getPreMatrix().length > 0 ? getPreMatrix()[0].length : 0;
    }
}