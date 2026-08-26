import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AlgorithmsConsolePrinter} and {@link AnalysisResponse}.
 *
 * <p>El printer no devuelve nada: su único efecto es escribir en {@code System.out}. Los tests
 * capturan esa salida y verifican qué se imprime y en qué notación, reutilizando el análisis
 * cacheado de {@link ThreadAllocatorTest} para no recalcularlo.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("AlgorithmsConsolePrinter y AnalysisResponse")
class AlgorithmsConsolePrinterTest {

  /**
   * Ejecuta una acción capturando todo lo que escriba en la salida estándar.
   *
   * @param action la acción a ejecutar
   * @return el texto escrito en System.out durante la acción
   */
  private static String capture(Consumer<AlgorithmsConsolePrinter> action) {
    AlgorithmsConsolePrinter printer = new AlgorithmsConsolePrinter(ThreadAllocatorTest.huang());
    PrintStream original = System.out;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
      action.accept(printer);
    } finally {
      System.setOut(original);
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }

  // ── printAll ──────────────────────────────────────────────

  @Test
  @DisplayName("printAll imprime los tres algoritmos en orden")
  void testPrintAllPrintsThreeAlgorithms() {
    String output = capture(AlgorithmsConsolePrinter::printAll);

    int first = output.indexOf("Algorithm 4.1");
    int second = output.indexOf("Algorithm 4.2");
    int third = output.indexOf("Algorithm 4.3");

    assertTrue(first >= 0 && second >= 0 && third >= 0, "faltan encabezados");
    assertTrue(first < second && second < third, "el orden de los algoritmos es incorrecto");
  }

  // ── Algoritmo 4.1 ─────────────────────────────────────────

  @Test
  @DisplayName("el algoritmo 4.1 imprime plazas, T-invariantes y el máximo de hilos")
  void testAlgorithm1Sections() {
    String output = capture(AlgorithmsConsolePrinter::printAlgorithm1);

    assertTrue(output.contains("Resources, restrictions and idle places"));
    assertTrue(output.contains("T-invariants"));
    assertTrue(output.contains("PI of IT"));
    assertTrue(output.contains("PA of IT"));
    assertTrue(output.contains("Action places"));
    assertTrue(output.contains("Max active threads: 3"));
    assertTrue(output.contains("Reachable markings: 26"));
  }

  @Test
  @DisplayName("las plazas de recurso salen etiquetadas y ordenadas")
  void testResourcePlacesFormatting() {
    String output = capture(AlgorithmsConsolePrinter::printResourcePlaces);
    assertTrue(output.contains("[P0, P5, P6, P7, P11, P12, P13]"), "salida: " + output.trim());
  }

  @Test
  @DisplayName("los T-invariantes se imprimen como soporte, no como coeficientes")
  void testTInvariantsShowSupport() {
    // Si se usara formatIndices en lugar de formatSupport, saldría algo como {T1, T0, T1}.
    String output = capture(AlgorithmsConsolePrinter::printTInvariants);

    assertTrue(output.contains("IT1 = {T0, T2, T4, T5}"), "salida: " + output.trim());
    assertTrue(output.contains("IT3 = {T6, T7, T8, T9}"));
  }

  @Test
  @DisplayName("PI de IT se imprime como lista de índices")
  void testPiOfItFormatting() {
    String output = capture(AlgorithmsConsolePrinter::printPiOfIt);
    assertTrue(output.contains("PI1: {P0, P1, P3, P4, P6, P12, P13}"), "salida: " + output.trim());
  }

  @Test
  @DisplayName("PA de IT solo lista plazas de acción")
  void testPaOfItFormatting() {
    String output = capture(AlgorithmsConsolePrinter::printPaOfIt);
    assertTrue(output.contains("PA1: {P1, P3, P4}"), "salida: " + output.trim());
  }

  // ── Algoritmo 4.2 ─────────────────────────────────────────

