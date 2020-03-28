package com.salomon.languagedetection.controller;

import com.salomon.languagedetection.LanguageDetector;
import com.salomon.languagedetection.model.ResponseMessage;
import opennlp.tools.langdetect.Language;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api")
public class LanguageDetectionController {

    /**
     * Returns the language for the sent text
     *
     * @param text A text for which the language shall be detected
     * @return HttpStatus.OK with result under key 'message'
     */
    @GetMapping("/language")
    public Language[] detectLanguage(@RequestParam String text) {
        return LanguageDetector.getInstance().detectLanguage(text);
    }
}

