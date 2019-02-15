getent group ddx >/dev/null || groupadd -g 5000 -r ddx
getent passwd ddxapp >/dev/null || useradd -r -u 1001 -g ddx -s /bin/bash -m -d /home/ddxapp -c "Ddxapp user" ddxapp

mkdir -p /var/run/demdex
chown ddxapp:ddx /var/run/demdex
exit 0
