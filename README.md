# Krishav Singh, Fin Feakes Software Development Coursework 2026/27

## Requirements

- JDK: Java 25
- JUnit: JUnit 5.10.2, using JUnit  Standalone 1.10.2 to compile and run the tests.
- Build tool: No build tools were used for this coursework

Keep the `.java` source files together in one directory. Ensure that in the source directory, there is a folder called 'out' for the class files to be stored in for compilation.
Steps 1 and 2 relate to running the game itself. The remaining steps relate to the test suite.

## 1. Compile the program

To compile the game itself, run this command in the source directory:

```
javac Card.java CardDeck.java CardGame.java CustomFormat.java Player.java -d out 
```

This creates the `.class` files needed to run the game. You don't need JUnit to compile or run the game itself.

## 2. Run the program

In the same directory, run:

```
java -cp out CardGame
```

Follow the prompts in the console:

1. Enter the number of players. It must be an integer greater than 1.
2. Enter the path to the pack file when prompted.

The pack file must contain 8 times the number of players' lines. Each line must contain one non-negative  integer. For example, for 2 players, the file must contain 16 lines, for 4 players it should have 32, and so on. You can create a plain-text file such as pack.txt in the same directory and enter pack.txt when prompted.

The game will reject a pack if the file cannot be found, if it has the wrong number of lines, or if a line is not a non-negative integer. Once a valid pack is supplied, the game starts and prints the winner to the console.

## 3. Download JUnit for testing

Download junit-platform-console-standalone-1.10.2.jar from:

https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar

Save the JAR in the same directory as the source files and CardGameTest.java.

## 4. Compile the source and tests

On Windows, run the following:

```
javac -cp ".;junit-platform-console-standalone-1.10.2.jar" *.java -d out
```

On Linux or macOS, use this instead:

```
javac -cp ".:junit-platform-console-standalone-1.10.2.jar" *.java -d out
```

These commands compile the production source files and CardGameTest.java. The code given was tested of Java 25 so please make sure you use this.

## 5. Run the test suite

Run this command from the source directory:

```
java -jar junit-platform-console-standalone-1.10.2.jar execute --class-path out --select-class CardGameTest
```

The console displays the tests that ran and a summary of successful and failed tests, in the usual JUnit way.

