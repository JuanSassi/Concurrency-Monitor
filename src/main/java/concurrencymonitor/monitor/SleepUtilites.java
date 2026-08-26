import java.util.Arrays;

class SleepUtilities {
  private static final int[] TIMES_SLEEPS;

  static {
    int[] times = PetrinetLoader.getTransitionTimes();
    TIMES_SLEEPS = Arrays.copyOf(times, times.length);
  }

  public static void nap(int transition) {
    if (transition < 0 || transition >= TIMES_SLEEPS.length) {
      throw new IllegalArgumentException("Invalid transition index: " + transition);
    }
    System.out.println("Nap for " + TIMES_SLEEPS[transition] + " seconds");
    try {
      Thread.sleep(TIMES_SLEEPS[transition] * 1000);
    } catch (InterruptedException e) {
      System.out.println("ERROR in nap(): " + e);
    }
  }
}
