package com.shanthigear.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving web pages.
 */
@Controller
public class WebController {
    
    /**
     * Serves the upload form page.
     * @return the name of the HTML template (without .html extension)
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/api/upload-form";
    }
    
    @GetMapping("/api/upload-form")
    public String showUploadForm() {
        return "upload-form";
    }
}
