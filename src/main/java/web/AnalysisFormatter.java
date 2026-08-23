import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Renders analysis results as display-ready strings.
 *
 * <p>Both output channels — the console ({@code AlgorithmsConsolePrinter}) and the REST API
 * ({@code AnalysisController}) — show the same information in the same notation, so the formatting
 * rules live here rather than being reimplemented on each side.
 *
 * <p>Two distinct shapes are handled, and confusing them silently produces wrong output:
 *
 * <ul>
 *   <li><b>Index lists</b> — {@code [2, 3, 5]} means places P2, P3 and P5. Use {@link
 *       #formatIndices(List, String)}. This is the shape of PI of IT, PA of IT, segments, segment
 *       places, forks and joins.
 *   <li><b>Invariant vectors</b> — {@code [1, 0, 1, 1]} means the coefficient of each transition,
 *       so the set to display is the <i>support</i> {T0, T2, T3}. Use {@link #formatSupport(List,
 *       String)}. This is the shape of everything returned by {@code Invariants}.
 * </ul>
 *
 * @author Sassi Juan Ignacio
 */
public final class AnalysisFormatter {

  /** Private constructor to prevent instantiation of this utility class. */
  private AnalysisFormatter() {
    // Utility class, no instances allowed
  }

  /**
   * Sorts a collection of indices and renders them as prefixed labels.
   *
   * @param indices the indices to label
   * @param prefix label to prepend to each index ({@code "T"} for transitions, {@code "P"} for
   *     places)
   * @return the labels in ascending index order
   */
  public static List<String> sortedLabels(Collection<Integer> indices, String prefix) {
    List<Integer> sorted = new ArrayList<>(indices);
    Collections.sort(sorted);
    return labels(sorted, prefix);
  }

  /**
   * Sorts a collection of indices and renders them as a labeled set, e.g. {@code {P0, P5, P6}}.
   *
   * @param indices the indices to format
   * @param prefix label to prepend to each index
   * @return the sorted set formatted as {@code {prefix..., prefix..., ...}}
   */
  public static String formatSortedSet(Collection<Integer> indices, String prefix) {
    return braces(sortedLabels(indices, prefix));
  }

  /**
   * Renders a list of indices as a labeled set, preserving the given order, e.g. {@code {P2, P3,
   * P5}}.
   *
   * @param indices the indices to format
   * @param prefix label to prepend to each index
   * @return the set formatted as {@code {prefix..., prefix..., ...}}
   */
  public static String formatIndices(List<Integer> indices, String prefix) {
    return braces(labels(indices, prefix));
  }

  /**
   * Renders an invariant's support — the positions holding a positive coefficient — as a labeled
   * set, e.g. {@code {T0, T2, T4, T5}} for a T-invariant or {@code {P1, P3}} for a P-invariant.
   *
   * <p>Note that the argument is a coefficient vector, not a list of indices: passing it to {@link
   * #formatIndices(List, String)} instead would label the coefficients themselves and produce
   * nonsense such as {@code {T1, T0, T1, T1}}.
   *
   * @param invariant the invariant vector
   * @param prefix label to prepend to each index
   * @return the support formatted as {@code {prefix0, prefix1, ...}}
   */
  public static String formatSupport(List<Integer> invariant, String prefix) {
    List<String> supportLabels = new ArrayList<>();
    for (int i = 0; i < invariant.size(); i++) {
      if (invariant.get(i) > 0) {
        supportLabels.add(prefix + i);
      }
    }
    return braces(supportLabels);
  }

  /**
   * Builds one numbered line per group of indices, e.g. {@code "PI1: {P2, P3, P5}"}.
   *
   * @param label line label, repeated with a 1-based counter appended ({@code "PI"}, {@code "S"}…)
   * @param separator text between the numbered label and the set ({@code ": "} or {@code " = "})
   * @param groups the index groups, one per line
   * @param prefix label to prepend to each index inside the set
   * @return one formatted line per group
   */
  public static List<String> labelIndexGroups(
      String label, String separator, List<List<Integer>> groups, String prefix) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < groups.size(); i++) {
      lines.add(label + (i + 1) + separator + formatIndices(groups.get(i), prefix));
    }
    return lines;
  }

  /**
   * Builds one numbered line per invariant, showing its support, e.g. {@code "IT1 = {T0, T2}"}.
   *
   * @param label line label, repeated with a 1-based counter appended ({@code "IT"}, {@code "IP"})
   * @param separator text between the numbered label and the set
   * @param invariants the invariant vectors, one per line
   * @param prefix label to prepend to each index inside the set
   * @return one formatted line per invariant
   */
  public static List<String> labelInvariants(
      String label, String separator, List<List<Integer>> invariants, String prefix) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < invariants.size(); i++) {
      lines.add(label + (i + 1) + separator + formatSupport(invariants.get(i), prefix));
    }
    return lines;
  }

  /**
   * Builds one line per segment with its maximum thread count, e.g. {@code "Max(MS1) = 2"}.
   *
   * @param threadsPerSegment maximum thread count of each segment, in segment order
   * @return one formatted line per segment
   */
  public static List<String> labelThreadsPerSegment(List<Integer> threadsPerSegment) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < threadsPerSegment.size(); i++) {
      lines.add("Max(MS" + (i + 1) + ") = " + threadsPerSegment.get(i));
    }
    return lines;
  }

  /**
   * Prepends the prefix to every index, preserving order.
   *
   * @param indices the indices to label
   * @param prefix label to prepend
   * @return the resulting labels
   */
  private static List<String> labels(List<Integer> indices, String prefix) {
    List<String> result = new ArrayList<>();
    for (Integer idx : indices) {
      result.add(prefix + idx);
    }
    return result;
  }

  /**
   * Joins labels with commas and wraps them in braces.
   *
   * @param labels the labels to join
   * @return the labels rendered as {@code {a, b, c}}
   */
  private static String braces(List<String> labels) {
    return "{" + String.join(", ", labels) + "}";
  }
}