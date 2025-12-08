# Movie Backend - Spring Boot Application

A Spring Boot 3.5.7 application for a movie streaming platform with AI recommendation and toxic-comment filtering.

## Prerequisites

Before running this project, ensure you have the following installed:

1. **Java 21** - Required JDK version
2. **Maven 3.6+** (or use the included Maven wrapper)
3. **PostgreSQL** - Database server
4. **Redis** - For session management and caching
5. **Meilisearch** - For search functionality

## External Services Required

The application uses the following external services (configured in `application.yaml`):
- **Cloudinary** - Image/video hosting
- **Cloudflare Stream** - Video streaming
- **TMDB API** - Movie database
- **Gmail SMTP** - Email service
- **Stripe** - Payment processing

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:
```sql
CREATE DATABASE moviedb;
```

The application is configured to connect to:
- **Host**: `localhost`
- **Port**: `5433`
- **Database**: `moviedb`
- **Username**: `h1eu`
- **Password**: `password`

Update these values in `src/main/resources/application.yaml` if needed.

### 2. Redis Setup

Start Redis server on port 6379:
```bash
# Windows (if installed)
redis-server

# Or using Docker
docker run -d -p 6379:6379 redis:latest
```

### 3. Meilisearch Setup

Start Meilisearch on port 7700:
```bash
# Using Docker
docker run -d -p 7700:7700 -e MEILI_MASTER_KEY=f40df06b47f029d32ac2bbd230e5c210a3ab613a getmeili/meilisearch:latest
```

Or download and run Meilisearch locally from [meilisearch.com](https://www.meilisearch.com/docs/learn/getting_started/installation)

### 4. Configuration

Review and update `src/main/resources/application.yaml` with your own:
- Database credentials
- Redis connection details
- External API keys (Cloudinary, Cloudflare, TMDB, etc.)
- Email SMTP settings
- JWT secret key

## Running the Application

### Option 1: Using Maven Wrapper (Recommended)

**Windows:**
```bash
.\mvnw.cmd spring-boot:run
```

**Linux/Mac:**
```bash
./mvnw spring-boot:run
```

### Option 2: Using Maven (if installed)

```bash
mvn spring-boot:run
```

### Option 3: Build and Run JAR

1. Build the project:
```bash
mvn clean package
```

2. Run the JAR:
```bash
java -jar target/movie-backend-0.0.1-SNAPSHOT.jar
```

### Option 4: Using IDE

1. Import the project as a Maven project in your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Run the `MovieBackendApplication.java` main class

## Application Endpoints

Once running, the application will be available at:
- **Base URL**: `http://localhost:8080`
- **API Documentation**: `http://localhost:8080/swagger-ui.html` (Swagger/OpenAPI)
- **Actuator**: `http://localhost:8080/actuator` (Health checks and metrics)

## Project Structure

```
src/
├── main/
│   ├── java/com/ptithcm/movie/
│   │   ├── auth/          # Authentication & authorization
│   │   ├── comment/       # Comment management
│   │   ├── movie/         # Movie-related features
│   │   ├── user/          # User management
│   │   ├── config/        # Configuration classes
│   │   └── external/      # External service integrations
│   └── resources/
│       └── application.yaml  # Application configuration
└── test/                  # Test files
```

## Troubleshooting

1. **Port already in use**: Change the port in `application.yaml` under `server.port`
2. **Database connection error**: Ensure PostgreSQL is running and credentials are correct
3. **Redis connection error**: Ensure Redis is running on port 6379
4. **Meilisearch connection error**: Ensure Meilisearch is running on port 7700

## Development

The project uses:
- **Spring Boot 3.5.7**
- **Java 21**
- **PostgreSQL** for database
- **Redis** for caching and sessions
- **JWT** for authentication
- **OAuth2** for social login (Google)
- **Lombok** for reducing boilerplate code

## License

See project files for license information.



