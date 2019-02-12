package com.chbrown13.tool_rec;

import com.jcabi.github.Branch;
import com.jcabi.github.Coordinates;
import com.jcabi.github.Fork;
import com.jcabi.github.Pull;
import com.jcabi.github.Repo;
import com.jcabi.github.RepoCommit;
import com.jcabi.github.RtGithub;
import com.jcabi.github.User;
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
 * Recommender is the main class for tool-recommender-bot and handles interactions with Github repositories.
 */
public class Recommender {

	private Repo repository;
	private Git git;
	private String user;
	private String repo;
	private String name;
	private Tool tool = null;

	public Recommender(Repo repo, Git git) {
		this.repository = repo;
		this.git = git;
		this.tool = new ErrorProne();
		this.user = repo.coordinates().user();
		this.repo = repo.coordinates().repo();
		this.name = this.user + "/" + this.repo;
		Utils.setProjectName(this.repo);
		Utils.setProjectOwner(this.user);
	}

	/**
	 * Sends email to researchers for review
	 * 
	 * @param text    Contents of email message
	 * @param subject Subject of the email
	 * @param to	  Email address to send recommendation
	 */
	private boolean email(String to) {
		SimpleEmail email = new SimpleEmail();
		String[] emailAcct = Utils.getCredentials(".email.creds");
		System.out.println(to);
		try {
			email.setHostName("smtp.googlemail.com");
			email.setSmtpPort(465);
			email.setAuthenticator(new DefaultAuthenticator(emailAcct[0], emailAcct[1]));
			email.setSSLOnConnect(true);
			email.setFrom("toolrecommenderbot@gmail.com");
			email.setSubject("[tool-recommender-bot] Error Prone found Java errors in your project " + this.repo);
			email.setMsg(this.tool.getRec());
			email.addTo("dcbrow10@ncsu.edu");
			email.send();	
			return true;	
		} catch (Exception e) {
			System.out.println("Email error");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get errors for each
	 * 
	 * @param base  Commit hash for base version
	 * @param head  Commit hash for changed version
	 * @param id    Code change id
	 * @return      True if bugs reported or no errors
	 */
	private boolean getErrors(String head) {
		List<Error> errors = this.tool.parseOutput(Utils.loadFile("tool_{h}.txt".replace("{h}", head)));
		return errors != null;
	}

	/**
	 * Compiles the GitHub project
	 * 
	 * @param hash   Commit version	
	 * @return       True if project compiles
	 */
	public boolean build(String hash) {
		try {
			String pom = String.join(File.separator, this.repo, "pom.xml");
			File tempPom = new File(String.join(File.separator, this.repo, "tool.xml"));
			String toolPom = Utils.updatePom(pom, this.tool);
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
	 * Starts tool-recommender-bot
	 * 
	 * @return   True if recommendation submitted
	 */
	private boolean start() {
		String hash = "";
		try {
			hash = this.repository.commits().iterate(new HashMap<String, String>()).iterator().next().sha();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} catch (AssertionError ae) {
			ae.printStackTrace();
			return false;
		}
		if (build(hash) && getErrors(hash)) {
			return true;
		}
		return false;
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

	/**
	 * Forks the repository before making changes
	 * 
	 * @param github   Github object for original repo
	 * @param user     GitHub user to fork project
	 * @param repo     Project name
	 */
	private static void fork(RtGithub github, String user, String repo) {
		try {
			github.entry()
			.uri().path("/repos/{user}/{repo}/forks".replace("{user}",user).replace("{repo}", repo)).back()
			.method(Request.POST)
			.fetch()
			.as(JsonResponse.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String[] gitAcct = Utils.getCredentials(".github.creds");
		RtGithub github = null;
		if (gitAcct[1] != null) {
			github = new RtGithub(gitAcct[0], gitAcct[1]);
		} else {
			github = new RtGithub(gitAcct[0]);
		}
		String projects = Utils.loadFile("projects.txt"); // load list of projects from a text file
		for(String proj: projects.split("\n")) {
			System.out.println(proj);
			String[] info = proj.split("/");
			fork(github, info[0], info[1]);
			Git git = clone(Utils.getUsername(), info[1]);
			if (git != null) {
				Repo repo = github.repos().get(new Coordinates.Simple(info[0], info[1]));
				Recommender toolBot = new Recommender(repo, git);
				boolean rec = toolBot.start();
				if (rec) {
					try {
						User user = github.users().get(info[0]);
						User.Smart u = new User.Smart(user);
						boolean sent = toolBot.email(u.email());
						if (sent) {
							BufferedWriter out = new BufferedWriter( 
								new FileWriter("recommended.txt", true)); // Recommended projects written here 
							out.write(proj + " " + u.email() + "\n"); 
							out.close(); 
						}
					} catch (ClassCastException cce) {
						System.out.println("No email address");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
