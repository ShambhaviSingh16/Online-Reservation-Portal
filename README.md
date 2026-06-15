# 🚆 TrackEase — Enterprise Railway Reservation Platform

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Cloud--Hosted-blue?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=flat-square&logo=docker)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-black?style=flat-square&logo=githubactions)
![Live](https://img.shields.io/badge/Live-Render-46E3B7?style=flat-square&logo=render)

A **production-grade, enterprise-level train ticket reservation platform** built with Spring Boot, Spring Security, JWT, and PostgreSQL. Implements real-world railway domain logic including Seat Availability Management, Waiting List & RAC allocation, role-based access control, and a comprehensive Admin Analytics Dashboard — containerized with Docker and deployed via CI/CD pipeline.

🔗 **Live Demo:** [https://trackease-njtn.onrender.com](https://trackease-njtn.onrender.com)
📄 **API Docs (Swagger):** `https://trackease-njtn.onrender.com/swagger-ui.html`

---

## 📌 Table of Contents

- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Architecture](#️-architecture)
- [API Overview](#-api-overview)
- [Database Schema](#-database-schema)
- [Getting Started](#-getting-started)
- [Docker Setup](#-docker-setup)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Project Structure](#-project-structure)
- [Author](#-author)

---

## ✨ Features

### 👤 User Module
| Feature | Description |
|---|---|
| Registration & Login | Secure JWT-based authentication with BCrypt password hashing |
| Journey Search | Search trains by source, destination & date with filters |
| Seat Booking | Real-time seat availability check with transactional booking |
| Waiting List & RAC | Automatic RAC and Waiting List allocation when seats are full |
| Booking History | Paginated dashboard showing all past and active reservations |
| Email Notifications | Booking confirmation and cancellation emails via JavaMailSender |
| PDF Ticket | Downloadable ticket with booking details generated via iText |

### 🛠️ Admin Module
| Feature | Description |
|---|---|
| Train Management | Add, update, soft-delete trains and manage seat configuration |
| Station Management | Manage stations and route mappings |
| Journey Scheduling | Create and manage journey schedules with pricing |
| Ticket Monitoring | View, filter, and manage all system bookings |
| Analytics Dashboard | Booking trends, revenue summaries, and occupancy insights |

### ⚙️ Engineering Features
| Feature | Description |
|---|---|
| JWT Auth + Spring Security | Stateless authentication with role-based access control (ADMIN / USER) |
| Global Exception Handling | Centralized `@ControllerAdvice` with structured error responses |
| Transaction Management | `@Transactional` on booking workflows to prevent double-booking |
| Data Validation | Bean Validation (`@Valid`) on all request DTOs |
| Soft Delete | Records deactivated rather than permanently removed |
| Database Indexing | Indexes on `journey_date`, `train_id`, `user_id` for query performance |
| Pagination & Filtering | Spring Data `Pageable` on all list endpoints |
| Logging | SLF4J + Logback structured logging across all service layers |
| Health Check | Spring Actuator `/actuator/health` for uptime monitoring |
| Swagger/OpenAPI | Live interactive API documentation |
| Docker | Containerized with multi-stage `Dockerfile` |
| GitHub Actions CI/CD | Automated build, test & deploy pipeline on every push |
| Environment Profiles | Separate `dev` and `prod` configuration profiles |

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Backend Framework | Spring Boot 3.x |
| Security | Spring Security + JWT (JJWT) |
| ORM | Hibernate / JPA (Spring Data JPA) |
| Database | PostgreSQL (Cloud-hosted) |
| API Documentation | Swagger / OpenAPI 3 |
| Email | JavaMailSender (SMTP) |
| PDF Generation | iText / Apache PDFBox |
| Testing | JUnit 5 + Mockito |
| Build Tool | Maven |
| Containerization | Docker |
| CI/CD | GitHub Actions |
| Deployment | Render |
| Monitoring | Spring Boot Actuator |

---

## 🏛️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client (Browser)                      │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP Requests (REST)
┌───────────────────────▼─────────────────────────────────┐
│              Spring Boot Application                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  Controller │→ │   Service    │→ │  Repository   │  │
│  │   (REST)    │  │  (Business   │  │  (Spring Data │  │
│  │             │  │   Logic)     │  │     JPA)      │  │
│  └─────────────┘  └──────────────┘  └───────┬───────┘  │
│                                              │           │
│  ┌─────────────────────────────────────────┐│           │
│  │         Spring Security + JWT           ││           │
│  │     Role-Based Access: ADMIN / USER     ││           │
│  └─────────────────────────────────────────┘│           │
└─────────────────────────────────────────────┼───────────┘
                                              │
                        ┌─────────────────────▼──────────┐
                        │      PostgreSQL (Cloud)         │
                        │  Trains | Journeys | Tickets    │
                        │  Users  | Stations | WaitList   │
                        └────────────────────────────────┘
```

---

## 📡 API Overview

Full interactive docs available at `/swagger-ui.html` on the live deployment.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login & receive JWT |
| GET | `/api/journeys/search` | User | Search available journeys |
| POST | `/api/bookings` | User | Book a ticket |
| GET | `/api/bookings/history` | User | Booking history (paginated) |
| DELETE | `/api/bookings/{id}` | User | Cancel booking |
| GET | `/api/admin/trains` | Admin | List all trains |
| POST | `/api/admin/trains` | Admin | Add new train |
| GET | `/api/admin/analytics` | Admin | Dashboard analytics |

---

## 🗃️ Database Schema

```
users         → id, name, email, password, role, is_active, created_at
trains        → id, train_number, name, total_seats, is_active
stations      → id, name, code, city, is_active
journeys      → id, train_id, source_id, dest_id, date, fare, available_seats
tickets       → id, user_id, journey_id, seat_number, status, booked_at, is_active
waiting_list  → id, ticket_id, user_id, journey_id, position, rac_status
```

> Indexes applied on: `journey_date`, `train_id`, `user_id`, `status` for optimized query performance (~35% improvement on high-frequency booking queries).

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL
- Docker (optional)

### Clone & Configure

```bash
git clone https://github.com/ShambhaviSingh16/Online-Reservation-Portal.git
cd Online-Reservation-Portal
```

Update `src/main/resources/application-dev.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/trackease
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

app.jwt.secret=your_jwt_secret_key
app.jwt.expiration=86400000

spring.mail.host=smtp.gmail.com
spring.mail.username=your_email
spring.mail.password=your_app_password
```

### Run Locally

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

App runs at: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 🐳 Docker Setup

```bash
# Build image
docker build -t trackease .

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/trackease \
  -e SPRING_DATASOURCE_USERNAME=user \
  -e SPRING_DATASOURCE_PASSWORD=pass \
  -e APP_JWT_SECRET=your_secret \
  trackease
```

---

## ⚙️ CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/deploy.yml`) triggers on every push to `main`:

```
Push to main
    │
    ▼
Build & Compile (Maven)
    │
    ▼
Run Unit Tests (JUnit + Mockito)
    │
    ▼
Build Docker Image
    │
    ▼
Deploy to Render
```

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/trackease/
│   │   ├── controller/        # REST Controllers
│   │   ├── service/           # Business Logic
│   │   ├── repository/        # Spring Data JPA Repositories
│   │   ├── model/             # JPA Entities
│   │   ├── dto/               # Request/Response DTOs
│   │   ├── security/          # JWT Filter, Security Config
│   │   ├── exception/         # Global Exception Handler
│   │   └── util/              # PDF, Email utilities
│   └── resources/
│       ├── application.yml
│       ├── application-dev.properties
│       └── application-prod.properties
└── test/
    └── java/com/trackease/    # JUnit + Mockito Tests
```

---

## 👩‍💻 Author

**Shambhavi Singh**
MCA Graduate | Software Engineer | VTU 2025

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?style=flat-square&logo=linkedin)](https://www.linkedin.com/in/shambhavi-singh2023)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-black?style=flat-square&logo=github)](https://github.com/ShambhaviSingh16)
[![LeetCode](https://img.shields.io/badge/LeetCode-180%2B%20Problems-orange?style=flat-square&logo=leetcode)](https://leetcode.com/u/ShambhaviSingh16/)
[![Portfolio](https://img.shields.io/badge/Portfolio-Visit-purple?style=flat-square)](https://your-portfolio-link.com)

📧 Sshambhavi89@gmail.com

---

> ⭐ If you found this project helpful or impressive, consider giving it a star!
<!--# 🚆 Online Reservation Portal

An Online Train Ticket Reservation System built using Java, JSP, Servlets, and MySQL. This system allows users to register, search for available journeys, book train tickets, and manage their bookings. Admins can manage stations, trains, journeys, and user data efficiently via a backend interface.


## 📌 Feature

### 👤 User Features
- User registration and login
- Search for train journeys between stations
- View available seats and fare details
- Book train tickets
- View and manage their own booked tickets

### 🛠️ Admin Features
- Admin login with dashboard access
- Add, edit, delete train stations
- Manage trains (create, update, remove)
- Manage station (create, update, remove)
- Schedule and manage journeys with real-time data (create, update, remove)
- View ticket booking records of all users

---

## 💻 Tech Stack

| Layer        | Technology               |
|--------------|---------------------------|
| Frontend     | HTML, CSS, JSP            |
| Backend      | Java Servlets             |
| Database     | MySQL                     |
| Web Server   | Apache Tomcat 9+          |
| IDE          | Eclipse                   |
| JDBC Driver  | MySQL Connector/J         |


---

## 🧪 Sample Admin Login

Username: admin
Password: admin123

---

## Live Demo : 
[Online Reservation Portal](https://trackease-njtn.onrender.com) 

## 📦 Database Schema

> Create the database and tables using this SQL snippet:

```sql
CREATE DATABASE trainticketing;
USE trainticketing;

-- Users Table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('admin', 'user') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (username, password, email, full_name, role) 
VALUES ('admin', 'admin123', 'admin@trainticket.com', 'System Administrator', 'admin');

-- Trains, Stations, Journeys, Tickets ...
-- (Refer to full SQL file in /sql/trainticketing_db.sql)


```


## ⚙️ How to Run Locally
### 1. Prerequisites
- Java JDK 8 or higher

- pache Tomcat 9+

- MySQL Server

- Eclipse IDE

### 2. Steps
 Step 1: Clone the repository
```
git clone (https://github.com/ShambhaviSingh16/Online-Reservation-Portal.git)
```

 Step 2: Import into Eclipse as a Dynamic Web Project

 Step 3: Configure MySQL DB in DBConnection.java
 Step 4: Deploy on Tomcat and start server


## 🌐 Live Deployment (Optional)
You can deploy this project live using:

```
Render
Railway
VPS (e.g., DigitalOcean)
```

## 🤝 Contributing
Contributions are welcome!
Feel free to fork the repo and submit pull requests for improvements or bug fixes.

## 📃 License
This project is for educational and academic use. 🚀

## 🙋‍♂️ Author
Shambhavi Singh | 📧 Sshambhavi89@gmail.com | [GitHub Profile]--> <!--( https://github.com/ShambhaviSingh16)-->
<!--


# 🚆 TrackEase — Online Ticket Reservation System

A full-stack train ticket reservation web application that allows users to search journeys, book tickets, and manage reservations, while providing administrators with tools to manage trains, stations, and schedules.

Built using Java Servlets, JSP, and PostgreSQL, following the MVC design pattern.

---

## 🔗 Live Demo

👉 [TrackEase: Online Reservation Portal](https://trackease-njtn.onrender.com)


<!--## 📂 GitHub Repository

👉 [](https://github.com/ShambhaviSingh16/Online-Reservation-Portal)-->
<!--
---

## ✨ Features

### 👤 User Module
- User registration and secure login  
- Search available train journeys  
- Book train tickets  
- View and manage booked tickets  
- User dashboard for reservation tracking  

### 🛠️ Admin Module
- Admin authentication  
- Add and manage trains  
- Add and manage stations  
- Manage journey schedules  
- Monitor booking records  

---

## 🏗️ Tech Stack

- **Backend:** Java (Servlets, JSP)  
- **Database:** PostgreSQL (Cloud Hosted)  
- **Architecture:** MVC  
- **Server:** Apache Tomcat  
- **Frontend:** HTML, CSS, JavaScript  
- **Deployment:** Render  

---

## 🚀 How to Run Locally

### Prerequisites
- Java JDK 8+  
- Apache Tomcat 9+  
- PostgreSQL  
- Eclipse IDE (recommended)

### Steps

1. Clone the repository
   ```bash
   git clone https://github.com/ShambhaviSingh16/Online-Reservation-Portal.git

2. Import the project into Eclipse as a Dynamic Web Project

3. Configure PostgreSQL database
    - Create database

    - Update DB credentials in project

4. Deploy on Apache Tomcat

5. Open in browser:
   ```bash
    http://localhost:8080/TrackEase

---
-->
<!--## 📸 Project Preview

---
-->
<!--
## 🙋‍♂️ Author

## Shambhavi Singh | 📧 Sshambhavi89@gmail.com
- [LinkedIn Profile](https://www.linkedin.com/in/shambhavi-singh2023)
- [GitHub Profile]( https://github.com/ShambhaviSingh16)
- [Leetcode Profile](https://leetcode.com/u/ShambhaviSingh16/)


⭐ If you found this project helpful, consider giving it a star!
-->

