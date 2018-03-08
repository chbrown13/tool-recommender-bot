package com.chbrown13.pull_rec;

import com.jcabi.github.*;

/**
 * Add comment to make a tool recommendation on pull request after review
 */
public class Comment {		

    public static String compile = "sudo javac -cp jcabi-github-0.23-jar-with-dependencies.jar com/chbrown13/pull_rec/Comment.java";
    public static String cmd = "java -cp .:jcabi-github-0.23-jar-with-dependencies.jar com.chbrown13.pull_rec.Comment {args}";
    public static String changes = "https://github.com/{user}/{repo}/pull/{num}/files";
    /**
     * Posts comment to a pull request on Github
     * 
     * @param pull    Pull request with fixed issue
     * @param connent Comment to post on PR
     * @param hash    Commit hash for pull request
     * @param file    Path of file to comment on
     * @param line    Line number of fix to post comment on
     */
    private static void comment(Pull.Smart pull, String comment, String hash, String file, int line) {
        try {
            PullComments pullComments = pull.comments();
            PullComment.Smart smartComment = new PullComment.Smart(pullComments.post(comment, hash, file, line));	
            System.out.println("Recommendation comment posted.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * sudo javac -cp jcabi-github-0.23-jar-with-dependencies.jar com/chbrown13/pull_rec/Comment.java 
     * java -cp .:jcabi-github-0.23-jar-with-dependencies.jar com.chbrown13.pull_rec.Comment user repo
     *      pull# hash filepath line#
     */
    public static void main(String[] args) {
		RtGithub github = new RtGithub("<username>", "<password>");
        Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
        Pull pull = repo.pulls().get(Integer.parseInt(args[2]));
        comment(new Pull.Smart(pull), args[3], args[4], args[5], Integer.parseInt(args[6]));
    }
}