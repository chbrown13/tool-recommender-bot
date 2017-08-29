package com.chbrown13.pull_rec;

import com.google.errorprone.*;
import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.json.JsonObject;

public class Analyzer {

	public static JsonObject parseErrorProne(String file) {
        /*File f = new File(file);
        if (!f.exists()) {
            System.err.println("file path not specified");
        }
        try {
            //String regex = "^\w+.java\:\d+\:";

            Scanner sc = new Scanner(f);

            while (sc.hasNextLine()) {
                System.out.println("Hello");
                String line = sc.nextLine();
                if (line != null) {
                    //if (matcher.lookingAt(regex)) {
                        System.out.println(line);

                    //}
                }
            }   
			sc.close();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }*/
		String regex = "^[\\w+/]*\\w.java\\:\\d+\\:.*\\:.*";
		String[] lines = file.split("\n");
		for(String line: lines) {
			if (line.matches(regex)) {
				System.out.println(line);
			}
		}
		return null;
	}

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

