package com.reviewerAnalysis.model;

import java.util.LinkedList;
import java.util.List;

public class PersonaResponse {

    private int ignored;
    private List<Item> items;

    // maybe use int for in between values
    private boolean isSleeper;

    public PersonaResponse(int ignored) {
        this.ignored = ignored;
        items = new LinkedList<>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public class Item {

        private String persona;
        private double confidence;

        public Item(String persona, double confidence) {
            this.persona = persona;
            this.confidence = confidence;
        }
    }
}
