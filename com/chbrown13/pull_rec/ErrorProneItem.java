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
	private String commit;
	private List<ErrorProneItem> similar;

	public ErrorProneItem(String name, String path, String line, String error, String msg, String hash, String log) {
		this.key = String.join(":", path, line, error);
		this.filename = name;
		this.filepath = path;
		this.line = Integer.parseInt(line);
		this.error = error;
		this.message = msg;
		this.commit = hash;
		this.log = log;
		this.similar = new ArrayList<ErrorProneItem>();
	}

	/**
	 * Creates comment to recommend ErrorProne for a Github pull request.
	 *
	 * @return   Message containing the fixed error and other similar errors found
	 */
	public String generateComment() {
		String comment = Utils.BASE_COMMENT;
		String simSentence = " Error Prone also found similar issues in {link}. ";
		comment = comment.replace("{fixed}", "```" + this.log + "```");
		Set<String> simSet = new HashSet<String>();
		for (ErrorProneItem e: this.similar) {
			simSet.add(e.getKey());
		}
		if (simSet.isEmpty()) {
			comment = comment.replace("{similar}", " ");
		} else if (simSet.size() == 1) {
			comment = comment.replace("{similar}", simSentence.replace("{link}", getErrorLink(simSet.iterator().next())));
		} else {
			Iterator<String> it = simSet.iterator();
			comment = comment.replace("{similar}", simSentence.replace("{link}", String.join(" and ", getErrorLink(it.next()), getErrorLink(it.next()))));
		}
		System.out.println(comment);
		return comment;
	}

	/**
	 * Gets errors similar to the current error.
	 *
	 * @return   List of other ErrorProneItems with the same error
	 */
	public List<ErrorProneItem> getSimilarErrors() {
		return this.similar;
	}
	
	/**
	 * Gets url to link to similar errors found by Error Prone in recommendation.
	 *
	 * @param epi   ErrorProneItem with error similar to fixed error
	 * @return      Url to line with similar error
	 */
	private String getErrorLink(String key) {
		ErrorProneItem epi = null;
		for (ErrorProneItem e: this.similar) {
			if (e.getKey().equals(key)) {
				epi = e;
				break;
			}
		}
		String url = Utils.LINK_URL.replace("{line}", epi.getLineNumberStr()).replace("{path}", epi.getRelativeFilePath()).replace("{sha}", epi.getCommit()).replace("{repo}", Utils.getProjectName()).replace("{user}", Utils.getProjectOwner());
		return Utils.MARKDOWN_LINK.replace("{src}", epi.getFileName()).replace("{url}", url);
	}

	/**
	 * Adds an ErrorProneItem with a similar error to current item's list.
	 *
	 * @param error   ErrorProneItem with the same error message as current object
	 */
	public void addSimilarError(ErrorProneItem error) {
		if (!this.similar.contains(error) && !this.filename.equals(error.getFileName())) {
			this.similar.add(error);
		}
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
	 * Gets the relative file path within the project folder.
	 *
	 * @param project   Project name
	 * @return          Filepath in project directory
	 */
	public String getRelativeFilePath() {
		String project = Utils.getProjectName();
		if (this.filepath.startsWith(project+"/")) {
			return this.filepath.replace(project+"/","");
		}
		return this.filepath;
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
	 * Gets hash of project commit where error was found.
	 *
	 * @return   git commit hash
	 */
	public String getCommit() {
		return this.commit;
	}

	/**
	 * Sets Github commit hash for ErrorProne error output.
	 *
	 * @param hash   git commit hash
	 */
	public void setCommit(String hash) {
		this.commit = hash;
	}

	/**
	 * Checks if ErrorProne reported multiple instances of the same error.
	 *
	 * @param error   Current ErrorProneItem
	 * @param list    List of previous ErrorProneItems
	 */
	private static void checkSimilarError(ErrorProneItem error, ArrayList<ErrorProneItem> list) {
		for (ErrorProneItem epi: list) {
			if (epi.getMessage().equals(error.getMessage()) && epi.getError().equals(error.getError()) && !epi.getKey().equals(error.getKey()) ) {
				error.addSimilarError(epi);
				epi.addSimilarError(error);
			}
		}
	}

	/**
	 * Parses output from ErrorProne static analysis of code and creates objects.
	 *
	 * @param file   Name of file containing ErrorProne output
	 * @return       List of ErrrorProneItems
	  */
	public static List<ErrorProneItem> parseErrorProneOutput(String file, boolean master) {
		String regex = "^[\\w+/]*\\w.java\\:\\d+\\:.*\\:.*";
		String[] lines = file.split("\n");
		ErrorProneItem temp = null;
		ArrayList<ErrorProneItem> list = new ArrayList<ErrorProneItem>();
		for (String line: lines) {
			if (line.matches("^\\d+\\serror[s]*$")) {
				continue;
			}
			else if (line.matches(regex)) {
				if (temp != null && !list.contains(temp)) {	list.add(temp); }
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
				temp = new ErrorProneItem(errorFileName, errorFilePath, errorLine, errorProne, errorMessage, null, line);
				checkSimilarError(temp, list);
			} else if (temp != null) {
				temp.addLog(line);
			}
		}
		if (temp != null && !list.contains(temp)) { list.add(temp); }
		if (list.size() != 1) {
			System.out.println("{n} errors found.".replace("{n}", Integer.toString(list.size())));
		} else { 
			System.out.println("1 error found.");
		}
		return list;
	}

	/**
	 * Runs ErrorProne static analysis tool on a java file.
	 * TODO: run ErrorProne from source code
	 *
	 * @param file   Name of file to analyze
	 * @return       ErrorProne results
	 */
	public static String analyzeCode(String file) {
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
			e.printStackTrace();
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
		return Objects.hash(key, filename, filepath, line, error, message, log, commit, similar);
	}
}

