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

package com.adobe.aam.shredder.ec2.log;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StartupResultPersist {

    private final String startupPersistFile;

    @Inject
    public StartupResultPersist(@Named("startupPersistFile") String startupPersistFile) {

        this.startupPersistFile = startupPersistFile;
    }

    public enum Result {
        SUCCESSFUL,
        FAILED,
        STARTUP_NOT_RUN
    }

    public void persist(boolean startupSuccessful) throws IOException {
        String result = startupSuccessful ? "true" : "false";
        Files.write(Paths.get(startupPersistFile), result.getBytes());
    }

    public Result getPreviousStartupResult() throws IOException {
        File file = new File(startupPersistFile);
        if (!file.exists()) {
            return Result.STARTUP_NOT_RUN;
        }

        String result = new String(Files.readAllBytes(Paths.get(startupPersistFile)));
        switch (result) {
            case "true":
                return Result.SUCCESSFUL;
            case "false":
                return Result.FAILED;
            default:
                return Result.STARTUP_NOT_RUN;
        }
    }
}
