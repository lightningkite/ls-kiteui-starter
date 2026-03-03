// by Claude — Generic in-memory mock for ClientModelRestEndpoints, used in UI tests
package com.lightningkite.lskiteuistarter.testing

import com.lightningkite.lightningserver.typed.ClientModelRestEndpoints
import com.lightningkite.services.database.*

class MockModelEndpoints<T : HasId<ID>, ID : Comparable<ID>>(
    items: List<T> = emptyList()
) : ClientModelRestEndpoints<T, ID> {

    val storage: MutableMap<ID, T> = items.associateBy { it._id }.toMutableMap()

    override suspend fun default(): T = throw NotImplementedError("default() not mocked")

    override suspend fun query(input: Query<T>): List<T> = storage.values.toList()

    override suspend fun queryPartial(input: QueryPartial<T>): List<Partial<T>> =
        throw NotImplementedError("queryPartial() not mocked")

    override suspend fun detail(id: ID): T =
        storage[id] ?: throw IllegalArgumentException("Not found: $id")

    override suspend fun insert(input: T): T {
        storage[input._id] = input
        return input
    }

    override suspend fun insertBulk(input: List<T>): List<T> {
        input.forEach { storage[it._id] = it }
        return input
    }

    override suspend fun upsert(id: ID, input: T): T {
        storage[id] = input
        return input
    }

    override suspend fun bulkReplace(input: List<T>): List<T> {
        input.forEach { storage[it._id] = it }
        return input
    }

    override suspend fun replace(id: ID, input: T): T {
        storage[id] = input
        return input
    }

    override suspend fun bulkModify(input: MassModification<T>): Int = 0

    override suspend fun modifyWithDiff(id: ID, input: Modification<T>): EntryChange<T> {
        val old = storage[id] ?: throw IllegalArgumentException("Not found: $id")
        return EntryChange(old, old) // no-op for mocks
    }

    override suspend fun modify(id: ID, input: Modification<T>): T =
        storage[id] ?: throw IllegalArgumentException("Not found: $id")

    override suspend fun bulkDelete(input: Condition<T>): Int = 0

    override suspend fun delete(id: ID) {
        storage.remove(id)
    }

    override suspend fun count(input: Condition<T>): Int = storage.size

    override suspend fun groupCount(input: GroupCountQuery<T>): Map<String, Int> = emptyMap()

    override suspend fun aggregate(input: AggregateQuery<T>): Double? = null

    override suspend fun groupAggregate(input: GroupAggregateQuery<T>): Map<String, Double?> = emptyMap()

    override suspend fun permissions(): ModelPermissions<T> =
        throw NotImplementedError("permissions() not mocked")
}
