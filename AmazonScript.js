// ==UserScript==
// @name         New Userscript
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        https://www.amazon.de/*
// @grant        none
// @require http://code.jquery.com/jquery-3.4.1.min.js
// ==/UserScript==

(function() {
    'use strict';

    var baseUrl = "https://api.ishift.de";
    //var baseUrl = "https://localhost";

    scan();

    $("ul.a-pagination").click(function() {
        // rescan if new reviews are loaded
        setTimeout(function() {
            // wait for new reviews to be loaded
            scan();
        }, 1000);
    });

    function scan() {
        var reviews = $("div[id^='customer_review']");
        reviews.each(function(index) {
            var ratingElement = $(this).find("i.review-rating");
            var ratingClass = ratingElement.attr("class");
            var rating = parseInt(ratingClass.match(/(\d+)/)[0]);
            var reviewText = $(this).find(".review-text-content span").text();

            var data = {
                rating: rating,
                reviewText: reviewText
            };

            ratingElement.parent().parent().append("<a class='a-link-normal use do-use'>Use</a> <a class='a-link-normal check'>Check</a>");

            var useElement = $(this).find("a.use");
            function changeUseButton() {
                if (useElement.hasClass("do-use")) {
                    useElement.html("Undo");
                    useElement.removeClass("do-use").addClass("undo-use");
                } else {
                    useElement.html("Use");
                    useElement.removeClass("undo-use").addClass("do-use");
                }
            }
            useElement.click(function() {
                var password = getPassword();
                var url = useElement.hasClass("do-use") ? baseUrl + ":8443/api/reviews/" + password : baseUrl + ":8443/api/delete/reviews/" + password
                if (password) {
                    $.ajax({
                        method: "POST",
                        url: url,
                        data: JSON.stringify(data),
                        success: function(response) {
                            console.log("response", response);
                            changeUseButton();
                        },
                        error: function(error) {
                            alert(error.responseJSON.message);
                            if (error.status == 403) {
                                localStorage.removeItem("SentimentAnalysisAPIPassword");
                            }
                        },
                        contentType: "application/json;charset=utf-8"
                    });
                } else {
                    alert("Permission denied");
                }
            });

            var checkElement = $(this).find("a.check");
            checkElement.click(function() {
                $.ajax({
                    method: "POST",
                    url: baseUrl + ":8443/api/reviews/calcRating?text=" + reviewText,
                    //data: JSON.stringify(data),
                    success: function (response) {
                        alert("Rating should be " + response.rating);
                    },
                    error: function (error) {
                        alert(error.responseJSON.message);
                    },
                    contentType: "application/json;charset=utf-8"
                });

            });
        });
    }

    function getPassword() {
        var password = localStorage.getItem("SentimentAnalysisAPIPassword");
        if (password) {
            console.log("password", password);
            return password;
        } else {
            return promtPassword("Please enter your password for using the API");
        }
    }

    function promtPassword(text) {
        var password = prompt(text);
        if (password != null) {
            localStorage.setItem("SentimentAnalysisAPIPassword", password);
            console.log("password", password);
            return password;
        }
        return null;
    }
})();
