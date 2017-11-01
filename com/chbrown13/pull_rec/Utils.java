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
import javax.xml.parsers.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The Utils class contains various helper methods and variables for the PullRecommender project.
 */
public class Utils {

	public static String MARKDOWN_LINK = "[{src}]({url})";

	public static String LINK_URL = "https://github.com/{user}/{repo}/blob/{sha}/{path}#L{line}";

	public static String RAW_URL = "https://raw.githubusercontent.com/{user}/{repo}/{sha}/{path}";

	public static String BASE_COMMENT = "Good job! The {desc} {tool} reported an error [1] used to be here, but you fixed it.{similar}Check out {link} for more information.\n\n\n[1] {fixed}";

	private static String MVN_COMPILE = "mvn -q -f {dir}/pom.xml compile";

	private static String REMOVE = "rm -rf {project}*";

	private static String CHECKOUT = "git --git-dir=_project/.git fetch origin pull/{num}/head:_name && git --git-dir=_project/.git checkout -f _name";

	private static String currentDir = System.getProperty("user.dir");

	private static List<String> created = new ArrayList<String>();

	private static boolean myTool = false;

	private static String projectName = "";
	
	private static String projectOwner = "";

	private static int fixLine = -1;

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
	 * Gets the local path of a file
	 * 
	 * @param path   Path to file in git repo
	 * @return       Path to file in current working directory
	 */
	public static String getLocalPath(String path) {
		String remove = currentDir;
		if (!currentDir.endsWith(projectName)) {
			remove += "/" + projectName;
		}
		return path.replace(remove, "");
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
	private static int getErrorOffset(Error error, String srcFile) {
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
		fixLine = line - 1;
		return line - 1;
	}

	/**
	 * Parse changes in file to determine if a fix was made
	 * 
	 * @param base    Name of source file
	 * @param pull    Name of destination file
	 * @param errorPos Character offset of error
	 * @return		   Changed line number
	 */
	private static int determineFix(String base, String pull, int errorPos) {
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
			tree1 = jdt1.generateFromFile(base);
			tree2 = jdt2.generateFromFile(pull);
			src = Generators.getInstance().getTree(base).getRoot();
			dst = Generators.getInstance().getTree(pull).getRoot();
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
					return posToLine(temp.getPos(), pull);
				}
			}
		} //INS or UPD or other
			temp = closestAction.getNode();
			while (!searchNode(temp, dst)) {
				temp = temp.getParent();
			}
			return posToLine(temp.getPos(), pull);
	}

	/**
	 * Checks if pull request changes only remove code without fix
	 * 
	 * @param base   Path to base file
	 * @param pull   Path to pull request version of file
	 * @param error  Current error to check
	 * @return       False if code is updated or added, otherwise true
	 */
	private static boolean isDeleteOnly(String file1, String file2, Error error) {
   		Run.initGenerators();
   		try {
   			ITree src = Generators.getInstance().getTree(file1).getRoot();
   			ITree dst = Generators.getInstance().getTree(file2).getRoot();
   			List<ITree> srcTrees = src.getTrees();
   			List<ITree> dstTrees = dst.getTrees();
   			LcsMatcher lcs = new LcsMatcher(src, dst, new MappingStore());
   			lcs.match();
			for (ITree tree: dst.getDescendants()) { // node added/updated in destination
   				if (!tree.isMatched()) {
   					return determineFix(file1, file2, getErrorOffset(error, file1)) < 0;
   				}
			}
			System.out.println("Error removed but may not have been fixed");
   			return true;
		   } catch (IOException e) { e.printStackTrace(); }
		   return true;
	}

	/**
	 * Downloads files in questions and determines if bug was actually fixed
	 * 
	 * @param base   Hash for base commit
	 * @param pull   Hash for PR commit
	 * @param error  Error in question
	 * @return       True if error prone bug was fixed, else false
	 */
	public static boolean isFix(String base, String pull, Error error) {
		String file1 = error.getFilePath();
		String file2 = file1.replace(projectName + "1", projectName+"2");
		//return false;
		return !isDeleteOnly(file1, file2, error);
	}

	/**
	 * Returns line number of code fix
	 * 
	 * @return   Line number of what is considered a fix or null if none
     */
	public static int getFix() {
		if (fixLine > 0) {
			return fixLine;
		} else if (fixLine == 0) {
			return 1;
		} else { // Shouldn't have been considered a fix
			return (Integer)null;
		}
	}

	/**
	 * Compiles the project to analyze code in the repository
	 * 
	 * @return   Output from the maven build
	 */
	public static String compile(String path) {
		String output = "";
		String cmd = MVN_COMPILE.replace("{dir}", path);
		try {
			Process p = Runtime.getRuntime().exec(cmd);	
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null) {
			    output += line + "\n";
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return output;
	}

	/**
	 * Modifies pom.xml file to add current tool plugin for maven build
	 * 
	 * @param file   Path to pom.xml file
	 * @param tool   Tool to analyze code
	 * @param file   New pom.xml file to write to
	 */
	public static void parseXML(String file, Tool tool, FileWriter writer) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				@Override
				public void startElement(String uri, String localName, String qName,
							Attributes attributes) throws SAXException {
					try {
						writer.write("<" + qName + ">");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void endElement(String uri, String localName,
					String qName) throws SAXException {
					try {
						if(qName.equals("plugins")) {
							writer.write(tool.getPlugin());
							writer.write("\n</plugins>");	
							myTool = true;							
						} else if (qName.equals("project") && !myTool) {
							writer.write(String.join("\n", "<build>", "<plugins>", 
								tool.getPlugin(), "</plugins>", "</build>", "</project>"));
							myTool = true;
						} else {
							writer.write("</" + qName + ">");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void characters(char ch[], int start, int length) throws SAXException {
					try {
						writer.write(new String(ch, start, length));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			saxParser.parse(file, handler);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Updates to pom.xml file to include tool plugin
	 * 
	 * @param dir   Current directory of the project
	 * @param tool  Static analysis tool to analyze code
	 */
	private static void addToolPomPlugin(String dir, Tool tool) {
		try{
			String pom = String.join("/",currentDir, dir, "pom.xml");
			File tempPom = new File(String.join("/",currentDir, dir, "pom.temp"));
			FileWriter writer = new FileWriter(tempPom, false);
			myTool = false;
			parseXML(pom, tool, writer);
			tempPom.renameTo(new File(pom));			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checkout pull request version of a git repository to analyze
	 * 
	 * @param pull   Pull Request number
	 * @param label  Pull Request branch name
	 * @param tool   Tool to perform analysis and recommend
	 * @return       Set of errors reported from tool
	 */
	public static Set<Error> checkout(int pull, String label, Tool tool) {
		/*Set<Error> errors = new HashSet<Error>();
		String cmds = CHECKOUT.replace("{num}", Integer.toString(pull)).replaceAll("_name", label).replaceAll("_project", projectName);
		try {
			cd(projectName);
			Git git = Git.open(new File(currentDir+"/.git"));
			File checkout = new File("checkout.sh");
			FileWriter fw = new FileWriter(checkout, false);
			fw.write(cmds);
			fw.close();
			Process p = Runtime.getRuntime().exec("sh checkout.sh");
			addToolPlugin(tool);
			String log = compile();
			System.out.println("2. "+log);
			errors = tool.parseOutput(log);
			//git.checkout().setName("master").call();			
			cd("..");		
		} catch (Exception e) {
			try {
				cd("..");
				e.printStackTrace();
			} catch (IOException io) { io.printStackTrace(); }
			return null;
		}*/
		return null;
	}

	/**
	 * Checkout specific version of a git repository to analyze
	 * 
	 * @param hash   Git SHA value
	 * @param author Creator of pull request
	 * @param tool   Tool to perform analysis and recommend
	 * @param base   True if checking base repo, false if PR version
	 * @return       Set errors reported from tool
	 */
	public static Set<Error> checkout(String hash, String author, Tool tool, boolean base) {
		String dirName = projectName;
		if(base) {
			dirName += "1";
		} else {
			dirName += "2";
		}
		try {
			Git git = Git.cloneRepository()
			.setURI("https://github.com/{owner}/{repo}.git".replace("{owner}", author)
				.replace("{repo}", projectName))
			.setDirectory(new File(dirName)).call();
			git.checkout().setName(hash).call();
			System.out.println(hash);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		addToolPomPlugin(dirName, tool);
		String log = compile(dirName);
		//System.out.println(dirName+". "+log);
		return tool.parseOutput(log);
	}

	public static void cleanup() {
		//Process p = Runtime.getRuntime().exec(REMOVE.replace("{project}", projectName));
	}

	/**
	 * Changes the current working directory of the program
	 * 
	 * @param dir   Directory to change into
	 */
	public static void cd(String dir) throws FileNotFoundException {
		String cmd = "cd " + dir;
		try {
			Process p = Runtime.getRuntime().exec(cmd);		
			if(!dir.equals("..")) {
				currentDir += "/" + dir;
			} else {
				currentDir = currentDir.substring(0, currentDir.lastIndexOf("/"));
			}
		} catch (IOException e) {
			throw new FileNotFoundException("Invalid directory name "+dir);			
		}
		/*if (dir.equals("..")) {
			if(currentDir.contains("/")) {
				directory = currentDir.substring(0, currentDir.lastIndexOf("/"));
				System.setProperty("user.dir", directory);
			} else {
				directory = currentDir;
			}
			currentDir = directory;		
		} else {
			directory = String.join("/", System.getProperty("user.dir"), dir);
			File f = new File(directory);
			if (f.exists() && f.isDirectory()) {
				System.setProperty("user.dir", directory);	
				currentDir = directory;				
			} else {
				throw new FileNotFoundException("Invalid directory name "+dir);
			}
		}*/
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
	 * Retrieve Github username and password. 
	 * Requires .github.creds file with username on the first line and password on the second line.
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
