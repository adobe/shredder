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

import com.adobe.aam.shredder.core.trigger.TriggerMessage;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;

public class PlaybookRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PlaybookRunner.class);
    private static final long COMMAND_TIMEOUT_MS = 60 * 60 * 1000;

    private final Collection<String> commands;
    private final MacroReplacer macroReplacer;
    private final CommandRunner commandRunner;

    @Inject
    public PlaybookRunner(@Named("commands") Collection<String> commands,
                          MacroReplacer macroReplacer,
                          CommandRunner commandRunner) {
        this.commands = commands;
        this.macroReplacer = macroReplacer;
        this.commandRunner = commandRunner;
    }

    public void runCommands(TriggerMessage trigger, Runnable heartbeat) {
        for (String command : commands) {
            runCommand(macroReplacer.replaceMacros(command, trigger), heartbeat);
        }
    }

    private void runCommand(String command, Runnable heartbeat) {
        try {
            LOG.info("Running command: {}", command);
            int exitCode = commandRunner.execute(command, heartbeat, COMMAND_TIMEOUT_MS);
            LOG.info("Command finished with exit code {} - {}.", exitCode, exitCode == 0 ? "SUCCESS" : "ERROR");
        } catch (Exception e) {
            LOG.error("Unable to run command", e);
        }
    }
}
