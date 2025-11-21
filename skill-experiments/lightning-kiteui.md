---
name: KiteUI / Lightning Server Combo Development
description: This skill should be used when the user wants to work with KiteUI and Lightning Server together.
version: 1.0.0
---

# Lightning Server & KiteUI Development Skill

This skill provides guidance for building applications with Lightning Server (backend) and KiteUI (frontend).

## Architecture Overview

### Project Structure
```
project/
├── shared/      # Shared models between client/server (multiplatform)
├── server/      # JVM backend with Lightning Server
└── apps/        # Multiplatform UI (Android, iOS, Web)
```

### Development Workflow
1. Define models in `shared/src/commonMain/kotlin/.../models.kt`
2. Create server endpoints in `server/src/main/kotlin/.../data/*Endpoints.kt`
3. **Regenerate SDK**: `./gradlew :server:generateSdk`
4. Implement UI screens in `apps/src/commonMain/kotlin/.../`
5. Test: Server (`./gradlew :server:serve`) + Frontend (`./gradlew :apps:jsBrowserDevelopmentRun`)

## KiteUI Reactive Programming Model

**CRITICAL: KiteUI is NOT React. It does not use virtual DOM.**

### Core Principles
1. **Views are created once** - The DOM is generated up front
2. **Properties update reactively** - Content, visibility, etc. update automatically
3. **Never create views inside reactive scopes** - Only update properties
4. **Based on SolidJS** - Uses reactive scopes that re-run when dependencies change

### Correct Patterns

#### ✅ DO: Create views once, update properties reactively
```kotlin
// Create the view once
shownWhen { messages().isEmpty() }.centered.text("No messages")

shownWhen { messages().isNotEmpty() }.recyclerView {
    children(messages) { message ->
        text {
            ::content { message().content }  // Updates reactively
        }
    }
}
```

#### ❌ DON'T: Create views inside reactive scopes
```kotlin
// WRONG - creates new views on every update
reactiveScope {
    if (messages().isEmpty()) {
        text("No messages")  // BAD
    } else {
        recyclerView { ... }  // BAD
    }
}
```

### DSL Syntax

#### Operator Chaining - Use `.` NOT `-`
```kotlin
// ✅ CORRECT
expanding.frame { ... }
card.padded.col { ... }
important.buttonTheme.button { ... }
centered.text("Hello")

// ❌ WRONG (deprecated)
expanding - frame { ... }
card - padded - col { ... }
```

#### Icons

Two valid patterns for creating icons:

```kotlin
// ✅ Pattern 1: Direct icon with description
icon(Icon.add, "Add item")
icon(Icon.settings, "Settings")

// ✅ Pattern 2: DSL block with source property
icon {
    source = Icon.star
    description = "Favorite"
}

// ❌ WRONG - Missing source property
icon { Icon.add }  // This doesn't work!
```

**Common Icon Names:**
- `Icon.add`, `Icon.remove`
- `Icon.home`, `Icon.settings`
- `Icon.send`, `Icon.chat`
- `Icon.arrowBack`, `Icon.chevronRight`
- `Icon.checkCircle`, `Icon.cancel`

**Note:** Not all intuitive icon names exist (e.g., `Icon.edit` may not be available). If an icon doesn't exist:
- Use `text("Label")` as a button label instead
- Or use a similar icon like `Icon.settings`
- Check the Icon class definition for available icons

### Reactive Data Fetching

#### Using `remember {}`
```kotlin
// Fetch data reactively - re-runs when dependencies change
val rooms = remember {
    currentSession()?.chatRooms?.list(Query(Condition.Always))?.invoke() ?: emptyList()
}

// Use in view
text {
    ::content { "Total: ${rooms().size}" }
}
```

#### Using `rememberSuspending` for Async Operations
```kotlin
// Automatic loading state management!
val data: Reactive<List<Post>> = rememberSuspending {
    delay(1000)  // Simulated loading
    val response = fetch("https://api.example.com/posts")
    Json.decodeFromString<List<Post>>(response.text())
}

// Use like any other reactive - automatically shows loading while fetching
recyclerView {
    children(data, id = { it.id }) { post ->
        text { ::content { post().title } }
    }
}
```

#### Using `LateInitSignal` for Manual Loading States
```kotlin
val data = LateInitSignal<String>()

// Automatically shows loading indicator while unset
text { ::content { data() } }

// Load data when ready
button {
    text("Load")
    onClick { data.value = "Loaded content!" }
}

// Clear to show loading again
button {
    text("Clear")
    onClick { data.unset() }
}
```

#### Using `PersistentProperty` for State that Survives Refreshes
```kotlin
// Persisted in browser localStorage/device storage
val theme = PersistentProperty("app-theme", "light")
val sessionToken = PersistentProperty<String?>("sessionToken", null)

// Use like any Signal
switch { checked bind theme.lens(
    get = { it == "dark" },
    modify = { _, dark -> if (dark) "dark" else "light" }
)}
```

#### Using `.debounceWrite()` for Search/API Rate Limiting
```kotlin
val searchQuery = Signal("").debounceWrite(500.milliseconds)

// Only triggers 500ms after user stops typing
reactive {
    val query = searchQuery()
    if (query.isNotEmpty()) {
        performSearch(query)
    }
}

textInput {
    hint = "Search..."
    content bind searchQuery
}
```

#### Using `awaitNotNull()` when you want to wait for something to be present / you require it
```kotlin
val room = remember {
    currentSession.awaitNotNull().chatRooms.get(roomId).awaitNotNull()
}
```

#### Derived Reactive Values with `remember`

Create reactive values that depend on multiple signals:

```kotlin
val showAll = Signal(false)
val allItems = remember {
    currentSession()?.items?.list(Query(Condition.Always))?.invoke() ?: emptyList()
}

// This reactive value updates when either showAll or allItems changes
val displayedItems = remember {
    val items = allItems()
    val userId = currentSession()?.userId
    if (showAll()) {
        items
    } else {
        items.filter { it.ownerId == userId }
    }
}

// Use in view
recyclerView {
    children(displayedItems, id = { it._id }) { item ->
        // Renders when displayedItems changes
    }
}
```

### Conditional Rendering

Use `shownWhen` to control visibility:

```kotlin
// Show/hide based on condition
shownWhen { isLoading() }.activityIndicator()
shownWhen { !isLoading() && items().isEmpty() }.text("No items")
shownWhen { !isLoading() && items().isNotEmpty() }.recyclerView { ... }
```

#### Conditional Clickability with Multiple Views

Use multiple `shownWhen` to render different view types based on state:

```kotlin
recyclerView {
    children(items, id = { it._id }) { item ->
        val canAccess = remember { /* compute access */ }

        card.col {
            // Clickable version for authorized users
            shownWhen { canAccess() }.button {
                text { ::content { item().name } }
                onClick { navigate(item()._id) }
            }

            // Non-clickable version for unauthorized users
            shownWhen { !canAccess() }.col {
                text { ::content { item().name } }
                subtext("Access denied")
            }
        }
    }
}
```

### Reactive Content Binding

Use `::content { }` for reactive text updates:

```kotlin
text {
    ::content { user().name }
}

h2 {
    ::content { "Count: ${items().size}" }
}
```

### Background Tasks with `load {}`

