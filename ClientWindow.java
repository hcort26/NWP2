package nwp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.TimerTask;
import java.util.Timer;

class ClientWindow implements ActionListener {
    private JFrame mainFrame;
    private JLabel questionLabel, timerLabel, pointsLabel;
    private JButton requestButton, answerButton;
    private JRadioButton[] answerOptions;
    private ButtonGroup optionsGroup;
    private TimerTask countdownTask;
    private String hostAddress;
    private int hostPort = 12345;
    private static boolean readyToAnswer = false;
    private Socket participantSocket;

    public ClientWindow() {
        hostAddress = JOptionPane.showInputDialog("Please input the host's IP address:");

        if (hostAddress == null || hostAddress.isEmpty()) {
            System.out.println("IP address not provided. Terminating application...");
            return;
        }

        setupUserInterface();
        try {
            participantSocket = new Socket(hostAddress, hostPort);
            System.out.println("Successfully connected to the quiz server.");
            mainFrame.setTitle("Successfully Established Connection to " + hostAddress);
            listenToSocket(participantSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupUserInterface() {
        mainFrame = new JFrame("Code Quiz Game");

        questionLabel = new JLabel("Question 1: Sample Query");
        questionLabel.setBounds(10, 5, 600, 100);
        mainFrame.add(questionLabel);

        answerOptions = new JRadioButton[4];
        optionsGroup = new ButtonGroup();
        for (int i = 0; i < answerOptions.length; i++) {
            answerOptions[i] = new JRadioButton("Answer " + (i + 1));
            answerOptions[i].addActionListener(this);
            answerOptions[i].setBounds(10, 110 + (i * 20), 350, 20);
            answerOptions[i].setEnabled(false);
            mainFrame.add(answerOptions[i]);
            optionsGroup.add(answerOptions[i]);
        }

        timerLabel = new JLabel("Remaining Time");
        timerLabel.setBounds(250, 250, 100, 20);
        countdownTask = new CountdownTimer(30);
        Timer quizTimer = new Timer();
        quizTimer.scheduleAtFixedRate(countdownTask, 0, 1000);
        mainFrame.add(timerLabel);

        pointsLabel = new JLabel("Your Points");
        pointsLabel.setBounds(50, 250, 100, 20);
        mainFrame.add(pointsLabel);

        requestButton = new JButton("Poll");
        requestButton.setBounds(10, 300, 100, 20);
        requestButton.addActionListener(this);
        mainFrame.add(requestButton);

        answerButton = new JButton("Send Answer");
        answerButton.setBounds(200, 300, 100, 20);
        answerButton.addActionListener(this);
        answerButton.setEnabled(readyToAnswer);
        mainFrame.add(answerButton);

        mainFrame.setSize(700, 400);
        mainFrame.setBounds(50, 50, 700, 400);
        mainFrame.setLayout(null);
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Poll":
                try {
                    if (!readyToAnswer) {
                        byte[] buf = "PollRequest".getBytes();
                        InetAddress address = InetAddress.getByName(hostAddress);
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, hostPort);
                        DatagramSocket ds = new DatagramSocket();
                        ds.send(packet);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                break;
            case "Send Answer":
                String selectedOption = null;
                for (JRadioButton option : answerOptions) {
                    if (option.isSelected()) {
                        selectedOption = option.getText();
                        break;
                    }
                }
                if (selectedOption != null) {
                    System.out.println("Selected Answer: " + selectedOption);
                    submitAnswer(selectedOption);
                }
                break;
        }
    }

    private void listenToSocket(Socket sock) throws IOException {
        BufferedReader socketReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String receivedData;
        while ((receivedData = socketReader.readLine()) != null) {
            handleSocketMessage(receivedData);
        }
        socketReader.close();
    }

    private void handleSocketMessage(String msg) {
        if (msg.startsWith("Q")) {
            parseQuestion(msg.substring(1));
            requestButton.setEnabled(true);
        } else if (msg.equals("ACK")) {
            System.out.println("ACK received");
            readyToAnswer = true;
            answerButton.setEnabled(true);
            for (JRadioButton option : answerOptions) {
                option.setEnabled(true);
                Color color = new Color(0,250,0);
                requestButton.setForeground(color);
            }
        } else if (msg.equals("NAK")) {
            System.out.println("NAK received");
        } else if (msg.startsWith("correct")) {
            String scoreUpdate = msg.split(" ")[1];
            readyToAnswer = false;
            answerButton.setEnabled(false);
            for (JRadioButton option : answerOptions) {
                option.setEnabled(false);
                Color color = new Color(250,0,0);
                requestButton.setForeground(color);
            }
            optionsGroup.clearSelection();
            pointsLabel.setText("Points: " + scoreUpdate);
        } else if (msg.startsWith("incorrect")) {
            String scoreUpdate = msg.split(" ")[1];
            readyToAnswer = false;
            answerButton.setEnabled(false);
            for (JRadioButton option : answerOptions) {
                option.setEnabled(false);
            }
            optionsGroup.clearSelection();
            pointsLabel.setText("Points: " + scoreUpdate);
        } else if (msg.startsWith("points")) {
            String scoreUpdate = msg.split(": ")[1];
            pointsLabel.setText("Points: " + scoreUpdate);
        } else if (msg.startsWith("END")) {
            questionLabel.setForeground(Color.red);
            questionLabel.setText("The game has ended! Please see your final scores below.");
            requestButton.setEnabled(false);
            countdownTask.cancel();
        } else if (msg.startsWith("Time")) {
            int newTime = Integer.parseInt(msg.split(" ")[1]);
            resetCountdownTimer(newTime);
        }
    }

    private void parseQuestion(String questionInfo) {
        String[] parts = questionInfo.split("\\[");
        String questionPart = parts[0];
        String choices = questionInfo.substring(questionPart.length() + 1, questionInfo.length() - 1);
        String questionNum = questionPart.split("\\.")[0].trim();
        String questionText = questionPart.substring(questionNum.length() + 1).trim();
        updateAnswerOptions(questionNum, questionText, choices);
    }

    private void updateAnswerOptions(String qNum, String qText, String optionsPart) {
        questionLabel.setText(qNum + ". " + qText);

        String[] optionSet = optionsPart.split(", ");
        for (int i = 0; i < this.answerOptions.length && i < optionSet.length; i++) {
            this.answerOptions[i].setText(optionSet[i].trim());
        }
    }

    private void submitAnswer(String answer) {
        try {
            PrintWriter socketWriter = new PrintWriter(participantSocket.getOutputStream(), true);
            socketWriter.println(answer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetCountdownTimer(int newTime) {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        countdownTask = new CountdownTimer(newTime);
        Timer resetTimer = new Timer();
        resetTimer.scheduleAtFixedRate(countdownTask, 0, 1000);
    }

    public static void main(String[] args) {
        new ClientWindow();
    }

    public class CountdownTimer extends TimerTask {
        private int timeLeft;

        public CountdownTimer(int time) {
            this.timeLeft = time;
        }

        @Override
        public void run() {
            if (timeLeft < 0) {
                timerLabel.setText("Out of time!");
                mainFrame.repaint();
                this.cancel();
                answerButton.setEnabled(false);
                requestButton.setEnabled(false);
                for (JRadioButton option : answerOptions) {
                    option.setEnabled(false);
                }
                optionsGroup.clearSelection();
                if (readyToAnswer) {
                    submitAnswer("Points 20");
                } else {
                    submitAnswer("Timeout");
                    countdownTask.cancel();
                }
                readyToAnswer = false;
                return;
            }

            if (timeLeft < 6)
                timerLabel.setForeground(Color.red);
            else
                timerLabel.setForeground(Color.black);

            timerLabel.setText(String.valueOf(timeLeft));
            timeLeft--;
            mainFrame.repaint();
        }
    }
}