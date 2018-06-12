/*
 *  Copyright 2018 Adobe Systems Incorporated. All rights reserved.
 *  This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License. You may obtain a copy
 *  of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *  OF ANY KIND, either express or implied. See the License for the specific language
 *  governing permissions and limitations under the License.
 */

package com.adobe.aam.shredder.core.command;

import com.amazonaws.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

public class CommandRunner extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(CommandRunner.class);
    private long lastHeartBeat;

    /**
     * @return the exit code
     */
    public int execute(String commandLine, Runnable heartbeat, long timeoutMs) throws IOException, InterruptedException, TimeoutException {

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{
                    "bash", "-c",
                    commandLine
            });

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
        if (timeNow - lastHeartBeat > 60 * 1000) {
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

}
