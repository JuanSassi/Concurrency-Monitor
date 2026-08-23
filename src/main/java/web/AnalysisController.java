import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Petri net analysis (Algorithms 4.1, 4.2, and 4.3) as JSON.
 *
 * <p>For now this always analyzes the net currently configured in {@code config.properties} — the
 * same net {@code Main} prints to the console. Letting the frontend choose a different catalog net,
 * or submit a custom one, is a follow-up step once this baseline works end to end.
 *
 * <p>All string formatting is delegated to {@link AnalysisFormatter}, the same one the console
 * printer uses, so both channels show identical notation for identical data.
 *
 * @see AnalysisFormatter
 * @author Sassi Juan Ignacio
 */
@RestController
public class AnalysisController {

  /**
   * Runs a fresh analysis and returns it formatted for direct display.
   *
   * @return the formatted analysis results
   */
  @GetMapping("/api/analysis")
  public AnalysisResponse getAnalysis() {
    ThreadAllocator allocator = new ThreadAllocator();
    return buildResponse(allocator);
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
        AnalysisFormatter.labelIndexGroups(
            "PS", " = ", allocator.computeAllSegmentPlaces(), "P"),
        AnalysisFormatter.labelThreadsPerSegment(threadsPerSegment),
        totalThreads);
  }
}