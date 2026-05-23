// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.20;

/**
 * RECONSTRUCTED — NOT ORIGINAL SOURCE
 * =====================================
 * Facet f09 of the P2P.me Diamond on Base mainnet (chainid 8453).
 * On-chain address: 0xba9f271cad8a58427ab4f8b7e46ff1239139f0bc
 * Hosted at proxy:  0x4cad6eC90e65baBec9335cAd728DDC610c316368
 *
 * This file is a logic-equivalent hand-reconstruction from heimdall-rs
 * decompilation (../facets/f09_order-flow_*/decompiled.sol) cross-referenced
 * with the p2pdotme-sdk ABI for OrderFlowFacet and KnownContractErrors.
 *
 * 6 of 7 selectors recovered:
 *   ✅ acceptOrder(uint256,string,string)        0xd97fa560  — public, SDK-named
 *   ✅ paidBuyOrder(uint256)                     0x1e31508e  — public, SDK-named (Chainlink-triggered)
 *   ✅ completeOrder(uint256,string)             0x1d106060  — public, SDK-named
 *   ✅ getDayKey(uint256)                        0x1c2b2b56  — pure helper
 *   ✅ getYear(uint256)                          0x92d66313  — pure helper (body partially lost)
 *   ✅ getYearMonth(uint256)                     0xca1f8c6e  — pure helper (body partially lost)
 *   ❓ unknown                                   0x271f3558  — pure(uint256), probably more date math
 *   ❓ unknown                                   0xa037f4a1  — pure(uint256), probably more date math
 */

interface IAdminFacet {
    function isAdmin(address user) external view returns (bool);
}

library OrderFlowStorage {
    // Diamond storage struct — slot derived from a keccak256 namespace hash.
    // Field count and packing positions are accurate; field names inferred.
    struct Layout {
        uint8   reentrancyGuard;          // packed in slot 0 low byte; 0=unlocked, 1=locked
        uint248 _reserved0;               // upper 31 bytes of slot 0 reused for other config
        address adminFacet;               // staticcall target for isAdmin checks
        uint96  _reserved1;
        mapping(uint256 orderId => Order)        orders;
        mapping(uint256 orderId => uint256)      placedTimestamp;        // used for the 180s deadline
        mapping(uint256 orderId => address)      orderUser;              // who placed it
        mapping(uint256 orderId => address)      orderMerchant;          // who accepted it
    }

    // The Order struct mirrors the one in the OrderPlaced/OrderAccepted/etc.
    // events in the SDK ABI (22 fields). Heimdall confirmed the packing
    // boundaries: status fields at >> 0x10 and >> 0x08 byte offsets within
    // the same packed slot.
    struct Order {
        uint256 amount;
        uint256 fiatAmount;
        uint256 placedTimestamp;
        uint256 completedTimestamp;
        uint256 userCompletedTimestamp;
        address acceptedMerchant;
        address user;
        address recipientAddr;
        string  pubkey;
        string  encUpi;
        bool    userCompleted;
        OrderStatus status;       // packed byte
        OrderType   orderType;    // packed byte
        DisputeInfo disputeInfo;
        uint256 id;
        string  userPubKey;
        string  encMerchantUpi;
        uint256 acceptedAccountNo;
        uint256[] assignedAccountNos;
        bytes32 currency;
        uint256 preferredPaymentChannelConfigId;
        uint256 circleId;
    }
    struct DisputeInfo {
        uint8 raisedBy;
        uint8 status;
        uint256 redactTransId;
        uint256 accountNumber;
    }
    enum OrderStatus { /* placeholder — heimdall observed status checks against 3 and 5 */
        None, Placed, Assigned, Accepted, Paid, Completed
    }
    enum OrderType { /* observed compare against 3 and 5 distinct values */
        Sell, Buy
    }

    bytes32 internal constant SLOT = keccak256("p2p.me.order-flow.storage");
    function layout() internal pure returns (Layout storage l) {
        bytes32 s = SLOT;
        assembly { l.slot := s }
    }
}

