import java.util.*;

/**
 * Clase que representa un grafo de marcado para plazas de acción.
 * Solo almacena y muestra las marcas alcanzables de las plazas de acción,
 * sin información sobre transiciones.
 */
public class ReachabilityTree {
    private PetriNet petriNet;
    private Set<Integer> actionPlaces;
    private Set<int[]> reachableMarkings; // Marcas únicas alcanzables
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
        getMarkSum();
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

    private void getMarkSum() {
        markSum = new int[reachableMarkings.size()];
        int i = 0;
        for (int[] marking : reachableMarkings) {
            int sum = 0;
            for (int tokens : marking) {
                sum += tokens;
            }
            markSum[i] = sum;
            i++;
        }
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
        System.out.println("=== MARCAS ALCANZABLES (Plazas de Acción) ===");
        System.out.println("Total de marcas únicas: " + reachableMarkings.size());
        System.out.println("Maximo numero de hilos activos: " + maxNumThreads);
        System.out.println();
        
        // Crear lista ordenada de plazas de acción para el encabezado
        List<Integer> sortedPlaces = new ArrayList<>(actionPlaces);
        Collections.sort(sortedPlaces);
        
        // Imprimir encabezado
        System.out.print("M\t");
        for (Integer place : sortedPlaces) {
            System.out.print("P" + place + "\t");
        }
        System.out.println("SUMA");
        
        // Imprimir línea separadora
        System.out.print("---\t");
        for (int j = 0; j < sortedPlaces.size(); j++) {
            System.out.print("---\t");
        }
        System.out.println("----");
        
        // Imprimir marcados
        int i = 0;
        for (int[] marking : reachableMarkings) {
            System.out.print("M" + i + "\t");
            for (int token : marking) {
                System.out.print(token + "\t");
            }
            System.out.println(markSum[i]);
            i++;
        }
    }
}