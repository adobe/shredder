# Shredder for EC2

Shredder for EC2 is a Java daemon service that can perform graceful shutdowns for AWS EC2 instances in an Auto Scaling Group (ASG).

It runs on the EC2 instance and constantly listens for a TERMINATION signal from the Auto Scaling Group (ASG). When such a signal is received, it will start running a list of defined commands (e.g. push data to S3, stop the running services gracefully). After the list of Bash commands is executed, Shredder for EC2 will send a COMPLETE lifecycle message to the Auto Scale Group (ASG). This will trigger the EC2 instance termination.

This tool is useful for services that are stateful - some data needs to be pushed before terminating the EC2 instance. Therefore, it prevents the data loss.

# Install

You can either build the latest packages (see below) or you can use the published artifacts.
For RedHat/Centos
```
sudo yum install https://github.com/adobe/shredder/releases/download/2.0.1/aam-shredder-ec2-2.0.1-20190215154045.noarch.rpm
```

By default, whenever shredder receives a shutdown signal, it will run all the scripts from `/opt/shutdown-scripts` (in lexicographical order). Therefore, you can put your cleanup scripts in this path. For instance:
```bash
$ tree /opt/shutdown-scripts
/opt/shutdown-scripts
├── myapp
│   └── shutdown.sh
└── route53-utils
    └── remove_dns.sh

$ cat /opt/shutdown-scripts/myapp/shutdown.sh
s3_bucket="s3://aam-dcs-shutdownhook-$ENVIRONMENT"
s3_shutdown_data_path="$s3_bucket/log/$AWS_REGION/$HOSTNAME/"

execute_command "aws s3 cp /path/to/data/to/upload/in/s3 $s3_shutdown_data_path --recursive --exclude '*' --include '*.log' --include '*.tmp' --exclude '*processed/*'"
```

Note that shredder automatically injects a set of useful environment variables when running the shutdown scripts: `$AWS_REGION`, `$AWS_ACCOUNT_ID`, `$AWS_INSTANCE_ID`, `$HOSTNAME`, `$ENVIRONMENT`.

In our case, each application RPM that requires a cleanup logic will automatically generate a shutdown script in the shutdown folder (eg. `/opt/shutdown-scripts/myapp/shutdown.sh`).

# How it works

![Shredder for EC2 diagram](https://user-images.githubusercontent.com/952836/35993150-1304c044-0d15-11e8-8a2e-857c5dbbd56e.png)

- When the Shredder for EC2 daemon starts, it will automatically create an SQS queue for the EC2 instance it is running on. For instance: asg-myapp-i-08d384477d1ce8643.
- It will subscribe this SQS queue to an SNS topic described below. 
- When the Auto Scale Group decides it needs to terminate an instance, it will send a SNS notification to this topic. The SNS notification contains the instance id, that is about to be terminated (see next section on how to configure this).
- Each Shredder-for-EC2 that is subscribed to that SNS topic will receive these nofications.
- The Shredder-for-EC2 daemon looks at the termination messages and checks to see if the message is attributed to this instance.
- If it is, then it will start executing the commands. After the commands are ran, Shredder for EC2 sends the COMPLETE lifecyle event to the ASG.

# Configure the Auto Scale Group to send notifications to SNS when it's terminating an EC2 instance

In order for the Shredder for EC2 to be able to receive notifications when the Auto Scaling Group (ASG) wants to terminate the EC2 instance, we need to add a shutdown hook on the ASG itself.

This can be achieved with a command similar to this:
```
aws autoscaling put-lifecycle-hook
   --lifecycle-hook-name my-hook
   --auto-scaling-group-name my-asg
   --lifecycle-transition autoscaling:EC2_INSTANCE_TERMINATING
   --role-arn arn:aws:iam::0123456789:role/myrole
   --notification-target-arn arn:aws:sns:us-east-1:0123456789:spinnaker-shutdowns-pending'
   --heartbeat-timeout 300
   --default-result CONTINUE
```

Reference:
- https://docs.aws.amazon.com/autoscaling/ec2/userguide/lifecycle-hooks.html

## Configure shutdown hooks in Spinnaker

Alternatively, for Spinnaker (https://www.spinnaker.io), instead of running the above command manually, you can instruct the Cloud Driver to add the shutdown hook on ALL auto scale groups being created with Spinnaker.

`$> vim /opt/spinnaker/config/clouddriver-local.yml`
```yaml
aws:
  defaultAssumeRole: role/<>
  accounts:

    - name: <>-aws
      accountId: "<>"
      regions:
        - name: us-east-1

    - name: adev-aws
      accountId: "<>"
      regions:
        - name: us-east-1
      lifecycleHooks:
        - defaultResult: 'CONTINUE'
          heartbeatTimeout: 7200
          lifecycleTransition: 'autoscaling:EC2_INSTANCE_TERMINATING'
          notificationTargetARN: 'arn:aws:sns:{{region}}:{{accountId}}:spinnaker-shutdowns-pending'
          roleARN: 'arn:aws:iam::{{accountId}}:role/spinnakerasg'
```

# AWS permissions

## Sample IAM role 

The IAM role (myrole/spinnakerasg) must have permissions to publish to the specified SNS topic:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "arn:aws:sns:us-east-1:1111111111:spinnaker-shutdowns-pending"
    }
  ]
}
```
The Auto Scale Group (ASG) must be able to assume the afore mentioned role. In order to do this, we must add a Trust Relationship like this on the `myrole/spinnakerasg` role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "autoscaling.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```


