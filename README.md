
# 🚆 Online Reservation Portal

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
Shambhavi Singh | 📧 Sshambhavi89@gmail.com | [GitHub Profile]( https://github.com/ShambhaviSingh16)