Run coroutines tied to the view lifecycle:

```kotlin
col {
    val counter = Signal(0)

    // Runs while view is active, cancels when view is destroyed
    load {
        while(true) {
            delay(1000)
            counter.value++
        }
    }

    text { ::content { "Count: ${counter()}" } }
}
```

**Use cases:**
- Polling for updates
- Live counters/timers
- Background data synchronization

### Lists with `recyclerView`

**CRITICAL: RecyclerView must have external size constraints!**

RecyclerView uses lazy rendering and has no intrinsic size. It **must always** be in a position where its size is determined externally:

```kotlin
val items = remember { /* fetch items */ }

// ✅ CORRECT: Use expanding modifier
expanding.recyclerView {
    children(items, id = { it._id }) { item ->
        card.button {
            text {
                ::content { item().name }
            }
            onClick {
                handleClick(item()._id)
            }
        }
    }
}

// ✅ CORRECT: Put inside a frame
frame {
    recyclerView {
        children(items, id = { it._id }) { item ->
            // ...
        }
    }
}

// ✅ CORRECT: Inside expanding.frame (fills space and scrolls if needed)
expanding.frame {
    recyclerView {
        children(items, id = { it._id }) { item ->
            // ...
        }
    }
}

// ✅ CORRECT: With shownWhen (inherits parent's sizing)
shownWhen { items().isNotEmpty() }.expanding.recyclerView {
    children(items, id = { it._id }) { item ->
        // ...
    }
}

// ❌ WRONG: recyclerView without size constraints
recyclerView {  // ERROR: No size constraints!
    children(items, id = { it._id }) { /* ... */ }
}
```

**Why size constraints are required:**
RecyclerView doesn't know how many items to render until it knows how much space it has. Without external size constraints, it cannot determine its viewport size and will not render correctly.

**Key distinctions:**
- `expanding` = "take up available space" (like CSS `flex: 1` or Android `weight=1`)
- `frame` = "fixed container" (provides size context)
- `expanding.frame` = "take up available space AND scroll if content overflows"
- RecyclerView needs size constraints from either `expanding`, `frame`, or a parent that provides size

**About the `id` parameter:**
- ✅ **Recommended:** `children(items, id = { it._id })` - Provides stable element identity for efficient updates
- ⚠️ **Works but not recommended:** `children(items)` - Without id, KiteUI can't efficiently track element changes
- The `id` function should return a unique, stable identifier for each item

### Simple List Rendering with `forEach()`

For static or simple lists where performance isn't critical:

```kotlin
val items = Signal(listOf("Apple", "Banana", "Cherry"))

col {
    forEach(items) {
        text(it)  // Simple, no reactive reference needed
    }
}
```

**When to use `forEach()` vs `children()`:**
- ✅ Use `forEach()` for small, static lists (<100 items)
- ✅ Use `children()` for large, dynamic lists that update frequently
- ✅ Use `children()` when items need reactive references

### Actions for Async Operations

Use `Action` instead of `launch {}` for suspend functions:

```kotlin
// ✅ CORRECT - Define Action at top level
val sendMessage = Action("Send Message") {
    val session = currentSession() ?: return@Action
    session.api.message.insert(message)
}

button {
    text("Send")
    action = sendMessage
}

// ✅ CORRECT - Define Action in recyclerView renderer (outside button)
recyclerView {
    children(items, id = { it._id }) { item ->
        // Define Action here, inside renderer but outside button
        val toggleAction = Action("Toggle") {
            val current = item()
            api.item.update(current.copy(active = !current.active))
        }

        card.col {
            button {
                text { ::content { item().name } }
                action = toggleAction  // Reference Action defined above
            }
        }
    }
}

// OK, but not great
button {
    onClick {
        api.message.insert(message)
    }
}

// ❌ WRONG - Defining Action inside button scope in renderer
recyclerView {
    children(items) { item ->
        button {
            val action = Action("Do Thing") { ... }  // BAD - won't compile
            action = action
        }
    }
}

// ❌ WRONG
button {
    onClick {
        launch {
            api.message.insert(message)
        }
    }
}
```

### Text Fields

```kotlin
val name = Signal("")

textInput {
    content bind name
    hint = "Enter name"
    action = submitAction  // Triggered on Enter
}
```

### Form Inputs: Switches, Checkboxes, and Radio Buttons

#### Switches and Checkboxes

Both switches and checkboxes work the same way - bind to a Boolean Signal:

```kotlin
val isPrivate = Signal(false)
val muteNotifications = Signal(false)

// Switch
row {
    expanding.text("Private Mode")
    switch {
        checked bind isPrivate
    }
}

// Checkbox
row {
    checkbox {
        checked bind muteNotifications
    }
    space()
    text("Mute notifications")
}
```

#### Radio Buttons

Use the `.equalTo()` extension for clean radio button binding:

```kotlin
val selectedOption = Signal<Int?>(null)

val options = listOf(
    null to "Never",
    1 to "After 1 day",
    7 to "After 7 days"
)

options.forEach { (value, label) ->
    button {
        row {
            radioButton {
                // ✅ BEST: Use .equalTo() for automatic bidirectional binding
                checked bind selectedOption.equalTo(value)
            }
            space()
            text(label)
        }
        onClick {
            selectedOption.value = value
        }
    }
}
```

**How `.equalTo()` works:**
- Returns true when `selectedOption() == value`
- When set to true, sets `selectedOption.value = value`
- Cleaner than manual `remember { }.withWrite { }` pattern

#### Form State Management Pattern

When working with forms that save to the server, use local Signals and an Action:

```kotlin
// Local state for form editing
val localIsPrivate = Signal(false)
val localMuteNotifications = Signal(false)

// Initialize from server data when dialog opens
reactive {
    if (showDialog()) {
        localIsPrivate.value = room().isPrivate
        localMuteNotifications.value = room().muteNotifications
    }
}

// Save action
val saveSettings = Action("Save") {
    val s = currentSession() ?: return@Action
    s.api.room.update(room().copy(
        isPrivate = localIsPrivate.value,
        muteNotifications = localMuteNotifications.value
    ))
    showDialog.value = false
}

// In UI
switch { checked bind localIsPrivate }
checkbox { checked bind localMuteNotifications }
button {
    text("Save")
    action = saveSettings
}
```

**Why local Signals?** Property setters and binding callbacks cannot call suspend functions, so you can't directly call API methods from checkbox/switch setters. Use local state + Action instead.

## Lightning Server Backend Patterns

### Model Definition

```kotlin
@Serializable
@GenerateDataClassPaths
data class ChatRoom(
    override val _id: Uuid = Uuid.random(),
    val name: String,
    @Index @References(User::class) val createdBy: Uuid,
    val createdAt: Instant = Clock.System.now(),
) : HasId<Uuid>
```

**Important:**
- Use `kotlin.time.Clock` not `kotlinx.datetime.Clock`
- Use `kotlin.uuid.Uuid` for IDs
- `@GenerateDataClassPaths` generates type-safe query paths
- `@Index` for frequently queried fields
- `@References` for foreign keys

### Endpoint Definition

