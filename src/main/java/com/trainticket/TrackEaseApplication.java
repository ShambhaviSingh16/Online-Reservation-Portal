package com.trainticket;

import javax.sql.DataSource;
import com.trainticket.util.DBUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.InitializingBean;

@SpringBootApplication(exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@ServletComponentScan
public class TrackEaseApplication {
    public static void main(String[] args) { SpringApplication.run(TrackEaseApplication.class, args); }
    @Bean InitializingBean configureLegacyDaos(DataSource dataSource) {
        return () -> DBUtil.configure(dataSource);
    }
}
