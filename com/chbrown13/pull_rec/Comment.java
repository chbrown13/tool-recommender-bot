package com.chbrown13.pull_rec;

import com.jcabi.github.*;

/**
 * Add comment to make a tool recommendation on pull request after review
 */
public class Comment {		

    private static void comment(Pull.Smart pull, String comment, String sha, String file, int line) {
        try {
            PullComments pullComments = pull.comments();
            PullComment.Smart smartComment = new PullComment.Smart(pullComments.post(comment, sha, file, line));	
            System.out.println(comment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //args: user repo pull# comment hash filepath line#
		RtGithub github = new RtGithub("tool-recommender-bot", "bot-recommender-tool");
        Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
        Pull pull = repo.pulls().get(Integer.parseInt(args[2]));
        comment(new Pull.Smart(pull), args[3], args[4], args[5], Integer.parseInt(args[6]));
    }
}