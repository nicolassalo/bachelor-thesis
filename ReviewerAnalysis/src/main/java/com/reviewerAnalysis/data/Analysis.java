package com.reviewerAnalysis.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String first;

    private String second;

    private String third;

    private String fourth;

    private String fifth;

    private String sixth;

    private String last;

    private double firstResult;

    private double secondResult;

    private double thirdResult;

    private double fourthResult;

    private double fifthResult;

    private double sixthResult;

    private double lastResult;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    public double getFirstResult() {
        return firstResult;
    }

    public void setFirstResult(double firstResult) {
        this.firstResult = firstResult;
    }

    public double getSecondResult() {
        return secondResult;
    }

    public void setSecondResult(double secondResult) {
        this.secondResult = secondResult;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public double getLastResult() {
        return lastResult;
    }

    public void setLastResult(double lastResult) {
        this.lastResult = lastResult;
    }

    public String getThird() {
        return third;
    }

    public void setThird(String third) {
        this.third = third;
    }

    public String getFourth() {
        return fourth;
    }

    public void setFourth(String fourth) {
        this.fourth = fourth;
    }

    public String getFifth() {
        return fifth;
    }

    public void setFifth(String fifth) {
        this.fifth = fifth;
    }

    public String getSixth() {
        return sixth;
    }

    public void setSixth(String sixth) {
        this.sixth = sixth;
    }

    public double getThirdResult() {
        return thirdResult;
    }

    public void setThirdResult(double thirdResult) {
        this.thirdResult = thirdResult;
    }

    public double getFourthResult() {
        return fourthResult;
    }

    public void setFourthResult(double fourthResult) {
        this.fourthResult = fourthResult;
    }

    public double getFifthResult() {
        return fifthResult;
    }

    public void setFifthResult(double fifthResult) {
        this.fifthResult = fifthResult;
    }

    public double getSixthResult() {
        return sixthResult;
    }

    public void setSixthResult(double sixthResult) {
        this.sixthResult = sixthResult;
    }

    public Analysis(String first, String second, String third, String fourth, String fifth, String sixth, String last, double firstResult, double secondResult, double thirdResult, double fourthResult, double fifthResult, double sixthResult, double lastResult) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
        this.sixth = sixth;
        this.last = last;
        this.firstResult = firstResult;
        this.secondResult = secondResult;
        this.thirdResult = thirdResult;
        this.fourthResult = fourthResult;
        this.firstResult= fifthResult;
        this.sixthResult = sixthResult;
        this.lastResult = lastResult;
    }

    public Analysis() {}

}
