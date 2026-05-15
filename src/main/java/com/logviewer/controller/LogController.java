package com.logviewer.controller;

import com.logviewer.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("files", logService.listLogFiles());
        return "index";
    }

    @GetMapping("/api/logs")
    @ResponseBody
    public List<String> listFiles() {
        return logService.listLogFiles();
    }

    @GetMapping("/api/logs/content")
    @ResponseBody
    public String getContent(@RequestParam String fileName, @RequestParam(defaultValue = "100") int lines) throws IOException {
        String content = logService.readLastLines(fileName, lines);
        return HtmlUtils.htmlEscape(content);
    }

    @GetMapping("/api/logs/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String fileName) {
        if (!logService.isPathAllowed(fileName)) {
            return ResponseEntity.status(403).build();
        }

        File file = new File(fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
