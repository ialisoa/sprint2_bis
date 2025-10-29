package com.example.service;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClasspathScannerService {

    public Map<String, Object> findClassesWithAnnotation(String annotationClassName, String basePackage) {
        try {
            // Charger la classe d'annotation
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) Class.forName(annotationClassName);
            
            // Configurer le scanner
            ConfigurationBuilder config = new ConfigurationBuilder()
                    .setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated);
            
            // Si un package de base est spécifié, on l'utilise, sinon on scanne tout le classpath
            if (basePackage != null && !basePackage.trim().isEmpty()) {
                config.setUrls(ClasspathHelper.forPackage(basePackage));
            } else {
                config.setUrls(ClasspathHelper.forJavaClassPath());
            }
            
            Reflections reflections = new Reflections(config);
            
            // Récupérer toutes les classes avec l'annotation spécifiée
            Set<Class<?>> annotatedClasses = new HashSet<>();
            
            // Si on recherche spécifiquement @Controller, on veut exclure les @RestController
            boolean isControllerSearch = "org.springframework.stereotype.Controller".equals(annotationClassName);
            
            // Si c'est une annotation de méthode (comme @GetMapping)
            boolean isMethodAnnotation = annotationClassName.endsWith("Mapping") || 
                                      annotationClassName.endsWith("GetMapping") ||
                                      annotationClassName.endsWith("PostMapping") ||
                                      annotationClassName.endsWith("PutMapping") ||
                                      annotationClassName.endsWith("DeleteMapping");
            
            if (isMethodAnnotation) {
                // Pour les annotations de méthode, on récupère les méthodes annotées
                Set<Method> methods = reflections.getMethodsAnnotatedWith(annotationClass);
                for (Method method : methods) {
                    if (!annotatedClasses.contains(method.getDeclaringClass())) {
                        annotatedClasses.add(method.getDeclaringClass());
                    }
                }
            } else {
                // Pour les annotations de classe
                Set<String> classNames = reflections.getStore().get(Scanners.TypesAnnotated.index()).get(annotationClass.getName());
                
                if (classNames != null) {
                    for (String className : classNames) {
                        try {
                            // Essayer de charger la classe
                            Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                            
                            // Vérifier que la classe a bien l'annotation
                            if (clazz.isAnnotationPresent(annotationClass)) {
                                // Si on recherche @Controller, on exclut les classes qui ont aussi @RestController
                                if (isControllerSearch && clazz.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class)) {
                                    continue;
                                }
                                annotatedClasses.add(clazz);
                            }
                        } catch (Throwable e) {
                            // Ignorer les classes qui ne peuvent pas être chargées
                            System.err.println("Impossible de charger la classe " + className + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            Map<String, Object> result = new LinkedHashMap<>();
            
            annotatedClasses.forEach(clazz -> {
                Map<String, Object> classInfo = new LinkedHashMap<>();
                classInfo.put("name", clazz.getName());
                classInfo.put("simpleName", clazz.getSimpleName());
                
                // Récupérer les méthodes de la classe
                List<Map<String, String>> methods = new ArrayList<>();
                try {
                    methods = Arrays.stream(clazz.getDeclaredMethods())
                            .map(method -> {
                                Map<String, String> methodInfo = new LinkedHashMap<>();
                                methodInfo.put("name", method.getName());
                                methodInfo.put("returnType", method.getReturnType().getSimpleName());
                                methodInfo.put("parameters", Arrays.stream(method.getParameterTypes())
                                        .map(Class::getSimpleName)
                                        .collect(Collectors.joining(", ")));
                                return methodInfo;
                            })
                            .collect(Collectors.toList());
                } catch (Throwable e) {
                    // En cas d'erreur lors de la récupération des méthodes, on continue avec une liste vide
                    System.err.println("Erreur lors de la récupération des méthodes pour " + clazz.getName() + ": " + e.getMessage());
                }
                
                classInfo.put("methods", methods);
                
                // Ajouter les informations de l'annotation
                Annotation annotation = clazz.getAnnotation(annotationClass);
                try {
                    // Essayer d'appeler la méthode value() si elle existe
                    Method valueMethod = annotationClass.getMethod("value");
                    if (valueMethod != null) {
                        String value = valueMethod.invoke(annotation).toString();
                        if (!value.isEmpty()) {
                            classInfo.put("annotationValue", value);
                        }
                    }
                } catch (Exception e) {
                    // La méthode value() n'existe pas pour cette annotation
                }
                
                result.put(clazz.getName(), classInfo);
            });
            
            return result;
            
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Annotation class not found: " + annotationClassName, e);
        } catch (Exception e) {
            throw new RuntimeException("Error scanning classes with annotation: " + annotationClassName, e);
        }
    }
    
    // Ancienne méthode conservée pour la rétrocompatibilité
    public Map<String, Object> findAnnotatedClasses(String packageName) {
        return findClassesWithAnnotation("com.example.annotation.ScanMe", packageName);
    }
}
