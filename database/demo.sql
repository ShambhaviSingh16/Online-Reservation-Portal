-- Local demo only. Passwords use the same PBKDF2 encoding as registration.
INSERT INTO users(username,password,email,full_name,role) VALUES
 ('admin','pbkdf2$sha256$600000$J0z9hGMchUiS0m2rYJIItw==$/99txzJu+mZvdl3um2zxjt2xxvfmWGd3tA9KkwA6tFQ=','admin@trackease.local','Demo Administrator','admin'),
 ('demo','pbkdf2$sha256$600000$i/uTztsZ9zX0UfkOJmkYbg==$uNqkWX4qtf1VoTH/JtgW0Jqlhg3rDJjPJdGs7jfDXRM=','demo@trackease.local','Demo Passenger','user')
 ON CONFLICT DO NOTHING;
INSERT INTO stations(station_code,station_name,city,state) VALUES
 ('NDLS','New Delhi','New Delhi','Delhi'),('CNB','Kanpur Central','Kanpur','Uttar Pradesh')
 ON CONFLICT DO NOTHING;
INSERT INTO trains(train_number,train_name,total_seats,created_by)
 SELECT '12034','TrackEase Demo Express',80,user_id FROM users WHERE username='admin'
 ON CONFLICT DO NOTHING;
INSERT INTO journeys(train_id,departure_station_id,arrival_station_id,departure_time,arrival_time,fare,available_seats)
 SELECT t.train_id,d.station_id,a.station_id,CURRENT_DATE + INTERVAL '1 day 8 hours',CURRENT_DATE + INTERVAL '1 day 13 hours',550,80
 FROM trains t,stations d,stations a
 WHERE t.train_number='12034' AND d.station_code='NDLS' AND a.station_code='CNB'
 AND NOT EXISTS (SELECT 1 FROM journeys j WHERE j.train_id=t.train_id AND DATE(j.departure_time)=CURRENT_DATE+1);
