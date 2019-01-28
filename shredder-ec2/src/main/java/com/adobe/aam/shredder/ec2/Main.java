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

package com.adobe.aam.shredder.ec2;

import com.adobe.aam.shredder.ec2.di.InjectorBuilder;
import com.adobe.aam.shredder.ec2.service.ShutdownService;
import com.adobe.aam.shredder.ec2.service.StartupService;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String argv[]) throws IOException {

        Injector injector = InjectorBuilder.build();

        LOG.info("Starting EC2 shredder daemon.");

        StartupService startupService = injector.getInstance(StartupService.class);
        boolean startupSuccessful = startupService.getStartupResult();

        ShutdownService shutdownService = injector.getInstance(ShutdownService.class);
        boolean shutdownSuccessful = shutdownService.getShutdownResult(startupSuccessful);
        LOG.error("Exiting. shutdownSuccessful={}", shutdownSuccessful);
    }
}
