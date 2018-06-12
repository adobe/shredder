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

import com.amazonaws.util.StringUtils;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class ShredderConfigModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(ShredderConfigModule.class);
    private static final String DEFAULT_SHUTDOWN_SCRIPTS_PATH = "/opt/shutdown-scripts/";
    


    @Override
    protected void configure() {
    }

    @Provides @Singleton
    public Config provideConfiguration() {
        return ConfigFactory.load();
    }

    @Provides
    @Named("scripts")
    public Collection<String> scripts(Config config) {
        return getShutdownScripts(config);
    }

    private Collection<String> getShutdownScripts(Config config) {
        String shutdownScriptsPath = config.getString("shutdown_scripts_path");
        if (StringUtils.isNullOrEmpty(shutdownScriptsPath)) {
            shutdownScriptsPath = DEFAULT_SHUTDOWN_SCRIPTS_PATH;
        }
        try {
            return Files.list(Paths.get(shutdownScriptsPath))
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(".sh"))
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("No shutdown scripts found");
            return Collections.emptyList();
        }
    }
}
