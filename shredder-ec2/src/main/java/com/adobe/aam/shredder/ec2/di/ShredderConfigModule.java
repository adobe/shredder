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

package com.adobe.aam.shredder.ec2.di;

import com.adobe.aam.shredder.core.command.ScriptRunner;
import com.adobe.aam.shredder.core.command.ScriptRunner.ScriptRunnerFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;

import javax.inject.Named;
import java.time.Duration;

public class ShredderConfigModule extends AbstractModule {

    @Override
    public void configure() {
    }

    @Provides
    @Named("scriptOutputPath")
    public String scriptOutputPath(Config config) {
        return config.getString("script_output_path");
    }

    @Provides
    @Named("startupPersistFile")
    public String startupPersistFile(Config config) {
        return config.getString("startup_persist_result_file");
    }

    @Provides
    @Named("startupScriptRunner")
    public ScriptRunner startupScriptRunner(ScriptRunnerFactory scriptRunnerFactory, Config config) {

        String startupScriptsPath = config.getString("startup_scripts_path");
        String startupScriptsPriority = config.hasPath("startup_scripts_priority")
                ? config.getString("startup_scripts_priority")
                : "";
        return scriptRunnerFactory.create(startupScriptsPath, startupScriptsPriority);
    }

    @Provides
    @Named("shutdownScriptRunner")
    public ScriptRunner shutdownScriptRunner(
            ScriptRunnerFactory scriptRunnerFactory,
            Config config) {

        String shutdownScriptsPath = config.getString("shutdown_scripts_path");
        String shutdownScriptsPriority = config.hasPath("shutdown_scripts_priority")
                ? config.getString("shutdown_scripts_priority")
                : "";
        return scriptRunnerFactory.create(shutdownScriptsPath, shutdownScriptsPriority);
    }

    @Provides
    @Named("maxWaitTimeOnShutdownFailure")
    public Duration getMaxWaitTimeOnShutdownFailure(Config config) {
        return config.hasPath("shutdown_wait_time_if_failure")
                ? config.getDuration("shutdown_wait_time_if_failure")
                : Duration.ofMillis(0);
    }

    @Provides
    @Named("sendCloudWatchMetrics")
    public Boolean sendCloudWatchMetrics(Config config) {
        return config.getBoolean("send_cloud_watch_metrics");
    }

    @Provides
    @Named("cloudWatchNamespace")
    public String getCloudWatchNamespace(Config config) {
        return config.getString("cloud_watch_namespace");
    }

    @Provides
    @Named("startupScriptsEnabled")
    public boolean startupScriptsEnabled(Config config) {
        return config.getBoolean("startup_scripts_enabled");
    }

    @Provides
    @Named("shutdownOnStartupFail")
    public boolean shutdownOnStartupFail(Config config) {
        return config.getBoolean("shutdown_on_startup_fail");
    }
}
