import java.util.*;

/**
 * Utility class for computing the nullspace (kernel) of integer matrices.
 * The nullspace of a matrix W consists of all vectors x such that W*x = 0.
 * This class uses Gaussian elimination with exact rational arithmetic to compute
 * a minimal integer basis for the nullspace.
 * 
 * <p>The algorithm guarantees exact results by using rational number arithmetic
 * internally and converting the final basis vectors to reduced integer form.</p>
 * 
 * @author Sassi Juan Ignacio
 */
class Nullspace {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Nullspace() {
        // Utility class, no instances allowed
    }

    /**
     * Inner class representing rational numbers with exact precision.
     * Uses long integer arithmetic to avoid rounding errors that would occur
     * with floating-point arithmetic. All rationals are automatically reduced
     * to canonical form (lowest terms with positive denominator).
     * 
     * <p>This class is essential for exact Gaussian elimination, as it prevents
     * the accumulation of floating-point errors that would compromise the
     * accuracy of the nullspace computation.</p>
     */
    static class Rational {
        /** The numerator of the rational number */
        long num;
        
        /** The denominator of the rational number (always positive after construction) */
        long den;

        /**
         * Constructs a rational number and reduces it to canonical form.
         * The rational is reduced to lowest terms using GCD, and the sign
         * is normalized to be in the numerator.
         * 
         * @param n the numerator
         * @param d the denominator (must not be zero)
         * @throws ArithmeticException if the denominator is zero
         */
        Rational(long n, long d) {
            if (d == 0) throw new ArithmeticException("Denominator zero");
            long g = gcd(Math.abs(n), Math.abs(d));
            n /= g;
            d /= g;
            // Keep the sign in the numerator
            if (d < 0) {
                n = -n;
                d = -d;
            }
            this.num = n;
            this.den = d;
        }

        /**
         * Constructs a rational number from an integer.
         * Equivalent to creating the rational n/1.
         * 
         * @param n the integer value
         */
        Rational(long n) {
            this(n, 1);
        }

        /**
         * Adds this rational to another rational.
         * Computes this + r using the formula: (a/b) + (c/d) = (ad + bc)/(bd)
         * 
         * @param r the rational to add
         * @return a new rational representing the sum
         */
        public Rational add(Rational r) {
            return new Rational(this.num * r.den + r.num * this.den,
                                this.den * r.den);
        }

        /**
         * Subtracts another rational from this rational.
         * Computes this - r using the formula: (a/b) - (c/d) = (ad - bc)/(bd)
         * 
         * @param r the rational to subtract
         * @return a new rational representing the difference
         */
        public Rational sub(Rational r) {
            return new Rational(this.num * r.den - r.num * this.den,
                                this.den * r.den);
        }

        /**
         * Multiplies this rational by another rational.
         * Computes this * r using the formula: (a/b) * (c/d) = (ac)/(bd)
         * 
         * @param r the rational to multiply by
         * @return a new rational representing the product
         */
        public Rational mul(Rational r) {
            return new Rational(this.num * r.num, this.den * r.den);
        }

        /**
         * Returns the negation of this rational.
         * Computes -this by negating the numerator.
         * 
         * @return a new rational representing -this
         */
        public Rational negate() {
            return new Rational(-this.num, this.den);
        }

        /**
         * Checks if this rational is zero.
         * A rational is zero if and only if its numerator is zero.
         * 
         * @return true if this rational equals zero, false otherwise
         */
        public boolean isZero() {
            return num == 0;
        }

