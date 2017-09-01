package com.chbrown13.pull_rec;

import com.jcabi.github.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.JsonObject;

/**
 * PullRecommender is the main class for this project and handles interactions with Github repositories.
 */
public class PullRecommender {

	private Repo repo;
	private String project;
	private List<ErrorProneItem> master;

	public PullRecommender(Repo repo) {
		this.repo = repo;
		this.project = repo.coordinates().repo();
		this.master = analyzeBase();
	}
	
	/**
	 * Post message recommending ErrorProne to Github on pull request fixing error.
	 *
	 * @param comment   Comment with recommendation
	 * @param pull	 	Pull request to comment on
	 * @param error     Error fixed in pull request
	 */
	private void makeRecommendation(String comment, Pull.Smart pull, ErrorProneItem error) {
		try {
			PullComments pullComments = pull.comments();	
			PullComment.Smart smartComment = new PullComment.Smart(pullComments.post(comment, error.getCommit(), error.getFilePath(), error.getLineNumber()));	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Analyze code of files in pull request and compare to master branch.
	 *
	 * @param pull   Current pull request
	 */
	private void analyze(Pull.Smart pull) {
		try {
			Iterator<JsonObject> fileit = pull.files().iterator();
			while (fileit.hasNext()) {
				ArrayList<String> recommended = new ArrayList<String>();
				JsonObject file = fileit.next();
				String commit = file.getString("contents_url").substring(file.getString("contents_url").indexOf("ref=")+4);
				String tempFile = "src.java";
				Utils.wgetFile(file.getString("raw_url"), tempFile);
				String log = ErrorProneItem.analyzeCode(tempFile);
				if(!log.isEmpty()) {
					List<ErrorProneItem> changes = ErrorProneItem.parseErrorProneOutput(log);
					for (ErrorProneItem epi: this.master) {
						if (!changes.contains(epi) && !recommended.contains(epi.getKey())) {
							System.out.println("Fixed: "+epi.getKey());
							epi.setCommit(commit);
							epi.setFilePath(file.getString("filename")); //Remove project directory from start
							String prComment = epi.generateComment();
							makeRecommendation(prComment, pull, epi);
							recommended.add(epi.getKey());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	/**
	 * Searches for new pull requests opened for a Github repository every 15 minutes.
	 * TODO: Remove test condition for pull 23
	 *
	 * @return   List of new pull requests
	 */
	private ArrayList<Pull.Smart> getPullRequests() {
		System.out.println("Getting new pull requests...");
		ArrayList<Pull.Smart> requests = new ArrayList<Pull.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("status","open");
		Iterator<Pull> pullit = this.repo.pulls().iterate(params).iterator();
		while (pullit.hasNext()) {
			Pull.Smart pull = new Pull.Smart(pullit.next());
			try {
				/*if (new Date().getTime() - pull.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)) {
					requests.add(pull);
					System.out.println(pull.url());
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}*/
			if (pull.number() >= 23) {
				requests.add(pull);
				System.out.println(pull.url());
			}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return requests;
	}
	
	/**
	 * Creates ErrorProneItems for master branch of the repository to compare with new pull requests
	 *
	 * @return   List of errors
	 */
	private List<ErrorProneItem> analyzeBase() {
		System.out.println("Analyzing master branch...");
		String log = null;
		try{
			BufferedReader br = new BufferedReader(new FileReader("master.txt"));
			StringBuilder sb = new StringBuilder();
		    String line = null;

		    while ((line = br.readLine()) != null) {
		        sb.append(line+"\n");
			}
	    	br.close();
			log = sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ErrorProneItem.parseErrorProneOutput(log);
	}

	public static void main(String[] args) {
		String[] acct = Utils.getCredentials(".github.creds");
        RtGithub github = new RtGithub(acct[0], acct[1]);
        Repo repo = github.repos().get(new Coordinates.Simple("chbrown13", "RecommenderTest"));
		PullRecommender recommender = new PullRecommender(repo);
		ArrayList<Pull.Smart> requests = recommender.getPullRequests();
		if (requests != null && !requests.isEmpty()) {
			for (Pull.Smart pull: requests) {
				recommender.analyze(pull);
			}
		} else {
			System.out.println("No new pull requests found.");
		}
    }
}

