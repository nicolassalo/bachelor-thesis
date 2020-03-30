package com.SentimentAnalysis.data;

import javax.persistence.*;

@Entity
public class Password {
    private static final int EXPIRATION_DEFAULT = 1000 * 60 * 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String password;

    public Password(String password) {
        this.password = password;
    }

    public Password() {}

    public String getPassword() {
        return password;
    }
}