```kotlin
object ChatRoomEndpoints : ServerBuilder() {
    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val userId = auth.id
            ModelPermissions(
                create = Condition.Always,
                read = condition { it.createdBy eq userId },
                update = condition { it.createdBy eq userId },
                delete = condition { it.createdBy eq userId },
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)

    // Custom endpoints
    val customEndpoint = path.path("custom").post bind ApiHttpHandler(
        summary = "Custom Operation",
        auth = UserAuth.require(),
        implementation = { request: RequestType ->
            // Implementation
            responseValue
        }
    )
}
```

### Register in Server.kt

```kotlin
val chatRooms = path.path("chat-rooms") module ChatRoomEndpoints
val messages = path.path("messages") module MessageEndpoints
```

### Database Queries

```kotlin
// Simple query
info.table().get(id)

// Conditional query
info.table().query(
    condition = condition { it.userId eq userId },
    orderBy = listOf(SortPart(Paths.createdAt, false)),
    limit = 50
)

// Update
info.table().updateOneById(id, modification {
    it.field assign newValue
})

// Insert
info.table().insertOne(item)
```

### Advanced Endpoint Patterns

#### REST + WebSocket Live Updates

Combine REST endpoints with real-time updates:

```kotlin
val info = database.modelInfo(
    auth = UserAuth.require(),
    permissions = { ModelPermissions.allowAll<ChatRoom>() }
)

// Combines REST CRUD with WebSocket updates!
val rest = path.path("rest") include
    ModelRestEndpoints(info) + ModelRestUpdatesWebsocket(info)
```

Clients automatically receive updates when data changes.

#### Error Documentation with Examples

```kotlin
val calculator = path.path("calc").post bind ApiHttpHandler(
    summary = "Perform arithmetic",
    description = "Calculates result of two numbers",
    auth = noAuth,
    errorCases = listOf(
        LSError(http = 400, detail = "invalid-operation", message = "Must be +, -, *, /"),
        LSError(http = 400, detail = "division-by-zero", message = "Cannot divide by zero")
    ),
    examples = listOf(
        ApiHttpHandler.Example(
            input = CalculatorRequest(10.0, 5.0, "+"),
            output = CalculatorResponse(15.0, "10.0 + 5.0 = 15.0")
        )
    ),
    implementation = { input ->
        when (input.operation) {
            "+" -> CalculatorResponse(input.a + input.b, "...")
            "/" -> if (input.b == 0.0)
                throw BadRequestException("division-by-zero", "Cannot divide by zero")
                else CalculatorResponse(input.a / input.b, "...")
            else -> throw BadRequestException("invalid-operation", "...")
        }
    }
)
```

#### Database Modification DSL

```kotlin
posts.updateOne(condition { it._id eq id }, modification {
    it.updatedAt assign Clock.System.now()
    it.viewCount += 1  // Increment

    if (input.title != null) {
        it.title assign input.title
    }
    if (input.status == PostStatus.PUBLISHED) {
        it.publishedAt assign Clock.System.now()
    }
})
```

**Operators:**
- `it.field assign value` - Set value
- `it.field += 1` - Increment
- `it.field -= 1` - Decrement

#### Complex Query Conditions

```kotlin
// AND conditions
val condition = condition {
    (it.postId eq postId) and (it.isApproved eq true)
}

// Multiple filters
var condition: Condition<BlogPost> = condition { it.status eq PostStatus.PUBLISHED }
if (tags.isNotEmpty()) {
    tags.forEach { tag ->
        condition = condition and condition { it.tags.any { it.eq(tag) } }
    }
}

// With sorting and pagination
posts.find(
    condition = condition,
    orderBy = listOf(SortPart(BlogPost.path.createdAt, false)),
    limit = 20,
    skip = 0
).toList()
```

#### WebSocket Pub/Sub Pattern

```kotlin
// Define topic
val chatTopic = path.path("ws").path("chat-topic").topic(ChatMessage.serializer())

// HTTP endpoint to publish
val sendMessage = path.path("send").arg<String>("msg").get bind HttpHandler {
    chatTopic.send(ChatMessage(content = route.arg1))
    HttpResponse.plainText("Sent!")
}

// WebSocket to subscribe
val chatSocket = path.path("ws").path("chat") bind WebSocketHandler(
    willConnect = { Uuid.random().toString() },
    didConnect = {
        subscribe(chatTopic)  // Subscribe to topic
        send("Welcome!")
    },
    messageFromClient = { frame ->
        val msg = Json.decodeFromString(ChatMessage.serializer(), frame.content.toString())
        chatTopic.send(msg)  // Broadcast to all
    },
    topicHandlers = {
        chatTopic bind { message ->
            send(Json.encodeToString(ChatMessage.serializer(), message.value))
        }
    },
    disconnect = { println("Client disconnected: $currentState") }
)
```

#### Cache-Aside Pattern

```kotlin
val getExpensiveData = path.path("data").get bind ApiHttpHandler(
    auth = noAuth,
    implementation = { _: Unit ->
        val cacheKey = "expensive-data"

        // Try cache first
        cache().get(cacheKey, MyData.serializer()) ?: run {
            // Cache miss - compute and cache
            val data = computeExpensiveData()
            cache().set(cacheKey, data, MyData.serializer(), 5.minutes)
            data
        }
    }
)
```

### Security and Multi-Tenant Access Control

**CRITICAL PRINCIPLE: Never trust the client. All security must be enforced server-side.**

#### Permission-Based Security Architecture

Lightning Server enforces security through the `ModelPermissions` system. Permissions are checked at the database query level, not in custom endpoints.

```kotlin
val info = Server.database.modelInfo(
    auth = UserAuth.require(),
    permissions = {
        val userId = auth.id

        ModelPermissions(
            create = condition { it.ownerId eq userId },
            read = condition { it.ownerId eq userId },
            update = condition { it.ownerId eq userId },
            delete = condition { it.ownerId eq userId },
        )
    }
)
```

**Common Permission Patterns:**

```kotlin
// Public read, authenticated create
create = Condition.Always,
read = Condition.Always,

// Owner-only access
val isOwner = condition { it.userId eq auth.id }
create = isOwner,
read = isOwner,
update = isOwner,
delete = isOwner,

// Role-based access
val isAdmin = condition { it.role inside allowedRoles }
delete = isAdmin,

// Combined conditions
update = isOwner and isAdmin,
read = isOwner or isPublic,
```

#### AuthCacheKey Pattern for Derived Authentication Data

Use `AuthCacheKey` to cache expensive authentication-derived data like roles or memberships:

```kotlin
object RoleCache : AuthCacheKey<User, UserRole> {
    override val id: String = "role"
    override val serializer: KSerializer<UserRole> = kotlinx.serialization.serializer()
    override val expireAfter: Duration = 5.minutes

    context(_: ServerRuntime)
    override suspend fun calculate(input: Authentication<User>): UserRole =
        input.fetch().role ?: UserRole.NoOne

    context(_: ServerRuntime) suspend fun Authentication<User>.userRole() = get(RoleCache)
    context(_: ServerRuntime) suspend fun AuthAccess<User>.userRole() = auth.userRole()
}

// Register in UserAuth
override val precache: List<AuthCacheKey<User, *>> = listOf(RoleCache)
```

**Using cached data in permissions:**

