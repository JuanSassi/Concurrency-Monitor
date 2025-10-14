import java.util.*;

class MatrixUtils {
    /**
     * Clase interna para representar nÃºmeros racionales con precisiÃ³n exacta.
     * Utiliza aritmÃ©tica de enteros largos para evitar errores de redondeo.
     */
    static class Rational {
        long num, den;

        /**
         * Constructor que crea un racional y lo reduce a su forma canÃ³nica.
         * 
         * @param n Numerador
         * @param d Denominador (no puede ser cero)
         * @throws ArithmeticException si el denominador es cero
         */
        Rational(long n, long d) {
            if (d == 0) throw new ArithmeticException("Denominator zero");
            long g = gcd(Math.abs(n), Math.abs(d));
            n /= g;
            d /= g;
            // Mantener el signo en el numerador
            if (d < 0) {
                n = -n;
                d = -d;
            }
            this.num = n;
            this.den = d;
        }

        /**
         * Constructor para crear un racional a partir de un entero.
         * @param n Valor entero
         */
        Rational(long n) {
            this(n, 1);
        }

        /**
         * Suma este racional con otro.
         * @param r Racional a sumar
         * @return Nuevo racional con el resultado
         */
        public Rational add(Rational r) {
            return new Rational(this.num * r.den + r.num * this.den,
                                this.den * r.den);
        }

        /**
         * Resta otro racional de este.
         * @param r Racional a restar
         * @return Nuevo racional con el resultado
         */
        public Rational sub(Rational r) {
            return new Rational(this.num * r.den - r.num * this.den,
                                this.den * r.den);
        }

        /**
         * Multiplica este racional por otro.
         * @param r Racional a multiplicar
         * @return Nuevo racional con el resultado
         */
        public Rational mul(Rational r) {
            return new Rational(this.num * r.num, this.den * r.den);
        }

        /**
         * Retorna el negativo de este racional.
         * @return -this
         */
        public Rational negate() {
            return new Rational(-this.num, this.den);
        }

        /**
         * Verifica si este racional es cero.
         * @return true si el numerador es cero
         */
        public boolean isZero() {
            return num == 0;
        }

        /**
         * Calcula el mÃ¡ximo comÃºn divisor usando el algoritmo de Euclides
         * @param a Primer nÃºmero
         * @param b Segundo nÃºmero
         * @return MCD(a, b)
         */
        private static long gcd(long a, long b) {
            while (b != 0) {
                long t = b;
                b = a % b;
                a = t;
            }
            return a;
        }
    }

    /**
     * Calcula el nullspace (nucleo) de la matriz W usando eliminaciÃ³n gaussiana.
     * El nullspace contiene todos los vectores x tales que W*x = 0.
     * 
     * El algoritmo:
     * 1. Convierte W a forma escalonada reducida por filas usando aritmÃ©tica racional
     * 2. Identifica las variables libres (columnas sin pivote)
     * 3. Para cada variable libre, construye un vector base del nullspace
     * 4. Convierte los vectores racionales a enteros multiplicando por el MCM
     * 5. Reduce cada vector dividiendo por su MCD
     * 
     * @return Lista de vectores base del nullspace (base entera minimal)
     */
    public static List<int[]> computeNullspace(int[][] W) {
        int m = W.length, n = W[0].length;
        Rational[][] A = new Rational[m][n];
        
        // Convertir matriz de enteros a racionales
        for (int i=0;i<m;i++) 
            for (int j=0;j<n;j++) 
                A[i][j] = new Rational(W[i][j]);

        int rank = 0;
        int[] pivots = new int[n]; 
        Arrays.fill(pivots, -1);

        // EliminaciÃ³n gaussiana: reducir a forma escalonada
        for (int col=0,row=0; col<n && row<m; col++) {
            // Buscar pivote no cero en la columna actual
            int sel = -1;
            for (int i=row;i<m;i++) 
                if (!A[i][col].isZero()) { 
                    sel=i; 
                    break; 
                }
            if (sel==-1) continue; // Columna toda ceros, es variable libre
            
            // Intercambiar filas para poner el pivote en la posiciÃ³n correcta
            Rational[] tmp=A[row]; A[row]=A[sel]; A[sel]=tmp;

            // Normalizar la fila del pivote (hacer el pivote = 1)
            Rational inv = new Rational(A[row][col].den, A[row][col].num);
            for (int j=col;j<n;j++) 
                A[row][j]=A[row][j].mul(inv);

            // Eliminar la columna en todas las demÃ¡s filas
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

        // Identificar variables libres (columnas sin pivote)
        List<Integer> freeVars=new ArrayList<>();
        for (int j=0;j<n;j++) 
            if (pivots[j]==-1) 
                freeVars.add(j);

        // Construir base del nullspace
        List<int[]> basis=new ArrayList<>();
        for (int free:freeVars) {
            // Crear vector con 1 en la variable libre y resolver para las demÃ¡s
            Rational[] vec=new Rational[n]; 
            for(int j=0;j<n;j++) vec[j]=new Rational(0);
            vec[free]=new Rational(1);
            
            // Para cada variable dependiente, calcular su valor
            for (int j=0;j<n;j++) {
                if (pivots[j]!=-1) {
                    Rational sum=new Rational(0);
                    for (int f:freeVars) 
                        sum=sum.add(A[pivots[j]][f].mul(vec[f]));
                    vec[j]=sum.negate();
                }
            }

            // Convertir vector racional a entero
            long lcm = 1;
            for (Rational r:vec) {
                long d = r.den;
                lcm = lcm(lcm, d);
            }

            int[] intVec=new int[n];
            for (int j=0;j<n;j++) 
                intVec[j]=(int)(vec[j].num * (lcm/vec[j].den));
            
            // Reducir vector dividiendo por MCD
            int g=gcdArray(intVec);
            if (g!=0) 
                for(int j=0;j<n;j++) 
                    intVec[j]/=g;
            
            basis.add(intVec);
        }
        return basis;
    }

    /**
     * Calcula el mÃ­nimo comÃºn mÃºltiplo de dos nÃºmeros.
     * @param a Primer nÃºmero
     * @param b Segundo nÃºmero
     * @return MCM(a, b)
     */
    public static long lcm(long a,long b){ 
        return a/gcd(a,b)*b; 
    }
    
    /**
     * Calcula el mÃ¡ximo comÃºn divisor de dos nÃºmeros.
     * @param a Primer nÃºmero
     * @param b Segundo nÃºmero
     * @return MCD(a, b)
     */
    public static long gcd(long a,long b){ 
        return b==0?a:gcd(b,a%b); 
    }

    /**
     * Calcula el MCD de todos los elementos de un array.
     * @param arr Array de enteros
     * @return MCD de todos los elementos
     */
    public static int gcdArray(int[] arr){
        int g = 0;
        for (int x : arr) g = gcd(g, Math.abs(x));
        return g;
    }

    /**
     * Calcula el MCD de dos enteros.
     * @param a Primer nÃºmero
     * @param b Segundo nÃºmero
     * @return MCD(a, b)
     */
    public static int gcd(int a, int b){
        return b == 0 ? a : gcd(b, a % b);
    }

    /**
     * Resta dos matrices elemento por elemento.
     * 
     * @param A Primera matriz
     * @param B Segunda matriz (debe tener las mismas dimensiones que A)
     * @return Matriz C = A - B
     */
    public static int[][] subtract(int[][] A, int[][] B) {
        int rows = A.length, cols = A[0].length;
        int[][] C = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                C[i][j] = A[i][j] - B[i][j];
        return C;
    }
}