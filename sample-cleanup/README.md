# Sample app using shredder-core

This is a sample application, which listens for an SNS trigger and takes some actions (eg. runs some scripts).

Here are a few use cases which can be implemented:
1. Detect when an EC2 node has been decommissioned, and remove it from 3rd party services (eg. Route53, Puppet)
2. Send an SNS notification from an EMR job and have an application that can run a series of commands/scripts when that happens.
3. A Cassandra node has been decommissioned and each node runs a sidecar that listens for this event. 
4. Other 


# To run
Pass the SHREDDER_CONFIG_FILE environment variable.
