# Chat refactor playbook

The Zapp fork ships a ~1,200-LOC `ChatViewModel` (`screen/chat/viewmodel/ChatViewModel.kt`) that owns identity, conversations, messages, contacts, media, location, moderation, PIN gating, biometrics, ToS, network status, and the scan-key bridge — every chat screen consumes it. That arrangement predates the upstream Zodl convention, which is **one screen → one VM → one State** (`AddressBookVM` + `AddressBookState`, `HomeVM` + `HomeState`, `TransactionProgressVM` + `TransactionProgressState`, etc.).

This document captures the migration trajectory so each subsequent screen can be peeled off the god VM without coordinating across the team.

## Status

| Screen | Owner today | Target owner | Status |
| --- | --- | --- | --- |
| Chat list | `ChatListVM` | `ChatListVM` | Migrated — reference impl |
| Identity setup | `ChatViewModel` | `ChatIdentitySetupVM` | Pending |
| Chat room | `ChatViewModel` | `ChatRoomVM` | Pending |
| Chat profile | `ChatViewModel` | `ChatProfileVM` | Pending |
| Chat settings | `ChatViewModel` | `ChatSettingsVM` | Pending |
| New conversation | `ChatViewModel` | `NewConversationVM` | Pending |
| Chat contacts | `ChatViewModel` | `ChatContactsVM` | Pending |
| Contact edit | `ChatViewModel` | `ContactEditVM` | Pending |
| Scan public key | `ChatScanPublicKeyVM` | `ChatScanPublicKeyVM` | Already correct |

## Reference implementation

`screen/chat/list/` is the canonical example. Read these together before starting a new migration:

- `ChatListState.kt` — declarative `data class` containing every UI prop. Sub-states (`ChatListItemState`, `ChatListNetworkChipState`, `ChatListLeaveDialogState`, …) are nested data classes. No raw `Color`, no formatting logic.
- `ChatListVM.kt` — extends `ViewModel` (not `AndroidViewModel`). Sources are private `MutableStateFlow`s on the VM itself; `state` is a single `StateFlow<ChatListState?>` assembled via `combine().stateIn(viewModelScope, SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT), null)` with the explicit `import kotlinx.coroutines.flow.WhileSubscribed`. Click handlers are private methods passed by reference (`::onBack`).
- `ChatListScreen.kt` — `koinViewModel<ChatListVM>()` + `collectAsStateWithLifecycle()` + `BackHandler` + render the View when state is non-null. Mirrors `AddressBookScreen.kt`.
- `screen/chat/view/ChatListView.kt` — pure visual `@Composable fun ChatListView(state: ChatListState, modifier: Modifier = …)`. No view-model imports.

Naming: VMs end in `VM`, not `ViewModel` (CLAUDE.md). Files live under `screen/chat/<feature>/` rather than `screen/chat/view|viewmodel`.

## Steps to migrate a screen

1. **Create `screen/chat/<feature>/`** alongside the existing view (do not move the view yet).
2. **Define `<Feature>State.kt`** as a flat `data class`. Sub-states for dialogs / sheets / list rows are nested data classes. Click handlers are `() -> Unit`. Text is `StringResource`, built via `stringRes(R.string.…)` (extract any hardcoded English first — see "Strings checklist" below).
3. **Define `<Feature>VM.kt`** following the upstream pattern:
   - Extends `ViewModel`.
   - Constructor takes use cases / repositories / `NavigationRouter`. **Do not take `ZappMessagingSDK` directly once an extracting use case exists** — for the first migrations it is acceptable to read the SDK directly because no use cases yet exist; once two or more VMs need the same read, hoist into a use case (`ObserveChatConversationsUseCase`, etc.).
   - Subscribe to the SDK in `init` via `viewModelScope.launch { … }`. Mirror the legacy ChatViewModel's event handlers for the slice the new VM owns.
   - Expose `val state: StateFlow<<Feature>State?>` via `combine(…).stateIn(viewModelScope, SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT), initialValue = null)`. `combine` has a max-5-arg overload — group related inputs into intermediate `Pair`/`Triple`s when you exceed it.
   - Navigation is owned by the VM. Use `navigationRouter.forward(<Args>)`, never via callbacks.
   - For the click-through to a screen still owned by `ChatViewModel`, emit a side-effect via `MutableSharedFlow<T>` and have the `*Screen` composable bridge to the legacy call. Remove the bridge once the destination is also migrated.
