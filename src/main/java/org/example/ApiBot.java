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
    private boolean isCreatingSurvey = false;

    @SuppressWarnings("deprecation")
    public ApiBot(){
        USERS = new HashSet<>();
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
                startSurveyCreation(chatId);
            } else if (isCreatingSurvey) {
                processSurveyCreationStep(chatId, messageText);
            } else if (currentSurvey != null) {
                processSurveyResponse(chatId, messageText);
            }
        }
    }
    private void sendWelcomeMessage(long chatId) {
        sendMessage(chatId, "Welcome to the community!");
    }
    private void handleJoin(Update update) {
        long chatId = update.getMessage().getChatId();
        if (!USERS.contains(chatId)) {
            USERS.add(chatId);
            String welcomeMessage = "A new member has joined! Community size: " + USERS.size();
            sendToAllMembers(welcomeMessage);
        }
    }

    private void startSurveyCreation(long chatId) {
        isCreatingSurvey = true;
        sendMessage(chatId, "Please enter the survey questions and options in the format: q1: a1,a2,a3;q2: a1,a2,a3;");
    }

    private void processSurveyCreationStep(long chatId, String messageText) {
        if (messageText.matches(".*:.*(,.*)*;.*")) {
            processCreateSurveyCommand(chatId, messageText);
            isCreatingSurvey = false;
        } else {
            sendMessage(chatId, "Invalid format. Please use the format: q1: a1,a2,a3;q2: a1,a2,a3;");
        }
    }

    private void processCreateSurveyCommand(long chatId, String messageText) {
        if (USERS.isEmpty()) {
            sendMessage(chatId, Constants.ERROR_1);
            return;
        }

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
        Survey survey = new Survey(questions, optionsPerQuestion, creatorChatId);
        this.currentSurvey = survey;
        sendSurveyOptions(creatorChatId);
    }

    private void sendSurveyOptions(long creatorChatId) {
        String surveyOptions = "\nWhen do you want to send the survey?\n1. " + Constants.IMMEDIATELY +
                "\n2. In a " + Constants.DELAY + " (write how many minutes to wait)";
        sendMessage(creatorChatId, surveyOptions);
    }

    private void processSurveyResponse(long chatId, String messageText) {
        if (messageText.equals(Constants.IMMEDIATELY)) {
            sendSurveyToAllMembers();
        } else if (messageText.startsWith(Constants.DELAY)) {
            try {
                int delayMinutes = Integer.parseInt(messageText.split(" ")[1]);
                SCHEDULER.schedule(this::sendSurveyToAllMembers, delayMinutes, TimeUnit.MINUTES);
            } catch (NumberFormatException e) {
                sendMessage(chatId, Constants.INVALID_INPUT + ", please enter the amount of minutes.");
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
                            sendMessage(chatId, "Invalid question number: " + questionNumber);
                            allResponsesValid = false;
                            continue;
                        }

                        String question = currentSurvey.getQUESTIONS().get(questionNumber - 1);
                        List<String> options = currentSurvey.getOptionsForQuestion(question);

                        if (answerNumber <= 0 || answerNumber > options.size()) {
                            sendMessage(chatId, "Invalid answer number for question " + questionNumber + ": " + answerNumber);
                            allResponsesValid = false;
                            continue;
                        }

                        String response = options.get(answerNumber - 1);
                        boolean isAdded = currentSurvey.addResponse(chatId, question, response);

                        if (isAdded) {
                            sendMessage(chatId, "Response for question " + questionNumber + " has been received.");
                        } else {
                            sendMessage(chatId, "Invalid answer or you have already responded to question " + questionNumber + ".");
                            allResponsesValid = false;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "Invalid format. Use the format: question_number: answer_number;");
                        allResponsesValid = false;
                    }
                } else {
                    sendMessage(chatId, "Invalid format. Use the format: question_number: answer_number;");
                    allResponsesValid = false;
                }
            }

            if (allResponsesValid) {
                sendMessage(chatId, "All your responses have been successfully received.");
                if (currentSurvey.getTotalVotes() == USERS.size()) {
                    new SurveyResults(currentSurvey).analyzeAndDisplayResults(currentSurvey.getCHAT_IDS(), this);
                    currentSurvey = null;
                }
            }
        }
    }

    private void sendSurveyToAllMembers() {
        for (String question : currentSurvey.getQUESTIONS()) {
            StringBuilder message = new StringBuilder(question).append("\n");
            List<String> options = currentSurvey.getOptionsForQuestion(question);

            for (int i = 0; i < options.size(); i++) {
                message.append(i + 1).append(". ").append(options.get(i)).append("\n");
            }
            sendToAllMembers(message.toString());
        }
        SCHEDULER.schedule(() -> new SurveyResults(currentSurvey).analyzeAndDisplayResults(currentSurvey.getCHAT_IDS(), this), 5, TimeUnit.MINUTES);
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