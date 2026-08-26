import java.util.List;

/**
 * Formatted results of the full analysis pipeline (Algorithms 4.1, 4.2, and 4.3), ready to display
 * without any further processing on the frontend.
 *
 * @param resourcePlaces resources, restrictions, and idle places, e.g. {@code {P0, P5, P6}}
 * @param actionPlaces all action places in the net, e.g. {@code {P1, P2, P3}}
 * @param tInvariants one formatted line per T-invariant, e.g. {@code "IT1 = {T0, T2, T4, T5}"}
 * @param piOfIt one formatted line per T-invariant's connected places (input+output, mixed)
 * @param paOfIt one formatted line per T-invariant's action places only
 * @param maxActiveThreads maximum simultaneously active threads (Algorithm 4.1 result)
 * @param reachableMarkings total number of reachable markings found
 * @param segments one formatted line per execution segment, e.g. {@code "S1 = {T6, T7, T8, T9}"}
 * @param forks fork places, e.g. {@code {P1}}
 * @param joins join places, e.g. {@code {P4}}
 * @param segmentPlaces one formatted line per segment's places (forks/joins included)
 * @param threadsPerSegment one formatted line per segment's max threads, e.g. {@code "Max(MS1) =
 *     1"}
 * @param totalMaxThreads sum of threadsPerSegment (Algorithm 4.3 result)
 * @author Sassi Juan Ignacio
 */
public record AnalysisResponse(
    String resourcePlaces,
    String actionPlaces,
    List<String> tInvariants,
    List<String> piOfIt,
    List<String> paOfIt,
    int maxActiveThreads,
    int reachableMarkings,
    List<String> segments,
    String forks,
    String joins,
    List<String> segmentPlaces,
    List<String> threadsPerSegment,
    int totalMaxThreads) {}
