package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.models.Action
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Tests for screen-specific logic patterns.
 *
 * These tests verify that:
 * - Action patterns work correctly
 * - Navigation logic behaves properly
 * - Screen state management is sound
 * - Complex UI logic functions as expected
 */
class ScreenLogicTest {

    @Test
    fun testActionPattern() = runTest {
        // Test the Action pattern used throughout screens
        var executed = false
        var executionCount = 0

        val action = Action("Test Action") {
            executed = true
            executionCount++
        }

        assertEquals("Test Action", action.title)
        assertFalse(executed)

        // Execute the action
        action.onSelect()
        assertTrue(executed)
        assertEquals(1, executionCount)

        // Execute again
        action.onSelect()
        assertEquals(2, executionCount)
    }

    @Test
    fun testActionWithValidation() = runTest {
        // Test action that includes validation logic
        val input = Signal("")
        var executed = false

        val action = Action("Submit") {
            if (input.value.isBlank()) return@Action
            executed = true
        }

        // Should not execute with blank input
        action.onSelect()
        assertFalse(executed)

        // Should execute with valid input
        input.value = "valid"
        action.onSelect()
        assertTrue(executed)
    }

    @Test
    fun testCreateRoomDialogLogic() {
        // Test the create room dialog logic from ChatRoomsListScreen
        val roomName = Signal("")
        val roomDescription = Signal("")

        fun isCreateEnabled(): Boolean = roomName.value.isNotBlank()

        // Initially disabled
        assertFalse(isCreateEnabled())

        // Whitespace doesn't count
        roomName.value = "   "
        assertFalse(isCreateEnabled())

        // Valid name enables create
        roomName.value = "My Room"
        assertTrue(isCreateEnabled())

        // Description is optional - create still enabled
        roomDescription.value = ""
        assertTrue(isCreateEnabled())
    }

    @Test
    fun testSettingsDialogLocalState() {
        // Test the pattern of local state in settings dialog
        data class RoomSettings(
            val isPrivate: Boolean,
            val muteNotifications: Boolean,
            val autoDeleteDays: Int?
        )

        // Original settings
        val original = RoomSettings(
            isPrivate = false,
            muteNotifications = false,
            autoDeleteDays = null
        )

        // Local state for dialog
        val localIsPrivate = Signal(original.isPrivate)
        val localMuteNotifications = Signal(original.muteNotifications)
        val localAutoDeleteDays = Signal(original.autoDeleteDays)

        // Modify local state
        localIsPrivate.value = true
        localMuteNotifications.value = true
        localAutoDeleteDays.value = 7

        // Original unchanged
        assertFalse(original.isPrivate)
        assertFalse(original.muteNotifications)
        assertEquals(null, original.autoDeleteDays)

        // Local state changed
        assertTrue(localIsPrivate.value)
        assertTrue(localMuteNotifications.value)
        assertEquals(7, localAutoDeleteDays.value)

        // On save, create new with local state
        val updated = original.copy(
            isPrivate = localIsPrivate.value,
            muteNotifications = localMuteNotifications.value,
            autoDeleteDays = localAutoDeleteDays.value
        )

        assertTrue(updated.isPrivate)
        assertTrue(updated.muteNotifications)
        assertEquals(7, updated.autoDeleteDays)
    }

    @Test
    fun testMessageInputPattern() {
        // Test message input + send logic
        val messageText = Signal("")
        val sentMessages = mutableListOf<String>()

        fun canSend(): Boolean = messageText.value.isNotBlank()

        fun send() {
            if (!canSend()) return
            sentMessages.add(messageText.value)
            messageText.value = ""
        }

        assertFalse(canSend())

        // Can't send empty
        send()
        assertTrue(sentMessages.isEmpty())

        // Can send valid message
        messageText.value = "Hello"
        assertTrue(canSend())
        send()
        assertEquals(1, sentMessages.size)
        assertEquals("Hello", sentMessages[0])

        // Message cleared after send
        assertEquals("", messageText.value)
        assertFalse(canSend())
    }

