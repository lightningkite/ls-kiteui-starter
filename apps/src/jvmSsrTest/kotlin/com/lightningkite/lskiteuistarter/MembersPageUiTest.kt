// by Claude — UI tests for MembersPage rendering
package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.testing.*
import kotlin.test.Test

class MembersPageUiTest {

    @Test
    fun titleRenders() = authenticatedUiTest(
        mockApi = defaultMockApi(),
        renderContent = { with(MembersPage()) { render() } },
    ) {
        waitForComponent("membersTitle")
        assertVisible("membersTitle")
    }

    @Test
    fun listRendersWithData() = authenticatedUiTest(
        mockApi = defaultMockApi(memberships = testMemberships),
        renderContent = { with(MembersPage()) { render() } },
    ) {
        waitForComponent("membersList")
        assertVisible("membersList")
    }

    @Test
    fun emptyListRenders() = authenticatedUiTest(
        mockApi = defaultMockApi(memberships = emptyList()),
        renderContent = { with(MembersPage()) { render() } },
    ) {
        waitForComponent("membersList")
        assertVisible("membersList")
    }
}
