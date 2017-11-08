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
	private static Set<Integer> prs = new HashSet<Integer>();	
	private static int recs = 0;;
	private static int open = 0;
	private static int pulls = 0;
	private static int removed = 0;

	public PullRecommender(Repo repo) {
		this.repo = repo;
		Utils.setProjectName(repo.coordinates().repo());
		Utils.setProjectOwner(repo.coordinates().user());
	}

	/**
	 * Post message recommending tool to Github on pull request fixing error.
	 *
	 * @param comment   Comment with recommendation
	 * @param pull	 	Pull request to comment on
	 * @param error     Error fixed in pull request
	 */
	private void makeRecommendation(Tool tool, Pull.Smart pull, Error error, String hash, int line, Set<Error> errors) {
		String comment = error.generateComment(tool, errors, hash);
		System.out.println(comment);
		recs += 1;
		prs.add(pull.number());
	}

	/**
	 * Checks if the change is actually a fix or not
	 */
	private boolean isFix(Set<Error> base, Set<Error> pull, Error error) {
		if (base.size() == 0 || pull.size() == 0) {
			return false;
		}
		return Utils.isFix(error);
	}
	/**
	 * Analyze code of files in pull request and compare to master branch.
	 *
	 * @param pull   Current pull request
	 * @return       Number of recommendations made
	 */
	private void analyze(Pull.Smart pull) {
		Tool tool = new ErrorProne();
		System.out.println("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		try {
			String author = pull.json().getJsonObject("head").getJsonObject("user").getString("login");
			String pullHash = pull.json().getJsonObject("head").getString("sha");
			String baseHash = pull.json().getJsonObject("base").getString("sha");
			Set<Error> baseErrors = Utils.checkout(baseHash, author, tool, true);
			Set<Error> pullErrors = Utils.checkout(pullHash, author, tool, false);
			if(baseErrors != null && pullErrors != null) {
				Set<Error> fixed = new HashSet<Error>();				
				fixed.addAll(baseErrors);				
				fixed.removeAll(pullErrors);
				int i = 0;
				System.out.println(baseErrors.size());
				System.out.println(pullErrors.size());
				System.out.println(fixed.size());
				for (Error e: fixed) {
					if (isFix(baseErrors, pullErrors, e)) {
						System.out.println("Fixed "+ e.getKey() +" in PR #"+Integer.toString(pull.number())+" "+pull.title());
						makeRecommendation(tool, pull, e, pullHash, Utils.getFix(), baseErrors);
					} else {
						removed += 1;
					}
				}	
			}
			Utils.cleanup();
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
			try{
				if (pull.state().equals("open")) {
					open += 1;
				}
			} catch (Exception e) {
				//nada
			}
			requests.add(pull);
			i++;
			pulls++;
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
		System.out.println("{num} recommendations made on {prs} pull request(s), {open} of which were open out of {pulls} total, while {rem} bug(s) fixed were not recommended because the error was just removed."
			.replace("{num}", Integer.toString(recs))
			.replace("{prs}", Integer.toString(prs.size()))
			.replace("{open}", Integer.toString(open))
			.replace("{pulls}", Integer.toString(pulls))
			.replace("{rem}", Integer.toString(removed)));
		for (Integer i: prs) {
			System.out.println(i);
		}
	}
}


