# PullRecommender

* Proof of concept for recommender tool. Used for research and to gather data on when/where we can expect recommendations on pull requests without actually commenting on the PRs.

Jenkins plugin to automatically recommend Google's Error Prone static analysis tool in pull requests for open source Java projects.

Run from source code:
```
* sudo javac -cp jcabi-github-0.23-jar-with-dependencies.jar:error_prone_ant-2.1.0.jar:gumtree.jar:$GUMTREE_HOME com/chbrown13/pull_rec/ErrorProneItem.java com/chbrown13/pull_rec/PullRecommender.java com/chbrown13/pull_rec/Utils.java 
* java -cp .:jcabi-github-0.23-jar-with-dependencies.jar:error_prone_ant-2.1.0.jar:gumtree.jar com.chbrown13.pull_rec.PullRecommender <owner> <repo> <min. pr#>
```
