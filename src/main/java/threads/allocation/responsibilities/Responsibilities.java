import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

public class Responsibilities {
    /** Pre matrix (token consumption by transitions) */
    private int[][] pre;
    
    /** Post matrix (token production by transitions) */
    private int[][] post;

    private Set<Integer> actionPlaces;
    private List<List<Integer>> tInvariants;

    private List<List<Integer>> sequences;
    private List<List<Integer>> notSequences;
    private List<List<Integer>> segments;

    private List<Integer> forks;
    private List<Integer> joins;

    public Responsibilities(int[][] pre, int[][] post, List<List<Integer>> tInvariants, 
                            Set<Integer> actionPlaces) {
        this.actionPlaces = actionPlaces;
        this.tInvariants = tInvariants;
        this.pre = pre;
        this.post = post;

        forks = new ArrayList<>();
        joins = new ArrayList<>();
        segments = new ArrayList<>();
        sequences = new ArrayList<>();
        notSequences = new ArrayList<>();

        isSequential();
        analyzeForkOrJoin();
        segmentInvariants();
    }

    private void isSequential() {
        // Para cada TI, verificar si comparte transiciones con otros TI
        for (int i = 0; i < tInvariants.size(); i++) {
            List<Integer> currentTI = tInvariants.get(i);
            boolean sharesTransitions = false;
            
            // Comparar con todas lOs demás TI
            for (int j = 0; j < tInvariants.size(); j++) {
                if (i == j) continue; // Saltar la misma fila
                
                List<Integer> otherTI = tInvariants.get(j);
                
                // Verificar si comparten alguna transición (posiciones con valores > 0)
                if (hasCommonTransitions(currentTI, otherTI)) {
                    sharesTransitions = true;
                    break;
                }
            }
            
            // Si este TI no comparte transiciones, es lineal
            if (!sharesTransitions) {
                sequences.add(currentTI);
            } else {
                notSequences.add(currentTI);
            }
        }
    }

    // Método auxiliar para verificar si dos TI comparten transiciones
    private boolean hasCommonTransitions(List<Integer> it1, List<Integer> it2) {
        // Buscar posiciones donde ambos TI tienen valores > 0
        for (int i = 0; i < it1.size(); i++) {
            if (it1.get(i) > 0 && it2.get(i) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isFork(int place) {
        // Un fork ocurre cuando un lugar tiene múltiples transiciones de salida
        // Revisamos la matriz POST: si el lugar produce tokens en más de una transición
        
        int outputTransitions = 0;
        
        // Recorrer todas las transiciones (columnas) para este lugar (fila)
        for (int transition = 0; transition < pre[place].length; transition++) {
            if (pre[place][transition] > 0) {
                outputTransitions++;
            }
        }
        
        return outputTransitions > 1;
    }

    private boolean isJoin(int place) {
        // Un join ocurre cuando un lugar tiene múltiples transiciones de entrada
        // Revisamos la matriz PRE: si el lugar consume tokens de más de una transición
        
        int inputTransitions = 0;
        
        // Recorrer todas las transiciones (columnas) para este lugar (fila)
        for (int transition = 0; transition < post[place].length; transition++) {
            if (post[place][transition] > 0) {
                inputTransitions++;
            }
        }
        
        return inputTransitions > 1;
    }

    private void analyzeForkOrJoin () {
        for(Integer p : actionPlaces){
            if(isJoin(p)) joins.add(p);
            if(isFork(p)) forks.add(p);
        }
    }

    private void segmentInvariants() {
        Set<List<Integer>> uniqueSegments = new HashSet<>();
        
        // Primero, convertir sequences a formato de Ã­ndices y agregarlas como segmentos
        for (List<Integer> seq : sequences) {
            List<Integer> sequenceIndices = new ArrayList<>();
            for (int idx = 0; idx < seq.size(); idx++) {
                if (seq.get(idx) > 0) {
                    sequenceIndices.add(idx);
                }
            }
            if (!sequenceIndices.isEmpty()) {
                uniqueSegments.add(sequenceIndices);
            }
        }
        
        // Ahora procesar notSequences
        for (List<Integer> sublist : notSequences) {
            // Primero, extraer los Ã­ndices de las transiciones activas (valor > 0)
            List<Integer> activeTransitions = new ArrayList<>();
            for (int idx = 0; idx < sublist.size(); idx++) {
                if (sublist.get(idx) > 0) {
                    activeTransitions.add(idx);
                }
            }
            
            // Ahora segmentar estas transiciones activas
            List<Integer> newSegment = new ArrayList<>();
            
            for (int t : activeTransitions) {
                // Agregar la transiciÃ³n actual al segmento
                newSegment.add(t);
                
                boolean isCutPoint = false;
                
                // Verificar si esta transiciÃ³n toca un fork o un join
                for (int p = 0; p < pre.length; p++) {
                    // Si produce hacia un fork O consume desde un join â†' punto de corte
                    if ((post[p][t] > 0 && forks.contains(p)) ||
                        (post[p][t] > 0 && joins.contains(p))) {
                        isCutPoint = true;
                        break;
                    }
                }
                
                // Si encontramos un punto de corte, guardamos el segmento y empezamos uno nuevo
                if (isCutPoint) {
                    uniqueSegments.add(new ArrayList<>(newSegment));
                    newSegment.clear(); // Empezar segmento nuevo VACÃO
                }
            }
            
            // Guardar el Ãºltimo segmento si quedÃ³ algo pendiente
            if (!newSegment.isEmpty()) {
                uniqueSegments.add(new ArrayList<>(newSegment));
            }
        }
        
        // Convertir el Set a List para almacenar en segments
        segments.addAll(uniqueSegments);
    }

    public void printAnalysis() {
        System.out.println("=================================");
        System.out.println("THREADS RESPONSIBILITY");
        System.out.println("=================================");
        for (int i = 0; i < segments.size(); i++) {
            // Formatear cada segmento con "T" antes de cada número
            List<String> formattedSegment = new ArrayList<>();
            for (Integer t : segments.get(i)) {
                formattedSegment.add("T" + t);
            }
            System.out.println("Segment " + (i + 1) + ": " + formattedSegment);
        }
        
        System.out.println("\n=================================");
        System.out.println("FORKS");
        if (forks.isEmpty()) {
            System.out.println("No forks found.");
        } else {
            // Formatear forks con "P" antes de cada número
            List<String> formattedForks = new ArrayList<>();
            for (Integer p : forks) {
                formattedForks.add("P" + p);
            }
            System.out.println("Fork places: " + formattedForks);
        }
        
        System.out.println("\n=================================");
        System.out.println("JOINS");
        if (joins.isEmpty()) {
            System.out.println("No joins found.");
        } else {
            // Formatear joins con "P" antes de cada número
            List<String> formattedJoins = new ArrayList<>();
            for (Integer p : joins) {
                formattedJoins.add("P" + p);
            }
            System.out.println("Join places: " + formattedJoins);
        }
    }
}