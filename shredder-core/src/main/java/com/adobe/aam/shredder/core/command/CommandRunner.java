/*
 * Copyright 2019 Adobe Systems Incorporated. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.aam.shredder.core.command;

import com.amazonaws.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.util.concurrent.TimeoutException;

import static com.adobe.aam.shredder.core.aws.servergroup.AutoScaleGroupHelper.HEARTBEAT_INTERVAL_MS;

public class CommandRunner extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(CommandRunner.class);
    private long lastHeartBeat;
    private final String instanceId;
    private final String region;
    private final String accountId;
    private final String environment;
    private final String scriptOutputPath;

    @Inject
    public CommandRunner(@Named("instanceId") String instanceId,
                         @Named("region") String region,
                         @Named("accountId") String accountId,
                         @Named("environment") String environment,
                         @Named("scriptOutputPath") String scriptOutputPath) {
        this.instanceId = instanceId;
        this.region = region;
        this.accountId = accountId;
        this.environment = environment;
        this.scriptOutputPath = scriptOutputPath;
    }

    /**
     * @return the exit code
     */
    public int execute(String commandLine, Runnable heartbeat, long timeoutMs) throws IOException, InterruptedException, TimeoutException {

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{
                    "bash", "-c",
                    buildCommand(commandLine)
            }, getEnvironment());

            waitProcessFinish(process, heartbeat, timeoutMs);
            logProcessOutputs(process);
            return process.exitValue();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void waitProcessFinish(Process process, Runnable heartbeat, long timeoutMs) throws InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        while (process.isAlive()) {
            Thread.sleep(1000);
            sendHeartbeat(heartbeat);
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                process.destroy();
                throw new TimeoutException("Killed process which exceeded " + timeoutMs + " ms.");
            }
        }
    }

    private void sendHeartbeat(Runnable heartbeat) {
        long timeNow = System.currentTimeMillis();
        if (timeNow - lastHeartBeat > HEARTBEAT_INTERVAL_MS) {
            lastHeartBeat = timeNow;
            heartbeat.run();
        }
    }

    private static void logProcessOutputs(Process process) throws IOException {
        String stdOutput = read(process.getInputStream());
        if (!StringUtils.isNullOrEmpty(stdOutput)) {
            LOG.info("Command output:\n>>>>>\n{}\n<<<<<", stdOutput);
        }

        String stdError = read(process.getErrorStream());
        if (!StringUtils.isNullOrEmpty(stdError)) {
            LOG.error("Command error:\n>>>>>\n{}\n<<<<<", stdError);
        }
    }

    private static String read(InputStream stream) throws IOException {
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(stream));
        StringBuilder message = new StringBuilder();
        String line;
        while ((line = stdInput.readLine()) != null) {
            message.append(line).append('\n');
        }

        return message.toString().trim();
    }

    private String[] getEnvironment() {
        return new String[]{
                "AWS_INSTANCE_ID=" + instanceId,
                "AWS_REGION=" + region,
                "AWS_ACCOUNT_ID=" + accountId,
                "ENVIRONMENT=" + environment
        };
    }

    private String buildCommand(String command) {
        //template for script execution
        //each script std and err output is redirected to `scriptOutputPath`
        return String.format("echo $(date -u) 'Executing: %1$s' >> %2$s 2>&1; " +
                             "source /etc/profile; " +
                             "%1$s + >> %2$s 2>&1",
                             command, scriptOutputPath);
    }
}
