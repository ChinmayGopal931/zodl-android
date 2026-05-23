// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.20;

/**
 * RECONSTRUCTED — NOT ORIGINAL SOURCE
 * =====================================
 * Facet f12 of the P2P.me Diamond on Base mainnet.
 * On-chain address: 0x2fd2b520ffd8593d3f4925207c48929056f4c0a7
 * Hosted at proxy:  0x4cad6eC90e65baBec9335cAd728DDC610c316368
 *
 * Largest facet by selector count after the getters: **49 admin setters**.
 * Most are thin: super-admin gate → write one storage slot → emit one event.
 * Recovered from heimdall (../facets/f12_admin-config_*/decompiled.sol) +
 * KnownContractErrors.kt + master.json.
 *
 * Naming key:
 *   ✅ — canonically named (OpenChain or p2pdotme-sdk)
 *   🔹 — semantically inferred from storage slot + event + caller context
 *   ❓ — selector + arg types known, but purpose opaque (cataloged as pass-throughs)
 */

library AdminStorage {
    struct Layout {
        // Slot 0 — packed config + super-admin map base
        mapping(address admin => bool) superAdmins;          // storage_map_a — bytes1 flag per address
        address expectedExchangeOwner;                       // store_c — set by 0x0f3e94d5, cleared by 0x4f619ba4
        address chainlinkForwarder;                          // store_d — setChainlinkForwarder
        mapping(uint256 => uint256) merchantStakes;          // storage_map_e — written by setMinStake / similar
        address functionsRouter;                             // store_f — setFunctionsRouter
        address oracleConsumer;                              // store_h — set by 0xb8fa10f7
        uint16  someBpsConfig;                               // store_b — setMaxBuyTxLimit?(uint16) (per master.json: setMaxBuyTxLimit took uint256; this one is uint16 — likely a different cap)
        uint24  cashbackBps;                                 // store_g — bounded > 1000 (0x03e8) by setter 0x2e692c3e
        address expectedWorkflowOwner;                       // store_s — setExpectedWorkflowOwner
        address pendingExchangeOwner;                        // store_v — set by 0xa9e66a67, finalized via 0xaa050846
        bool    exchangeStatus;                              // store_t — toggleExchangeStatus
        uint256 priceCurveSize;                              // store_p — referenced in updateAdmin batch validations

        // Mappings indexed by various ids — exact key shape is map-dependent
        mapping(uint256 => uint256) configMapI;              // storage_map_i — used as id-lookup intermediate
        mapping(uint256 => bytes32) configMapJ;              // storage_map_j — packed merchant detail
        mapping(uint256 => bytes32) configMapK;              // storage_map_k
        mapping(uint256 => bytes32) configMapL;              // storage_map_l
        mapping(uint256 => bytes32) configMapM;              // storage_map_m
        mapping(uint256 => bytes32) configMapN;              // storage_map_n
        mapping(uint256 => bytes32) configMapO;              // storage_map_o
        mapping(uint256 => bytes32) configMapQ;              // storage_map_q
        mapping(uint256 => bytes32) configMapR;              // storage_map_r
        mapping(uint256 => bytes32) configMapU;              // storage_map_u
    }
    bytes32 internal constant SLOT = keccak256("p2p.me.admin-config.storage");
    function layout() internal pure returns (Layout storage l) {
        bytes32 s = SLOT;
        assembly { l.slot := s }
    }
}

