import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unit tests for {@link AnalysisController}, without a Spring context.
 *
 * <p>El controller se instancia con {@code new} y sus métodos se llaman directamente. Eso cubre lo
 * que sí es lógica propia — el armado del payload y el mapeo de excepciones — sin pagar el arranque
 * de un contexto.
 *
 * <p>Lo que <b>no</b> cubre: ruteo real, deserialización JSON y los códigos HTTP efectivos. Eso
 * necesita {@code @WebMvcTest} con {@code MockMvc}. Acá las anotaciones se verifican por reflexión,
 * que detecta una ruta o un status cambiados por error pero no que Spring los aplique.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("AnalysisController")
class AnalysisControllerTest {

  /** Controller compartido: no tiene estado mutable entre llamadas. */
  private static final AnalysisController CONTROLLER = new AnalysisController();

  /** Respuesta de la red por defecto, calculada una sola vez. */
  private static AnalysisResponse defaultResponse;

  /**
   * Devuelve la respuesta de la red por defecto, calculándola la primera vez.
   *
   * @return el payload del análisis por defecto
   */
  private static synchronized AnalysisResponse defaultResponse() {
    if (defaultResponse == null) {
      defaultResponse = CONTROLLER.getDefaultAnalysis();
    }
    return defaultResponse;
  }

  // ── GET /api/nets ─────────────────────────────────────────

  @Test
  @DisplayName("listNets devuelve el catálogo completo")
  void testListNets() {
    List<PetriNetCatalog.CatalogEntry> nets = CONTROLLER.listNets();

    assertEquals(new PetriNetCatalog().list(), nets);
    assertFalse(nets.isEmpty());
  }

  // ── GET /api/analysis ─────────────────────────────────────

  @Test
  @DisplayName("getDefaultAnalysis analiza la red configurada por defecto")
  void testDefaultAnalysis() {
    PetriNetCatalog catalog = new PetriNetCatalog();
    assertEquals(CONTROLLER.getCatalogAnalysis(catalog.getDefaultId()), defaultResponse());
  }

  @Test
  @DisplayName("getCatalogAnalysis analiza cualquier red del catálogo")
  void testCatalogAnalysis() {
    for (PetriNetCatalog.CatalogEntry entry : CONTROLLER.listNets()) {
      AnalysisResponse response = CONTROLLER.getCatalogAnalysis(entry.id());
      assertTrue(response.maxActiveThreads() > 0, "red " + entry.name());
      assertTrue(response.reachableMarkings() > 0, "red " + entry.name());
    }
  }

  @Test
  @DisplayName("un id desconocido propaga PetriNetValidationException")
  void testUnknownIdPropagates() {
    assertThrows(
        PetriNetValidationException.class, () -> CONTROLLER.getCatalogAnalysis("no-existe"));
  }

  @Test
  @DisplayName("cada petición devuelve un payload nuevo, no una instancia compartida")
  void testEachRequestBuildsItsOwnPayload() {
    AnalysisResponse first = CONTROLLER.getCatalogAnalysis("1");
    AnalysisResponse second = CONTROLLER.getCatalogAnalysis("1");

    assertNotSame(first, second);
    assertEquals(first, second, "el mismo análisis debe dar el mismo resultado");
  }

  // ── POST /api/analysis ────────────────────────────────────

  @Test
  @DisplayName("analyzeCustomNet analiza una red enviada por el cliente")
  void testCustomNet() {
    PetriNetRequest request =
        new PetriNetRequest(
            new int[][] {{1, 0}, {0, 1}},
            new int[][] {{0, 1}, {1, 0}},
            new int[] {1, 0},
            new int[] {0, 0});

    AnalysisResponse response = CONTROLLER.analyzeCustomNet(request);

    assertEquals(2, response.reachableMarkings());
    assertEquals(1, response.maxActiveThreads());
  }

  @Test
  @DisplayName("un cuerpo vacío lanza PetriNetValidationException")
  void testNullBody() {
    PetriNetValidationException e =
        assertThrows(PetriNetValidationException.class, () -> CONTROLLER.analyzeCustomNet(null));
    assertTrue(e.getMessage().contains("vacío"));
  }

  @Test
  @DisplayName("una red del cliente demasiado grande se rechaza antes de analizar")
  void testOversizedCustomNet() {
    PetriNetRequest tooBig =
        new PetriNetRequest(new int[17][17], new int[17][17], new int[17], new int[17]);

    PetriNetValidationException e =
        assertThrows(PetriNetValidationException.class, () -> CONTROLLER.analyzeCustomNet(tooBig));
    assertTrue(e.getMessage().contains("16"), "debe citar el límite de PetriNetRequest");
  }

  // ── armado del payload ────────────────────────────────────

