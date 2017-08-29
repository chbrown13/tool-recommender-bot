package com.chbrown13.pull_rec;

import com.google.errorprone.*;
import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.json.JsonObject;

public class Analyzer {

	public static JsonObject parseErrorProne(String file) {
		String regex = "^[\\w+/]*\\w.java\\:\\d+\\:.*\\:.*";
		String[] lines = file.split("\n");
		for(String line: lines) {
			if (line.matches(regex)) {
				System.out.println(line);
			}
		}
		return null;
	}

	public static void wgetFile(String fileUrl, String output) {
		String s;

		try {
			URL url = new URL(fileUrl);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			BufferedWriter out = new BufferedWriter(new FileWriter(output));
			while ((s = br.readLine()) != null) {
				out.write(s+"\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
}

