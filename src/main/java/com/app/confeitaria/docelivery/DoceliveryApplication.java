package com.app.confeitaria.docelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling; // ADICIONADO

@EntityScan("com.app.confeitaria.docelivery.model.entity")
@SpringBootApplication
@EnableScheduling // <-- ADICIONE ESTA ANOTAÇÃO AQUI
public class DoceliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoceliveryApplication.class, args);
	}
}