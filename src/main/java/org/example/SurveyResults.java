package org.example;

import java.util.*;

public class SurveyResults {
    private final Survey SURVEY;

    public SurveyResults(Survey survey) {
        this.SURVEY = survey;
    }

    public void analyzeAndDisplayResults(long chatId, ApiBot bot) {
        Map<String, Map<String, Integer>> results = SURVEY.getResults();

        StringBuilder resultMessage = new StringBuilder("תוצאות הסקר:\n");

        for (String question : results.keySet()) {
            resultMessage.append("\n").append(question).append("\n");
            Map<String, Integer> questionResults = results.get(question);

            // מיון התוצאות מהתשובה עם הכי הרבה הצבעות ועד להכי פחות
            questionResults.entrySet()
                    .stream()
                    .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                    .forEach(entry -> {
                        String option = entry.getKey();
                        int votes = entry.getValue();
                        resultMessage.append(option).append(": ").append(votes).append(" הצבעות\n");
                    });
        }

        bot.sendMessage(chatId, resultMessage.toString());
    }
}