package co.electriccoin.zcash.ui.screen.chat.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappChipVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappFab
import co.electriccoin.zcash.ui.design.component.zapp.ZappInputField
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.ZappStatusChip
import co.electriccoin.zcash.ui.design.component.zapp.ellipsizeAddress
import co.electriccoin.zcash.ui.design.component.zapp.initialsOf
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZappNavBar
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.AddressBookContact
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.screen.addressbook.WalletAddressesSection
import co.electriccoin.zcash.ui.screen.chat.contacts.ChatContactsState
import co.electriccoin.zcash.ui.screen.chat.model.ChatContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContactsView(
    state: ChatContactsState,
    modifier: Modifier = Modifier,
) {
    val contacts = state.contacts
    val scannedPublicKey = state.scannedPublicKey
    val scannedWalletAddress = state.scannedWalletAddress
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<ChatContact?>(null) }

    // Reopen the sheet automatically after a scan completes — the scanner
    // screen disposes this composition, so the sheet has to be re-summoned
    // once we're back.
    LaunchedEffect(scannedPublicKey) {
        if (scannedPublicKey != null) {
            showAddDialog = true
        }
    }

    LaunchedEffect(scannedWalletAddress) {
        if (scannedWalletAddress != null) {
            showAddDialog = true
        }
    }

    val c = ZappTheme.colors
    val grouped =
        remember(contacts) {
            contacts
                .sortedBy { it.name.lowercase() }
                .groupBy { (it.name.firstOrNull() ?: '?').uppercaseChar() }
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ZappScreenHeader(
                title = state.title.getValue(),
                right = {
                    ZappStatusChip(
                        text = stringResource(R.string.chat_contacts_saved_count_fmt, contacts.size),
                        variant = ZappChipVariant.Muted,
                    )
                },
            )

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Contacts,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = c.textSubtle,
                        )
                        Spacer(Modifier.height(12.dp))
                        BasicText(
                            stringResource(R.string.chat_contacts_empty_title),
                            style = ZappTheme.typography.sectionTitle.copy(color = c.text),
                        )
                        Spacer(Modifier.height(6.dp))
                        BasicText(
                            stringResource(R.string.chat_contacts_empty_subtitle),
                            style = ZappTheme.typography.body.copy(color = c.textMuted),
                        )
                    }
                }
            } else {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = navBarBottom + ZappNavBar.CLEARANCE_DP.dp,
                    ),
                ) {
                    grouped.forEach { (letter, bucket) ->
                        item(key = "header-$letter") {
                            BasicText(
                                text = letter.toString(),
                                style = ZappTheme.typography.groupLabel.copy(color = c.textMuted),
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 14.dp,
                                    bottom = 4.dp,
                                ),
                            )
                        }
                        items(
                            items = bucket,
                            key = { it.publicKey },
                        ) { contact ->
                            ContactListItem(
                                contact = contact,
                                onChat = { state.onStartChat(contact.publicKey) },
                                onEdit = { editingContact = contact },
                            )
                        }
                    }
                }
            }
        }

        ZappFab(
            icon = Icons.Default.PersonAdd,
            contentDescription = stringResource(R.string.chat_contacts_add_content_description),
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    end = 20.dp,
                    bottom = ZappNavBar.FAB_BOTTOM_PADDING_DP.dp,
                ),
        )

        if (state.showBackButton) {
            ZappBackButton(
                onClick = state.onBack,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = 20.dp,
                        bottom = ZappNavBar.FAB_BOTTOM_PADDING_DP.dp,
                    ),
            )
        }
    }

    if (showAddDialog) {
        AddContactSheet(
            existingKeys = contacts.map { it.publicKey }.toSet(),
            scannedPublicKey = scannedPublicKey,
            scannedWalletAddress = scannedWalletAddress,
            onScanPublicKey = state.onScanPublicKey,
            onScanWalletAddress = state.onScanWalletAddress,
            onConsumeScannedKey = state.onConsumeScannedPublicKey,
            onConsumeScannedWalletAddress = state.onConsumeScannedWalletAddress,
            onDismiss = { showAddDialog = false },
            onAdd = { publicKey, name, walletAddress, walletAddresses ->
                state.onAddContact(publicKey, name, walletAddress, walletAddresses)
                state.onConsumeScannedPublicKey()
                state.onConsumeScannedWalletAddress()
                showAddDialog = false
            }
        )
    }

    editingContact?.let { contact ->
        EditChatContactSheet(
            contact = contact,
            scannedWalletAddress = scannedWalletAddress,
            onScanWalletAddress = state.onScanWalletAddress,
            onConsumeScannedWalletAddress = state.onConsumeScannedWalletAddress,
            onDismiss = { editingContact = null },
            onSave = { name, walletAddress, walletAddresses ->
                state.onUpdateContact(contact.publicKey, name, walletAddress, walletAddresses)
                editingContact = null
            },
            onDelete = {
                state.onDeleteContact(contact.publicKey)
                editingContact = null
            },
        )
    }
}

