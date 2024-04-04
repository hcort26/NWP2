package nwp;

import java.util.List;

class QuestionAndAnswer {
    private String questionText;
    private List<String> answerChoices;
    private String correctAnswer;

    public QuestionAndAnswer(String q, List<String> opts, String correct) {
        this.questionText = q;
        this.answerChoices = opts;
        this.correctAnswer = correct;
    }

    public String getQuestion() {
        return questionText;
    }

    public List<String> getOptions() {
        return answerChoices;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    @Override
    public String toString() {
        return questionText + answerChoices.toString();
    }
}

