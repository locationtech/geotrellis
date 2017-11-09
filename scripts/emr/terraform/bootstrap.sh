#!/bin/bash

# Parses a configuration file put in place by EMR to determine the role of this node
is_master() {
  if [ $(jq '.isMaster' /mnt/var/lib/info/instance.json) = 'true' ]; then
    return 0
  else
    return 1
  fi
}

if is_master; then
    sudo yum -y update
    curl -sL https://rpm.nodesource.com/setup_6.x | sudo -E bash -
    sudo yum install -y nodejs

    sudo pip-3.4 install jupyter
    sudo npm install -g configurable-http-proxy
    sudo pip-3.4 install jupyterhub
    sudo pip-3.4 install --upgrade notebook

    sudo pip-3.4 install sudospawner
    sudo pip-3.4 install "https://github.com/jupyterhub/oauthenticator/archive/f5e39b1ece62b8d075832054ed3213cc04f85030.zip"

    curl -L -o /tmp/jupyter-scala https://raw.githubusercontent.com/jupyter-scala/jupyter-scala/98bac7034f07e3e51d101846953aecbdb7a4bb5d/jupyter-scala
    chmod +x /tmp/jupyter-scala
    /tmp/jupyter-scala

    # Set up user account to manage JupyterHub
    sudo groupadd shadow
    sudo chgrp shadow /etc/shadow
    sudo chmod 640 /etc/shadow
    sudo useradd -G shadow -r hublauncher
    sudo groupadd jupyterhub

    echo 'hublauncher ALL=(%jupyterhub) NOPASSWD: /usr/local/bin/sudospawner' | sudo tee -a /etc/sudoers
    echo 'hublauncher ALL=(ALL) NOPASSWD: /usr/sbin/useradd' | sudo tee -a /etc/sudoers
    echo 'hublauncher ALL=(hdfs) NOPASSWD: /usr/bin/hdfs' | sudo tee -a /etc/sudoers

    # Environment setup
    cat <<EOF > /tmp/oauth_profile.sh
export AWS_DNS_NAME=$(aws ec2 describe-network-interfaces --filters Name=private-ip-address,Values=$(hostname -i) | jq -r '.[] | .[] | .Association.PublicDnsName')
export OAUTH_CALLBACK_URL=http://\$AWS_DNS_NAME:8000/hub/oauth_callback
export OAUTH_CLIENT_ID=$OAUTH_CLIENT_ID
export OAUTH_CLIENT_SECRET=$OAUTH_CLIENT_SECRET

alias launch_hub='sudo -u hublauncher -E env "PATH=/usr/local/bin:$PATH" jupyterhub --JupyterHub.spawner_class=sudospawner.SudoSpawner --SudoSpawner.sudospawner_path=/usr/local/bin/sudospawner --Spawner.notebook_dir=/home/{username}'
EOF
    sudo mv /tmp/oauth_profile.sh /etc/profile.d
    . /etc/profile.d/oauth_profile.sh

    # Setup required scripts/configurations for launching JupyterHub
    cat <<EOF > /tmp/new_user
#!/bin/bash

user=\$1

sudo useradd -m -G jupyterhub,hadoop \$user
sudo -u hdfs hdfs dfs -mkdir /user/\$user

EOF
    chmod +x /tmp/new_user
    sudo chown root:root /tmp/new_user
    sudo mv /tmp/new_user /usr/local/bin
    
    cat <<EOF > /tmp/jupyterhub_config.py
from oauthenticator.github import LocalGitHubOAuthenticator

c = get_config()
c.JupyterHub.authenticator_class = LocalGitHubOAuthenticator
c.LocalGitHubOAuthenticator.create_system_users = True

c.JupyterHub.spawner_class='sudospawner.SudoSpawner'
c.SudoSpawner.sudospawner_path='/usr/local/bin/sudospawner'
c.Spawner.notebook_dir='/home/{username}'
c.LocalAuthenticator.add_user_cmd = ['new_user']

EOF

    # Execute
    cd /tmp
fi
