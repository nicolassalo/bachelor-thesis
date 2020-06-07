package com.reviewerAnalysis.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Password {

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
