package com.chbrown13.pull_rec;

import com.jcabi.github.*;
import com.jcabi.http.response.JsonResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.*;


/**
 * PullRecommender is the main class for this project and handles interactions with Github repositories.
 */
public class PullRecommender {

	private static int year = 2008;

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
		return date;
	}

	public static void main(String[] args) {
		int n = 0;
		int x = 0;
		int month = 2;
		final Github github = new RtGithub("chbrown13", "git_down1");
		try { 
			while (true) {
				final JsonResponse resp = github.entry()
					.uri().path("/search/repositories")
					.queryParam("q", "language:java created:{date}".replace("{date}", getDates(month)))
					.queryParam("sort", "forks")
					.queryParam("order", "desc").back()
					.fetch()
					.as(JsonResponse.class);
				month += 1;
				JsonArray items = resp.json().readObject().getJsonArray("items");
				if (items == null || items.isEmpty()) {
					System.out.println(resp);
					break;
				} else {
					final List<JsonObject> repos = items.getValuesAs(JsonObject.class);
					for (final JsonObject r: repos) {
						n += 1;
						String coords = r.get("full_name").toString().replaceAll("\"", "");
						Repo repo = github.repos().get(new Coordinates.Simple(coords));
						// Map<String, String> params = new HashMap<String, String>();
						// params.put("state", "open");
						// Iterator<Pull> pulls = repo.pulls().iterate(params).iterator();
						// int pr = 0;
						// while (pr < 9 && pulls.hasNext()) {
						// 	pulls.next();
						// 	pr++;
						// }
						// if (!pulls.hasNext()) {
						// 	continue;
						// }
						// Pull.Smart pull10 = new Pull.Smart(pulls.next());
						// if (new Date().getTime() - pull10.createdAt().getTime() <= TimeUnit.MILLISECONDS.convert(180, TimeUnit.DAYS)
						if (repo.contents().exists("pom.xml", "master")) {
							x += 1;							
							try {
								PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("projects.txt", true)));
								out.println(coords);
								coords += "*";
								out.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						System.out.println(coords);				
						if (n % 100 == 0) {
							break;
						}								
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(n);
			System.out.println(x);
		}
		System.out.println(n);
		System.out.println(x);
	}
}