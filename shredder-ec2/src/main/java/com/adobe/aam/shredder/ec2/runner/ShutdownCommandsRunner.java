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

package com.adobe.aam.shredder.ec2.runner;

import com.adobe.aam.shredder.core.command.ScriptRunner;
import com.adobe.aam.shredder.ec2.aws.LifecycleHandler;
import com.adobe.aam.shredder.ec2.trigger.ShutdownLifecycleHookMessage;

import javax.inject.Inject;
import javax.inject.Named;

public class ShutdownCommandsRunner {

    private final LifecycleHandler lifecycleHandler;
    private final ScriptRunner shutdownScriptRunner;

    @Inject
    public ShutdownCommandsRunner(LifecycleHandler lifecycleHandler,
                                  @Named("shutdownScriptRunner") ScriptRunner shutdownScriptRunner) {
        this.lifecycleHandler = lifecycleHandler;
        this.shutdownScriptRunner = shutdownScriptRunner;
    }

    public boolean getRunShutdownScriptsResult(ShutdownLifecycleHookMessage trigger) {
        return shutdownScriptRunner.runScripts(() -> lifecycleHandler.sendHeartbeat(trigger));
    }
}
