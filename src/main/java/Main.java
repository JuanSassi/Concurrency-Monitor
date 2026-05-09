import java.util.List;

public class Main {
  public static void main(String[] args) {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);
    Invariants inv = new Invariants(w);
    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, inv);

    Responsibilities resp =
        new Responsibilities(pre, post, inv.getTInvariants(), classifier.getActionPlaces());

    System.out.println("Forks: " + resp.getForkPlaces());
    System.out.println("Joins: " + resp.getJoinPlaces());
    System.out.println("Segments:");
    List<List<Integer>> segs = resp.getSegments();
    for (int i = 0; i < segs.size(); i++) {
      System.out.println("  S" + (i + 1) + " → " + segs.get(i));
    }
  }
}
