import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ThreadAllocator}.
 *
 * <p>Es el orquestador de los tres algoritmos: construirlo dispara el pipeline completo
 * (invariantes, clasificación, árbol de alcanzabilidad y segmentación). Como para exampleHuang eso
 * cuesta más de un segundo, el análisis se calcula una sola vez por clase.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("ThreadAllocator")
class ThreadAllocatorTest {

  /** Análisis de exampleHuang, calculado una sola vez. */
  private static ThreadAllocator huangAllocator;

  /**
   * Devuelve el análisis de exampleHuang, construyéndolo la primera vez.
   *
   * @return el analizador ya poblado
   */
  static synchronized ThreadAllocator huang() {
    if (huangAllocator == null) {
      huangAllocator = allocatorOf("exampleHuang.properties");
    }
    return huangAllocator;
  }

  // ── Algoritmo 4.1 ─────────────────────────────────────────

  @Test
  @DisplayName("GOLDEN: exampleHuang admite 3 hilos activos simultáneos")
  void testMaxActiveThreads() {
    assertEquals(3, huang().getMaxActiveThreads());
    assertEquals(3, huang().getTree().getMaxNumThreads(), "debe delegar en el árbol");
  }

  @Test
  @DisplayName("GOLDEN: PI de IT de exampleHuang")
  void testPiOfIt() {
    assertEquals(
        List.of(
            List.of(0, 1, 3, 4, 6, 12, 13),
            List.of(0, 1, 2, 4, 5, 12, 13),
            List.of(7, 8, 9, 10, 11, 12, 13)),
        huang().computePiOfIt());
  }

  @Test
  @DisplayName("computePiOfIt devuelve copias mutables e independientes")
  void testPiOfItIsMutableCopy() {
    List<List<Integer>> first = huang().computePiOfIt();
    first.get(0).clear();

    assertFalse(huang().computePiOfIt().get(0).isEmpty(), "la copia no debe afectar al análisis");
  }

  // ── Algoritmo 4.2 ─────────────────────────────────────────

  @Test
  @DisplayName("GOLDEN: segmentos de ejecución de exampleHuang")
  void testSegments() {
    assertEquals(
        List.of(List.of(6, 7, 8, 9), List.of(0), List.of(2, 4), List.of(5), List.of(1, 3)),
        huang().getSegments());
  }

  @Test
  @DisplayName("GOLDEN: plazas de cada segmento de exampleHuang")
  void testSegmentPlaces() {
    assertEquals(
        List.of(List.of(8, 9, 10), List.of(1), List.of(3), List.of(4), List.of(2)),
        huang().computeAllSegmentPlaces());
  }

  @Test
  @DisplayName("hay una lista de plazas por cada segmento")
  void testOneePlaceGroupPerSegment() {
    assertEquals(huang().getSegments().size(), huang().computeAllSegmentPlaces().size());
  }

  @Test
  @DisplayName("las plazas de un segmento son todas plazas de acción")
  void testSegmentPlacesAreActionPlaces() {
    for (List<Integer> group : huang().computeAllSegmentPlaces()) {
      assertTrue(
          huang().getClassifier().getActionPlaces().containsAll(group),
          "se coló una plaza de recurso: " + group);
    }
  }

  @Test
  @DisplayName("las plazas de cada segmento salen ordenadas")
  void testSegmentPlacesAreSorted() {
    for (List<Integer> group : huang().computeAllSegmentPlaces()) {
      for (int i = 1; i < group.size(); i++) {
        assertTrue(group.get(i - 1) < group.get(i), "orden roto en " + group);
      }
    }
  }

  @Test
  @DisplayName("una plaza que es fork y join a la vez queda fuera de todo segmento")
  void testForkAndJoinPlaceIsExcluded() {
    // En travelAgencySystem, P9 tiene varios productores y varios consumidores: no tiene
    // lado asignable, así que se excluye a propósito. Contarla sumaría un hilo que no
    // existe — un token ahí es trabajo encolado, no un hilo corriendo.
    ThreadAllocator travel = allocatorOf("travelAgencySystem.properties");

    assertTrue(travel.getResponsibilities().getForkPlaces().contains(9), "precondición");
    assertTrue(travel.getResponsibilities().getJoinPlaces().contains(9), "precondición");

    for (List<Integer> group : travel.computeAllSegmentPlaces()) {
      assertFalse(group.contains(9), "P9 no debe aparecer en " + group);
    }
  }

  // ── Algoritmo 4.3 ─────────────────────────────────────────

  @Test
  @DisplayName("GOLDEN: hilos por segmento y total de exampleHuang")
  void testThreadsPerSegment() {
    assertEquals(List.of(1, 1, 1, 1, 1), huang().getThreadsPerSegment());
    assertEquals(5, huang().getTotalThreads());
  }

