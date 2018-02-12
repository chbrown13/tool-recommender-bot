package com.chbrown13.pull_rec;

import com.jcabi.github.*;
import com.jcabi.http.response.JsonResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.JsonObject;
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
	private String removed = "";

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
	private static void sendEmail(String text, String subject, int pr) {
		SimpleEmail email = new SimpleEmail();
		String viewChanges = Comment.changes.replace("{user}", Utils.getProjectOwner())
			.replace("{repo}", Utils.getProjectName())
			.replace("{num}", Integer.toString(pr));
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
	private void makeRecommendation(Tool tool, Pull.Smart pull, Error error, int line, Set<Error> errors) {
		try {
			String base = pull.json().getJsonObject("base").getString("sha");
			String head = pull.json().getJsonObject("head").getString("sha");			
			String comment = error.generateComment(tool, errors, base);
			String link = "\n\n" + Utils.SURVEY.replace("{project}", Utils.getProjectName()).replace("{pull}", Integer.toString(pull.number()));
			comment += link;
			String args = String.join(" ", Utils.getProjectOwner(), Utils.getProjectName(), 
				Integer.toString(pull.number()), "\""+comment+"\"", head, error.getLocalFilePath(), 
				Integer.toString(line)
			);

			String run = Comment.cmd.replace("{args}", args);
			sendEmail(String.join("\n", Comment.compile, run), "Recommendation Review", pull.number());
			recs += 1;
			prs.add(pull.number());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Checks if the change is actually a fix or not
	 */
	private boolean isFix(Set<Error> base, Set<Error> pull, Error error, List<String> files) {
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
	 * Analyze code of files in pull request and compare to master branch.
	 *
	 * @param pull   Current pull request
	 * @return       Number of recommendations made
	 */
	private void analyze(Pull.Smart pull) {
		System.out.println("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		removed = "";
		Tool tool = new ErrorProne();		
		String developer = "";
		boolean pullRec = false;
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
				pullRec = true;
			}		
		}
		if (!pullRec) {
			System.out.println("No java changes.");
			return;
		}
		try {
			Set<Error> baseErrors = Utils.checkout(pull, tool, true);
			Set<Error> pullErrors = Utils.checkout(pull, tool, false);
			if(baseErrors != null && pullErrors != null) {
				System.out.println(Integer.toString(baseErrors.size()) +"------"+Integer.toString(pullErrors.size()));				
				Set<Error> fixed = new HashSet<Error>();				
				fixed.addAll(baseErrors);				
				fixed.removeAll(pullErrors);
				int i = 0;
				for (Error e: fixed) {
					if (isFix(baseErrors, pullErrors, e, javaFiles)) {
						fix += 1;			
						int line = Utils.getFix(pull, e);			
						System.out.println("Fixed "+ e.getKey() +" in PR #"+Integer.toString(pull.number())
							+ " reported at line " + e.getLineNumberStr() + " possibly fixed at line " + Integer.toString(line));
						
						makeRecommendation(tool, pull, e, line, baseErrors);
					} else {
						rem += 1;
						removed += e.getError() + "\n";
					}
				}	
			}
			Utils.cleanup();
		} catch (IOException e) {
			e.printStackTrace();
			Utils.cleanup();
		}	
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
		params.put("state", "open");
		Iterator<Pull> pullit = this.repo.pulls().iterate(params).iterator();
		int i = 0;
		while (pullit.hasNext()) {
			Pull.Smart pull = new Pull.Smart(pullit.next());
			try {
				if (new Date().getTime() - pull.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					analyze(pull);					
					requests.add(pull);
					String out = "Recommendations: {rec}\nFixes: {fix}\nRemoved: {rem}\n{err}"
						.replace("{rec}", Integer.toString(recs))
						.replace("{fix}", Integer.toString(fix - recs))
						.replace("{rem}", Integer.toString(rem))
						.replace("{err}", removed);
					sendEmail(out, "New Pull Request", pull.number());						
				} else {
					System.out.println("No new pull requests");
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return requests;
	}

	public static void main(String[] args) {
		String[] gitAcct = Utils.getCredentials(".github.creds");
		RtGithub github = new RtGithub(gitAcct[0], gitAcct[1]);
		Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		PullRecommender recommender = new PullRecommender(repo);
		ArrayList<Pull.Smart> requests = recommender.getPullRequests();
	}
}
