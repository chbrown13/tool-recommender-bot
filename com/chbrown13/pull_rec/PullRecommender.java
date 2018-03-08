package com.chbrown13.pull_rec;

import com.jcabi.github.*;
import com.jcabi.http.response.JsonResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.*;
import org.apache.commons.mail.*;


/**
 * PullRecommender is the main class for this project and handles interactions with Github repositories.
 */
public class PullRecommender {

	private Repo repo;
	private Set<Integer> prs = new HashSet<Integer>();	
	private int recs = 0;;
	private int rem = 0;
	private int fix = 0;
	private int noSim = 0;
	private int intro = 0;
	private String removed = "";
	private String noSimilar = "";
	private String introduced = "";
	private int baseErrorCount = 0;
	private int commErrorCount = 0;

	public PullRecommender(Repo repo) {
		this.repo = repo;
		Utils.setProjectName(repo.coordinates().repo());
		Utils.setProjectOwner(repo.coordinates().user());
	}

	/**
	 * Sends email to researchers for review
	 * 
	 * @param text    Contents of email message
	 * @param subject Subject of the email
	 * @param pr	  Pull request number
	 */
	private static void sendEmail(String text, String subject, String hash) {
		SimpleEmail email = new SimpleEmail();
		String viewChanges = Comment.changes.replace("{user}", Utils.getProjectOwner())
			.replace("{repo}", Utils.getProjectName())
			.replace("{num}", hash);
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
	 * Post message recommending ErrorProne to Github on pull request fixing error.
	 *
	 * @param comment   Comment with recommendation
	 * @param pull	 	Pull request to comment on
	 * @param error     Error fixed in pull request
	 */
	private void makeRecommendation(Tool tool, Pull.Smart pull, Error error, int line, List<Error> errors) {
		try {
			String base = pull.json().getJsonObject("base").getString("sha");
			String head = pull.json().getJsonObject("head").getString("sha");			
			String comment = error.generateComment(tool, errors, base);
			if (comment != null) {
				String link = "\n\n" + Utils.SURVEY.replace("{project}", Utils.getProjectName()).replace("{pull}", Integer.toString(pull.number()));
				comment += link;
				String args = String.join(" ", Utils.getProjectOwner(), Utils.getProjectName(), 
					Integer.toString(pull.number()), "\""+comment+"\"", head, error.getLocalFilePath(), 
					Integer.toString(line)
				);

				String run = Comment.cmd.replace("{args}", args);
				//sendEmail(String.join("\n", Comment.compile, run), "Recommendation Review", pull.number());
				recs += 1;
				prs.add(pull.number());
			} else {
				noSim += 1;
				noSimilar += error.getLog() + "\n";
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Checks if the change is actually a fix or not
	 */
	private boolean isFix(List<Error> base, List<Error> pull, Error error, List<String> files) {
		boolean fileCheck = false;
		for (String f: files) {
			if (error.getFilePath().contains(f)) {
				fileCheck = true;
				break;
			}
		}
		if (base.size() > 0 && fileCheck) {
			return Utils.isFix(error);
		}
		return false;
	}

	/**
	 * Analyze code of files in commit and compare to master branch.
	 *
	 * @param commit   Current commit
	 * @return       Number of recommendations made
	 */
	private void analyze(RepoCommit.Smart commit) {
		System.out.println("Analyzing " + commit.sha() + "...");
		removed = "";
		Tool tool = new ErrorProne();		
		String developer = "";
		boolean java = false;
		ArrayList<String> javaFiles = new ArrayList<String>();
		try {
			JsonArray files = commit.json().getJsonArray("files");
			System.out.println(files.size());
			for (int i = 0; i < files.size(); i++) {
				if (files.getJsonObject(i).getString("filename").endsWith(".java")) {
					java = true;
					javaFiles.add(files.getJsonObject(i).getString("filename"));
				}
			}           
			if (!java) {
				System.out.println("No java changes.");
				return;
			} else {
				List<Error> baseErrors = Utils.checkout(commit, tool, true);
				List<Error> commErrors = Utils.checkout(commit, tool, false);
				if(baseErrors != null && commErrors != null) {
					System.out.println(Integer.toString(baseErrors.size()) +"------"+Integer.toString(commErrors.size()));				
					baseErrorCount = baseErrors.size();
					commErrorCount = commErrors.size();
					List<Error> fixed = new ArrayList<Error>();	
					List<Error> added = new ArrayList<Error>();
					for (Error e: baseErrors) {
						System.out.println(e.getKey());
						if (!commErrors.contains(e)) {
							fixed.add(e);
						}
					}
					for (Error e: commErrors) {
						System.out.println(e.getKey());
						if (!baseErrors.contains(e)) {
							added.add(e);
							intro += 1;
							introduced += "-" + e.getError() + "\n";
						}
					}
					for (Error e: fixed) {
						if (isFix(baseErrors, commErrors, e, javaFiles)) {
							fix += 1;			
							int line = 0;//Utils.getFix(commit, e);			
							System.out.println("Fixed "+ e.getKey() + " in commit " + commit.sha()
								+ " reported at line " + e.getLineNumberStr() + " possibly fixed at line " + Integer.toString(line));
							//makeRecommendation(tool, commit, e, line, baseErrors);
						} else {
							rem += 1;
							removed += "-" + e.getError() + "\n";
						}
					}
				}
				Utils.cleanup();
			} 
		} catch (IOException e) {
				e.printStackTrace();
				//Utils.cleanup();
		}	
	}

	/**
	 * Searches for new commits opened for a Github repository every 15 minutes.
	 *
	 * @return   List of new pull requests
	 */
	private ArrayList<RepoCommit.Smart> getChanges() {
		System.out.println("Getting new commits...");
		ArrayList<RepoCommit.Smart> commits = new ArrayList<RepoCommit.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		//params.put("state", "all");
		Iterator<RepoCommit> it = this.repo.commits().iterate(params).iterator();
		int i = 0;
		while (it.hasNext()) {
			RepoCommit.Smart commit = new RepoCommit.Smart(it.next());
			//try {
				if (i < 10) { //new Date().getTime() - pull.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					analyze(commit);					
					commits.add(commit);
					String out = "Recommendations: {rec}\nFixes: {fix}\nRemoved: {rem}\n{err}\nFixed but so similar: {sim}\n{simErr}\nIntroduced: {intro}\n {new}"
						.replace("{rec}", Integer.toString(recs))
						.replace("{fix}", Integer.toString(fix - recs))
						.replace("{rem}", Integer.toString(rem))
						.replace("{err}", removed)
						.replace("{sim}", Integer.toString(noSim))
						.replace("{simErr}", noSimilar)
						.replace("{intro}", Integer.toString(intro))
						.replace("{new}", introduced);
					out += "\n" + Integer.toString(baseErrorCount) + "------" + Integer.toString(commErrorCount); 
					// sendEmail(out, "New Commit", commit.sha());
					System.out.println(out);
					break;						
				} else {
					if (commits.isEmpty()) {
						System.out.println("No new commits");
					}
					break;
				}
			/*} catch (IOException e) {
				e.printStackTrace();
				return null;
			}*/
		}
		return commits;
	}

	public static void main(String[] args) {
		String[] gitAcct = Utils.getCredentials(".github.creds");
		RtGithub github = new RtGithub(gitAcct[0], gitAcct[1]);
		Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		PullRecommender recommender = new PullRecommender(repo);
		//recommender.getChanges();
		try {
			final JsonResponse jr = github.entry().uri().path("repos/chbrown13/RecommenderTest/commits/4c2c3ec0a00f92083533cf72eccc90ca742a3939/comments")
				.queryParam("body", "Java Test")
				.queryParam("path", "com/chbrown13/pull_rec/PullRecommender")
				.queryParam("position", 1)
				.back().method("POST").fetch().as(JsonResponse.class);
			System.out.println(jr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
