<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TrackEase</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        :root {
            --primary: #4361ee;
            --primary-light: #4895ef;
            --secondary: #3f37c9;
            --dark: #212529;
            --light: #f8f9fa;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: var(--light);
        }
        
        .hero-section {
            background: linear-gradient(135deg, var(--primary), var(--secondary));
            color: white;
            padding: 5rem 0;
            position: relative;
            overflow: hidden;
        }
        
        .hero-content {
            position: relative;
            z-index: 1;
        }
        
        .hero-title {
            font-weight: 700;
            font-size: 2.8rem;
        }
        
        .hero-subtitle {
            font-weight: 300;
            font-size: 1.2rem;
            opacity: 0.9;
        }
        
        .auth-card {
            border-radius: 10px;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
            border: none;
            overflow: hidden;
        }
        
        .auth-header {
            background: linear-gradient(135deg, var(--primary), var(--primary-light));
            color: white;
            padding: 1.5rem;
            text-align: center;
        }
        
        .auth-body {
            padding: 2rem;
        }
        
        .btn-primary {
            background-color: var(--primary);
            border-color: var(--primary);
        }
        
        .btn-primary:hover {
            background-color: var(--secondary);
            border-color: var(--secondary);
        }
        
        .btn-outline-light:hover {
            color: var(--primary);
        }
        
        .feature-icon {
            font-size: 2rem;
            color: var(--primary);
            margin-bottom: 1rem;
        }
        
        .train-icon {
            animation: float 3s ease-in-out infinite;
        }
        
        @keyframes float {
            0%, 100% { transform: translateY(0); }
            50% { transform: translateY(-10px); }
        }
        
        /* Footer */
        footer {
            background-color: var(--dark);
            color: white;
            padding: 4rem 0 2rem;
        }
        
        .footer-links h5 {
            margin-bottom: 1.5rem;
            font-weight: 600;
        }
        
        .footer-links ul {
            list-style: none;
            padding-left: 0;
        }
        
        .footer-links li {
            margin-bottom: 0.8rem;
        }
        
        .footer-links a {
            color: rgba(255,255,255,0.7);
            text-decoration: none;
            transition: all 0.3s ease;
        }
        
        .footer-links a:hover {
            color: white;
            padding-left: 5px;
        }
        
    </style>
