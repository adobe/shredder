/*
 * Copyright 2019 Adobe Systems Incorporated. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.aam.shredder.core.log;

import com.adobe.aam.shredder.core.HostnameProvider;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteLogUploader {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteLogUploader.class);
    private static final AtomicInteger counter = new AtomicInteger();
    private LogsUploader logsUploader;
    private String appName;
    private List<String> logPaths;
    private final String region;
    private final HostnameProvider hostnameProvider;

    @Inject
    public RemoteLogUploader(LogsUploader logsUploader,
                             HostnameProvider hostnameProvider,
                             @Named("appName") String appName,
                             @Named("logPaths") List<String> logPaths,
                             @Named("region") String region) {
        this.logsUploader = logsUploader;
        this.hostnameProvider = hostnameProvider;
        this.appName = appName;
        this.logPaths = logPaths;
        this.region = region;
    }

    public void upload(String operation, boolean successful) {

        String s3Path = getS3Path(operation, successful);
        logPaths.forEach(localPath -> doUpload(new File(localPath), s3Path));
    }

    private void doUpload(File localPath, String s3Path) {
        try {
            int nextInt = counter.getAndIncrement();
            String tmpLogDir = copyToTmpDir(localPath, nextInt);
            logsUploader.upload(tmpLogDir, s3Path + "/" + nextInt);
            deleteTmpDir(tmpLogDir);
        } catch (InterruptedException | IOException | AmazonS3Exception e) {
            LOG.error("Failed to upload log file", e);
        }
    }

    /**
     * Copying to a tmp folder is required to prevent uploading a log file while it's being updated.
     * In that case the content MD5 might not match, thus leading to an upload error.
     */
    private String copyToTmpDir(File source, int nextInt) throws IOException {
        File dest = new File("/tmp/shredder_tmp_" + nextInt);
        dest.mkdirs();
        if (source.isDirectory()) {
            FileUtils.copyDirectory(source, dest);
        } else {
            FileUtils.copyFileToDirectory(source, dest);
        }
        return dest.getAbsolutePath();
    }

    private void deleteTmpDir(String dir) throws IOException {
        FileUtils.deleteDirectory(new File(dir));
    }

    /**
     * @return example: myapp/log/startup_successful/us-east-1/va6-myapp/shredder.log
     */
    private String getS3Path(String operation, boolean successful) {
        return appName + "/log/" + operation + '_' + (successful ? "successful" : "failed") +
                '/' + region + '/' + hostnameProvider.getHostname();
    }
}
