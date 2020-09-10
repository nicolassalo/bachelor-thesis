package com.controller;

import com.services.LanguageDetector;
import com.model.TextModel;
import opennlp.tools.langdetect.Language;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/languageDetection")
public class LanguageDetectionController {

    /**
     * Returns the language for the sent text
     *
     * @param text A text for which the language shall be detected
     * @return HttpStatus.OK with result under key 'message'
     */
    @PostMapping("/detect")
    public Language[] detectLanguage(@Valid @RequestBody TextModel text) {
        return LanguageDetector.getInstance().detectLanguage(text.getText());
    }
}

