package com.chbrown13.pull_rec;

import java.io.*;
import java.net.*;
import java.util.*;

public class Utils {

	public static String BASE_COMMENT = "You fixed the following error in this Pull Request:\n{fixed}\nGoogle's Error Prone static analysis tool can be used to find more issues similar to this{errors}Check out http://errorprone.info for more information.";

	public static String ERROR_PRONE_CMD = "java -Xbootclasspath/p:error_prone_ant-2.1.1.jar com.google.errorprone.ErrorProneCompiler {file}";
	
	public static String COMMENT_CMD = "curl --user \"chbrown13:git_down1\" --data '{\"body\":\"java test\", \"commit_id\":\"7397eba35d2de962c00bafd2e734e32fa05f65eb\", \"path\":\"README.md\", \"position\":4}' -H \"Content-Type: application/json\" https://api.github.com/repos/chbrown13/RecommenderTest/pulls/20/comments -v";

	public static void postComment() {
		try {
			Process p = Runtime.getRuntime().exec(COMMENT_CMD);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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

	public static String[] getCredentials(String filename) {
		String[] creds = new String[2];
		File file = new File(filename);
		try {
			int i = 0;
		    Scanner sc = new Scanner(file);
		    while (i < 2) {
		        creds[i] = sc.nextLine();
				i++;
		    }
		    sc.close();
		} 
		catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
		return creds;
	}
}
