# Multi-Player Trivia Game Project README

## Overview
This project involves the development of a multi-player trivia game that can be played over the network. The game incorporates various computer science concepts such as concurrency control, socket programming, GUI design, and network communication. The project is divided into two phases.

## Development Phases
1. **Phase 1 - Client Application Development:**
    - Utilization of provided classes (`ClientWindow.java` and `ClientWindowTest.java`) for GUI and event handling.
    - Implementation of client-side logic using Java Swing for user interactions.

2. **Phase 2 - Server Application Development:**
    - Designing a multi-threaded server capable of handling multiple client connections.
    - Implementing TCP and UDP socket communication for reliable and fast data transfer.

## Key Features
### Client Features:
1. GUI displaying questions and multiple-choice options.
2. Interaction through buttons and radio buttons for answering.
3. Timed responses with score and timer displays.

### Server Features:
1. Multi-threading for handling multiple clients.
2. TCP for sending questions and responses.
3. UDP for quick, non-guaranteed communication, primarily for buzzer-like interactions.
4. Queue implementation for managing message orders.
5. Server mechanisms for synchronization, deadlock prevention, and handling out-of-order messages.
6. A "Kill-Switch" for terminating clients under specific conditions.
7. A repository of 20 trivia questions and options.
8. Score tracking and final winner announcement.

## Requirements
1. **Client-Server Interaction:**
    - Clients receive questions over TCP.
    - Clients use a static identifier (`ClientID`) for communication.
    - Polling, answering, and scoring mechanisms with respective TCP and UDP communications.

2. **Server Responsiveness and Control:**
    - Handling of buzzers (UDP) and acknowledgments (TCP).
    - Decision-making process for sending responses to clients.
    - Mechanisms to prevent server deadlock or infinite waits.
    - Score tracking and end-of-game handling.

3. **Additional Considerations:**
    - Clients can join or leave anytime, with the server maintaining a list of active clients.
    - Documentation of any additional reasonable assumptions made during development.

## Technical Stack
- **Programming Language:** Java
- **Networking:** TCP/UDP Sockets
- **Concurrency:** Multi-threading
- **GUI:** Java Swing
