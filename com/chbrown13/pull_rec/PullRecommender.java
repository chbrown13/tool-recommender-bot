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
	private int recs;
	private Set<String> prs;

	public PullRecommender(Repo repo) {
		this.repo = repo;
		Utils.setProjectName(repo.coordinates().repo());
		Utils.setProjectOwner(repo.coordinates().user());
		prs = new HashSet<String>();
	}

	/**
	 * Gets the number of recommendations made
	 *
	 * @return   Count of recommendations
     */
	public int getRecommendationCount() {
		return this.recs;
	}

	public int getPRCount() {
		return this.prs.size();
	}
	
	/**
	 * Post message recommending ErrorProne to Github on pull request fixing error.
	 *
	 * @param comment   Comment with recommendation
	 * @param pull	 	Pull request to comment on
	 * @param error     Error fixed in pull request
	 */
	private void makeRecommendation(Pull.Smart pull, ErrorProneItem epi, String hash, int line, List<ErrorProneItem> errors) {
		String comment = epi.generateComment(errors, hash);
		System.out.println(comment);
		recs += 1;
		prs.add(epi.getKey());
	}

	/**
	 * Analyze code of files in pull request and compare to master branch.
	 *
	 * @param pull   Current pull request
	 * @return       Number of recommendations made
	 */
	private void analyze(Pull.Smart pull) {
		Map<String, String> errors = new HashMap<String, String>();
		System.out.println("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		try {
			String pullHash = pull.json().getJsonObject("head").getString("sha");
			String baseHash = pull.json().getJsonObject("base").getString("sha");
			Map<String, String> baseErrors = Utils.checkout(baseHash);
			Map<String, String> pullErrors = Utils.checkout(pullHash);
			List<ErrorProneItem> allErrors = new ArrayList<ErrorProneItem>();
			List<ErrorProneItem> fixed = new ArrayList<ErrorProneItem>();
			for (String file: baseErrors.keySet()) {
				allErrors.addAll(ErrorProneItem.parseOutput(baseErrors.get(file)));
				if (!pullErrors.containsKey(file)) {
					//Deleted file
					continue;
				} else if (baseErrors.get(file).equals(pullErrors.get(file))) {
					//No bugs fixed
					continue;
				} else {
					Set<ErrorProneItem> baseEP = ErrorProneItem.parseOutput(baseErrors.get(file));
					Set<ErrorProneItem> pullEP = ErrorProneItem.parseOutput(pullErrors.get(file));
					for (ErrorProneItem e: baseEP) {
						if (!pullEP.contains(e) && !fixed.contains(e)) {
							fixed.add(e);
						}
					}
				}
			}
			for (ErrorProneItem error: fixed) {
				if (Utils.isFix(baseHash, pullHash, error)) {
					makeRecommendation(pull, error, pullHash, Utils.getFix(), allErrors);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	/**
	 * Searches for new pull requests opened for a Github repository every 15 minutes.
	 *
	 * @return   List of new pull requests
	 */
	private ArrayList<Pull.Smart> getPullRequests(int num) {
		System.out.println("Getting new pull requests...");
		ArrayList<Pull.Smart> requests = new ArrayList<Pull.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", "all");
		Iterator<Pull> pullit = this.repo.pulls().iterate(params).iterator();
		int i = 0;
		while (pullit.hasNext()) {
			if (i >= num) {
				break;
			}
			Pull.Smart pull = new Pull.Smart(pullit.next());
			requests.add(pull);
			i++;
		}
		return requests;
	}

	public static void main(String[] args) {
		String[] acct = Utils.getCredentials(".github.creds");
        RtGithub github = new RtGithub(acct[0], acct[1]);
        Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		PullRecommender recommender = new PullRecommender(repo);
		ArrayList<Pull.Smart> requests = recommender.getPullRequests(Integer.parseInt(args[2]));
		if (requests != null && !requests.isEmpty()) {
			for (Pull.Smart pull: requests) {
				recommender.analyze(pull);
			}
		}
		System.out.println("{num} recommendations made for {prs} prs.".replace("{num}", Integer.toString(recommender.getRecommendationCount())).replace("{prs}", Integer.toString(recommender.getPRCount())));
	}
}


