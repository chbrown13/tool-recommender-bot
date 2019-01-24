package com.chbrown13.tool_rec;

import com.jcabi.github.Branch;
import com.jcabi.github.Coordinates;
import com.jcabi.github.Pull;
import com.jcabi.github.Repo;
import com.jcabi.github.RepoCommit;
import com.jcabi.github.RtGithub;
import com.jcabi.http.response.*;
import com.jcabi.http.Request;
import java.io.*;
import java.net.HttpURLConnection;
import java.text.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.*;
import org.apache.commons.mail.*;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.*;
import org.eclipse.jgit.util.io.*;


/**
 * Recommender is the main class for this project and handles interactions with Github repositories.
 */
public class Recommender {

	private Repo repository;
	private Git git;
	private String user;
	private String repo;
	private String name;
	private Set<String> fixes = new HashSet<String>();	
	private List<String> changes;
	private Tool tool = null;
	private static String type = "";
	private static String log = "";
	private static String stats = "";
	private static int recs = 0;
	private static int sim = 0;

	public static final String COMMIT = "commit";

	public Recommender(Repo repo, Git git) {
		this.repository = repo;
		this.git = git;
		this.tool = new ErrorProne();
		this.changes = new ArrayList<String>();
		this.user = repo.coordinates().user();
		this.repo = repo.coordinates().repo();
		this.name = this.user + "/" + this.repo;
		Utils.setProjectName(this.repo);
		Utils.setProjectOwner(this.user);
	}

	/**
	 * Reset class variables for each PR
	 */
	private void reset() {
		this.stats = "";
		this.log = "";
		try {
			Set<String> path = new HashSet<String>();
			path.add("pom.xml");
			path.add("target/");
			this.git.clean().setPaths(path).call();
			this.git.reset().setMode(ResetType.HARD).call();
			this.git.checkout().setName("refs/heads/master").call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * tool-recommender-bot logging
	 */
	private void log(String msg) {
		System.out.println(msg);
		this.log += "\n\n" + msg + "\n";
	}

	/**
	 * Get errors for each
	 * 
	 * @param base  Commit hash for base version
	 * @param head  Commit hash for changed version
	 * @param id    Code change id
	 */
	private boolean getErrors(String head) {
		List<Error> errors = this.tool.parseOutput(Utils.loadFile("tool_{h}.txt".replace("{h}", head)));
		System.out.println(errors.size());
		return errors.size() > 0;
	}

	/**
	 * Compiles the GitHub project
	 * 
	 * @param hash   Commit version	
	 */
	public boolean build(String hash) {
		try {
			String pom = String.join(File.separator, this.repo, "pom.xml");
			File tempPom = new File(String.join(File.separator, this.repo, "tool.xml"));
			String toolPom = Utils.updatePom(pom, this.tool);;
			if(toolPom != null) {
				FileWriter writer = new FileWriter(tempPom, false);
				writer.write(toolPom);
				writer.close();	
				tempPom.renameTo(new File(pom));
				boolean compile = Utils.compile(this.repo, hash);
				return compile;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Analyze code of files in commits and compare to base branch.
	 *
	 * @param commit   Current commit
	 */
	private boolean analyze() {
		String hash = this.repository.commits().iterate(new HashMap<String, String>()).iterator().next().sha();
		try {
			this.git.checkout().setCreateBranch(true).setForce(true).setName("tool-rec-bot5").setStartPoint(hash).call();
		} catch (GitAPIException e) {
			e.printStackTrace();
			return false;
		}
		return build(hash) && getErrors(hash);
	}

	/**
	 * Formats information from analysis to send in review email
	 * 
	 * @param id   ID for GitHub code change (Commit hash/Pull Request number)
	 */
	private void results(String id) {
		log(this.stats);
		// sendEmail(this.log, "New " + this.type + ": " + this.name + " " + id, id);
		reset();
		this.recs = 0;
		this.sim = 0;
		this.stats = "";
	}

	/**
	 * Clone a git repository to make recommendation.
	 * 
	 * @param user   Git username
	 * @param repo   Git project name
	 * @return       Git object
	 */
	private static Git clone(String user, String repo) {
		Git git = null;
		try {
			git = Git.cloneRepository()
			.setURI("https://github.com/{owner}/{repo}.git".replace("{owner}", user).replace("{repo}", repo))
			.setCredentialsProvider(new UsernamePasswordCredentialsProvider(Utils.getUsername(), Utils.getPassword()))
			.setDirectory(new File(repo))
			.setCloneAllBranches(true).call();
		} catch (Exception e) {
			try {
				git = Git.open(new File(repo + File.separator + ".git"));
			} catch (IOException io) {
				e.printStackTrace();
			}
		}
		return git;
	}

	public static void main(String[] args) {
		String[] gitAcct = Utils.getCredentials(".github.creds");
		RtGithub github = null;
		if (gitAcct[1] != null) {
			github = new RtGithub(gitAcct[0], gitAcct[1]);
		} else {
			github = new RtGithub(gitAcct[0]);
			gitAcct[1] = "";
		}
		Git git = clone(args[0], args[1]);
		if (git != null) {
			Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
			Recommender toolBot = new Recommender(repo, git);
			boolean b = toolBot.analyze();
			System.out.println(b);
			if (b) {
				 System.out.println(String.join(File.separator, args[1], "pom.xml"));
				try {
					git.add().addFilepattern("pom.xml").call();
					git.commit().setMessage("Error Prone\n\nAdding the Error Prone tool to automatically check for Java errors")
					.setAuthor("tool-recommender-bot", "toolrecommenderbot@gmail.com")
					.setCommitter("tool-recommender-bot", "toolrecommenderbot@gmail.com").call();
					git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider("username", "password")).call();
				} catch (GitAPIException e) {
					e.printStackTrace();
				}
				try {
					Iterator<Branch> bs = repo.branches().iterate().iterator();
					while(bs.hasNext()) {
						System.out.println(bs.next().name());
					}
					Pull.Smart p = new Pull.Smart(repo.pulls().create("Error Prone Static Analysis Tool", "tool-rec-bot5", "master"));
					p.body("body yo");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//toolBot.reset();
		}
	}
}
