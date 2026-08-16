package com.skuri.skuri_backend;

import com.skuri.skuri_backend.common.time.ApplicationTimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class SkuriBackendApplication {

	public static void main(String[] args) {
		ApplicationTimeZone.initialize();
		SpringApplication.run(SkuriBackendApplication.class, args);
	}

}
