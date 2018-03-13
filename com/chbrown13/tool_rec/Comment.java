package com.chbrown13.tool_rec;

import com.jcabi.github.*;
import com.jcabi.http.*;
import com.jcabi.http.response.*;
import java.net.*;
import javax.json.*;

/**
 * Post comment for a tool recommendation on GitHub code changes
 */
public class Comment {		

    public static String compile = "sudo javac -cp jcabi-github-0.23-jar-with-dependencies.jar:error_prone_ant-2.1.0.jar:gumtree.jar:org.eclipse.jgit-4.9.0.201710071750-r.jar:jsch-0.1.46.jar:commons-email-1.3.1.jar:mail-1.4.7.jar com/chbrown13/tool_rec/ErrorProne.java com/chbrown13/tool_rec/Tool.java com/chbrown13/tool_rec/Error.java com/chbrown13/tool_rec/Recommender.java com/chbrown13/tool_rec/Utils.java com/chbrown13/tool_rec/Comment.java";
    public static String cmd = "java -cp .:jcabi-github-0.23-jar-with-dependencies.jar com.chbrown13.tool_rec.Comment {args}";
    public static String changes = "https://github.com/{user}/{repo}/{type}/{num}/";
    private static String commitPath = "/repos/{user}/{repo}/commits/{sha}/comments";
    
    /**
     * Posts comment to a commit
     * 
     * @param github   GitHub object
     * @param hash     Commit hash
     * @param comment  Tool recommendation comment
     * @param file     Path of file to comment on
     * @param line     Line number of fix to post comment on
     */
    private static void comment(RtGithub github, String hash, String comment, String file, int line) {
        path = commitPath.replace("{sha}", hash);
        try {
            final JsonStructure args = Json.createObjectBuilder()
                .add("body", comment)
                .add("path", file)
                .add("position", line).build();
            JsonResponse res = github.entry().method(Request.POST).uri()
                .path(path)
                .back().body().set(args).back().fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_CREATED)
                .as(JsonResponse.class);
            System.out.println(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Posts comment to a pull request
     * 
     * @param pull    Pull request with fixed issue
     * @param comment Tool recommendation comment
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
     * java -cp .:jcabi-github-0.23-jar-with-dependencies.jar com.chbrown13.tool_rec.Comment 
     *  user repo type pull# comment hash filepath line#
     * 
     * OR
     * 
     * java -cp .:jcabi-github-0.23-jar-with-dependencies.jar com.chbrown13.tool_rec.Comment 
     * user repo type hash comment filepath line#
     */
    public static void main(String[] args) {
        RtGithub github = new RtGithub("tool-recommender-bot", "bot-recommender-tool");
        Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
        if (args[2].equals(Recommender.PULL)) {
            Pull pull = repo.pulls().get(Integer.parseInt(args[3]));
            comment(new Pull.Smart(pull), args[4], args[5], args[6], Integer.parseInt(args[7]));
        } else if (args[2].equals(Recommender.COMMIT)) {
            commitPath = commitPath.replace("{user}", args[0]).replace("{repo}", args[1]);
            comment(github, args[3], args[4], args[5], Integer.parseInt(args[6]));
        }
    }
}
