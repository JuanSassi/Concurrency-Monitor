/**
 * A Petri net submitted from the frontend, before validation.
 *
 * <p>Deserialized straight from the request body. It performs no structural validation of its own —
 * that belongs to {@link PetriNetDefinition} and is triggered by {@link #toDefinition()}. What this
 * record does add is a much tighter size limit than the catalog path uses.
 *
 * <p><b>Why the extra limit.</b> {@code PetriNetDefinition} allows up to 200 places and
 * transitions, which is fine for nets the project ships. A net typed in by a stranger is a
 * different matter: both the reachability search and the invariant search grow explosively with
 * size, and a 40-transition net is enough to keep the server busy for hours. Sixteen keeps every
 * analysis comfortably interactive while still covering the classic textbook nets.
 *
 * @param pre pre-incidence matrix, dimension [places][transitions]
 * @param post post-incidence matrix, dimension [places][transitions]
 * @param initialMarking initial marking vector, dimension [places]
 * @param temporalTransitions per-transition temporal flag vector, dimension [transitions]
 * @author Sassi Juan Ignacio
 */
public record PetriNetRequest(
    int[][] pre, int[][] post, int[] initialMarking, int[] temporalTransitions) {

  /** Largest net accepted from a client, in places and in transitions. */
  public static final int MAX_USER_DIMENSION = 16;

  /**
   * Validates the submitted size and builds a definition.
   *
   * @return the validated net
   * @throws PetriNetValidationException if a component is missing or the net exceeds the size limit
   */
  public PetriNetDefinition toDefinition() {
    validatePresent();
    validateSize();
    return new PetriNetDefinition(pre, post, initialMarking, temporalTransitions);
  }

  /**
   * Checks that every component arrived in the request body.
   *
   * @throws PetriNetValidationException if any component is missing or the matrices are empty
   */
  private void validatePresent() {
    if (pre == null || post == null || initialMarking == null || temporalTransitions == null) {
      throw new PetriNetValidationException(
          "Faltan componentes de la red: se esperan 'pre', 'post', 'initialMarking' y"
              + " 'temporalTransitions'.");
    }
    if (pre.length == 0 || pre[0] == null || pre[0].length == 0) {
      throw new PetriNetValidationException("La red debe tener al menos un lugar y una transición.");
    }
  }

  /**
   * Enforces the client-side size ceiling.
   *
   * @throws PetriNetValidationException if the net has more places or transitions than allowed
   */
  private void validateSize() {
    int places = pre.length;
    int transitions = pre[0].length;

    if (places > MAX_USER_DIMENSION || transitions > MAX_USER_DIMENSION) {
      throw new PetriNetValidationException(
          "Red demasiado grande: máximo "
              + MAX_USER_DIMENSION
              + " lugares y "
              + MAX_USER_DIMENSION
              + " transiciones. Recibido: "
              + places
              + " lugares y "
              + transitions
              + " transiciones.");
    }
  }
}