```kotlin
permissions = {
    val userRole = with(UserAuth.RoleCache) { auth.userRole() }
    val isAdmin = userRole >= UserRole.Admin

    ModelPermissions(
        delete = if (isAdmin) Condition.Always else Condition.Never
    )
}
```

#### Multi-Tenant Security: Room Membership Example

For multi-tenant systems (chat rooms, organizations, teams), cache user memberships:

```kotlin
object RoomMembershipCache : AuthCacheKey<User, Set<Uuid>> {
    override val id: String = "membership"
    override val serializer: KSerializer<Set<Uuid>> = kotlinx.serialization.serializer()
    // Alternative: SetSerializer(Uuid.serializer()) - equally valid
    override val expireAfter: Duration = 5.minutes

    context(_: ServerRuntime)
    override suspend fun calculate(input: Authentication<User>): Set<Uuid> {
        // Access table through modelInfo for proper typing
        return Server.chatRooms.info.table().find(
            condition { it.memberIds.any { it eq input.id } }
        ).toList().map { it._id }.toSet()
    }

    context(_: ServerRuntime) suspend fun Authentication<User>.roomMemberships() = get(RoomMembershipCache)
    context(_: ServerRuntime) suspend fun AuthAccess<User>.roomMemberships() = auth.roomMemberships()
}
```

**Using room memberships in message permissions:**

```kotlin
val info = Server.database.modelInfo(
    auth = UserAuth.require(),
    permissions = {
        val userId = auth.id
        val userRoomIds = with(UserAuth.RoomMembershipCache) { auth.roomMemberships() }
        val isAuthor = condition<Message> { it.authorId eq userId }
        val inUserRoom = condition<Message> { it.chatRoomId inside userRoomIds }

        ModelPermissions(
            // Users can only create messages in rooms they're members of
            create = inUserRoom and isAuthor,
            // Users can only read messages from rooms they're in
            read = inUserRoom,
            // Only authors can update/delete, and only in rooms they're in
            update = isAuthor and inUserRoom,
            delete = isAuthor and inUserRoom,
        )
    }
)
```

#### Database Query Patterns for Set Membership

When working with `Set<T>` fields in conditions:

```kotlin
// Check if Set contains a value
condition { it.memberIds.any { it eq userId } }

// Check if value is in Set (inverse)
condition { userId inside it.memberIds }

// Check if Set contains any of multiple values
condition { it.tags.any { it inside allowedTags } }

// Runtime check (not in query)
if (userId in room.memberIds) { /* user is member */ }
```

#### Enforcing Field Values with postPermissionsForUser

Use `postPermissionsForUser` to enforce server-controlled field values:

```kotlin
val info = Server.database.modelInfo(
    auth = UserAuth.require(),
    permissions = {
        val userId = auth.id
        val userRoomIds = with(UserAuth.RoomMembershipCache) { auth.roomMemberships() }

        ModelPermissions(
            create = condition { it.chatRoomId inside userRoomIds } and
                     condition { it.authorId eq userId },
            // ...
        )
    },
    postPermissionsForUser = {
        // Force authorId and createdAt to be correct on create
        it.interceptCreate { model ->
            model.copy(
                authorId = auth.id,
                createdAt = Clock.System.now()
            )
        }
    }
)
```

**When to use postPermissionsForUser vs permissions:**
- **permissions**: Validate that data meets requirements
- **postPermissionsForUser**: Enforce/override field values to ensure correctness

#### Common Security Anti-Patterns

```kotlin
// ❌ DANGEROUS: Allows any authenticated user to access anything
ModelPermissions(
    read = Condition.Always,  // Anyone can read anything!
    create = Condition.Always // Anyone can create anything!
)

// ❌ BAD: Relying on "client should only request"
// Comment: "Client should only request messages from rooms they're in"
// Reality: Clients can't be trusted - server must enforce!

// ❌ WRONG: Custom endpoint doing validation that should be in permissions
val send = path.path("send").post bind ApiHttpHandler(
    auth = UserAuth.require(),
    implementation = { request: SendMessageRequest ->
        // Manually checking room membership - permissions should do this!
        val room = database().table<ChatRoom>().get(request.chatRoomId)
            ?: throw NotFoundException()
        if (auth.id !in room.memberIds)
            throw ForbiddenException("Not a member")

        // ...
    }
)

// ✅ CORRECT: Permissions enforce security, custom endpoint just for convenience
val info = modelInfo(
    permissions = {
        val userRoomIds = with(UserAuth.RoomMembershipCache) { auth.roomMemberships() }
        ModelPermissions(
            create = condition { it.chatRoomId inside userRoomIds }
        )
    }
)
// Custom endpoint becomes optional - permissions already enforce security!
```

#### Security Best Practices

1. **Design permissions first** - Think about access control before custom endpoints
2. **Question every `Condition.Always`** - It's rarely correct for multi-tenant data
3. **Use AuthCacheKey for derived data** - Roles, memberships, permissions lists
4. **Cache invalidation** - Invalidate cached memberships when they change (see TODO patterns)
5. **postPermissionsForUser for enforcement** - Use it to ensure server-controlled fields are correct
6. **Test security** - Write tests that verify unauthorized access is blocked
7. **Combine conditions correctly** - Use `and`, `or` to build complex access rules

#### Table Access Patterns

```kotlin
// ✅ Access through modelInfo for properly configured tables
Server.chatRooms.info.table()

// ❌ Direct database access doesn't include modelInfo configuration
Server.database().table<ChatRoom>()

// Convert query results to list
table.find(condition { ... }).toList()

// Update with modification DSL
table.updateOneById(id, modification {
    it.field assign newValue
})
```

#### Testing Endpoints

```kotlin
// TestHelper.kt
object TestHelper {
    inline fun testServer(action: context(TestRunner<Server>) Server.()->Unit) {
        Server.test(
            settings = { database set Database.Settings("ram") },
            action
        )
    }
}

// Test file
class ChatRoomEndpointsTest {
    @Test
    fun testCreateRoom() = runBlocking {
        TestHelper.testServer {
            val user = Server.users.info.table()
                .insertOne(User(email = "test@example.com".toEmailAddress()))!!

            val response = Server.chatRooms.rest.create.test(
                user = user,
                body = ChatRoom(name = "Test Room", createdBy = user._id)
            )

            assertEquals(HttpStatus.Created, response.status)
            val room = response.body?.parse(ChatRoom.serializer())
            assertEquals("Test Room", room?.name)
        }
    }

    @Test
    fun testUnauthorizedAccessBlocked() = runBlocking {
        TestHelper.testServer {
            val user1 = Server.users.info.table()
                .insertOne(User(email = "user1@example.com".toEmailAddress()))!!
            val user2 = Server.users.info.table()
                .insertOne(User(email = "user2@example.com".toEmailAddress()))!!

            // User1 creates a room
            val room = ChatRoom(name = "Private", createdBy = user1._id)
            Server.chatRooms.info.table().insertOne(room)

            // User2 should NOT be able to read it (if permissions are correct)
            val user2Rooms = Server.chatRooms.info.table().query(
                Query(condition { it.createdBy eq user2._id })
            )
            assertFalse(room._id in user2Rooms.map { it._id })
        }
    }
}
```

## Client SDK Usage

After running `./gradlew :server:generateSdk`:

### Session Management Pattern (Best Practice)

