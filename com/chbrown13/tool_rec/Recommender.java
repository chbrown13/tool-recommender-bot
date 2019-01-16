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
	private Set<String> fixes = new HashSet<String>();	
	private List<String> changes;
	private Tool tool = null;
	private static String type = "";
	private static String log = "";
	private static String stats = "";
	private static int recs = 0;
	private static int sim = 0;

	public static final String PULL = "pull";
	public static final String COMMIT = "commit";

	public Recommender(Repo repo) {
		this.repository = repo;
		this.git = null;
		this.tool = new ErrorProne();
		this.changes = new ArrayList<String>();
		this.user = repo.coordinates().user();
		this.repo = repo.coordinates().repo();
		Utils.setProjectName(repo.coordinates().repo());
		Utils.setProjectOwner(repo.coordinates().user());
	}

	/**
	 * Reset class variables for each PR
	 */
	private void reset() {
		this.recs = 0;
		this.sim = 0;
		this.stats = "";
		Utils.cleanup(this.repo);
		/*try {
			Set<String> path = new HashSet<String>();
			path.add("pom.xml");
			path.add("target/");
			this.git.clean().setPaths(path).call();
			this.git.reset().setMode(ResetType.HARD).call();
			this.git.checkout().setName("refs/heads/master").call();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	/**
	 * tool-recommender-bot logging
	 */
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
			sendEmail(String.join("\n", Comment.compile, run), "Recommendation Review " + this.repo, error.getId());
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
			String pom = String.join("/", this.repo, "pom.xml");
			File tempPom = new File(String.join("/", this.repo, "tool.xml"));
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
				this.git.checkout().setCreateBranch(true).setForce(true).setName("basehead").setStartPoint(base).call();
			} catch (GitAPIException e) {
				System.out.println("Git base checkout error");
				return false;
			} catch (JGitInternalException j) {
				System.out.println("Git base checkout error");
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

	private boolean checkout(String user, String repo, String base, String head, String id) {
		System.out.println("https://github.com/{owner}/{repo}.git"
		.replace("{owner}", user).replace("{repo}", repo));
		try {
			this.git = Git.cloneRepository()
			.setURI("https://github.com/{owner}/{repo}.git"
				.replace("{owner}", user).replace("{repo}", repo))
			.setCredentialsProvider(new UsernamePasswordCredentialsProvider(Utils.getUsername(), Utils.getPassword()))
			.setDirectory(new File(repo))
			.setCloneAllBranches(true).call();
			this.git.checkout().setCreateBranch(true).setForce(true).setName("pull").setStartPoint(head).call();
		} catch (Exception e) {
			e.printStackTrace();
			log("Git checkout error");
			return false;
		}
		System.out.println(base + "---" + head);
		return true;
	}
	/**
	 * Analyze code of files in pull request and compare to base branch.
	 *
	 * @param pull   Current pull request
	 */
	private boolean analyze(Pull.Smart pull) {
		log("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		this.type = PULL;
		List<String> javaFiles = new ArrayList<String>();
		String hash = "";
		String newHash = "";
		String newOwner = "";
		String newRepo = "";
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
			} /*else if (f.getString("filename").equals("pom.xml")) {
				log("\n\nModified pom.xml\n\n");
				return false;
			}*/
		}
		if (javaFiles.size() == 0) {
			log("\n\nNo java changes\n\n");
			return false;
		}
		try {
			JsonObject head = pull.json().getJsonObject("head");
			hash = pull.json().getJsonObject("base").getString("sha");
			newHash = head.getString("sha");
			newOwner = head.getJsonObject("user").getString("login");
			newRepo = head.getJsonObject("repo").getString("name");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}	
		if(checkout(newOwner, newRepo, hash, newHash, Integer.toString(pull.number()))) {
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
	private boolean analyze(RevCommit commit) {
		log("Analyzing commit " + ObjectId.toString(commit.getId()) + "...");
		this.type = COMMIT;
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
			Repository repository = this.git.getRepository();
			RevWalk rw = new RevWalk(repository);
			RevCommit parent = rw.parseCommit(ObjectId.fromString(oldHash));
			DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
			df.setRepository(repository);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);
			List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
			for (DiffEntry diff: diffs) {
				String f = diff.getNewPath();
				if (f.endsWith(".java")) {
					javaFiles.add(f);
				} else if (f.endsWith("pom.xml")) {
					log("\n\nModified pom.xml\n\n");
					return false;
				}
			}
			if (javaFiles.size() == 0) {
				log("\n\nNo java changes\n\n");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		try {
			this.git2 = Git.cloneRepository()
			.setURI("https://github.com/{owner}/{repo}.git"
				.replace("{owner}", Utils.getProjectOwner()).replace("{repo}", Utils.getProjectName()))
			.setCredentialsProvider(new UsernamePasswordCredentialsProvider(Utils.getUsername(), Utils.getPassword()))
			.setDirectory(new File(Utils.getProjectName()+"2"))
			.setCloneAllBranches(true).call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return checkout(oldHash, hash, hash);
	}

	/**
	 * Formats information from analysis to send in review email
	 * 
	 * @param id   ID for GitHub code change (Commit hash/Pull Request number)
	 */
	private void results(String id) {
		String out = "";
		sendEmail(this.stats, "New " + this.type, id);
		log(this.stats);
		reset();
	}

	/**
	 * Searches for new pull requests opened for a Github repository every 15 minutes.
	 * 
	 * @return    List of new pull requests
	 */
	private void getPullRequests(String n) {
		log("Getting pull requests...");
		ArrayList<Pull.Smart> requests = new ArrayList<Pull.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", "open");
		Iterator<Pull> pullit = this.repository.pulls().iterate(params).iterator();
		int i = 0;
		while (pullit.hasNext()) {
			Pull.Smart pull = new Pull.Smart(pullit.next());
			System.out.print(pull.number());
			System.out.println(" " + n);
			try {
				if (i < Integer.parseInt(n)) { //new Date().getTime() - pull.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					if (analyze(pull)) {
						requests.add(pull);
						results(Integer.toString(pull.number()));
					} else {
						reset();
					}
				} else {
				// 	if (requests.isEmpty()) {
				// 		log("No new pull requests");
				// 	}
				 	break;
				}
				i += 1;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * Searches for new commits made to Github repositories every 15 minutes.
	 * 
	 * @param branch Current branch
	 * @param hash   Commit hash of current branch head
	 * @return  List of new commits
	 */
	/*private void getCommits(String branch, String hash) {
		log("Getting commits from {branch} branch...".replace("{branch}", branch));
		ArrayList<RevCommit> commits = new ArrayList<RevCommit>();
		try {
			this.git.checkout().setName(hash).call();
			Iterator<RevCommit> revCommits = this.git.log().call().iterator();
			while (revCommits.hasNext()) {
				RevCommit commit = revCommits.next();
				long commitTime = (long) commit.getCommitTime() * 1000;
				long now = new Date().getTime();
				if (now - commitTime <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					analyze(commit);
					commits.add(commit);
					String out = results(ObjectId.toString(commit.getId()));
				} else { 
					if (commits.isEmpty()) {
						log("No new commits");
					}
					break;
				 }
			}
			this.git.checkout().setName("refs/heads/master").call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

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
		Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		Recommender toolBot = new Recommender(repo);
		toolBot.getPullRequests(args[2]);
		// try {
		// 	Collection<Ref> refs = git.lsRemote().call();
		// 	for (Ref r: refs) {
		// 		if (r.getName().startsWith("refs/heads/")) {
		// 			String branch = r.getName().split("/")[2];
		// 			String hash = ObjectId.toString(r.getObjectId());
		// 			toolBot.getCommits(branch, hash);
		// 		}
		// 	}
		// } catch (Exception e) {
		// 	e.printStackTrace();
		// }
		Utils.cleanup(args[1]);
	}
}
