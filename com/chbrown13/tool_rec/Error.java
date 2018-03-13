package com.chbrown13.tool_rec;

import java.util.*;

public class Error {
    private String key;
	private String filename;
	private String filepath;
	private int line;
	private String error;
	private String message;
	private String log;

	public Error(String name, String path, String line, String error, String msg, String log) {
		String project = Utils.getProjectName();
		String realPath = path.replace(project+"1", project).replace(project+"2", project);
		this.filename = name;
		this.filepath = path;
		this.line = Integer.parseInt(line);
		this.error = error;
		this.message = msg;
		this.log = log;
    }

    /**
	 * Gets static analysis output for current error
	 *
	 * @return   Error message
	 */
	public String getLog() {
		return this.log;
	}
	
	/**
	 * Appends output to the log.
	 *
 	 * @param log   Message to add to log
	 */
	public void addLog(String log) {
		this.log += "\n"+log;
	}
	
	/**
	 * Gets key to uniquely identify errors
	 *
	 * @return   String key for error
	 */
	public String getKey() {
		return String.join(":", getLocalFilePath(), this.error);
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
	 * Sets the filepath for error.
	 *
	 * @param path   Filepath to error
	 */
	public void setFilePath(String path) {
		this.filepath = path;
	}

	/**
	 * Gets filename of file containing error reported.
	 *
 	 * @return   String filepath
	 */
	public String getFileName() {
		return this.filename;
	}

	/**
	 * Gets the local filepath to where error was reported in the project.
	 *
 	 * @return   String filepath
	 */
	public String getLocalFilePath() {
        String path = this.filepath.replace(Utils.getCurrentDir() + "/", "");
        return path.substring(path.indexOf("/") + 1);
	}
	
	/**
	 * Sets the filename for error.
	 *
	 * @param path   Filename of file with error
	 */
	public void setFileName(String file) {
		this.filename = file;
	}

	/**
	 * Gets the error message from static analysis output.
	 *
	 * @return   Error message
	 */
	public String getMessage() {
		return this.message;
	}
	
	/**
	 * Gets the name of the error reported in tool output.
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
	 * Creates comment to recommend tool from error for a Github code change.
	 *
	 * @return   Message with fixed error and tool recommendation
	 */
	public String generateComment(Tool tool, List<Error> similar, String sha) {
		String comment = Utils.BASE_COMMENT.replace("{desc}", tool.getDescription())
			.replace("{tool}", tool.getName())
			.replace("{link}", tool.getLink());
		String simSentence = " {tool} also found {issue} in {link}. ".replace("{tool}", tool.getName()); //TODO replace with tool
		comment = comment.replace("{fixed}", "\\`\\`\\`" + String.join(":", this.filename, getLineNumberStr(), " ") + this.message + this.log + "\\`\\`\\`");
		List<Error> simList = new ArrayList<Error>();
		Error sim;
		for (Error epi: similar) {
			if (!this.equals(epi) && this.similar(epi)) {
				simList.add(epi);
			}
		}
		Iterator<Error> iter = simList.iterator();
		if (simList.isEmpty()) {
			return null;
		} else if (simList.size() == 1) {
				comment = comment.replace("{similar}", simSentence.replace("{link}", getErrorLink(iter.next(), sha)).replace("{issue}", "a similar issue"));
		} else {
			comment = comment.replace("{similar}", simSentence.replace("{link}", String.join(" and ", getErrorLink(iter.next(), sha), getErrorLink(iter.next(), sha))).replace("{issue}", "similar issues"));
		}
		System.out.println("Err- "+comment);
		return comment;
	}

	/**
	 * Gets url to link to similar errors found by Tool in recommendation.
	 *
	 * @param err   Error with others similar to fixed error
	 * @param hash  Git commit hash
	 * @return      Url to line with a similar error
	 */
	private String getErrorLink(Error err, String hash) {
		String url = Utils.LINK_URL.replace("{line}", err.getLineNumberStr())
			.replace("{path}", err.getLocalFilePath())
			.replace("{sha}", hash).replace("{repo}", Utils.getProjectName())
			.replace("{user}", Utils.getProjectOwner());
		return Utils.MARKDOWN_LINK.replace("{src}", err.getFileName()).replace("{url}", url);
	}

	/**
	 * Compare Error objects based on error
	 */
	public boolean similar(Object o) {
		if (o instanceof Error) {
			Error e = (Error) o;
			return e.getError().equals(this.error);
		}
		return false;
	}

	/**
	 * Compare Error objects based on key
	 *
	 * @param o   Error
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Error) {
			Error e = (Error) o;
			return e.getKey().equals(this.key);
		}
	    return false;
	}

    /**
	 * Hash Error objects based on key
	 *
	 * @return   Hash value
	 */
    @Override
    public int hashCode() {
		return Objects.hash(key);
	}
}
