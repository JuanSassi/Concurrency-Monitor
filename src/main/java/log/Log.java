import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for managing log files with automatic timestamp formatting.
 * This class creates log files in a dedicated log directory and writes
 * timestamped messages to them. If a file with the same name already exists,
 * it automatically creates a new file with a numbered suffix.
 * 
 * <p>Log files are stored in a "log/" subdirectory and automatically
 * receive a ".log" extension if not provided.</p>
 * 
 * @author Sassi Juan Ignacio
 */
public class Log {
    /** The full path to the log file */
    private String filePath;

    /** Formatter for timestamps in format "yyyy-MM-dd HH:mm:ss" */
    private DateTimeFormatter formatter;
    
    /**
     * Creates a new Log instance with the specified file name.
     * The log directory is created if it doesn't exist, and the file
     * is created in the log/ subdirectory. If a file with the same name
     * already exists, a numbered suffix is added.
     * 
     * @param fileName the name of the log file (without path). The ".log"
     *                 extension is added automatically if not present
     */
    public Log(String fileName) {
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Create log directory if it doesn't exist
        File logDir = new File("log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        // Build full path: log/filename.log
        String fullPath = "log" + File.separator + fileName;
        if (!fileName.endsWith(".log")) {
            fullPath += ".log";
        }
        
        this.filePath = getAvailableFilePath(fullPath);
        createLogFile();
    }
    
    /**
     * Finds an available file path by adding a numbered suffix if the
     * original path already exists. The method checks for existing files
     * and increments a counter until a unique filename is found.
     * 
     * @param originalPath the desired file path
     * @return an available file path, either the original or with a
     *         numbered suffix like "filename (1).log"
     */
    private String getAvailableFilePath(String originalPath) {
        File file = new File(originalPath);
        
        // If it doesn't exist, use the original name
        if (!file.exists()) {
            return originalPath;
        }
        
        // Separate name and extension
        String name = originalPath;
        String extension = "";
        int lastDot = originalPath.lastIndexOf('.');
        
        if (lastDot > 0) {
            name = originalPath.substring(0, lastDot);
            extension = originalPath.substring(lastDot);
        }
        
        // Search for an available number
        int counter = 1;
        String newPath;
        do {
            newPath = name + " (" + counter + ")" + extension;
            file = new File(newPath);
            counter++;
        } while (file.exists());
        
        return newPath;
    }

    /**
     * Creates the physical log file at the determined file path.
     * If the file cannot be created, an error message is printed to
     * standard error but the exception is not propagated.
     */
    private void createLogFile() {
        try {
            File file = new File(filePath);
            file.createNewFile();
        } catch (IOException e) {
            System.err.println("Error al crear archivo de log: " + e.getMessage());
        }
    }

    /**
     * Writes a message to the log file with the current timestamp.
     * Each message is written on a new line with the format:
     * [yyyy-MM-dd HH:mm:ss] message
     * 
     * <p>If an error occurs during writing, it is printed to standard
     * error but does not stop execution.</p>
     * 
     * @param message the message to write to the log file
     */    
    public void write(String message) {
        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            String timestamp = LocalDateTime.now().format(formatter);
            out.println("[" + timestamp + "] " + message);
            
        } catch (IOException e) {
            System.err.println("Error al escribir en el log: " + e.getMessage());
        }
    }
    
    /**
     * Gets the complete file path where log messages are being written.
     * 
     * @return the absolute or relative path to the log file
     */
    public String getFilePath() {
        return filePath;
    }
}