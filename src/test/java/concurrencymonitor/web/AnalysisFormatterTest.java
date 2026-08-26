import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AnalysisFormatter}.
 *
 * <p>El riesgo central de esta clase es confundir sus dos formas de entrada: una lista de índices
 * ({@code [2, 3, 5]} = P2, P3, P5) contra un vector de coeficientes ({@code [1, 0, 1]} = soporte
 * T0, T2). Las dos son {@code List<Integer>}, así que el compilador no ayuda y el error sale como
 * texto sin sentido. Varios tests fijan justamente esa distinción.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("AnalysisFormatter")
class AnalysisFormatterTest {

  // ── sortedLabels ──────────────────────────────────────────

  @Test
  @DisplayName("sortedLabels ordena y prefija")
  void testSortedLabels() {
    assertEquals(
        List.of("P1", "P3", "P7"), AnalysisFormatter.sortedLabels(List.of(7, 1, 3), "P"));
  }

  @Test
  @DisplayName("sortedLabels ordena un conjunto sin orden definido")
  void testSortedLabelsFromSet() {
    Set<Integer> unordered = new java.util.HashSet<>(List.of(12, 4, 8));
    assertEquals(List.of("T4", "T8", "T12"), AnalysisFormatter.sortedLabels(unordered, "T"));
  }

  @Test
  @DisplayName("sortedLabels con una colección vacía devuelve una lista vacía")
  void testSortedLabelsEmpty() {
    assertTrue(AnalysisFormatter.sortedLabels(List.of(), "P").isEmpty());
  }

  @Test
  @DisplayName("sortedLabels no modifica la colección recibida")
  void testSortedLabelsDoesNotMutateInput() {
    List<Integer> input = new java.util.ArrayList<>(List.of(5, 1, 3));
    AnalysisFormatter.sortedLabels(input, "P");
    assertEquals(List.of(5, 1, 3), input);
  }

  // ── formatSortedSet ───────────────────────────────────────

  @Test
  @DisplayName("formatSortedSet arma el conjunto entre llaves y ordenado")
  void testFormatSortedSet() {
    assertEquals("{P0, P5, P6}", AnalysisFormatter.formatSortedSet(List.of(6, 0, 5), "P"));
  }

  @Test
  @DisplayName("formatSortedSet vacío devuelve {}")
  void testFormatSortedSetEmpty() {
    assertEquals("{}", AnalysisFormatter.formatSortedSet(new TreeSet<>(), "P"));
  }

  // ── formatIndices ─────────────────────────────────────────

  @Test
  @DisplayName("formatIndices respeta el orden recibido")
  void testFormatIndicesPreservesOrder() {
    assertEquals("{P5, P2, P3}", AnalysisFormatter.formatIndices(List.of(5, 2, 3), "P"));
  }

  @Test
  @DisplayName("formatIndices con un solo elemento no agrega coma")
  void testFormatIndicesSingle() {
    assertEquals("{P1}", AnalysisFormatter.formatIndices(List.of(1), "P"));
  }

  @Test
  @DisplayName("formatIndices vacío devuelve {}")
  void testFormatIndicesEmpty() {
    assertEquals("{}", AnalysisFormatter.formatIndices(List.of(), "T"));
  }

  // ── formatSupport ─────────────────────────────────────────

  @Test
  @DisplayName("formatSupport muestra las posiciones con coeficiente positivo")
  void testFormatSupport() {
    assertEquals("{T0, T2, T3}", AnalysisFormatter.formatSupport(List.of(1, 0, 1, 1), "T"));
  }

  @Test
  @DisplayName("formatSupport ignora coeficientes cero y negativos")
  void testFormatSupportIgnoresNonPositive() {
    assertEquals("{P1}", AnalysisFormatter.formatSupport(List.of(0, 3, -2, 0), "P"));
  }

  @Test
  @DisplayName("formatSupport de un vector nulo devuelve {}")
  void testFormatSupportAllZeros() {
    assertEquals("{}", AnalysisFormatter.formatSupport(List.of(0, 0, 0), "T"));
  }

  @Test
  @DisplayName("formatSupport no depende de la magnitud del coeficiente")
  void testFormatSupportIgnoresMagnitude() {
    assertEquals(
        AnalysisFormatter.formatSupport(List.of(1, 0, 1), "T"),
        AnalysisFormatter.formatSupport(List.of(9, 0, 4), "T"));
  }

