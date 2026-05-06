import java.util.List;

public class Main {
  public static void main(String[] args) {
    int[][] pre = PetrinetLoader.getPreMatrix();
    int[][] post = PetrinetLoader.getPostMatrix();
    int[] m0 = PetrinetLoader.getInitialMarkingVector();
    int[][] w = Matrix.subtract(post, pre);

    Invariants inv = new Invariants(w);
    inv.printTInvariants();
    inv.printPInvariants();

    PlaceClassifier classifier = new PlaceClassifier(pre, post, m0, inv);
    classifier.printClassification();

    System.out.println("Action places per T-invariant:");
    List<List<Integer>> paOfIt = classifier.getPaOfIt();
    for (int i = 0; i < paOfIt.size(); i++) {
      System.out.println("  IT" + (i + 1) + " → " + paOfIt.get(i));
    }
  }
}
