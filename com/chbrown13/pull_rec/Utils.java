package com.chbrown13.pull_rec;

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.LcsMatcher;
import com.github.gumtreediff.tree.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The Utils class contains various helper methods and variables for the PullRecommender project.
 */
public class Utils {

	public static String MARKDOWN_LINK = "[{src}]({url})";

	public static String LINK_URL = "https://github.com/{user}/{repo}/blob/{sha}/{path}#L{line}";

	public static String BASE_COMMENT = "Good job! The static analysis tool Error Prone reported an error [1] used to be here, but you fixed it.{similar}Check out http://errorprone.info for more information.\n\n\n[1] {fixed}";

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
	 * Gets line number from offset of updated/inserted line
	 * 
	 * @param offset   Character position in the updated file
	 * @param file     Path to changed file
	 * @return         New line number
	 */
	private static int getDiffLineNumber(int offset, String file) {
		int count = 0;
		try {
			InputStream in = new FileInputStream(file);
			BufferedReader buf = new BufferedReader(new InputStreamReader(in));
			String line = buf.readLine();
			StringBuilder sb = new StringBuilder();				
			while(line != null){
				sb.append(line).append("\n");
				line = buf.readLine();
			}
					
			String code = sb.toString();
			String[] lines = code.substring(offset).split("\n");
			count = lines.length;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return count;
	}

	/**
	 * Analyze updates to a file to determine if changes were actually a fix.
	 * 
	 * @param file1   File from master branch
	 * @param file2   File from pull request
	 * @return        Line number of what is considered a fix, otherwise null
     */
	public static int getFix(String file1, String file2) {
		//gumtree analysis to determine fix
		Run.initGenerators();
		try {
			ITree src = Generators.getInstance().getTree(file1).getRoot();
			ITree dst = Generators.getInstance().getTree(file2).getRoot();
			
			List<ITree> srcTrees = src.getTrees();
			List<ITree> dstTrees = dst.getTrees();
			LcsMatcher lcs = new LcsMatcher(src, dst, new MappingStore());
			lcs.match();
			for (ITree tree: dstTrees) { // node added/updated in destination
				if (!tree.isMatched()) {
					//System.out.print(tree.getShortLabel());
					//System.out.print("    ");
					//System.out.print(tree.getPos());
					//System.out.print("    ");
					//System.out.print(tree.getLabel());
					//System.out.print("    ");
					//System.out.println(tree.getType());
					return getDiffLineNumber(tree.getPos(), file2);
				}
			}
			return -1;
		} catch (IOException e) { e.printStackTrace(); }
		return -1;
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
