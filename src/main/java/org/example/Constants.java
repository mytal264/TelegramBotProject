package org.example;

public class Constants {
        public static final String BOT_USER_NAME = "Gossip0824Bot";
        public static final String BOT_TOKEN = "6897103543:AAHw5Uu8oZHI_XgSZfl9G4ewpezmEF5Prnk";

        public static final int MIN_USERS = 3;

        public static final String[] START = {"/start","Hi"};
        public static final String CREATE_SURVEY = "/createsurvey";
        public static final String WELCOME = "Welcome to the community!";
        public static final String USERS_AMOUNT = "A new member has joined! Community size: ";
        public static final String ASK_TIMING = "When do you want to send the survey?";
        public static final String IMMEDIATELY = "Immediately";
        public static final String DELAY = "delay";
        public static final String ASK_MINUTES = ", write how many minutes to wait.";
        public static final String SURVEY_FORMAT = "Please enter the survey questions and options in the format: q1:a1,a2,a3; q2:a1,a2,a3;";
        public static final String SURVEY_REQUIREMENTS = "(1-3 questions, for each question 2-4 possible answers)";
        public static final String ANSWER_FORMAT = ", Use the format: question_number: answer_number;";
        public static final String ANSWER_RECEIVED = "Response has been received for question number: ";
        public static final String END_SURVEY = "All your responses have been successfully received.";
        public static final String INVALID_INPUT = "Invalid input";
        public static final String ERROR_1 = "You need at least 3 members in the community to create a survey.";
        public static final String ERROR_2 = "Another user is currently creating a survey. Please wait.";
        public static final String ERROR_3 = "You can only have up to 3 questions in the survey.";
        public static final String ERROR_4 = "Each question must have between 2 and 4 answers. Please try again.";
        public static final String ERROR_5 = "Invalid question number: ";
        public static final String ERROR_6 = "Invalid answer number for question number: ";
        public static final String ERROR_7 = "Invalid answer or you have already responded to question number: ";

}