        /**
         * Computes the greatest common divisor using Euclid's algorithm.
         * Uses the recursive formula: gcd(a, b) = gcd(b, a mod b) with base case gcd(a, 0) = a.
         * 
         * @param a the first number (non-negative)
         * @param b the second number (non-negative)
         * @return the GCD of a and b
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
     * Computes the nullspace (kernel) of matrix W using Gaussian elimination.
     * The nullspace contains all vectors x such that W*x = 0.
     * 
     * <p>Algorithm steps:</p>
     * <ol>
     *   <li>Convert W to reduced row echelon form using rational arithmetic</li>
     *   <li>Identify free variables (columns without pivots)</li>
     *   <li>For each free variable, construct a basis vector of the nullspace</li>
     *   <li>Convert rational vectors to integers by multiplying by LCM of denominators</li>
     *   <li>Reduce each vector by dividing by its GCD</li>
     * </ol>
     * 
     * @param W the input matrix (m x n)
     * @return a list of basis vectors for the nullspace, each as an integer array of length n.
     *         Returns an empty list if the nullspace is trivial (only the zero vector).
     */
    public static List<int[]> compute(int[][] W) {
        int m = W.length, n = W[0].length;
        Rational[][] A = new Rational[m][n];
        
        // Convert integer matrix to rational matrix
        for (int i=0;i<m;i++) 
            for (int j=0;j<n;j++) 
                A[i][j] = new Rational(W[i][j]);

        int rank = 0;
        int[] pivots = new int[n]; 
        Arrays.fill(pivots, -1);

        // Gaussian elimination: reduce to row echelon form
        for (int col=0,row=0; col<n && row<m; col++) {
            // Find non-zero pivot in current column
            int sel = -1;
            for (int i=row;i<m;i++) 
                if (!A[i][col].isZero()) { 
                    sel=i; 
                    break; 
                }
            if (sel==-1) continue; // All zeros in column, it's a free variable
            
            // Swap rows to put pivot in correct position
            Rational[] tmp=A[row]; A[row]=A[sel]; A[sel]=tmp;

            // Normalize pivot row (make pivot = 1)
            Rational inv = new Rational(A[row][col].den, A[row][col].num);
            for (int j=col;j<n;j++) 
                A[row][j]=A[row][j].mul(inv);

            // Eliminate column in all other rows
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

        // Identify free variables (columns without pivots)
        List<Integer> freeVars=new ArrayList<>();
        for (int j=0;j<n;j++) 
            if (pivots[j]==-1) 
                freeVars.add(j);

        // Build nullspace basis
        List<int[]> basis=new ArrayList<>();
        for (int free:freeVars) {
            // Create vector with 1 in free variable and solve for others
            Rational[] vec=new Rational[n]; 
            for(int j=0;j<n;j++) vec[j]=new Rational(0);
            vec[free]=new Rational(1);
            
            // For each dependent variable, compute its value
            for (int j=0;j<n;j++) {
                if (pivots[j]!=-1) {
                    Rational sum=new Rational(0);
                    for (int f:freeVars) 
                        sum=sum.add(A[pivots[j]][f].mul(vec[f]));
                    vec[j]=sum.negate();
                }
            }

            // Convert rational vector to integer vector
            long lcm = 1;
            for (Rational r:vec) {
                long d = r.den;
                lcm = lcm(lcm, d);
            }

            int[] intVec=new int[n];
            for (int j=0;j<n;j++) 
                intVec[j]=(int)(vec[j].num * (lcm/vec[j].den));
            
            // Reduce vector by dividing by GCD
            int g=gcdArray(intVec);
            if (g!=0) 
                for(int j=0;j<n;j++) 
                    intVec[j]/=g;
            
            basis.add(intVec);
        }
        return basis;
    }

    /**
     * Computes the least common multiple of two numbers.
     * Uses the formula: lcm(a, b) = (a * b) / gcd(a, b)
     * 
     * @param a the first number
     * @param b the second number
     * @return the LCM of a and b
     */
    public static long lcm(long a,long b){ 
        return a/gcd(a,b)*b; 
    }
    
    /**
     * Computes the greatest common divisor of two numbers using Euclid's algorithm.
     * 
     * @param a the first number
     * @param b the second number
     * @return the GCD of a and b
     */
    public static long gcd(long a,long b){ 
        return b==0?a:gcd(b,a%b); 
    }

    /**
     * Computes the GCD of all elements in an array.
     * The result is the largest integer that divides all array elements.
     * 
     * @param arr the array of integers
     * @return the GCD of all elements in the array
     */
    public static int gcdArray(int[] arr){
        int g = 0;
        for (int x : arr) g = gcd(g, Math.abs(x));
        return g;
    }

    /**
     * Computes the greatest common divisor of two integers using Euclid's algorithm.
     * 
     * @param a the first integer
     * @param b the second integer
     * @return the GCD of a and b
     */
    public static int gcd(int a, int b){
        return b == 0 ? a : gcd(b, a % b);
    }
}