# Bank Card Management Application
---

## Build and Run Instructions

1. **Build the project:**

   **Stop and remove existing Docker containers, volumes, and images (optional but recommended on first run):**

   ```bash
   docker-compose down --volumes --rmi all
   ```
2. **Start the application:**

   ```bash
   docker-compose up
   ```

   This will start all necessary services in detached mode.

---

3. **Open Swagger UI:**

   http://localhost:8080/api/swagger-ui/index.html