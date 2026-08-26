import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tests for {@link WebApplication}.
 *
 * <p>Una clase de arranque tiene poco que testear unitariamente: {@code main} levanta un servidor,
 * lo que es una prueba de integración, no unitaria. Lo que sí vale la pena fijar es la
 * configuración deliberada que documenta el javadoc — sin escaneo de componentes, con el controller
 * registrado a mano — porque es una decisión frágil que alguien podría "simplificar" a
 * {@code @SpringBootApplication} y romper el arranque.
 *
 * @author Sassi Juan Ignacio
 */
@DisplayName("WebApplication")
class WebApplicationTest {

  @Test
  @DisplayName("está anotada como configuración de Spring Boot con autoconfiguración")
  void testConfigurationAnnotations() {
    assertTrue(WebApplication.class.isAnnotationPresent(SpringBootConfiguration.class));
    assertTrue(WebApplication.class.isAnnotationPresent(EnableAutoConfiguration.class));
  }

  @Test
  @DisplayName("NO usa escaneo de componentes")
  void testNoComponentScan() {
    // Todas las clases del proyecto viven en el paquete por defecto. Con @ComponentScan,
    // Spring escanearía el classpath entero — incluidas sus propias autoconfiguraciones —
    // y el arranque falla. Si alguien reemplaza esto por @SpringBootApplication, que
    // incluye @ComponentScan, este test lo frena antes de que llegue a producción.
    assertFalse(
        WebApplication.class.isAnnotationPresent(ComponentScan.class),
        "el escaneo de componentes rompe el arranque desde el paquete por defecto");
  }

  @Test
  @DisplayName("el controller se registra explícitamente como @Bean")
  void testControllerBeanIsDeclared() throws NoSuchMethodException {
    assertTrue(
        WebApplication.class
            .getMethod("analysisController")
            .isAnnotationPresent(Bean.class),
        "sin @Bean el controller no queda registrado y las rutas devuelven 404");
  }

  @Test
  @DisplayName("el método de bean construye un controller usable")
  void testBeanMethodBuildsController() {
    AnalysisController controller = new WebApplication().analysisController();

    assertNotNull(controller);
    assertFalse(controller.listNets().isEmpty(), "el controller debe poder leer el catálogo");
  }

  @Test
  @DisplayName("cada invocación del método de bean crea una instancia nueva")
  void testBeanMethodIsNotCached() {
    // Spring cachea el singleton por su cuenta; el método en sí no debe hacerlo.
    WebApplication application = new WebApplication();
    assertNotSame(application.analysisController(), application.analysisController());
  }
}