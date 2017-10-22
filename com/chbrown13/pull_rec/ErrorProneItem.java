package com.chbrown13.pull_rec;

//import com.google.errorprone.*;
//import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.util.*;

/**
 * The ErrorProneItem class contains methods concerning the ErrorProne static analysis tool and an object storing information for a bug reported.
 */
public class ErrorProneItem {

	private String key;
	private String filename;
	private String filepath;
	private int line;
	private String error;
	private String message;
	private String log;

	public ErrorProneItem(String name, String path, String line, String error, String msg, String log) {
		this.key = String.join(":", path, line, error);
		this.filename = name;
		this.filepath = path;
		this.line = Integer.parseInt(line);
		this.error = error;
		this.message = msg;
		this.log = log;
	}

	/**
	 * Creates comment to recommend ErrorProne for a Github pull request.
	 *
	 * @return   Message with fixed error and EP recommendation
	 */
	public String generateComment(List<ErrorProneItem> other, String sha) {
		String comment = Utils.BASE_COMMENT;
		String simSentence = " Error Prone also found similar issues in {link}. ";
		comment = comment.replace("{fixed}", "```" + this.log + "```");
		Set<ErrorProneItem> similarSet = new HashSet<ErrorProneItem>();
		ErrorProneItem sim;
		for (ErrorProneItem epi: other) {
			if ((!epi.getKey().equals(this.getKey()) && !epi.getFileName().equals(this.getFileName())) && 
				(epi.getError().equals(this.getError()) || epi.getMessage().equals(this.getMessage()))) {
				similarSet.add(epi);
			}
		}
		Iterator<ErrorProneItem> iter = similarSet.iterator();
		if (similarSet.isEmpty()) {
			comment = comment.replace("{similar}", " ");
		} else if (similarSet.size() == 1) {
				comment = comment.replace("{similar}", simSentence.replace("{link}", getErrorLink(iter.next(), sha)));
		} else {
			comment = comment.replace("{similar}", simSentence.replace("{link}", String.join(" and ", getErrorLink(iter.next(), sha), getErrorLink(iter.next(), sha))));
		}
		return comment;
	}

	/**
	 * Gets url to link to similar errors found by Error Prone in recommendation.
	 *
	 * @param epi   ErrorProneItem with error similar to fixed error
	 * @param hash  Git commit hash
	 * @return      Url to line with a similar error
	 */
	private String getErrorLink(ErrorProneItem epi, String hash) {
		String url = Utils.LINK_URL.replace("{line}", epi.getLineNumberStr()).replace("/{path}", Utils.getLocalPath(epi.getFilePath())).replace("{sha}", hash).replace("{repo}", Utils.getProjectName()).replace("{user}", Utils.getProjectOwner());
		return Utils.MARKDOWN_LINK.replace("{src}", epi.getFileName()).replace("{url}", url);
	}
	
	/**
	 * Gets ErrorProne static analysis output for current error
	 *
	 * @return   ErrorProne error message
	 */
	public String getLog() {
		return this.log;
	}
	
	/**
	 * Appends ErrorProne output to the log.
	 *
 	 * @param log   Message to add to log
	 */
	public void addLog(String log) {
		this.log += "\n"+log;
	}
	
	/**
	 * Gets key to uniquely identify ErrorProneItems
	 *
	 * @return   String key for ErrorProneItem
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Gets filepath to where error was reported.
	 *
 	 * @return   String filepath
	 */
	public String getFilePath() {
		return this.filepath;
	}

	/**
	 * Sets the filepath for ErrorProne error.
	 *
	 * @param path   Filepath to error
	 */
	public void setFilePath(String path) {
		this.filepath = path;
	}

	/**
	 * Gets filename of file containing ErrorProne error reported.
	 *
 	 * @return   String filepath
	 */
	public String getFileName() {
		return this.filename;
	}

	/**
	 * Sets the filename for ErrorProne error.
	 *
	 * @param path   Filename of file with error
	 */
	public void setFileName(String file) {
		this.filename = file;
	}

	/**
	 * Gets the error message from ErrorProne analysis output.
	 *
	 * @return   Error message
	 */
	public String getMessage() {
		return this.message;
	}
	
	/**
	 * Gets the name of the error reported in ErrorProne output.
	 *
	 * @return   Error name
	 */
	public String getError() {
		return this.error;
	}

	/**
	 * Gets the line number in file where error was found.
	 *
	 * @return   Line number
	 */
	public int getLineNumber() {
		return this.line;
	}

	/**
	 * Gets line number where error was found as a string.
	 *
	 * @return   String representation of line number
	 */
	public String getLineNumberStr() {
		return Integer.toString(this.line);
	}

	/**
	 * Parses output from ErrorProne static analysis of code and creates objects.
	 *
	 * @param msg    ErrorProne output
	 * @return       List of ErrrorProneItems
	  */
	public static Set<ErrorProneItem> parseOutput(String msg) {
		String regex = "^[\\w+/]*\\w.java\\:\\d+\\:.*\\:.*";
		String[] lines = msg.split("\n");
		ErrorProneItem temp = null;
		Set<ErrorProneItem> set = new HashSet<ErrorProneItem>();
		for (String line: lines) {
			if (line.matches("^\\d+\\serror[s]*$")) {
				continue;
			}
			else if (line.matches(regex)) {
				if (temp != null && !set.contains(temp)) {	set.add(temp); }
				String[] error = line.split(":");
				String errorFilePath = error[0];
				String errorFileName = errorFilePath.substring(errorFilePath.lastIndexOf("/")+1);
				String errorLine = error[1];
				String errorProne;
				if (error[3].contains("[")) {
					errorProne = error[3].substring(error[3].indexOf("[")+1,error[3].indexOf("]"));
				} else {
					errorProne = error[2].trim();
				}
				String errorMessage = error[3];
				temp = new ErrorProneItem(errorFileName, errorFilePath, errorLine, errorProne, errorMessage, "");
			} else if (temp != null) {
				temp.addLog(line);
			}
		}
		if (temp != null && !set.contains(temp)) { set.add(temp); }
		return set;
	}

	/**
	 * Runs ErrorProne static analysis tool on a java file.
	 * TODO: run ErrorProne from source code
	 *
	 * @param file   Name of file to analyze
	 * @return       ErrorProne results
	 */
	public static String analyze(String file) {
		String cmd = Utils.ERROR_PRONE_CMD.replace("{file}", file);
		String output = "";
		try {
			Process p = Runtime.getRuntime().exec(cmd);	
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null) {
			    output += line + "\n";
			}
		} catch (IOException e) {
			return null;
		}
		return output;
	}

	/**
	 * Compare ErrorProneItem objects based on key
	 *
	 * @param o   ErrorProneItem
	 */
	@Override
 	public boolean equals(Object o) {
        if (o instanceof ErrorProneItem){
			ErrorProneItem a = (ErrorProneItem) o;
            return a.getKey() == this.key;
        }
		return false;
  	}

	/**
	 * Hash ErrorProneItem objects based on class variables.
	 *
	 * @return   Hash value
	 */
	@Override
	public int hashCode() {
		return Objects.hash(key, filename, filepath, line, error, message, log);
	}
}