The recommended pattern for managing user sessions:

```kotlin
// PersistentProperty stores token across refreshes
val sessionToken = PersistentProperty<String?>("sessionToken", null)

// Reactive session that auto-refreshes when token changes
val currentSession: Reactive<UserSession?> = rememberSuspending {
    val token = sessionToken() ?: return@rememberSuspending null
    val authApi = api.withHeaderCalculator(api.userAuth.accessToken(token))

    try {
        val self = authApi.userAuth.getSelf()
        UserSession(api = authApi, userId = self._id)
    } catch (e: Exception) {
        sessionToken.value = null  // Clear invalid token
        null
    }
}

// Redirect if logged out
reactive {
    if (currentSession() == null)
        pageNavigator.reset(LandingPage())
}
```

**Benefits:**
- Token persists across page refreshes
- Auto-validates token on load
- Clears invalid tokens automatically
- Reactive - all views update when session changes

### Access via currentSession

```kotlin
// Reactive access to session
val session = currentSession

// Get cached/live data
val rooms = remember {
    currentSession()?.chatRooms?.list(Query(Condition.Always))?.invoke() ?: emptyList()
}

// Direct API calls
val createRoom = Action("Create") {
    val s = currentSession() ?: return@Action
    s.api.chatRoom.insert(room)
}
```

### ModelCache provides reactive lists

```kotlin
// These are on UserSession (extends CachedApi)
currentSession()?.chatRooms  // ModelCache for ChatRoom
currentSession()?.messages   // ModelCache for Message
currentSession()?.users      // ModelCache for User
```

## Dialogs and Overlays

### Dialog Pattern (Recommended)

Use the `dialog { close -> }` pattern for modal dialogs instead of manual Signal management:

```kotlin
// ✅ BEST: Use dialog with close callback
button {
    text("Settings")
    onClick {
        dialog { close ->
            col {
                h2("Room Settings")

                // Form controls
                switch { checked bind isPrivate }
                checkbox { checked bind muteNotifications }

                // Action buttons
                row {
                    button {
                        text("Cancel")
                        onClick { close() }
                    }
                    important.button {
                        text("Save")
                        action = saveAction
                        onClick { close() }  // Close after action
                    }
                }
            }
        }
    }
}

// ❌ OLD WAY: Manual Signal management (avoid this)
val showDialog = Signal(false)

button {
    text("Settings")
    onClick { showDialog.value = true }
}

shownWhen { showDialog() }.dismissBackground {
    onClick { showDialog.value = false }
}

shownWhen { showDialog() }.centered.card.col {
    h2("Room Settings")
    // ... content with manual showDialog.value = false everywhere
}
```

**Benefits of `dialog { close -> }`:**
- No manual Signal needed to track dialog state
- Automatic overlay/background handling
- `close()` callback provided automatically
- Cleaner, more maintainable code

### Other Dialog Types

```kotlin
// Confirmation dialog
confirmDanger("Delete", body = "Delete this item?") {
    // Action to perform if confirmed
    deleteItem()
}

// Toast notification
toast("Settings saved successfully!")

// Custom toast with duration
toast(duration = 5.seconds) {
    row {
        icon(Icon.checkCircle, "Success")
        text("Operation completed!")
    }
}

// Bottom sheet (requires coordinatorFrame)
coordinatorFrame!!.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED) {
    col {
        coordinatorDragHandle()
        h2("Options")
        button {
            text("Close")
            onClick { it.close() }
        }
    }
}
```

## Advanced UI Patterns

### Keyboard Hints
```kotlin
textInput {
    hint = "Email"
    keyboardHints = KeyboardHints.email
    content bind email
}

textInput {
    hint = "Password"
    keyboardHints = KeyboardHints.password
    content bind password
}
```

### TextInput Action (Enter Key Behavior)
```kotlin
textInput {
    hint = "Password"
    content bind password
    action = Action("Log In", Icon.login) {
        submitLogin()
    }
}
```

### Size Constraints
```kotlin
sizeConstraints(maxWidth = 50.rem).card.col { }
sizeConstraints(width = 20.rem).field("Email") { }
sizeConstraints(minHeight = 10.rem).col { }
```

### Background Image Pattern
```kotlin
unpadded.frame {
    image {
        source = Resources.backgroundImage
        scaleType = ImageScaleType.Crop
        opacity = 0.5
    }
    padded.col {
        // Foreground content here
        h1("Title")
        text("Content overlays the background")
    }
}
```

### ViewPager for Swipeable Pages
```kotlin
viewPager {
    children(Constant((1..5).toList()), { it }) { page ->
        col {
            centered.h1 { ::content { "Page ${page()}" } }
            text("Swipe to navigate")
        }
    }
}
```

### Link Component for Navigation
```kotlin
link {
    text { content = "Go to Settings" }
    to = { SettingsPage }
}

link {
    text { content = "View Profile" }
    to = { ProfilePage(userId) }
}
```

### List Editing with `.lensByElementAssumingSetNeverManipulates()`
```kotlin
val items = Signal(listOf("Item 1", "Item 2", "Item 3"))

recyclerView {
    children(items.lensByElementAssumingSetNeverManipulates()) { itemObs ->
        textField {
            content bind itemObs.flatten()
        }
    }
}
```
**Use case:** Direct editing of list items without recreating the entire list

## Common View Components

### Layouts
```kotlin
col { /* vertical */ }
row { /* horizontal */ }
frame { /* fixed container */ }
expanding.frame { /* scrollable container - fills space AND scrolls */ }
```

**Layout Modifiers:**
```kotlin
// expanding = flex: 1 1 0px (takes up available space)
expanding.text("Fills space")
expanding.col { /* grows to fill parent */ }

// frame = container
frame { col { /* ... */ } }

// expanding.frame = grows to fill space AND scrolls content
expanding.frame {
    col {
        // Many items - will scroll if needed
    }
}
```

### Layout Best Practices

**Avoid Redundant Wrapping**

A `frame` with only one child element is redundant - just use the child directly:

```kotlin
// ❌ WRONG: Redundant frame wrapper
frame {
    text("Hello")
}

// ✅ CORRECT: Use element directly
text("Hello")

// ❌ WRONG: Unnecessary frame around single recyclerView
frame {
    recyclerView {
        children(items, id = { it._id }) { /* ... */ }
    }
}

// ✅ CORRECT: Use expanding directly
expanding.recyclerView {
    children(items, id = { it._id }) { /* ... */ }
}
```

**When frames ARE needed:**
```kotlin
// ✅ Multiple children in a layout
frame {
    col {
        text("Header")
        text("Body")
    }
}

// ✅ Scrollable container with content
expanding.frame {
    col {
        // Many items that need scrolling
    }
}

// ✅ Providing size constraints
frame {
    // Child needs explicit sizing from parent
    recyclerView { /* ... */ }
}
```

**Key principle:** Every layout element should have a purpose. If removing it doesn't change behavior, it shouldn't be there.

### Text
```kotlin
h1("Title")
h2 { ::content { dynamic } }
text("Static")
text { ::content { dynamic } }
subtext { ::content { secondary } }
```

### Interactive
```kotlin
button { text("Click") }
card.button { /* clickable card */ }
textInput { content bind signal }
```

### Spacing
```kotlin
space()
separator()
```

