package com.chbrown13.tool_rec;

import com.jcabi.github.*;
import com.jcabi.http.response.JsonResponse;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.*;


/**
 * Search for projects to use for the tool-recommender-bot evaluation
 */
public class Recommender {

	private static int year = 2008;
	private static int stars1 = 1;
	private static int stars2 = 2;
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

	private static String getStars() {
		String starStr = Integer.toString(stars1) + ".." + Integer.toString(stars2);
		int temp = stars2;
		stars2 += stars1;
		stars1 = temp;
		return starStr;
	}

	public static void main(String[] args) {
		int n = 0;
		final Github github = new RtGithub("chbrown13", "git_down1");
		ArrayList<String> projects = new ArrayList<String>();
		try { 
			while (true) {
				final JsonResponse resp = github.entry()
					.uri().path("/search/repositories")
					.queryParam("q", "language:java stars:{stars}".replace("{stars}", getStars()))
					.queryParam("per_page", 100)
					.queryParam("sort", "forks")
					.queryParam("order", "desc").back()
					.fetch()
					.as(JsonResponse.class);
				JsonArray items = resp.json().readObject().getJsonArray("items");
				if (items.isEmpty()) {
					continue;
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
								try {
									PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("projects.txt", true)));
									out.println(coords);
									out.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}						
						}		
						n += 1;
						System.out.println(n);			
					}	
				}	
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(n);
		System.out.println(projects.size());
	}
}
