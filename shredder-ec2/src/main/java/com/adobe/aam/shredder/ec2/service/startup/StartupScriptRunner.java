package com.adobe.aam.shredder.ec2.service.startup;

import com.adobe.aam.shredder.ec2.runner.StartupCommandsRunner;

public class StartupScriptRunner implements StartupRunner {

    private StartupCommandsRunner startupCommandsRunner;

    public StartupScriptRunner(StartupCommandsRunner startupCommandsRunner) {
        this.startupCommandsRunner = startupCommandsRunner;
    }

    @Override
    public boolean getStartupResult() {
        return startupCommandsRunner.getRunStartupScriptsResult();
    }
}
