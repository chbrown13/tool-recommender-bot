# Update
sudo yum -y install epel-release
sudo yum -y update

# Install and Start Jenkins
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
sudo yum -y install jenkins
sudo systemctl start jenkins.service
sudo systemctl enable jenkins.service
sudo firewall-cmd --zone=public --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# Install Maven
wget http://mirror.olnevhost.net/pub/apache/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz
tar xvf apache-maven-3.5.2-bin.tar.gz
sudo mv apache-maven-3.5.2  /usr/local/apache-maven

# Install Ansible
sudo yum -y install ansible
echo "localhost ansible_connection=local" > hosts
sudo mv hosts /etc/ansible/hosts

# Download files and dependencies
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/eval_scripts/start-bot.yml
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/eval_scripts/stop-bot.yml
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/jenkins.xml
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/eval_scripts/eval.py
wget -q https://repo1.maven.org/maven2/com/google/errorprone/error_prone_ant/2.3.1/error_prone_ant-2.3.1.jar
wget -q https://github.com/GumTreeDiff/gumtree/releases/download/2.0.0/gumtree.jar
wget -q https://repo.eclipse.org/content/groups/releases//org/eclipse/jgit/org.eclipse.jgit/4.9.0.201710071750-r/org.eclipse.jgit-4.9.0.201710071750-r.jar
wget -q http://repo1.maven.org/maven2/com/jcabi/jcabi-github/0.38/jcabi-github-0.38-jar-with-dependencies.jar
wget -q http://central.maven.org/maven2/com/jcraft/jsch/0.1.46/jsch-0.1.46.jar
wget -q http://central.maven.org/maven2/org/apache/commons/commons-email/1.3.1/commons-email-1.3.1.jar
wget -q http://central.maven.org/maven2/javax/mail/mail/1.4.7/mail-1.4.7.jar

# Create jenkins config files
python eval.py

### Manual Steps ###
# Go to http://<ip>:8080/ 
# - Install suggested plugins
# - Create admin user
# - Enable anonymous authorization 
#    - (Configure Global Security Settings > Anyone can do anything)
#    - TODO: Secure jenkins so we don't have to do this (Issue #14)
# sudo visudo
# - Disable requiretty
# - Update jenkins user privileges (jenkins  ALL= NOPASSWD: ALL)
# sudo ansible-playbook start-bot.yml
