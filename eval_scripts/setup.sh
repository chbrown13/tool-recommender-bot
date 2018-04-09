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

# Start tool-recommender-bot study
wget https://raw.githubusercontent.com/chbrown13/PullRecommender/master/eval_scripts/tool-recommender-bot.yml
wget https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/eval_scripts/delete.yml
wget https://raw.githubusercontent.com/chbrown13/PullRecommender/master/jenkins.xml
wget https://raw.githubusercontent.com/chbrown13/PullRecommender/master/eval_scripts/eval.py
python eval.py

## Manual Steps ##
# Go to http://<ip>:8080/ 
# Install suggested plugins
# Follow instructions to start jenkins and create admin user
# Enable anonymous authorization (Configure Global Security Settings, Anyone can do anything)
# Disable requiretty in visudo
# Change jenkins user privileges in visudo (jenkins  ALL= NOPASSWD: ALL)
