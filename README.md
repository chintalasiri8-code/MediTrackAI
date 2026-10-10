# MediTrack AI

Smart Treatment Journey Management System powered by Spring Boot (Java 17) and React (Vite).

## Folder Structure
```
MediTrackAI/
├── backend/    # Spring Boot 3 + Spring Data JPA + MySQL
└── frontend/   # React + Vite + Axios
```

## Database Configuration
- **Database Engine**: MySQL 8.0+
- **Database Name**: `meditrack_ai`
- **Default Credentials**: `root` / `<your_mysql_password>` (Configurable via `SPRING_DATASOURCE_USERNAME` & `SPRING_DATASOURCE_PASSWORD`)

## How to Run

### 1. Run Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
The backend starts at `http://localhost:8080`.

### 2. Run Frontend (React Vite)
```bash
cd frontend
npm install
npm run dev
```
The frontend starts at `http://localhost:5173`.