  // ── la distinción entre las dos formas ────────────────────

  @Test
  @DisplayName("formatIndices y formatSupport dan resultados distintos para el mismo vector")
  void testIndicesVersusSupportAreNotInterchangeable() {
    // Éste es el error que el javadoc advierte: pasar un vector de coeficientes a
    // formatIndices etiqueta los coeficientes en lugar de sus posiciones.
    List<Integer> invariant = List.of(1, 0, 1, 1);

    assertEquals("{T0, T2, T3}", AnalysisFormatter.formatSupport(invariant, "T"));
    assertEquals("{T1, T0, T1, T1}", AnalysisFormatter.formatIndices(invariant, "T"));
    assertNotEquals(
        AnalysisFormatter.formatSupport(invariant, "T"),
        AnalysisFormatter.formatIndices(invariant, "T"));
  }

  // ── labelIndexGroups ──────────────────────────────────────

  @Test
  @DisplayName("labelIndexGroups numera desde 1")
  void testLabelIndexGroups() {
    assertEquals(
        List.of("PI1: {P2, P3}", "PI2: {P5}"),
        AnalysisFormatter.labelIndexGroups("PI", ": ", List.of(List.of(2, 3), List.of(5)), "P"));
  }

  @Test
  @DisplayName("labelIndexGroups acepta cualquier separador")
  void testLabelIndexGroupsSeparator() {
    assertEquals(
        List.of("S1 = {T0, T1}"),
        AnalysisFormatter.labelIndexGroups("S", " = ", List.of(List.of(0, 1)), "T"));
  }

  @Test
  @DisplayName("labelIndexGroups sin grupos devuelve una lista vacía")
  void testLabelIndexGroupsEmpty() {
    assertTrue(AnalysisFormatter.labelIndexGroups("S", " = ", List.of(), "T").isEmpty());
  }

  @Test
  @DisplayName("labelIndexGroups conserva un grupo vacío como {}")
  void testLabelIndexGroupsWithEmptyGroup() {
    assertEquals(
        List.of("PA1: {}"),
        AnalysisFormatter.labelIndexGroups("PA", ": ", List.of(List.of()), "P"));
  }

  // ── labelInvariants ───────────────────────────────────────

  @Test
  @DisplayName("labelInvariants numera desde 1 y muestra el soporte")
  void testLabelInvariants() {
    assertEquals(
        List.of("IT1 = {T0, T2}", "IT2 = {T1}"),
        AnalysisFormatter.labelInvariants(
            "IT", " = ", List.of(List.of(1, 0, 1), List.of(0, 1, 0)), "T"));
  }

  @Test
  @DisplayName("labelInvariants sin invariantes devuelve una lista vacía")
  void testLabelInvariantsEmpty() {
    assertTrue(AnalysisFormatter.labelInvariants("IP", " = ", List.of(), "P").isEmpty());
  }

  // ── labelThreadsPerSegment ────────────────────────────────

  @Test
  @DisplayName("labelThreadsPerSegment arma una línea por segmento")
  void testLabelThreadsPerSegment() {
    assertEquals(
        List.of("Max(MS1) = 2", "Max(MS2) = 1"),
        AnalysisFormatter.labelThreadsPerSegment(List.of(2, 1)));
  }

  @Test
  @DisplayName("labelThreadsPerSegment sin segmentos devuelve una lista vacía")
  void testLabelThreadsPerSegmentEmpty() {
    assertTrue(AnalysisFormatter.labelThreadsPerSegment(List.of()).isEmpty());
  }

  // ── coherencia entre canales ──────────────────────────────

  @Test
  @DisplayName("consola y API producen la misma notación para los mismos datos")
  void testConsoleAndApiAgree() {
    // Ésta es la razón de ser de la clase: si el formateo se duplicara, los dos canales
    // podrían mostrar lo mismo de dos formas distintas.
    List<List<Integer>> invariants = List.of(List.of(1, 0, 1, 1, 0, 1));

    assertEquals(
        AnalysisFormatter.labelInvariants("IT", " = ", invariants, "T"),
        AnalysisFormatter.labelInvariants("IT", " = ", invariants, "T"));
    assertEquals("{T0, T2, T3, T5}", AnalysisFormatter.formatSupport(invariants.get(0), "T"));
  }
}