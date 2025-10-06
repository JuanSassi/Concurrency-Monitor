import java.util.*;

/**
 * Calcula P-invariantes o T-invariantes de una Red de Petri.
 * 
 * - T-invariantes: vectores y no negativos tal que W*y = 0 (secuencias de disparo que retornan al marcado inicial)
 * - P-invariantes: vectores x no negativos tal que x^T*W = 0 o equivalentemente W^T*x = 0 (componentes conservativas)
 * Donde W es la matriz de incidencia de la red (Post - Pre)
 * 
 * @author Juan Ignacio Sassi
 */
public class Invariants {
    private final int[][] W;
    private final boolean isPInvariant;

    /**
     * Constructor que inicializa el calculador de invariantes.
     * 
     * @param W Matriz de incidencia (Post - Pre) de la Red de Petri
     * @param isPInvariant true para calcular P-invariantes, false para T-invariantes
     */
    public Invariants(int[][] W, boolean isPInvariant) {
        this.isPInvariant = isPInvariant;
        
        // Para P-invariantes necesitamos W^T (transpuesta)
        if(isPInvariant){
            int rows = W.length;
            int cols = W[0].length;
            int[][] Wt = new int[cols][rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    Wt[j][i] = W[i][j];
                }
            }
            this.W = Wt;
        } else {
            this.W = W;
        }
    }

    /**
     * Calcula e imprime todos los invariantes minimales.
     * 
     * El proceso:
     * 1. Calcula la base del nullspace de W (o W^T para P-invariantes)
     * 2. Genera todas las combinaciones lineales no negativas de los vectores base
     * 3. Filtra solo los invariantes minimales (sin componentes redundantes)
     * 4. Ordena e imprime los resultados
     */
    public void computeInvariants() {
        List<int[]> nullBasis=MatrixUtils.computeNullspace(this.W);
        System.out.println("\nBase del nullspace (" + nullBasis.size() + " vectores):");
        for (int i = 0; i < nullBasis.size(); i++) {
            System.out.println("v" + (i + 1) + " = " + Arrays.toString(nullBasis.get(i)));
        }

        // LÃ­mite para cada coeficiente en las combinaciones lineales
        int maxCoeff = 4 ;
        
        Set<List<Integer>> all = new HashSet<>();
        
        // Generar todas las combinaciones lineales no negativas
        generateCombinations(nullBasis, new int[nullBasis.size()], 0, maxCoeff, all);
        
        if (all.isEmpty()) {
            System.out.println("\nWARNING: No invariants found.");
        }

        // Filtrar solo los invariantes minimales
        List<List<Integer>> minimal=new ArrayList<>();
        for(List<Integer> inv:all) 
            if(isMinimal(inv,all)) 
                minimal.add(inv);

        // Ordenar por suma de componentes (invariantes mÃ¡s simples primero)
        minimal.sort(Comparator.comparingInt(v->v.stream().mapToInt(Integer::intValue).sum()));

        System.out.println("\n======================================================================");
        System.out.println((isPInvariant ? "P" : "T") + "- (minimal invariants found): " + minimal.size());
        System.out.println("======================================================================\n");

        for(int i=0;i<minimal.size();i++) {
            String prefix = isPInvariant ? "x" : "y";
            System.out.println(prefix+(i+1)+" = "+minimal.get(i));
        }
    }

    /**
     * Genera recursivamente todas las combinaciones lineales no negativas de los vectores base.
     * 
     * Para cada vector base, prueba coeficientes de -maxCoeff a +maxCoeff.
     * Solo agrega combinaciones que resulten en vectores completamente no negativos.
     * 
     * @param basis Vectores base del nullspace
     * @param coeffs Array de coeficientes (se va llenando recursivamente)
     * @param idx Ãndice actual en la recursiÃ³n
     * @param maxCoeff Valor mÃ¡ximo absoluto para cada coeficiente
     * @param set Conjunto donde se almacenan las combinaciones vÃ¡lidas
     */
    private void generateCombinations(List<int[]> basis, int[] coeffs, int idx, int maxCoeff, Set<List<Integer>> set) {
        if (idx == basis.size()) {
            // Caso base: se han asignado todos los coeficientes
            
            int n = basis.get(0).length;
            int[] combo = new int[n];
            boolean allZero = true;
            
            // Calcular la combinaciÃ³n lineal: combo = Î£(coeffs[i] * basis[i])
            for (int i = 0; i < basis.size(); i++) {
                if (coeffs[i] != 0) allZero = false;
                for (int j = 0; j < n; j++) {
                    combo[j] += coeffs[i] * basis.get(i)[j];
                }
            }
            
            if (allZero) return; // Ignorar el vector cero trivial
            
            // Verificar que sea no negativo y tenga al menos un componente positivo
            boolean nonNeg = true, anyPos = false;
            for (int v : combo) {
                if (v < 0) {
                    nonNeg = false;
                    break;
                }
                if (v > 0) anyPos = true;
            }
            
            if (nonNeg && anyPos) {
                // Reducir el vector a su forma minimal dividiendo por el MCD
                int g = MatrixUtils.gcdArray(combo);
                if (g > 0) {
                    for (int j = 0; j < combo.length; j++) {
                        combo[j] /= g;
                    }
                }
                
                // Agregar al conjunto (duplicados se eliminan automÃ¡ticamente)
                List<Integer> lst = new ArrayList<>();
                for (int v : combo) lst.add(v);
                set.add(lst);
            }
            return;
        }
        
        // Caso recursivo: probar todos los coeficientes posibles para el vector actual
        for (int c = -maxCoeff; c <= maxCoeff; c++) {
            coeffs[idx] = c;
            generateCombinations(basis, coeffs, idx + 1, maxCoeff, set);
        }
    }

    /**
     * Verifica si un invariante es minimal.
     * 
     * Un invariante es minimal si no existe otro invariante cuyo soporte
     * (conjunto de Ã­ndices con valores positivos) sea un subconjunto propio del suyo.
     * 
     * Ejemplo: Si x1 = [1,1,0] y x2 = [1,0,0], entonces x1 NO es minimal porque
     * el soporte de x2 {0} estÃ¡ estrictamente contenido en el soporte de x1 {0,1}.
     * 
     * @param inv Invariante a verificar
     * @param all Conjunto de todos los invariantes encontrados
     * @return true si el invariante es minimal, false en caso contrario
     */
    private boolean isMinimal(List<Integer> inv, Set<List<Integer>> all) {
        // Calcular el soporte del invariante actual
        Set<Integer> supp = new HashSet<>();
        for (int i = 0; i < inv.size(); i++) 
            if (inv.get(i) > 0) 
                supp.add(i);
        
        // Verificar contra todos los demÃ¡s invariantes
        for (List<Integer> other : all) {
            if (other.equals(inv)) continue;
            
            Set<Integer> supp2 = new HashSet<>();
            for (int i = 0; i < other.size(); i++) 
                if (other.get(i) > 0) 
                    supp2.add(i);
            
            // Si otro invariante tiene soporte estrictamente contenido,
            // entonces este NO es minimal
            if (supp.containsAll(supp2) && !supp.equals(supp2)) 
                return false;
        }
        return true;
    }
}