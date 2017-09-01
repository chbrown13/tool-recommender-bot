package com.chbrown13.pull_rec;

import java.io.*;
import java.util.*;
import com.jcabi.github.*;
import java.util.concurrent.TimeUnit;
import javax.json.JsonObject;
			
public class PullRecommender {

	private Repo repo;
	private String project;
	private List<ErrorProneItem> master;

	public PullRecommender(Repo repo) {
		this.repo = repo;
		this.project = repo.coordinates().repo();
		this.master = analyzeBase();
	}
	
	private void makeRecommendation(String comment, Pull.Smart pull, ErrorProneItem error, JsonObject file) {
		try {
			PullComments pullComments = pull.comments();	
			PullComment.Smart smartComment = new PullComment.Smart(pullComments.post(comment, error.getCommit(), file.getString("filename"), error.getLineNumber()));	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void analyze(Pull.Smart pull) {
		try {
			ArrayList<String> fixed = new ArrayList<String>();
			Iterator<JsonObject> fileit = pull.files().iterator();
			while (fileit.hasNext()) {
				JsonObject file = fileit.next();
				String commit = file.getString("contents_url").substring(file.getString("contents_url").indexOf("ref=")+4);
				String tempFile = "src.java";
				Utils.wgetFile(file.getString("raw_url"), tempFile);
				String log = ErrorProneItem.analyzeCode(tempFile);
				if(!log.isEmpty()) {
					List<ErrorProneItem> changes = ErrorProneItem.parseErrorProneOutput(log);
					for (ErrorProneItem epi: this.master) {
						if (!changes.contains(epi) && !fixed.contains(epi.getKey())) {
							epi.setCommit(commit);
							System.out.println("Fixed: "+epi.getKey());
							fixed.add(epi.getKey());
							String prComment = epi.generateComment();
							makeRecommendation(prComment, pull, epi, file);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

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
		if (!requests.isEmpty()) {
			for (Pull.Smart pull: requests) {
				recommender.analyze(pull);
			}
		} else {
			System.out.println("No new pull requests found.");
		}
    }
}

