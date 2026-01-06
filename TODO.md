# Support Chat - Missing Features

## Critical (Blocking Real-World Use)

### Requires Model Changes
- [ ] **Conversation status field** - Add `status: Status` enum (`Open`, `NeedsHuman`, `Resolved`) to `SystemChatConversation`
- [ ] **Auto-escalation flag** - When AI responds with escalation language, automatically set status to `NeedsHuman`

### Can Implement Without Model Changes
- [ ] **Typing/thinking indicator** - Show visual feedback while AI is processing (deferred)
- [ ] **Admin notifications** - Alert admins when conversations need attention (can use existing `autoProcess=false` as trigger)

## High Priority (Expected in Production)

### Can Implement Without Model Changes
- [x] **Enter key to send** - Standard UX pattern for chat input ✓
- [x] **Conversation naming** - Allow users to set conversation name via edit button ✓
- [x] **Search/filter on admin dashboard** - Filter by user email, name, and "Needs Attention" ✓
- [ ] **Auto-scroll to new messages** - Scroll to bottom when new messages arrive
- [x] **File attachments UI** - Upload via UploadEarlyEndpoint, display in messages ✓
- [x] **Better timestamp formatting** - Show relative times ("2 minutes ago") via `toRelativeTimeString()` ✓

### Requires Model Changes
- [ ] **Unread message count** - Track last read timestamp per conversation per user

## Medium Priority (Nice to Have)

### Can Implement Without Model Changes
- [ ] **Canned responses for admins** - Quick templates for common replies (can be client-side initially) (deferred)
- [ ] **Conversation count/stats on admin dashboard** - Show total open, needs attention, resolved
- [ ] **Keyboard shortcuts** - Escape to clear input, Ctrl+Enter for newline
- [ ] **Message formatting** - Support basic markdown in messages
- [ ] **Copy message content** - Click to copy a message

### Requires Model Changes
- [ ] **Conversation assignment** - Assign conversations to specific admin users
- [ ] **Admin-only notes** - Internal notes visible only to support staff
- [ ] **User feedback/rating** - Let users rate support quality after resolution
- [ ] **Tags/categories** - Organize conversations by issue type
- [ ] **AI knowledge base reference** - Store and reference company-specific documentation

## Lower Priority

- [ ] **Conversation history export** - Export chat transcript as PDF/text
- [ ] **Time-based auto-escalation** - Flag conversations waiting too long for response
- [ ] **Typing indicators for users** - Show when admin is typing (requires WebSocket enhancement)
- [ ] **Read receipts** - Show when messages have been read

## Test Coverage Gaps

- [ ] Permission enforcement tests (user can only see own conversations)
- [ ] Message creation flow tests
- [ ] Human takeover flow tests
- [ ] Admin permission tests
- [ ] WebSocket update tests

---

## Implementation Notes

**Features using existing model capabilities:**
- `autoProcess: Boolean` - Already used for human takeover, can also indicate "needs attention"
- `attachments: List<ServerFile>` - Model supports it, UI doesn't expose it
- `name: String` - Exists on conversation, just not editable in UI
- `skipAutoResponse: Boolean` - Already implemented for admin responses

**Workarounds without model changes:**
- "Needs attention" can be inferred from `autoProcess=false` OR recent user message with no admin response
- Status filtering can be done client-side based on `autoProcess` and message timestamps
