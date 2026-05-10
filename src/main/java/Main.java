import java.util.List;

public class Main {
  public static void main(String[] args) {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);
    Invariants inv = new Invariants(w);
    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, inv);

    ReachabilityTree tree = new ReachabilityTree(classifier.getActionPlaces());

    System.out.println("Action places (sorted): " + tree.getSortedActionPlaces());
    System.out.println("Reachable markings:     " + tree.getNumReachableMarkings());
    System.out.println("Max active threads:     " + tree.getMaxNumThreads());

    System.out.println("\nFirst 5 markings:");
    List<int[]> markings = tree.getReachableMarkings();
    for (int i = 0; i < Math.min(5, markings.size()); i++) {
      System.out.println("  M" + i + " → " + java.util.Arrays.toString(markings.get(i)));
    }
  }
}