contract AdminConfigFacet {
    // ── Custom errors (verified against KnownContractErrors / OpenChain) ──
    error NotSuperAdmin();           // 0x16c726b1
    error NotAdmin();                // 0x7bfa4b9f
    error ZeroAddress();             // 0xd92e233d
    error InvalidAddress();          // 0xe6c4247b
    error InvalidInput();            // 0xb4fa3fb3
    error BatchTooLarge();           // 0xbb1cb70b
    error ArrayLengthMismatch();     // 0xa24a13a6
    error NotAuthorized();           // 0xea8e4eb5
    error NotAContract();            // 0x09ee12d5
    error Unknown_883e365f();        // 0x883e365f — used by 0xa9e66a67 (queueExchangeOwner)

    // ── Events (some canonical, most synthesized from emit-site patterns) ─
    event SuperAdminUpdated(address indexed who, bool added);                                          // 0x65592336
    event Unknown_e7a8ccaa(uint16 value);                                                              // 0xe7a8ccaa
    event FunctionsRouterUpdated(address indexed router);                                              // 0x471c950e
    event Unknown_f1277a22(uint256 id);                                                                // 0xf1277a22
    event ExpectedWorkflowOwnerUpdated(address indexed previous, address indexed current);             // 0xb1...
    event Unknown_0067245d(address indexed previous, address indexed current);                         // 0x0067245d — pending → confirmed exchange owner
    event UpdatedExchangeStatus(address indexed actor, bool previous, bool current);                   // (canonical)
    event Unknown_cc790d80(/* variadic — heimdall observed two arities, indeterminate */);             // 0xcc790d80
    event Unknown_000fc11d(uint24 cashbackBps);                                                        // 0x000fc11d
    event Unknown_f96b30e3(address indexed actor, uint256 id, uint256 status);                         // 0xf96b30e3
    event Unknown_60f48695(address indexed previous, address indexed current);                         // 0x60f48695 — expectedExchangeOwner update
    event Unknown_6880bcb1(/* 6-arity payment-channel snapshot */);                                    // 0x6880bcb1
    event Unknown_68a9923f(address indexed forwarder);                                                 // 0x68a9923f — chainlinkForwarder

    modifier onlySuperAdmin() {
        AdminStorage.Layout storage s = AdminStorage.layout();
        if (!s.superAdmins[msg.sender]) revert NotSuperAdmin();
        _;
    }

    // ════════════════════════════════════════════════════════════════════════
    // RECOGNIZED SETTERS — full reconstructions
    // ════════════════════════════════════════════════════════════════════════

    /// @custom:selector 0xd02c21b1 — setChainlinkForwarder(address)
    function setChainlinkForwarder(address forwarder) external onlySuperAdmin {
        if (forwarder == address(0)) revert InvalidAddress();
        AdminStorage.layout().chainlinkForwarder = forwarder;
        emit Unknown_68a9923f(forwarder);
    }

    /// @custom:selector 0xb79ea77f — setFunctionsRouter(address)
    function setFunctionsRouter(address router) external onlySuperAdmin {
        AdminStorage.layout().functionsRouter = router;
        emit FunctionsRouterUpdated(router);
    }

    /// @custom:selector 0x26bbf2ae — setExpectedWorkflowOwner(address)
    function setExpectedWorkflowOwner(address newOwner) external onlySuperAdmin {
        if (newOwner == address(0)) revert InvalidAddress();
        AdminStorage.Layout storage s = AdminStorage.layout();
        address previous = s.expectedWorkflowOwner;
        s.expectedWorkflowOwner = newOwner;
        emit ExpectedWorkflowOwnerUpdated(previous, newOwner);
    }

    /// @custom:selector 0x2952c715 — toggleExchangeStatus()
    function toggleExchangeStatus() external onlySuperAdmin {
        AdminStorage.Layout storage s = AdminStorage.layout();
        bool prev = s.exchangeStatus;
        s.exchangeStatus = !prev;
        emit UpdatedExchangeStatus(msg.sender, prev, !prev);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 🔹 INFERRED SETTERS — naming based on storage slot + event signature
    // ════════════════════════════════════════════════════════════════════════

    /// @custom:selector 0x9e54503b — addSuperAdmin(address)  [inferred from emit(addr, true)]
    /// Writes 1 to superAdmins[uint256-cast-of-address] and emits the boolean-true variant.
    function addSuperAdmin(address who) external onlySuperAdmin {
        AdminStorage.layout().superAdmins[who] = true;
        emit SuperAdminUpdated(who, true);
    }

    /// @custom:selector 0x7cae13a1 — revokeSuperAdmin(address)  [inferred from emit(addr, false)]
    function revokeSuperAdmin(address who) external onlySuperAdmin {
        AdminStorage.layout().superAdmins[who] = false;
        emit SuperAdminUpdated(who, false);
    }

    /// @custom:selector 0x2e692c3e — setCashbackBps(uint24)  [inferred: bound > 1000 = max 99.999%]
    function setCashbackBps(uint24 bps) external onlySuperAdmin {
        if (bps <= 1000) revert InvalidInput();
        AdminStorage.layout().cashbackBps = bps;
        emit Unknown_000fc11d(bps);
    }

    /// @custom:selector 0xd7307fc7 — setSomeCap(uint16)  [purpose opaque; writes store_b, emits event with the value]
    function setSomeCap(uint16 value) external onlySuperAdmin {
        AdminStorage.layout().someBpsConfig = value;
        emit Unknown_e7a8ccaa(value);
    }

    /// @custom:selector 0x0f3e94d5 — setExpectedExchangeOwner(address)
    function setExpectedExchangeOwner(address newOwner) external onlySuperAdmin {
        if (newOwner == address(0)) revert ZeroAddress();
        AdminStorage.Layout storage s = AdminStorage.layout();
        address previous = s.expectedExchangeOwner;
        s.expectedExchangeOwner = newOwner;
        emit Unknown_60f48695(previous, newOwner);
    }

    /// @custom:selector 0xb8fa10f7 — setOracleConsumer(address)
    function setOracleConsumer(address newConsumer) external onlySuperAdmin {
        if (newConsumer == address(0)) revert InvalidAddress();
        AdminStorage.layout().oracleConsumer = newConsumer;
        // (no event emitted by this setter, per heimdall)
    }

    /// @custom:selector 0xa9e66a67 — queueExchangeOwnerTransfer(address)
    /// Writes a pending exchange owner; finalized when that address calls 0xaa050846.
    function queueExchangeOwnerTransfer(address pendingOwner) external onlySuperAdmin {
        if (pendingOwner == address(0)) revert Unknown_883e365f();
        AdminStorage.Layout storage s = AdminStorage.layout();
        if (s.pendingExchangeOwner != address(0)) revert Unknown_883e365f();
        s.pendingExchangeOwner = pendingOwner;
        emit Unknown_0067245d(address(0), pendingOwner);
    }

    /// @custom:selector 0xaa050846 — acceptExchangeOwnership()
    /// Two-step ownership handover: pending owner must call this from a contract address
    /// (NotAContract check) and match storage_map.expectedExchangeOwner.
    function acceptExchangeOwnership() external {
        AdminStorage.Layout storage s = AdminStorage.layout();
        if (msg.sender != s.expectedExchangeOwner) revert NotAuthorized();
        if (msg.sender.code.length == 0) revert NotAContract();
        s.pendingExchangeOwner = msg.sender;
        s.expectedExchangeOwner = address(0);
        emit Unknown_0067245d(s.pendingExchangeOwner, msg.sender);
    }

    /// @custom:selector 0x4f619ba4 — clearExpectedExchangeOwner()
    function clearExpectedExchangeOwner() external onlySuperAdmin {
        AdminStorage.Layout storage s = AdminStorage.layout();
        if (msg.sender == s.expectedExchangeOwner) revert NotAuthorized();
        s.expectedExchangeOwner = address(0);
        emit Unknown_60f48695(address(0), address(0));
    }

    /// @custom:selector 0x5bbcb659 — clearMerchantStake(uint256)  [inferred: writes storage_map_e[id] = 0]
    function clearMerchantStake(uint256 merchantId) external onlySuperAdmin {
        AdminStorage.layout().merchantStakes[merchantId] = 0;
        emit Unknown_f1277a22(merchantId);
    }

    /// @custom:selector 0xc1f91760 — setMerchantStakeStatus(uint256 id, uint256 status<4)
    /// Observed: requires status < 4, packs status into byte 2 of storage_map_e[id].
    function setMerchantStakeStatus(uint256 merchantId, uint256 status) external onlySuperAdmin {
        if (status >= 4) revert NotSuperAdmin(); // observed: reverts NOT_SUPER_ADMIN on out-of-range too
        AdminStorage.Layout storage s = AdminStorage.layout();
        uint256 prev = s.merchantStakes[merchantId];
        s.merchantStakes[merchantId] = (prev & ~(uint256(0xff) << 16)) | (status << 16);
        emit Unknown_f96b30e3(msg.sender, merchantId, status);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ❓ OPAQUE FUNCTIONS — selectors + arg types known, body not recoverable
    //
    // Most of these are pure-stub in heimdall output because the compiler
    // inlined them away or the optimizer eliminated branches. Selectors are
    // documented for cross-referencing; bodies revert with a sentinel.
    // ════════════════════════════════════════════════════════════════════════

    /// @custom:selector 0xad541c20 (in master.json as Unresolved)
    function unknown_ad541c20(uint256) external pure { revert("reconstructed: body lost"); }
    /// @custom:selector 0x6893c391 — known from master.json as setSellPrice(uint256)
    function setSellPrice(uint256) external pure { revert("reconstructed: body lost"); }
    /// @custom:selector 0x8b62fa8c — setBuyPrice(uint256)
    function setBuyPrice(uint256) external pure { revert("reconstructed: body lost"); }
    /// @custom:selector 0xed9f18fb — setMinStake(uint256)
    function setMinStake(uint256) external pure { revert("reconstructed: body lost"); }
    /// @custom:selector 0x0db354bc — setBaseSpread(uint256)
    function setBaseSpread(uint256) external pure { revert("reconstructed: body lost"); }
    /// @custom:selector 0x670a6fd9 — updateAdmin(address) [SDK shows (address,bool); heimdall recovered (address) only]
    function updateAdmin(address) external pure { revert("reconstructed: body lost"); }
    /// @custom:selector 0xebe3bb62 — setSuperAdmin(address) [from master.json]
    function setSuperAdmin(address) external pure { revert("reconstructed: body lost"); }
    /// @custom:selector 0x70a26ec4 — updateProcessingTime((uint256,uint256,uint256,uint256))
    function updateProcessingTime(uint256) external pure { revert("reconstructed: body lost"); }

    // ── Remaining 28 opaque selectors (uint256/address pass-throughs in heimdall)
    // 0x4f619ba4, 0x7cae13a1 already handled above. Others:
    // 0x67f75e85, 0x26f6d76a, 0xb80d8390, 0xf6067dd1, 0x68a39b5f(uint256,address),
    // 0x81fbf4d2(uint256,uint256,uint256,uint256) — multi-arg with array-length checks,
    // 0xd418f29a, 0xc8016fb9, 0x88c76f79(address,address), 0x89d2e8fa,
    // 0xcca6ba8a(uint256,uint256), 0xb66d6bcb(uint256,uint256), 0x134238dd, 0xa98fa0d4,
    // 0xa2db4ed9, 0xd122f407, 0x4457cd38, 0xcec5599c, 0x2177be25, 0x7e3f9d49,
    // 0x57bc58e1(uint256,uint256), 0x7b6a00d8(uint256,uint256), 0xfdb959bc, 0xdf4b1239,
    // 0x5f4c82d2(address), 0xcd26d379(address), 0x23962f1a(uint256), 0xa9e66a67 already handled.
    //
    // The complex multi-mapping flows in 0x5f4c82d2 and 0xcd26d379 (~50 lines of nested
    // storage access each) appear to be merchant onboarding / circle membership updates,
    // but the control flow is too tangled for confident reconstruction. See raw heimdall
    // output in ../facets/f12_admin-config_*/decompiled.sol.
}
