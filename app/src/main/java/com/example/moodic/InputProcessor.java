package com.example.moodic;

public class InputProcessor {


    // Process the raw input based on the input type
    public static String processInput(String rawInput, String inputType) {
        switch (inputType) {
            case "mood":
                return processMoodInput(rawInput);
            case "genre":
                return processGenreInput(rawInput);
            case "track":
                return processTrackInput(rawInput);
            default:
                return "Invalid input type";
        }
    }

    // Example: Process mood input
    private static String processMoodInput(String rawInput) {
        // Assume rawInput is a string representing mood (e.g., "happy", "sad", "neutral")
        switch (rawInput.toLowerCase()) {
            case "happy":
                return "Positive mood detected";
            case "sad":
                return "Negative mood detected";
            case "neutral":
                return "Neutral mood detected";
            default:
                return "Unrecognized mood";
        }
    }

    // Example: Process genre input
    private static String processGenreInput(String rawInput) {
        // Process raw genre input, e.g., if rawInput contains "pop", "rock", etc.
        if (rawInput.equalsIgnoreCase("pop") || rawInput.equalsIgnoreCase("rock")) {
            return "Favorite genre: " + rawInput;
        } else {
            return "Unknown genre: " + rawInput;
        }
    }

    // Example: Process track input (you can modify this if necessary)
    private static String processTrackInput(String rawInput) {
        // Assume rawInput is a string representing a track name
        if (rawInput != null && !rawInput.isEmpty()) {
            return "Track added: " + rawInput;
        } else {
            return "Invalid track input";
        }
    }

    // Determine final mood or input outcome (simplified)
    public static String determineFinalMood(String processedMoodInput) {
        if (processedMoodInput.contains("Positive")) {
            return "User is in a good mood";
        } else if (processedMoodInput.contains("Negative")) {
            return "User is in a bad mood";
        } else {
            return "Mood is neutral";
        }
    }
}
