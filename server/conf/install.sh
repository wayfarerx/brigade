#!/bin/bash
# This will install the circumvolve server on an EC2 instance when it first launches.

# Upgrade to Java 1.8
yum -y install java-1.8.0
yum -y remove java-1.7.0-openjdk

# Create the circumvolve user.
useradd -d /opt/circumvolve -m circumvolve
echo "su -c '/opt/circumvolve/circumvolve' circumvolve &" >> /etc/rc.local

# Configure the boot script.
su circumvolve
cd /opt/circumvolve
cat > circumvolve <<- EOM
#!/bin/bash
cd
aws s3 cp s3://circumvolve.wayfarerx.net/token.txt .
aws s3 cp s3://circumvolve.wayfarerx.net/circumvolve.jar .
cat token.txt | java -jar circumvolve.jar circumvolve.wayfarerx.net guilds
EOM
chmod +x circumvolve
