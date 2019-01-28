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

import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptRunner.class);
    private static final long SCRIPT_TIMEOUT_MS = 60 * 60 * 1000;
    private final String scriptsPriority;
    private final String scriptsPath;
    private final CommandRunner commandRunner;

    public interface ScriptRunnerFactory {
        ScriptRunner create(@Assisted("scriptsPath") String scriptsPath,
                            @Assisted("scriptsPriority") String scriptsPriority);
    }

    @Inject
    public ScriptRunner(@Assisted("scriptsPath") String scriptsPath,
                        @Assisted("scriptsPriority") String scriptsPriority,
                        CommandRunner commandRunner) {
        this.scriptsPath = scriptsPath;
        this.scriptsPriority = scriptsPriority;
        this.commandRunner = commandRunner;
    }

    /**
     * @return true if all scripts finished with exit code 0, false otherwise.
     */
    public boolean runScripts(Runnable heartbeat) {
        List<String> scriptsPriorityList = Arrays.asList(scriptsPriority.split(":"));
        return getAllScripts(scriptsPath, scriptsPriorityList)
                .stream()
                .allMatch(script -> getRunScriptResult(script, heartbeat));
    }

    private boolean getRunScriptResult(String script, Runnable heartbeat) {
        try {
            LOG.info("Running script: {}", script);
            int exitCode = commandRunner.execute(script, heartbeat, SCRIPT_TIMEOUT_MS);
            LOG.info("Command finished with exit code {} - {}.", exitCode, exitCode == 0 ? "SUCCESS" : "ERROR");
            return exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Unable to run script", e.getMessage());
            return false;
        }
    }

    public String getScriptsPath() {
        return scriptsPath;
    }

    private static Collection<String> getAllScripts(String scriptsPath, List<String> scriptsPriority) {
        try {
            return Files.walk(Paths.get(scriptsPath))
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(".sh"))
                    .map(File::getAbsolutePath)
                    .sorted((script1, script2) -> compareScriptsPriority(script1, script2, scriptsPriority))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("No execution scripts found in {}", scriptsPath);
            return Collections.emptySet();
        }
    }

    private static int compareScriptsPriority(String script1, String script2, List<String> scriptsPriority) {
        for (String scriptPriority : scriptsPriority) {
            if (script1.endsWith(scriptPriority.trim())) {
                // script1 should be run before script2
                return -1;
            }

            if (script2.endsWith(scriptPriority.trim())) {
                // script1 should be run after script2
                return 1;
            }
        }
        return 0;
    }
}
