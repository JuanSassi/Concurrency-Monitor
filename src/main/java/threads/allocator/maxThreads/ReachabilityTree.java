import java.util.*;
import java.util.ArrayList;

/**
 * Clase que representa un grafo de marcado para plazas de acción.
 * Solo almacena y muestra las marcas alcanzables de las plazas de acción,
 * sin información sobre transiciones.
 */
public class ReachabilityTree {
    private PetriNet petriNet;
    private Set<Integer> actionPlaces;
    private Set<int[]> reachableMarkings;
    private int[] markSum;
    private int maxNumThreads;
    
    /**
     * Constructor del grafo de marcado
     */
    public ReachabilityTree(Set<Integer> actionPlaces) {
        this.petriNet = PetriNet.getInstance();
        this.actionPlaces = actionPlaces;
        this.reachableMarkings = new LinkedHashSet<>();

        buildReachabilitySet();
        this.markSum = getMarkSum();
        this.maxNumThreads = getMaxNumThreads();
    }
    
    /**
     * Construye el conjunto de todas las marcas alcanzables
     */
    private void buildReachabilitySet() {
        Map<String, int[]> visited = new HashMap<>();
        Queue<int[]> queue = new LinkedList<>();
        
        // Marca inicial
        int[] initialMarking = petriNet.getMarking();
        int[] initialActionMarking = extractActionPlaces(initialMarking);
        
        String key = markingToString(initialMarking);
        visited.put(key, initialActionMarking);
        reachableMarkings.add(initialActionMarking);
        queue.add(initialMarking);
        
        // Exploración BFS
        while (!queue.isEmpty()) {
            int[] currentMarking = queue.poll();
            setMarking(currentMarking);
            
            // Probar todas las transiciones habilitadas
            for (int t = 0; t < petriNet.getNumTransitions(); t++) {
                if (petriNet.transitionEnabled(t)) {
                    // Disparar transición
                    if (petriNet.isTemporary(t)) {
                        petriNet.consumeTokens(t);
                        petriNet.produceTokens(t);
                    } else {
                        petriNet.fire(t);
                    }
                    
                    // Obtener nueva marca
                    int[] newMarking = petriNet.getMarking();
                    String newKey = markingToString(newMarking);
                    
                    // Si es una marca nueva, agregarla
                    if (!visited.containsKey(newKey)) {
                        int[] newActionMarking = extractActionPlaces(newMarking);
                        visited.put(newKey, newActionMarking);
                        reachableMarkings.add(newActionMarking);
                        queue.add(newMarking);
                    }
                    
                    // Restaurar marca para probar otras transiciones
                    setMarking(currentMarking);
                }
            }
        }
    }

    private int[] getMarkSum() {
        int[] marks = new int[reachableMarkings.size()];
        int i = 0;
        for (int[] marking : reachableMarkings) {
            int sum = 0;
            for (int tokens : marking) {
                sum += tokens;
            }
            marks[i] = sum;
            i++;
        }
        return marks;
    }

    /**
     * Obtiene el valor máximo de suma de tokens en todas las marcas alcanzables
     */
    public int getMaxNumThreads() {
        int max = 0;
        for (int sum : markSum) {
            if (sum > max) {
                max = sum;
            }
        }
        return max;
    }
    
    /**
     * Extrae solo los valores de las plazas de acción
     */
    private int[] extractActionPlaces(int[] fullMarking) {
        int[] actionMarking = new int[actionPlaces.size()];
        int index = 0;
        List<Integer> sortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(sortedPlaces);
        for (Integer placeIndex : sortedPlaces) {
            if (placeIndex >= 0 && placeIndex < fullMarking.length) {
                actionMarking[index++] = fullMarking[placeIndex];
            }
        }
        return actionMarking;
    }
    
    /**
     * Convierte un marcado a String para usarlo como clave
     */
    private String markingToString(int[] marking) {
        return Arrays.toString(marking);
    }
    
    /**
     * Establece un marcado en la red de Petri
     */
    private void setMarking(int[] marking) {
        try {
            java.lang.reflect.Method method = petriNet.getClass()
                .getDeclaredMethod("setMarking", int[].class);
            method.setAccessible(true);
            method.invoke(petriNet, (Object) marking);
        } catch (Exception e) {
            throw new RuntimeException("Agrega setMarking(int[]) en PetriNet", e);
        }
    }
    
