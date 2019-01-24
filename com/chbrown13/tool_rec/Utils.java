package com.chbrown13.tool_rec;

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.*;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.*;
import com.github.gumtreediff.tree.*;
import com.jcabi.github.Pull;
import com.jcabi.github.RepoCommit;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import javax.json.*;
import javax.xml.parsers.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The Utils class contains various helper methods and variables for tool-recommender-bot
 */
public class Utils {

	private static String MVN_COMPILE = "mvn -f {dir}/pom.xml --log-file tool_{sha}.txt compile";
	private static String MVN_VALIDATE = "mvn -f {dir}/pom.xml validate";
	private static String currentDir = System.getProperty("user.dir");
	private static String projectName = "";
	private static String projectOwner = "";
	private static String username = "";
	private static String password = "";

	/**
	 * Stores current user's Github login
	 *
	 * @param name   Github account username
	 */
	private static void setUsername(String name) {
		username = name;
	}

	/**
	 * Returns the GitHub username
	 */
	public static String getUsername() {
		return username;
	}

	/**
	 * Stores current user's Github password
	 *
	 * @param pass   Github account password
	 */
	private static void setPassword(String pass) {
		if (pass != null) {
			password = pass;
		} else {
			password = "";
		}
	}

	/**
	 * Returns the GitHub user password
	 */
	public static String getPassword() {
		return password;
	}

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
	 * Compiles the project to analyze code in the repository
	 * 
	 * @param dir   Local path to repo
	 * @param hash  Commit hash for current version
	 * 
	 * @return      Output from the maven build
	 */
	public static boolean compile(String dir, String hash) {
		String valid = MVN_VALIDATE.replace("{dir}", dir);
		String compile = MVN_COMPILE.replace("{dir}", dir).replace("{sha}", hash);
		try {
			try {
				System.out.println(valid);
				Process p1 = Runtime.getRuntime().exec(valid);
				p1.waitFor();
				if(p1.exitValue() == 0) {
					System.out.println(compile);
					Process p2 = Runtime.getRuntime().exec(compile);
					p2.waitFor();	
				} else {
					System.out.println("invalid pom");
					return false;
				}
			} catch (InterruptedException ie) {
				ie.printStackTrace();
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Modifies base pom.xml file to add current tool plugin for maven build
	 * 
	 * @param file   Path to pom.xml file in base version of code
	 * @param tool   Tool to analyze code
	 */
	public static String updatePom(String file, Tool tool) {
		Scanner scan = null;
		String toolPom = "";
		boolean properties = false;
		boolean plugin = false;
		boolean profile = false;
		String pom = "";
		try {
			scan = new Scanner(new File(file));
			pom = loadFile(file);
		} catch (FileNotFoundException e) {
			System.out.println("No pom.xml");
			return null;
		}
		if(pom.contains("com.google.errorprone")) {
			System.out.println("Already has Error Prone.");
			return null;
		}
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String tag = line.replace("\n","").trim();
			if(tag.equals("<properties>") && !properties) {
				while (!tag.equals("</properties>")) {
					toolPom += line;
					line = scan.nextLine();
					tag = line.replace("\n","").trim();
				}
				toolPom += tool.getProperty();
				properties = true;
			} else if(tag.equals("<pluginManagement>") && !plugin) {
				while(!tag.equals("</plugins>")) {
					toolPom += line;
					line = scan.nextLine();
					tag = line.replace("\n","").trim();
				}
				toolPom += tool.getPlugin();
				plugin = true;
			} else if(tag.equals("<reporting>")) {
				while(!tag.equals("</reporting>")) {
					toolPom += line;
					line = scan.nextLine();
					tag = line.replace("\n","").trim();
				}
			} else if (tag.equals("<profiles>") && !profile) {
				while(!tag.equals("</profiles>")) {
					toolPom += line;
					line = scan.nextLine();
					tag = line.replace("\n","").trim();
				}
				toolPom += tool.getProfile();
				profile = true;
			} else if (tag.equals("</plugins>") && !plugin) {
				toolPom += tool.getPlugin();
				plugin = true;
			} else if (tag.equals("</build>") && !plugin) {
				toolPom += "<plugins>\n{p}</plugins>\n".replace("{p}", tool.getPlugin());
				plugin = true;
			} else if (tag.equals("</project>")) {
				if(!properties) {
					toolPom += "<properties>\n{p}</properties>\n".replace("{p}", tool.getProperty());
				}
				if(!plugin) {
					toolPom += "<build>\n<plugins>\n{p}</plugins>\n</build>\n".replace("{p}", tool.getPlugin());
				}
				if(!profile) {
					toolPom += "<profiles>\n{p}</profiles>\n".replace("{p}", tool.getProfile());
				}
			}
			toolPom += line + "\n";
		}
		return toolPom;
	}	

	/**
	 * Utility method to load the contents of a file into a string
	 */
	public static String loadFile(String path) {
		String str = "";
		Scanner sc = null;
		File file = new File(path);
		try {
			sc = new Scanner(file);
		} catch (FileNotFoundException e) {
			return null;
		}
		while (sc.hasNextLine()) {
			str += sc.nextLine() + "\n";
		}
		sc.close();
		return str;
	}

	/**
	 * Retrieve username and password from file
	 * Github and email data in files with username or auth token on line 1 and password on line 2
	 *
	 * @param filename   Name of credentials file
	 * @return           Array containing username and password
	 */
	public static String[] getCredentials(String filename) {
		String secret = loadFile(filename);
		String[] creds = new String[2];
		int i = 0;
		for (String s: secret.split("\n")) {
			creds[i] = s;
			i++;
		}
		if (filename.contains("github")) {
			setUsername(creds[0]);
			setPassword(creds[1]);
		}
		return creds;
	}
}
