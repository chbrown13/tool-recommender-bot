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
	private String date = "YYYY-MM-DDTHH:MM:SSZ";

	public static final String PULL = "pull";
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
		//Utils.cleanup(this.repo);
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
	 * Sends email to researchers for review
	 * 
	 * @param text    Contents of email message
	 * @param subject Subject of the email
	 * @param id	  Code change id (PR number or commit hash)
	 */
	private void sendEmail(String text, String subject, String id) {
		SimpleEmail email = new SimpleEmail();
		String viewChanges = Comment.changes.replace("{user}", this.user)
			.replace("{repo}", this.repo).replace("{type}", this.type)
			.replace("{num}", id);
		if (this.type.equals(PULL)) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create message recommending ErrorProne to Github code changes.
	 *
	 * @param id	 Code change id (PR number or commit hash)
	 * @param error  Fixed error 
	 * @param line   Line number of fix
	 * @param head   Hash of code changes
	 */
	private void makeRecommendation(String comment, Error error, int line, String head) {
		if (comment != null && comment != "") {
			if(comment.contains("similar")) {
				sim += 1;
			}
			String args = "";
			if (this.type.equals(PULL)) {
				args = String.join(" ", this.user, this.repo, PULL,
				error.getId(), "\""+comment+"\"", head, error.getLocalFilePath(), 
				Integer.toString(line));
			} else if (this.type.equals(COMMIT)) {
				args = String.join(" ", this.user, this.repo, COMMIT,
				error.getId(), "\""+comment+"\"", error.getLocalFilePath(), Integer.toString(line));
			}
			String run = Comment.cmd.replace("{args}", args);
			sendEmail(String.join("\n", Comment.compile, run), "Recommendation Review: " + this.name + " " + error.getId(), error.getId());
			recs += 1;
		}
	} 

	/**
	 * Check if errors are fixed or added in pull request.
	 * 
	 * @param baseErrors   Errors reported in original version
	 * @param headErrors   Errors reported in modified code
	 * @param base         Original commit hash
	 * @param head         Updated commit hash
	 * @param id           Pull request number
	 */
	private void checkFix(List<Error> baseErrors, List<Error> headErrors, String base, String head, String id) {
		String comment = "";
		List<Error> fixed = new ArrayList<Error>();	
		List<Error> added = new ArrayList<Error>();
		List<Error> removed = new ArrayList<Error>();
		if(baseErrors != null && headErrors != null) {
			log("base");
			for (Error e: baseErrors) {
				//System.out.println(e.getKey());
				if (!headErrors.contains(e)) {
					if (Utils.isFix(e, base, head)) {
						e.setId(id);
						fixed.add(e);
						int line = Utils.getFix(id, this.type, e);			
						log("Fixed "+ e.getKey() + " in line " + e.getLineStr() + 
							" fixed at line " + Integer.toString(line));
						comment = e.generateComment(this.tool, baseErrors, base, true);
						makeRecommendation(comment, e, line, base);
					} else {
						removed.add(e);
						log("Removed "+ e.getKey() + " in line " + e.getLineStr());
					}
				}
			}
			stats += "Total base: " + Integer.toString(baseErrors.size()) + "\n";
			stats += "Fixed: " + Integer.toString(fixed.size()) + "\n";
			stats += "Removed: " + Integer.toString(removed.size()) + "\n";
			log("head");
			for (Error e: headErrors) {
				//System.out.println(e.getKey());
				if (!baseErrors.contains(e)) {
					e.setId(id);
					added.add(e);
					log("Added "+ e.getKey() + " in line " + e.getLineStr());
					comment = e.generateComment(this.tool, headErrors, head, false);
					makeRecommendation(comment, e, e.getLine(), head);
				}
			}
			stats += "Total head: " + Integer.toString(headErrors.size()) + "\n";
			stats += "Introduced: " + Integer.toString(added.size()) + "\n";
			stats += "Similar: " + Integer.toString(sim) + "\n";
		}
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
	 * Get errors for each
	 * 
	 * @param base  Commit hash for base version
	 * @param head  Commit hash for changed version
	 * @param id    Code change id
	 */
	private boolean getErrors(String base, String head, String id) {
		List<Error> baseErrors = null;
		List<Error> headErrors = null;			
		headErrors = this.tool.parseOutput(Utils.loadFile("tool_{h}.txt".replace("{h}", head)));
		String baseFile = "tool_{b}.txt".replace("{b}", base);
		if (new File(baseFile).exists()) {
			System.out.println(baseFile + " exists");
		} else {
			try {
				reset();
				this.git.checkout().setCreateBranch(true).setForce(true).setName("basehead_"+id ).setStartPoint(base).call();
			} catch (GitAPIException e) {
				e.printStackTrace();
				System.out.println("Git base checkout error");
				return false;
			} catch (JGitInternalException j) {
				j.printStackTrace();
				System.out.println("Jgit base checkout error");
				return false;
			}
			if(!build(base)) {
				System.out.println("Compile base 1st error");
				return false;
			}
			System.out.println("Compile build base");
		}
		//System.out.println(Utils.loadFile("tool_{h}.txt".replace("{h}", base)));
		baseErrors = this.tool.parseOutput(Utils.loadFile(baseFile));
		System.out.println(headErrors.size());
		System.out.println(baseErrors.size());
		checkFix(baseErrors, headErrors, base, head, id);
		stats += "Recommendations: " + Integer.toString(recs) + "\n";
		return true;
	}

	private boolean checkout(String base, String head, String id) {
		System.out.println("https://github.com/{owner}/{repo}.git"
		.replace("{owner}", this.user).replace("{repo}", this.repo));
		try {
			this.git.checkout().setCreateBranch(true).setForce(true).setName("tool-rec-bot-"+id).setStartPoint(head).call();
		} catch (Exception e) {
			e.printStackTrace();
			log("Git checkout error");
			return false;
		}
		System.out.println(base + "---" + head);
		return true;
	}

	/**
	 * Check if java files are modified in code change
	 * 
	 * @param files   JsonArray of files changed in commit or pull request
	 * @return        True if java files changed, false if no java changes
	 */
	private boolean javaFiles(JsonArray files) {
		for (int i = 0; i < files.size(); i++) {
			JsonObject file = files.getJsonObject(i);
			if (file.getString("filename").endsWith(".java")) {
				return true;
			}
		}
		log("No java changes.");
		return false;
	}

	/**
	 * Analyze code of files in pull request and compare to base branch.
	 *
	 * @param pull   Current pull request
	 */
	private boolean analyze(Pull.Smart pull) {
		log("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		this.type = PULL;
		String hash = "";
		String newHash = "";
		String newOwner = "";
		String newRepo = "";
		try {
			if (javaFiles(pull.json().getJsonArray("files"))) {
				JsonObject head = pull.json().getJsonObject("head");
				hash = pull.json().getJsonObject("base").getString("sha");
				newHash = head.getString("sha");
				newOwner = head.getJsonObject("user").getString("login");
				newRepo = head.getJsonObject("repo").getString("name");
			} else {
				return false;
			}
		} catch (Exception e) {
				e.printStackTrace();
				return false;
		}	
		if(checkout(hash, newHash, Integer.toString(pull.number()))) {
			if (build(newHash)) {
				return getErrors(hash, newHash, Integer.toString(pull.number()));
			}
		}		
		return false;
	}

	/**
	 * Analyze code of files in commits and compare to base branch.
	 *
	 * @param commit   Current commit
	 */
	private boolean analyze(RepoCommit.Smart commit) {
		log("Analyzing commit " + commit.sha() + "...");
		this.type = COMMIT;
		String hash = commit.sha();
		String oldHash = "";		
		String id = commit.sha().substring(0, 7);
		try {
			if (javaFiles(commit.json().getJsonArray("files"))) {
				oldHash = commit.json().getJsonArray("parents").getJsonObject(0).getString("sha");
				JsonObject head = commit.json().getJsonObject("head");
			} else {
				return false;
			}
		 } catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if(checkout(oldHash, hash, id)) {
			if (build(hash)) {
				return getErrors(oldHash, hash, id);
			}
		}
		return false;
	}

	/**
	 * Formats information from analysis to send in review email
	 * 
	 * @param id   ID for GitHub code change (Commit hash/Pull Request number)
	 */
	private void results(String id) {
		log(this.stats);
		sendEmail(this.log, "New " + this.type + ": " + this.name + " " + id, id);
		reset();
		this.recs = 0;
		this.sim = 0;
		this.stats = "";
	}

	/**
	 * Searches for new pull requests opened for a Github repository every 15 minutes.
	 * 
	 * @return    List of new pull requests
	 */
	private void getPullRequests() {
		log("Checking for new pull requests...");
		ArrayList<Pull.Smart> changes = new ArrayList<Pull.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", "open");
		params.put("since", Utils.getTime());
		Iterator<Pull> pulls = this.repository.pulls().iterate(params).iterator();
		int i = 0;
		while (pulls.hasNext()) {
			Pull.Smart pull = new Pull.Smart(pulls.next());
			//if (new Date().getTime() - pull.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
				if (analyze(pull)) {
					changes.add(pull);
					results(Integer.toString(pull.number()));
				} else {
					reset();
				}
			//}
			i += 1;
			System.out.println(pull.number() + " " + i);
			
		}
		if (changes.isEmpty()) {
			log("No new pull requests");
		}
	}

	/**
	 * Searches for new commits made to Github repositories every 15 minutes.
	 * 
	 * @param branch Current branch
	 * @param hash   Commit hash of current branch head
	 * @return  List of new commits
	 */
	private void getCommits() {
		log("Checking for new commits...");
		ArrayList<RepoCommit.Smart> changes = new ArrayList<RepoCommit.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("since", Utils.getTime());
		Iterator<RepoCommit> commits = this.repository.commits().iterate(params).iterator();
		int i = 0;
		while (commits.hasNext()) {
			RepoCommit.Smart commit = new RepoCommit.Smart(commits.next());
			if (analyze(commit)) {
				changes.add(commit);
			}
			results(commit.sha());
			reset();
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
			// toolBot.getPullRequests();
			toolBot.getCommits();
			Utils.cleanup(args[1]);
		}
	}
}
