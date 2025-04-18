# java-chat-application
Client and admin side discord like chat application running as a java server.

Running instructions

To compile:
 1. Navigate to the project root (where src/ is):
 cd "C:\Users\sonny bell\OneDrive\OneDrive - York St John University\Year 2\Programming 04\java-chat-application\java_chat_app"
 2. Compile to target folder:
 javac -d target src/main/java/com/sonnybell/java_chat_app/*.java

Then From the same root folder:
 1. Run server
    java -cp target/classes com.sonnybell.java_chat_app.server
 2. Run client
    java -cp target/classes com.sonnybell.java_chat_app.Client

