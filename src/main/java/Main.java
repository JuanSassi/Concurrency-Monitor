import java.util.List;

public class Main {
  public static void main(String[] args) {
    ThreadAllocator allocator = new ThreadAllocator();

    allocator.Algorithm1();
    allocator.Algorithm2();
    allocator.Algorithm3();

    new AllocationLogger(allocator).logAll();
  }
}