  @Test
  @DisplayName("cada campo del payload corresponde al dato correcto del análisis")
  void testResponseFieldMapping() {
    // AnalysisResponse tiene 13 campos posicionales y muchos comparten tipo: intercambiar
    // piOfIt con paOfIt, o forks con joins, compila perfecto y devuelve datos cruzados.
    // Este test compara campo por campo contra un análisis calculado por separado.
    PetriNetCatalog catalog = new PetriNetCatalog();
    ThreadAllocator allocator = new ThreadAllocator(catalog.load(catalog.getDefaultId()));
    AnalysisResponse response = defaultResponse();

    assertEquals(
        AnalysisFormatter.formatSortedSet(allocator.getClassifier().getResourcePlaces(), "P"),
        response.resourcePlaces(),
        "resourcePlaces");
    assertEquals(
        AnalysisFormatter.formatSortedSet(allocator.getClassifier().getActionPlaces(), "P"),
        response.actionPlaces(),
        "actionPlaces");
    assertEquals(
        AnalysisFormatter.labelInvariants(
            "IT", " = ", allocator.getInvariants().getTInvariants(), "T"),
        response.tInvariants(),
        "tInvariants");
    assertEquals(
        AnalysisFormatter.labelIndexGroups("PI", " = ", allocator.getClassifier().getPiOfIt(), "P"),
        response.piOfIt(),
        "piOfIt");
    assertEquals(
        AnalysisFormatter.labelIndexGroups("PA", " = ", allocator.getClassifier().getPaOfIt(), "P"),
        response.paOfIt(),
        "paOfIt");
    assertEquals(allocator.getMaxActiveThreads(), response.maxActiveThreads(), "maxActiveThreads");
    assertEquals(
        allocator.getTree().getNumReachableMarkings(),
        response.reachableMarkings(),
        "reachableMarkings");
    assertEquals(
        AnalysisFormatter.labelIndexGroups("S", " = ", allocator.getSegments(), "T"),
        response.segments(),
        "segments");
    assertEquals(
        AnalysisFormatter.formatIndices(allocator.getResponsibilities().getForkPlaces(), "P"),
        response.forks(),
        "forks");
    assertEquals(
        AnalysisFormatter.formatIndices(allocator.getResponsibilities().getJoinPlaces(), "P"),
        response.joins(),
        "joins");
    assertEquals(
        AnalysisFormatter.labelIndexGroups("PS", " = ", allocator.computeAllSegmentPlaces(), "P"),
        response.segmentPlaces(),
        "segmentPlaces");
    assertEquals(
        AnalysisFormatter.labelThreadsPerSegment(allocator.getThreadsPerSegment()),
        response.threadsPerSegment(),
        "threadsPerSegment");
    assertEquals(allocator.getTotalThreads(), response.totalMaxThreads(), "totalMaxThreads");
  }

  @Test
  @DisplayName("forks y joins no están intercambiados")
  void testForksAndJoinsAreNotSwapped() {
    // En exampleHuang el fork es P1 y el join es P4: si estuvieran cruzados este test lo ve.
    assertEquals("{P1}", defaultResponse().forks());
    assertEquals("{P4}", defaultResponse().joins());
  }

  @Test
  @DisplayName("piOfIt y paOfIt no están intercambiados")
  void testPiAndPaAreNotSwapped() {
    // PA siempre es un subconjunto de PI, así que sus líneas nunca pueden ser más largas.
    List<String> pi = defaultResponse().piOfIt();
    List<String> pa = defaultResponse().paOfIt();

    assertEquals(pi.size(), pa.size());
    for (int i = 0; i < pi.size(); i++) {
      assertTrue(pi.get(i).startsWith("PI" + (i + 1)), "etiqueta incorrecta: " + pi.get(i));
      assertTrue(pa.get(i).startsWith("PA" + (i + 1)), "etiqueta incorrecta: " + pa.get(i));
      assertTrue(pa.get(i).length() <= pi.get(i).length(), "PA no puede ser mayor que PI");
    }
  }

  @Test
  @DisplayName("los T-invariantes se serializan como soporte, no como coeficientes")
  void testTInvariantsUseSupportNotation() {
    assertEquals("IT1 = {T0, T2, T4, T5}", defaultResponse().tInvariants().get(0));
  }

  @Test
  @DisplayName("el total del payload es la suma de los hilos por segmento")
  void testTotalIsConsistent() {
    assertEquals(
        defaultResponse().threadsPerSegment().size(), defaultResponse().segments().size());
    assertTrue(defaultResponse().totalMaxThreads() >= defaultResponse().maxActiveThreads());
  }

  // ── manejo de excepciones ─────────────────────────────────

  @Test
  @DisplayName("onInvalidNet devuelve el mensaje de la excepción tal cual")
  void testOnInvalidNet() {
    AnalysisController.ApiError error =
        CONTROLLER.onInvalidNet(new PetriNetValidationException("red inválida"));
    assertEquals("red inválida", error.error());
  }

  @Test
  @DisplayName("onMalformedBody devuelve un mensaje fijo e ignora la excepción recibida")
  void testOnMalformedBody() {
    // El handler no usa su parámetro, así que se le pasa null a propósito: eso evita
    // depender de un constructor de Spring que está deprecado.
    AnalysisController.ApiError error = CONTROLLER.onMalformedBody(null);
    assertTrue(error.error().contains("JSON"));
  }