  @Test
  @DisplayName("el total es la suma de los hilos por segmento")
  void testTotalIsTheSum() {
    int sum = 0;
    for (Integer threads : huang().getThreadsPerSegment()) {
      sum += threads;
    }
    assertEquals(sum, huang().getTotalThreads());
  }

  @Test
  @DisplayName("hay un valor de hilos por cada segmento")
  void testOneThreadCountPerSegment() {
    assertEquals(huang().getSegments().size(), huang().getThreadsPerSegment().size());
  }

  @Test
  @DisplayName("ningún segmento admite menos de un hilo")
  void testEverySegmentHasAtLeastOneThread() {
    for (Integer threads : huang().getThreadsPerSegment()) {
      assertTrue(threads >= 1, "un segmento con 0 hilos no se podría ejecutar");
    }
  }

  @Test
  @DisplayName("GOLDEN: travelAgencySystem reparte 14 hilos en 6 segmentos")
  void testTravelAgencyThreads() {
    ThreadAllocator travel = allocatorOf("travelAgencySystem.properties");

    assertEquals(List.of(5, 1, 1, 5, 1, 1), travel.getThreadsPerSegment());
    assertEquals(14, travel.getTotalThreads());
    assertEquals(5, travel.getMaxActiveThreads());
  }

  // ── composición del pipeline ──────────────────────────────

  @Test
  @DisplayName("los componentes del análisis quedan accesibles y son coherentes entre sí")
  void testComponentsAreWired() {
    assertEquals(
        huang().getInvariants().getTInvariants().size(),
        huang().getClassifier().getPiOfIt().size(),
        "una entrada de PI por T-invariante");
    assertEquals(
        huang().getClassifier().getActionPlaces().size(),
        huang().getTree().getSortedActionPlaces().size(),
        "el árbol debe seguir las mismas plazas de acción que el clasificador");
  }

  @Test
  @DisplayName("getPetriNet devuelve siempre la misma instancia analizada")
  void testPetriNetIsShared() {
    // Es intencional: el Monitor que corre después debe reutilizar exactamente esta red,
    // no una copia, para que análisis y simulación no se separen.
    assertSame(huang().getPetriNet(), huang().getPetriNet());
  }

  @Test
  @DisplayName("la red devuelta está en su marcado inicial")
  void testPetriNetIsAtInitialMarking() {
    PetriNetDefinition d =
        PetriNetProperties.fromResource("exampleHuang.properties").toDefinition();
    org.junit.jupiter.api.Assertions.assertArrayEquals(
        d.initialMarking(), huang().getPetriNet().getMarking());
  }

  @Test
  @DisplayName("dos análisis de la misma red no comparten estado")
  void testTwoAnalysesAreIndependent() {
    PetriNetDefinition d =
        PetriNetProperties.fromResource("travelAgencySystem.properties").toDefinition();
    ThreadAllocator first = new ThreadAllocator(d);
    ThreadAllocator second = new ThreadAllocator(d);

    assertNotSame(first.getPetriNet(), second.getPetriNet());
    first.getPetriNet().setMarking(new int[first.getPetriNet().getNumPlaces()]);

    org.junit.jupiter.api.Assertions.assertArrayEquals(
        d.initialMarking(), second.getPetriNet().getMarking());
  }

  @Test
  @DisplayName("getSegments es inmutable")
  void testSegmentsAreUnmodifiable() {
    assertThrows(UnsupportedOperationException.class, () -> huang().getSegments().clear());
  }

  @Test
  @DisplayName("el constructor sin argumentos usa la red de config.properties")
  void testNoArgConstructorUsesConfiguredNet() {
    ThreadAllocator fromConfig = new ThreadAllocator();
    assertEquals(
        PetrinetLoader.getNumPlaces(), fromConfig.getPetriNet().getNumPlaces());
  }

  // ── propagación de errores ────────────────────────────────

  @Test
  @DisplayName("una red no acotada aborta el análisis con PetriNetValidationException")
  void testUnboundedNetPropagates() {
    // Un ciclo acotado P0↔P1 (que aporta los invariantes que Invariants necesita) más una
    // transición T2 que produce en P2 sin consumir nada: el árbol de alcanzabilidad es
    // infinito y el tope de 50.000 marcados tiene que cortarlo.
    PetriNetDefinition unbounded =
        new PetriNetDefinition(
            new int[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 0}},
            new int[][] {{0, 1, 0}, {1, 0, 0}, {0, 0, 1}},
            new int[] {1, 0, 0},
            new int[] {0, 0, 0});

    assertThrows(PetriNetValidationException.class, () -> new ThreadAllocator(unbounded));
  }

  /**
   * Construye el analizador para una red del classpath.
   *
   * @param resource el nombre del recurso
   * @return el analizador con el pipeline ya ejecutado
   */
  static ThreadAllocator allocatorOf(String resource) {
    return new ThreadAllocator(PetriNetProperties.fromResource(resource).toDefinition());
  }
}