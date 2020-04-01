# Bachelor Thesis

This is the repository for my bachelor thesis including programming code and written texts

## Current progress

Version 1 (01. April 2020):
* AI with API for detecting languages english, german and 4 others
* AI for sentiment analysis including
    * one API endpoint for evaluating the sentiment of a review
    * one API endpoint for adding reviews to the AI training data and
    * one API endpoint for removing accidentally added reviews from the AI training data
    * 200+ training reviews 


## How to use

The following section explains how to use the API for language detection and for sentiment analysis.
The API endpoints for adding and removing review training data is **password protected** and **not part of the service**.

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
      url: "https://api.ishift.de:8443/sentimentAnalysis/reviews/calcRating",
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
      url: "https://api.ishift.de:8081/sentimentAnalysis/reviews/calcRating",
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


### And coding style tests

Explain what these tests test and why

```
Give an example
```

## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Dropwizard](http://www.dropwizard.io/1.0.2/docs/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [ROME](https://rometools.github.io/rome/) - Used to generate RSS Feeds

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). 

## Authors

* **Billie Thompson** - *Initial work* - [PurpleBooth](https://github.com/PurpleBooth)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Hat tip to anyone whose code was used
* Inspiration
* etc

