# java-chat-application
Client and admin side discord like chat application running as a java server.

Running instructions

*Building the application*
    Run in the root directory terminal "mvn clean verify" to build.
    Then still in the root directory run:

Terminal instructions

mvn clean package

*Server:*
    java -jar target/java_chat_app-1.0-SNAPSHOT.jar server
    java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar server

*Client:*
    java -jar target/java_chat_app-1.0-SNAPSHOT.jar client
    java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar client

To run the client in javafx
    mvn javafx:run "-Djavafx.run.args=client"
    java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar GUI

To access the http server page:
Enter http://localhost:8080/ into your web browser once the server is running.

Other instructions
*Client:*
    you can enter the command "quit" at any time to quit the client