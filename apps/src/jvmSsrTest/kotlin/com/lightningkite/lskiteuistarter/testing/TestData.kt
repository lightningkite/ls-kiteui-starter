// by Claude — Shared test data for UI tests
package com.lightningkite.lskiteuistarter.testing

import com.lightningkite.toEmailAddress
import com.lightningkite.lskiteuistarter.*
import kotlin.uuid.Uuid

val testUserId = Uuid.parse("00000000-0000-0000-0000-000000000001")
val testOrgId = Uuid.parse("00000000-0000-0000-0000-000000000010")

val testUser = User(
    _id = testUserId,
    email = "test@example.com".toEmailAddress(),
    name = "Test User",
    role = UserRole.Admin,
)

val testOrg = Organization(
    _id = testOrgId,
    name = "Test Organization",
)

val testMemberships = listOf(
    Membership(_id = Uuid.parse("00000000-0000-0000-0000-000000000100"), organization = testOrgId, user = testUserId, role = MemberRole.Owner),
    Membership(_id = Uuid.parse("00000000-0000-0000-0000-000000000101"), organization = testOrgId, user = Uuid.parse("00000000-0000-0000-0000-000000000002"), role = MemberRole.Member),
)

val testInventoryItems = listOf(
    InventoryItem(_id = Uuid.parse("00000000-0000-0000-0000-000000001001"), organization = testOrgId, name = "Laptop", category = ItemCategory.Electronics, quantity = 5),
    InventoryItem(_id = Uuid.parse("00000000-0000-0000-0000-000000001002"), organization = testOrgId, name = "Desk", category = ItemCategory.Furniture, quantity = 10),
    InventoryItem(_id = Uuid.parse("00000000-0000-0000-0000-000000001003"), organization = testOrgId, name = "Stapler", category = ItemCategory.Office, quantity = 25, notes = "Red Swingline"),
)

fun defaultMockApi(
    memberships: List<Membership> = testMemberships,
    inventoryItems: List<InventoryItem> = testInventoryItems,
) = MockApi(
    testUser = testUser,
    memberships = memberships,
    inventoryItems = inventoryItems,
    organizations = listOf(testOrg),
)
