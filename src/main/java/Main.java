/**
 * Console entry point: runs the full Petri net analysis once and hands the result to the console
 * printer and to the file logger.
 *
 * @author Sassi Juan Ignacio
 */
public class Main {
  public static void main(String[] args) {
    ThreadAllocator allocator = new ThreadAllocator();

    new AlgorithmsConsolePrinter(allocator).printAll();
    new AllocationLogger(allocator).logAll();
  }
}