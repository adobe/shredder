getent group ddx >/dev/null || groupadd -r ddx
getent passwd ddxapp >/dev/null || useradd -r -g ddx -s /bin/bash -m -d /home/ddxapp -c "Ddxapp user" ddxapp

exit 0
