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
	 * Analyze updates to a file to determine if changes were actually a fix.
	 * 
	 * @param file1   File from master branch
	 * @param file2   File from pull request
	 * @return        True if change is considered a fix, otherwise false
     */
	public static boolean isFix(String file1, String file2) {
		//gumtree analysis to determine fix
		return true;
	}

	/**
	 * Download contents of a file from a web url, similar to wget linux command
	 *
	 * @param fileUrl   String url of the file to download
	 * @param output    Name of file to store contents
	 */
	public static void wget(String fileUrl, String output) {
		String s = "";
		try {
			URL url = new URL(fileUrl);
			InputStream in;
			try {
				in = url.openStream();
			} catch (FileNotFoundException e) {
				//File URL does not exist, possibly new file in PR
				System.out.println("---newfile---");
				in = new ByteArrayInputStream("".getBytes("UTF-8"));
			}
			File out = new File(output);
			out.getParentFile().mkdirs();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			while ((s = br.readLine()) != null) {
				bw.write(s+"\n");
			}
			bw.close();
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
