package com.chbrown13.tool_rec;

import java.io.*;
import java.util.*;

public class Error {
	
    private String key;
	private String filename;
	private String filepath;
	private int line;
	private int offset;
	private String error;
	private String message;
	private String log;
	private String id;

	public Error(String path, String file, String line, String offset, String error, String msg, String log) {
		String project = Utils.getProjectName();
		this.filename = file;
		this.filepath = path;
		this.line = Integer.parseInt(line);
		this.offset = Integer.parseInt(offset);
		this.error = error;
		this.message = msg;
		this.log = log;
		this.key = String.join(":", path, this.error);
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
	 * Sets additional output from the log.
	 *
 	 * @param log   Message to add to log
	 */
	public void setLog(String log) {
		this.log = "\n"+log;
	}

	/**
	 * Gets code change ID for this error
	 * 
	 * @return   commit hash or pull request number
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets ID this error
	 * 
	 * @param id   Code change ID (commit hash or pull request number)
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Gets key to uniquely identify errors
	 *
	 * @return   String key for error
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
        String path = this.filepath.replace(Utils.getProjectName() + File.separator, "");
        return path.substring(path.indexOf(File.separator) + 1);
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
	public int getLine() {
		return this.line;
	}

	/**
	 * Gets the line number in file where error was found.
	 *
	 * @return   Line number as string
	 */
	public String getLineStr() {
		return Integer.toString(this.line);
	}

	/**
	 * Gets the offset on line where error occurs.
	 *
	 * @return   Column number
	 */
	public int getOffset() {
		return this.offset;
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
