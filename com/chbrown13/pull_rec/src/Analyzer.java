package com.chbrown13.pull_rec.src;

import java.io.*;
import com.google.errorprone.*;
import com.sun.tools.javac.main.Main.Result;

public class Analyzer {
	
	public Analyzer() {
		System.out.println("Static analysis w/ Error Prone");
	}
	
	public static void getFile(String url) {
		System.out.println(url);
	}

    public static void main(String[] args) {
        System.out.println("Hello Error Prone");
		String[] file = new String[1];
		file[0] = "./RecommenderTest/src/HelloWorld.java";
		try {
			PrintWriter out = new PrintWriter(args[0]);
			Result res = ErrorProneCompiler.compile(file,out);
			System.out.println("compile "+res);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("error");
		}
    }
}
