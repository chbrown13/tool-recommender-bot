package com.chbrown13.pull_rec;

import java.util.*;

public abstract class Tool {

    private final String MAVEN = "";
    private String name;
    private String desc;
    private String link;

    public Tool (String name, String desc, String link) {
        this.name = name;
        this.desc = desc;
        this.link = link;
    }

    /**
	 * Returns the name of the current tool.
	 */
    public String getName() {
        return this.name;
    }

    /**
	 * Returns the description of the current tool.
	 */
    public String getDescription() {
        return this.desc;
    }

    /**
	 * Returns the URL for the current tool.
	 */
    public String getLink() {
        return this.link;
    }

    public abstract String getPlugin();
    
    public abstract Set<Error> parseOutput(String log);
}