import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced logging class for temporal analysis of Petri net transitions.
 * Provides comprehensive statistical analysis capabilities for performance evaluation.
 */
class Log {
    // Thread-safe counters
    private static final Map<Integer, AtomicInteger> countFiredTransitions = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Long>> transitionTimestamps = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Long>> processingTimes = new ConcurrentHashMap<>();
    private static final AtomicInteger invariants = new AtomicInteger(0);
    private static final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

    private static final int[] temporalTransitions;

    // For temporary transitions timing analysis
    private static final Map<Integer, List<Long>> temporaryTransitionDurations = new ConcurrentHashMap<>();
    
    static {
        temporalTransitions = ConfigLoader.getTemporalTransitionsVector();
        // Initialize data structures for all transitions
        for (int i = 0; i < ConfigLoader.getNumTransitions(); i++) {
            countFiredTransitions.put(i, new AtomicInteger(0));
            transitionTimestamps.put(i, Collections.synchronizedList(new ArrayList<>()));
            processingTimes.put(i, Collections.synchronizedList(new ArrayList<>()));
            if (isTemporary(i)) {
                temporaryTransitionDurations.put(i, Collections.synchronizedList(new ArrayList<>()));
            }
        }
    }

    /**
     * Records the firing of a transition with timestamp
     */
    public static void addTransitionFiring(int transition) {
        countFiredTransitions.get(transition).incrementAndGet();
        transitionTimestamps.get(transition).add(System.currentTimeMillis());
        invariants.incrementAndGet();
    }

    /**
     * Records the duration of a temporary transition
     */
    public static void addTemporaryTransitionDuration(int transition, long duration) {
        if (isTemporary(transition)) {
            temporaryTransitionDurations.get(transition).add(duration);
        }
    }

    /**
     * Records the time between consecutive firings of the same transition
     */
    public static void recordProcessingTime(int transition, long processingTime) {
        processingTimes.get(transition).add(processingTime);
    }

    public static int getCount(int transition) {
        return countFiredTransitions.get(transition).get();
    }

    public static boolean endExecution() {
        return invariants.get() >= ConfigLoader.getMaxInvariants();
    }

    /**
     * Comprehensive statistical analysis
     */
    public static void printDetailedStatistics() {
        long totalExecutionTime = System.currentTimeMillis() - startTime.get();
        
        System.out.println("=== ANÁLISIS TEMPORAL DETALLADO ===");
        printPolicyInfo();
        System.out.println("Tiempo total de ejecución: " + totalExecutionTime + " ms");
        System.out.println("Total de invariantes completados: " + invariants.get());
        System.out.println();
        
        // Analysis by transition type
        analyzeImmediateTransitions();
        analyzeTemporaryTransitions();
        analyzeThroughput(totalExecutionTime);
        analyzeSequencePerformance();
    }

    private static void printPolicyInfo() {
        if (Policy.isBalancedPolicy()) {
            System.out.println("Política balanceada (50% Canceladas, 50% Confirmadas)");
        } else {
            System.out.println("Política de procesamiento priorizada (20% Canceladas, 80% Confirmadas)");
        }
    }

    private static void analyzeImmediateTransitions() {
        System.out.println("=== ANÁLISIS DE TRANSICIONES INMEDIATAS ===");
        for (int i = 0; i < ConfigLoader.getNumTransitions(); i++) {
            if (!isTemporary(i)) {
                int count = countFiredTransitions.get(i).get();
                List<Long> timestamps = transitionTimestamps.get(i);
                
                if (count > 0) {
                    System.out.println("T" + i + ": " + count + " disparos");
                    
                    if (timestamps.size() > 1) {
                        List<Long> intervals = calculateIntervals(timestamps);
                        StatisticalAnalysis stats = new StatisticalAnalysis(intervals);
                        
                        System.out.printf("  Intervalo promedio: %.2f ms\n", stats.getMean());
                        System.out.printf("  Desviación estándar: %.2f ms\n", stats.getStandardDeviation());
                        System.out.printf("  Min: %d ms, Max: %d ms\n", stats.getMin(), stats.getMax());
                    }
                }
            }
        }
        System.out.println();
    }

