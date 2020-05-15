// ==UserScript==
// @name         New Userscript
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        https://www.amazon.de/*
// @grant        none
// @require http://code.jquery.com/jquery-3.4.1.min.js
// @require      https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.3/moment.min.js
// ==/UserScript==

(function() {
    'use strict';
    console.log("Script by Nicolas Salomon is active");
    var baseUrl = "https://ishift.de";
    //var baseUrl = "https://localhost";
    moment.updateLocale('de', {
        monthsShort : [
            "Jan", "Feb", "Mar", "Apr", "Mai", "Jun",
            "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"
        ]
    });

    const scrapeInterval = 1000; // ms
    const maxReviewAmount = 3;

    // wait for elements to be loaded
    setTimeout(function() {
        if (window.location.href.includes("gp/profile")) {
            // for when there are more than ten reviews
            if (getUrlParam("afterDataFetch") == "true") {
                console.log(JSON.parse(localStorage.getItem("reviewData")));
                $.ajax({
                    method: "POST",
                    url: baseUrl + "/reviewerAnalysis/",
                    data: JSON.stringify({
                        reviews: JSON.parse(localStorage.getItem("reviewData"))
                    }),
                    success: function(response) {
                        console.log("response", response);
                    },
                    error: function(error) {
                        alert(error.responseJSON.message);
                    },
                    contentType: "application/json;charset=utf-8"
                });
            } else {
                $("html, body").animate({scrollTop: $(document).height() - $(window).height()});
                setTimeout(function () {
                    $("html, body").animate({scrollTop: 0});
                    reviewerAnalysis();
                }, 1000);
            }
        } else if (window.location.href.includes("gp/customer-reviews") && getUrlParam("openedByScript") == "true") {
            console.log("adding review to set");
            addSingleReviewToSet();
        } else {
            sentimentAnalysis();
        }
    }, 1000);

    function getUrlParam(param) {
        var queryString = window.location.search;
        var urlParams = new URLSearchParams(queryString);
        return urlParams.get(param);
    }

    $("ul.a-pagination").click(function() {
        // rescan if new reviews are loaded
        setTimeout(function() {
            // wait for new reviews to be loaded
            sentimentAnalysis();
        }, 1000);
    });

    function addSingleReviewToSet() {
        var timestamp = moment($(".review-date").text(), "DD MMM YYYY").unix();
        var datePreviousReview = getUrlParam("previousReview");
        var timeSincePreviousReview = datePreviousReview ? timestamp - parseInt(datePreviousReview) : null;

        var isPurchaseVerified = $("div.review-format-strip span.a-text-bold").length > 0;

        var reviewText = $(".review-text-content span").html();

        var ratingElement = $("i.review-rating");
        var ratingClass = ratingElement.attr("class");
        var rating = parseInt(ratingClass.match(/(\d+)/)[0]);

        var hasImage = $("div.review-image-tile-section").length > 0;

        var hasVideo = $("div.video-block").length > 0;

        var reviewData = JSON.parse(localStorage.getItem("reviewData"));
        if (!Array.isArray(reviewData)) {
            reviewData = [];
        }
        reviewData.push({
            timestamp: timestamp,
            rating: rating,
            reviewText: reviewText,
            timeSincePreviousReview: timeSincePreviousReview,
            hasImage: hasImage,
            hasVideo: hasVideo,
            isPurchaseVerified: isPurchaseVerified,
            persona: null
        });
        localStorage.setItem("reviewData", JSON.stringify(reviewData));
        var links = JSON.parse(localStorage.getItem("reviewLinks"))
        var link = links[0];
        if (link) {
            links.splice(0, 1);
            localStorage.setItem("reviewLinks", JSON.stringify(links));
            window.location.replace(link);
        } else {
            window.location.replace(localStorage.getItem("reviewerProfileLink"));
        }
    }

    function reviewerAnalysis() {
        $("div.name-container > span").append("<a class='a-link-normal check-persona'>Detect Persona</a>");

        $(".check-persona").click(function() {
            var reviews = $("div#profile-at-card-container > .profile-at-card");

            // Easier to get the lastReviewDate
            reviews = reviews.get().reverse();

            var lastReviewDate = null;
            var counter = 0;
            console.log("scanning " + reviews.length + " reviews with an interval of " + scrapeInterval + " ms");

            var links = [];

            $(reviews).each(function(index) {
                var link = $(this).find("a.profile-at-review-link")[0].href;
                if (!link.includes("?")) {
                    link += "?openedByScript=true";
                } else {
                    link += "&openedByScript=true";
                }
                if (lastReviewDate) {
                    link += "&previousReview=" + lastReviewDate;
                } else if (counter > 0) {
                    console.error("counter is greater than 0 but last review date is unknown");
                }
                links.push(link);

                lastReviewDate = moment($(this).find(".profile-at-user-info span.a-profile-descriptor").text(), "DD MMM YYYY").unix();
                counter++;

            });

            if (links.length > maxReviewAmount) {
                links.splice(0, links.length - maxReviewAmount);
            }

            localStorage.setItem("reviewData", JSON.stringify([]));

            var reviewerProfileLink = window.location.href;
            reviewerProfileLink += reviewerProfileLink.includes("?") ? "&afterDataFetch=true" : "?afterDataFetch=true";
            localStorage.setItem("reviewerProfileLink", reviewerProfileLink);

            var link = links[0];
            links.splice(0, 1);
            localStorage.setItem("reviewLinks", JSON.stringify(links));
            window.open(link, '_self');
        });
    }

    function sentimentAnalysis() {
        var reviews = $("div[id^='customer_review']");
        reviews.each(function(index) {
            var ratingElement = $(this).find("i.review-rating");
            var ratingClass = ratingElement.attr("class");
            var rating = parseInt(ratingClass.match(/(\d+)/)[0]);

            // .text() removes html elements like <br>. For review structure analysis use .html()
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
                var url = useElement.hasClass("do-use") ? baseUrl + "/sentimentAnalysis/reviews/" + password : baseUrl + "/sentimentAnalysis/delete/reviews/" + password
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
                    url: baseUrl + "/sentimentAnalysis/reviews/calcRating",
                    data: JSON.stringify({text: reviewText}),
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
