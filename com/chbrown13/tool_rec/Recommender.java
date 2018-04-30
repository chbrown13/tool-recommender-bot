package com.chbrown13.tool_rec;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Pull;
import com.jcabi.github.Pull.Smart;
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
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.transport.*;


/**
 * Recommender is the main class for this project and handles interactions with Github repositories.
 */
public class Recommender {

	private Repo repo;
	private Git git;
	private Set<String> fixes = new HashSet<String>();	
	private List<String> changes = new ArrayList<String>();
	private int recs = 0;
	private int rem = 0;
	private int fix = 0;
	private int intro = 0;
	private String removed = "";
	private String introduced = "";
	private String noSimilar = "";
	private int baseErrorCount = 0;
	private int newErrorCount = 0;
	private int baseErrorCountFiles = 0;
	private int newErrorCountFiles = 0;
	private Tool tool = null;
	private static String type = "";
	private static String log = "";
	

	public static final String PULL = "pull";
	public static final String COMMIT = "commit";

	public Recommender(Repo repo, Git git) {
		this.repo = repo;
		this.git = git;
		Utils.setProjectName(repo.coordinates().repo());
		Utils.setProjectOwner(repo.coordinates().user());
	}

	/**
	 * Reset class variables for each PR
	 */
	private void reset() {
		removed = "";
		introduced = "";
		noSimilar = "";
		baseErrorCount = 0;
		baseErrorCountFiles = 0;
		newErrorCount = 0;
		newErrorCountFiles = 0;
		intro = 0;
		recs = 0;
		rem = 0;
		fix = 0;
		log = "";
	}

	private void log(String msg) {
		System.out.println(msg);
		log += "\n\n" + msg + "\n";
	}

