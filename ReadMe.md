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

   http://localhost:8080/swagger-ui/index.html

4. **Authorization instructions**
   1. Go to the **AuthController**
   2. Execute the **login** request
      - Use **user1–user2** if you need an **ADMIN** role
      - Use **user3–user11** if you need a **USER** role 
      
        **Example request:**
      ```bash
      curl -X POST "http://localhost:8080/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
          "username": "user1",
          "password": "12345"
        }'
      ```
   3. You will receive a **JWT token** in the response

      **Example response:**
      ```json
      {
        "token": "eyJhbGciOiJIUzI1NiJ9..."
      }
      ```
   4. Scroll the Swagger page all the way to the top
   5. Click the **Authorize** button on the right
   6. Paste the token into the field **without `Bearer`** — it will be added automatically
   7. Click the **Authorize** button
   8. After obtaining the token, you can execute secured requests.  
      **Example (create a card as ADMIN):**
      ```bash
      curl -X POST "http://localhost:8080/cards" \
        -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
        -H "Content-Type: application/json" \
        -d '{
          "cardNumber": "1111222233334444",
          "ownerName": "user3",
          "expiryDate": "2030-01-01",
          "userId": "<USER_ID>"
        }'
      ``` 
      
   After that, all secured endpoints will be available within the current session.