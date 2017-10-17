package com.chbrown13.pull_rec;

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.*;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.LcsMatcher;
import com.github.gumtreediff.tree.*;
import java.io.*;
import java.lang.*;
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
	 * Gets the Gumtree node of error reported by ErrorProne
	 * 
	 * @param root   root of the file AST tree
	 * @param loc    Character offset location of error
	 * @return       ITree node of error
	 */
	private static ITree getErrorNode(ITree root, int loc) {
		List<ITree> it = root.getDescendants();
		ITree temp = root;
		int i = 0;
		while (temp.getPos() <= loc) {
			i++;
			temp = it.get(i);
		}
		return temp;
	}

	/**
	 * Get the character offset of an error based on the reported line
	 * 
	 * @param error   Error reported by ErrorProne
	 * @param srcFile Filename of source code
	 * @return        character offset where error begins
	 */
	private static int getErrorOffset(ErrorProneItem error, String srcFile) {
		String log = error.getLog();
		String prev = null;
		int loc;
		int chars = 0;
		String bug = null;
		for(String line: log.split("\n")) {
			if(line.contains("^")) {
				loc = line.indexOf("^");
				bug = prev.substring(loc);
				break;
			}
			prev = line;
		}

		File file = new File(srcFile);
		try {
			int i = 0;
		    Scanner sc = new Scanner(file);
		    while (sc.hasNext()) {
				if (i < error.getLineNumber()-1) {
					chars += sc.nextLine().length(); //ignore
				} else {
					String str = sc.nextLine();
					if (str.contains(bug)) {
						chars += str.indexOf(bug);
						return chars;
					}
				}
				i++;
		    }
		    sc.close();
		} 
		catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Check if a node exists in the updated file
	 * 
	 * @param node   ITree to search for in destination file
	 * @param map    Mapping between file versions
	 * @return       True if node is in new file, else false
	 */
	private static boolean searchNode(ITree node, ITree tree) {
		if (node.getParent() == null) {
			return false;
		}
		else {
			ITree parent = node.getParent();
			if (parent.getChildren().contains(node)) {
				return true;
			} else {
				return searchNode(node, parent);
			}
		}
	}

	/**
	 * Gets the line number based on character offset
	 * 
	 * @param pos   Position of node in source code
	 * @param f     Name of destination file
	 * @return      Line number of node
	 */
	private static int posToLine(int pos, String f) {
		File file = new File(f);
		int line = 0;
		int count = 0;
		try {
		    Scanner sc = new Scanner(file);
		    while (sc.hasNext()) {
				count += sc.nextLine().length();
				line += 1;
				if (count >= pos) {
					break;
				}
			}
		    sc.close();
		} 
		catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
		return line - 1;
	}

	/**
	 * Parse changes in file to determine if a fix was made
	 * 
	 * @param file1    Name of source file
	 * @param file2    Name of destination file
	 * @param errorPos Character offset of error
	 * @return		   Changed line number
	 */
	private static int analyzeDiff(String file1, String file2, int errorPos) {
		JdtTreeGenerator jdt1 = new JdtTreeGenerator();
		JdtTreeGenerator jdt2 = new JdtTreeGenerator();
		ITree src = null;
		ITree dst = null;
		TreeContext tree1 = null;
		TreeContext tree2 = null;
		Matcher m = null;
		ActionGenerator g = null;
		Action closestAction = null;
		try {
			tree1 = jdt1.generateFromFile(file1);
			tree2 = jdt2.generateFromFile(file2);
			src = Generators.getInstance().getTree(file1).getRoot();
			dst = Generators.getInstance().getTree(file2).getRoot();
			m = Matchers.getInstance().getMatcher(src, dst);
			m.match();
			g = new ActionGenerator(src, dst, m.getMappings());
			g.generate();
		} catch (IOException e) {
			e.printStackTrace();
		}

		MappingStore store = m.getMappings();		
		ITree errorNode = getErrorNode(tree1.getRoot(), errorPos);
		List<Action> actions = g.getActions(); 
		try {
			closestAction = actions.get(0);			
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
		for(Action a: actions) {
			int pos = a.getNode().getPos();
			if (Math.abs(errorNode.getPos() - pos) < Math.abs(errorNode.getPos() - closestAction.getNode().getPos())) {
				closestAction = a;
			}
		}
		ITree temp = null;
		if (closestAction.toString().startsWith("DEL")) { //get closest sibling or parent
			int i = errorNode.positionInParent();
			while (i > 0) {
				i -= 1;
				temp = errorNode.getParent().getChildren().get(i);
				if (searchNode(temp, dst)) {
					System.out.println("1");
					return posToLine(temp.getPos(), file2);
				}
			}
		} //INS or UPD or other
			temp = closestAction.getNode();
			while (!searchNode(temp, dst)) {
				temp = temp.getParent();
			}
			return posToLine(temp.getPos(), file2);
	}

	/**
	 * Checks changes to see if there was actually a fix or just code removed.
	 * 
	 * @param file1   File from master branch
	 * @param file2   File from pull request
	 * @return        Line number of what is considered a fix, otherwise null
     */
	public static int getFix(String file1, String file2, ErrorProneItem error) {
		//gumtree analysis to determine fix
		Run.initGenerators();
		int errorLocation = getErrorOffset(error, file1);
		try {
			ITree src = Generators.getInstance().getTree(file1).getRoot();
			ITree dst = Generators.getInstance().getTree(file2).getRoot();
			
			List<ITree> srcTrees = src.getTrees();
			List<ITree> dstTrees = dst.getTrees();
			//LcsMatcher lcs = new LcsMatcher(src, dst, new MappingStore());
			//lcs.match();
			for (ITree tree: dstTrees) { // node added/updated in destination
				if (!tree.isMatched()) {
					//Not all deletes
					return analyzeDiff(file1, file2, errorLocation);
				}
			}
		} catch (IOException e) { 
			e.printStackTrace(); 
		}
		System.out.println("Error code removed, but may not have been fixed.");
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
	 * Retrieve Github username and password from a file. Expects file to contain the username on 
	 * the first line and password on the second line.
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
