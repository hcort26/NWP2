package nwp;

import java.util.List;

class QuestionAndAnswer {
	
	private List<String> answerChoices;
	
    private String correctAnswer;
    private String questionText;
    
    public QuestionAndAnswer(String q, List<String> opts, String correct) {
        this.questionText = q;
        this.answerChoices = opts;
        this.correctAnswer = correct;
    }

    public String getQuestion() {
        return questionText;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getOptions() {
        return answerChoices;
    }

    @Override
    public String toString() {
        return questionText + answerChoices.toString();
    }
}

