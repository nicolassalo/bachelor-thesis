package com.reviewerAnalysis.data;

import javax.persistence.*;
import java.util.List;

@Entity
public class Reviewer {

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