@Composable
private fun ContactListItem(
    contact: ChatContact,
    onChat: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = ZappTheme.colors
    val initials = remember(contact.name) { initialsOf(contact.name) }
    val shortKey = remember(contact.publicKey) { contact.publicKey.ellipsizeAddress() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(c.accent, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = initials,
                style = ZappTheme.typography.rowTitle.copy(color = c.onAccent),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = contact.name,
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = shortKey,
                style = ZappTheme.typography.mono.copy(color = c.textMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onChat)
                .semantics {
                    contentDescription = "Start chat"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactSheet(
    existingKeys: Set<String>,
    scannedPublicKey: String?,
    scannedWalletAddress: String?,
    onScanPublicKey: () -> Unit,
    onScanWalletAddress: () -> Unit,
    onConsumeScannedKey: () -> Unit,
    onConsumeScannedWalletAddress: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: (publicKey: String, name: String, walletAddress: String, walletAddresses: Map<String, String>) -> Unit,
) {
    val c = ZappTheme.colors
    var nameInput by remember { mutableStateOf(TextFieldValue("")) }
    var publicKeyInput by remember { mutableStateOf(TextFieldValue("")) }
    var walletAddressInput by remember { mutableStateOf(TextFieldValue("")) }
    var error by remember { mutableStateOf<String?>(null) }

    // Additional address fields
    var showAdditionalAddresses by remember { mutableStateOf(false) }
    var transparentAddr by remember { mutableStateOf(TextFieldValue("")) }
    var evmAddr by remember { mutableStateOf(TextFieldValue("")) }
    var solanaAddr by remember { mutableStateOf(TextFieldValue("")) }
    var scanTargetField by remember { mutableStateOf<String?>(null) }

    // When a scan result arrives via the VM, populate the input and consume it
    // so re-opening the sheet later doesn't pre-fill stale data.
    LaunchedEffect(scannedPublicKey) {
        scannedPublicKey?.let { key ->
            publicKeyInput = TextFieldValue(key)
            error = null
            onConsumeScannedKey()
        }
    }

    // Route scanned wallet address to the correct field
    LaunchedEffect(scannedWalletAddress, scanTargetField) {
        if (scannedWalletAddress != null && scanTargetField != null) {
            val tfv = TextFieldValue(scannedWalletAddress)
            when (scanTargetField) {
                AddressBookContact.ADDR_TYPE_TRANSPARENT -> transparentAddr = tfv
                AddressBookContact.ADDR_TYPE_EVM -> evmAddr = tfv
                AddressBookContact.ADDR_TYPE_SOLANA -> solanaAddr = tfv
            }
            showAdditionalAddresses = true
            scanTargetField = null
            onConsumeScannedWalletAddress()
        }
    }

    // Default scan (no target field) goes to the primary wallet address
    LaunchedEffect(scannedWalletAddress) {
        if (scannedWalletAddress != null && scanTargetField == null) {
            walletAddressInput = TextFieldValue(scannedWalletAddress)
            error = null
            onConsumeScannedWalletAddress()
        }
    }

    val cleanedKey by remember { derivedStateOf { publicKeyInput.text.trim().removePrefix("0x") } }
    val isValidKey by remember {
        derivedStateOf {
            cleanedKey.length == 64 &&
                cleanedKey.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        }
    }

    val onScanAddrField: (String) -> Unit = { addrType ->
        scanTargetField = addrType
        onScanWalletAddress()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RectangleShape,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .padding(bottom = 28.dp),
        ) {
            BasicText(
                text = "Add New Contact",
                style = ZappTheme.typography.sectionTitle.copy(
                    color = c.text,
                    fontWeight = FontWeight.Black,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Name field
            ZappInputField(
                value = nameInput,
                onValueChange = { nameInput = it; error = null },
                placeholder = stringResource(co.electriccoin.zcash.ui.R.string.contact_name_hint),
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.textSubtle,
                    )
                },
            )

            Spacer(Modifier.height(12.dp))

            // Messaging Key field
            ZappInputField(
                value = publicKeyInput,
                onValueChange = { publicKeyInput = it; error = null },
                placeholder = "Messaging Key (64 hex chars)",
                leadingIcon = {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.textSubtle,
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onScanPublicKey)
                            .semantics { contentDescription = "Scan messaging key QR"; role = Role.Button },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = c.textSubtle,
                        )
                    }
                },
            )

            // Valid key confirmation row
            if (isValidKey) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.successSoft, RectangleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = c.success,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = "${cleanedKey.take(10)}…${cleanedKey.takeLast(6)}",
                        style = ZappTheme.typography.chip.copy(color = c.success),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Wallet Address field (Unified)
            ZappInputField(
                value = walletAddressInput,
                onValueChange = { walletAddressInput = it; error = null },
                placeholder = stringResource(co.electriccoin.zcash.ui.R.string.contact_address_hint),
                leadingIcon = {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.textSubtle,
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onScanWalletAddress)
                            .semantics { contentDescription = "Scan wallet address QR"; role = Role.Button },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = c.textSubtle,
                        )
                    }
                },
            )

            // Inline error message
            error?.let {
                Spacer(Modifier.height(8.dp))
                BasicText(
                    text = it,
                    style = ZappTheme.typography.caption.copy(color = c.danger),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Additional Addresses section
            WalletAddressesSection(
                expanded = showAdditionalAddresses,
                onToggle = { showAdditionalAddresses = !showAdditionalAddresses },
                transparentAddr = transparentAddr,
                onTransparentChange = { transparentAddr = it },
                evmAddr = evmAddr,
                onEvmChange = { evmAddr = it },
                solanaAddr = solanaAddr,
                onSolanaChange = { solanaAddr = it },
                onScanAddress = onScanAddrField,
            )

            Spacer(Modifier.height(20.dp))

            // Add Contact primary CTA
            val keyboard = LocalSoftwareKeyboardController.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(c.accent, RectangleShape)
                    .clickable(onClick = {
                        val pk = publicKeyInput.text.trim().removePrefix("0x")
                        val name = nameInput.text.trim()
                        val wallet = walletAddressInput.text.trim()
                        val isValidHex = pk.length == 64 && pk.all {
                            it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
                        }
                        val addrs = buildMap {
                            if (transparentAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_TRANSPARENT, transparentAddr.text.trim())
                            if (evmAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_EVM, evmAddr.text.trim())
                            if (solanaAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_SOLANA, solanaAddr.text.trim())
                        }
                        when {
                            name.isEmpty() -> error = "Name is required"
                            pk.isEmpty() -> error = "Messaging key is required"
                            !isValidHex -> error = "Invalid messaging key — must be 64 hex characters"
                            existingKeys.contains(pk) -> error = "Contact already exists"
                            else -> {
                                keyboard?.hide()
                                onAdd(pk, name, wallet, addrs)
                            }
                        }
                    })
                    .semantics { contentDescription = "Add Contact"; role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "SAVE",
                    style = ZappTheme.typography.button.copy(
                        color = c.onAccent,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditChatContactSheet(
    contact: ChatContact,
    scannedWalletAddress: String?,
    onScanWalletAddress: () -> Unit,
    onConsumeScannedWalletAddress: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, walletAddress: String, walletAddresses: Map<String, String>) -> Unit,
    onDelete: () -> Unit,
) {
    val c = ZappTheme.colors
    var nameInput by remember { mutableStateOf(TextFieldValue(contact.name)) }
    var walletAddressInput by remember { mutableStateOf(TextFieldValue(contact.walletAddress.orEmpty())) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Additional address fields
    var showAdditionalAddresses by remember { mutableStateOf(false) }
    var transparentAddr by remember { mutableStateOf(TextFieldValue("")) }
    var evmAddr by remember { mutableStateOf(TextFieldValue("")) }
    var solanaAddr by remember { mutableStateOf(TextFieldValue("")) }
    var scanTargetField by remember { mutableStateOf<String?>(null) }

    // Route scanned wallet address to the correct field
    LaunchedEffect(scannedWalletAddress, scanTargetField) {
        if (scannedWalletAddress != null && scanTargetField != null) {
            val tfv = TextFieldValue(scannedWalletAddress)
            when (scanTargetField) {
                AddressBookContact.ADDR_TYPE_TRANSPARENT -> transparentAddr = tfv
                AddressBookContact.ADDR_TYPE_EVM -> evmAddr = tfv
                AddressBookContact.ADDR_TYPE_SOLANA -> solanaAddr = tfv
            }
            showAdditionalAddresses = true
            scanTargetField = null
            onConsumeScannedWalletAddress()
        }
    }

    // Default scan (no target field) goes to the primary wallet address
    LaunchedEffect(scannedWalletAddress) {
        if (scannedWalletAddress != null && scanTargetField == null) {
            walletAddressInput = TextFieldValue(scannedWalletAddress)
            error = null
            onConsumeScannedWalletAddress()
        }
    }

    val onScanAddrField: (String) -> Unit = { addrType ->
        scanTargetField = addrType
        onScanWalletAddress()
    }

    val hasChanges = nameInput.text.trim() != contact.name ||
        walletAddressInput.text.trim() != (contact.walletAddress.orEmpty()) ||
        transparentAddr.text.isNotBlank() || evmAddr.text.isNotBlank() || solanaAddr.text.isNotBlank()
    val shortKey = remember(contact.publicKey) { contact.publicKey.ellipsizeAddress() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RectangleShape,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .padding(bottom = 28.dp),
        ) {
            BasicText(
                text = "Edit Contact",
                style = ZappTheme.typography.sectionTitle.copy(
                    color = c.text,
                    fontWeight = FontWeight.Black,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Name field
            ZappInputField(
                value = nameInput,
                onValueChange = { nameInput = it; error = null },
                placeholder = stringResource(co.electriccoin.zcash.ui.R.string.contact_name_hint),
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.textSubtle,
                    )
                },
            )

            Spacer(Modifier.height(12.dp))

            // Messaging key — read-only display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceInput, RectangleShape)
                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = c.textSubtle,
                )
                Spacer(Modifier.width(10.dp))
                BasicText(
                    text = shortKey,
                    style = ZappTheme.typography.mono.copy(color = c.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Wallet Address field (editable, Unified)
            ZappInputField(
                value = walletAddressInput,
                onValueChange = { walletAddressInput = it; error = null },
                placeholder = stringResource(co.electriccoin.zcash.ui.R.string.contact_address_hint),
                leadingIcon = {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.textSubtle,
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onScanWalletAddress)
                            .semantics { contentDescription = "Scan wallet address QR"; role = Role.Button },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = c.textSubtle,
                        )
                    }
                },
            )

            // Inline error
            error?.let {
                Spacer(Modifier.height(8.dp))
                BasicText(
                    text = it,
                    style = ZappTheme.typography.caption.copy(color = c.danger),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Additional Addresses section
            WalletAddressesSection(
                expanded = showAdditionalAddresses,
                onToggle = { showAdditionalAddresses = !showAdditionalAddresses },
                transparentAddr = transparentAddr,
                onTransparentChange = { transparentAddr = it },
                evmAddr = evmAddr,
                onEvmChange = { evmAddr = it },
                solanaAddr = solanaAddr,
                onSolanaChange = { solanaAddr = it },
                onScanAddress = onScanAddrField,
            )

            Spacer(Modifier.height(20.dp))

            // Delete confirmation inline
            if (showDeleteConfirm) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.dangerSoft, RectangleShape)
                        .border(BorderStroke(1.dp, c.danger), RectangleShape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column {
                        BasicText(
                            text = "Delete contact?",
                            style = ZappTheme.typography.rowTitle.copy(
                                color = c.danger,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            text = "This cannot be undone.",
                            style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted),
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(BorderStroke(1.dp, c.border), RectangleShape)
                                    .clickable(onClick = { showDeleteConfirm = false })
                                    .semantics {
                                        contentDescription = "Cancel delete"
                                        role = Role.Button
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = "CANCEL",
                                    style = ZappTheme.typography.button.copy(
                                        color = c.text,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                    ),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(c.danger, RectangleShape)
                                    .clickable(onClick = onDelete)
                                    .semantics {
                                        contentDescription = "Confirm delete contact"
                                        role = Role.Button
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = "DELETE",
                                    style = ZappTheme.typography.button.copy(
                                        color = c.onAccent,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // Save + Delete CTAs
            if (!showDeleteConfirm) {
                val keyboard = LocalSoftwareKeyboardController.current
                val saveEnabled = hasChanges && nameInput.text.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            if (saveEnabled) c.accent else c.surfaceAlt,
                            RectangleShape,
                        )
                        .then(
                            if (saveEnabled) {
                                Modifier.clickable(onClick = {
                                    val name = nameInput.text.trim()
                                    if (name.isEmpty()) {
                                        error = "Name is required"
                                    } else {
                                        keyboard?.hide()
                                        val addrs = buildMap {
                                            if (transparentAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_TRANSPARENT, transparentAddr.text.trim())
                                            if (evmAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_EVM, evmAddr.text.trim())
                                            if (solanaAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_SOLANA, solanaAddr.text.trim())
                                        }
                                        onSave(name, walletAddressInput.text.trim(), addrs)
                                    }
                                })
                            } else {
                                Modifier
                            }
                        )
                        .semantics {
                            contentDescription = "Save changes"
                            role = Role.Button
                            if (!saveEnabled) disabled()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "SAVE CHANGES",
                        style = ZappTheme.typography.button.copy(
                            color = if (saveEnabled) c.onAccent else c.textSubtle,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(c.dangerSoft, RectangleShape)
                        .clickable(onClick = { showDeleteConfirm = true })
                        .semantics {
                            contentDescription = "Delete contact"
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "DELETE CONTACT",
                        style = ZappTheme.typography.button.copy(
                            color = c.danger,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                        ),
                    )
                }
            }
        }
    }
}
