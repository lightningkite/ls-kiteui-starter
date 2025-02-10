package com.lightningkite.template

import com.lightningkite.lightningserver.aws.AwsAdapter
import com.lightningkite.lightningserver.aws.prepareModelsServerAws
import com.lightningkite.lightningserver.db.DynamoDbCache

class AwsHandler : AwsAdapter() {
    init {
        Server
        DynamoDbCache
        prepareModelsServerAws()
        preventLambdaTimeoutReuse = true
        loadSettings()
    }
}