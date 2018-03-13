package com.chbrown13.tool_rec;

import com.jcabi.github.*;
import com.jcabi.http.response.*;
import com.jcabi.http.Request;
import java.io.*;
import java.net.HttpURLConnection;
import java.text.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.*;
import org.apache.commons.mail.*;


/**
 * Recommender is the main class for this project and handles interactions with Github repositories.
 */
public class Recommender {

	private Repo repo;
	private Set<String> fixes = new HashSet<String>();	
	private int recs = 0;
	private int rem = 0;
	private int fix = 0;
	private int intro = 0;
	private String removed = "";
	private String introduced = "";
	private String noSimilar = "";
	private int baseErrorCount = 0;
	private int newErrorCount = 0;
	private Tool tool = null;
	private Object change = null;
	private static String type = "";

	public static final String PULL = "pull";
	public static final String COMMIT = "commit";

	public Recommender(Repo repo) {
		this.repo = repo;
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
		newErrorCount = 0;
		intro = 0;
		recs = 0;
		rem = 0;
		fix = 0;
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
			System.out.println("Email sent for review: " + subject);	
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
			noSimilar += error.getLog() + "\n";
		}
	}

	/**
	 * Checks if the change is actually a fix or not
	 */
	private boolean checkFix(List<Error> baseErrors, List<Error> changeErrors, List<String> files, String base, String head, String id) {
		boolean fix = false;
		if(baseErrors != null && changeErrors != null) {
			List<Error> fixed = new ArrayList<Error>();	
			List<Error> added = new ArrayList<Error>();
			System.out.println("sizes");
			System.out.println(baseErrors.size());
			System.out.println(changeErrors.size());
			for (Error e: baseErrors) {
				if (files.contains(e.getLocalFilePath())) {
					baseErrorCount += 1;
					if ((!changeErrors.contains(e) || Collections.frequency(baseErrors, e) >= Collections.frequency(changeErrors, e)) && !fixed.contains(e)) {
						fixed.add(e);
					}
				}
			}
			for (Error e: changeErrors) {
				if (files.contains(e.getLocalFilePath())) {
					newErrorCount += 1;
					if ((!baseErrors.contains(e) || Collections.frequency(baseErrors, e) <= Collections.frequency(changeErrors, e)) && !added.contains(e)) {
						added.add(e);
						intro += 1;
						introduced += "-" + e.getKey() + "\n";
					}
				}
			}
			for (Error e: fixed) {
				System.out.println(e.getFilePath());
				if (Utils.isFix(e)) {
					fix = true;
					int line = Utils.getFix(id, e);			
					System.out.println("Fixed "+ e.getKey() + " reported at line " + 
					e.getLineNumberStr() + " possibly fixed at line " + Integer.toString(line));
					makeRecommendation(tool, id, e, line, changeErrors, base, head);
				} else {
					rem += 1;
					removed += "-" + e.getKey() + "\n";
				}
			}	
		}
		return fix;
	}

	/**
	 * Analyze code of files in pull request and compare to master branch.
	 *
	 * @param pull   Current pull request
	 */
	private void analyze(Pull.Smart pull) {
		System.out.println("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		this.type = PULL;
		this.change = pull;
		tool = new ErrorProne();		
		List<String> javaFiles = new ArrayList<String>();
		Iterator<JsonObject> files = null;
		try {
			files = pull.files().iterator();			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		while (files.hasNext()) {
			JsonObject f = files.next();
			if (f.getString("filename").endsWith(".java") && f.getString("status").equals("modified")) {
				javaFiles.add(f.getString("filename"));
			}		
		}
		if (javaFiles.size() == 0) {
			System.out.println("No java changes.");
			return;
		}
		try {
			JsonObject head = pull.json().getJsonObject("head");
			String hash = pull.json().getJsonObject("base").getString("sha");
			String newHash = head.getString("sha");
			String owner = "";
			String repo = "";
			try {
				owner = head.getJsonObject("repo").getString("full_name").split("/")[0];
				repo = head.getJsonObject("repo").getString("full_name").split("/")[1];
			} catch (NullPointerException|ClassCastException exc) { //unknown repository
				owner = head.getJsonObject("user").getString("login");
				repo = Utils.getProjectName();
			}
			List<Error> baseErrors = Utils.checkout(hash, tool, true, PULL);
			List<Error> changeErrors = Utils.checkout(newHash, owner, repo, tool, false, PULL);
			checkFix(baseErrors, changeErrors, javaFiles, hash, newHash, Integer.toString(pull.number()));
			Utils.cleanup();
		} catch (IOException e) {
			e.printStackTrace();
			Utils.cleanup();
		}	
	}

	/**
	 * Analyze code of files in commits and compare to master branch.
	 *
	 * @param commit   Current commit
	 */
	private void analyze (RepoCommit.Smart commit) {
		System.out.println("Analyzing commit #" + commit.sha() + "...");
		this.type = COMMIT;
		this.change = commit;
		tool = new ErrorProne();		
		List<String> javaFiles = new ArrayList<String>();
		try {
			JsonArray files = commit.json().getJsonArray("files");
			for (int i = 0; i < files.size(); i++) {
				String filename = files.getJsonObject(i).getString("filename");
				if (filename.endsWith(".java")) {
					javaFiles.add(filename);
				}
			}
			if (javaFiles.size() == 0) {
				System.out.println("No java changes.");
				return;
			}
			String hash = commit.json().getJsonArray("parents").getJsonObject(0).getString("sha");
			String newHash = commit.json().getString("sha");
			List<Error> baseErrors = Utils.checkout(hash, tool, true, COMMIT);
			List<Error> changeErrors = Utils.checkout(newHash, tool, false, COMMIT);
			checkFix(baseErrors, changeErrors, javaFiles, hash, newHash, newHash);
			Utils.cleanup();
		} catch (IOException e) {
			e.printStackTrace();
			Utils.cleanup();
		}
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
		out += "\n" + Integer.toString(baseErrorCount) + "------" + Integer.toString(newErrorCount); 
		sendEmail(out, "New " + type, id);
		System.out.println(out);
		reset();
		return out;
	}

	/**
	 * Searches for new pull requests opened for a Github repository every 15 minutes.
	 *
	 * @return   List of new pull requests
	 */
	private ArrayList<Pull.Smart> getPullRequests() {
		System.out.println("Getting new pull requests...");
		ArrayList<Pull.Smart> requests = new ArrayList<Pull.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", "all");
		Iterator<Pull> pullit = this.repo.pulls().iterate(params).iterator();
		while (pullit.hasNext()) {
			Pull.Smart pull = new Pull.Smart(pullit.next());
			try {
				if (new Date().getTime() - pull.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					analyze(pull);					
					requests.add(pull);
					String out = results(Integer.toString(pull.number()));
				} else {
					if (requests.isEmpty()) {
						System.out.println("No new pull requests");
					}
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return requests;
	}

	/**
	 * Searches for new commits made to Github repositories every 15 minutes.
	 *
	 * @return  List of new commits
	 */
	private ArrayList<RepoCommit.Smart> getCommits() {
		System.out.println("Getting new commits...");
		ArrayList<RepoCommit.Smart> commits = new ArrayList<RepoCommit.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", "all");
		Iterator<RepoCommit> it = this.repo.commits().iterate(params).iterator();
		int i = 0;
		while (it.hasNext()) {
			RepoCommit.Smart commit = new RepoCommit.Smart(it.next());
			try {
				String date = commit.json().getJsonObject("commit").getJsonObject("author").getString("date");
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				Date created = format.parse(date);
				if (new Date().getTime() - created.getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					analyze(commit);					
					commits.add(commit);
					String out = results(commit.sha());
					i += 1;
				} else {
					if (commits.isEmpty()) {
						System.out.println("No new commits");
					}
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return commits;
	}

	public static void main(String[] args) {
		String[] gitAcct = Utils.getCredentials(".github.creds");
		RtGithub github = new RtGithub(gitAcct[0], gitAcct[1]);
		Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		Recommender toolBot = new Recommender(repo);
		toolBot.getCommits();
		toolBot.getPullRequests();
	}
}
