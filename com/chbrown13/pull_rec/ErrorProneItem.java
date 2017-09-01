package com.chbrown13.pull_rec;

//import com.google.errorprone.*;
//import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.util.*;

public class ErrorProneItem {

	private String key;
	private String filename;
	private String filepath;
	private int line;
	private String error;
	private String message;
	private String log;
	private String commit;
	private ArrayList<ErrorProneItem> similar;

	public ErrorProneItem(String key, String name, String path, int line, String err, String msg, String log) {
		this.key = key;
		this.filename = name;
		this.filepath = path;
		this.line = line;
		this.error = err;
		this.message = msg;
		this.log = log;
		this.similar = new ArrayList<ErrorProneItem>();
	}

	public String generateComment() {
		String comment = Utils.BASE_COMMENT;
		comment = comment.replace("{fixed}", "\n"+this.log+"\n");
		if (this.similar.isEmpty()) {
			comment = comment.replace("{errors}", ". ");
		} else {
			comment = comment.replace("{errors}", " such as:\n\n" + this.getSimilarErrorsStr());
		}
		return comment;
	}

	public ArrayList<ErrorProneItem> getSimilarErrors() {
		return this.similar;
	}
	
	public String getSimilarErrorsStr() {
		String str = "";
		for (ErrorProneItem epi: this.similar) {
			str += epi.log + "\n\n";
		}
		return str;
	}

	public void addSimilarError(ErrorProneItem error) {
		this.similar.add(error);
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

	public String getProjectPath(String project) {
		if (this.filepath.startsWith(project+"/")) {
			return this.filepath.replace(project+"/","");
		}
		return this.filepath;
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
	
	public String getError() {
		return this.error;
	}

	public int getLineNumber() {
		return this.line;
	}

	public String getCommit() {
		return this.commit;
	}

	public void setCommit(String hash) {
		this.commit = hash;
	}

	private static void checkSimilarError(ErrorProneItem error, ArrayList<ErrorProneItem> list) {
		for (ErrorProneItem epi: list) {
			if (epi.getError().equals(error.getError()) && !epi.getKey().equals(error.getKey())) {
				error.addSimilarError(epi);
				epi.addSimilarError(error);
			}
		}
	}

	public static List<ErrorProneItem> parseErrorProneOutput(String file) {
		String regex = "^[\\w+/]*\\w.java\\:\\d+\\:.*\\:.*";
		String[] lines = file.split("\n");
		ErrorProneItem temp = null;
		ArrayList<ErrorProneItem> list = new ArrayList<ErrorProneItem>();
		ArrayList<ErrorProneItem> seen = new ArrayList<ErrorProneItem>();
		for (String line: lines) {
			if (line.matches(regex)) {
				if (temp != null) {	list.add(temp); }
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
				String errorKey = String.join(":", errorFilePath, error[1], errorProne);
				String errorMessage = error[3];
				temp = new ErrorProneItem(errorKey, errorFileName, errorFilePath, errorLine, errorProne, errorMessage, line);
				seen.add(temp);
				checkSimilarError(temp, seen);
			} else if (temp != null) {
				temp.addLog(line);
			}
		}
		if (temp != null) { list.add(temp); }
		System.out.println(String.format("%d errors found", list.size()));	
		return list;
	}

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

	@Override
 	public boolean equals(Object o) {
        if (o instanceof ErrorProneItem){
			ErrorProneItem a = (ErrorProneItem) o;
            return a.getKey() == this.key;
        }
		return false;
  	}

	@Override
	public int hashCode() {
		return Objects.hash(key, filename, filepath, line, error, message, log, commit, similar);
	}
}