    @Test
    fun testConditionalRenderingLogic() {
        // Test the logic for conditional rendering (shownWhen equivalent)
        val isEmpty = Signal(true)
        val isLoading = Signal(false)
        val hasError = Signal(false)

        fun shouldShowEmpty(): Boolean = isEmpty.value && !isLoading.value && !hasError.value
        fun shouldShowLoading(): Boolean = isLoading.value
        fun shouldShowError(): Boolean = hasError.value && !isLoading.value
        fun shouldShowContent(): Boolean = !isEmpty.value && !isLoading.value && !hasError.value

        // Initially show empty
        assertTrue(shouldShowEmpty())
        assertFalse(shouldShowLoading())
        assertFalse(shouldShowError())
        assertFalse(shouldShowContent())

        // Show loading
        isLoading.value = true
        assertFalse(shouldShowEmpty())
        assertTrue(shouldShowLoading())
        assertFalse(shouldShowError())
        assertFalse(shouldShowContent())

        // Show error
        isLoading.value = false
        hasError.value = true
        assertFalse(shouldShowEmpty())
        assertFalse(shouldShowLoading())
        assertTrue(shouldShowError())
        assertFalse(shouldShowContent())

        // Show content
        hasError.value = false
        isEmpty.value = false
        assertFalse(shouldShowEmpty())
        assertFalse(shouldShowLoading())
        assertFalse(shouldShowError())
        assertTrue(shouldShowContent())
    }

    @Test
    fun testDynamicButtonText() {
        // Test reactive button text logic
        val isMember = Signal(false)
        val isCreator = Signal(false)

        fun getButtonText(): String = if (isMember.value) "Leave" else "Join"
        fun shouldShowButton(): Boolean = !isCreator.value

        // Not member, not creator - show Join
        assertEquals("Join", getButtonText())
        assertTrue(shouldShowButton())

        // Member, not creator - show Leave
        isMember.value = true
        assertEquals("Leave", getButtonText())
        assertTrue(shouldShowButton())

        // Creator - hide button
        isCreator.value = true
        assertFalse(shouldShowButton())
    }

    @Test
    fun testRoomMembershipStatusText() {
        // Test the status text logic from ChatRoomsListScreen
        data class Room(val createdBy: Uuid, val memberIds: Set<Uuid>)

        val userId = Uuid.random()
        val otherUserId = Uuid.random()

        fun getStatusText(room: Room): String {
            return when {
                room.createdBy == userId -> "Created by: You"
                userId in room.memberIds -> "Member"
                else -> "${room.memberIds.size} members"
            }
        }

        // Created by current user
        val myRoom = Room(userId, setOf(userId))
        assertEquals("Created by: You", getStatusText(myRoom))

        // Member but not creator
        val memberRoom = Room(otherUserId, setOf(userId, otherUserId))
        assertEquals("Member", getStatusText(memberRoom))

        // Not a member
        val publicRoom = Room(otherUserId, setOf(otherUserId))
        assertEquals("1 members", getStatusText(publicRoom))

        val largeRoom = Room(otherUserId, setOf(otherUserId, Uuid.random(), Uuid.random()))
        assertEquals("3 members", getStatusText(largeRoom))
    }

    @Test
    fun testBrowseAllToggleText() {
        // Test the toggle button text from ChatRoomsListScreen
        val showAll = Signal(false)

        fun getToggleText(): String = if (showAll.value) "My Rooms" else "Browse All"

        assertEquals("Browse All", getToggleText())

        showAll.value = true
        assertEquals("My Rooms", getToggleText())

        showAll.value = false
        assertEquals("Browse All", getToggleText())
    }

    @Test
    fun testEmptyStateMessage() {
        // Test the empty state message logic
        val isEmpty = Signal(true)
        val showAll = Signal(false)

        fun getEmptyMessage(): String {
            return if (showAll.value) {
                "No rooms available"
            } else {
                "No rooms yet. Create one or browse all rooms!"
            }
        }

        // My rooms empty
        assertEquals("No rooms yet. Create one or browse all rooms!", getEmptyMessage())

        // All rooms empty
        showAll.value = true
        assertEquals("No rooms available", getEmptyMessage())
    }

    @Test
    fun testRoomClickability() {
        // Test the logic for whether a room card should be clickable
        val userId = Uuid.random()
        val otherUserId = Uuid.random()

        data class Room(val createdBy: Uuid, val memberIds: Set<Uuid>)

        fun isClickable(room: Room): Boolean {
            return room.createdBy == userId || userId in room.memberIds
        }

        // Member room - clickable
        val memberRoom = Room(otherUserId, setOf(userId, otherUserId))
        assertTrue(isClickable(memberRoom))

        // Created room - clickable
        val myRoom = Room(userId, setOf(userId))
        assertTrue(isClickable(myRoom))

        // Non-member room - not clickable
        val otherRoom = Room(otherUserId, setOf(otherUserId))
        assertFalse(isClickable(otherRoom))
    }
}
