package com.chbrown13.pull_rec;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The Utils class contains various helper methods and variables for the PullRecommender project.
 */
public class Utils {

	public static String MARKDOWN_LINK = "[{src}]({url})";

	public static String LINK_URL = "https://github.com/{user}/{repo}/blob/{sha}/{path}#L{line}";

	public static String BASE_COMMENT = "Good job! The static analysis tool Error Prone reported that the below error [1] used to be here, but you removed it.{similar}Check out http://errorprone.info for more information.\n\n\n[1] {fixed}";

	public static String ERROR_PRONE_CMD = "java -Xbootclasspath/p:error_prone_ant-2.1.0.jar com.google.errorprone.ErrorProneCompiler {file}";
	
	private static String projectName = "";

	private static String projectOwner = "";

	/**
	 * Stores current project name.
	 *
	 * @param name   Github repository name
	 */
	public static void setProjectName(String name) {
		projectName = name;
	}

	/**
	 * Gets repository name for current project.
	 *
	 * @returns   Github project name
	 */
	public static String getProjectName() {
		return projectName;
	}

	/**
	 * Stores username of Github repository for current project.
	 *
	 * @param user   Username of repo owner
	 */
	public static void setProjectOwner(String user) {
		projectOwner = user;
	}
	
	/**
	 * Gets owner of Github repository for current project.
     *
	 * @return   Username of repo owner
	 */	
	public static String getProjectOwner() {
		return projectOwner;
	}

	/**
	 * Download contents of a file from a web url, similar to wget command
	 *
	 * @param fileUrl   String url of the file to download
	 * @param output    Name of file to store contents
	 */
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

	/**
	 * Retrieve Github username and password from a file. Expects file to contain the username on the first line and password on the second line.
	 *
	 * @param filename   Name of credentials file
	 * @return           Array containing the Github username and password
	 */
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