4. **Create `<Feature>Screen.kt`** as a `@Composable internal fun` that resolves the VM via `koinViewModel<<Feature>VM>()`, collects with `collectAsStateWithLifecycle()`, wires `BackHandler`, and renders the View.
5. **Rewrite the view** to take `state: <Feature>State` and a `Modifier`. Strip all `viewModel.x.collectAsState()` calls. Preserve the visual code verbatim — this step is mechanical.
6. **Register the VM** in `di/ViewModelModule.kt` (`viewModelOf(::<Feature>VM)`). Use cases register in `di/UseCaseModule.kt`.
7. **Update `AndroidChat.kt`** to call the new `<Feature>Screen` instead of the view. Keep the legacy `ChatViewModel` resolved at this entry point only if it provides side effects (e.g. SDK initialization) that the new VM relies on — otherwise drop the reference.
8. **Do not remove anything from `ChatViewModel`** — the project policy is to retain dead/duplicate code during the migration so we never have to dig it out of `git log` if a regression appears. Annotate the now-orphan slices with `// DEAD CODE [hidden]: ChatListVM owns this` (or equivalent) so the next migration recognises them.

## Strings checklist

Before introducing a `<Feature>State`, sweep its view for hardcoded English strings:

```bash
rg -n '"[A-Z][a-zA-Z ]{2,}"' ui-lib/src/main/java/co/electriccoin/zcash/ui/screen/chat/view/<View>.kt
```

Add entries to `ui-lib/src/main/res/ui/chat/values/strings.xml`. The Zapp fork has no `values-es/` for chat yet — that is a follow-up; the default-fallback behaviour means an English-only locale ships correctly today. When you add Spanish translations, mirror the file structure used by the upstream screens (`res/ui/<screen>/values-es/strings.xml`).

## Use cases worth extracting next

Once a second VM needs the same SDK call, hoist it into a use case under `common/usecase/`:

- `ObserveChatConversationsUseCase` — wraps `sdk.getConversations` + `sdk.messageReceived/inviteReceived/groupDeleted/groupRenamed/memberLeft/memberAdded` into a single `Flow<List<ChatConversation>>`. Today `ChatListVM` and `ChatViewModel` each maintain their own copy; the next migration (`ChatContactsVM` or `NewConversationVM`) will want this.
- `ObserveChatConnectionUseCase` — combines `isOnline`/`peerCount`/`dhtHealth` into a `ChatConnectionInfo` data class.
- `SendChatMessageUseCase` / `SendChatMediaUseCase` (with `withContext(Dispatchers.IO)` around `ImageProcessor`) — required for `ChatRoomVM`.
- `VerifyChatPinUseCase` / `ExportChatSeedUseCase` — used by `ChatProfileVM` and `ChatSettingsVM`.
- `ValidatePublicKeyUseCase` — currently duplicated in `NewConversationView` and `ChatViewModel`.
- `MapMessageStatusUseCase` — replaces the fragile `when (status) { "sent" -> … }` string parsing in `ChatViewModel.subscribeToSDKEvents`.

## Repository carve-outs

When two or more VMs need the same stateful slice, promote it to a Koin `single` repository:

- `ChatModerationRepository` — already exists.
- `ChatConnectionRepository` — would own `isOnline / peerCount / dhtHealth / connectionDetails` as `StateFlow`s.
- `ChatConversationsRepository` — would own the merged conversation list + unread counts.
- `ChatTosRepository` — wraps `StandardPreferenceKeys.IS_CHAT_TOS_ACCEPTED` for observability.

Repositories should subscribe to the SDK once (in their constructor or via an app-scoped `CoroutineScope`) and expose results as `StateFlow`s. Both `ChatListVM` and the legacy `ChatViewModel` can then read from the repository instead of duplicating subscriptions.

## What to delete (eventually)

Once every screen above has migrated:

- `screen/chat/viewmodel/ChatViewModel.kt` — its remaining responsibilities should all live in repositories or per-screen VMs.
- `ChatViewModel.ConnectionStatus` / `ChatViewModel.DhtHealth` — replaced by the top-level enums in `screen/chat/list/ChatListConnection.kt` (move them out of `list/` into `model/` once they have a second consumer).
- The `toLegacy()` bridges at the bottom of `ChatListView.kt` — only exist because `NetworkDetailsSheet` (shared with `ChatRoomView`) still expects the legacy enums.
- The `onLegacyConversationSelected` hook on `ChatListScreen` — only exists because `ChatRoomView` reads `currentConversation` from its own `ChatViewModel` instance.

Per project convention these deletions should land in a final cleanup PR; do not interleave them with the per-screen migrations.
