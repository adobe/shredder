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

package com.adobe.aam.shredder.core.di;

import com.amazonaws.util.StringUtils;
import com.github.rholder.retry.*;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ConfigModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigModule.class);
    private static final String DEFAULT_CONFIG_FILE = "/enter/path/to/reference.conf";

    @Override
    protected void configure() {

    }

    @Provides @Singleton
    public Config provideConfiguration() {
        try {
            Retryer<Config> retryer = RetryerBuilder.<Config>newBuilder()
                    .retryIfException()
                    .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.neverStop())
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            if (attempt.hasException()) {
                                LOG.info("Retrying to read config in 10 seconds.", attempt.getExceptionCause());
                            }
                        }
                    })
                    .build();
            return retryer.call(ConfigModule::getConfig);
        } catch (RetryException | ExecutionException e) {
            throw new RuntimeException("Unable to read configs.", e);
        }
    }

    @Provides @Named("commands")
    public Collection<String> commands(Config config) {
        return config.getStringList("commands");
    }

    private static Config getConfig() throws ConfigException {
        String confOverrides = System.getenv("SHREDDER_APP_CONFIG");
        Config override = confOverrides == null ? ConfigFactory.empty() : ConfigFactory.parseString(confOverrides);

        File configFile = getConfigFile();
        if (!configFile.exists()) {
            throw new ConfigException("Unable to find file " + configFile.getAbsolutePath()) {};
        }

        LOG.info("Reading config file from {}", configFile);
        Config cfg = ConfigFactory.parseFile(configFile);
        return override
                .withFallback(ConfigFactory.systemEnvironment())
                .withFallback(cfg)
                .resolve();
    }

    /**
     * @return
     * 1. Environment variable
     * 2. Java property
     * 3. default config file
     */
    private static File getConfigFile() {
        String file = System.getenv("SHREDDER_CONFIG_FILE");
        if (StringUtils.isNullOrEmpty(file)) {
            file = System.getProperty("SHREDDER_CONFIG_FILE");
        }

        if (StringUtils.isNullOrEmpty(file)) {
            file = DEFAULT_CONFIG_FILE;
        }

        return new File(file);
    }
}
