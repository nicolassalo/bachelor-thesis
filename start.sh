#!/bin/bash

cd LanguageDetection;
mvn spring-boot:run & echo $! > ./pid.file &
cd ../SentimentAnalysis;
mvn spring-boot:run & echo $! > ./pid.file &
cd ../ReviewerAnalysis;
mvn spring-boot:run & echo $! > ./pid.file &

