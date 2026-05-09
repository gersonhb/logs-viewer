package com.logviewer.config;

import org.springframework.beans.factory.annotation.Value;
import com.logviewer.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private LogService logService;

    private final Map<String, ExecutorService> tailers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // We will start tailing when the user sends the absolute path
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String absolutePath = message.getPayload();
        stopTailing(session.getId());

        if (!logService.isPathAllowed(absolutePath)) {
            session.sendMessage(new TextMessage("Error: Acceso no autorizado a la ruta especificada."));
            return;
        }

        File logFile = new File(absolutePath);
        if (!logFile.exists()) {
            session.sendMessage(new TextMessage("Error: File not found " + absolutePath));
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        tailers.put(session.getId(), executor);

        executor.submit(() -> {
            try (RandomAccessFile reader = new RandomAccessFile(logFile, "r")) {
                long lastKnownPosition = logFile.length();
                
                // Initially send the last 50 lines or so
                // For simplicity, we just start from the end
                
                while (session.isOpen()) {
                    long length = logFile.length();
                    if (length > lastKnownPosition) {
                        reader.seek(lastKnownPosition);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            session.sendMessage(new TextMessage(HtmlUtils.htmlEscape(line)));
                        }
                        lastKnownPosition = reader.getFilePointer();
                    } else if (length < lastKnownPosition) {
                        // File was truncated/rotated
                        lastKnownPosition = 0;
                        reader.seek(0);
                    }
                    Thread.sleep(1000);
                }
            } catch (IOException | InterruptedException e) {
                // Handle or log
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        stopTailing(session.getId());
    }

    private void stopTailing(String sessionId) {
        ExecutorService executor = tailers.remove(sessionId);
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
