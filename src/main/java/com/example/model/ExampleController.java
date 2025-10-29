package com.example.model;

import com.example.annotation.ScanMe;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ScanMe("Example de contrôleur")
@RestController
public class ExampleController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    public void exampleMethod(String param1, int param2) {
        // Méthode d'exemple
    }
}
