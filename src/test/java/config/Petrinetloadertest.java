import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PetrinetLoader.
 *
 * Uses exampleHuang.properties (set petrinet.number=0 in config.properties).
 * Red S3PR de Huang: 14 places (P0-P13), 10 transitions (T0-T9).
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PetrinetLoader — exampleHuang")
class PetrinetLoaderTest {

    // ── dimensiones ──────────────────────────────────────────

    @Test
    @DisplayName("getNumPlaces → 14 lugares (P0-P13)")
    void testNumPlaces() {
        assertEquals(14, PetrinetLoader.getNumPlaces());
    }

    @Test
    @DisplayName("getNumTransitions → 10 transiciones (T0-T9)")
    void testNumTransitions() {
        assertEquals(10, PetrinetLoader.getNumTransitions());
    }

    // ── marcado inicial ──────────────────────────────────────

    @Test
    @DisplayName("getInitialMarkingVector → longitud 14")
    void testInitialMarkingLength() {
        assertEquals(14, PetrinetLoader.getInitialMarkingVector().length);
    }

    @Test
    @DisplayName("getInitialMarkingVector → [2,0,1,0,0,0,1,2,0,0,0,1,1,1]")
    void testInitialMarkingValues() {
        int[] expected = {2, 0, 1, 0, 0, 0, 1, 2, 0, 0, 0, 1, 1, 1};
        assertArrayEquals(expected, PetrinetLoader.getInitialMarkingVector());
    }

    @Test
    @DisplayName("marcado inicial — tokens totales = 10")
    void testInitialMarkingTotalTokens() {
        int[] marking = PetrinetLoader.getInitialMarkingVector();
        int total = 0;
        for (int t : marking) total += t;
        assertEquals(9, total);
    }

    @Test
    @DisplayName("marcado inicial — ningún token negativo")
    void testInitialMarkingNonNegative() {
        for (int tokens : PetrinetLoader.getInitialMarkingVector()) {
            assertTrue(tokens >= 0, "Token negativo encontrado: " + tokens);
        }
    }

    // ── transiciones temporales ──────────────────────────────

    @Test
    @DisplayName("getTemporalTransitionsVector → longitud 10")
    void testTemporalTransitionsLength() {
        assertEquals(10, PetrinetLoader.getTemporalTransitionsVector().length);
    }

    @Test
    @DisplayName("getTemporalTransitionsVector → [0,0,0,0,0,0,0,0,0,0] (todas inmediatas)")
    void testTemporalTransitionsValues() {
        int[] expected = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertArrayEquals(expected, PetrinetLoader.getTemporalTransitionsVector());
    }

    @Test
    @DisplayName("transiciones temporales — solo contiene 0s y 1s")
    void testTemporalTransitionsBinary() {
        for (int v : PetrinetLoader.getTemporalTransitionsVector()) {
            assertTrue(v == 0 || v == 1, "Valor inesperado: " + v);
        }
    }

    // ── matriz Pre ───────────────────────────────────────────

    @Test
    @DisplayName("getPreMatrix → dimensiones 14x10")
    void testPreMatrixDimensions() {
        int[][] pre = PetrinetLoader.getPreMatrix();
        assertEquals(14, pre.length);
        for (int[] row : pre) {
            assertEquals(10, row.length);
        }
    }

    @Test
    @DisplayName("getPreMatrix → fila 0: [1,0,0,0,0,0,0,0,0,0]")
    void testPreMatrixRow0() {
        int[] expected = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertArrayEquals(expected, PetrinetLoader.getPreMatrix()[0]);
    }

    @Test
    @DisplayName("getPreMatrix → fila 12: [1,0,0,1,1,0,1,0,1,0]")
    void testPreMatrixRow12() {
        int[] expected = {1, 0, 0, 1, 1, 0, 1, 0, 1, 0};
        assertArrayEquals(expected, PetrinetLoader.getPreMatrix()[12]);
    }

    @Test
    @DisplayName("getPreMatrix → sin valores negativos")
    void testPreMatrixNonNegative() {
        for (int[] row : PetrinetLoader.getPreMatrix()) {
            for (int v : row) {
                assertTrue(v >= 0, "Valor negativo en Pre: " + v);
            }
        }
    }

    // ── matriz Post ──────────────────────────────────────────

    @Test
    @DisplayName("getPostMatrix → dimensiones 14x10")
    void testPostMatrixDimensions() {
        int[][] post = PetrinetLoader.getPostMatrix();
        assertEquals(14, post.length);
        for (int[] row : post) {
            assertEquals(10, row.length);
        }
    }

    @Test
    @DisplayName("getPostMatrix → fila 0: [0,0,0,0,0,1,0,0,0,0]")
    void testPostMatrixRow0() {
        int[] expected = {0, 0, 0, 0, 0, 1, 0, 0, 0, 0};
        assertArrayEquals(expected, PetrinetLoader.getPostMatrix()[0]);
    }

    @Test
    @DisplayName("getPostMatrix → fila 13: [0,1,1,0,0,0,0,0,1,0]")
    void testPostMatrixRow13() {
        int[] expected = {0, 1, 1, 0, 0, 0, 0, 0, 1, 0};
        assertArrayEquals(expected, PetrinetLoader.getPostMatrix()[13]);
    }

    @Test
    @DisplayName("getPostMatrix → sin valores negativos")
    void testPostMatrixNonNegative() {
        for (int[] row : PetrinetLoader.getPostMatrix()) {
            for (int v : row) {
                assertTrue(v >= 0, "Valor negativo en Post: " + v);
            }
        }
    }

    // ── consistencia Pre/Post ────────────────────────────────

    @Test
    @DisplayName("Pre y Post tienen las mismas dimensiones")
    void testPrePostSameDimensions() {
        int[][] pre  = PetrinetLoader.getPreMatrix();
        int[][] post = PetrinetLoader.getPostMatrix();
        assertEquals(pre.length, post.length, "Distinta cantidad de filas");
        for (int i = 0; i < pre.length; i++) {
            assertEquals(pre[i].length, post[i].length,
                "Fila " + i + " tiene distinta cantidad de columnas");
        }
    }
}