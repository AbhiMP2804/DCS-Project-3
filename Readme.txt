📨 Persistent and Asynchronous Multicast System
A Java-based implementation of a persistent and asynchronous multicast communication system built on a coordinator-participant model. This project highlights advanced concepts in distributed systems, focusing on multicast messaging, asynchronous communication, and temporally-bound persistence.

🚀 Key Features
Multicast Coordinator: Manages the multicast group, orchestrates communication, and stores messages for persistence.
Multicast Participants: Send and receive messages asynchronously through the coordinator.
Asynchronous Messaging: Non-blocking message delivery—senders aren’t held up waiting for all recipients.
Temporally-Bound Persistence: Offline participants receive missed messages within a specified time threshold when they reconnect.
🧑‍💻 Supported Participant Commands
register [portnumber]: Join the multicast group.
deregister: Leave the group.
disconnect: Temporarily go offline.
reconnect [portnumber]: Rejoin the group and retrieve missed messages.
msend [message]: Send a multicast message to all active participants.
🛠️ Technical Details
Language: Java
Communication Protocol: TCP for reliable message transmission.
Concurrency: The coordinator manages multiple participant connections concurrently.
Multithreading: Participants handle command inputs and message reception in parallel.
🧩 System Components
1. Coordinator
Manages the multicast group.
Handles participant registration, deregistration, and reconnection.
Stores messages for offline participants (within a set time threshold).
Ensures message delivery according to temporal restrictions.
2. Participant
Each participant is assigned a unique ID.
Two dedicated threads:
Thread A: Manages user commands.
Thread B: Receives multicast messages from the coordinator.
Logs all received messages to a file for persistence.
⚙️ Configuration
Coordinator Configuration
Port number for incoming messages.
Persistence time threshold (in seconds).
Participant Configuration
Participant ID.
Message log file name.
Coordinator's IP address and port number.
💡 Key Implementation Insights
Guarantees no message loss within the defined persistence window.
Prevents message delivery beyond the time threshold.
Ensures no duplicate message delivery.
Manages participant re-registration seamlessly.
Assumes the coordinator remains active throughout the system operation.
