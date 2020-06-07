package com.reviewerAnalysis.data;

import javax.persistence.*;
import java.util.Map;

@Entity
public class Stats {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String lang;

    private double wekaAccuracy;

    private double nlpAccuracy;

    @ElementCollection
    @JoinTable(name="weka_persona_accuracies", joinColumns=@JoinColumn(name="stats_id"))
    @MapKeyColumn (name="persona")
    @Column(name="accuracy")
    private Map<String, Double> wekaPersonaAccuracies;

    @ElementCollection
    @JoinTable(name="nlp_persona_accuracies", joinColumns=@JoinColumn(name="stats_id"))
    @MapKeyColumn (name="persona")
    @Column(name="accuracy")
    private Map<String, Double> nlpPersonaAccuracies;

    public Stats(String lang, double wekaAccuracy, double nlpAccuracy, Map<String, Double> personaAccuracies, Map<String, Double> nlpPersonaAccuracies) {
        this.lang = lang;
        this.wekaAccuracy = wekaAccuracy;
        this.nlpAccuracy = nlpAccuracy;
        this.wekaPersonaAccuracies = personaAccuracies;
        this.nlpPersonaAccuracies = nlpPersonaAccuracies;
    }

    public Stats() {}

    public Long getId() {
        return id;
    }

    public String getLang() {
        return lang;
    }

    public double getWekaAccuracy() {
        return wekaAccuracy;
    }


    public double getNlpAccuracy() {
        return nlpAccuracy;
    }


    public Map<String, Double> getWekaPersonaAccuracies() {
        return wekaPersonaAccuracies;
    }


    public Map<String, Double> getNlpPersonaAccuracies() {
        return nlpPersonaAccuracies;
    }
}
