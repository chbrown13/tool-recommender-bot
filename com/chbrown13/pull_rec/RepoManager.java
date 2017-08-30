package com.chbrown13.pull_rec;

import java.io.*;
import java.util.*;
import com.jcabi.github.*;
import java.util.concurrent.TimeUnit;
import javax.json.JsonObject;
			
public class RepoManager {

	private Repo repo;
	private String project;
	private List<Analyzer> master;

	public RepoManager(Repo repo, String name) {
		this.repo = repo;
		this.project = name;
		this.master = analyzeBase();
	}
	
	private void analyze(Pull.Smart pull) {
		try {
			ArrayList<String> fixed = new ArrayList<String>();
			Iterator<JsonObject> fileit = pull.files().iterator();
			while (fileit.hasNext()) {
				JsonObject file = fileit.next();
				String srcFile = "src.java";
				Utils.wgetFile(file.getString("raw_url"), srcFile);
				String log = Analyzer.errorProne(srcFile);
				if(!log.isEmpty()) {
					List<Analyzer> fileChange = Analyzer.parseErrorProne(log);
					for (Analyzer err: this.master) {
						if (!fileChange.contains(err) && !fixed.contains(err.getKey())) {
							System.out.println("Fixed: "+err.getKey());
							fixed.add(err.getKey());
							String prComment = err.generateComment();
							System.out.println(prComment);
							//comment on PR
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<Pull.Smart> getRequests() {
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
	
	private List<Analyzer> analyzeBase() {
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
		
		return Analyzer.parseErrorProne(log);
	}

	public static void main(String[] args) {
		String[] acct = Utils.getCredentials(".github.creds");
        RtGithub github = new RtGithub(acct[0], acct[1]);
        Repo repo = github.repos().get(new Coordinates.Simple("chbrown13", "RecommenderTest"));
		RepoManager manager = new RepoManager(repo, "RecommenderTest");
		ArrayList<Pull.Smart> requests = manager.getRequests();
		for (Pull.Smart pull: requests) {
			manager.analyze(pull);
		}
    }
}

