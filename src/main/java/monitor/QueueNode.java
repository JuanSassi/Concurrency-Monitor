import java.util.concurrent.Semaphore;

/**
 * Nodo de cola usado en todas las colas del monitor (entrada, wait, cortesía).
 *
 * <p>Cada hilo que necesita bloquearse en el monitor crea un {@code QueueNode} propio. El semáforo
 * interno {@link #sem} empieza en 0 (bloqueado). El hilo se bloquea llamando a {@code
 * sem.acquire()} y es despertado cuando otro hilo llama a {@code sem.release()} sobre este nodo.
 *
 * <p>Este patrón (semáforo personal por hilo) es la implementación canónica de colas de espera con
 * señalización selectiva (a diferencia de {@code notifyAll()} que despierta a todos).
 */
public final class QueueNode {

  /** Transición que este hilo quiere disparar. */
  private final int transition;

  /** Segmento (t-invariante) al que pertenece la transición. */
  private final int segment;

  /**
   * Semáforo personal del hilo. Inicializado en 0 → el hilo se bloquea en acquire() hasta que
   * alguien haga release().
   */
  private final Semaphore sem = new Semaphore(0);

  public QueueNode(int transition, int segment) {
    this.transition = transition;
    this.segment = segment;
  }

  public int getTransition() {
    return transition;
  }

  public int getSegment() {
    return segment;
  }

  /** Devuelve el semáforo personal para que el hilo se bloquee o sea despertado. */
  public Semaphore getSemaphore() {
    return sem;
  }
}
