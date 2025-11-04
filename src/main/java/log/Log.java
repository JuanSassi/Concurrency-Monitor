import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private String filePath;
    private DateTimeFormatter formatter;
    
    public Log(String fileName) {
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Crear directorio log/ si no existe
        File logDir = new File("log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        // Construir ruta completa: log/nombreArchivo.log
        String fullPath = "log" + File.separator + fileName;
        if (!fileName.endsWith(".log")) {
            fullPath += ".log";
        }
        
        this.filePath = getAvailableFilePath(fullPath);
        createLogFile();
    }
    
    private String getAvailableFilePath(String originalPath) {
        File file = new File(originalPath);
        
        // Si no existe, usar el nombre original
        if (!file.exists()) {
            return originalPath;
        }
        
        // Separar nombre y extensión
        String name = originalPath;
        String extension = "";
        int lastDot = originalPath.lastIndexOf('.');
        
        if (lastDot > 0) {
            name = originalPath.substring(0, lastDot);
            extension = originalPath.substring(lastDot);
        }
        
        // Buscar un número disponible
        int counter = 1;
        String newPath;
        do {
            newPath = name + " (" + counter + ")" + extension;
            file = new File(newPath);
            counter++;
        } while (file.exists());
        
        return newPath;
    }
    
    private void createLogFile() {
        try {
            File file = new File(filePath);
            file.createNewFile();
        } catch (IOException e) {
            System.err.println("Error al crear archivo de log: " + e.getMessage());
        }
    }
    
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
    
    public String getFilePath() {
        return filePath;
    }
}