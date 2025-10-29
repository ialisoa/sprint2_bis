package com.example.controller;

import com.example.service.ClasspathScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ScanController {

    private final ClasspathScannerService scannerService;

    @Autowired
    public ScanController(ClasspathScannerService scannerService) {
        this.scannerService = scannerService;
    }

    @GetMapping("/")
    public String scanClasses(
            @RequestParam(value = "annotation", required = false, defaultValue = "") String annotationName,
            @RequestParam(value = "basePackage", required = false, defaultValue = "") String basePackage,
            Model model) {
        
        // Si aucun filtre n'est spécifié, on affiche les contrôleurs par défaut
        if (annotationName == null || annotationName.trim().isEmpty()) {
            annotationName = "org.springframework.stereotype.Controller";
        }
        
        try {
            model.addAttribute("scannedClasses", scannerService.findClassesWithAnnotation(annotationName, basePackage));
            model.addAttribute("searchedAnnotation", annotationName);
            model.addAttribute("basePackage", basePackage);
            return "scan-results";
        } catch (IllegalArgumentException e) {
            // Si l'annotation n'est pas trouvée, suggérer les noms complets des annotations courantes
            String message = e.getMessage();
            if (message != null && message.contains("Annotation class not found")) {
                String simpleName = annotationName.contains(".") ? 
                    annotationName.substring(annotationName.lastIndexOf('.') + 1) : annotationName;
                
                message += "\n\nConseil : Essayez avec l'un de ces noms complets :\n" +
                          "- Pour les contrôleurs MVC: org.springframework.stereotype.Controller\n" +
                          "- Pour les contrôleurs REST: org.springframework.web.bind.annotation.RestController\n" +
                          "- Pour les méthodes GET: org.springframework.web.bind.annotation.GetMapping\n" +
                          "- Pour les services: org.springframework.stereotype.Service\n" +
                          "- Pour les composants: org.springframework.stereotype.Component";
                
                if (!annotationName.contains(".")) {
                    message += "\n\nVous avez entré un nom simple. Utilisez le nom complet de l'annotation.";
                }
                
                throw new IllegalArgumentException(message, e);
            }
            throw e;
        }
    }
}
