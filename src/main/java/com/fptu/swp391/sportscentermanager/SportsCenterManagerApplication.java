package com.fptu.swp391.sportscentermanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

//@SpringBootApplication
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SportsCenterManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsCenterManagerApplication.class, args);
    }

}