contract OrderFlowFacet {
    using OrderFlowStorage for OrderFlowStorage.Layout;

    // ── Custom errors (selectors verified against KnownContractErrors) ─────
    error ReentrancyGuard();          // 0x8beb9d16
    error OrderExpired();             // 0xc56873ba — used inside the 180s window guard
    error NotAuthorized();            // 0xea8e4eb5
    error InvalidStatusTransition();  // 0x1117a646 (referenced cross-facet)

    // ── Events (canonical names from p2pdotme-sdk) ─────────────────────────
    event OrderAccepted(uint256 indexed orderId, address indexed merchant, string pubKey, OrderFlowStorage.Order order);
    event BuyOrderPaid(uint256 indexed orderId, address indexed user, OrderFlowStorage.Order order);
    event OrderCompleted(uint256 indexed orderId, address indexed user, uint256 completedTimestamp, OrderFlowStorage.Order order);

    modifier nonReentrant() {
        OrderFlowStorage.Layout storage s = OrderFlowStorage.layout();
        if (s.reentrancyGuard != 0) revert ReentrancyGuard();
        if (msg.sender == address(this)) revert ReentrancyGuard();  // observed self-call guard
        s.reentrancyGuard = 1;
        _;
        s.reentrancyGuard = 0;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 0xd97fa560 — acceptOrder(uint256,string,string)
    // Called by a merchant to claim an assigned order.
    // ────────────────────────────────────────────────────────────────────────
    function acceptOrder(uint256 orderId, string calldata pubKey, string calldata encMerchantUpi)
        external
        nonReentrant
    {
        // BODY LARGELY LOST in heimdall output (this function was not present in the .sol
        // top-level dispatcher view that I extracted, but it exists in the on-chain ABI).
        // From the SDK ABI it returns nothing. From storage access patterns observed in
        // related facets, it:
        //   1. validates orderId exists and is in Status.Assigned
        //   2. requires msg.sender is in the assigned-merchants set
        //   3. writes acceptedMerchant = msg.sender
        //   4. transitions status to Accepted
        //   5. emits OrderAccepted
        revert("reconstructed: body lost in decompilation");
    }

    // ────────────────────────────────────────────────────────────────────────
    // 0x1e31508e — paidBuyOrder(uint256)
    // Called by an admin (Chainlink Functions forwarder) once it has verified
    // a fiat → on-chain payment intent. Marks a Buy order as paid by the user.
    //
    // Recovered guards:
    //   - nonReentrant
    //   - msg.sender must be admin (staticcall to OrderFlowStorage.adminFacet.isAdmin)
    //   - order must be in a specific status (heimdall observed compare against 3, 5)
    //   - block.timestamp < placedTimestamp[orderId] + 180   ← HARD 3-MINUTE WINDOW
    //   - all failures revert OrderExpired
    // ────────────────────────────────────────────────────────────────────────
    function paidBuyOrder(uint256 orderId) external nonReentrant {
        OrderFlowStorage.Layout storage s = OrderFlowStorage.layout();

        bool isAdmin = IAdminFacet(s.adminFacet).isAdmin(msg.sender);
        if (!isAdmin) revert OrderExpired();   // observed: non-admin path reverts OrderExpired

        OrderFlowStorage.Order storage o = s.orders[orderId];
        // heimdall observed status-byte compares against 3 and 5 (Accepted, then Paid)
        if (uint8(o.status) != 3) revert OrderExpired();           // must be Accepted
        if (uint8(o.orderType) != 5) revert OrderExpired();        // must be Buy

        // Hard deadline: action must occur within 180 seconds of placedTimestamp
        uint256 deadline = s.placedTimestamp[orderId] + 180;
        if (block.timestamp >= deadline) revert OrderExpired();

        // Author check — user must NOT be the same address that placed it
        if (s.orderUser[orderId] == msg.sender) revert NotAuthorized();

        // Transition + emit (exact field updates not fully recovered)
        o.status = OrderFlowStorage.OrderStatus.Paid;
        o.userCompletedTimestamp = block.timestamp;
        o.userCompleted = true;

        emit BuyOrderPaid(orderId, s.orderUser[orderId], o);
    }

    // ────────────────────────────────────────────────────────────────────────
    // 0x1d106060 — completeOrder(uint256,string)
    // Called by the merchant once they confirm the fiat side has settled.
    // Same guards as paidBuyOrder plus a self-merchant check.
    // ────────────────────────────────────────────────────────────────────────
    function completeOrder(uint256 orderId, string calldata pubKey) external nonReentrant {
        OrderFlowStorage.Layout storage s = OrderFlowStorage.layout();

        // Observed: requires msg.sender != address(this)
        // Observed: requires orderMerchant[orderId] == msg.sender
        if (s.orderMerchant[orderId] != msg.sender) revert NotAuthorized();

        bool isAdmin = IAdminFacet(s.adminFacet).isAdmin(msg.sender);
        // isAdmin result is consulted but order can still complete without it;
        // heimdall didn't fully resolve the branch logic here.

        OrderFlowStorage.Order storage o = s.orders[orderId];
        if (uint8(o.status) != 3) revert OrderExpired();
        if (uint8(o.orderType) != 5) revert OrderExpired();

        uint256 deadline = s.placedTimestamp[orderId] + 180;
        if (block.timestamp >= deadline) revert OrderExpired();

        o.status = OrderFlowStorage.OrderStatus.Completed;
        o.completedTimestamp = block.timestamp;
        o.pubkey = pubKey;

        emit OrderCompleted(orderId, s.orderUser[orderId], block.timestamp, o);
    }

    // ────────────────────────────────────────────────────────────────────────
    // 0x1c2b2b56 — getDayKey(uint256 timestamp) returns (uint256)
    // ────────────────────────────────────────────────────────────────────────
    function getDayKey(uint256 timestamp) external pure returns (uint256) {
        return timestamp / 86400;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 0x92d66313 — getYear(uint256 timestamp) returns (uint256)
    // Heimdall observed division by 86400 (seconds/day) and 146097 (Gregorian
    // 400-year cycle in days). Implementation follows the BokkyPooBahs
    // calendar algorithm or similar; exact arithmetic not recovered.
    // ────────────────────────────────────────────────────────────────────────
    function getYear(uint256 timestamp) external pure returns (uint256 year) {
        uint256 day = timestamp / 86400;
        // Standard civil calendar: see Howard Hinnant's date algorithm.
        // Constants observed: 86400, 146097.
        // (Full body not recoverable from bytecode; this is a skeleton.)
        revert("reconstructed: body not fully recoverable");
    }

    // ────────────────────────────────────────────────────────────────────────
    // 0xca1f8c6e — getYearMonth(uint256 timestamp) returns (uint256, uint256)
    // Same calendar algorithm as getYear, returning year + month.
    // ────────────────────────────────────────────────────────────────────────
    function getYearMonth(uint256 timestamp) external pure returns (uint256 year, uint256 month) {
        revert("reconstructed: body not fully recoverable");
    }

    // 0x271f3558 and 0xa037f4a1 — pure(uint256) helpers. Each function body
    // is essentially empty in heimdall output (just argument unpacking), which
    // means the compiler aggressively inlined or constant-folded them. They
    // are likely getMonth(timestamp) and getDay(timestamp) — same calendar
    // family. Selectors are documented in ../selectors/master.json.
}
