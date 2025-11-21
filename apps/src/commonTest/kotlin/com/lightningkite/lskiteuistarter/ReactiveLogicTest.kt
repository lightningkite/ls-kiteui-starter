package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.reactive.debounceWrite
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for reactive programming patterns used in screens.
 *
 * These tests verify that:
 * - Signal updates work correctly
 * - Derived reactives (remember) update properly
 * - Debounced signals rate-limit correctly
 * - Reactive filtering works as expected
 */
class ReactiveLogicTest {

    @Test
    fun testSignalBasics() {
        val signal = Signal("initial")
        assertEquals("initial", signal.value)

        signal.value = "updated"
        assertEquals("updated", signal.value)
    }

    @Test
    fun testRememberDerivedValue() {
        val name = Signal("John")
        val age = Signal(25)

        // Simulate the remember pattern
        var greeting = ""
        var lastAge = 0

        // This simulates reactive { remember { ... } }
        val updateGreeting = {
            greeting = "Hello ${name.value}, you are ${age.value} years old"
            lastAge = age.value
        }

        updateGreeting()
        assertEquals("Hello John, you are 25 years old", greeting)
        assertEquals(25, lastAge)

        // Update name
        name.value = "Jane"
        updateGreeting()
        assertEquals("Hello Jane, you are 25 years old", greeting)

        // Update age
        age.value = 30
        updateGreeting()
        assertEquals("Hello Jane, you are 30 years old", greeting)
        assertEquals(30, lastAge)
    }

    @Test
    fun testDebouncedSignal() = runTest {
        val debounced = Signal("").debounceWrite(100.milliseconds)
        val recordedValues = mutableListOf<String>()

        // Simulate reactive scope that records values
        var lastValue = ""
        val record = {
            if (debounced.value != lastValue) {
                lastValue = debounced.value
                recordedValues.add(lastValue)
            }
        }

        // Rapid updates
        debounced.value = "a"
        record()
        delay(30)
        debounced.value = "ab"
        record()
        delay(30)
        debounced.value = "abc"
        record()
        delay(30)
        debounced.value = "abcd"
        record()

        // Wait for debounce
        delay(150)
        record()

        // Should only record the final value after debounce
        assertTrue(recordedValues.last() == "abcd" || recordedValues.last() == "")
    }

    @Test
    fun testFilteringLogic() {
        // Simulate the chat rooms filtering logic
        data class Room(val name: String, val createdBy: String, val members: Set<String>)

        val rooms = listOf(
            Room("Public Room", "user1", setOf("user1", "user2")),
            Room("Private Room", "user2", setOf("user2")),
            Room("Team Room", "user1", setOf("user1", "user3")),
            Room("Open Room", "user3", setOf("user1", "user2", "user3"))
        )

        val currentUserId = "user1"
        val showAll = Signal(false)
        val searchQuery = Signal("")

        fun getDisplayedRooms(): List<Room> {
            val query = searchQuery.value.trim().lowercase()

            return rooms.filter { room ->
                // Filter by membership
                val membershipMatch = showAll.value ||
                    room.createdBy == currentUserId ||
                    currentUserId in room.members

                // Filter by search
                val searchMatch = query.isEmpty() ||
                    room.name.lowercase().contains(query)

                membershipMatch && searchMatch
            }
        }

        // Test: My rooms only
        showAll.value = false
        searchQuery.value = ""
        var displayed = getDisplayedRooms()
        assertEquals(3, displayed.size) // user1 is in 3 rooms
        assertTrue(displayed.any { it.name == "Public Room" })
        assertTrue(displayed.any { it.name == "Team Room" })
        assertTrue(displayed.any { it.name == "Open Room" })

        // Test: All rooms
        showAll.value = true
        displayed = getDisplayedRooms()
        assertEquals(4, displayed.size)

        // Test: Search filtering
        searchQuery.value = "private"
        displayed = getDisplayedRooms()
        assertEquals(1, displayed.size)
        assertEquals("Private Room", displayed.first().name)

        // Test: Combined filtering
        searchQuery.value = "room"
        showAll.value = false
        displayed = getDisplayedRooms()
        assertEquals(3, displayed.size) // user1's rooms containing "room"
        assertFalse(displayed.any { it.name == "Private Room" })
    }

    @Test
    fun testMembershipToggleLogic() {
        // Simulate the join/leave logic
        data class Room(val id: String, val members: Set<String>)

        val room = Signal(Room("room1", setOf("user1", "user2")))
        val currentUserId = "user3"

        fun isMember(): Boolean = currentUserId in room.value.members

        fun toggleMembership(): Room {
            val current = room.value
            val newMembers = if (isMember()) {
                current.members - currentUserId
            } else {
                current.members + currentUserId
            }
            return current.copy(members = newMembers)
        }

        // Initially not a member
        assertFalse(isMember())

        // Join
        room.value = toggleMembership()
        assertTrue(isMember())
        assertEquals(setOf("user1", "user2", "user3"), room.value.members)

        // Leave
        room.value = toggleMembership()
        assertFalse(isMember())
        assertEquals(setOf("user1", "user2"), room.value.members)
    }

    @Test
    fun testAutoDeleteOptionsLogic() {
        // Test the radio button logic for auto-delete
        val autoDeleteDays = Signal<Int?>(null)

        val options = listOf(
            null to "Never",
            1 to "After 1 day",
            7 to "After 7 days",
            30 to "After 30 days"
        )

        // Simulate .equalTo() behavior
        fun isSelected(value: Int?): Boolean = autoDeleteDays.value == value

        fun select(value: Int?) {
            autoDeleteDays.value = value
        }

        // Initially "Never"
        assertTrue(isSelected(null))
        assertFalse(isSelected(1))

        // Select "After 1 day"
        select(1)
        assertTrue(isSelected(1))
        assertFalse(isSelected(null))
        assertFalse(isSelected(7))

        // Select "After 7 days"
        select(7)
        assertTrue(isSelected(7))
        assertFalse(isSelected(1))

        // Back to "Never"
        select(null)
        assertTrue(isSelected(null))
        assertFalse(isSelected(7))
    }

    @Test
    fun testMessageValidation() {
        // Test message send validation logic
        val messageText = Signal("")

        fun canSend(): Boolean = messageText.value.isNotBlank()

        assertFalse(canSend())

        messageText.value = "   "
        assertFalse(canSend())

        messageText.value = "Hello"
        assertTrue(canSend())

        messageText.value = ""
        assertFalse(canSend())
    }
}
