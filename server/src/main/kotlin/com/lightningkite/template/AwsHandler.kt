package com.lightningkite.template

import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.definition.builder.*
import com.lightningkite.lightningserver.deprecations.*
import com.lightningkite.lightningserver.encryption.*
import com.lightningkite.lightningserver.engine.awsserverless.AwsAdapter
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.pathing.*
import com.lightningkite.lightningserver.runtime.*
import com.lightningkite.lightningserver.serialization.*
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.lightningserver.settings.*
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningserver.websockets.*
import com.lightningkite.services.cache.*
import com.lightningkite.services.cache.dynamodb.DynamoDbCache
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.email.*
import com.lightningkite.services.files.*
import com.lightningkite.services.files.s3.S3PublicFileSystem
import com.lightningkite.services.notifications.*
import com.lightningkite.services.sms.*
import kotlinx.coroutines.runBlocking
import kotlin.uuid.Uuid

/**
 * Entry point for AWS Lambda.
 */
class AwsHandler(definition: ServerDefinition) : AwsAdapter(definition) {
    init {
        S3PublicFileSystem
        DynamoDbCache
        loadSettings()
    }
}
