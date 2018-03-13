# tool-recommender-bot
tool-recommender-bot is an automated recommendation system designed to increase awareness and adoption of software engineering tools among developers. The initial implementation automatically recommends Error Prone, an open source static analysis tool for Java code, to GitHub users.

This branch shows our approach for searching for GitHub repositories to evaluate tool-recommender-bot.

Run from source code:
```
$ sudo javac -cp jcabi-github-0.23-jar-with-dependencies.jar com/chbrown13/tool_rec/Recommender.java
$ java -cp .:jcabi-github-0.23-jar-with-dependencies.jar com.chbrown13.tool_rec.Recommender
```