    private static void analyzeTemporaryTransitions() {
        System.out.println("=== ANÁLISIS DE TRANSICIONES TEMPORALES ===");
        
        // CORRECCIÓN: Iterar sobre las transiciones temporales correctamente
        for (int i = 0; i < ConfigLoader.getNumTransitions(); i++) {
            if (isTemporary(i)) {  // Solo procesar transiciones temporales
                int count = countFiredTransitions.get(i).get();
                
                if (count > 0) {
                    System.out.println("T" + i + ": " + count + " disparos");
                    
                    List<Long> durations = temporaryTransitionDurations.get(i);
                    // CORRECCIÓN: Verificar que durations no sea null
                    if (durations != null && !durations.isEmpty()) {
                        StatisticalAnalysis stats = new StatisticalAnalysis(durations);
                        
                        System.out.printf("  Duración promedio: %.2f ms\n", stats.getMean());
                        System.out.printf("  Desviación estándar: %.2f ms\n", stats.getStandardDeviation());
                        System.out.printf("  Min: %d ms, Max: %d ms\n", stats.getMin(), stats.getMax());
                        
                        // Análisis de distribución
                        analyzeDistribution(durations, stats, "T" + i);
                    }
                    
                    // Analyze firing intervals
                    List<Long> timestamps = transitionTimestamps.get(i);
                    if (timestamps != null && timestamps.size() > 1) {
                        List<Long> intervals = calculateIntervals(timestamps);
                        StatisticalAnalysis intervalStats = new StatisticalAnalysis(intervals);
                        System.out.printf("  Intervalo entre disparos: %.2f ± %.2f ms\n", 
                                        intervalStats.getMean(), intervalStats.getStandardDeviation());
                    }
                }
            }
        }
        System.out.println();
    }

    private static void analyzeThroughput(long totalTime) {
        System.out.println("=== ANÁLISIS DE THROUGHPUT ===");
        double totalThroughput = (double) invariants.get() / totalTime * 1000; // per second
        System.out.printf("Throughput total: %.2f invariantes/segundo\n", totalThroughput);
        
        // Throughput por tipo de proceso
        analyzeSequenceThroughput("Proceso de entrada", Sequences.entryProcess, totalTime);
        analyzeSequenceThroughput("Proceso de reserva (arriba)", Sequences.reservationProcessAbove, totalTime);
        analyzeSequenceThroughput("Proceso de reserva (abajo)", Sequences.reservationProcessBelow, totalTime);
        analyzeSequenceThroughput("Proceso de confirmación", Sequences.confirmationProcess, totalTime);
        analyzeSequenceThroughput("Proceso de cancelación", Sequences.cancellationProcess, totalTime);
        System.out.println();
    }

    private static void analyzeSequenceThroughput(String processName, int[] sequence, long totalTime) {
        int minCount = Integer.MAX_VALUE;
        for (int transition : sequence) {
            minCount = Math.min(minCount, countFiredTransitions.get(transition).get());
        }
        double throughput = (double) minCount / totalTime * 1000;
        System.out.printf("%s: %.2f procesos/segundo\n", processName, throughput);
    }
    

    private static void analyzeSequencePerformance() {
        System.out.println("=== ANÁLISIS DE RENDIMIENTO POR SECUENCIA ===");
        
        // Análisis de balanceo de carga entre managers
        int reservationsAbove = countFiredTransitions.get(2).get(); // T2
        int reservationsBelow = countFiredTransitions.get(3).get(); // T3
        double balanceRatio = reservationsAbove > 0 ? (double) reservationsBelow / reservationsAbove : 0;
        
        System.out.println("Balanceo de carga entre managers:");
        System.out.println("  Reservas procesadas por manager arriba (T2): " + reservationsAbove);
        System.out.println("  Reservas procesadas por manager abajo (T3): " + reservationsBelow);
        System.out.printf("  Ratio de balanceo: %.2f\n", balanceRatio);
        
        // Análisis de confirmaciones vs cancelaciones
        int confirmations = countFiredTransitions.get(6).get(); // T6 - start confirmation
        int cancellations = countFiredTransitions.get(7).get(); // T7 - start cancellation
        
        if (confirmations + cancellations > 0) {
            double confirmationRate = (double) confirmations / (confirmations + cancellations) * 100;
            System.out.printf("Tasa de confirmación: %.1f%%\n", confirmationRate);
            System.out.printf("Tasa de cancelación: %.1f%%\n", 100 - confirmationRate);
        }
    }

