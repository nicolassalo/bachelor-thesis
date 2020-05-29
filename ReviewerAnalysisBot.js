// ==UserScript==
// @name         Reviewer Analysis Bot
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  try to take over the world!
// @author       You
// @match        https://www.amazon.de/*
// @grant        none
// @require      http://code.jquery.com/jquery-3.4.1.min.js
// ==/UserScript==

(function() {
    'use strict';
    console.log("Scrape bot by Nicolas Salomon is active");

    if (localStorage.getItem("reviewerProfiles") == null) {
        localStorage.setItem("reviewerProfiles", JSON.stringify([]));
    }

    if (getUrlParam("reviewerType") == "all_reviews") {
        $("body").append("<div style='position: fixed; bottom: 0; right: 50%; background-color: white' class='save-links'><button>Save links</button></div>");
        $(".save-links").click(function() {
            collect();
        });
    }

    var counter = 0;
    function collect() {
        $("#cm_cr-review_list div[id^='customer_review']").each(function() {
            var link = $(this).find("a.a-profile")[0].href;
            var links = JSON.parse(localStorage.getItem("reviewerProfiles"));
            if (!links.includes(link)) {
                links.push(link);
                localStorage.setItem("reviewerProfiles", JSON.stringify(links));
                counter++;
            }
        })
        if ($("#cm_cr-pagination_bar li.a-last a").length > 0) {
            $("#cm_cr-pagination_bar li.a-last a")[0].click();
            setTimeout(function() {
                collect();
            }, 500);
        } else {
            console.log("Added", counter + " links");
            counter = 0;
        }
    }

    function getUrlParam(param) {
        var queryString = window.location.search;
        var urlParams = new URLSearchParams(queryString);
        return urlParams.get(param);
    }

    function run() {

    }
})();
