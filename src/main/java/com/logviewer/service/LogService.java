package com.logviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    @Value("${logs.directories:./logs}")
    private String[] logsDirectories;

    @PostConstruct
    public void init() {
        for (String dir : logsDirectories) {
            File folder = new File(dir);
            logger.info("Verificando ruta de logs: {}", folder.getAbsolutePath());
            if (!folder.exists()) {
                logger.warn("¡ADVERTENCIA! El directorio no existe: {}", folder.getAbsolutePath());
            }
        }
    }

    public List<String> listLogFiles() {
        List<File> allFiles = new ArrayList<>();
        
        for (String dir : logsDirectories) {
            File folder = new File(dir);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((d, name) -> 
                    name.toLowerCase().endsWith(".log") || 
                    name.toLowerCase().endsWith(".txt") || 
                    name.toLowerCase().contains("catalina") ||
                    name.toLowerCase().contains("out")
                );
                if (files != null) {
                    allFiles.addAll(Arrays.asList(files));
                }
            }
        }

        return allFiles.stream()
                .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
                .map(File::getAbsolutePath) // Usamos la ruta completa para identificar archivos únicos
                .collect(Collectors.toList());
    }

    public String readLastLines(String absolutePath, int numLines) throws IOException {
        if (!isPathAllowed(absolutePath)) {
            logger.error("Intento de acceso no autorizado a la ruta: {}", absolutePath);
            return "Error: Acceso denegado a la ruta especificada.";
        }

        Path path = Paths.get(absolutePath);
        if (!Files.exists(path)) {
            return "File not found: " + absolutePath;
        }

        List<String> lines = Files.readAllLines(path);
        int size = lines.size();
        int start = Math.max(0, size - numLines);
        return String.join("\n", lines.subList(start, size));
    }

    public boolean isPathAllowed(String absolutePath) {
        if (absolutePath == null || absolutePath.contains("..")) {
            return false;
        }

        try {
            Path targetPath = Paths.get(absolutePath).toAbsolutePath().normalize();
            for (String dir : logsDirectories) {
                Path allowedPath = Paths.get(dir).toAbsolutePath().normalize();
                if (targetPath.startsWith(allowedPath)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error validando ruta: {}", absolutePath, e);
        }
        return false;
    }
}
