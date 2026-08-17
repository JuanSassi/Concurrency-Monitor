import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for the log subsystem using the Huang S3PR net.
 *
 * <p>These tests verify that log files are created and written without exceptions. Content
 * correctness is covered by the analysis tests (ThreadAllocatorTest, ReachabilityTreeTest, etc.).
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("Log subsystem — smoke tests")
class AllocationLoggerTest {

  private static final String LOG_DIR = "log";

  private ThreadAllocator allocator;

  @BeforeEach
  void setUp() {
    allocator = new ThreadAllocator();
    // Clean up any existing log files before each test
    deleteLogDirectory();
  }

  @AfterEach
  void tearDown() {
    deleteLogDirectory();
  }

  private void deleteLogDirectory() {
    File logDir = new File(LOG_DIR);
    if (logDir.exists()) {
      File[] files = logDir.listFiles();
      if (files != null) {
        for (File f : files) {
          if (!f.delete()) {
            System.err.println("No se pudo borrar: " + f.getAbsolutePath());
          }
        }
      }
      if (!logDir.delete()) {
        System.err.println("No se pudo borrar el directorio: " + logDir.getAbsolutePath());
      }
    }
  }

  // ── Log base ──────────────────────────────────────────────

  @Test
  @DisplayName("Log crea el directorio log/ si no existe")
  void testLogCreatesDirectory() {
    new Log("test");
    assertTrue(new File(LOG_DIR).exists(), "El directorio log/ debe existir");
  }

  @Test
  @DisplayName("Log crea el archivo .log")
  void testLogCreatesFile() {
    Log log = new Log("test");
    assertTrue(new File(log.getFilePath()).exists(), "El archivo de log debe existir");
  }

  @Test
  @DisplayName("Log.write no lanza excepción")
  void testLogWriteDoesNotThrow() {
    Log log = new Log("test");
    assertDoesNotThrow(() -> log.write("mensaje de prueba"));
  }

  @Test
  @DisplayName("Log.write produce contenido en el archivo")
  void testLogWriteProducesContent() {
    Log log = new Log("test");
    log.write("mensaje de prueba");
    assertTrue(new File(log.getFilePath()).length() > 0, "El archivo de log no debe estar vacío");
  }

  @Test
  @DisplayName("Log crea archivo con sufijo numérico si ya existe uno con el mismo nombre")
  void testLogNumberedSuffixOnDuplicate() {
    Log log1 = new Log("duplicate");
    Log log2 = new Log("duplicate");
    assertNotNull(log1.getFilePath());
    assertNotNull(log2.getFilePath());
    assertTrue(
        !log1.getFilePath().equals(log2.getFilePath()),
        "Dos logs con el mismo nombre deben tener paths distintos");
  }

  // ── AlgorithmsLog ─────────────────────────────────────────

  @Test
  @DisplayName("AlgorithmsLog.logAlgorithm1 no lanza excepción")
  void testLogAlgorithm1DoesNotThrow() {
    AlgorithmsLog log = new AlgorithmsLog();
    assertDoesNotThrow(
        () ->
            log.logAlgorithm1(
                allocator.getInvariants().getTInvariants(),
                allocator.computePiOfIt(),
                allocator.getClassifier().getPaOfIt(),
                allocator.getClassifier().getActionPlaces(),
                allocator.getMaxActiveThreads(),
                allocator.getTree().getNumReachableMarkings()));
  }

  @Test
  @DisplayName("AlgorithmsLog.logAlgorithm2 no lanza excepción")
  void testLogAlgorithm2DoesNotThrow() {
    AlgorithmsLog log = new AlgorithmsLog();
    assertDoesNotThrow(
        () ->
            log.logAlgorithm2(
                allocator.getSegments(),
                allocator.getResponsibilities().getForkPlaces(),
                allocator.getResponsibilities().getJoinPlaces()));
  }

  @Test
  @DisplayName("AlgorithmsLog.logAlgorithm3 no lanza excepción")
  void testLogAlgorithm3DoesNotThrow() {
    AlgorithmsLog log = new AlgorithmsLog();
    assertDoesNotThrow(
        () ->
            log.logAlgorithm3(
                allocator.getSegments(),
                allocator.computeAllSegmentPlaces(),
                allocator.getThreadsPerSegment()));
  }