    private static List<Long> calculateIntervals(List<Long> timestamps) {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < timestamps.size(); i++) {
            intervals.add(timestamps.get(i) - timestamps.get(i-1));
        }
        return intervals;
    }

    private static void analyzeDistribution(List<Long> values, StatisticalAnalysis stats, String transitionName) {
        // Análisis de distribución normal (test de proximidad)
        long withinOneStdDev = values.stream()
            .mapToLong(v -> v)
            .filter(v -> Math.abs(v - stats.getMean()) <= stats.getStandardDeviation())
            .count();
        
        long withinTwoStdDev = values.stream()
            .mapToLong(v -> v)
            .filter(v -> Math.abs(v - stats.getMean()) <= 2 * stats.getStandardDeviation())
            .count();
        
        double oneStdDevPercentage = (double) withinOneStdDev / values.size() * 100;
        double twoStdDevPercentage = (double) withinTwoStdDev / values.size() * 100;
        
        System.out.printf("  Distribución %s:\n", transitionName);
        System.out.printf("    Dentro de 1σ: %.1f%% (esperado: 68.3%%)\n", oneStdDevPercentage);
        System.out.printf("    Dentro de 2σ: %.1f%% (esperado: 95.4%%)\n", twoStdDevPercentage);
        
        // Análisis de sesgo
        if (oneStdDevPercentage < 60) {
            System.out.println("    ⚠ Distribución posiblemente no normal (sesgo detectado)");
        }
    }

    /**
     * Export data for external analysis (e.g., Excel, R, Python)
     */
    public static void exportDataForAnalysis() {
        System.out.println("=== DATOS PARA ANÁLISIS EXTERNO ===");
        System.out.println("Transition,Count,MeanInterval,StdDev,Min,Max");
        
        for (int i = 0; i < ConfigLoader.getNumTransitions(); i++) {
            List<Long> timestamps = transitionTimestamps.get(i);
            if (timestamps.size() > 1) {
                List<Long> intervals = calculateIntervals(timestamps);
                StatisticalAnalysis stats = new StatisticalAnalysis(intervals);
                
                System.out.printf("T%d,%d,%.2f,%.2f,%d,%d\n", 
                    i, countFiredTransitions.get(i).get(),
                    stats.getMean(), stats.getStandardDeviation(),
                    stats.getMin(), stats.getMax());
            }
        }
    }
    /** 
     * Checks if a given transition value corresponds to a temporary transition.
     * Temporary transitions are those defined in this enumeration.
     * The transition consumes tokens, waits for a predetermined time and finally 
     * produces tokens.
     *
     * @param transitionValue The numeric value of the transition to check
     * @return true if the transition is temporary (defined in this enum), false otherwise
     */
    private static boolean isTemporary(int transitionValue) {
        if (transitionValue < 0 || transitionValue >= temporalTransitions.length) {
            throw new IllegalArgumentException("Invalid transition index: " + transitionValue);
        }
        return temporalTransitions[transitionValue] != 0;
    }
}

/**
 * Helper class for statistical calculations
 */
class StatisticalAnalysis {
    private final List<Long> data;
    private final double mean;
    private final double standardDeviation;
    private final long min;
    private final long max;

    public StatisticalAnalysis(List<Long> data) {
        this.data = new ArrayList<>(data);
        this.mean = calculateMean();
        this.standardDeviation = calculateStandardDeviation();
        this.min = Collections.min(data);
        this.max = Collections.max(data);
    }

    private double calculateMean() {
        return data.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private double calculateStandardDeviation() {
        if (data.size() < 2) return 0.0;
        
        double variance = data.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    public double getMean() { return mean; }
    public double getStandardDeviation() { return standardDeviation; }
    public long getMin() { return min; }
    public long getMax() { return max; }
}