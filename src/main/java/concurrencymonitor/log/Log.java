import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base class for log file management with automatic timestamp formatting.
 *
 * <p>Log files are stored in a {@code log/} subdirectory and automatically receive a {@code .log}
 * extension. If a file with the same name already exists, a numbered suffix is added (e.g. {@code
 * algorithms (1).log}).
 *
 * @author Sassi Juan Ignacio
 */
public class Log {

  /** Formatter for timestamps in format {@code yyyy-MM-dd HH:mm:ss}. */
  private final DateTimeFormatter formatter;

  /** Full path to the log file. */
  private final String filePath;

  /**
   * Creates a new {@code Log} instance. The {@code log/} directory is created if it does not exist.
   * If a file with the same name already exists, a numbered suffix is appended.
   *
   * @param fileName the base name of the log file (without path; {@code .log} is added if absent)
   */
  public Log(String fileName) {
    this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    File logDir = new File("log");
    if (!logDir.exists() && !logDir.mkdirs()) {
      System.err.println("Warning: could not create log directory");
    }

    String fullPath = "log" + File.separator + fileName;
    if (!fileName.endsWith(".log")) {
      fullPath += ".log";
    }

    this.filePath = resolveAvailablePath(fullPath);
    createFile();
  }

  /**
   * Returns an available file path, appending a numeric suffix if the desired path already exists.
   *
   * @param desired the preferred file path
   * @return a path that does not yet exist on disk
   */
  private static String resolveAvailablePath(String desired) {
    if (!new File(desired).exists()) {
      return desired;
    }

    int lastDot = desired.lastIndexOf('.');
    String name = lastDot > 0 ? desired.substring(0, lastDot) : desired;
    String ext = lastDot > 0 ? desired.substring(lastDot) : "";

    int counter = 1;
    String candidate;
    do {
      candidate = name + " (" + counter++ + ")" + ext;
    } while (new File(candidate).exists());

    return candidate;
  }

  /** Creates the physical log file. Errors are printed to stderr but do not propagate. */
  private void createFile() {
    try {
      new File(filePath).createNewFile();
    } catch (IOException e) {
      System.err.println("Error creating log file: " + e.getMessage());
    }
  }

  /**
   * Appends a timestamped message to the log file using UTF-8 encoding.
   *
   * @param message the message to write
   */
  public void write(String message) {
    try (FileOutputStream fos = new FileOutputStream(filePath, true);
        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(osw);
        PrintWriter out = new PrintWriter(bw)) {
      out.println("[" + LocalDateTime.now().format(formatter) + "] " + message);
    } catch (IOException e) {
      System.err.println("Error writing to log: " + e.getMessage());
    }
  }

  /**
   * Returns the full path of the log file.
   *
   * @return the log file path
   */
  public String getFilePath() {
    return filePath;
  }
}
