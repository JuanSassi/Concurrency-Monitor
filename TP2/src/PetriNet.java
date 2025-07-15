public class PetriNet {
    private static PetriNet petriNet = new PetriNet();
    private final int client = 5;
    private final int door = 1;
    private final int attendees = 5;
    private final int manager1 = 1;
    private final int manager2 = 1;
    private final int agent = 1;
    private final int[][] initialMarking = {
            {client},
            {door},
            {0},
            {0},
            {attendees},
            {0},
            {manager1},
            {manager2},
            {0},
            {0},
            {agent},
            {0},
            {0},
            {0},
            {0}
    };
    private final int[][] inputIncidenceMatrix = {
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };
    private final int[][] outputIncidenceMatrix = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0}
    };
    private final int numP = inputIncidenceMatrix.length;
    private final int numT = inputIncidenceMatrix[0].length;
    private int[][] currentMarking = initialMarking;

    private PetriNet() {
    }

    public static PetriNet getInstance() {
        return petriNet;
    }

    public int getNumP(){
        return numP;
    }

    public int[][] getInitialMarking(){
        return initialMarking;
    }

    public void inputFiring(int transition){

        // Vector característico de la secuencia de disparos
        int[][] firingCountVector = new int[numT][1];
        firingCountVector[transition][0] = 1;

        // Cambio neto en las marcas
        int[][] netMarkingChange = matrixByVector(inputIncidenceMatrix,firingCountVector);
        for (int i = 0; i < numP; i++) {
            currentMarking[i][0] -= netMarkingChange[i][0];
        }
    }

    public void outputFiring(int transition){

        // Vector característico de la secuencia de disparos
        int[][] firingCountVector = new int[numT][1];
        firingCountVector[transition][0] = 1;

        // Cambio neto en las marcas
        int[][] netMarkingChange = matrixByVector(outputIncidenceMatrix,firingCountVector);
        for (int i = 0; i < numP; i++) {
            currentMarking[i][0] += netMarkingChange[i][0];
        }
    }

    private int[][] matrixByVector(int[][] matrix, int[][] vector){
        int[][] resultingVector = new int[numP][1];
        for (int i = 0; i < numP; i++) {
            for (int j = 0; j < numT; j++) {
                resultingVector[i][0] += matrix[i][j] * vector[j][0];
            }
        }
        return resultingVector;
    }

    public boolean isEnabled(int transition) {
        for (int i = 0; i < numP; i++) {
            if (inputIncidenceMatrix[i][transition] > 0 && currentMarking[i][0] < inputIncidenceMatrix[i][transition]) {
                return false;
            }
        }
        return true;
    }
}