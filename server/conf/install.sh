#!/bin/bash
# This will install the brigade server on an EC2 instance when it first launches.

# Upgrade to Java 1.8
yum -y install java-1.8.0
yum -y remove java-1.7.0-openjdk

# Create the brigade user.
useradd -d /opt/brigade -m brigade
echo "su -c '/opt/brigade/brigade' brigade &" >> /etc/rc.local

# Configure the boot script.
su brigade
cd /opt/brigade
cat > brigade <<- EOM
#!/bin/bash
cd
aws s3 cp s3://brigade.wayfarerx.net/token.txt .
aws s3 cp s3://brigade.wayfarerx.net/brigade.jar .
cat token.txt | java -jar brigade.jar s3://brigade.wayfarerx.net/guilds
EOM
chmod +x brigade
