package com.logviewer.controller;

import com.logviewer.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
        return logService.readLastLines(fileName, lines);
    }
}