  @Test
  @DisplayName("el algoritmo 4.2 imprime segmentos, forks y joins")
  void testAlgorithm2Sections() {
    String output = capture(AlgorithmsConsolePrinter::printAlgorithm2);

    assertTrue(output.contains("S1 = {T6, T7, T8, T9}"), "salida: " + output.trim());
    assertTrue(output.contains("Forks:"));
    assertTrue(output.contains("Joins:"));
    assertTrue(output.contains("{P1}"), "el fork de exampleHuang es P1");
    assertTrue(output.contains("{P4}"), "el join de exampleHuang es P4");
    assertTrue(output.contains("PS1 = {P8, P9, P10}"));
  }

  // ── Algoritmo 4.3 ─────────────────────────────────────────

  @Test
  @DisplayName("el algoritmo 4.3 imprime una línea por segmento y el total")
  void testAlgorithm3Sections() {
    String output = capture(AlgorithmsConsolePrinter::printAlgorithm3);

    assertTrue(output.contains("Max(MS1) = 1"), "salida: " + output.trim());
    assertTrue(output.contains("Max(MS5) = 1"));
    assertTrue(output.contains("Maximum number of threads in the system = 5"));
  }

  @Test
  @DisplayName("el total impreso coincide con getTotalThreads")
  void testPrintedTotalMatchesAllocator() {
    String output = capture(AlgorithmsConsolePrinter::printAlgorithm3);
    assertTrue(
        output.contains("= " + ThreadAllocatorTest.huang().getTotalThreads()),
        "la consola y el analizador deben coincidir");
  }

  // ── P-invariantes ─────────────────────────────────────────

  @Test
  @DisplayName("printPInvariants existe pero printAll no lo llama")
  void testPInvariantsAreNotPartOfPrintAll() {
    // El método está implementado y funciona, pero ninguno de los tres algoritmos lo
    // invoca, así que los P-invariantes nunca se muestran por consola.
    assertTrue(capture(AlgorithmsConsolePrinter::printPInvariants).contains("P-invariants"));
    assertFalse(capture(AlgorithmsConsolePrinter::printAll).contains("P-invariants"));
  }

  // ── AnalysisResponse ──────────────────────────────────────

  @Test
  @DisplayName("AnalysisResponse conserva todos sus campos")
  void testAnalysisResponseAccessors() {
    AnalysisResponse response = sampleResponse();

    assertEquals("{P0}", response.resourcePlaces());
    assertEquals("{P1}", response.actionPlaces());
    assertEquals(List.of("IT1 = {T0}"), response.tInvariants());
    assertEquals(3, response.maxActiveThreads());
    assertEquals(26, response.reachableMarkings());
    assertEquals(5, response.totalMaxThreads());
  }

  @Test
  @DisplayName("AnalysisResponse compara por contenido")
  void testAnalysisResponseEquality() {
    // A diferencia de PetriNetDefinition y PetriNetRequest, este record no tiene campos
    // array, así que el equals autogenerado sí funciona como uno espera.
    assertEquals(sampleResponse(), sampleResponse());
    assertEquals(sampleResponse().hashCode(), sampleResponse().hashCode());
  }

  @Test
  @DisplayName("el payload de la API usa la misma notación que la consola")
  void testApiUsesSameNotationAsConsole() {
    // Ambos canales pasan por AnalysisFormatter, así que la cadena debe ser idéntica.
    ThreadAllocator allocator = ThreadAllocatorTest.huang();
    String fromApi =
        AnalysisFormatter.formatSortedSet(allocator.getClassifier().getActionPlaces(), "P");

    assertTrue(
        capture(AlgorithmsConsolePrinter::printActionPlaces)
                .contains(fromApi.replace("{", "[").replace("}", "]"))
            || capture(AlgorithmsConsolePrinter::printActionPlaces).contains(fromApi),
        "la consola debería mostrar las mismas plazas que la API: " + fromApi);
  }

  /**
   * Construye una respuesta de ejemplo con valores fijos.
   *
   * @return la respuesta de ejemplo
   */
  private static AnalysisResponse sampleResponse() {
    return new AnalysisResponse(
        "{P0}",
        "{P1}",
        List.of("IT1 = {T0}"),
        List.of("PI1 = {P0, P1}"),
        List.of("PA1 = {P1}"),
        3,
        26,
        List.of("S1 = {T0}"),
        "{P1}",
        "{P4}",
        List.of("PS1 = {P1}"),
        List.of("Max(MS1) = 1"),
        5);
  }
}
