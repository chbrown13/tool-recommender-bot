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
	 * Get errors for each
	 * 
	 * @param base  Commit hash for base version
	 * @param head  Commit hash for changed version
	 * @param id    Code change id
	 */
	private boolean getErrors(String head) {
		List<Error> errors = this.tool.parseOutput(Utils.loadFile("tool_{h}.txt".replace("{h}", head)));
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
			this.git.checkout().setCreateBranch(true).setForce(true).setName("tool-rec-bot7").setStartPoint(hash).call();
		} catch (GitAPIException e) {
			e.printStackTrace();
			return false;
		}
		return build(hash) && getErrors(hash);
	}

	private void commit() {
		String user = "tool-recommender-bot";
		String email = "toolrecommenderbot@gmail.com";
		try {
			this.git.add().addFilepattern("pom.xml").call();
			this.git.commit().setMessage("Error Prone\n\nAdding the Error Prone tool to automatically check for Java errors")
			.setAuthor(user, email)
			.setCommitter(user, email).call();
			this.git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider("username", "password")).call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	private void recommend() {
		if (analyze()) {
			commit();			
			try {
				Pull.Smart p = new Pull.Smart(this.repository.pulls().create("Error Prone Static Analysis Tool", "tool-rec-bot7", "master"));
				p.body("body yo");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
			toolBot.recommend();
		}
		//toolBot.reset();
	}
}
