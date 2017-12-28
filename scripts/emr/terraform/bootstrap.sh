#!/bin/bash

S3_ACCESS_KEY=$1
S3_SECRET_KEY=$2
S3_NOTEBOOK_BUCKET=$3
S3_NOTEBOOK_PREFIX=$4
EXECUTE_SCRIPT=$5

# Parses a configuration file put in place by EMR to determine the role of this node
is_master() {
  if [ $(jq '.isMaster' /mnt/var/lib/info/instance.json) = 'true' ]; then
    return 0
  else
    return 1
  fi
}

if is_master && [ "$EXECUTE_SCRIPT" == "true" ] ; then
    echo "Installing system software ..."
    curl -sL https://rpm.nodesource.com/setup_6.x | sudo -E bash -
    sudo yum install -y -q nodejs
    sudo service crond start

    sudo pip-3.4 -q install jupyter
    sudo npm install -g configurable-http-proxy
    sudo pip-3.4 -q install jupyterhub
    sudo pip-3.4 -q install --upgrade notebook
    sudo pip-3.4 -q install sudospawner
    sudo pip-3.4 -q install s3contents

    echo "Setting up user accounts ..."
    sudo groupadd shadow
    sudo chgrp shadow /etc/shadow
    sudo chmod 640 /etc/shadow
    sudo useradd -G shadow -r hublauncher
    sudo groupadd jupyterhub
    echo 'hublauncher ALL=(%jupyterhub) NOPASSWD: /usr/local/bin/sudospawner' | sudo tee -a /etc/sudoers

    sudo adduser -G hadoop,jupyterhub user
    echo 'user:password' | sudo chpasswd

    echo 'cd /tmp && sudo -u hublauncher -E env "PATH=/usr/local/bin:$PATH" jupyterhub -f /tmp/jupyterhub_config.py' | at now + 1 minute

    echo "Installing jupyter-scala kernel ..."
    curl -L -q -o /tmp/jupyter-scala https://raw.githubusercontent.com/jupyter-scala/jupyter-scala/98bac7034f07e3e51d101846953aecbdb7a4bb5d/jupyter-scala
    chmod +x /tmp/jupyter-scala
    sudo -u user /tmp/jupyter-scala > /dev/null

    echo "Configuring jupyterhub and S3 notebook storage ..."
    cat <<EOF > /tmp/jupyterhub_config.py
c = get_config()

# Let JupyterHub use sudospawner for spawning notebook instances
c.JupyterHub.spawner_class='sudospawner.SudoSpawner'
c.SudoSpawner.sudospawner_path='/usr/local/bin/sudospawner'
EOF

    cat <<EOF > /tmp/per_user_jupyter_notebook_config.py
from s3contents import S3ContentsManager

c = get_config()

# Tell Jupyter to use S3ContentsManager for all storage.
c.NotebookApp.contents_manager_class = S3ContentsManager
c.S3ContentsManager.access_key_id = "$S3_ACCESS_KEY"
c.S3ContentsManager.secret_access_key = "$S3_SECRET_KEY"
c.S3ContentsManager.bucket = "$S3_NOTEBOOK_BUCKET"
c.S3ContentsManager.prefix = "$S3_NOTEBOOK_PREFIX"
EOF

    sudo -u user mkdir /home/user/.jupyter
    sudo -u user cp /tmp/per_user_jupyter_notebook_config.py /home/user/.jupyter/jupyter_notebook_config.py

    # Fix a problem in the Jupyter notebook FileContentsManager (required by S3Contents)
    cat <<EOF > /tmp/manager.patch
33c33
<
---
> import notebook.transutils
EOF
    patch /usr/local/lib/python3.4/site-packages/notebook/services/contents/manager.py -i /tmp/manager.patch -o /tmp/manager.py
    sudo mv /tmp/manager.py /usr/local/lib/python3.4/site-packages/notebook/services/contents/manager.py
    sudo chown root:root /usr/local/lib/python3.4/site-packages/notebook/services/contents/manager.py
    sudo chmod 644 /usr/local/lib/python3.4/site-packages/notebook/services/contents/manager.py

    # Environment setup
    cat <<EOF > /tmp/jupyter_profile.sh
export AWS_DNS_NAME=$(aws ec2 describe-network-interfaces --filters Name=private-ip-address,Values=$(hostname -i) | jq -r '.[] | .[] | .Association.PublicDnsName')

alias launch_hub='sudo -u hublauncher -E env "PATH=/usr/local/bin:$PATH" jupyterhub -f /tmp/jupyterhub_config.py'
EOF
    sudo mv /tmp/jupyter_profile.sh /etc/profile.d
    . /etc/profile.d/jupyter_profile.sh

    # Install boilerplate extension
    cd /tmp
    mkdir boilerplate
    mv bp.js boilerplate/main.js
    sudo npm install requirejs
    sudo /usr/local/bin/jupyter nbextension install --system boilerplate
    sudo /usr/local/bin/jupyter nbextension enable --system boilerplate/main

    echo "Running at host $AWS_DNS_NAME"
fi
