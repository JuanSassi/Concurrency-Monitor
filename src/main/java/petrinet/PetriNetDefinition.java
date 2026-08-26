/**
 * Immutable, self-validating definition of a Petri net's structure.
 *
 * <p>Bundles everything needed to construct a {@code PetriNet} and run the full analysis pipeline:
 * pre/post incidence matrices, initial marking, and which transitions are temporal. Structural
 * validity (consistent dimensions, non-negative values, sane size) is enforced once, at
 * construction time — regardless of whether the data comes from a properties file (catalog) or a
 * JSON request body (a net entered by hand on the front end).
 *
 * @param pre pre-incidence matrix, dimension [numPlaces][numTransitions]
 * @param post post-incidence matrix, dimension [numPlaces][numTransitions]
 * @param initialMarking initial marking vector, dimension [numPlaces]
 * @param temporalTransitions per-transition sleep time in seconds (0 = immediate), dimension
 *     [numTransitions]
 * @author Sassi Juan Ignacio
 */
public record PetriNetDefinition(
    int[][] pre, int[][] post, int[] initialMarking, int[] temporalTransitions) {

  /** Upper bound on places/transitions accepted — protects against pathological input. */
  private static final int MAX_DIMENSION = 200;

  public PetriNetDefinition {
    validate(pre, post, initialMarking, temporalTransitions);
    pre = deepCopy(pre);
    post = deepCopy(post);
    initialMarking = initialMarking.clone();
    temporalTransitions = temporalTransitions.clone();
  }

  /**
   * Builds a definition from the currently configured properties file — the existing
   * console/catalog behaviour, unchanged.
   *
   * @return a validated definition read via {@link PetrinetLoader}
   */
  public static PetriNetDefinition fromProperties() {
    return new PetriNetDefinition(
        PetrinetLoader.getPreMatrix(),
        PetrinetLoader.getPostMatrix(),
        PetrinetLoader.getInitialMarkingVector(),
        PetrinetLoader.getTemporalTransitionsVector());
  }

  private static void validate(int[][] pre, int[][] post, int[] m0, int[] temporal) {
    validateNotNull(pre, post, m0, temporal);
    validateBaseDimensions(pre);

    int numPlaces = pre.length;
    int numTransitions = pre[0].length;

    validateShapesMatch(post, m0, temporal, numPlaces, numTransitions);
    checkRowsConsistent(pre, numTransitions, "pre");
    checkRowsConsistent(post, numTransitions, "post");
    checkNonNegative(pre, "pre");
    checkNonNegative(post, "post");
    checkNonNegative(m0, "el marcado inicial");
  }

  private static void validateNotNull(int[][] pre, int[][] post, int[] m0, int[] temporal) {
    if (pre == null || post == null || m0 == null || temporal == null) {
      throw new PetriNetValidationException("Ningún componente de la red puede ser null.");
    }
  }

  private static void validateBaseDimensions(int[][] pre) {
    if (pre.length == 0 || pre[0].length == 0) {
      throw new PetriNetValidationException(
          "La red debe tener al menos un lugar y una transición.");
    }
    if (pre.length > MAX_DIMENSION || pre[0].length > MAX_DIMENSION) {
      throw new PetriNetValidationException(
          "Red demasiado grande: máximo " + MAX_DIMENSION + " lugares/transiciones.");
    }
  }

  private static void validateShapesMatch(
      int[][] post, int[] m0, int[] temporal, int numPlaces, int numTransitions) {
    if (post.length != numPlaces) {
      throw new PetriNetValidationException(
          "post debe tener " + numPlaces + " filas (una por lugar), tiene " + post.length + ".");
    }
    if (m0.length != numPlaces) {
      throw new PetriNetValidationException(
          "El marcado inicial debe tener " + numPlaces + " elementos, tiene " + m0.length + ".");
    }
    if (temporal.length != numTransitions) {
      throw new PetriNetValidationException(
          "El vector de transiciones temporales debe tener "
              + numTransitions
              + " elementos, tiene "
              + temporal.length
              + ".");
    }
  }

  private static void checkRowsConsistent(int[][] matrix, int expectedCols, String name) {
    for (int i = 0; i < matrix.length; i++) {
      if (matrix[i] == null || matrix[i].length != expectedCols) {
        throw new PetriNetValidationException(
            "La fila " + i + " de " + name + " debe tener " + expectedCols + " columnas.");
      }
    }
  }

  private static void checkNonNegative(int[][] matrix, String name) {
    for (int[] row : matrix) {
      for (int v : row) {
        if (v < 0) {
          throw new PetriNetValidationException(name + " no puede contener valores negativos.");
        }
      }
    }
  }

  private static void checkNonNegative(int[] vector, String name) {
    for (int v : vector) {
      if (v < 0) {
        throw new PetriNetValidationException(name + " no puede contener valores negativos.");
      }
    }
  }

  private static int[][] deepCopy(int[][] matrix) {
    int[][] copy = new int[matrix.length][];
    for (int i = 0; i < matrix.length; i++) {
      copy[i] = matrix[i].clone();
    }
    return copy;
  }

  // Accesores explícitos: devuelven copias defensivas, no la referencia interna
  // (los accessors autogenerados por el record expondrían el array mutable).

  @Override
  public int[][] pre() {
    return deepCopy(pre);
  }

  @Override
  public int[][] post() {
    return deepCopy(post);
  }

  @Override
  public int[] initialMarking() {
    return initialMarking.clone();
  }

  @Override
  public int[] temporalTransitions() {
    return temporalTransitions.clone();
  }
}
