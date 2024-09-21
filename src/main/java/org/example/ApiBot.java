package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.*;

public class ApiBot extends TelegramLongPollingBot {
    private Survey currentSurvey;
    private final Set<Long> USERS;
    private final ScheduledExecutorService SCHEDULER;
    private ScheduledFuture<?> resultsTimer;
    private boolean isCreatingSurvey = false;
    private Long currentCreator;
    private final Map<Long, Set<String>> USERS_RESPONSES;

    @SuppressWarnings("deprecation")
    public ApiBot(){
        USERS = new HashSet<>();
        USERS_RESPONSES = new HashMap<>();
        SCHEDULER = Executors.newScheduledThreadPool(1);
    }
@Override
public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
        String messageText = update.getMessage().getText().trim().toLowerCase();
        long chatId = update.getMessage().getChatId();
        if (messageText.equals(Constants.START[0]) || (messageText.equalsIgnoreCase(Constants.START[1]))) {
            sendWelcomeMessage(chatId);
            handleJoin(update);
        } else if (messageText.equals(Constants.CREATE_SURVEY)) {
            if (USERS.size() < Constants.MIN_USERS) {
                sendMessage(chatId, Constants.ERROR_1);
            } else if (currentCreator != null) {
                sendMessage(chatId, Constants.ERROR_2);
            } else {
                currentCreator = chatId;
                createSurveyInstructions(chatId);
            }
        } else if (isCreatingSurvey && chatId == currentCreator) {
            checkSurveyInfo(chatId, messageText);
        } else if (currentSurvey != null) {
            processSurveyResponse(chatId, messageText);
        }
    }
}
    private void sendWelcomeMessage(long chatId) {
        sendMessage(chatId, Constants.WELCOME+"\nSend '"+Constants.CREATE_SURVEY+"' for creat a survey");
    }
    private void handleJoin(Update update) {
        long chatId = update.getMessage().getChatId();
        if (!USERS.contains(chatId)) {
            USERS.add(chatId);
            String welcomeMessage = Constants.USERS_AMOUNT + USERS.size();
            sendToAllMembers(welcomeMessage);
        }
    }

    private void createSurveyInstructions(long chatId) {
        isCreatingSurvey = true;
        sendMessage(chatId, Constants.SURVEY_FORMAT +"\n"+ Constants.SURVEY_REQUIREMENTS);
    }

    private void checkSurveyInfo(long chatId, String messageText) {
        if (messageText.matches(".*:.*(,.*)*;.*")) {
            String[] questionsOptionsArray = messageText.split(";");
            if (questionsOptionsArray.length > 3) {
                sendMessage(chatId, Constants.ERROR_3);
                return;
            }
            for (String questionOption : questionsOptionsArray) {
                String[] questionOptionParts = questionOption.split(":");
                if (questionOptionParts.length == 2) {
                    List<String> options = Arrays.asList(questionOptionParts[1].split(","));
                    if (options.size() < 2 || options.size() > 4) {
                        sendMessage(chatId, Constants.ERROR_4);
                        return;
                    }
                }
            }
            analyzeSurveyInfo(chatId, messageText);
            isCreatingSurvey = false;
        } else {
            sendMessage(chatId, Constants.INVALID_INPUT + ", " +Constants.SURVEY_FORMAT);
        }
    }

    private void analyzeSurveyInfo(long chatId, String messageText) {
        String[] questionsOptionsArray = messageText.split(";");
        List<String> questions = new ArrayList<>();
        Map<String, List<String>> optionsPerQuestion = new HashMap<>();

        for (String questionOption : questionsOptionsArray) {
            String[] questionOptionParts = questionOption.split(":");
            if (questionOptionParts.length == 2) {
                String question = questionOptionParts[0].trim();
                List<String> options = Arrays.asList(questionOptionParts[1].split(","));
                questions.add(question);
                optionsPerQuestion.put(question, options);
            }
        }
        createSurvey(chatId, questions, optionsPerQuestion);
    }

    private void createSurvey(long creatorChatId, List<String> questions, Map<String, List<String>> optionsPerQuestion) {
        this.currentSurvey = new Survey(questions, optionsPerQuestion, creatorChatId);
        surveyTimeOptions(creatorChatId);
    }

    private void surveyTimeOptions(long creatorChatId) {
        String surveyOptions = Constants.ASK_TIMING + "\n1. " + Constants.IMMEDIATELY +
                "\n2. In a " + Constants.DELAY + Constants.ASK_MINUTES;
        sendMessage(creatorChatId, surveyOptions);
    }

    private void processSurveyResponse(long chatId, String messageText) {
        if (messageText.equalsIgnoreCase(Constants.IMMEDIATELY)) {
            sendSurvey();
        } else if (messageText.startsWith(Constants.DELAY)) {
            try {
                int delayMinutes = Integer.parseInt(messageText.split(" ")[1]);
                SCHEDULER.schedule(this::sendSurvey, delayMinutes, TimeUnit.MINUTES);
            } catch (NumberFormatException e) {
                sendMessage(chatId, Constants.INVALID_INPUT + Constants.ASK_MINUTES);
            }
        } else {
            String[] responsePairs = messageText.split(";");
            boolean allResponsesValid = true;

            for (String responsePair : responsePairs) {
                String[] responseParts = responsePair.trim().split(":", 2);

                if (responseParts.length == 2) {
                    try {
                        int questionNumber = Integer.parseInt(responseParts[0].trim());
                        int answerNumber = Integer.parseInt(responseParts[1].trim());

                        if (questionNumber <= 0 || questionNumber > currentSurvey.getQUESTIONS().size()) {
                            sendMessage(chatId, Constants.ERROR_5 + questionNumber);
                            allResponsesValid = false;
                            continue;
                        }

                        String question = currentSurvey.getQUESTIONS().get(questionNumber - 1);
                        List<String> options = currentSurvey.getPossibleAnswersForQuestion(question);

                        if (answerNumber <= 0 || answerNumber > options.size()) {
                            sendMessage(chatId, Constants.ERROR_6 + questionNumber);
                            allResponsesValid = false;
                            continue;
                        }

                        String response = options.get(answerNumber - 1);
                        boolean isAdded = currentSurvey.addResponse(chatId, question, response);

                        if (isAdded) {
                            sendMessage(chatId, Constants.ANSWER_RECEIVED + questionNumber);

                            USERS_RESPONSES.putIfAbsent(chatId, new HashSet<>());
                            USERS_RESPONSES.get(chatId).add(question);
                        } else {
                            sendMessage(chatId, Constants.ERROR_7 + questionNumber);
                            allResponsesValid = false;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, Constants.INVALID_INPUT + Constants.ANSWER_FORMAT);
                        allResponsesValid = false;
                    }
                } else {
                    sendMessage(chatId, Constants.INVALID_INPUT + Constants.ANSWER_FORMAT);
                    allResponsesValid = false;
                }
            }

            if (allResponsesValid) {
                sendMessage(chatId, Constants.END_SURVEY);
                if (allUsersAnsweredAllQuestions()) {
                    if (resultsTimer != null && !resultsTimer.isDone()) {
                        resultsTimer.cancel(false);
                    }
                    sendSurveyResults();
                }
            }
        }
    }

    private boolean allUsersAnsweredAllQuestions() {
        if (currentSurvey == null) {
            return false;
        }
        int totalQuestions = currentSurvey.getQUESTIONS().size();
        for (Long userId : USERS) {
            Set<String> answeredQuestions = USERS_RESPONSES.get(userId);
            if (answeredQuestions == null || answeredQuestions.size() < totalQuestions) {
                return false;
            }
        }
        return true;
    }

    private void sendSurvey() {
        for (String question : currentSurvey.getQUESTIONS()) {
            StringBuilder message = new StringBuilder(question).append("\n");
            List<String> options = currentSurvey.getPossibleAnswersForQuestion(question);
            for (int i = 0; i < options.size(); i++) {
                message.append(i + 1).append(". ").append(options.get(i)).append("\n");
            }
            sendToAllMembers(message.toString());
        }
        resultsTimer = SCHEDULER.schedule(this::sendSurveyResults, 5, TimeUnit.MINUTES);
    }

    private void sendSurveyResults() {
        if (currentSurvey != null) {
            new SurveyResults(currentSurvey).analyzeSurveyResults(currentSurvey.getCHAT_IDS(), this);
            resetSurveyState();
        }
    }

    private void resetSurveyState() {
        currentSurvey = null;
        currentCreator = null;
        USERS_RESPONSES.clear();
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return Constants.BOT_USER_NAME;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getBotToken() {
        return Constants.BOT_TOKEN;
    }

    private void sendToAllMembers(String message) {
        for (Long memberId : USERS) {
            sendMessage(memberId, message);
        }
    }
}