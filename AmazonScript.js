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
    // Your code here...
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

            ratingElement.parent().parent().append("<a class='a-link-normal use'>Use</a>");

            var useElement = $(this).find("a.use");
            function removeElement() {
                useElement.html("");
            }
            useElement.click(function() {
                $.ajax({
                    method: "POST",
                    url: "http://localhost:8080/api/reviews",
                    data: JSON.stringify(data),
                    success: function(response) {
                        console.log("response", response);
                        removeElement();
                    },
                    error: function(error) {
                        alert(error.responseJSON.message);
                    },
                    contentType: "application/json;charset=utf-8"
                });
            });
        });
    }
})();
