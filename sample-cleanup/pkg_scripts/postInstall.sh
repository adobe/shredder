{
    find /usr/local/demdex/sample-cleanup -type f -exec chmod -x {} \;
    find /usr/local/demdex/sample-cleanup -type f -name \*.sh -exec chmod +x {} \;

    chmod +x /etc/init.d/sample-cleanup

    # Notify New Relic of deployment if agent is found.
    if [ -f "/etc/newrelic-java/newrelic.jar" ]; then
       java -jar /etc/newrelic-java/newrelic.jar deployment --revision=1.0 # TODO get %{_rpmVersion}
    fi
} &> /dev/null || :
