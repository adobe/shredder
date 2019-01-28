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

package com.adobe.aam.shredder.ec2.service.startup;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.adobe.aam.shredder.core.aws.servergroup.AutoScaleGroupHelper.HEARTBEAT_INTERVAL_MS;

public class StartupExternalSignalReceiver implements StartupRunner {

    private static final Logger LOG = LoggerFactory.getLogger(StartupExternalSignalReceiver.class);
    private final CountDownLatch waitExternalSignal = new CountDownLatch(1);
    private final AtomicBoolean syncResult = new AtomicBoolean();
    private final HttpServer server;
    private final int httpPort;
    private final Duration timeout;
    private final Runnable heartbeat;

    public StartupExternalSignalReceiver(int httpPort, Duration timeout, Runnable heartbeat) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(httpPort), 0);
        this.httpPort = httpPort;
        this.timeout = timeout;
        this.heartbeat = heartbeat;
    }

    @Override
    public boolean getStartupResult() {
        server.createContext("/health", response -> sendResponse(response, "OK"));
        server.createContext("/startup-ok", new SuccessHandler(syncResult));
        server.createContext("/startup-fail", new FailureHandler(syncResult));
        server.setExecutor(null); // creates a default executor
        server.start();

        Timer heartbeatTimer = startHeartbeatTimer();
        boolean startupSuccessful = waitHttpResult();
        heartbeatTimer.cancel();
        server.stop(0);
        return startupSuccessful;
    }

    private Timer startHeartbeatTimer() {
        TimerTask task = new TimerTask() {
            public void run() {
                heartbeat.run();
            }
        };
        Timer heartbeatTimer = new Timer("HeartbeatTimer");
        heartbeatTimer.schedule(task, 0, HEARTBEAT_INTERVAL_MS);
        return heartbeatTimer;
    }

    private boolean waitHttpResult() {
        LOG.info("Waiting up to {} minutes for an external HTTP signal to be received on port {}, /startup-ok or /startup-fail",
                timeout.toMinutes(), httpPort);
        boolean startupSuccessful;
        synchronized (syncResult) {
            try {
                waitExternalSignal.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
                startupSuccessful = syncResult.get();
            } catch (InterruptedException e) {
                LOG.error("Interrupted while waiting for external signal to tell us that startup was ok", e);
                startupSuccessful = false;
            }
        }

        return startupSuccessful;
    }

    class SuccessHandler implements HttpHandler {
        private final AtomicBoolean syncResult;

        public SuccessHandler(AtomicBoolean syncResult) {
            this.syncResult = syncResult;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            sendResponse(httpExchange, "OK acknowledged");
            notifyResult(syncResult, true);
        }
    }

    class FailureHandler implements HttpHandler {
        private final AtomicBoolean syncResult;

        public FailureHandler(AtomicBoolean syncResult) {
            this.syncResult = syncResult;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            sendResponse(httpExchange, "FAILURE acknowledged");
            notifyResult(syncResult, false);
        }
    }

    private synchronized void notifyResult(AtomicBoolean syncResult, boolean result) {
        syncResult.set(result);
        waitExternalSignal.countDown();
    }

    private static void sendResponse(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
