package com.chbrown13.pull_rec;

import com.jcabi.github.*;
import com.jcabi.http.response.JsonResponse;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.*;


/**
 * PullRecommender is the main class for this project and handles interactions with Github repositories.
 */
public class PullRecommender {

	private static int year = 2016;

	private static String url = "https://raw.githubusercontent.com/{user}/{repo}/master/pom.xml";

	private static boolean wgetErrorProne(String user, String repo) {
		String fileUrl = url.replace("{user}", user).replace("{repo}", repo);
		String s = "";
		try {
			URL url = new URL(fileUrl);
			InputStream in;
			try {
				in = url.openStream();
			} catch (FileNotFoundException e) {
				//File URL does not exist, possibly new file in PR
				in = new ByteArrayInputStream("".getBytes("UTF-8"));
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((s = br.readLine()) != null) {
				if (s.contains("com.google.errorprone")) {
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private static String getDates(int month) {
		String date = "{y1}-{m1}-01..{y2}-{m2}-01";
		int m1 = month % 12;
		int m2 = (month + 1) % 12;
		if (m1 == 0) {
			date = date.replace("{y1}", Integer.toString(year))
				.replace("{y2}", Integer.toString(year+1))
				.replace("{m1}", "12").replace("{m2}", "01");
			year += 1;
		} else if (m2 == 0) {
			date = date.replace("{y1}", Integer.toString(year))
				.replace("{y2}", Integer.toString(year))
				.replace("{m1}", Integer.toString(m1))
				.replace("{m2}", "12");
		} else if (m2 < 10) {
			date = date.replace("{y1}", Integer.toString(year))
				.replace("{y2}", Integer.toString(year))
				.replace("{m1}", "0"+Integer.toString(m1))
				.replace("{m2}", "0"+Integer.toString(m2));
		} else if (m2 == 10) {
			date = date.replace("{y1}", Integer.toString(year))
				.replace("{y2}", Integer.toString(year))
				.replace("{m1}", "09")
				.replace("{m2}", "10");
		}
		else {
			date = date.replace("{y1}", Integer.toString(year))
				.replace("{y2}", Integer.toString(year))
				.replace("{m1}", Integer.toString(m1))
				.replace("{m2}", Integer.toString(m2));
		}
		System.out.println(date);
		return date;
	}

	public static void main(String[] args) {
		int n = 0;
		int x = 0;
		int month = 1;
		int pg = 1;
		final Github github = new RtGithub("chbrown13", "git_down1");
		ArrayList<String> projects = new ArrayList<String>();
		try { 
			while (true) {
				final JsonResponse resp = github.entry()
					.uri().path("/search/repositories")
					.queryParam("q", "language:java created:{date}".replace("{date}", getDates(month)))
					.queryParam("per_page", 100)
					.queryParam("page", pg)
					.queryParam("sort", "forks")
					.queryParam("order", "desc").back()
					.fetch()
					.as(JsonResponse.class);
				JsonArray items = resp.json().readObject().getJsonArray("items");
				System.out.println(pg);
				pg += 1;
				if (items == null || items.isEmpty()) {
					System.out.println(resp);
					break;
				} else {
					final List<JsonObject> repos = items.getValuesAs(JsonObject.class);
					System.out.println(repos.size());
					for (final JsonObject r: repos) {
						String coords = r.get("full_name").toString().replaceAll("\"", "");
						Repo repo = github.repos().get(new Coordinates.Simple(coords));
						if (repo.contents().exists("pom.xml", "master") && wgetErrorProne(repo.coordinates().user(), repo.coordinates().repo())) {
							if(!projects.contains(coords)) {
								System.out.print(coords);
								projects.add(coords);
								x += 1;	
							}						
							try {
								PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("projects.txt", true)));
								out.println(coords);
								out.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}		
						n += 1;
						System.out.println(n);			
					}	
				}
				if (pg % 5 == 0) {
					pg = 1;
					month += 1;
				}		
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(n);
		System.out.println(x);
		System.out.println(projects.size());
	}
}