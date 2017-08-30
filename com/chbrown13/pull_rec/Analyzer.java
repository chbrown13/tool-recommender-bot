package com.chbrown13.pull_rec;

//import com.google.errorprone.*;
//import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.util.*;

public class Analyzer {

	private String key;
	private String filename;
	private String filepath;
	private int line;
	private String error;
	private String message;
	private String log;
	private ArrayList<Analyzer> similar;

	public Analyzer(String key, String name, String path, int line, String err, String msg, String log) {
		this.key = key;
		this.filename = name;
		this.filepath = path;
		this.line = line;
		this.error = err;
		this.message = msg;
		this.log = log;
		this.similar = new ArrayList<Analyzer>();
	}

	public void print() {
		System.out.println();
		System.out.println("\tKey- "+this.key);
		System.out.println("\tFilepath- "+this.filepath);
		System.out.println("\tFilename- "+this.filename);
		System.out.println("\tLine Number- "+this.line);
		System.out.println("\tError- "+this.error);
		System.out.println("\tError Message- "+this.message);
		System.out.println("\tLog- ");
		System.out.println(this.log);
		System.out.println();
	}

	public String generateComment() {
		String comment = Utils.BASE_COMMENT;
		comment = comment.replace("{fixed}", "\n"+this.log+"\n");
		if(this.similar.isEmpty()) {
			comment = comment.replace("{errors}", ". ");
		} else {
			comment = comment.replace("{errors}", " such as:\n\n" + this.similar.get(0).getLog()+"\n\n");
		}
		return comment;
	}

	public String getLog() {
		return this.log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public void addLog(String log) {
		this.log += "\n"+log;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getFilePath() {
		return this.filepath;
	}

	public void setFilePath(String path) {
		this.filepath = path;
	}

	public String getFileName() {
		return this.filename;
	}

	public void setFileName(String file) {
		this.filename = file;
	}

	public String getMessage() {
		return this.message;
	}

	public static List<Analyzer> parseErrorProne(String file) {
		String regex = "^[\\w+/]*\\w.java\\:\\d+\\:.*\\:.*";
		String[] lines = file.split("\n");
		Analyzer temp = null;
		ArrayList<Analyzer> list = new ArrayList<Analyzer>();
		for (String line: lines) {
			if (line.matches(regex)) {
				if (temp != null) {	list.add(temp);	}
				String[] error = line.split(":");
				String errorFilePath = error[0];
				String errorFileName = errorFilePath.substring(errorFilePath.lastIndexOf("/")+1);
				int errorLine = Integer.parseInt(error[1]);
				String errorProne;
				if (error[3].contains("[")) {
					errorProne = error[3].substring(error[3].indexOf("[")+1,error[3].indexOf("]"));
				} else {
					errorProne = error[2].trim();
				}
				String errorKey = errorFilePath+":"+errorLine+":"+errorProne;
				String errorMessage = error[3];
				temp = new Analyzer(errorKey, errorFileName, errorFilePath, errorLine, errorProne, errorMessage, line);
			} else if (temp != null) {
				temp.addLog(line);
			}
		}
		if (temp != null) { list.add(temp); }
		System.out.println(String.format("%d errors found", list.size()));	
		return list;
	}

	public static String errorProne(String file) {
		String cmd = "java -Xbootclasspath/p:error_prone_ant-2.1.0.jar com.google.errorprone.ErrorProneCompiler " + file;
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

	@Override
 	public boolean equals(Object o) {
        if (o instanceof Analyzer){
			Analyzer a = (Analyzer) o;
            return a.getKey() == this.key;
        }
		return false;
  	}

	@Override
	public int hashCode() {
		return Objects.hash(key, filename, filepath, line, error, message, log);
	}
}

