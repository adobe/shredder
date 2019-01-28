package com.adobe.aam.shredder.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostnameProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HostnameProvider.class);

    public String getHostname() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Unable to fetch hostname", e);
            return "no-hostname";
        }
    }
}
