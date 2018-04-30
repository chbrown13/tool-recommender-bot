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


# Install Ansible
sudo yum -y install ansible
echo "localhost ansible_connection=local" > hosts
sudo mv hosts /etc/ansible/hosts


# Download tool-recommender-bot files and dependencies
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/eval_scripts/start-bot.yml
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/eval_scripts/stop-bot.yml
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/jenkins.xml
wget -q https://raw.githubusercontent.com/chbrown13/tool-recommender-bot/master/eval_scripts/eval.py


# Run python setup script files
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
