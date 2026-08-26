import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PetriNetCatalog}.
 *
 * <p>El catálogo lee {@code config.properties} del classpath en su constructor, así que estos tests
 * dependen de los recursos reales del proyecto. Las aserciones son estructurales: verifican
 * invariantes que deben valer sea cual sea la red configurada, no valores concretos.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("PetriNetCatalog")
class PetriNetCatalogTest {

  /** Catálogo recreado antes de cada test, para no compartir estado entre casos. */
  private PetriNetCatalog catalog;

  @BeforeEach
  void setUp() {
    catalog = new PetriNetCatalog();
  }

  // ── list ──────────────────────────────────────────────────

  @Test
  @DisplayName("el catálogo expone al menos una red")
  void testListIsNotEmpty() {
    assertFalse(catalog.list().isEmpty());
  }

  @Test
  @DisplayName("todos los ids son numéricos y únicos")
  void testIdsAreNumericAndUnique() {
    Set<String> seen = new HashSet<>();
    for (PetriNetCatalog.CatalogEntry entry : catalog.list()) {
      assertTrue(entry.id().matches("\\d+"), "id no numérico: " + entry.id());
      assertTrue(seen.add(entry.id()), "id duplicado: " + entry.id());
    }
  }

  @Test
  @DisplayName("los ids se listan en orden numérico ascendente")
  void testListIsSortedByNumericId() {
    List<PetriNetCatalog.CatalogEntry> entries = catalog.list();
    for (int i = 1; i < entries.size(); i++) {
      int previous = Integer.parseInt(entries.get(i - 1).id());
      int current = Integer.parseInt(entries.get(i).id());
      assertTrue(previous < current, "orden incorrecto en la posición " + i);
    }
  }

  @Test
  @DisplayName("el nombre visible no incluye la extensión .properties")
  void testDisplayNameHasNoExtension() {
    for (PetriNetCatalog.CatalogEntry entry : catalog.list()) {
      assertFalse(entry.name().endsWith(".properties"), "extensión sin quitar: " + entry.name());
      assertFalse(entry.name().isEmpty(), "nombre vacío para el id " + entry.id());
    }
  }

  @Test
  @DisplayName("petrinet.number no aparece como una red del catálogo")
  void testSettingKeyIsNotAnEntry() {
    for (PetriNetCatalog.CatalogEntry entry : catalog.list()) {
      assertNotNull(entry.id());
      assertFalse("number".equals(entry.id()), "petrinet.number se coló como red");
    }
  }

  // ── getDefaultId ──────────────────────────────────────────

  @Test
  @DisplayName("el id por defecto pertenece al catálogo")
  void testDefaultIdIsInCatalog() {
    String defaultId = catalog.getDefaultId();
    boolean found = catalog.list().stream().anyMatch(e -> e.id().equals(defaultId));
    assertTrue(found, "el id por defecto no está listado: " + defaultId);
  }

  @Test
  @DisplayName("el id por defecto es estable entre llamadas")
  void testDefaultIdIsStable() {
    assertEquals(catalog.getDefaultId(), catalog.getDefaultId());
  }

  // ── load ──────────────────────────────────────────────────

  @Test
  @DisplayName("load del id por defecto devuelve una definición válida")
  void testLoadDefault() {
    PetriNetDefinition d = catalog.load(catalog.getDefaultId());
    assertNotNull(d);
    assertTrue(d.pre().length > 0);
    assertEquals(d.pre().length, d.post().length);
    assertEquals(d.pre().length, d.initialMarking().length);
    assertEquals(d.pre()[0].length, d.temporalTransitions().length);
  }

  @Test
  @DisplayName("todas las redes del catálogo se cargan sin errores")
  void testEveryEntryLoads() {
    for (PetriNetCatalog.CatalogEntry entry : catalog.list()) {
      PetriNetDefinition d = catalog.load(entry.id());
      assertNotNull(d, "no cargó la red " + entry.id());
      assertEquals(
          d.pre().length, d.initialMarking().length, "marcado inconsistente en " + entry.name());
    }
  }

  @Test
  @DisplayName("dos load del mismo id devuelven definiciones independientes")
  void testLoadReturnsIndependentInstances() {
    String id = catalog.getDefaultId();
    PetriNetDefinition first = catalog.load(id);
    PetriNetDefinition second = catalog.load(id);

    assertNotSame(first, second);
    assertArrayEquals(first.pre(), second.pre(), "mismo contenido");

    first.pre()[0][0] = 12345;
    assertNotEqualsFirstCell(second, 12345);
  }

  /**
   * Verifica que la primera celda de pre no fue contaminada por otra definición.
   *
   * @param definition la definición a inspeccionar
   * @param forbidden el valor que no debe aparecer
   */
  private static void assertNotEqualsFirstCell(PetriNetDefinition definition, int forbidden) {
    assertFalse(definition.pre()[0][0] == forbidden, "las definiciones comparten estado");
  }

  // ── load: entradas inválidas ──────────────────────────────

  @Test
  @DisplayName("load con id desconocido lanza PetriNetValidationException")
  void testLoadUnknownId() {
    PetriNetValidationException e =
        assertThrows(PetriNetValidationException.class, () -> catalog.load("id-que-no-existe"));
    assertTrue(e.getMessage().contains("id-que-no-existe"), "el mensaje debe citar el id pedido");
  }

  @Test
  @DisplayName("load con id null lanza PetriNetValidationException")
  void testLoadNullId() {
    assertThrows(PetriNetValidationException.class, () -> catalog.load(null));
  }

  @Test
  @DisplayName("load con cadena vacía lanza PetriNetValidationException")
  void testLoadEmptyId() {
    assertThrows(PetriNetValidationException.class, () -> catalog.load(""));
  }

  @Test
  @DisplayName("el catálogo actúa como whitelist: un nombre de recurso arbitrario es rechazado")
  void testLoadIsWhitelisted() {
    // Verifica la propiedad de seguridad documentada en la clase: el id del llamador nunca
    // llega al classloader. Si esto dejara de valer, un cliente podría leer cualquier
    // recurso del classpath pasando su nombre directamente.
    assertThrows(PetriNetValidationException.class, () -> catalog.load("config.properties"));
    assertThrows(PetriNetValidationException.class, () -> catalog.load("../../etc/passwd"));
  }

  // ── independencia entre instancias ────────────────────────

  @Test
  @DisplayName("dos catálogos independientes exponen el mismo contenido")
  void testTwoCatalogsAgree() {
    PetriNetCatalog other = new PetriNetCatalog();
    assertEquals(catalog.list().size(), other.list().size());
    assertEquals(catalog.getDefaultId(), other.getDefaultId());
  }
}
