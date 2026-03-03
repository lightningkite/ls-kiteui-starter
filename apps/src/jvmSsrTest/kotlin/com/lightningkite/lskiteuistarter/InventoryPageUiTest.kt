// by Claude — UI tests for InventoryPage rendering
package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.testing.*
import kotlin.test.Test

class InventoryPageUiTest {

    @Test
    fun titleRenders() = authenticatedUiTest(
        mockApi = defaultMockApi(),
        renderContent = { with(InventoryPage()) { render() } },
    ) {
        waitForComponent("inventoryTitle")
        assertVisible("inventoryTitle")
    }

    @Test
    fun listRendersWithItems() = authenticatedUiTest(
        mockApi = defaultMockApi(inventoryItems = testInventoryItems),
        renderContent = { with(InventoryPage()) { render() } },
    ) {
        waitForComponent("inventoryList")
        assertVisible("inventoryList")
    }

    @Test
    fun emptyListRenders() = authenticatedUiTest(
        mockApi = defaultMockApi(inventoryItems = emptyList()),
        renderContent = { with(InventoryPage()) { render() } },
    ) {
        waitForComponent("inventoryList")
        assertVisible("inventoryList")
    }
}
