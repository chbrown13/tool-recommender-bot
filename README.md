# tool-recommender-bot
This bot is an automated recommendation system designed to increase awareness and adoption of software engineering tools among developers. tool-recommender-bot automatically creates pull requests adding static analysis tools to build configuration files for projects on GitHub. The initial implementation automatically recommends Error Prone, an open source static analysis tool for Java code, but we aim to extend this system to suggest a wide variety of development tools in the future. More information about this version tool-recommender-bot can be found in our paper ***"[Sorry to Bother You: Designing Bots for Effective Recommendations](https://chbrown13.github.io/dcbrow10/files/nudge/BotSE.pdf)"*** published at the [1st International Workshop on Bots in Software Engineering](http://botse.org/) in Montreal, Canada.

### Set Up:
* Dependencies:
	* Java
	* Maven
* Install required jar files: 
	* jcabi-github-0.38-jar-with-dependencies.jar
	* org.eclipse.jgit-4.9.0.201710071750-r.jar
	* snakeyaml-1.25.jar
	* slf4j.jar (optional)
* Create a .github.creds file with two lines, one that contains your github username and one with your password.


### Run:
```
$ sudo javac -cp jcabi-github-0.38-jar-with-dependencies.jar:org.eclipse.jgit-4.9.0.201710071750-r.jar:snakeyaml-1.25.jar com/chbrown13/tool_rec/ErrorProne.java com/chbrown13/tool_rec/Tool.java com/chbrown13/tool_rec/Error.java com/chbrown13/tool_rec/Recommender.java com/chbrown13/tool_rec/Utils.java
$ java -cp .:jcabi-github-0.38-jar-with-dependencies.jar:org.eclipse.jgit-4.9.0.201710071750-r.jar:snakeyaml-1.25.jar com.chbrown13.tool_rec.Recommender
```
