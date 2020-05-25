# Reviewer Analysis

This is the repository for my bachelor thesis including programming code and written texts.

The purpose of this software project is to categorize an author of amazon reviews into predefined personas using machine learning.

## Current progress

Version 2 (25. May 2020)
* Tampermonkey script for creating a user interface for using the API with real data
* Main Software finished including
    * APIs for adding and removing review training data
    * 1 AI using weka library for detecting the persona for a given review
    * 1 AI using natural language processing for detecting the persona for a given review

Version 1 (01. April 2020):
* AI with API for detecting languages english, german and 4 others
* AI for sentiment analysis including
    * one API endpoint for evaluating the sentiment of a review
    * one API endpoint for adding reviews to the AI training data and
    * one API endpoint for removing accidentally added reviews from the AI training data
    * 200+ training reviews 
* currently supporting only german reviews. Requests with reviews in another language return an error.


## How to use

The following section explains how to use the API for language detection and for sentiment analysis.
The API endpoints for adding and removing review training data is **password protected** and **not part of the service**.

Every API is being used in the AmazonScript, which is located in the root of this repository.

#### Reviewer analysis API

* **URL**

  https://api.ishift.de/reviewerAnalysis/

* **Method:**
  
  `POST`
  
*  **URL Params**

   None 

* **Data Params**

  `{reviews: [ReviewModel]}`
  
    The ReviewModel should contain the following keys
    * `rating` a number from 1 to 5
    * `reviewText` the html representation of the review text including \<br\> tags
    * `timeSincePreviousReview` the time since the previous review in seconds
    * `hasImage` true or false
    * `hasVideo` true or false
    * `isPurchaseVerified` true or false

* **Success Response:**
  
  * **Code:** 200 <br />
    **Content:** 
    ```
    {
        "ignored": <0..n>,
            "personasByReviewVariables": [
                {
                    "persona": string,
                    "confidence": <0..1>
                }
            ],
            "personasByLanguageProcessing": [
                {
                    "persona": string,
                    "confidence": <0..1>
                }
            ],
            "result": {
                    "persona": string,
                    "confidence": <0..1>
            },
            "activeness": <1..10>,
            "elaborateness": <1..10>
    }
    ```
 

* **Sample Call:**

  ```
  $.ajax({
      method: "POST",
      url: "https://api.ishift.de/reviewerAnalysis/",
      data: JSON.stringify(
          {
              reviews: [
                  {
                      rating: 5,
                      reviewText: "Habe Sockelleisten, welche ich mir aus Dachlatten gefräst 4x gestrichen.<br>2x mit diesen Pinseln, im Gegensatz zu den anderen Pinseln musste ich hier keine Borsten entfernen.<br>Bin sehr zufrieden mit den Teilen, bei der recht dickflüssigen, fast zähen Farbe waren sie top.",
                      timeSincePreviousReview: 0,
                      hasPicture: false,
                      hasVideo: false,
                      isPurchaseVerified: true
                  }
              ]
          }
      ),
      success: function (response) {
          console.log(response)
      },
      error: function (error) {
          alert(error.responseJSON.message);
      },
      contentType: "application/json;charset=utf-8"
  });
  ``` 

#### Sentiment analysis API

* **URL**

  https://api.ishift.de/sentimentAnalysis/reviews/calcRating

* **Method:**
  
  `POST`
  
*  **URL Params**

   None 

* **Data Params**

  `{text: <reviewText>}`

* **Success Response:**
  
  * **Code:** 200 <br />
    **Content:** 
    ```
    {
        rating: <1..5>, 
        languageConfidence: <0..1>
    }
    ```
 
* **Error Response:**

  * **Code:** 400 BAD REQUEST <br />
    **Content:** `{message: "Language currently not supported"}`

  OR

  * **Code:** 400 BAD REQUEST <br />
    **Content:** `{message: "Not sure that this is really german. Only with a confidence of 78 %."}`

* **Sample Call:**

  ```
  $.ajax({
      method: "POST",
      url: "https://api.ishift.de/sentimentAnalysis/reviews/calcRating",
      data: JSON.stringify({text: "Super Produkt"}),
      success: function (response) {
          alert("Rating should be " + response.rating);
      },
      error: function (error) {
          alert(error.responseJSON.message);
      },
      contentType: "application/json;charset=utf-8"
  });
  ``` 

#### Language detection API

* **URL**

  http://api.ishift.de/languageDetection/detect

* **Method:**
  
  `POST`
  
*  **URL Params**

   None 

* **Data Params**

  `{text: <reviewText>}`

* **Success Response:**
  
  * **Code:** 200 <br />
    **Content:** 
    ```
    [
        {
            "lang": "de",
            "confidence": 0.9999760016291265
        },
        {
            "lang": "en",
            "confidence": 2.1571782002703068E-5
        },
        {
            "lang": "pob",
            "confidence": 2.426314759896866E-6
        },
        {
            "lang": "spa",
            "confidence": 2.3112546832111294E-10
        },
        {
            "lang": "ita",
            "confidence": 4.280121662280208E-11
        },
        {
            "lang": "fra",
            "confidence": 1.8423835064068247E-13
        }
    ]
    ```
 
* **Error Response:**

  * **Code:** 400 BAD REQUEST (empty request body) <br />
    **Content:** 
    ```
    {
        "timestamp": "2020-04-01T19:42:55.145+0000",
        "status": 400,
        "error": "Bad Request",
        "message": "Required request body is missing: public opennlp.tools.langdetect.Language[] com.salomon.languagedetection.controller.LanguageDetectionController.detectLanguage(com.salomon.languagedetection.model.TextModel)",
        "path": "/languageDetection/detect"
    }
    ```

* **Sample Call:**

  ```
  $.ajax({
      method: "POST",
      url: "http://api.ishift.de/languageDetection/detect",
      data: JSON.stringify({text: "Dieser Text ist deutsch"}),
      success: function (response) {
          console.log(response);
      },
      error: function (error) {
          alert(error.responseJSON.message);
      },
      contentType: "application/json;charset=utf-8"
  });
  ``` 

## Deployment

### Spring-boot application

System must have Java, PostgreSQL and Maven installed.


### Amazon TamperMonkey script

For using the [AmazonScript](https://github.com/nicolassalo/bachelor-thesis/blob/master/AmazonScript.js), you need to have [Tampermonkey](https://www.tampermonkey.net/) installed in your browser.
In order to add the script, you have to go to the tampermonkey dashboard, open a new script, replace the default content with the Javascript of the file and press Save.
To make sure it worked, go to amazon.de and open the developer console. It should log "Script by Nicolas Salomon is active".

## Built With

* [Spring Boot](https://spring.io/projects/spring-boot) - REST API Framework
* [Maven](https://maven.apache.org/) - Dependency Management
* [PostgreSQL](https://www.postgresql.org/) - Database Management

## Author

**Nicolas Salomon** - [Github](https://github.com/nicolassalo/)

