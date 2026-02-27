
<!--# 🚆 Online Reservation Portal

An Online Train Ticket Reservation System built using Java, JSP, Servlets, and MySQL. This system allows users to register, search for available journeys, book train tickets, and manage their bookings. Admins can manage stations, trains, journeys, and user data efficiently via a backend interface.


## 📌 Features

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
[Online Reservation Portal]()

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

# 🚆 TrackEase — Online Ticket Reservation System

A full-stack train ticket reservation web application that allows users to search journeys, book tickets, and manage reservations, while providing administrators with tools to manage trains, stations, and schedules.

Built using Java Servlets, JSP, and PostgreSQL, following the MVC design pattern.

---

## 🔗 Live Demo

👉 https://trackease-njtn.onrender.com


## 📂 GitHub Repository

👉 https://github.com/ShambhaviSingh16/Online-Reservation-Portal

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

<!--## 📸 Project Preview

---
-->

## 🙋‍♂️ Author

## Shambhavi Singh | 📧 Sshambhavi89@gmail.com
- [LinkedIn Profile](https://www.linkedin.com/in/shambhavi-singh2023)
- [GitHub Profile]( https://github.com/ShambhaviSingh16)
- [Leetcode Profile](https://leetcode.com/u/ShambhaviSingh16/)


⭐ If you found this project helpful, consider giving it a star!