### Conditional
```kotlin
shownWhen { condition }.view
```

## Configuration Files

### settings.json (Server)
```json
{
  "database": "json-files://local/database",
  "files": { "storageUrl": "local://local/files" },
  "email": "console",
  "cors": { "allowedDomains": ["http://localhost:8080"] }
}
```

## Common Commands

```bash
# Server
./gradlew :server:serve          # Run dev server
./gradlew :server:generateSdk    # Generate client SDK
./gradlew :server:test           # Run tests

# Frontend
./gradlew :apps:jsBrowserDevelopmentRun  # Web dev server
./gradlew :apps:viteBuild                # Production build
./gradlew :apps:installDebug             # Android

# Full build
./gradlew build
```

## Useful Reactive Extensions

KiteUI includes powerful reactive lens extensions for common transformations:

### Value Comparison

```kotlin
// Radio button binding with .equalTo()
val selected = Signal(1)
radioButton { checked bind selected.equalTo(1) }
radioButton { checked bind selected.equalTo(2) }
```

### Collection Membership

```kotlin
// Toggle set/list membership
val tags = Signal(setOf<String>())
checkbox { checked bind tags.contains("important") }

val items = Signal(listOf<String>())
checkbox { checked bind items.contains("featured") }
```

### Null Handling

```kotlin
// Nullable to non-null with default
val name = Signal<String?>(null)
textInput { content bind name.notNull("Default Name") }

// String null to blank
val description = Signal<String?>(null)
textInput { content bind description.nullToBlank() }
```

### String to Number Conversions

```kotlin
// String signal to number binding
val ageStr = Signal("")
val age = Signal<Int?>(null)

numberInput { content bind ageStr.asInt() }
numberInput { content bind ageStr.asDouble() }
numberInput { content bind ageStr.asFloat() }
numberInput { content bind ageStr.asLong() }

// Hexadecimal conversions
val hexStr = Signal("")
numberInput { content bind hexStr.asIntHex() }
```

### How Lens Extensions Work

These extensions use the reactive lens pattern to create bidirectional bindings:

```kotlin
// Example: .equalTo() implementation
infix fun <T> MutableReactive<T>.equalTo(value: T): MutableReactive<Boolean> = lens(
    get = { it == value },                    // Read: returns true when equal
    modify = { o, it -> if (it) value else o } // Write: sets value when true
)
```

This pattern allows:
- **Reading:** Transform the value for display
- **Writing:** Transform user input back to the original type
- **Type safety:** Compile-time checking of transformations

## Testing Patterns

### Test Organization

KiteUI/Lightning Server apps support three types of tests:

1. **Reactive Logic Tests** - Unit tests for reactive computations and business logic
2. **Screen Logic Tests** - Tests for screen-specific patterns without rendering
3. **Server Integration Tests** - Full-stack tests against test server instance

### Reactive Logic Tests

Test reactive patterns in isolation without rendering UI:

```kotlin
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

        var greeting = ""
        val updateGreeting = {
            greeting = "Hello ${name.value}, you are ${age.value} years old"
        }

        updateGreeting()
        assertEquals("Hello John, you are 25 years old", greeting)

        name.value = "Jane"
        updateGreeting()
        assertEquals("Hello Jane, you are 25 years old", greeting)
    }

    @Test
    fun testDebouncedSignal() = runTest {
        val debounced = Signal("").debounceWrite(100.milliseconds)
        val recordedValues = mutableListOf<String>()

        // Rapid updates
        debounced.value = "a"
        delay(30)
        debounced.value = "ab"
        delay(30)
        debounced.value = "abc"

        // Wait for debounce
        delay(150)

        // Only final value recorded after debounce period
        assertTrue(debounced.value == "abc")
    }

    @Test
    fun testFilteringLogic() {
        // Test the exact filtering logic from your screens
        data class Room(val name: String, val members: Set<String>)

        val rooms = listOf(
            Room("Public Room", setOf("user1", "user2")),
            Room("Private Room", setOf("user2")),
            Room("Team Room", setOf("user1", "user3"))
        )

        val currentUserId = "user1"
        val showAll = Signal(false)
        val searchQuery = Signal("")

        fun getDisplayedRooms(): List<Room> {
            val query = searchQuery.value.trim().lowercase()

            return rooms.filter { room ->
                val membershipMatch = showAll.value || currentUserId in room.members
                val searchMatch = query.isEmpty() || room.name.lowercase().contains(query)
                membershipMatch && searchMatch
            }
        }

        // Test: My rooms only
        showAll.value = false
        var displayed = getDisplayedRooms()
        assertEquals(2, displayed.size)

        // Test: All rooms
        showAll.value = true
        displayed = getDisplayedRooms()
        assertEquals(3, displayed.size)

        // Test: Search filtering
        searchQuery.value = "private"
        displayed = getDisplayedRooms()
        assertEquals(1, displayed.size)
        assertEquals("Private Room", displayed.first().name)
    }
}
```

### Screen Logic Tests

Test screen-specific patterns without rendering:

```kotlin
class ScreenLogicTest {
    @Test
    fun testActionPattern() = runTest {
        var executed = false

        val action = Action("Test Action") {
            executed = true
        }

        assertEquals("Test Action", action.title)
        assertFalse(executed)

        action.onSelect()
        assertTrue(executed)
    }

    @Test
    fun testActionWithValidation() = runTest {
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
    fun testSettingsDialogLocalState() {
        data class Settings(
            val isPrivate: Boolean,
            val muteNotifications: Boolean,
            val autoDeleteDays: Int?
        )

        val original = Settings(
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
        localAutoDeleteDays.value = 7

        // Original unchanged
        assertFalse(original.isPrivate)
        assertEquals(null, original.autoDeleteDays)

        // Local state changed
        assertTrue(localIsPrivate.value)
        assertEquals(7, localAutoDeleteDays.value)
    }

    @Test
    fun testConditionalRenderingLogic() {
        val isEmpty = Signal(true)
        val isLoading = Signal(false)
        val hasError = Signal(false)

        fun shouldShowEmpty(): Boolean = isEmpty.value && !isLoading.value && !hasError.value
        fun shouldShowLoading(): Boolean = isLoading.value
        fun shouldShowError(): Boolean = hasError.value && !isLoading.value
        fun shouldShowContent(): Boolean = !isEmpty.value && !isLoading.value && !hasError.value

        // Initially show empty
        assertTrue(shouldShowEmpty())

        // Show loading
        isLoading.value = true
        assertTrue(shouldShowLoading())
        assertFalse(shouldShowEmpty())

        // Show error
        isLoading.value = false
        hasError.value = true
        assertTrue(shouldShowError())

        // Show content
        hasError.value = false
        isEmpty.value = false
        assertTrue(shouldShowContent())
    }
}
```

### Server Integration Tests

Test endpoints and database operations using `Server.test()`:

