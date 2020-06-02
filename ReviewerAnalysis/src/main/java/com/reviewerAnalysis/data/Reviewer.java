package com.reviewerAnalysis.data;

import javax.persistence.*;
import java.util.List;

@Entity
public class Reviewer {
    private static final int EXPIRATION_DEFAULT = 1000 * 60 * 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany
    private List<Review> reviews;

    public Reviewer(List<Review> reviews) {
        this.reviews = reviews;
    }

    public Reviewer() {}

    public List<Review> getReviews() {
        return reviews;
    }

    public Long getId() {
        return this.id;
    }
}
