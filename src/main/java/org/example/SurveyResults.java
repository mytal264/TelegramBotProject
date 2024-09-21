package org.example;

import java.util.*;

public class SurveyResults {
    private final Survey SURVEY;

    public SurveyResults(Survey survey) {
        this.SURVEY = survey;
    }

    public void analyzeSurveyResults(long chatId, ApiBot bot) {
        Map<String, Map<String, Integer>> results = SURVEY.getResults();
        StringBuilder resultMessage = new StringBuilder("\nSurvey Results:\n");

        for (String question : results.keySet()) {
            resultMessage.append("\n").append(question).append(":\n");

            Map<String, Integer> questionResults = results.get(question);
            int totalVotes = questionResults.values().stream().mapToInt(Integer::intValue).sum();

            List<Map.Entry<String, Integer>> sortedResults = new ArrayList<>(questionResults.entrySet());

            sortedResults.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

            for (Map.Entry<String, Integer> entry : sortedResults) {
                String option = entry.getKey();
                int votes = entry.getValue();
                double percentage = (totalVotes > 0) ? ((double) votes / totalVotes) * 100 : 0;
                resultMessage.append(String.format("%s: %.2f%% (%d votes)\n", option, percentage, votes));
            }
        }
        bot.sendMessage(chatId, resultMessage.toString());
    }
}