  @Test
  @DisplayName("AlgorithmsLog produce contenido en el archivo")
  void testAlgorithmsLogProducesContent() {
    AlgorithmsLog log = new AlgorithmsLog();
    log.logAlgorithm1(
        allocator.getInvariants().getTInvariants(),
        allocator.computePiOfIt(),
        allocator.getClassifier().getPaOfIt(),
        allocator.getClassifier().getActionPlaces(),
        allocator.getMaxActiveThreads(),
        allocator.getTree().getNumReachableMarkings());
    assertTrue(new File(log.getFilePath()).length() > 0);
  }

  // ── ReachabilityTreeLog ───────────────────────────────────

  @Test
  @DisplayName("ReachabilityTreeLog.logMarkings no lanza excepción")
  void testReachabilityTreeLogDoesNotThrow() {
    ReachabilityTreeLog log = new ReachabilityTreeLog();
    assertDoesNotThrow(
        () ->
            log.logMarkings(
                allocator.getTree().getReachableMarkings(),
                allocator.getTree().getSortedActionPlaces(),
                allocator.getMaxActiveThreads(),
                true));
  }

  @Test
  @DisplayName("ReachabilityTreeLog produce contenido en el archivo")
  void testReachabilityTreeLogProducesContent() {
    ReachabilityTreeLog log = new ReachabilityTreeLog();
    log.logMarkings(
        allocator.getTree().getReachableMarkings(),
        allocator.getTree().getSortedActionPlaces(),
        allocator.getMaxActiveThreads(),
        true);
    assertTrue(new File(log.getFilePath()).length() > 0);
  }

  @Test
  @DisplayName("ReachabilityTreeLog respeta el límite de 20 markings cuando fullPrint=false")
  void testReachabilityTreeLogLimitedPrint() {
    ReachabilityTreeLog logFull = new ReachabilityTreeLog();
    ReachabilityTreeLog logLimited = new ReachabilityTreeLog();

    logFull.logMarkings(
        allocator.getTree().getReachableMarkings(),
        allocator.getTree().getSortedActionPlaces(),
        allocator.getMaxActiveThreads(),
        true);

    logLimited.logMarkings(
        allocator.getTree().getReachableMarkings(),
        allocator.getTree().getSortedActionPlaces(),
        allocator.getMaxActiveThreads(),
        false);

    // Full print debe producir más contenido que el limitado (26 markings > 20)
    assertTrue(
        new File(logFull.getFilePath()).length() > new File(logLimited.getFilePath()).length(),
        "fullPrint=true debe producir más contenido que fullPrint=false");
  }

  // ── TreePerSegmentLog ─────────────────────────────────────

  @Test
  @DisplayName("TreePerSegmentLog.logAllSegments no lanza excepción")
  void testTreePerSegmentLogDoesNotThrow() {
    TreePerSegmentLog log = new TreePerSegmentLog();
    assertDoesNotThrow(
        () ->
            log.logAllSegments(
                allocator.getSegments(),
                allocator.computeAllSegmentPlaces(),
                allocator.getTree().getReachableMarkings(),
                allocator.getTree().getSortedActionPlaces(),
                allocator.getThreadsPerSegment(),
                true));
  }

  @Test
  @DisplayName("TreePerSegmentLog produce contenido en el archivo")
  void testTreePerSegmentLogProducesContent() {
    TreePerSegmentLog log = new TreePerSegmentLog();
    log.logAllSegments(
        allocator.getSegments(),
        allocator.computeAllSegmentPlaces(),
        allocator.getTree().getReachableMarkings(),
        allocator.getTree().getSortedActionPlaces(),
        allocator.getThreadsPerSegment(),
        true);
    assertTrue(new File(log.getFilePath()).length() > 0);
  }

  // ── AllocationLogger ──────────────────────────────────────

  @Test
  @DisplayName("AllocationLogger.logAll no lanza excepción")
  void testAllocationLoggerDoesNotThrow() {
    assertDoesNotThrow(() -> new AllocationLogger(allocator).logAll());
  }

  @Test
  @DisplayName("AllocationLogger.logAll crea el directorio log/")
  void testAllocationLoggerCreatesLogDir() {
    new AllocationLogger(allocator).logAll();
    assertTrue(new File(LOG_DIR).exists());
  }

  @Test
  @DisplayName("AllocationLogger.logAll crea 3 archivos de log")
  void testAllocationLoggerCreatesThreeFiles() {
    new AllocationLogger(allocator).logAll();
    File logDir = new File(LOG_DIR);
    assertTrue(logDir.exists());
    assertTrue(logDir.listFiles().length >= 3, "Se deben crear al menos 3 archivos de log");
  }
}
