import java.util.List;

public class Main {
  public static void main(String[] args) {
    ThreadAllocator allocator = new ThreadAllocator();

    System.out.println("=== Algorithm 4.1 ===");
    System.out.println("Max active threads: " + allocator.getMaxActiveThreads());
    System.out.println("Reachable markings: " + allocator.getTree().getNumReachableMarkings());

    System.out.println("\n=== Algorithm 4.2 ===");
    System.out.println("Segments: " + allocator.getSegments());
    System.out.println("Forks:    " + allocator.getResponsibilities().getForkPlaces());
    System.out.println("Joins:    " + allocator.getResponsibilities().getJoinPlaces());

    System.out.println("\n=== Algorithm 4.3 ===");
    List<List<Integer>> segmentPlaces = allocator.computeAllSegmentPlaces();
    List<Integer> threadsPerSegment = allocator.getThreadsPerSegment();
    for (int i = 0; i < allocator.getSegments().size(); i++) {
      System.out.println(
          "S"
              + (i + 1)
              + " transitions="
              + allocator.getSegments().get(i)
              + " places="
              + segmentPlaces.get(i)
              + " maxThreads="
              + threadsPerSegment.get(i));
    }

    System.out.println("\n=== PI of IT ===");
    List<List<Integer>> piOfIt = allocator.computePiOfIt();
    for (int i = 0; i < piOfIt.size(); i++) {
      System.out.println("IT" + (i + 1) + " → " + piOfIt.get(i));
    }

    new AllocationLogger(allocator).logAll();
  }
}
