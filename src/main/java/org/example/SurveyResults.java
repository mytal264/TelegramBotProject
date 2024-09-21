package org.example;

import java.util.*;

public class SurveyResults {
    private final Survey SURVEY;

    public SurveyResults(Survey survey) {
        this.SURVEY = survey;
    }

    public void analyzeSurveyResults(long chatId, ApiBot bot) {
        Map<String, Map<String, Integer>> results = SURVEY.getResults();
        StringBuilder resultMessage = new StringBuilder("\nSurvey results: ");
        for (String question : results.keySet()) {
            resultMessage.append("\n").append(question);
            Map<String, Integer> questionResults = results.get(question);
            questionResults.entrySet().stream()
                    .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                    .forEach(entry -> {
                        String option = entry.getKey();
                        int votes = entry.getValue();
                        resultMessage.append("\n").append(option).append(": ").append(votes).append(" votes");
                    });
        }
        bot.sendMessage(chatId, resultMessage.toString());
    }
}