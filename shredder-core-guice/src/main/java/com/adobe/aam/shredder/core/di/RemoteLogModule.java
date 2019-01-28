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

import com.adobe.aam.shredder.core.command.ScriptRunner;
import com.adobe.aam.shredder.core.log.LogsNoOpUploader;
import com.adobe.aam.shredder.core.log.LogsS3Uploader;
import com.adobe.aam.shredder.core.log.LogsUploader;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

public class RemoteLogModule implements Module {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteLogModule.class);

    @Override
    public void configure(Binder binder) {
        binder.install(new FactoryModuleBuilder().build(ScriptRunner.ScriptRunnerFactory.class));
    }

    @Provides
    public LogsUploader getLogsUploader(@Named("awsEnabled") boolean awsEnabled,
                                        Config config) {
        if (!awsEnabled) {
            return new LogsNoOpUploader();
        }

        String remoteLogDestination = config.hasPath("remote_log_destination")
                ? config.getString("remote_log_destination").toLowerCase()
                : "";
        switch (remoteLogDestination) {
            case "s3":
                String region = config.getString("remote_log_s3_region");
                return new LogsS3Uploader(getS3Client(region), config.getString("remote_log_s3_bucket"));
            default:
                return new LogsNoOpUploader();
        }
    }

    private AmazonS3 getS3Client(String region) {
        return AmazonS3ClientBuilder.standard().withRegion(region).build();
    }
}
