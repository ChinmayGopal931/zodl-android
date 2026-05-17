@file:Suppress("TooManyFunctions")

package co.electriccoin.zcash.ui.screen.addressbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.AddressBookContact
import co.electriccoin.zcash.ui.design.component.BlankBgScaffold
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.CircularScreenProgressIndicator
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.Spacer
import co.electriccoin.zcash.ui.design.component.listitem.ContactListItemState
import co.electriccoin.zcash.ui.design.component.zapp.ZappBackButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButton
import co.electriccoin.zcash.ui.design.component.zapp.ZappButtonVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappInputField
import co.electriccoin.zcash.ui.design.component.zapp.ZappRowDivider
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.imageRes
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.design.util.withStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBookView(
    state: AddressBookState
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    BlankBgScaffold(
        topBar = {
            ZappScreenHeader(
                title = state.title.getValue(),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .testTag(AddressBookTag.TOP_APP_BAR),
            )
        },
        bottomBar = {
            AddressBookBottomBar(
                onBack = state.onBack,
                onAddContact = { showAddSheet = true },
            )
        },
    ) { paddingValues ->
        when {
            state.items.isEmpty() && state.isLoading -> {
                CircularScreenProgressIndicator()
            }

            state.items.isEmpty() && !state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(id = R.string.address_book_empty),
                        style = ZappTheme.typography.rowTitle.copy(
                            color = ZappTheme.colors.text,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    itemsIndexed(
                        contentType = { _, item -> item.contentType },
                        items = state.items,
                    ) { index, item ->
                        when (item) {
                            is AddressBookItem.Contact -> {
                                ZappContactRow(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    state = item.state,
                                )
                                if (index != state.items.lastIndex &&
                                    state.items[index + 1] is AddressBookItem.Contact
                                ) {
                                    ZappRowDivider(
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        inset = true,
                                    )
                                }
                            }

                            is AddressBookItem.Title -> {
                                if (index == 0) {
                                    Spacer(Modifier.height(16.dp))
                                } else {
                                    Spacer(Modifier.height(20.dp))
                                }
                                ZappSectionTitle(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    state = item
                                )
                                Spacer(Modifier.height(4.dp))
                            }

                            AddressBookItem.Empty -> {
                                Spacer(modifier = Modifier.height(68.dp))
                                EmptyItem(
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Track which additional-address field is waiting for a scan result
    var scanTargetField by rememberSaveable { mutableStateOf<String?>(null) }

    // Add Contact bottom sheet
    if (showAddSheet) {
        AddContactSheet(
            scannedMessagingKey = state.scannedMessagingKey,
            onConsumeScannedMessagingKey = { state.onConsumeScannedMessagingKey?.invoke() },
            onScanMessagingKey = { state.onScanMessagingKey?.invoke() },
            scannedAddress = state.scannedAddress,
            onConsumeScannedAddress = { state.onConsumeScannedAddress?.invoke() },
            onScanWalletAddress = { state.onScanQr?.invoke() },
            scanTargetField = scanTargetField,
            onScanAddrField = { addrType ->
                scanTargetField = addrType
                state.onScanWalletAddress?.invoke() ?: state.onScanQr?.invoke()
            },
            onConsumeScanTarget = { scanTargetField = null },
            onDismiss = { showAddSheet = false },
            onAdd = { name, messagingKey, walletAddress, walletAddresses ->
                state.onSaveNewContact?.invoke(name, messagingKey, walletAddress, walletAddresses)
                showAddSheet = false
            },
        )
    }

    // Edit Contact bottom sheet
    state.editingContact?.let { editData ->
        EditContactSheet(
            editData = editData,
            scannedAddress = state.scannedAddress,
            onConsumeScannedAddress = { state.onConsumeScannedAddress?.invoke() },
            scanTargetField = scanTargetField,
            onScanAddrField = { addrType ->
                scanTargetField = addrType
                state.onScanWalletAddress?.invoke() ?: state.onScanQr?.invoke()
            },
            onConsumeScanTarget = { scanTargetField = null },
            onDismiss = { state.onDismissEdit?.invoke() },
            onSave = { name, walletAddress, walletAddresses ->
                state.onUpdateContact?.invoke(name, walletAddress, walletAddresses)
            },
            onDelete = {
                state.onDeleteContact?.invoke()
            },
        )
    }
}

// ── Bottom bar: back + Add New Contact on the same row ───────────────────────

@Composable
private fun AddressBookBottomBar(
    onBack: () -> Unit,
    onAddContact: () -> Unit,
) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(BorderStroke(1.dp, c.border), RectangleShape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ZappBackButton(onClick = onBack)
        ZappButton(
            text = stringResource(R.string.address_book_add),
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            variant = ZappButtonVariant.Primary,
            onClick = onAddContact,
        )
    }
}

// ── Add Contact bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun AddContactSheet(
    scannedMessagingKey: String?,
    onConsumeScannedMessagingKey: () -> Unit,
    onScanMessagingKey: () -> Unit,
    scannedAddress: String?,
    onConsumeScannedAddress: () -> Unit,
    onScanWalletAddress: () -> Unit,
    scanTargetField: String?,
    onScanAddrField: (addrType: String) -> Unit,
    onConsumeScanTarget: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: (name: String, messagingKey: String, walletAddress: String, walletAddresses: Map<String, String>) -> Unit,
) {
    val c = ZappTheme.colors
    var nameInput by remember { mutableStateOf(TextFieldValue("")) }
    var messagingKeyInput by remember { mutableStateOf(TextFieldValue("")) }
    var walletAddressInput by remember { mutableStateOf(TextFieldValue("")) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdditionalAddresses by remember { mutableStateOf(false) }
    var transparentAddr by remember { mutableStateOf(TextFieldValue("")) }
    var evmAddr by remember { mutableStateOf(TextFieldValue("")) }
    var solanaAddr by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(scannedMessagingKey) {
        scannedMessagingKey?.let { key ->
            messagingKeyInput = TextFieldValue(key)
            error = null
            onConsumeScannedMessagingKey()
        }
    }

    // Route scanned address to the target additional-address field
    LaunchedEffect(scannedAddress, scanTargetField) {
        if (scannedAddress != null && scanTargetField != null) {
            val tfv = TextFieldValue(scannedAddress)
            when (scanTargetField) {
                AddressBookContact.ADDR_TYPE_TRANSPARENT -> transparentAddr = tfv
                AddressBookContact.ADDR_TYPE_EVM -> evmAddr = tfv
                AddressBookContact.ADDR_TYPE_SOLANA -> solanaAddr = tfv
            }
            showAdditionalAddresses = true
            onConsumeScannedAddress()
            onConsumeScanTarget()
        }
    }

    LaunchedEffect(scannedAddress) {
        scannedAddress?.let { addr ->
            if (scanTargetField == null) {
                walletAddressInput = TextFieldValue(addr)
                error = null
                onConsumeScannedAddress()
            }
        }
    }

    val cleanedKey by remember { derivedStateOf { messagingKeyInput.text.trim().removePrefix("0x") } }
    val isValidKey by remember {
        derivedStateOf {
            cleanedKey.length == 64 &&
                cleanedKey.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        }
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
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            BasicText(
                text = stringResource(R.string.add_new_contact_title),
                style = ZappTheme.typography.screenTitle.copy(
                    color = c.text,
                    fontWeight = FontWeight.Black,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Name field
            ZappInputField(
                value = nameInput,
                onValueChange = { nameInput = it; error = null },
                placeholder = stringResource(R.string.contact_name_hint),
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
                value = messagingKeyInput,
                onValueChange = { messagingKeyInput = it; error = null },
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
                            .clickable(onClick = onScanMessagingKey)
                            .semantics {
                                contentDescription = "Scan messaging key QR"
                                role = Role.Button
                            },
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

            // Wallet Address field
            ZappInputField(
                value = walletAddressInput,
                onValueChange = { walletAddressInput = it; error = null },
                placeholder = stringResource(R.string.contact_address_hint),
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
                            .semantics {
                                contentDescription = "Scan wallet address QR"
                                role = Role.Button
                            },
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

            // ADD CONTACT CTA
            val keyboard = LocalSoftwareKeyboardController.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(c.accent, RectangleShape)
                    .clickable(onClick = {
                        val name = nameInput.text.trim()
                        val mk = messagingKeyInput.text.trim().removePrefix("0x")
                        val wallet = walletAddressInput.text.trim()
                        val addrs = buildMap {
                            if (transparentAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_TRANSPARENT, transparentAddr.text.trim())
                            if (evmAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_EVM, evmAddr.text.trim())
                            if (solanaAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_SOLANA, solanaAddr.text.trim())
                        }
                        when {
                            name.isEmpty() -> error = "Name is required"
                            else -> {
                                keyboard?.hide()
                                onAdd(name, mk, wallet, addrs)
                            }
                        }
                    })
                    .semantics { contentDescription = "Add Contact"; role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(R.string.add_new_contact_primary_btn).uppercase(),
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

// ── Edit Contact bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun EditContactSheet(
    editData: EditContactData,
    scannedAddress: String?,
    onConsumeScannedAddress: () -> Unit,
    scanTargetField: String?,
    onScanAddrField: (addrType: String) -> Unit,
    onConsumeScanTarget: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, walletAddress: String, walletAddresses: Map<String, String>) -> Unit,
    onDelete: () -> Unit,
) {
    val c = ZappTheme.colors
    var nameInput by remember { mutableStateOf(TextFieldValue(editData.originalName)) }
    var walletAddressInput by remember { mutableStateOf(TextFieldValue(editData.originalAddress)) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAdditionalAddresses by remember {
        mutableStateOf(editData.walletAddresses.isNotEmpty())
    }

    // Additional wallet address fields
    var transparentAddr by remember {
        mutableStateOf(TextFieldValue(editData.walletAddresses[AddressBookContact.ADDR_TYPE_TRANSPARENT].orEmpty()))
    }
    var evmAddr by remember {
        mutableStateOf(TextFieldValue(editData.walletAddresses[AddressBookContact.ADDR_TYPE_EVM].orEmpty()))
    }
    var solanaAddr by remember {
        mutableStateOf(TextFieldValue(editData.walletAddresses[AddressBookContact.ADDR_TYPE_SOLANA].orEmpty()))
    }

    // Route scanned address to the target field
    LaunchedEffect(scannedAddress, scanTargetField) {
        if (scannedAddress != null && scanTargetField != null) {
            val tfv = TextFieldValue(scannedAddress)
            when (scanTargetField) {
                AddressBookContact.ADDR_TYPE_TRANSPARENT -> transparentAddr = tfv
                AddressBookContact.ADDR_TYPE_EVM -> evmAddr = tfv
                AddressBookContact.ADDR_TYPE_SOLANA -> solanaAddr = tfv
            }
            showAdditionalAddresses = true
            onConsumeScannedAddress()
            onConsumeScanTarget()
        }
    }

    fun collectWalletAddresses(): Map<String, String> = buildMap {
        if (transparentAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_TRANSPARENT, transparentAddr.text.trim())
        if (evmAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_EVM, evmAddr.text.trim())
        if (solanaAddr.text.isNotBlank()) put(AddressBookContact.ADDR_TYPE_SOLANA, solanaAddr.text.trim())
    }

    val hasAddrChanges =
        transparentAddr.text.trim() != editData.walletAddresses[AddressBookContact.ADDR_TYPE_TRANSPARENT].orEmpty() ||
        evmAddr.text.trim() != editData.walletAddresses[AddressBookContact.ADDR_TYPE_EVM].orEmpty() ||
        solanaAddr.text.trim() != editData.walletAddresses[AddressBookContact.ADDR_TYPE_SOLANA].orEmpty()

    val hasChanges = nameInput.text.trim() != editData.originalName ||
        walletAddressInput.text.trim() != editData.originalAddress ||
        hasAddrChanges
    val isValid = nameInput.text.isNotBlank() &&
        (walletAddressInput.text.isNotBlank() || editData.originalAddress.isBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RectangleShape,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            BasicText(
                text = stringResource(R.string.address_book_edit_contact_title),
                style = ZappTheme.typography.screenTitle.copy(
                    color = c.text,
                    fontWeight = FontWeight.Black,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Name field
            ZappInputField(
                value = nameInput,
                onValueChange = { nameInput = it; error = null },
                placeholder = stringResource(R.string.contact_name_hint),
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

            // Primary Wallet Address field
            ZappInputField(
                value = walletAddressInput,
                onValueChange = { walletAddressInput = it; error = null },
                placeholder = stringResource(R.string.contact_address_hint),
                leadingIcon = {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = c.textSubtle,
                    )
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

            // Additional Addresses toggle
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
                            text = stringResource(R.string.address_book_edit_delete_confirm_title),
                            style = ZappTheme.typography.rowTitle.copy(
                                color = c.danger,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            text = stringResource(R.string.address_book_edit_delete_confirm_message),
                            style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted),
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Cancel
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
                                    text = stringResource(R.string.address_book_edit_delete_confirm_no).uppercase(),
                                    style = ZappTheme.typography.button.copy(
                                        color = c.text,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                    ),
                                )
                            }
                            // Confirm delete
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
                                    text = stringResource(R.string.address_book_edit_delete_confirm_yes).uppercase(),
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
                Spacer(Modifier.height(12.dp))
            }

            // SAVE CHANGES CTA
            if (!showDeleteConfirm) {
                val keyboard = LocalSoftwareKeyboardController.current
                val saveEnabled = hasChanges && isValid
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
                                    val wallet = walletAddressInput.text.trim()
                                    when {
                                        name.isEmpty() -> error = "Name is required"
                                        else -> {
                                            keyboard?.hide()
                                            onSave(name, wallet, collectWalletAddresses())
                                        }
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
                        text = stringResource(R.string.address_book_edit_save_btn).uppercase(),
                        style = ZappTheme.typography.button.copy(
                            color = if (saveEnabled) c.onAccent else c.textSubtle,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // DELETE CONTACT button
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
                        text = stringResource(R.string.address_book_edit_delete_btn).uppercase(),
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

// ── List components ──────────────────────────────────────────────────────────

@Composable
private fun ZappSectionTitle(
    state: AddressBookItem.Title,
    modifier: Modifier = Modifier
) {
    BasicText(
        modifier = modifier,
        text = state.title.getValue().uppercase(),
        style = ZappTheme.typography.groupLabel.copy(
            color = ZappTheme.colors.textMuted,
            fontWeight = FontWeight.Black,
        ),
    )
}

@Composable
private fun EmptyItem(modifier: Modifier = Modifier) {
    val c = ZappTheme.colors
    Box(
        modifier = modifier
            .border(1.dp, c.border, RectangleShape)
            .padding(horizontal = 20.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(id = R.string.address_book_empty),
            style = ZappTheme.typography.rowTitle.copy(
                color = c.text,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun ZappContactRow(
    state: ContactListItemState,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = c.accent),
                onClick = state.onClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ZappContactAvatar(state = state)
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = state.name.getValue(),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = state.address.getValue(),
                style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ZappContactAvatar(
    state: ContactListItemState,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    val icon = state.bigIcon
    val displayText = if (icon is ImageResource.DisplayString) icon.value else "?"
    val textColor = if (icon is ImageResource.DisplayString) c.text else c.textMuted
    Box(
        modifier = modifier
            .size(40.dp)
            .background(c.surfaceAlt, RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = displayText,
            style = ZappTheme.typography.rowTitle.copy(
                color = textColor,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}

// ── Additional Wallet Addresses section ──────────────────────────────────────

@Composable
internal fun WalletAddressesSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    transparentAddr: TextFieldValue,
    onTransparentChange: (TextFieldValue) -> Unit,
    evmAddr: TextFieldValue,
    onEvmChange: (TextFieldValue) -> Unit,
    solanaAddr: TextFieldValue,
    onSolanaChange: (TextFieldValue) -> Unit,
    onScanAddress: ((addrType: String) -> Unit)? = null,
) {
    val c = ZappTheme.colors

    // Toggle header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .semantics {
                contentDescription = "Additional addresses"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 3dp accent stripe
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(c.accent, RectangleShape),
        )
        Spacer(Modifier.width(12.dp))
        BasicText(
            text = stringResource(R.string.address_book_additional_addresses).uppercase(),
            style = ZappTheme.typography.eyebrow.copy(
                color = c.accent,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            ),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            text = if (expanded) "−" else "+",
            style = ZappTheme.typography.button.copy(
                color = c.accent,
                fontWeight = FontWeight.Black,
            ),
        )
    }

    if (expanded) {
        Spacer(Modifier.height(12.dp))

        WalletAddressField(
            label = stringResource(R.string.address_book_addr_transparent),
            placeholder = stringResource(R.string.address_book_addr_transparent_hint),
            value = transparentAddr,
            onValueChange = onTransparentChange,
            onScan = onScanAddress?.let { { it(AddressBookContact.ADDR_TYPE_TRANSPARENT) } },
            validation = validateTransparentAddress(transparentAddr.text),
        )

        Spacer(Modifier.height(10.dp))

        WalletAddressField(
            label = stringResource(R.string.address_book_addr_evm),
            placeholder = stringResource(R.string.address_book_addr_evm_hint),
            value = evmAddr,
            onValueChange = onEvmChange,
            onScan = onScanAddress?.let { { it(AddressBookContact.ADDR_TYPE_EVM) } },
            validation = validateEvmAddress(evmAddr.text),
        )

        Spacer(Modifier.height(10.dp))

        WalletAddressField(
            label = stringResource(R.string.address_book_addr_solana),
            placeholder = stringResource(R.string.address_book_addr_solana_hint),
            value = solanaAddr,
            onValueChange = onSolanaChange,
            onScan = onScanAddress?.let { { it(AddressBookContact.ADDR_TYPE_SOLANA) } },
            validation = validateSolanaAddress(solanaAddr.text),
        )
    }
}

/** Validation result for a wallet address field. */
internal enum class AddrValidation { EMPTY, VALID, INVALID }

internal fun validateUnifiedAddress(addr: String): AddrValidation {
    if (addr.isBlank()) return AddrValidation.EMPTY
    val t = addr.trim()
    return if (t.startsWith("u1") && t.length >= 78) AddrValidation.VALID else AddrValidation.INVALID
}

internal fun validateTransparentAddress(addr: String): AddrValidation {
    if (addr.isBlank()) return AddrValidation.EMPTY
    val t = addr.trim()
    return if ((t.startsWith("t1") || t.startsWith("t3")) && t.length in 34..36) AddrValidation.VALID else AddrValidation.INVALID
}

internal fun validateEvmAddress(addr: String): AddrValidation {
    if (addr.isBlank()) return AddrValidation.EMPTY
    val t = addr.trim()
    return if (t.startsWith("0x") && t.length == 42 && t.drop(2).all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        AddrValidation.VALID
    } else {
        AddrValidation.INVALID
    }
}

internal fun validateSolanaAddress(addr: String): AddrValidation {
    if (addr.isBlank()) return AddrValidation.EMPTY
    val t = addr.trim()
    return if (t.length in 32..44 && t.all { it.isLetterOrDigit() }) AddrValidation.VALID else AddrValidation.INVALID
}

@Composable
internal fun WalletAddressField(
    label: String,
    placeholder: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onScan: (() -> Unit)? = null,
    validation: AddrValidation = AddrValidation.EMPTY,
) {
    val c = ZappTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = label.uppercase(),
            style = ZappTheme.typography.mono.copy(
                color = c.textMuted,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            ),
        )
        Spacer(Modifier.height(4.dp))
        ZappInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            leadingIcon = {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (validation) {
                        AddrValidation.VALID -> c.success
                        AddrValidation.INVALID -> c.danger
                        AddrValidation.EMPTY -> c.textSubtle
                    },
                )
            },
            trailingIcon = if (onScan != null) {
                {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onScan)
                            .semantics {
                                contentDescription = "Scan $label QR"
                                role = Role.Button
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = c.textSubtle,
                        )
                    }
                }
            } else null,
        )
        // Validation feedback
        if (validation == AddrValidation.INVALID) {
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = stringResource(R.string.contact_address_error_invalid),
                style = ZappTheme.typography.caption.copy(color = c.danger),
            )
        } else if (validation == AddrValidation.VALID) {
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = c.success,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = "${value.text.trim().take(8)}…${value.text.trim().takeLast(6)}",
                    style = ZappTheme.typography.chip.copy(color = c.success),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewScreens
@Composable
private fun AddressBookDataPreview() {
    ZcashTheme {
        AddressBookView(
            state =
                AddressBookState(
                    isLoading = false,
                    onBack = {},
                    items =
                        listOf(
                            AddressBookItem.Title(stringRes("Title")),
                            AddressBookItem.Contact(
                                ContactListItemState(
                                    name = stringRes("Name Surname"),
                                    address = stringRes("3iY5ZSkRnevzSMu4hosasdasdasdasd12312312dasd9hw2").withStyle(),
                                    bigIcon = imageRes("NS"),
                                    smallIcon = null,
                                    isShielded = false,
                                    onClick = {}
                                )
                            ),
                        ),
                    scanButton = ButtonState(text = stringRes("Scan")),
                    manualButton = ButtonState(text = stringRes("Manual")),
                    title = stringRes("Address book"),
                    info = null,
                    onSaveNewContact = { _, _, _, _ -> },
                ),
        )
    }
}

@PreviewScreens
@Composable
private fun EmptyAddressBookPreview() {
    ZcashTheme {
        AddressBookView(
            state =
                AddressBookState(
                    isLoading = false,
                    onBack = {},
                    items = emptyList(),
                    scanButton = ButtonState(text = stringRes("Scan")),
                    manualButton = ButtonState(text = stringRes("Manual")),
                    title = stringRes("Select Recipient"),
                    info = null,
                    onSaveNewContact = { _, _, _, _ -> },
                ),
        )
    }
}

@PreviewScreens
@Composable
private fun LoadingPreview() {
    ZcashTheme {
        AddressBookView(
            state =
                AddressBookState(
                    isLoading = true,
                    onBack = {},
                    items = emptyList(),
                    scanButton = ButtonState(text = stringRes("Scan")),
                    manualButton = ButtonState(text = stringRes("Manual")),
                    title = stringRes("Select Recipient"),
                    info = null,
                ),
        )
    }
}
