import paramiko

def jenkins(user, repo):
    with open("jenkins.xml") as f:
        config = f.read()
    config = config.replace("{user}", user).replace("{repo}", repo)
    return config

def ssh():
    ssh = paramiko.SSHClient()
    ssh.load_system_host_keys()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect("", username="", password="")
    ssh_stdin, stdout, stderr = ssh.exec_command("python projects.py")
    return stdout.readlines()

def main():
    i = 0
    projects = [x for x in ssh() if x != '\n']
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