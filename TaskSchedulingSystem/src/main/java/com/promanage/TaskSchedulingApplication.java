package com.promanage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TaskSchedulingApplication
 *
 * Entry point for the ProManage AI-Powered Task Scheduling System.
 *
 * When this runs:
 *   1. Spring Boot starts a web server on port 8080
 *   2. Serves index.html from src/main/resources/static/
 *   3. Exposes REST API endpoints at /api/...
 *   4. Java calls Python FastAPI (port 8000) for AI predictions
 *   5. Java saves/reads data from PostgreSQL
 *
 * HOW TO RUN:
 *   - Start Python server first:  uvicorn main:app --reload  (port 8000)
 *   - Then run this class
 *   - Open browser: http://localhost:8080
 */
@SpringBootApplication
public class TaskSchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskSchedulingApplication.class, args);
        System.out.println("\n==========================================");
        System.out.println("  ProManage Task Scheduling System");
        System.out.println("  Open browser: http://localhost:8080");
        System.out.println("==========================================\n");
    }
}
