package com.chbrown13.pull_rec;

//import com.google.errorprone.*;
//import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.util.*;

/**
 * The ErrorProne class contains methods concerning the ErrorProne static analysis tool and an object storing information for a bug reported.
 */
public class ErrorProne extends Tool {
	
	private static String RUN_CMD = "java -Xbootclasspath/p:error_prone_ant-2.1.0.jar com.google.errorprone.ErrorProneCompiler {file}";	
	private String name = "Error Prone";

	public String getName()  {
		return this.name;
	}
	
	/**
	 * Parses output from ErrorProne static analysis of code and creates objects.
	 *
	 * @param msg    ErrorProne output
	 * @return       List of ErrrorProne
	  */
	public static Set<Error> parseOutput(String msg) {
		String regex = "^[\\w+/]*\\w.java\\:\\d+\\:.*\\:.*";
		String[] lines = msg.split("\n");
		Error temp = null;
		Set<Error> set = new HashSet<Error>();
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
				temp = new Error(errorFileName, errorFilePath, errorLine, errorProne, errorMessage, "");
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
		String cmd = RUN_CMD.replace("{file}", file);
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
}