	/**
	 * Sends email to researchers for review
	 * 
	 * @param text    Contents of email message
	 * @param subject Subject of the email
	 * @param id	  Code change id (PR number or commit hash)
	 */
	private static void sendEmail(String text, String subject, String id) {
		SimpleEmail email = new SimpleEmail();
		String viewChanges = Comment.changes.replace("{user}", Utils.getProjectOwner())
			.replace("{repo}", Utils.getProjectName()).replace("{type}", type)
			.replace("{num}", id);
		if (type.equals(PULL)) {
			viewChanges += "files/";
		}
		String[] emailAcct = Utils.getCredentials(".email.creds");
		text += "\n" + log;
		try {
			email.setHostName("smtp.googlemail.com");
			email.setSmtpPort(465);
			email.setAuthenticator(new DefaultAuthenticator(emailAcct[0], emailAcct[1]));
			email.setSSLOnConnect(true);
			email.setFrom("toolrecommenderbot@gmail.com");
			email.setSubject("[tool-recommender-bot] " + subject);
			email.setMsg(String.join("\n", viewChanges, text));
			email.addTo("dcbrow10@ncsu.edu");
			email.send();		
			//log("Email sent for review: " + subject);	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create message recommending ErrorProne to Github code changes.
	 *
	 * @param tool   Tool to recommend
	 * @param id	 Code change id (PR number or commit hash)
	 * @param error  Fixed error 
	 * @param line   Line number of fix
	 * @param errors List of errors
	 * @param base   Original hash
	 * @param head   Hash of code changes
	 */
	private void makeRecommendation(Tool tool, String id, Error error, int line, List<Error> errors, String base, String head) {
		String comment = error.generateComment(tool, errors, head);
		if (comment != null) {
			String link = "\n\n" + Utils.SURVEY.replace("{project}", Utils.getProjectName()).replace("{id}", id);
			comment += link;
			String args = "";
			if (this.type.equals(PULL)) {
				args = String.join(" ", Utils.getProjectOwner(), Utils.getProjectName(), PULL,
				id, "\""+comment+"\"", head, error.getLocalFilePath(), 
				Integer.toString(line));
			} else if (this.type.equals(COMMIT)) {
				args = String.join(" ", Utils.getProjectOwner(), Utils.getProjectName(), COMMIT,
				id, "\""+comment+"\"", error.getLocalFilePath(), Integer.toString(line));
			}
			String run = Comment.cmd.replace("{args}", args);
			sendEmail(String.join("\n", Comment.compile, run), "Recommendation Review", id);
			recs += 1;
			fixes.add(id);
		} else {
			fix += 1;
			noSimilar += "None similar to " + error.getLog() + "\n";
		}
	}

	/**
	 * Checks if the change is actually a fix or not
	 */
	private boolean checkFix(List<Error> baseErrors, List<Error> changeErrors, String base, String head, String id) {
		boolean fix = false;
		if(baseErrors != null && changeErrors != null) {
			List<Error> fixed = new ArrayList<Error>();	
			List<Error> added = new ArrayList<Error>();
			log("base");
			for (Error e: baseErrors) {
				baseErrorCount += 1;
				//if (files.contains(e.getLocalFilePath())) {
					baseErrorCountFiles += 1;
					log("\n\n" + e.getKey() + "- " + e.getLog());
					if (!changeErrors.contains(e) || Collections.frequency(baseErrors, e) > Collections.frequency(changeErrors, e)) {
						fixed.add(e);
					}
				//}
			}
			log("change");
			for (Error e: changeErrors) {
				newErrorCount += 1;
				//if (files.contains(e.getLocalFilePath())) {
					newErrorCountFiles += 1;
					log("\n\n" + e.getKey() + "- " + e.getLog());
					if (!baseErrors.contains(e) || Collections.frequency(baseErrors, e) < Collections.frequency(changeErrors, e)) {
						added.add(e);
						intro += 1;
						introduced += "-" + e.getLog() + "\n";
					}
				//}
			}
			log(Integer.toString(baseErrorCount) + "------" + Integer.toString(newErrorCount) + "\n"); 
			log(Integer.toString(baseErrorCountFiles) + "------" + Integer.toString(newErrorCountFiles) + " (files)"); 
			for (Error e: fixed) {
				if (Utils.isFix(e, base, head)) {
					fix = true;
					int line = Utils.getFix(id, type, e);			
					log("Fixed "+ e.getKey() + " reported at line " + e.getLineNumberStr() + 
						" possibly fixed at line " + Integer.toString(line));
					makeRecommendation(tool, id, e, line, changeErrors, base, head);
				} else {
					rem += 1;
					removed += "-" + e.getLog() + "\n";
				}
			}
		}
		return fix;
	}

	/**
	 * Checkout code versions and get errors for each
	 * 
	 * @param git   Git repository object
	 * @param base  Commit hash for base version
	 * @param head  Commit hash for changed version
	 * @param id    Code change id
	 */
	private boolean checkout (Git git, String base, String head, String id) {
		this.changes.add(base);
		this.changes.add(head);
		List<Error> baseErrors = null;
		List<Error> changeErrors = null;
		Set<String> path = new HashSet<String>();
		try {
			git.checkout().setName(base).call();
			baseErrors = Utils.getErrors(git, base, tool);
			log("base: " + base);
			git.checkout().setName(head).call();
			changeErrors = Utils.getErrors(git, head, tool);
			log("head: " + head);
			path.add("pom.xml");
			git.clean().setPaths(path).call();
			git.reset().setMode(ResetType.HARD).call();
		} catch (GitAPIException e) {
			e.printStackTrace();
			log("Git checkout error");
			return false;
		}
		if (baseErrors == null || changeErrors == null) {
			log("Maven compile error");
			return false;
		} else if (baseErrors.size() == 0) {
			log("No errors found");
			return false;
		} else {
			System.out.println(baseErrors.size());
			System.out.println(changeErrors.size());
			return checkFix(baseErrors, changeErrors, base, head, id);
		}
	}

	/**
	 * Analyze code of files in pull request and compare to base branch.
	 *
	 * @param pull   Current pull request
	 */
	private boolean analyze(Git git, Pull.Smart pull) {
		log("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		this.type = PULL;
		tool = new ErrorProne();		
		List<String> javaFiles = new ArrayList<String>();
		String hash = "";
		String newHash = "";
		Iterator<JsonObject> files = null;
		try {
			files = pull.files().iterator();			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		while (files.hasNext()) {
			JsonObject f = files.next();
			if (f.getString("filename").endsWith(".java") && f.getString("status").equals("modified")) {
				javaFiles.add(f.getString("filename"));
			} else if (f.getString("filename").equals("pom.xml")) {
				log("\n\nModified pom.xml\n\n");
				return false;
			}
		}
		if (javaFiles.size() == 0) {
			log("\n\nNo java changes\n\n");
			return false;
		}
		try {
			JsonObject head = pull.json().getJsonObject("head");
			hash = pull.json().getJsonObject("base").getString("sha");
			newHash = head.getString("sha");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}	
		return checkout(git, hash, newHash, Integer.toString(pull.number()));
	}

	/**
	 * Analyze code of files in commits and compare to base branch.
	 *
	 * @param commit   Current commit
	 */
	private boolean analyze(Git git, RevCommit commit) {
		log("Analyzing commit " + ObjectId.toString(commit.getId()) + "...");
		this.type = COMMIT;
		tool = new ErrorProne();
		String hash = "";
		String oldHash = "";		
		List<String> javaFiles = new ArrayList<String>();
		try {
			if (commit.getParentCount() == 0 || commit.getParentCount() > 1) {
				log("Parent count is " + Integer.toString(commit.getParentCount()));
				return false;
			}
			hash = ObjectId.toString(commit.getId());
			oldHash = ObjectId.toString(commit.getParent(0).getId());
			if (this.changes.contains(hash) || this.changes.contains(oldHash)) {
				log("Already seen this code change in a previous branch");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return checkout(git, oldHash, hash, hash);
	}

	/**
	 * Formats information from analysis to send in review email
	 * 
	 * @param id   ID for GitHub code change (Commit hash/Pull Request number)
	 */
	private String results(String id) {
		String out = "Recommendations: {rec}\nRemoved: {rem}\n{err}Fixes: {sim}\n{simErr}Introduced: {intro}\n{new}"
			.replace("{rec}", Integer.toString(recs))
			.replace("{rem}", Integer.toString(rem))
			.replace("{err}", removed)
			.replace("{sim}", Integer.toString(fix))
			.replace("{simErr}", noSimilar)
			.replace("{intro}", Integer.toString(intro))
			.replace("{new}", introduced);
		sendEmail(out, "New " + type, id);
		log(out);
		reset();
		return out;
	}

	/**
	 * Searches for new pull requests opened for a Github repository every 15 minutes.
	 * @param git Git repository object
	 * @return    List of new pull requests
	 */
	private void getPullRequests(Git git) {
		log("Getting pull requests...");
		ArrayList<Pull.Smart> requests = new ArrayList<Pull.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", "open");
		Iterator<Pull> pullit = this.repo.pulls().iterate(params).iterator();
		while (pullit.hasNext()) {
			Pull.Smart pull = new Pull.Smart(pullit.next());
			try {
				if (new Date().getTime() - pull.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					analyze(git, pull);					
					requests.add(pull);
					String out = results(Integer.toString(pull.number()));
				} else {
					if (requests.isEmpty()) {
						log("No new pull requests");
					}
					break;
				}
				git.checkout().setName("refs/heads/master").call();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * Searches for new commits made to Github repositories every 15 minutes.
	 * 
	 * @param git    Git repository object
	 * @param branch Current branch
	 * @param hash   Commit hash of current branch head
	 * @return  List of new commits
	 */
	private void getCommits(Git git, String branch, String hash) {
		log("Getting commits from {branch} branch...".replace("{branch}", branch));
		ArrayList<RevCommit> commits = new ArrayList<RevCommit>();
		try {
			git.checkout().setName(hash).call();
			Iterator<RevCommit> revCommits = git.log().call().iterator();
			while (revCommits.hasNext()) {
				RevCommit commit = revCommits.next();
				long commitTime = (long) commit.getCommitTime() * 1000;
				long now = new Date().getTime();
				if (now - commitTime <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					System.out.println(ObjectId.toString(commit.getId()));
					analyze(git, commit);
					commits.add(commit);
					String out = results(ObjectId.toString(commit.getId()));
				} else { 
					if (commits.isEmpty()) {
						log("No new commits");
					}
					break;
				 }
			}
			git.checkout().setName("refs/heads/master").call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String[] gitAcct = Utils.getCredentials(".github.creds");
		RtGithub github = null;
		Git git = null;
		if (gitAcct[1] != null) {
			github = new RtGithub(gitAcct[0], gitAcct[1]);
		} else {
			github = new RtGithub(gitAcct[0]);
			gitAcct[1] = "";
		}
		try {
			git = Git.cloneRepository()
			.setURI("https://github.com/{owner}/{repo}.git"
				.replace("{owner}", args[0]).replace("{repo}", args[1]))
			.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitAcct[0], gitAcct[1]))
			.setDirectory(new File(args[1]))
			.setCloneAllBranches(true).call();
		} catch (JGitInternalException jgie) {
			try {
				git = Git.open(new File("{repo}/.git".replace("{repo}", args[1])));
				git.pull().call();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception cloneE) {
			cloneE.printStackTrace();
		}
		Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		Recommender toolBot = new Recommender(repo, git);
		toolBot.getPullRequests(git);
		try {
			Collection<Ref> refs = git.lsRemote().call();
			for (Ref r: refs) {
				if (r.getName().startsWith("refs/heads/")) {
					String branch = r.getName().split("/")[2];
					String hash = ObjectId.toString(r.getObjectId());
					toolBot.getCommits(git, branch, hash);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