</head>
<body>
    <!-- Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark" style="background: linear-gradient(135deg, var(--primary), var(--secondary));">
        <div class="container">
            <a class="navbar-brand fw-bold" href="#">
                <i class="fas fa-train me-2"></i>TrackEase
            </a>
            <div class="d-flex">
                <a href="login.jsp" class="btn btn-outline-light me-2">Login</a>
                <a href="register.jsp" class="btn btn-light">Register</a>
            </div>
        </div>
    </nav>

    <!-- Hero Section -->
    <section class="hero-section">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-lg-6 hero-content">
                    <h1 class="hero-title mb-3">Book Tickets in Minutes</h1>
                    <p class="hero-subtitle mb-4">Fast, secure and convenient travel ticket booking platform</p>
                    <div class="d-flex gap-3">
                        <a href="register.jsp" class="btn btn-light btn-lg px-4">Get Started</a>
                        <a href="#features" class="btn btn-outline-light btn-lg px-4">Learn More</a>
                    </div>
                </div>
                <div class="col-lg-6 text-center d-none d-lg-block">
                    <i class="fas fa-train train-icon" style="font-size: 15rem; opacity: 0.8;"></i>
                </div>
            </div>
        </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="py-5 bg-white">
        <div class="container">
            <div class="row mb-5">
                <div class="col-12 text-center">
                    <h2 class="fw-bold mb-3">Why Choose TrackEase?</h2>
                    <p class="text-muted">Experience the best in travel</p>
                </div>
            </div>
            <div class="row g-4">
                <div class="col-md-4">
                    <div class="text-center p-4">
                        <div class="feature-icon">
                            <i class="fas fa-bolt"></i>
                        </div>
                        <h4>Instant Booking</h4>
                        <p class="text-muted">Book your travel tickets in just a few clicks with our intuitive platform.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="text-center p-4">
                        <div class="feature-icon">
                            <i class="fas fa-shield-alt"></i>
                        </div>
                        <h4>Secure Payments</h4>
                        <p class="text-muted">Your transactions are protected with industry-standard security measures.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="text-center p-4">
                        <div class="feature-icon">
                            <i class="fas fa-headset"></i>
                        </div>
                        <h4>24/7 Support</h4>
                        <p class="text-muted">Our customer support team is always ready to assist you.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Auth Section -->
    <section class="py-5 bg-light">
        <div class="container">
            <div class="row justify-content-center">
                <div class="col-lg-8">
                    <div class="row g-4">
                        <div class="col-md-6">
                            <div class="auth-card h-100">
                                <div class="auth-header">
                                    <h4><i class="fas fa-sign-in-alt me-2"></i>Existing User</h4>
                                </div>
                                <div class="auth-body">
                                    <p class="mb-4">Login to manage your bookings and access exclusive features.</p>
                                    <a href="login.jsp" class="btn btn-primary w-100">Login Now</a>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="auth-card h-100">
                                <div class="auth-header">
                                    <h4><i class="fas fa-user-plus me-2"></i>New User</h4>
                                </div>
                                <div class="auth-body">
                                    <p class="mb-4">Create an account to start booking travel tickets and enjoy our services.</p>
                                    <a href="register.jsp" class="btn btn-primary w-100">Register Now</a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

   
    <!-- Footer -->
    <footer id="contact">
        <div class="container">
            <div class="row">
                <div class="col-lg-4 mb-4 mb-lg-0">
                    <h5><i class="fas fa-train me-2"></i> TrackEase</h5>
                    <p class="mt-3">Your trusted partner for comfortable and reliable travel journeys across India.</p>
                    <div class="mt-4">
                        <a href="#" class="social-icon"><i class="fab fa-facebook-f"></i></a>
                        <a href="#" class="social-icon"><i class="fab fa-twitter"></i></a>
                        <a href="#" class="social-icon"><i class="fab fa-instagram"></i></a>
                        <a href="#" class="social-icon"><i class="fab fa-linkedin-in"></i></a>
                    </div>
                </div>
                <div class="col-lg-2 col-md-6 mb-4 mb-md-0">
                    <h5>Quick Links</h5>
                    <ul class="footer-links">
                        <li><a href="index.jsp">Home</a></li>
                        <li><a href="#features">Features</a></li>
                        <li><a href="#about">About</a></li>
                        <li><a href="#contact">Contact</a></li>
                    </ul>
                </div>
                <div class="col-lg-2 col-md-6 mb-4 mb-md-0">
                    <h5>Services</h5>
                    <ul class="footer-links">
                        <li><a href="login.jsp">Login</a></li>
                        <li><a href="register.jsp">Register</a></li>
                        <li><a href="#">Book Tickets</a></li>
                        <li><a href="#">Check PNR</a></li>
                    </ul>
                </div>
                <div class="col-lg-4">
                    <h5>Contact Us</h5>
                    <ul class="footer-links">
                        <li><i class="fas fa-map-marker-alt me-2"></i> Bangalore, Karnataka, India</li>
                        <li><i class="fas fa-phone me-2"></i> +91 9879879879</li>
                        <li><i class="fas fa-envelope me-2"></i> info@TrackEase.com</li>
                    </ul>
                </div>
            </div>
            <hr class="mt-4 mb-3" style="border-color: rgba(255,255,255,0.1);">
            <div class="row">
                <div class="col-md-12 text-center">
                    <p class="mb-0">&copy; 2025 TrackEase. All rights reserved.</p>
                </div>
            </div>
        </div>
    </footer>

    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>    