```kotlin
class ServerIntegrationTest {
    @Test
    fun testUserCreation() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()

            val newUser = User(
                email = "test@example.com",
                name = "Test User",
                role = UserRole.User
            )

            users.insertOne(newUser)

            val found = users.findOne(condition { it.email eq "test@example.com" })
            assertNotNull(found)
            assertEquals("Test User", found.name)
        }
    }

    @Test
    fun testChatRoomMembership() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val users = db.table<User>()
            val chatRooms = db.table<ChatRoom>()

            // Create users
            val creator = User(email = "creator@example.com", name = "Creator")
            val joiner = User(email = "joiner@example.com", name = "Joiner")
            users.insertOne(creator)
            users.insertOne(joiner)

            // Creator creates a room
            val room = ChatRoom(
                name = "Test Room",
                description = "Test",
                createdBy = creator._id,
                memberIds = setOf(creator._id)
            )
            chatRooms.insertOne(room)

            // Verify initial membership
            var currentRoom = chatRooms.get(room._id)!!
            assertTrue(creator._id in currentRoom.memberIds)
            assertTrue(joiner._id !in currentRoom.memberIds)

            // Joiner joins the room using modification DSL
            chatRooms.updateOneById(
                room._id,
                modification { it.memberIds.assign(currentRoom.memberIds + joiner._id) }
            )

            // Verify updated membership
            currentRoom = chatRooms.get(room._id)!!
            assertTrue(creator._id in currentRoom.memberIds)
            assertTrue(joiner._id in currentRoom.memberIds)
        }
    }

    @Test
    fun testMessageQueryByRoom() = runTest {
        Server.test(settings = { database set Database.Settings("ram") }) {
            val db = Server.database()
            val messages = db.table<Message>()

            // Create messages in different rooms
            val roomId1 = Uuid.random()
            val roomId2 = Uuid.random()
            val userId = Uuid.random()

            messages.insertOne(Message(chatRoomId = roomId1, authorId = userId, content = "Room 1 Message 1"))
            messages.insertOne(Message(chatRoomId = roomId1, authorId = userId, content = "Room 1 Message 2"))
            messages.insertOne(Message(chatRoomId = roomId2, authorId = userId, content = "Room 2 Message"))

            // Query messages for room 1
            val room1Messages = messages.query(Query(condition { it.chatRoomId eq roomId1 }))
            assertEquals(2, room1Messages.size)
            assertTrue(room1Messages.all { it.chatRoomId == roomId1 })

            // Query messages for room 2
            val room2Messages = messages.query(Query(condition { it.chatRoomId eq roomId2 }))
            assertEquals(1, room2Messages.size)
        }
    }
}
```

### Testing Best Practices

**DO:**
- ✅ Test reactive logic without rendering UI
- ✅ Use `Server.test()` with RAM database for fast tests
- ✅ Test complex filtering/computation logic in isolation
- ✅ Test validation and business rules
- ✅ Test database queries and modifications
- ✅ Use `runTest` for coroutine-based tests
- ✅ Test edge cases (empty lists, null values, etc.)

