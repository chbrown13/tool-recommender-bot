package com.chbrown13.pull_rec;

//import com.google.errorprone.*;
//import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.util.*;

/**
 * The ErrorProne class contains methods concerning the ErrorProne static analysis tool and an object storing information for a bug reported.
 */
public class ErrorProne extends Tool {
	
	private final String RUN_CMD = "java -Xbootclasspath/p:error_prone_ant-2.1.0.jar com.google.errorprone.ErrorProneCompiler {file}";	
	private final String MAVEN = "<plugin>"+
	"<groupId>org.apache.maven.plugins</groupId>" +
	"<artifactId>maven-compiler-plugin</artifactId>" +
	"<version>3.3</version>" +
	"<configuration>" +
	  "<compilerId>javac-with-errorprone</compilerId>" +
	  "<forceJavacCompilerUse>true</forceJavacCompilerUse>" +
	  "<source>8</source>" +
	  "<target>8</target>" +
	"</configuration>" +
	"<dependencies>" +
	  "<dependency>" +
		"<groupId>org.codehaus.plexus</groupId>" +
		"<artifactId>plexus-compiler-javac-errorprone</artifactId>" +
		"<version>2.8</version>" +
	  "</dependency>" +
	  "<dependency>" +
		"<groupId>com.google.errorprone</groupId>" +
		"<artifactId>error_prone_core</artifactId>" +
		"<version>2.1.1</version>" +
	  "</dependency>" +
	"</dependencies>" +
  "</plugin>";

	public ErrorProne() {
		super("Error Prone", "static analysis tool", "http://errorprone.info");
	}

	/**
	 * Returns the Error Prone maven plugin for build
	 */
	@Override
	public String getPlugin() {
		return this.MAVEN;
	}
	
	/**
	 * Parses output from ErrorProne static analysis of code and creates objects.
	 *
	 * @param msg    ErrorProne output
	 * @return       List of ErrrorProne
	  */
	@Override
	public Set<Error> parseOutput(String msg) {
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
	@Override
	public String analyze(String file) {
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

