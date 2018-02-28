# tool-recommender-bot
Jenkins plugin to automatically recommend Google's Error Prone static analysis tool in pull requests for open source Java projects.

Set Up:
* Install required jar files 
	* error_prone_ant-2.1.0.jar 
	* gumtree.jar 
	* jcabi-github-0.23-jar-with-dependencies.jar
	* org.eclipse.jgit-4.9.0.201710071750-r.jar
	* jsch-0.1.46.jar
	* commons-email-1.3.1.jar
	* mail-1.4.7.jar
	* slf4j.jar (optional)
* Create a .github.creds file with two lines, one that contains your github username and one with your password.


Run from source code:
```
$ sudo javac -cp jcabi-github-0.23-jar-with-dependencies.jar:error_prone_ant-2.1.0.jar:gumtree.jar:org.eclipse.jgit-4.9.0.201710071750-r.jar:jsch-0.1.46.jar:commons-email-1.3.1.jar:mail-1.4.7.jar com/chbrown13/pull_rec/ErrorProne.java com/chbrown13/pull_rec/Tool.java com/chbrown13/pull_rec/Error.java com/chbrown13/pull_rec/PullRecommender.java com/chbrown13/pull_rec/Utils.java com/chbrown13/pull_rec/Comment.java
$ java -cp .:jcabi-github-0.23-jar-with-dependencies.jar:error_prone_ant-2.1.0.jar:gumtree.jar:org.eclipse.jgit-4.9.0.201710071750-r.jar:jsch-0.1.46.jar:commons-email-1.3.1.jar:mail-1.4.7.jar com.chbrown13.pull_rec.PullRecommender <owner> <project>
```