## Sample IAM role for EC2 server running Shredder-for-EC2

The EC2 instance where you plan to deploy Shredder-for-EC2 needs have access to subscribe/unsubscribe to the SNS topic. It also needs access to the SQS queue under the desired prefix. Furthermore, the EC2 instance needs to be able to send heart beats to the Auto Scaling Group (ASG).

Therefore, the IAM role used for the EC2 instance, needs to have these permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "sqs:*"
      ],
      "Resource": [
        "arn:aws:sqs:*:1111111111:*:*"
      ],
      "Effect": "Allow"
    },
    {
      "Action": [
        "sns:Unsubscribe"
      ],
      "Resource": [
        "*"
      ],
      "Effect": "Allow"
    },
    {
      "Action": [
        "sns:Subscribe"
      ],
      "Resource": [
        "arn:aws:sns:us-east-1:1111111111:*"
      ],
      "Effect": "Allow"
    },
    {
      "Action": [
        "autoscaling:RecordLifecycleActionHeartbeat",
        "autoscaling:CompleteLifecycleAction"
      ],
      "Resource": "arn:aws:autoscaling:*:*:*",
      "Effect": "Allow"
    }
  ]
}
```

# Shredder for EC2 features

## Automatic variable injection
shredder automatically injects a set of useful environment variables when running the shutdown scripts: `$AWS_REGION`, `$AWS_ACCOUNT_ID`, `$AWS_INSTANCE_ID`, `$HOSTNAME`, `$ENVIRONMENT`.

## Sends heartbeats to the AWS Auto Scale group 
Even if a command takes 1 hour to run, the daemon will periodically send heartbeats to the ASG so that it keeps the EC2 instance alive

# Additional cleanup on remote services

After the EC2 instance is terminated, it is sometimes useful to notify another system about this event. For instance, you might have another service where you need to do some DNS/monitoring cleanup for the EC2 node that was terminated. If available, you could just do an HTTP call to such service, directly from the Shredder for EC2 (e.g. `https://my-remote-monitoring-service.com/remove/asg-ec2-i3`).

However, if such an API is not available, you might want to run a daemon on the remote service itself. This daemon would listen for EC2 instance termination complete messages, and will perform the cleanup when such a message is received. This can be achieved by writing your own custom application based on the shredder-core module, which reads SNS notifications from a second topic (e.g. spinnaker-shutdown-complete). 

The Shredder-for-EC2 can be instrumented to send a notification to a different SNS topic, when the cleanup is completed. You can just add a shutdown script in /opt/shutdown-scripts, which will be executed by shredder:

```bash
$ cat /opt/shutdown-scripts/notify-external.sh

target_sns_topic="arn:aws:sns:$AWS_REGION:$AWS_ACCOUNT_ID:spinnaker-shutdowns-complete"
aws sns publish --subject 'Shutdown complete' --message '{\"hostname\": \"$HOSTNAME\", \"app\": \"myapp\"}' --target-arn $target_sns_topic --region $AWS_REGION
```

# Build

To build this project:

```sh
$ git clone git@github.com:adobe/shredder.git
$ cd shredder

# Build RPM for RedHat/Centos
$ ./gradlew clean build shredder-ec2:buildRpm

# find . -name "*rpm"
./shredder-ec2/build/distributions/aam-shredder-ec2-1.0.0-20180612235633.noarch.rpm

# Build Debian
$ ./gradlew clean build shredder-ec2:buildDeb

# find . -name "*deb"
./shredder-ec2/build/distributions/aam-shredder-ec2_1.0.0-20180612235918_noarch.deb
```

# Open in IntelliJ

```sh
$ git clone git@github.com:adobe/shredder.git
$ cd shredder
$ idea .
```

## Run from IntelliJ

To run shredder-ec2 locally, you can pass environment variables to bypass AWS resource lookups.
```
AWS_PROFILE=aam-npe
SHREDDER_CONFIG_FILE=shredder-ec2/src/main/resources/reference.conf
region=us-east-1
instanceId=i-0a3806d7164d3de2f
accountId=<enter-aam-npe-account-id>
```

# Other use cases
The shredder-core module can be used for implementing other use cases. See the sample-cleanup sample application. 

# Credits

Project is based on https://github.com/scopely/shudder, which is written in python.

# Bugs and Feedback

For bugs, questions and discussions please use the [GitHub Issues](https://github.com/adobe/shredder/issues).


# LICENSE

Copyright 2018 Adobe Systems Incorporated

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
