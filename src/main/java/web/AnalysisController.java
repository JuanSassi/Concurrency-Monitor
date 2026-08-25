import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Petri net analysis (Algorithms 4.1, 4.2, and 4.3) as JSON.
 *
 * <p>Three ways to pick a net:
 *
 * <ul>
 *   <li>{@code GET /api/nets} — the catalog, for populating a selector
 *   <li>{@code GET /api/analysis/{id}} — analyze a catalog net
 *   <li>{@code POST /api/analysis} — analyze a net submitted in the request body
 * </ul>
 *
 * <p>{@code GET /api/analysis} without an id analyzes the net configured in {@code
 * petrinet.number}, preserving the original behaviour.
 *
 * <p>Every route builds its own {@link ThreadAllocator} from its own definition, so concurrent
 * requests never share analysis state.
 *
 * <p>All string formatting is delegated to {@link AnalysisFormatter}, the same one the console
 * printer uses, so both channels show identical notation for identical data.
 *
 * @see PetriNetCatalog
 * @see AnalysisFormatter
 * @author Sassi Juan Ignacio
 */
@RestController
public class AnalysisController {

  /** Catalog of the nets shipped with the application. */
  private final PetriNetCatalog catalog = new PetriNetCatalog();

  /**
   * An error returned to the client.
   *
   * @param error human-readable description of what went wrong
   */
  public record ApiError(String error) {}

  /**
   * Lists the nets available in the catalog.
   *
   * @return the catalog entries
   */
  @GetMapping("/api/nets")
  public List<PetriNetCatalog.CatalogEntry> listNets() {
    return catalog.list();
  }

  /**
   * Analyzes the net configured as the default one.
   *
   * @return the formatted analysis results
   */
  @GetMapping("/api/analysis")
  public AnalysisResponse getDefaultAnalysis() {
    return analyze(catalog.load(catalog.getDefaultId()));
  }

  /**
   * Analyzes a net from the catalog.
   *
   * @param id the catalog id of the requested net
   * @return the formatted analysis results
   */
  @GetMapping("/api/analysis/{id}")
  public AnalysisResponse getCatalogAnalysis(@PathVariable("id") String id) {
    return analyze(catalog.load(id));
  }

  /**
   * Analyzes a net submitted by the client.
   *
   * @param request the net to analyze
   * @return the formatted analysis results
   */
  @PostMapping("/api/analysis")
  public AnalysisResponse analyzeCustomNet(@RequestBody PetriNetRequest request) {
    if (request == null) {
      throw new PetriNetValidationException("El cuerpo de la petición está vacío.");
    }
    return analyze(request.toDefinition());
  }

  /**
   * Runs the full pipeline for a definition and formats the result.
   *
   * @param definition the net to analyze
   * @return the display-ready results
   */
  private AnalysisResponse analyze(PetriNetDefinition definition) {
    return buildResponse(new ThreadAllocator(definition));
  }

  /**
   * Formats a completed analysis into the response payload.
   *
   * @param allocator the analysis to render
   * @return the display-ready results
   */
  private AnalysisResponse buildResponse(ThreadAllocator allocator) {
    PlaceClassifier classifier = allocator.getClassifier();
    Invariants invariants = allocator.getInvariants();
    Responsibilities responsibilities = allocator.getResponsibilities();

    List<Integer> threadsPerSegment = allocator.getThreadsPerSegment();
    int totalThreads = 0;
    for (Integer threads : threadsPerSegment) {
      totalThreads += threads;
    }

    return new AnalysisResponse(
        AnalysisFormatter.formatSortedSet(classifier.getResourcePlaces(), "P"),
        AnalysisFormatter.formatSortedSet(classifier.getActionPlaces(), "P"),
        AnalysisFormatter.labelInvariants("IT", " = ", invariants.getTInvariants(), "T"),
        AnalysisFormatter.labelIndexGroups("PI", " = ", classifier.getPiOfIt(), "P"),
        AnalysisFormatter.labelIndexGroups("PA", " = ", classifier.getPaOfIt(), "P"),
        allocator.getMaxActiveThreads(),
        allocator.getTree().getNumReachableMarkings(),
        AnalysisFormatter.labelIndexGroups("S", " = ", allocator.getSegments(), "T"),
        AnalysisFormatter.formatIndices(responsibilities.getForkPlaces(), "P"),
        AnalysisFormatter.formatIndices(responsibilities.getJoinPlaces(), "P"),
        AnalysisFormatter.labelIndexGroups("PS", " = ", allocator.computeAllSegmentPlaces(), "P"),
        AnalysisFormatter.labelThreadsPerSegment(threadsPerSegment),
        totalThreads);
  }

  /**
   * Maps invalid net data to 400 — the client sent something the analysis cannot accept.
   *
   * @param e the validation failure
   * @return the error payload
   */
  @ExceptionHandler(PetriNetValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError onInvalidNet(PetriNetValidationException e) {
    return new ApiError(e.getMessage());
  }

  /**
   * Maps malformed JSON to 400 rather than letting it surface as a generic server error.
   *
   * @param e the deserialization failure
   * @return the error payload
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError onMalformedBody(HttpMessageNotReadableException e) {
    return new ApiError("El cuerpo de la petición no es un JSON válido para una red de Petri.");
  }

  /**
   * Maps configuration problems to 500 — the catalog itself is broken, not the request.
   *
   * @param e the configuration failure
   * @return the error payload
   */
  @ExceptionHandler(ConfigurationException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiError onConfigurationError(ConfigurationException e) {
    return new ApiError("Error de configuración del servidor: " + e.getMessage());
  }

  /**
   * Catch-all: cualquier excepción no prevista se devuelve legible en vez del genérico de Spring.
   *
   * @param e la excepción inesperada
   * @return el payload de error
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiError onUnexpectedError(Exception e) {
    return new ApiError(e.getClass().getSimpleName() + ": " + e.getMessage());
  }
}
