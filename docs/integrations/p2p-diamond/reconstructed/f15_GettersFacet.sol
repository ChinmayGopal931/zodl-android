// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.20;

/**
 * RECONSTRUCTED — NOT ORIGINAL SOURCE
 * =====================================
 * Facet f15 of the P2P.me Diamond on Base mainnet.
 * On-chain address: 0x004b3e89344161b9cc793c745156a50e97407f4a
 * Hosted at proxy:  0x4cad6eC90e65baBec9335cAd728DDC610c316368
 *
 * **Largest facet: 82 view functions** (29% of the entire Diamond by selector
 * count). Reads-only — no state mutation.
 *
 * Reconstruction strategy: most functions are one-line storage reads, so the
 * useful artifact here is the **signature + return-type + storage-slot table**.
 * I've grouped them by what they read; functions returning structured data
 * (`getMerchantConfig`, `getOrdersById`, `getUser`, `getActiveLiquidity`,
 * `fetchMerchant*Orders`) are stubbed with their ABI-correct return signature.
 *
 * Legend:
 *   ✅ canonical (OpenChain or p2pdotme-sdk)
 *   🔹 inferred from return type + storage pattern
 *   ❓ selector + types known, storage role opaque
 */

library GettersStorage {
    // Heimdall observed 30+ distinct storage maps and 10+ standalone slots in
    // this facet. Most are shared with other facets via Diamond storage; the
    // names below reflect what each slot/map appears to *contain*, not the
    // original variable names.
    struct Layout {
        // Standalone packed slots
        uint256 nextOrderId;             // storage_map "getNextOrderId" (recovered)
        bool    exchangeStatus;          // getExchangeStatus
        uint16  assignedOrdersThreshold; // getAssignedOrdersThreshold (uint16)
        uint24  cashbackBps;             // getCashbackPercentage (uint24)
        uint256 orderExpiry;             // getOrderExpiry — appears to be the same 180s used in order-flow
        address chainlinkForwarder;      // getChainlinkForwarderAddress
        address expectedWorkflowOwner;   // getExpectedWorkflowOwner
        address functionsRouter;         // getFunctionsRouter
        uint256 currentPaymentChannelId; // getCurrentPaymentChannelId
        uint256 paymentChannelCount;     // currentPaymentChannelCount

        // Mappings — each `storage_map_X` from the decompile becomes one entry
        mapping(address => bool)    isAdminFlag;       // storage_map_a — read by isAdmin
        mapping(address => bool)    isSuperAdminFlag;  // separate bit / packed in same map
        mapping(address => bool)    isBlacklistedFlag;
        mapping(address => uint16)  merchantTier;
        mapping(address => uint256) merchantStake;     // storage_map_k — getMerchantStake
        mapping(address => uint256) userBalance;       // storage_map_b — getUserBalance (inferred)
        mapping(uint256 => address) orderMerchant;     // storage_map_c (orderId → merchant)
        mapping(bytes32 => uint256) marketPrice;       // getMarketPrice(bytes32 currency)
        mapping(bytes32 => uint256) totalStake;        // getTotalStake(bytes32 currency)
        mapping(bytes32 => uint256) smallOrderThreshold; // getSmallOrderThreshold(bytes32 currency)
        mapping(uint256 => uint256) misc_storage_map_a;  // generic id→value reads
        mapping(uint256 => bytes32) misc_storage_map_d;
        mapping(uint256 => bytes32) misc_storage_map_e;
        mapping(uint256 => bytes32) misc_storage_map_f;
        mapping(uint256 => bytes32) misc_storage_map_g;
        mapping(uint256 => bytes32) misc_storage_map_h;
        mapping(uint256 => bytes32) misc_storage_map_p;
        mapping(uint256 => bytes32) misc_storage_map_q;
        mapping(uint256 => bytes32) misc_storage_map_r;
    }
    bytes32 internal constant SLOT = keccak256("p2p.me.getters.storage");
    function layout() internal pure returns (Layout storage l) {
        bytes32 s = SLOT;
        assembly { l.slot := s }
    }
}

