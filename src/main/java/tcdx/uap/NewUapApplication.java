package tcdx.uap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})//禁用SpringBoot默认的Security
public class NewUapApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewUapApplication.class, args);
    }
}
