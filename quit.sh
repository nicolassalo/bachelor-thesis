#!/bin/bash

cd LanguageDetection;
kill $(cat ./pid.file)
cd ../SentimentAnalysis;
kill $(cat ./pid.file)

