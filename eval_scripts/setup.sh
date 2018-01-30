# Update
sudo yum -y install epel-release
sudo yum -y update

# Install commands
sudo yum -y install wget
sudo yum -y install unzip
sudo yum -y install python-pip
sudo pip install --upgrade pip
sudo pip install paramiko

# Install Java
sudo yum -y install java-1.8.0-openjdk.x86_64
sudo cp /etc/profile /etc/profile_backup
echo 'export JAVA_HOME=/usr/lib/jvm/jre-1.8.0-openjdk' | sudo tee -a /etc/profile
echo 'export JRE_HOME=/usr/lib/jvm/jre' | sudo tee -a /etc/profile
source /etc/profile
echo $JAVA_HOME
echo $JRE_HOME
cd ~

# Install and Start Jenkins
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
sudo yum -y install jenkins
sudo systemctl start jenkins.service
sudo systemctl enable jenkins.service
sudo firewall-cmd --zone=public --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
wget http://localhost:8080/jnlpJars/jenkins-cli.jar

# Install Groovy
sudo yum -y install groovy

# Create Jenkins user

# Install Ansible
sudo yum -y install ansible
echo "localhost ansible_connection=local" > hosts
sudo mv hosts /etc/ansible/hosts

# Start tool-recommender-bot study
wget https://raw.githubusercontent.com/chbrown13/PullRecommender/master/eval_scripts/tool-recommender-bot.yml
wget https://gist.githubusercontent.com/chbrown13/ca345f0d96401144373b745975724412/raw/72cf4f75ec85bc9ad94c1e072965c797503e52b9/.projects_ssh
wget https://raw.githubusercontent.com/chbrown13/PullRecommender/master/jenkins.xml
wget https://raw.githubusercontent.com/chbrown13/PullRecommender/master/eval_scripts/eval.py
python eval.py
ansible-playbook tool-recommender-bot.yml
