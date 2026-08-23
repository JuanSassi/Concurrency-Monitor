import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Petri net analysis (Algorithms 4.1, 4.2, and 4.3) as JSON.
 *
 * <p>For now this always analyzes the net currently configured in {@code config.properties} — the
 * same net {@code Main} prints to the console. Letting the frontend choose a different catalog net,
 * or submit a custom one, is a follow-up step once this baseline works end to end.
 *
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

  private AnalysisResponse buildResponse(ThreadAllocator allocator) {
    PlaceClassifier classifier = allocator.getClassifier();
    Invariants invariants = allocator.getInvariants();
    Responsibilities responsibilities = allocator.getResponsibilities();

    String resourcePlaces = formatSet(classifier.getResourcePlaces(), "P");
    String actionPlaces = formatSet(classifier.getActionPlaces(), "P");

    List<String> tInvariants = labelEach("IT", invariants.getTInvariants(), "T");
    List<String> piOfIt = labelEach("PI", classifier.getPiOfIt(), "P");
    List<String> paOfIt = labelEach("PA", classifier.getPaOfIt(), "P");
    List<String> segments = labelEach("S", allocator.getSegments(), "T");
    List<String> segmentPlaces = labelEach("PS", allocator.computeAllSegmentPlaces(), "P");

    String forks = formatIndices(responsibilities.getForkPlaces(), "P");
    String joins = formatIndices(responsibilities.getJoinPlaces(), "P");

    List<Integer> threadsPerSegment = allocator.getThreadsPerSegment();
    List<String> threadsPerSegmentLines = new ArrayList<>();
    int totalThreads = 0;
    for (int i = 0; i < threadsPerSegment.size(); i++) {
      threadsPerSegmentLines.add("Max(MS" + (i + 1) + ") = " + threadsPerSegment.get(i));
      totalThreads += threadsPerSegment.get(i);
    }

    return new AnalysisResponse(
        resourcePlaces,
        actionPlaces,
        tInvariants,
        piOfIt,
        paOfIt,
        allocator.getMaxActiveThreads(),
        allocator.getTree().getNumReachableMarkings(),
        segments,
        forks,
        joins,
        segmentPlaces,
        threadsPerSegmentLines,
        totalThreads);
  }

  private String formatSet(Set<Integer> indices, String prefix) {
    List<Integer> sorted = new ArrayList<>(indices);
    Collections.sort(sorted);
    return formatIndices(sorted, prefix);
  }

  private String formatIndices(List<Integer> indices, String prefix) {
    List<String> labels = new ArrayList<>();
    for (Integer idx : indices) {
      labels.add(prefix + idx);
    }
    return "{" + String.join(", ", labels) + "}";
  }

  private List<String> labelEach(String label, List<List<Integer>> groups, String prefix) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < groups.size(); i++) {
      lines.add(label + (i + 1) + " = " + formatIndices(groups.get(i), prefix));
    }
    return lines;
  }
}