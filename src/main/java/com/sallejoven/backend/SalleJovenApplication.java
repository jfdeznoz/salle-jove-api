package com.sallejoven.backend;

import com.sallejoven.backend.config.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.sallejoven.backend.config.security.RSAKeyRecord;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, RSAKeyRecord.class})
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class SalleJovenApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalleJovenApplication.class, args);
	}

}
