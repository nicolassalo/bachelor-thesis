package com.reviewerAnalysis;

import com.reviewerAnalysis.data.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class PersonaDetection {

    private static PersonaDetection instance = null;

    @Autowired
    PersonaRepository personaRepository;

    public static PersonaDetection getInstance() {
        if (instance == null) {
            instance = new PersonaDetection();
        }
        return instance;
    }

    private PersonaDetection() {}

    private String detectPersona() {
        return personaRepository.findById((long) Math.ceil(Math.random() * personaRepository.findAll().size())).get().getName();
    }
}
