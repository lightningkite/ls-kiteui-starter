// by Claude — End-to-end integration tests: real server permissions + KiteUI frontend
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.createFileReferenceFromBytes
import com.lightningkite.lskiteuistarter.data.InventoryItemEndpoints
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.insertOne
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InventoryIntegrationTest {

    @Test
    fun authenticatedUserSeesInventoryPage() = integrationTest(
        initialPage = InventoryPage(),
        setup = { runtime ->
            with(runtime) {
                val (user) = seedTestOrg()
                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("inventoryTitle")
        assertTextVisible("Inventory")
    }

    @Test
    fun inventoryListShowsItemsFromServer() = integrationTest(
        initialPage = InventoryPage(),
        setup = { runtime ->
            with(runtime) {
                val (user, org) = seedTestOrg()

                InventoryItemEndpoints.info.table().insertOne(
                    InventoryItem(organization = org._id, name = "Integration Widget", category = ItemCategory.Electronics, quantity = 7)
                )
                InventoryItemEndpoints.info.table().insertOne(
                    InventoryItem(organization = org._id, name = "Integration Gadget", category = ItemCategory.Office, quantity = 3)
                )

                loginAs(user)
            }
        },
    ) { _ ->
        waitForId("inventoryList")
        pollForTexts("Integration Widget", "Integration Gadget")
    }

    @Test
    fun createItemThroughFormWithRealPermissions() = integrationTest(
        initialPage = InventoryPage(),
        setup = { runtime ->
            with(runtime) {
                val testOrg = seedTestOrg()
                loginAs(testOrg.user)
            }
        },
    ) { runtime ->
        waitForId("addItemButton")
        click("addItemButton")

        waitForId("nameInput")
        // yield() lets queued bind-setup coroutines run: inside runBlocking(Unconfined), launch{}
        // with DEFAULT start queues coroutines rather than running them immediately. Without
        // this yield, setValue fires invokeAllListeners() before the bind listener is registered.
        yield()
        setValue("nameInput", "Real Server Widget")
        setValue("categorySelect", "Electronics")
        setValue("notesInput", "Created via integration test")
        click("saveButton")

        waitForId("inventoryTitle")

        val items = with(runtime) {
            InventoryItemEndpoints.info.table()
                .find(Condition.Always)
                .toList()
        }
        assertTrue(items.any { it.name == "Real Server Widget" }, "Item should exist in real database")
        val created = items.first { it.name == "Real Server Widget" }
        assertEquals(ItemCategory.Electronics, created.category)
        assertEquals("Created via integration test", created.notes)
    }

    @Test
    fun orgScopingPreventsOtherOrgItems() = integrationTest(
        initialPage = InventoryPage(),
        setup = { runtime ->
            with(runtime) {
                val orgA = seedTestOrg(email = "alice@test.com", orgName = "Org A")
                val orgB = seedTestOrg(email = "bob@test.com", orgName = "Org B")

                InventoryItemEndpoints.info.table().insertOne(
                    InventoryItem(organization = orgA.org._id, name = "Org A Widget")
                )
                InventoryItemEndpoints.info.table().insertOne(
                    InventoryItem(organization = orgB.org._id, name = "Org B Gadget")
                )

                loginAs(orgB.user)
            }
        },
    ) { _ ->
        waitForId("inventoryList")
        waitFor { snapshot().contains("Org B Gadget") }

        val snap = snapshot()
        assertTrue(!snap.contains("Org A Widget"), "User B should not see items from Org A")
    }

    // by Claude — Full end-to-end flow: view inventory → edit item → verify update in UI and database
    @Test
    fun viewEditAndVerifyFullFlow() = integrationTest(
        initialPage = InventoryPage(),
        setup = { runtime ->
            with(runtime) {
                val (user, org) = seedTestOrg()
                InventoryItemEndpoints.info.table().insertOne(
                    InventoryItem(
                        organization = org._id,
                        name = "Original Widget",
                        category = ItemCategory.Electronics,
                        quantity = 5,
                        notes = "Before edit",
                    )
                )
                loginAs(user)
            }
        },
    ) { runtime ->
        waitForId("inventoryList")
        waitFor { snapshot().contains("Original Widget") }

        val items = with(runtime) {
            InventoryItemEndpoints.info.table().find(Condition.Always).toList()
        }
        val item = items.first { it.name == "Original Widget" }
        navigate("inventory/${item._id}/edit")

        waitFor { snapshot().contains("Edit: Original Widget") }
        waitFor(description = "name input populated") {
            snapshot("nameInput").contains("= \"Original Widget\"")
        }

        setValue("nameInput", "Updated Widget")
        setValue("quantityInput", "42")
        setValue("notesInput", "After edit")
        click("saveButton")

        waitForId("inventoryTitle")
        waitFor { snapshot().contains("Updated Widget") }

        val updated = with(runtime) {
            InventoryItemEndpoints.info.table().find(Condition.Always).toList()
        }
        val updatedItem = updated.first { it._id == item._id }
        assertEquals("Updated Widget", updatedItem.name)
        assertEquals(42, updatedItem.quantity)
        assertEquals("After edit", updatedItem.notes)
    }

    @Test
    fun deleteItemThroughEditPage() = integrationTest(
        initialPage = InventoryPage(),
        setup = { runtime ->
            with(runtime) {
                val (user, org) = seedTestOrg()
                InventoryItemEndpoints.info.table().insertOne(
                    InventoryItem(organization = org._id, name = "Doomed Widget")
                )
                loginAs(user)
            }
        },
    ) { runtime ->
        waitForId("inventoryList")
        waitFor { snapshot().contains("Doomed Widget") }

        val items = with(runtime) {
            InventoryItemEndpoints.info.table().find(Condition.Always).toList()
        }
        val item = items.first { it.name == "Doomed Widget" }
        navigate("inventory/${item._id}/edit")

        waitFor { snapshot().contains("Edit: Doomed Widget") }
        click("deleteButton")

        waitForId("inventoryTitle")

        val remaining = with(runtime) {
            InventoryItemEndpoints.info.table().find(Condition.Always).toList()
        }
        assertTrue(remaining.none { it._id == item._id }, "Item should be deleted from database")
    }

    @Test
    fun unauthenticatedUserRedirectedFromInventory() = integrationTest(
        initialPage = InventoryPage(),
    ) { _ ->
        waitFor { snapshot().contains("Sign in to get started") }
    }

    /**
     * Demonstrates AI driver external-service mocking: verifies that clicking the photo
     * upload button invokes the file picker with the correct MIME filter.
     *
     * [MockExternalServices.pendingFileResponses] queues a fake [FileReference] so the
     * upload button receives a file without opening any OS dialog.  After the click,
     * [MockExternalServices.calls] records that [MockExternalServices.Call.RequestFile]
     * was made — this is the assertion pattern for testing native OS interactions.
     */
    @Test
    fun photoUploadButtonCallsFilePicker() {
        val mock = MockExternalServices()
        // Queue a fake image so requestFile() returns it instead of opening an OS dialog
        mock.pendingFileResponses.add(
            createFileReferenceFromBytes(
                bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), // PNG magic bytes
                mimeType = "image/png",
                fileName = "test.png",
            )
        )

        integrationTest(
            initialPage = InventoryPage(),
            mocks = mock,
            setup = { runtime ->
                with(runtime) {
                    val (user, org) = seedTestOrg()
                    InventoryItemEndpoints.info.table().insertOne(
                        InventoryItem(organization = org._id, name = "Photo Test Widget")
                    )
                    loginAs(user)
                }
            },
        ) { runtime ->
            val items = with(runtime) {
                InventoryItemEndpoints.info.table().find(Condition.Always).toList()
            }
            val item = items.first { it.name == "Photo Test Widget" }
            navigate("inventory/${item._id}/edit")

            waitForId("uploadPhotoButton")
            click("uploadPhotoButton")

            // The key assertion: file picker was invoked with image/* MIME filter
            assertTrue(
                mock.calls.any { it is MockExternalServices.Call.RequestFile },
                "Expected RequestFile call but mock recorded: ${mock.calls}"
            )
            val requestCall = mock.calls.filterIsInstance<MockExternalServices.Call.RequestFile>().first()
            assertTrue(
                requestCall.mimeTypes.contains("image/*"),
                "Expected image/* MIME filter but got: ${requestCall.mimeTypes}"
            )
        }
    }
}