**DON'T:**
- ❌ Try to render actual UI in tests (KiteUI doesn't support UI testing yet)
- ❌ Use real database in tests (use RAM database)
- ❌ Test trivial getters/setters
- ❌ Forget to test debounced behavior with appropriate delays

### Test File Organization

```
apps/src/commonTest/kotlin/
├── ReactiveLogicTest.kt        # Reactive computation tests
├── ScreenLogicTest.kt          # Screen behavior tests
└── ServerIntegrationTest.kt    # Server endpoint tests
```

**Why commonTest?** Tests can run on all platforms (JVM, JS, iOS) for maximum portability.

## Example: Custom Edit Operation (Message Editing)

This example demonstrates implementing a custom operation beyond basic CRUD, with proper permission enforcement and reactive UI updates.

### Backend: Custom Edit Endpoint

**1. Add request model to shared/models.kt:**
```kotlin
@Serializable
data class EditMessageRequest(
    val messageId: Uuid,
    val newContent: String
)
```

**2. Add custom endpoint to MessageEndpoints.kt:**
```kotlin
val edit = path.path("edit").post bind ApiHttpHandler(
    summary = "Edit a message",
    description = "Updates the content of an existing message. Only the author can edit their messages.",
    auth = UserAuth.require(),
    implementation = { request: EditMessageRequest ->
        val userId = auth.id
        val message = info.table().get(request.messageId) ?: throw NotFoundException("Message not found")

        // Verify user is the author
        if (message.authorId != userId) {
            throw ForbiddenException("Only the author can edit this message")
        }

        // Update the message content and editedAt timestamp
        info.table().updateOneById(request.messageId, modification {
            it.content assign request.newContent
            it.editedAt assign Clock.System.now()
        })

        info.table().get(request.messageId)!!
    }
)
```

**Key patterns:**
- Manual permission check for operations that ModelPermissions can't express
- Fetch before update to validate ownership
- Use `modification {}` DSL for field-level updates
- Update timestamp fields when editing (editedAt pattern)
- Return updated entity for client to use

**3. Regenerate SDK:**
```bash
./gradlew :server:generateSdk
```

**4. Update CachedApi.kt to expose ModelCache:**

The SDK generator creates the API interfaces, but you need to manually add ModelCache properties to `CachedApi.kt`:

```kotlin
open class CachedApi(val uncached: Api) {
    // ... existing caches ...
    open val chatRooms = ModelCache(uncached.chatRoom, ChatRoom.serializer())
    open val messages = ModelCache(uncached.message, Message.serializer())
}
```

**Why this is needed:**
- The generated API has endpoints like `api.chatRoom` (singular) for CRUD operations
- `ModelCache` wraps these endpoints to provide reactive caching: `session.chatRooms.list()`
- The plural naming (`chatRooms`, `messages`) is a convention for cached collections
- This must be done manually because CachedApi is not auto-generated

**IMPORTANT:** After adding new model endpoints, always:
1. Run `./gradlew :server:generateSdk` to generate API interfaces
2. Update `CachedApi.kt` to add corresponding ModelCache properties
3. Rebuild the frontend

### Frontend: Edit Dialog UI

```kotlin
recyclerView {
    children(messages, id = { it._id }) { message ->
        val isAuthor = remember { currentSession()?.userId == message().authorId }

        // Define Action outside button scope
        val editMessage = Action("Edit Message") {
            val s = currentSession() ?: return@Action
            dialog { close ->
                val editedContent = Signal(message().content)

                val saveEdit = Action("Save Edit") {
                    s.api.message.editAMessage(EditMessageRequest(
                        messageId = message()._id,
                        newContent = editedContent.value
                    ))
                    close()
                }

                card.padded.col {
                    h2("Edit Message")
                    space()
                    textInput {
                        content bind editedContent
                        hint = "Message content"
                    }
                    space()
                    row {
                        expanding.button {
                            text("Cancel")
                            onClick { close() }
                        }
                        space()
                        expanding.important.buttonTheme.button {
                            text("Save")
                            ::enabled { editedContent().isNotBlank() }
                            action = saveEdit
                        }
                    }
                }
            }
        }

        col {
            card.padded.col {
                row {
                    expanding.subtext {
                        ::content { users()[message().authorId]?.name ?: "Unknown" }
                    }
                    // Show edit button only for author
                    shownWhen { isAuthor() }.button {
                        icon(Icon.edit, "Edit")
                        action = editMessage
                    }
                }
                space()
                text {
                    ::content { message().content }
                }
                space()
                row {
                    subtext {
                        ::content { message().createdAt.toString() }
                    }
                    // Show "edited" indicator if message was edited
                    shownWhen { message().editedAt != null }.space()
                    shownWhen { message().editedAt != null }.subtext("(edited)")
                }
            }
            space()
        }
    }
}
```

**Key patterns:**
- **Conditional actions:** Use `shownWhen { isAuthor() }` to show edit button only to author
- **Nested Actions:** Define `editMessage` Action in renderer, `saveEdit` Action inside dialog
- **Capture reactive values:** Call `message()` BEFORE the dialog to capture the current value
- **Local state:** Use `Signal(currentMessage.content)` for dialog editing without affecting original until save
- **Dialog pattern:** Use `dialog { close -> }` for modal editing interface
- **Edited indicator:** Check `editedAt != null` to show "(edited)" label
- **Reactive updates:** Changes automatically propagate when API call completes (via WebSocket updates)

**CRITICAL: Capturing Reactive Values in Actions**

When working with reactive values inside Actions, you must capture the value BEFORE creating UI elements:

```kotlin
// ❌ WRONG - Calling reactive inside dialog causes suspension issues
val editMessage = Action("Edit") {
    dialog { close ->
        val content = Signal(message().content)  // ERROR: Can't call suspend here
        // ...
    }
}

// ✅ CORRECT - Capture value before dialog
val editMessage = Action("Edit") {
    val currentMessage = message()  // Capture the value first
    dialog { close ->
        val content = Signal(currentMessage.content)  // Use captured value
        // ...
    }
}
```

This is because `message()` is a suspend function (reactive accessor), and `dialog { }` is not a suspend context.

### Edited Fields Pattern

Common pattern for tracking modifications to entities:

**Model with editedAt field:**
```kotlin
@Serializable
@GenerateDataClassPaths
data class Message(
    override val _id: Uuid = Uuid.random(),
    val chatRoomId: Uuid,
    val authorId: Uuid,
    val content: String,
    val createdAt: Instant = Clock.System.now(),
    val editedAt: Instant? = null,  // null = never edited
) : HasId<Uuid>
```

**Update pattern:**
```kotlin
info.table().updateOneById(id, modification {
    it.content assign newContent
    it.editedAt assign Clock.System.now()  // Mark as edited
})
```

**UI indication:**
```kotlin
row {
    subtext { ::content { message().createdAt.toString() } }
    shownWhen { message().editedAt != null }.space()
    shownWhen { message().editedAt != null }.subtext("(edited)")
}
```

### Custom Operations vs ModelPermissions

**Use ModelPermissions when:**
- Operation fits CRUD pattern (create, read, update, delete)
- Permissions can be expressed as database conditions
- All fields follow same access rules

**Use custom endpoints when:**
- Operation has complex validation logic
- Need to fetch related entities before acting
- Operation combines multiple database operations
- Need custom error messages or responses
- Permission check requires runtime logic beyond database conditions

**Example comparison:**
```kotlin
// ✅ ModelPermissions: Simple owner-only access
permissions = {
    val isAuthor = condition { it.authorId eq auth.id }
    ModelPermissions(update = isAuthor)
}

// ✅ Custom endpoint: Complex validation
val edit = path.path("edit").post bind ApiHttpHandler(
    auth = UserAuth.require(),
    implementation = { request ->
        val message = info.table().get(request.messageId) ?: throw NotFoundException()

        // Complex checks
        if (message.authorId != auth.id) throw ForbiddenException("Not author")
        if (message.isLocked) throw ForbiddenException("Message locked")
        if (Clock.System.now() - message.createdAt > 24.hours)
            throw ForbiddenException("Edit window expired")

        // Update...
    }
)
```

## Key Reminders

### Frontend (KiteUI)
1. **Always regenerate SDK** after changing server endpoints AND update CachedApi.kt manually
2. **Views created once**, properties update reactively - never create views inside reactive scopes
3. **Use `.` not `-`** for operator chaining (KiteUI v7+)
4. **Use `Action`** for async operations, not `launch {}`
5. **Define Actions outside button scopes** in recyclerView renderers
6. **Use `shownWhen`** for conditional rendering
7. **Use `::content { }`** for reactive text binding
8. **Use `dialog { close -> }`** for dialogs instead of manual Signal management
9. **Icon syntax:** `icon(Icon.add, "Description")` or `icon { source = Icon.add }` - not all icon names exist
10. **Radio buttons:** Use `.equalTo()` for clean binding: `checked bind selected.equalTo(value)`
11. **RecyclerView children:** Always provide `id` parameter: `children(items, id = { it._id })`
12. **RecyclerView MUST have size constraints** - Use `expanding.recyclerView` or put inside `frame`
13. **Avoid redundant frames** - A frame with one child is redundant; use the child directly
14. **Capture reactive values before dialogs** - Call `val x = reactive()` before `dialog {}`, not inside
15. **Call `.invoke()`** on reactive lists in `remember {}`
16. **Call `item()` to get value** from reactive reference in renderers
17. **Reactive filtering** - Use `remember` to create derived reactive values
18. **Lens extensions** - Use `.contains()`, `.notNull()`, `.asInt()`, etc. for common transformations
19. **Async data loading** - Use `rememberSuspending` for automatic loading state management
20. **Loading states** - Use `LateInitSignal` for manual loading indicators
21. **Persistent state** - Use `PersistentProperty` for data that survives refreshes
22. **Debounced input** - Use `.debounceWrite()` to rate-limit API calls
23. **Background tasks** - Use `load {}` for coroutines tied to view lifecycle
24. **Simple lists** - Use `forEach()` for small/static lists, `children()` for large/dynamic
25. **Keyboard hints** - Set `keyboardHints` on textInput for email, password, etc.
26. **TextInput actions** - Add `action` property for Enter key behavior

### Backend (Lightning Server)
24. **Never trust the client** - All security must be server-enforced, not client-side
25. **Design permissions first** - Think about ModelPermissions before custom endpoints
26. **Question `Condition.Always`** - It's rarely correct for multi-tenant data
27. **Use AuthCacheKey** - Cache expensive derived auth data (roles, memberships)
28. **Access tables via modelInfo** - Use `Server.modelName.info.table()` not `database().table<T>()`
29. **Set membership in queries** - Use `it.memberIds.any { it eq userId }` for Set fields
30. **postPermissionsForUser** - Use to enforce server-controlled field values (authorId, timestamps)
31. **Test security** - Write tests that verify unauthorized access is blocked
32. **Use `kotlin.time.Clock`** not `kotlinx.datetime.Clock`
33. **WebSocket updates** - Combine `ModelRestEndpoints + ModelRestUpdatesWebsocket`
34. **Error documentation** - Use `LSError` and `examples` in ApiHttpHandler
35. **Database modifications** - Use `assign`, `+=`, `-=` in modification DSL
36. **WebSocket pub/sub** - Use `topic()` for real-time broadcasting
37. **Caching** - Use cache-aside pattern with TTL for expensive operations
38. **Convert query results** - Use `.find().toList()` to get List from database queries

### Testing
39. **Server tests** - Use `Server.test()` with RAM database for fast tests
40. **Reactive logic tests** - Test Signal, remember, and filtering logic in isolation
41. **Screen logic tests** - Test Action, validation, and UI state logic without rendering
42. **Integration tests** - Test database operations and business logic with test server