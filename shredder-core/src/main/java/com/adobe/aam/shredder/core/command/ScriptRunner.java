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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

public class ScriptRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptRunner.class);
    private static final long SCRIPT_TIMEOUT_MS = 60 * 60 * 1000;

    private final Collection<String> scripts;
    private final CommandRunner commandRunner;

    @Inject
    public ScriptRunner(@Named("scripts") Collection<String> scripts,
                        CommandRunner commandRunner) {
        this.scripts = scripts;
        this.commandRunner = commandRunner;
    }

    public void runScripts(Runnable heartbeat) {
        for (String script : scripts) {
            runScript(script, heartbeat);
        }
    }

    private void runScript(String script, Runnable heartbeat) {
        try {
            LOG.info("Running script: {}", script);
            int exitCode = commandRunner.execute(script, heartbeat, SCRIPT_TIMEOUT_MS);
            LOG.info("Command finished with exit code {} - {}.", exitCode, exitCode == 0 ? "SUCCESS" : "ERROR");
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Unable to run script", e.getMessage());
        }
    }
}
