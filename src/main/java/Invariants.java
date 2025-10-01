import java.util.*;

public class Invariants {
    private final int[][] W;
    private final boolean isPInvariant;

    public Invariants(int[][] W, boolean isPInvariant) {
        this.isPInvariant = isPInvariant;
        
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

    // Resta de matrices
    public static int[][] subtract(int[][] A, int[][] B) {
        int rows = A.length, cols = A[0].length;
        int[][] C = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    class Rational {
        long num, den;

        Rational(long n, long d) {
            if (d == 0) throw new ArithmeticException("Denominator zero");
            long g = gcd(Math.abs(n), Math.abs(d));
            n /= g;
            d /= g;
            if (d < 0) {
                n = -n;
                d = -d;
            }
            this.num = n;
            this.den = d;
        }

        Rational(long n) {
            this(n, 1);
        }

        public Rational add(Rational r) {
            return new Rational(this.num * r.den + r.num * this.den,
                                this.den * r.den);
        }

        public Rational sub(Rational r) {
            return new Rational(this.num * r.den - r.num * this.den,
                                this.den * r.den);
        }

        public Rational mul(Rational r) {
            return new Rational(this.num * r.num, this.den * r.den);
        }

        public Rational div(Rational r) {
            return new Rational(this.num * r.den, this.den * r.num);
        }

        public Rational negate() {
            return new Rational(-this.num, this.den);
        }

        public boolean isZero() {
            return num == 0;
        }

        public double toDouble() {
            return (double) num / den;
        }

        public String toString() {
            if (den == 1) return Long.toString(num);
            return num + "/" + den;
        }

        private static long gcd(long a, long b) {
            while (b != 0) {
                long t = b;
                b = a % b;
                a = t;
            }
            return a;
        }
    }

    // Calcular nullspace de W
    private List<int[]> computeNullspace() {
        int m = W.length, n = W[0].length;
        Rational[][] A = new Rational[m][n];
        for (int i=0;i<m;i++) for (int j=0;j<n;j++) A[i][j] = new Rational(W[i][j]);

        int rank = 0;
        int[] pivots = new int[n]; Arrays.fill(pivots, -1);

        for (int col=0,row=0; col<n && row<m; col++) {
            int sel = -1;
            for (int i=row;i<m;i++) if (!A[i][col].isZero()) { sel=i; break; }
            if (sel==-1) continue;
            Rational[] tmp=A[row]; A[row]=A[sel]; A[sel]=tmp;

            Rational inv = new Rational(A[row][col].den, A[row][col].num);
            for (int j=col;j<n;j++) A[row][j]=A[row][j].mul(inv);

            for (int i=0;i<m;i++) {
                if (i!=row && !A[i][col].isZero()) {
                    Rational factor=A[i][col];
                    for (int j=col;j<n;j++)
                        A[i][j]=A[i][j].sub(factor.mul(A[row][j]));
                }
            }
            pivots[col]=row;
            row++; rank++;
        }

        List<Integer> freeVars=new ArrayList<>();
        for (int j=0;j<n;j++) if (pivots[j]==-1) freeVars.add(j);

        List<int[]> basis=new ArrayList<>();
        for (int free:freeVars) {
            Rational[] vec=new Rational[n]; for(int j=0;j<n;j++) vec[j]=new Rational(0);
            vec[free]=new Rational(1);
            for (int j=0;j<n;j++) if (pivots[j]!=-1) {
                Rational sum=new Rational(0);
                for (int f:freeVars) sum=sum.add(A[pivots[j]][f].mul(vec[f]));
                vec[j]=sum.negate();
            }

            long lcm = 1;
            for (Rational r:vec) {
                long d = r.den;
                lcm = lcm(lcm, d);
            }

            int[] intVec=new int[n];
            for (int j=0;j<n;j++) intVec[j]=(int)(vec[j].num * (lcm/vec[j].den));
            int g=gcdArray(intVec);
            if (g!=0) for(int j=0;j<n;j++) intVec[j]/=g;
            basis.add(intVec);
        }
        return basis;
    }

    private static long lcm(long a,long b){ return a/gcd(a,b)*b; }
    private static long gcd(long a,long b){ return b==0?a:gcd(b,a%b); }

    private int gcdArray(int[] arr){
        int g = 0;
        for (int x : arr) g = gcd(g, Math.abs(x));
        return g;
    }

    private int gcd(int a, int b){
        return b == 0 ? a : gcd(b, a % b);
    }


    // --- Algoritmo principal ---
    public void computeInvariants() {
        List<int[]> nullBasis=computeNullspace();

        // Ajustar maxSum según el tipo de invariante
        int maxCoeff = isPInvariant ? 3 : 3;  // Límite por coeficiente individual
        
        Set<List<Integer>> all = new HashSet<>();
        
        // Generar todas las combinaciones posibles
        generateCombinations(nullBasis, new int[nullBasis.size()], 0, maxCoeff, all);
        
        if (all.isEmpty()) {
            System.out.println("\n⚠ ADVERTENCIA: No se encontraron invariantes.");
        }

        List<List<Integer>> minimal=new ArrayList<>();
        for(List<Integer> inv:all) if(isMinimal(inv,all)) minimal.add(inv);

        minimal.sort(Comparator.comparingInt(v->v.stream().mapToInt(Integer::intValue).sum()));

        System.out.println("\n======================================================================");
        System.out.println((isPInvariant ? "P" : "T") + "-INVARIANTES MINIMALES: " + minimal.size());
        System.out.println("======================================================================\n");

        for(int i=0;i<minimal.size();i++) {
            String prefix = isPInvariant ? "x" : "y";
            System.out.println(prefix+(i+1)+" = "+minimal.get(i));
        }
    }

    // Nueva generación de combinaciones más simple y exhaustiva
    private void generateCombinations(List<int[]> basis, int[] coeffs, int idx, int maxCoeff, Set<List<Integer>> set) {
        if (idx == basis.size()) {
            // Calcular la combinación lineal
            int n = basis.get(0).length;
            int[] combo = new int[n];
            boolean allZero = true;
            
            for (int i = 0; i < basis.size(); i++) {
                if (coeffs[i] != 0) allZero = false;
                for (int j = 0; j < n; j++) {
                    combo[j] += coeffs[i] * basis.get(i)[j];
                }
            }
            
            if (allZero) return;
            
            // Verificar que sea no negativo y tenga al menos un valor positivo
            boolean nonNeg = true, anyPos = false;
            for (int v : combo) {
                if (v < 0) {
                    nonNeg = false;
                    break;
                }
                if (v > 0) anyPos = true;
            }
            
            if (nonNeg && anyPos) {
                // Reducir por GCD
                int g = gcdArray(combo);
                if (g > 0) {
                    for (int j = 0; j < combo.length; j++) {
                        combo[j] /= g;
                    }
                }
                
                // Agregar al conjunto
                List<Integer> lst = new ArrayList<>();
                for (int v : combo) lst.add(v);
                set.add(lst);
            }
            return;
        }
        
        // Probar todos los coeficientes de -maxCoeff a +maxCoeff
        for (int c = -maxCoeff; c <= maxCoeff; c++) {
            coeffs[idx] = c;
            generateCombinations(basis, coeffs, idx + 1, maxCoeff, set);
        }
    }

    // Verificar si es minimal
    private boolean isMinimal(List<Integer> inv, Set<List<Integer>> all) {
        Set<Integer> supp = new HashSet<>();
        for (int i = 0; i < inv.size(); i++) 
            if (inv.get(i) > 0) 
                supp.add(i);
        
        for (List<Integer> other : all) {
            if (other.equals(inv)) continue;
            
            Set<Integer> supp2 = new HashSet<>();
            for (int i = 0; i < other.size(); i++) 
                if (other.get(i) > 0) 
                    supp2.add(i);
            
            // Si otro invariante tiene soporte estrictamente contenido en este,
            // entonces este NO es minimal
            if (supp.containsAll(supp2) && !supp.equals(supp2)) 
                return false;
        }
        return true;
    }
}