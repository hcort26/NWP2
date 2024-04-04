package nwp;

import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

class ParticipantConnection implements Comparable<ParticipantConnection> {
    private Socket connectionSocket;
    private PrintWriter outputWriter;
    private BufferedReader inputReader;
    private String answerKey;
    private int points;
    private boolean isEligibleToAnswer;

    public ParticipantConnection(Socket conn) throws IOException {
        this.connectionSocket = conn;
        this.outputWriter = new PrintWriter(conn.getOutputStream(), true);
        this.inputReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        this.points = 0;
        this.isEligibleToAnswer = false;
        broadcastMessage("points: " + points);
    }

    public void defineCorrectAnswer(String ans) {
        this.answerKey = ans.substring(ans.length() - 1);
    }

    public void broadcastMessage(String msg) throws IOException {
        outputWriter.println(msg);
    }

    public Socket getConnection() {
        return this.connectionSocket;
    }

    public String getParticipantID() {
        return connectionSocket.getRemoteSocketAddress().toString();
    }

    public void receiveMessages() {
        try {
            String receivedMsg;
            while ((receivedMsg = inputReader.readLine()) != null) {
                System.out.println("Received from participant: " + receivedMsg);
                if (receivedMsg.startsWith("Points")) {
                    points -= Integer.parseInt(receivedMsg.substring("Points ".length()).trim());
                    broadcastMessage("points: " + points);
                    System.out.println("Participant did not answer in time");
                    QuizServer.advanceAllToNextQuestion();
                } else if (receivedMsg.startsWith("Timeout")) {
                    QuizServer.notifyTimeout(this);
                } else {
                    evaluateAnswer(receivedMsg);
                }
            }
        } catch (IOException ex) {
            try {
                System.out.println("Participant disconnected: " + connectionSocket.getRemoteSocketAddress());
                QuizServer.removeParticipant(this);
                if (isEligibleToAnswer) {
                    QuizServer.advanceAllToNextQuestion();
                }
                disconnect();
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
        }
    }

    private void evaluateAnswer(String participantAnswer) {
        participantAnswer = participantAnswer.trim().substring(0, 1);

        if (this.answerKey.equals(participantAnswer)) {
            System.out.println("Participant answered correctly.");
            points += 10;
            try {
                broadcastMessage("correct " + points);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Participant answered incorrectly.");
            points -= 10;
            try {
                broadcastMessage("incorrect " + points);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            QuizServer.advanceAllToNextQuestion();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int compareTo(ParticipantConnection other) {
        return Integer.compare(other.points, this.points);
    }

    public boolean canParticipantAnswer() {
        return isEligibleToAnswer;
    }

    public void setEligibleToAnswer(boolean status) {
        this.isEligibleToAnswer = status;
    }

    public int getPoints() {
        return points;
    }

    public void disconnect() throws IOException {
        if (inputReader != null) inputReader.close();
        if (outputWriter != null) outputWriter.close();
        if (connectionSocket != null) connectionSocket.close();
    }
}

