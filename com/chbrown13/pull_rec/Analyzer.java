package com.chbrown13.pull_rec;

import java.io.*;
import java.net.*;
import com.google.errorprone.*;
import com.sun.tools.javac.main.Main.Result;

public class Analyzer {

	public static void wgetFile(String fileUrl, String output) {
		String s;

		try {
			URL url = new URL(fileUrl);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			BufferedWriter out = new BufferedWriter(new FileWriter(output));
			while ((s = br.readLine()) != null) {
				out.write(s);
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void errorProne(String file, String output) {
		System.out.println("error-prone");
	}
}

