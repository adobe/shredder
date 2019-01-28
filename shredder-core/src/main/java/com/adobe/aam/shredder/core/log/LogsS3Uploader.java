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

import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LogsS3Uploader implements LogsUploader {

    private static final Logger LOG = LoggerFactory.getLogger(LogsS3Uploader.class);

    private final String s3Bucket;
    private final TransferManager transferManager;

    public LogsS3Uploader(AmazonS3 s3Client, String s3Bucket) {
        this.s3Bucket = s3Bucket;
        this.transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
    }

    @Override
    public synchronized void upload(String sourceDir, String s3Path) throws InterruptedException {
        LOG.info("Uploading logs from {} to S3: {}/{}", sourceDir, s3Bucket, s3Path);

        File logDir = new File(sourceDir);
        if (!logDir.exists()) {
            LOG.warn("Log directory could not be found: {}", logDir);
            return;
        }

        MultipleFileUpload upload = transferManager.uploadDirectory(s3Bucket, s3Path, logDir, true);

        upload.addProgressListener((ProgressListener) progressEvent -> {
            LOG.info("Upload status update: {}", progressEvent);
        });
        upload.waitForCompletion();
    }
}
