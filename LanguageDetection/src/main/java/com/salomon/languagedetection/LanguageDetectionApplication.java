package com.salomon.languagedetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class LanguageDetectionApplication {

	public static void main(String[] args) {
		SpringApplication.run(LanguageDetectionApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void initialize() {
		LanguageDetector.getInstance().trainModel();
	}
}
