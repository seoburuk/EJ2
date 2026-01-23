# CLAUDE.MD - Project Context for AI Assistant

## Project Overview
**EJ2** is a full-stack timetable management application built with Java 8, Spring Framework, React, and MariaDB. It allows users to create, manage, and visualize course schedules.

## Tech Stack

### Backend
- **Java Version**: Java 8
- **Framework**: Spring Framework 5.3.30 (XML-based configuration, NO Spring Boot)
- **Web Server**: Apache Tomcat 9
- **ORM**: Hibernate 5.6.15
- **Build Tool**: Maven
- **Packaging**: WAR file deployed to Tomcat

### Frontend
- **Framework**: React 18.2.0
- **HTTP Client**: Axios 1.6.0
- **Routing**: React Router DOM 7.12.0
- **Build Tool**: react-scripts 5.0.1
- **Additional**: html2canvas 1.4.1 (for screenshot/export functionality)

### Database
- **DBMS**: MariaDB 10.6
- **Connection Pool**: HikariCP (configured in RootConfig.java)
- **Default Database**: `appdb`
- **Credentials**:
  - User: `appuser` / Password: `apppassword`
  - Root: Password: `rootpassword`

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Web Server (Frontend)**: Nginx (production deployment)

## Project Structure

```
EJ2/
├── backend/                          # Spring Framework backend
│   ├── src/main/java/com/ej2/
│   │   ├── config/                   # Spring configuration
│   │   │   ├── RootConfig.java       # Root context: DB, Hibernate, Transactions
│   │   │   └── WebConfig.java        # Web context: MVC, CORS, JSON
│   │   ├── controller/               # REST API controllers
│   │   │   ├── UserController.java
│   │   │   ├── TimetableController.java
│   │   │   └── CourseSearchController.java
│   │   ├── model/                    # JPA entities
│   │   │   ├── User.java
│   │   │   ├── Timetable.java
│   │   │   ├── TimetableCourse.java
│   │   │   └── DaySchedule.java
│   │   ├── repository/               # JPA repositories
│   │   │   ├── UserRepository.java
│   │   │   ├── TimetableRepository.java
│   │   │   └── TimetableCourseRepository.java
│   │   ├── service/                  # Business logic
│   │   │   ├── UserService.java
│   │   │   └── TimetableService.java
│   │   └── converter/                # JPA attribute converters
│   │       ├── IntegerListConverter.java
│   │       └── DayScheduleListConverter.java
│   ├── src/main/webapp/WEB-INF/
│   │   └── web.xml                   # Servlet configuration
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                         # React frontend
│   ├── src/
│   │   ├── index.js                  # Entry point
│   │   ├── App.js                    # Main app component with routing
│   │   └── pages/
│   │       └── Users/
│   │           └── UsersPage.js
│   ├── public/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docker-compose.yml                # Multi-container orchestration
└── README.md
```

## Key Features

### Core Functionality
1. **User Management**: CRUD operations for users
2. **Timetable Management**: Create and manage multiple timetables per user
3. **Course Management**: Add/remove courses to timetables
4. **Course Search**: Search available courses
5. **Schedule Visualization**: View timetable in grid format

### Domain Model
- **User**: Base entity for authentication and timetable ownership
- **Timetable**: Container for course schedules (users can have multiple)
- **TimetableCourse**: Association between timetables and courses with scheduling info
- **DaySchedule**: Represents time slots for courses (serialized as JSON)

## API Endpoints

### User API (`/api/users`)
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Timetable API (`/api/timetables`)
- `GET /api/timetables/user/{userId}` - Get user's timetables
- `POST /api/timetables` - Create new timetable
- `PUT /api/timetables/{id}` - Update timetable
- `DELETE /api/timetables/{id}` - Delete timetable

### Course Search API (`/api/courses`)
- Course search and filtering endpoints

## Configuration Notes

### Backend Configuration
- **Context Path**: `/ej2` (configured in web.xml)
- **API Base Path**: `/api` (configured in WebConfig)
- **Full API URL**: `http://localhost:8080/ej2/api/*`
- **CORS**: Enabled for `http://localhost:3000` (development)
- **JSON**: Jackson configured for camelCase ↔ snake_case conversion
- **Transactions**: Annotation-driven (@Transactional)

### Frontend Configuration
- **Dev Server**: Runs on port 3000
- **Proxy**: Configured to proxy `/ej2` to `http://localhost:8080/ej2`
- **Production**: Nginx serves static files and proxies API requests

### Database Schema
- Tables are auto-created by Hibernate (hbm2ddl.auto=update)
- Uses JPA annotations for entity mapping
- Custom converters for List<Integer> and List<DaySchedule> JSON serialization

## Development Workflow

### Running with Docker Compose (Recommended)
```bash
docker-compose up --build
```
- Frontend: http://localhost:3000
- Backend: http://localhost:8080/ej2/api
- MariaDB: localhost:3306

### Running Locally (Development)
1. **Backend**:
   - Build: `cd backend && mvn clean package`
   - Deploy WAR to Tomcat
   - Ensure MariaDB is running

2. **Frontend**:
   - Install: `cd frontend && npm install`
   - Run: `npm start`

## Important Constraints & Patterns

### Java 8 Limitations
- No lambda streams in some contexts
- No var keyword
- Must use older date/time APIs where applicable
- Be mindful of functional programming syntax support

### Spring Framework (NOT Spring Boot)
- XML-based servlet configuration (web.xml)
- Java-based Spring configuration (@Configuration classes)
- Manual component scanning configuration
- NO application.properties (configuration in Java classes)
- NO embedded server (requires external Tomcat)

### Code Style
- Backend follows standard Java naming conventions
- Frontend uses modern React hooks and functional components
- API uses RESTful conventions
- JSON uses snake_case (backend) ↔ camelCase (frontend) conversion

## Common Tasks

### Adding a New Entity
1. Create `@Entity` class in `model/`
2. Create repository interface extending JpaRepository
3. Create service class with `@Service` and `@Transactional`
4. Create controller with `@RestController` and `@RequestMapping`
5. Add relationship mappings to existing entities if needed

### Adding a New API Endpoint
1. Add method to appropriate controller
2. Use `@GetMapping`, `@PostMapping`, etc.
3. Return ResponseEntity for proper HTTP status codes
4. Add service method for business logic
5. Test with curl or frontend integration

### Frontend API Integration
1. Use axios to call API
2. Remember proxy configuration handles `/ej2` prefix
3. Handle loading and error states
4. Update state with response data

## Troubleshooting Resources
- See `0119_1_troubleshooting_guide.md` for common issues
- See `0118_1_BEGINNER_GUIDE.md` for detailed setup instructions
- See `TROUBLESHOOTING_20260118_2.md` for additional debugging info

## Database Access
```bash
# Connect to MariaDB container
docker exec -it <container_name> mysql -u appuser -p
# Password: apppassword
```

## Recent Changes
- See `CHANGES_0119.md` for recent modifications
- Timetable feature recently added (see git commit: 89df8e00)

## Notes for AI Assistant
- This project uses **Spring Framework 5.3, NOT Spring Boot**
- Configuration is XML (web.xml) + Java (@Configuration), NOT application.properties
- WAR deployment to Tomcat, NOT embedded server
- Java 8 compliance required (no newer Java features)
- Frontend proxies `/ej2` context path to backend
- Database schema auto-generates via Hibernate
- CORS is configured for localhost:3000 development
