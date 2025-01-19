# AiFeeder

AiFeeder is an Android application designed to assist in maintaining your pet's feeding schedule. The app uses your phone’s camera to periodically check the bowl of an automatic pet feeder and ensures that it never runs out of food.

## Features

1. **Automated Bowl Monitoring**
   - The app periodically takes a picture of the pet feeder's bowl using the phone’s camera.
   - It uses the Gemini API to analyze the image and determine whether the bowl needs refilling.

2. **Automatic Food Dispensing**
   - If the Gemini API detects that the food is insufficient, the app triggers the feeder to dispense more food.

## Requirements

- **Gemini API Key**
  - Before running the app, add your Gemini API key to the `local.properties` file in the following format:
    ```
    GEMINI_API_KEY=xxx
    ```
    Replace `xxx` with your actual Gemini API key.

- **Network Request File**
  - The app requires a `feedPet.har` file in the project root directory to control the pet feeder.
  - Every time food needs to be dispensed, the app will send a network request identical to the one defined in the `feedPet.har` file.

## Installation

1. Clone the repository.
2. Add the `GEMINI_API_KEY` to the `local.properties` file.
3. Place the `feedPet.har` file in the project root directory.
4. Build and run the app on an Android device with a working camera.
