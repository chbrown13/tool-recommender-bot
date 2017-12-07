package com.chbrown13.pull_rec;

import com.jcabi.github.*;
import com.jcabi.http.response.JsonResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.JsonObject;
// import javax.mail.*;
// import javax.mail.internet.*;
// import javax.activation.*;
// import javax.mail.Session;
// import javax.mail.Transport;

/**
 * PullRecommender is the main class for this project and handles interactions with Github repositories.
 */
public class PullRecommender {

	private Repo repo;
	private static Set<Integer> prs = new HashSet<Integer>();	
	private static int recs = 0;;
	private static int open = 0;
	private static int removed = 0;
	private static int fixes = 0;

	public PullRecommender(Repo repo) {
		this.repo = repo;
		Utils.setProjectName(repo.coordinates().repo());
		Utils.setProjectOwner(repo.coordinates().user());
	}

	/*private void sendEmail() {
				// Recipient's email ID needs to be mentioned.
				String to = "dcbrown06@gmail.com";
		
			  // Sender's email ID needs to be mentioned
			  String from = "toolrecommenderbot@gmail.com";
		
			  // Assuming you are sending email from localhost
			  String host = "localhost";
		
			  // Get system properties
			  Properties properties = System.getProperties();
		
			  // Setup mail server
			  properties.setProperty("mail.smtp.host", host);
		
			  // Get the default Session object.
			  Session session = Session.getDefaultInstance(properties);
		
			  try {
				 // Create a default MimeMessage object.
				 MimeMessage message = new MimeMessage(session);
		
				 // Set From: header field of the header.
				 message.setFrom(new InternetAddress(from));
		
				 // Set To: header field of the header.
				 message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		
				 // Set Subject: header field
				 message.setSubject("This is the Subject Line!");
		
				 // Now set the actual message
				 message.setText("This is actual message");
		
				 // Send message
				 Transport.send(message);
				 System.out.println("Sent message successfully....");
			  } catch (MessagingException mex) {
				 mex.printStackTrace();
			  }
	}*/
	
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
			System.out.println(String.join(" ", Utils.getProjectOwner(), Utils.getProjectName(), 
				Integer.toString(pull.number()), "\""+comment+"\"", head, error.getLocalFilePath(), 
				Integer.toString(line))
			);
			//PullComments pullComments = pull.comments();
			//PullComment.Smart smartComment = new PullComment.Smart(pullComments.post(comment, sha, error.getLocalFilePath(), line));	
			recs += 1;
		} catch (IOException e) {
			e.printStackTrace();
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
					fixes += 1;
					if (isFix(baseErrors, pullErrors, e, javaFiles)) {
						System.out.println("Fixed "+ e.getKey() +" in PR #"+Integer.toString(pull.number())
							+ " reported at line " + e.getLineNumberStr() + " possibly fixed at line " + Integer.toString(Utils.getFix()));
						makeRecommendation(tool, pull, e, Utils.getFix(), baseErrors);
					} else {
						removed += 1;
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
		String[] acct = Utils.getCredentials(".github.creds");
		RtGithub github = new RtGithub(acct[0], acct[1]);
        Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		PullRecommender recommender = new PullRecommender(repo);
		ArrayList<Pull.Smart> requests = recommender.getPullRequests();
		System.out.println("{recs} recommendations made on {pulls} pull request(s) out of {totals} total. {fix} were just fixed."
			.replace("{recs}", Integer.toString(recs))
			.replace("{pulls}", Integer.toString(prs.size()))
			.replace("{totals}", Integer.toString(requests.size()))
			.replace("{fix}", Integer.toString(fixes - recs)));
		for (Integer i: prs) {
			System.out.println(i);
		}	
	}
}