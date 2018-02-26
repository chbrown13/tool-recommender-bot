def jenkins(user, repo):
    with open("jenkins.xml") as f:
        config = f.read()
    config = config.replace("{user}", user).replace("{repo}", repo)
    return config

def main():
    i = 0
    with open("projects.txt") as f:
        projects = f.readlines()
    for p in projects:
        git = p.replace("\n","").split("/")
        if git:
            i += 1
            filename = "tool_recommender_bot"+str(i)
            config = jenkins(git[0], git[1]) 
            xml = open(filename+".xml", "w")
            xml.write(config)
            xml.close()
            files = open("configs.txt", "a")
            files.write(filename+"\n")
            files.close()

if __name__ == "__main__":
    main()