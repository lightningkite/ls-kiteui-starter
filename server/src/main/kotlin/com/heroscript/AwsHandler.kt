package com.heroscript

import com.lightningkite.lightningserver.engine.awsserverless.AwsAdapter

/**
 * Entry point for AWS Lambda.
 */
class AwsHandler() : AwsAdapter(Server.build())
