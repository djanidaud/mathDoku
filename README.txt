How to compile and run your code from the command line:

If you are running Java 11 or later, you will need to download the JavaFX libraries separately.Now to compile the application, simply type:
javac --module-path=(path to javaFX folder)/javafx-sdk-11.0.2/lib --add-modules=ALL-MODULE-PATH Main.java
Then you can run the application using:
java --module-path=(path to javaFX folder)/javafx-sdk-11.0.2/lib --add-modules=ALL-MODULE-PATH Main

