package org.example;

import java.util.*;

public class Survey {
    private final List<String> QUESTIONS;
    private final Map<String, List<String>> POSSIBLE_ANSWERS;
    private final Map<Long, Map<String, String>> USERS_RESPONSES = new HashMap<>();
    private final long CHAT_IDS;

    public Survey(List<String> questions, Map<String, List<String>> optionsPerQuestion, long creatorChatId) {
        this.QUESTIONS = questions;
        this.POSSIBLE_ANSWERS = optionsPerQuestion;
        this.CHAT_IDS = creatorChatId;
    }

    public List<String> getQUESTIONS() {
        return QUESTIONS;
    }

    public List<String> getOptionsForQuestion(String question) {
        return POSSIBLE_ANSWERS.get(question);
    }

    public boolean addResponse(long userId, String question, String response) {
        if (!USERS_RESPONSES.containsKey(userId)) {
            USERS_RESPONSES.put(userId, new HashMap<>());
        }

        if (POSSIBLE_ANSWERS.containsKey(question)
                && POSSIBLE_ANSWERS.get(question).contains(response.toLowerCase())
                && !USERS_RESPONSES.get(userId).containsKey(question)) {
            USERS_RESPONSES.get(userId).put(question, response.toLowerCase());
            return true;
        }

        return false;
    }

    public Map<String, Map<String, Integer>> getResults() {
        Map<String, Map<String, Integer>> results = new HashMap<>();
        for (String question : QUESTIONS) {
            results.put(question, new HashMap<>());
            for (String option : POSSIBLE_ANSWERS.get(question)) {
                results.get(question).put(option, 0);
            }
        }

        for (Map<String, String> responses : USERS_RESPONSES.values()) {
            for (Map.Entry<String, String> entry : responses.entrySet()) {
                String question = entry.getKey();
                String response = entry.getValue();
                results.get(question).put(response, results.get(question).get(response) + 1);
            }
        }

        return results;
    }

    public int getTotalVotes() {
        return USERS_RESPONSES.size();
    }

    public long getCHAT_IDS() {
        return CHAT_IDS;
    }
}