    /**
     * Obtiene todas las marcas alcanzables de las plazas de acción
     */
    public Set<int[]> getReachableMarkings() {
        return new LinkedHashSet<>(reachableMarkings);
    }
    
    /**
     * Obtiene el número de marcas alcanzables
     */
    public int getNumReachableMarkings() {
        return reachableMarkings.size();
    }
    
    /**
     * Imprime todas las marcas alcanzables
     */
    public void printMarkings() {
        System.out.println("=== ACHIEVABLE MARKS (Action places) ===");
        System.out.println("Total unique marks: " + reachableMarkings.size());
        System.out.println("Maximum number of active threads: " + maxNumThreads);
        System.out.println();
        
        // Crear lista ordenada de plazas de acción para el encabezado
        List<Integer> sortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(sortedPlaces);
        
        // Imprimir encabezado
        System.out.print("M\t");
        for (Integer place : sortedPlaces) {
            System.out.print("P" + place + "\t");
        }
        System.out.println("SUM");
        
        // Imprimir línea separadora
        System.out.print("---\t");
        for (int j = 0; j < sortedPlaces.size(); j++) {
            System.out.print("---\t");
        }
        System.out.println("----");
        
        // Imprimir marcados
        int i = 0;
        boolean fullPrint = ConfigLoader.getFullprint();
        for (int[] marking : reachableMarkings) {
            if (i >= 20 && !fullPrint) break;
            System.out.print("M" + i + "\t");
            for (int token : marking) {
                System.out.print(token + "\t");
            }
            System.out.println(markSum[i]);
            i++;
        }
    }

    /**
     * Imprime las marcas alcanzables mostrando solo las plazas especificadas en el segmento
     * @param segment Lista de índices de transiciones del segmento
     * @param segmentPlaces Lista de plazas de acción del segmento
     */
    public void printSegment(List<Integer> segment, List<Integer> segmentPlaces) {
        if (segmentPlaces.isEmpty()) {
            System.out.println("=== SEGMENT MARKS (Transitions: " + segment + ") ===");
            System.out.println("It hasn't action place neither forks and joins in this segment");
            System.out.println("Maximum number of active threads in segment: " + 1);
            System.out.println();
            return;
        }
        
        // Calcular el máximo número de hilos para este segmento
        int maxThreadsSegment = 0;
        List<Integer> allSortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(allSortedPlaces);
        
        for (int[] marking : reachableMarkings) {
            int segmentSum = 0;
            for (Integer segmentPlace : segmentPlaces) {
                int markingIndex = allSortedPlaces.indexOf(segmentPlace);
                if (markingIndex >= 0 && markingIndex < marking.length) {
                    segmentSum += marking[markingIndex];
                }
            }
            if (segmentSum > maxThreadsSegment) {
                maxThreadsSegment = segmentSum;
            }
        }
        
        System.out.println("=== SEGMENT MARKS (Transitions: " + segment + ") ===");
        System.out.println("Action places in segment (without forks and joins): " + segmentPlaces);
        System.out.println("Maximum number of active threads in segment: " + maxThreadsSegment);
        System.out.println();
        
        // Ordenar las plazas del segmento
        Collections.sort(segmentPlaces);
        
        // Imprimir encabezado
        System.out.print("M\t");
        for (Integer place : segmentPlaces) {
            System.out.print("P" + place + "\t");
        }
        System.out.println("SUM");
        
        // Imprimir línea separadora
        System.out.print("---\t");
        for (int j = 0; j < segmentPlaces.size(); j++) {
            System.out.print("---\t");
        }
        System.out.println("----");
        
        // Imprimir marcados filtrados
        int i = 0;
        boolean fullPrint = ConfigLoader.getFullprint();
        for (int[] marking : reachableMarkings) {
            if (i >= 20 && !fullPrint) break;
            
            System.out.print("M" + i + "\t");
            int segmentSum = 0;
            
            // Imprimir solo los tokens de las plazas del segmento
            for (Integer segmentPlace : segmentPlaces) {
                // Encontrar el índice de esta plaza en el marking
                int markingIndex = allSortedPlaces.indexOf(segmentPlace);
                if (markingIndex >= 0 && markingIndex < marking.length) {
                    int tokens = marking[markingIndex];
                    System.out.print(tokens + "\t");
                    segmentSum += tokens;
                } else {
                    System.out.print("0\t");
                }
            }
            
            System.out.println(segmentSum);
            i++;
        }
    }
}