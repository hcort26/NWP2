package nwp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

class QuizServer {
    private static final int serverPort = 12345;
    private static List<QuestionAndAnswer> questionList;
    private static int currentQuestion = 0;
    private static boolean acceptingBuzzes = true;
    private static List<ParticipantConnection> participants = new ArrayList<>();
    public static int countTimeouts = 0;
    private static boolean displayedWinners = false;

    public static void main(String[] args) {
        questionList = new ArrayList<>();
        try {
            loadQuestionsFromFile("QA.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try (ServerSocket serverSock = new ServerSocket(serverPort)) {
            System.out.println("Server up and running. Awaiting connections...");

            BuzzListener buzzListener = new BuzzListener();
            buzzListener.start();

            while (true) {
                Socket clientSock = serverSock.accept();
                ParticipantConnection newParticipant = new ParticipantConnection(clientSock);
                participants.add(newParticipant);
                System.out.println("Participant joined: " + clientSock.getRemoteSocketAddress().toString());

                new Thread(() -> {
                    try {
                        notifyParticipantOfCurrentQuestion(newParticipant);
                        newParticipant.receiveMessages();
                    } catch (IOException e) {
                        System.out.println("Error handling participant connection.");
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            System.out.println("Server encountered an error.");
            e.printStackTrace();
        }
    }

    private static class BuzzListener extends Thread {
        private DatagramSocket ds;
        private boolean active;
        private byte[] buffer = new byte[256];

        public BuzzListener() throws SocketException {
            this.ds = new DatagramSocket(serverPort);
        }

        public void run() {
            active = true;
            while (active) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    ds.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    System.out.println("Received: " + received + " from: " + address.getHostAddress() + ":" + port);
                    if (acceptingBuzzes) {
                        acceptingBuzzes = false;
                        if (participants.isEmpty()) {
                            System.out.println("No participants connected.");
                        } else {
                            ParticipantConnection matchingParticipant = null;
                            for (ParticipantConnection pc : participants) {
                                if (pc.getConnection().getInetAddress().equals(address)) {
                                    matchingParticipant = pc;
                                    break;
                                }
                            }
                            if (matchingParticipant != null) {
                                System.out.println("Acknowledging buzz from " + address.getHostAddress());
                                try {
                                    matchingParticipant.broadcastMessage("ACK");
                                    setParticipantTimer("10", matchingParticipant);
                                    matchingParticipant.setEligibleToAnswer(true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                System.out.println("No matching participant for " + address.getHostAddress());
                            }
                        }
                    } else {
                        ParticipantConnection matchingParticipant = null;
                        for (ParticipantConnection pc : participants) {
                            if (pc.getConnection().getInetAddress().equals(address)) {
                                matchingParticipant = pc;
                                break;
                            }
                        }
                        if (matchingParticipant != null) {
                            System.out.println("Sending NAK to " + address.getHostAddress());
                            try {
                                matchingParticipant.broadcastMessage("NAK");
                                System.out.println("NAK sent to " + address.getHostAddress());
                            } catch (IOException e) {
                                System.out.println(matchingParticipant.getConnection() + " has closed");
                            }
                        } else {
                            System.out.println("No TCP participant match for " + address.getHostAddress());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ds.close();
        }
    }

    public static void loadQuestionsFromFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) throw new FileNotFoundException();

        Scanner fileReader = new Scanner(file);
        while (fileReader.hasNextLine()) {
            String q = fileReader.nextLine();
            if (!q.isEmpty()) {
                List<String> options = new ArrayList<>();
                options.add(fileReader.nextLine());
                options.add(fileReader.nextLine());
                options.add(fileReader.nextLine());
                options.add(fileReader.nextLine());
                String correctAns = fileReader.nextLine();
                questionList.add(new QuestionAndAnswer(q, options, correctAns));
            }
        }
        fileReader.close();
    }

    private static void notifyParticipantOfCurrentQuestion(ParticipantConnection pc) throws IOException {
        if (currentQuestion < questionList.size()) {
            setParticipantTimer("15", pc);
            QuestionAndAnswer currentQA = questionList.get(currentQuestion);
            String questionInfo = "Q" + currentQA.toString();
            try {
                pc.broadcastMessage(questionInfo);
            } catch (Exception e) {
                System.out.println(pc.getConnection() + " has closed");
            }

            pc.defineCorrectAnswer(currentQA.getCorrectAnswer());
        } else {
            System.out.println("Quiz over.");
            if (!displayedWinners) {
                announceWinners();
                displayedWinners = true;
            }

            try {
                pc.broadcastMessage("END");
            } catch (Exception e) {
                System.out.println(pc.getConnection() + " has closed");
            }
        }
    }

    public static void advanceAllToNextQuestion() throws IOException {
        countTimeouts = 0;
        triggerTimersForAllParticipants("15");
        acceptingBuzzes = true;
        currentQuestion++;
        List<ParticipantConnection> safeList = new ArrayList<>(participants);

        for (ParticipantConnection pc : safeList) {
            notifyParticipantOfCurrentQuestion(pc);
            pc.setEligibleToAnswer(false);
        }
    }

    public static synchronized void removeParticipant(ParticipantConnection pc) {
        participants.remove(pc);
    }

    public static void triggerTimersForAllParticipants(String time) throws IOException {
        for (ParticipantConnection pc : participants) {
            try {
                pc.broadcastMessage("Time " + time);
            } catch (Exception e) {
                System.out.println(pc.getConnection() + " has closed");
            }
        }
    }

    public static void setParticipantTimer(String time, ParticipantConnection pc) throws IOException {
        try {
            pc.broadcastMessage("Time " + time);
        } catch (Exception e) {
            System.out.println(pc.getConnection() + " has closed");
        }
    }

    public static synchronized void notifyTimeout(ParticipantConnection pc) {
        countTimeouts++;
        if (countTimeouts >= participants.size()) {
            System.out.println("All participants timed out");
            countTimeouts = 0;
            try {
                if (currentQuestion < questionList.size()) {
                    advanceAllToNextQuestion();
                } else {
                    try {
                        pc.broadcastMessage("END");
                    } catch (Exception e) {
                        System.out.println(pc.getConnection() + " has closed");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void announceWinners() {
        if (participants.isEmpty()) {
            System.out.println("No participants in the quiz.");
            return;
        }
        Collections.sort(participants);
        System.out.println("Winner:");
        System.out.println(participants.get(0).getParticipantID() + " with points: " + participants.get(0).getPoints());

        System.out.println("Final Points:");
        for (ParticipantConnection pc : participants) {
            System.out.println(pc.getParticipantID() + ": " + pc.getPoints());
        }
    }
}