contract GettersFacet {
    using GettersStorage for GettersStorage.Layout;

    error CurrencyNotSupported();          // 0x02a6fdd2 — observed in getter 0x7e9aafc8

    // ════════════════════════════════════════════════════════════════════════
    // ✅ CANONICAL — full signatures from p2pdotme-sdk
    // ════════════════════════════════════════════════════════════════════════

    /// @custom:selector 0x24d7806c — isAdmin(address) returns (bool)
    function isAdmin(address user) external view returns (bool) {
        return GettersStorage.layout().isAdminFlag[user];
    }

    /// @custom:selector 0xdf7f453b — isSuperAdmin(address) returns (bool)
    function isSuperAdmin(address user) external view returns (bool) {
        return GettersStorage.layout().isSuperAdminFlag[user];
    }

    /// @custom:selector 0xfe575a87 — isBlacklisted(address) returns (bool)
    function isBlacklisted(address user) external view returns (bool) {
        return GettersStorage.layout().isBlacklistedFlag[user];
    }

    /// @custom:selector 0x46be235e — currentPaymentChannelCount() returns (uint256)
    function currentPaymentChannelCount() external view returns (uint256) {
        return GettersStorage.layout().paymentChannelCount;
    }

    /// @custom:selector 0xa5fcf5f4 — getCurrentPaymentChannelId() returns (uint256)
    function getCurrentPaymentChannelId() external view returns (uint256) {
        return GettersStorage.layout().currentPaymentChannelId;
    }

    /// @custom:selector 0x8158900b — getNextOrderId() returns (uint256)
    function getNextOrderId() external view returns (uint256) {
        return GettersStorage.layout().nextOrderId;
    }

    /// @custom:selector 0xb01dcb31 — getExchangeStatus() returns (bool)
    function getExchangeStatus() external view returns (bool) {
        return GettersStorage.layout().exchangeStatus;
    }

    /// @custom:selector 0x5fbfb4d2 — getCashbackPercentage() returns (uint24)
    function getCashbackPercentage() external view returns (uint24) {
        return GettersStorage.layout().cashbackBps;
    }

    /// @custom:selector 0x7fdf732d — getAssignedOrdersThreshold() returns (uint16)
    function getAssignedOrdersThreshold() external view returns (uint16) {
        return GettersStorage.layout().assignedOrdersThreshold;
    }

    /// @custom:selector 0x5097bbcc — getOrderExpiry() returns (uint256)
    function getOrderExpiry() external view returns (uint256) {
        return GettersStorage.layout().orderExpiry;
    }

    /// @custom:selector 0x81137634 — getChainlinkForwarderAddress() returns (address)
    function getChainlinkForwarderAddress() external view returns (address) {
        return GettersStorage.layout().chainlinkForwarder;
    }

    /// @custom:selector 0x6f0b0769 — getExpectedWorkflowOwner() returns (address)
    function getExpectedWorkflowOwner() external view returns (address) {
        return GettersStorage.layout().expectedWorkflowOwner;
    }

    /// @custom:selector 0x3395a79a — getFunctionsRouter() returns (address)
    function getFunctionsRouter() external view returns (address) {
        return GettersStorage.layout().functionsRouter;
    }

    /// @custom:selector 0x1f431fa6 — getMarketPrice(bytes32 currency) returns (uint256)
    function getMarketPrice(bytes32 currency) external view returns (uint256) {
        return GettersStorage.layout().marketPrice[currency];
    }

    /// @custom:selector 0x3234fef5 — getTotalStake(bytes32 currency) returns (uint256)
    function getTotalStake(bytes32 currency) external view returns (uint256) {
        return GettersStorage.layout().totalStake[currency];
    }

    /// @custom:selector 0x6b2d3913 — getSmallOrderThreshold(bytes32 currency) returns (uint256)
    function getSmallOrderThreshold(bytes32 currency) external view returns (uint256) {
        return GettersStorage.layout().smallOrderThreshold[currency];
    }

    /// @custom:selector 0xa60cb55f — getMerchantStake(address) returns (uint256)
    function getMerchantStake(address merchant) external view returns (uint256) {
        return GettersStorage.layout().merchantStake[merchant];
    }

    /// @custom:selector 0x59c69313 — isOrderExpired(uint256 orderId) returns (bool)
    /// Mirrors the same status+deadline guard used by paidBuyOrder/completeOrder in f09.
    function isOrderExpired(uint256 orderId) external view returns (bool) {
        GettersStorage.Layout storage s = GettersStorage.layout();
        // Observed: same packed-status checks (>>0x10, >>0x08) + 180s window
        uint256 packed = uint256(s.misc_storage_map_q[orderId]);
        uint8 status = uint8(packed >> 16);
        uint8 orderType = uint8(packed >> 8);
        if (status != 3 || orderType != 5) return false;
        uint256 placedTs = uint256(s.misc_storage_map_d[orderId]);
        return block.timestamp >= placedTs + 180;
    }

    // ── Functions returning structured data (return shape from SDK; body partial) ──

    /// @custom:selector 0x6f77926b — getUser(address) returns (UserRecord)
    function getUser(address user) external view returns (bytes memory) {
        // ABI: returns a packed User struct. Body involves dynamic string fields;
        // exact field layout from SDK ABI (not reproduced here for brevity).
        revert("reconstructed: structured-return body lost");
    }
    /// @custom:selector 0x93a42939 — getMerchantConfig(address) returns (MerchantConfig)
    function getMerchantConfig(address merchant) external view returns (bytes memory) {
        revert("reconstructed: structured-return body lost");
    }
    /// @custom:selector 0x4e6cdee4 — getMerchantDetails(address) returns (MerchantDetails)
    function getMerchantDetails(address merchant) external view returns (bytes memory) {
        revert("reconstructed: structured-return body lost");
    }
    /// @custom:selector 0xcea99cd6 — getOrdersById(uint256) returns (Order)
    function getOrdersById(uint256 orderId) external view returns (bytes memory) {
        // Returns the 22-field Order struct (see f09_OrderFlowFacet.sol for shape).
        revert("reconstructed: structured-return body lost");
    }
    /// @custom:selector 0xb677d43d — getAdditionalOrderDetails(uint256) returns (...)
    function getAdditionalOrderDetails(uint256 orderId) external view returns (bytes memory) {
        revert("reconstructed: structured-return body lost");
    }
    /// @custom:selector 0xdef7fe02 — getActiveLiquidity(bytes32 currency) returns (...)
    function getActiveLiquidity(bytes32 currency) external view returns (bytes memory) {
        revert("reconstructed: structured-return body lost");
    }
    /// @custom:selector 0x7d8f7243 — fetchMerchantAcceptedOrders(address) returns (Order[])
    function fetchMerchantAcceptedOrders(address merchant) external view returns (bytes memory) {
        // Observed: pagination cap of 0x64 (100 orders) in heimdall output
        revert("reconstructed: array-return body lost");
    }
    /// @custom:selector 0x4f843f8b — fetchMerchantAssignedOrders(address) returns (Order[])
    function fetchMerchantAssignedOrders(address merchant) external view returns (bytes memory) {
        revert("reconstructed: array-return body lost");
    }
    /// @custom:selector 0x9ba18bf8 — userOrdersArr(address) returns (uint256[])
    function userOrdersArr(address user) external view returns (uint256[] memory) {
        revert("reconstructed: array-return body lost");
    }

    /// @custom:selector 0x2681b36e — getPaymentChannelConfigs() returns (PaymentChannelConfig[])
    function getPaymentChannelConfigs() external view returns (bytes memory) {
        revert("reconstructed: structured-return body lost");
    }
    /// @custom:selector 0x0e759e32 — getProcessingTime() returns (ProcessingTime)
    function getProcessingTime() external view returns (bytes memory) { revert(""); }
    /// @custom:selector 0x25ba147d — getCashbackConfig() returns (CashbackConfig)
    function getCashbackConfig() external view returns (bytes memory) { revert(""); }

    // ════════════════════════════════════════════════════════════════════════
    // 🔹 INFERRED — simple single-slot reads. Signature recovered, semantic
    // name is a best guess from storage map + return type. Selectors are the
    // source of truth; cross-reference master.json.
    // ════════════════════════════════════════════════════════════════════════

    /// @custom:selector 0x30764388 — returns merchant tier byte for an address
    function _f15_30764388(address a) external view returns (uint16) { return GettersStorage.layout().merchantTier[a]; }

    /// @custom:selector 0x97575c8b — returns bool for a uint256 key (id-flag lookup)
    function _f15_97575c8b(uint256 id) external view returns (bool) { return GettersStorage.layout().misc_storage_map_a[id] != 0; }

    /// @custom:selector 0xd773a0fa — returns bool but body wraps an encoded read; arg is uint256
    function _f15_d773a0fa(uint256 id) external view returns (bool) { return GettersStorage.layout().misc_storage_map_e[id] != 0; }

    /// @custom:selector 0x60a25cbf — returns address keyed by uint256
    function _f15_60a25cbf(uint256 id) external view returns (address) { return address(uint160(uint256(GettersStorage.layout().misc_storage_map_a[id]))); }

    /// @custom:selector 0x7da6dacc — returns address keyed by uint256 (probably "orderMerchant" — SDK has fetchMerchantAcceptedOrders nearby)
    function _f15_7da6dacc(uint256 orderId) external view returns (address) { return GettersStorage.layout().orderMerchant[orderId]; }

    /// @custom:selector 0x99940a13 — returns uint256 keyed by address (userBalance / userScore)
    function _f15_99940a13(address a) external view returns (uint256) { return GettersStorage.layout().userBalance[a]; }

    // ════════════════════════════════════════════════════════════════════════
    // ❓ OPAQUE — selectors + arg + return type known; semantic role unclear.
    // All read from `misc_storage_map_a` or a sibling. Cross-reference the raw
    // heimdall output in ../facets/f15_getters_*/decompiled.sol for the exact
    // storage slot if you need to disambiguate.
    //
    // To keep this file scannable, the rest are documented as a one-line table:
    // ════════════════════════════════════════════════════════════════════════

    // selector    | arg(s)              | returns      | storage slot read
    // ----------------------------------------------------------------------
    // 0x1e277523  | uint256             | uint64       | misc_storage_map_a
    // 0x2a7f59df  | uint256             | uint256      | misc_storage_map_a
    // 0x3a29a687  | uint256             | uint256      | misc_storage_map_a
    // 0xd481b371  | address             | uint256      | misc_storage_map_a
    // 0xe7aa6770  | uint256             | uint256      | orderMerchant chain
    // 0x24fd71ab  | uint256             | uint256      | misc_storage_map_a
    // 0x164de6e4  | uint256             | uint64       | misc_storage_map_k
    // 0x6129f71a  | uint256             | uint256      | misc_storage_map_a
    // 0x63304e64  | uint256             | uint256      | misc_storage_map_a
    // 0x7608ccbf  | uint256             | bytes        | misc_storage_map_a (two-slot encode)
    // 0x925f1e1b  | uint256             | uint64       | misc_storage_map_a
    // 0x67c84efd  | uint256             | bytes        | dynamic struct read
    // 0xc472ec88  | uint256             | uint256      | misc_storage_map_a
    // 0x1aee9b21  | uint256             | uint256      | misc_storage_map_a
    // 0x6b2d3913  | uint256             | uint256      | misc_storage_map_a (≠ getSmallOrderThreshold above; different selector)
    // 0x7e9aafc8  | uint256             | uint256      | misc_storage_map_a (reverts CurrencyNotSupported if zero)
    // 0xad641d16  | uint256             | uint256      | misc_storage_map_a
    // 0x817cdd32  | uint256             | uint256      | misc_storage_map_a
    // 0x47991cb8  | address             | bytes        | dynamic struct read
    // 0xd4786e0e  | uint256             | uint256      | misc_storage_map_a
    // 0x695fbea3  | uint256             | uint64       | misc_storage_map_a
    // 0xc4e5de74  | address             | bool         | misc_storage_map_a
    // 0xdebaa93a  | address             | bool         | misc_storage_map_a
    // 0x929dfc27  | address             | bool         | misc_storage_map_a
    // 0xc3c2d5bf  | uint256             | bytes        | dynamic encode
    // 0x67391532  | address             | bytes        | merchant-detail dynamic read
    // 0x27bc9fac  | address             | bool         | storage_map_c (status byte check)
    // 0x587711ee  | uint256             | address      | orderMerchant→adminMap chain
    // 0x399ac68c  | uint256             | uint256      | misc_storage_map_a
    // 0x1b3e22fc  | uint256             | uint256      | misc_storage_map_a
    // 0x4bb031b7  | uint256             | (none)       | order validation read — reverts on mismatch
    // 0x432ed230  |                     | address      | standalone address slot (unresolved_432ed230)
    // 0x4ecbda32  |                     | address      | standalone address slot (unresolved_4ecbda32)
    // 0x432ed230  | (none)              | address      | "unresolved_432ed230" public state var
    //
    // — pure passthroughs (heimdall body is empty / just arg unpacking) —
    // 0x3ffe5c8a (address,uint256,uint256)
    // 0x98f21159 (address)
    // 0x36b0ec9a (uint256,uint256,uint256,address,uint256,uint256,uint256)
    // 0x55a8069d (address)
    // 0x9d866c4b (address)
    // 0x58ae4f57 (address)
    // 0x4e5aa977 (address,uint256)
    // 0xa7240588 (address)
    // 0xbed55ee0 (address)
    // 0xb85c25ae (address,uint256)
    // 0xe3a7e9ca (uint256)
    // 0xc6d114f1 (uint256,address,address,uint256,uint256,uint256)
    // 0xb8f48fe6 (address)
    // 0x0d5ad0fc (address)  -> bytes
    // 0xd0b5d99e -- not in this facet; skip
}