  @Test
  @DisplayName("onConfigurationError antepone que el problema es del servidor")
  void testOnConfigurationError() {
    AnalysisController.ApiError error =
        CONTROLLER.onConfigurationError(new ConfigurationException("falta config.properties"));

    assertTrue(error.error().contains("configuración del servidor"));
    assertTrue(error.error().contains("falta config.properties"));
  }

  @Test
  @DisplayName("onUnexpectedError incluye el tipo de la excepción")
  void testOnUnexpectedError() {
    AnalysisController.ApiError error =
        CONTROLLER.onUnexpectedError(new IllegalStateException("algo raro"));

    assertTrue(error.error().contains("IllegalStateException"));
    assertTrue(error.error().contains("algo raro"));
  }

  @Test
  @DisplayName("el catch-all también atrapa el crash de Invariants con espacio nulo vacío")
  void testCatchAllCoversInvariantsCrash() {
    // Una red sin invariantes hace crashear a Invariants con IndexOutOfBoundsException.
    // Hoy eso cae en el catch-all y sale como 500. Cuando se arregle ese bug, este test
    // deja de reflejar el camino real y hay que revisarlo.
    AnalysisController.ApiError error =
        CONTROLLER.onUnexpectedError(new IndexOutOfBoundsException("Index 0 out of bounds"));
    assertTrue(error.error().startsWith("IndexOutOfBoundsException"));
  }

  // ── anotaciones de Spring ─────────────────────────────────

  @Test
  @DisplayName("la clase está anotada como @RestController")
  void testIsRestController() {
    assertTrue(AnalysisController.class.isAnnotationPresent(RestController.class));
  }

  @Test
  @DisplayName("las rutas GET y POST son las esperadas")
  void testRoutes() throws NoSuchMethodException {
    assertEquals(
        "/api/nets", method("listNets").getAnnotation(GetMapping.class).value()[0]);
    assertEquals(
        "/api/analysis", method("getDefaultAnalysis").getAnnotation(GetMapping.class).value()[0]);
    assertEquals(
        "/api/analysis/{id}",
        method("getCatalogAnalysis", String.class).getAnnotation(GetMapping.class).value()[0]);
    assertEquals(
        "/api/analysis",
        method("analyzeCustomNet", PetriNetRequest.class)
            .getAnnotation(PostMapping.class)
            .value()[0]);
  }

  @Test
  @DisplayName("el cuerpo del POST se toma de @RequestBody")
  void testPostTakesRequestBody() throws NoSuchMethodException {
    Method post = method("analyzeCustomNet", PetriNetRequest.class);
    assertTrue(post.getParameters()[0].isAnnotationPresent(RequestBody.class));
  }

  @Test
  @DisplayName("los datos inválidos del cliente mapean a 400 y los del servidor a 500")
  void testStatusMapping() throws NoSuchMethodException {
    // Es la distinción que justifica tener dos jerarquías de excepción separadas.
    assertEquals(
        HttpStatus.BAD_REQUEST,
        method("onInvalidNet", PetriNetValidationException.class)
            .getAnnotation(ResponseStatus.class)
            .value());
    assertEquals(
        HttpStatus.BAD_REQUEST,
        method("onMalformedBody", org.springframework.http.converter.HttpMessageNotReadableException.class)
            .getAnnotation(ResponseStatus.class)
            .value());
    assertEquals(
        HttpStatus.INTERNAL_SERVER_ERROR,
        method("onConfigurationError", ConfigurationException.class)
            .getAnnotation(ResponseStatus.class)
            .value());
    assertEquals(
        HttpStatus.INTERNAL_SERVER_ERROR,
        method("onUnexpectedError", Exception.class).getAnnotation(ResponseStatus.class).value());
  }

  @Test
  @DisplayName("cada handler declara el tipo de excepción que atrapa")
  void testExceptionHandlerTypes() throws NoSuchMethodException {
    assertEquals(
        PetriNetValidationException.class,
        method("onInvalidNet", PetriNetValidationException.class)
            .getAnnotation(ExceptionHandler.class)
            .value()[0]);
    assertEquals(
        ConfigurationException.class,
        method("onConfigurationError", ConfigurationException.class)
            .getAnnotation(ExceptionHandler.class)
            .value()[0]);
    assertEquals(
        Exception.class,
        method("onUnexpectedError", Exception.class)
            .getAnnotation(ExceptionHandler.class)
            .value()[0]);
  }

  /**
   * Busca un método público del controller por nombre y tipos de parámetro.
   *
   * @param name el nombre del método
   * @param parameterTypes los tipos de sus parámetros
   * @return el método encontrado
   * @throws NoSuchMethodException si el método no existe con esa firma
   */
  private static Method method(String name, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    return AnalysisController.class.getMethod(name, parameterTypes);
  }
}