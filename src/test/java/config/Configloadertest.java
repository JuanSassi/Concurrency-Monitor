import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigLoader.
 *
 * These tests use the real config.properties (petrinet.number=1 → travelAgencySystem)
 * located in src/test/resources/ — same files as production, values hardcoded
 * from the actual .properties so tests are deterministic.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("ConfigLoader")
class ConfigLoaderTest {

    // ── execution ────────────────────────────────────────────

    @Test
    @DisplayName("getMaxInvariants → 186")
    void testGetMaxInvariants() {
        assertEquals(186, ConfigLoader.getMaxInvariants());
    }

    // ── policies ─────────────────────────────────────────────

    @Test
    @DisplayName("isStandardPolicies → true")
    void testIsStandardPolicies() {
        assertTrue(ConfigLoader.isStandardPolicies());
    }

    @Test
    @DisplayName("getStandardPolicies → 0.5")
    void testGetStandardPolicies() {
        assertEquals(0.5, ConfigLoader.getStandardPolicies(), 1e-9);
    }

    @Test
    @DisplayName("getTotalPolicies → 2 (policies.value.1 y policies.value.2)")
    void testGetTotalPolicies() {
        assertEquals(2, ConfigLoader.getTotalPolicies());
    }

    @Test
    @DisplayName("getValuePolicies(1) → 0.8")
    void testGetValuePolicies1() {
        assertEquals(0.8, ConfigLoader.getValuePolicies(1), 1e-9);
    }

    @Test
    @DisplayName("getValuePolicies(2) → 0.75")
    void testGetValuePolicies2() {
        assertEquals(0.75, ConfigLoader.getValuePolicies(2), 1e-9);
    }

    @Test
    @DisplayName("getValuePolicies con índice 0 lanza RuntimeException")
    void testGetValuePoliciesInvalidLow() {
        assertThrows(RuntimeException.class, () -> ConfigLoader.getValuePolicies(0));
    }

    @Test
    @DisplayName("getValuePolicies con índice mayor al total lanza RuntimeException")
    void testGetValuePoliciesInvalidHigh() {
        int total = ConfigLoader.getTotalPolicies();
        assertThrows(RuntimeException.class, () -> ConfigLoader.getValuePolicies(total + 1));
    }

    // ── petrinet selection ───────────────────────────────────

    @Test
    @DisplayName("getPetrinetNumber → 1")
    void testGetPetrinetNumber() {
        assertEquals(0, ConfigLoader.getPetrinetNumber());
    }

    @Test
    @DisplayName("getPetrinetFile → travelAgencySystem.properties")
    void testGetPetrinetFile() {
        assertEquals("exampleHuang.properties", ConfigLoader.getPetrinetFile());
    }

    // ── tree ─────────────────────────────────────────────────

    @Test
    @DisplayName("getFullprint → true")
    void testGetFullprint() {
        assertTrue(ConfigLoader.getFullprint());
    }
}