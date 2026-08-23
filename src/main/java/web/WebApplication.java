import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for web mode: starts an embedded server exposing the Petri net analysis as a REST
 * API, and serving the React (CDN-based) frontend as a static resource.
 *
 * <p>Uses {@code @SpringBootConfiguration} + {@code @EnableAutoConfiguration} instead of the more
 * common {@code @SpringBootApplication}, and registers {@link AnalysisController} explicitly via a
 * {@code @Bean} method instead of relying on {@code @ComponentScan}. This is intentional: every
 * class in this project lives in the unnamed (default) package, and Spring Boot's classpath
 * component scanning refuses to run safely from the default package — it ends up scanning the
 * entire classpath, including Spring's own internal autoconfiguration classes, and crashes at
 * startup. Registering the controller by hand sidesteps that scan entirely.
 *
 * <p>This is a separate entry point from {@code Main}, which remains the original console mode —
 * neither depends on the other.
 *
 * @author Sassi Juan Ignacio
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class WebApplication {

  /**
   * Registers the analysis controller as a bean, without relying on classpath component scanning
   * (see the class-level Javadoc for why).
   *
   * @return the controller instance
   */
  @Bean
  public AnalysisController analysisController() {
    return new AnalysisController();
  }

  /**
   * Starts the embedded web server.
   *
   * @param args command-line arguments, passed through to Spring Boot
   */
  public static void main(String[] args) {
    SpringApplication.run(WebApplication.class, args);
  }
}