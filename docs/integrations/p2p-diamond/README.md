# P2P.me Diamond — Decompiled Facet Catalog (Base mainnet)

Proxy: `0x4cad6eC90e65baBec9335cAd728DDC610c316368`
Generated: heimdall-rs v0.9.2 + OpenChain + p2pdotme-sdk ABIs
Total selectors: 307 across 20 facets

## Regenerating

1. `cargo install --locked --git https://github.com/Jon-Becker/heimdall-rs heimdall-cli`
2. Re-fetch the facet list: `cast call $DIAMOND "facets()" --rpc-url https://mainnet.base.org`
3. For each facet address, `eth_getCode` → pipe hex into `heimdall decompile -d -o <dir> -a <combined-abi> --include-sol <hex>`
4. `combined.abi.json` is built by merging `selectors/resolved.json` with the SDK ABIs in `~/dev/p2pdotme-sdk/src/contracts/abis` and `~/dev/user-app-client/src/core/p2pdotme/contracts/abis`

The Diamond is **upgradeable** (EIP-2535) — facet addresses and selector routing can change at any time without a new proxy address. Re-run after any `DiamondCut` event on the proxy.

- 89 canonically named (29%)
- 273 have heimdall-recovered argument types (89%)
- 289 have at least one of the above (94%)
- 18 selectors had no recoverable info — likely fallback/internal

## Facet #00 — `diamond-cut`  (1 selectors)
`0x35251169363ddccfe1ebdaef91a5f237ca4004e9`  →  `decompiled/f00_diamond-cut_0x35251169363ddccfe1ebdaef91a5f237ca4004e9/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x1f931c1c` | `diamondCut((address,uint8,bytes4[])[],address,bytes)` | `—` |

## Facet #01 — `diamond-loupe`  (5 selectors)
`0x51d5314052ab906d197d64f6c14e171b0d5fe6b5`  →  `decompiled/f01_diamond-loupe_0x51d5314052ab906d197d64f6c14e171b0d5fe6b5/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xcdffacc6` | `facetAddress(bytes4)` | `—` |
| `0x52ef6b2c` | `facetAddresses()` | `facetAddresses() → (bytes memory)` |
| `0xadfca15e` | `facetFunctionSelectors(address)` | `facetFunctionSelectors(address arg0) → (bytes memory)` |
| `0x7a0ed627` | `facets()` | `facets()` |
| `0x01ffc9a7` | `supportsInterface(bytes4)` | `supportsInterface(bytes4 arg0) → (bool)` |

## Facet #02 — `ownership`  (2 selectors)
`0x5d0d5cbbfa2ff20c7669618d4642074b4151a565`  →  `decompiled/f02_ownership_0x5d0d5cbbfa2ff20c7669618d4642074b4151a565/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x8da5cb5b` | `owner()` | `—` |
| `0xf2fde38b` | `transferOwnership(address)` | `—` |

## Facet #03 — `versioning`  (2 selectors)
`0x81790b161a50732c42b454d218142968cb36a345`  →  `decompiled/f03_versioning_0x81790b161a50732c42b454d218142968cb36a345/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xa0a8e460` | `contractVersion()` | `—` |
| `0x65b36b69` | `—` | `Unresolved_65b36b69(uint256 arg0)` |

## Facet #04 — `unknown-small`  (2 selectors)
`0xec41b57de682adf4e36bb1cd65fe6406f39a0b1d`  →  `decompiled/f04_unknown-small_0xec41b57de682adf4e36bb1cd65fe6406f39a0b1d/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xd0b5d99e` | `—` | `—` |
| `0x7206f373` | `—` | `Unresolved_7206f373(uint256 arg0, uint256 arg1, address arg2, uint256 arg3, uint` |

## Facet #05 — `staking-rewards`  (15 selectors)
`0x6568520adfa69c2c399d235bdb8e84157a7c735d`  →  `decompiled/f05_staking-rewards_0x6568520adfa69c2c399d235bdb8e84157a7c735d/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xae169a50` | `claimReward(uint256)` | `claimReward(uint256 arg0)` |
| `0xa0c7f71c` | `claimable(uint256,address)` | `Unresolved_a0c7f71c(uint256 arg0)` |
| `0x357e20d1` | `—` | `Unresolved_357e20d1(uint256 arg0, address arg1)` |
| `0xe39c08fc` | `earned(uint256,address)` | `—` |
| `0x326be8eb` | `—` | `Unresolved_326be8eb(uint256 arg0) → (uint256)` |
| `0x4dbc04bc` | `—` | `Unresolved_4dbc04bc(uint256 arg0)` |
| `0x811d581e` | `—` | `Unresolved_811d581e(uint256 arg0)` |
| `0x459efcd8` | `—` | `Unresolved_459efcd8(uint256 arg0)` |
| `0x246132f9` | `notifyRewardAmount(uint256,uint256)` | `Unresolved_246132f9(uint256 arg0)` |
| `0x721c6513` | `requestExit(uint256)` | `requestExit(uint256 arg0)` |
| `0x7b0472f0` | `stake(uint256,uint256)` | `Unresolved_7b0472f0(uint256 arg0)` |
| `0x586c226d` | `totalEarned(uint256,address)` | `Unresolved_586c226d(uint256 arg0)` |
| `0x80203152` | `—` | `Unresolved_80203152(uint256 arg0, address arg1)` |
| `0x56ffbecc` | `—` | `Unresolved_56ffbecc(uint256 arg0)` |
| `0x3fe2b470` | `—` | `Unresolved_3fe2b470(uint256 arg0)` |

## Facet #06 — `cycle-epoch`  (15 selectors)
`0x0bff8cee3c866041dc63f71ef0c106c62630d400`  →  `decompiled/f06_cycle-epoch_0x0bff8cee3c866041dc63f71ef0c106c62630d400/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x97ec170c` | `—` | `Unresolved_97ec170c(address arg0, uint256 arg1)` |
| `0xa29d08ce` | `—` | `Unresolved_a29d08ce(uint256 arg0, uint256 arg1)` |
| `0x13758207` | `—` | `Unresolved_13758207(address arg0, uint256 arg1)` |
| `0xdf735cca` | `—` | `Unresolved_df735cca(address arg0)` |
| `0x671c6865` | `—` | `Unresolved_671c6865(address arg0, uint256 arg1)` |
| `0xd4964514` | `—` | `Unresolved_d4964514(uint256 arg0)` |
| `0x369a5662` | `—` | `Unresolved_369a5662(address arg0)` |
| `0xdd5043fd` | `—` | `Unresolved_dd5043fd()` |
| `0x1177fd3f` | `—` | `Unresolved_1177fd3f(uint256 arg0)` |
| `0x74f3d7a3` | `—` | `Unresolved_74f3d7a3()` |
| `0x655bce22` | `getCycle()` | `getCycle() → (bytes memory)` |
| `0xc7a2170b` | `—` | `Unresolved_c7a2170b(address arg0) → (bytes memory)` |
| `0x657fa066` | `—` | `Unresolved_657fa066(uint256 arg0) → (bytes memory)` |
| `0x4146780d` | `—` | `Unresolved_4146780d(address arg0) → (bytes memory)` |
| `0xff519a14` | `—` | `—` |

## Facet #07 — `disputes`  (8 selectors)
`0x79b564945ceb2f3a1ebf67c16a5020fba38bfba6`  →  `decompiled/f07_disputes_0x79b564945ceb2f3a1ebf67c16a5020fba38bfba6/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x66b7467b` | `—` | `Unresolved_66b7467b(uint256 arg0, uint256 arg1)` |
| `0xff92ab86` | `—` | `—` |
| `0xfce1235c` | `failSafe(uint256)` | `failSafe(uint256 arg0)` |
| `0xe66128d7` | `—` | `Unresolved_e66128d7(uint256 arg0) → (uint256)` |
| `0x9f686097` | `raiseDispute(uint256,uint256)` | `Unresolved_9f686097(uint256 arg0)` |
| `0x29ef9e00` | `—` | `Unresolved_29ef9e00(uint256 arg0)` |
| `0x1b624123` | `releaseMerchantFunds(address,uint256)` | `Unresolved_1b624123(address arg0)` |
| `0x56a837f4` | `setReputationManager(address)` | `setReputationManager(address arg0)` |

## Facet #08 — `order-assignment`  (8 selectors)
`0x9a100830c9094dbec746a8314e4b35da3e1a86b9`  →  `decompiled/f08_order-assignment_0x9a100830c9094dbec746a8314e4b35da3e1a86b9/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x0a74aece` | `assignMerchants(uint256)` | `assignMerchants(uint256 arg0)` |
| `0x514fcac7` | `cancelOrder(uint256)` | `cancelOrder(uint256 arg0)` |
| `0x1dc46885` | `—` | `Unresolved_1dc46885(uint256 arg0, uint256 arg1, address arg2, uint256 arg3, uint` |
| `0xf05df5a0` | `—` | `—` |
| `0xe8576b23` | `—` | `Unresolved_e8576b23(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0xdbc96580` | `—` | `Unresolved_dbc96580(address arg0)` |
| `0xee8e56bf` | `—` | `Unresolved_ee8e56bf(address arg0)` |
| `0x6d5da5ad` | `—` | `Unresolved_6d5da5ad(address arg0)` |

## Facet #09 — `order-flow`  (8 selectors)
`0xba9f271cad8a58427ab4f8b7e46ff1239139f0bc`  →  `decompiled/f09_order-flow_0xba9f271cad8a58427ab4f8b7e46ff1239139f0bc/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xd97fa560` | `acceptOrder(uint256,string,string)` | `—` |
| `0x1d106060` | `completeOrder(uint256,string)` | `Unresolved_1d106060(uint256 arg0, uint256 arg1)` |
| `0x1c2b2b56` | `getDayKey(uint256)` | `getDayKey(uint256 arg0) → (uint256)` |
| `0x92d66313` | `getYear(uint256)` | `getYear(uint256 arg0)` |
| `0xca1f8c6e` | `getYearMonth(uint256)` | `getYearMonth(uint256 arg0)` |
| `0x1e31508e` | `paidBuyOrder(uint256)` | `paidBuyOrder(uint256 arg0)` |
| `0x271f3558` | `—` | `Unresolved_271f3558(uint256 arg0)` |
| `0xa037f4a1` | `—` | `Unresolved_a037f4a1(uint256 arg0)` |

## Facet #10 — `permissions`  (16 selectors)
`0xe3be102ab0ccbb966885e5e7cafa52f89599acab`  →  `decompiled/f10_permissions_0xe3be102ab0ccbb966885e5e7cafa52f89599acab/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xec89678c` | `—` | `—` |
| `0x5b2dae41` | `—` | `Unresolved_5b2dae41(uint256 arg0)` |
| `0xa0ee740f` | `—` | `Unresolved_a0ee740f(uint256 arg0)` |
| `0x0912489a` | `—` | `Unresolved_0912489a(address arg0)` |
| `0x5e4b61c5` | `—` | `Unresolved_5e4b61c5()` |
| `0x7eeab954` | `getPermissions(address,uint256)` | `Unresolved_7eeab954(address arg0)` |
| `0x4616d0b4` | `—` | `Unresolved_4616d0b4(uint256 arg0, address arg1, uint256 arg2, uint256 arg3)` |
| `0x574bc677` | `hasPermission(uint256,address,bytes4)` | `Unresolved_574bc677(uint256 arg0, address arg1)` |
| `0xdbafa9f0` | `isGlobalAdmin(address)` | `isGlobalAdmin(address arg0) → (bool)` |
| `0x675dcb14` | `—` | `Unresolved_675dcb14(uint256 arg0)` |
| `0xcd9e511e` | `—` | `Unresolved_cd9e511e(uint256 arg0, address arg1, uint256 arg2)` |
| `0xb9aaddff` | `—` | `Unresolved_b9aaddff(address arg0)` |
| `0x97f8c8b3` | `—` | `Unresolved_97f8c8b3(uint256 arg0, address arg1, uint256 arg2)` |
| `0xe09cf319` | `—` | `Unresolved_e09cf319(address arg0)` |
| `0x503f2600` | `—` | `Unresolved_503f2600(address arg0) → (bool)` |
| `0x67c64267` | `—` | `Unresolved_67c64267(address arg0)` |

## Facet #11 — `unknown-tiny`  (3 selectors)
`0x9eae2951353b71cad82e9cf16982db9d28586a5f`  →  `decompiled/f11_unknown-tiny_0x9eae2951353b71cad82e9cf16982db9d28586a5f/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x04ad634d` | `—` | `Unresolved_04ad634d(uint256 arg0, uint256 arg1, uint256 arg2, address arg3, uint` |
| `0xa58e94ac` | `—` | `—` |
| `0x2dce60da` | `—` | `Unresolved_2dce60da(uint256 arg0)` |

## Facet #12 — `admin-config`  (50 selectors)
`0x2fd2b520ffd8593d3f4925207c48929056f4c0a7`  →  `decompiled/f12_admin-config_0x2fd2b520ffd8593d3f4925207c48929056f4c0a7/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xaa050846` | `—` | `Unresolved_aa050846()` |
| `0x9e54503b` | `—` | `Unresolved_9e54503b(uint256 arg0)` |
| `0xfdb959bc` | `—` | `—` |
| `0x4f619ba4` | `—` | `Unresolved_4f619ba4()` |
| `0x7cae13a1` | `—` | `Unresolved_7cae13a1(uint256 arg0)` |
| `0x23962f1a` | `—` | `Unresolved_23962f1a(uint256 arg0)` |
| `0x7b6a00d8` | `—` | `Unresolved_7b6a00d8(uint256 arg0, uint256 arg1)` |
| `0xcca6ba8a` | `—` | `Unresolved_cca6ba8a(uint256 arg0, uint256 arg1)` |
| `0xb66d6bcb` | `—` | `Unresolved_b66d6bcb(uint256 arg0, uint256 arg1)` |
| `0xdf4b1239` | `—` | `Unresolved_df4b1239()` |
| `0x0f3e94d5` | `—` | `Unresolved_0f3e94d5(address arg0)` |
| `0x5f4c82d2` | `—` | `Unresolved_5f4c82d2(address arg0)` |
| `0xcd26d379` | `—` | `Unresolved_cd26d379(address arg0)` |
| `0x5bbcb659` | `—` | `Unresolved_5bbcb659(uint256 arg0)` |
| `0x68a39b5f` | `—` | `Unresolved_68a39b5f(uint256 arg0, address arg1)` |
| `0xa98fa0d4` | `—` | `Unresolved_a98fa0d4(uint256 arg0)` |
| `0xd7307fc7` | `—` | `Unresolved_d7307fc7(uint16 arg0)` |
| `0x0db354bc` | `setBaseSpread(bytes32,uint256)` | `Unresolved_0db354bc(uint256 arg0)` |
| `0x8b62fa8c` | `setBuyPrice(bytes32,uint256)` | `Unresolved_8b62fa8c(uint256 arg0)` |
| `0xd418f29a` | `—` | `Unresolved_d418f29a(uint256 arg0)` |
| `0x81fbf4d2` | `—` | `Unresolved_81fbf4d2(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3)` |
| `0x88c76f79` | `—` | `Unresolved_88c76f79(address arg0, address arg1)` |
| `0x2e692c3e` | `—` | `Unresolved_2e692c3e(uint24 arg0)` |
| `0xd02c21b1` | `setChainlinkForwarder(address)` | `setChainlinkForwarder(address arg0)` |
| `0xc1f91760` | `—` | `Unresolved_c1f91760(uint256 arg0, uint256 arg1)` |
| `0xc8016fb9` | `—` | `Unresolved_c8016fb9(uint256 arg0)` |
| `0x26bbf2ae` | `setExpectedWorkflowOwner(address)` | `setExpectedWorkflowOwner(address arg0)` |
| `0xb79ea77f` | `setFunctionsRouter(address)` | `setFunctionsRouter(address arg0)` |
| `0xa9e66a67` | `—` | `Unresolved_a9e66a67(address arg0)` |
| `0x7e3f9d49` | `—` | `Unresolved_7e3f9d49(uint256 arg0)` |
| `0x67f75e85` | `—` | `Unresolved_67f75e85(uint256 arg0)` |
| `0xf6067dd1` | `—` | `Unresolved_f6067dd1(uint256 arg0)` |
| `0xb80d8390` | `—` | `Unresolved_b80d8390(uint256 arg0)` |
| `0xed9f18fb` | `setMinStake(bytes32,uint256)` | `Unresolved_ed9f18fb(uint256 arg0)` |
| `0xad541c20` | `—` | `Unresolved_ad541c20(uint256 arg0)` |
| `0x2177be25` | `—` | `Unresolved_2177be25(uint256 arg0)` |
| `0xb8fa10f7` | `—` | `Unresolved_b8fa10f7(address arg0)` |
| `0x6893c391` | `setSellPrice(bytes32,uint256)` | `Unresolved_6893c391(uint256 arg0)` |
| `0xa2db4ed9` | `—` | `Unresolved_a2db4ed9(uint256 arg0)` |
| `0x134238dd` | `—` | `Unresolved_134238dd(uint256 arg0)` |
| `0x89d2e8fa` | `—` | `Unresolved_89d2e8fa(uint256 arg0)` |
| `0xd122f407` | `—` | `Unresolved_d122f407(uint256 arg0)` |
| `0xebe3bb62` | `setSuperAdmin(address,bool)` | `Unresolved_ebe3bb62(address arg0)` |
| `0xcec5599c` | `—` | `Unresolved_cec5599c(uint256 arg0)` |
| `0x2952c715` | `toggleExchangeStatus()` | `toggleExchangeStatus()` |
| `0x670a6fd9` | `updateAdmin(address,bool)` | `Unresolved_670a6fd9(address arg0)` |
| `0x4457cd38` | `—` | `Unresolved_4457cd38(uint256 arg0)` |
| `0x26f6d76a` | `—` | `Unresolved_26f6d76a(uint256 arg0)` |
| `0x70a26ec4` | `updateProcessingTime((uint256,uint256,uint256,uint256))` | `Unresolved_70a26ec4(uint256 arg0)` |
| `0x57bc58e1` | `—` | `Unresolved_57bc58e1(uint256 arg0, uint256 arg1)` |

## Facet #13 — `circles`  (25 selectors)
`0xc6d1f13df1ab2f974443d6bd5d16c4e6fba46cdc`  →  `decompiled/f13_circles_0xc6d1f13df1ab2f974443d6bd5d16c4e6fba46cdc/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xa1af3c87` | `—` | `Unresolved_a1af3c87(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0xd79d6cae` | `—` | `Unresolved_d79d6cae(address arg0)` |
| `0x9c313d72` | `—` | `Unresolved_9c313d72(uint256 arg0, address arg1, uint256 arg2, uint256 arg3, uint` |
| `0x23c5ac83` | `—` | `Unresolved_23c5ac83(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint` |
| `0x10b99fc9` | `—` | `Unresolved_10b99fc9(uint256 arg0, address arg1)` |
| `0x4e419537` | `—` | `Unresolved_4e419537(uint256 arg0, address arg1)` |
| `0x6366a783` | `—` | `Unresolved_6366a783(uint256 arg0, address arg1)` |
| `0x4a408331` | `—` | `Unresolved_4a408331(uint256 arg0, address arg1)` |
| `0x258b1c1e` | `—` | `Unresolved_258b1c1e(uint256 arg0) → (address)` |
| `0x64ccecde` | `getCircle(uint256)` | `getCircle(uint256 arg0)` |
| `0x77da3815` | `—` | `Unresolved_77da3815(address arg0) → (uint256)` |
| `0xe1f2dfce` | `getCircleCount()` | `—` |
| `0xe0ed9d38` | `—` | `Unresolved_e0ed9d38(uint256 arg0) → (uint256)` |
| `0x93d42fa1` | `—` | `Unresolved_93d42fa1(uint256 arg0) → (uint256)` |
| `0x6252ec94` | `—` | `Unresolved_6252ec94(address arg0) → (uint256)` |
| `0xc3e55bab` | `—` | `Unresolved_c3e55bab(uint256 arg0) → (uint256)` |
| `0xb3e3ae20` | `—` | `—` |
| `0x76148a7e` | `—` | `Unresolved_76148a7e(address arg0) → (bytes memory)` |
| `0x9c6628ff` | `—` | `Unresolved_9c6628ff(uint256 arg0) → (bool)` |
| `0xba096bd4` | `—` | `Unresolved_ba096bd4(uint256 arg0)` |
| `0x99c02a82` | `—` | `Unresolved_99c02a82(address arg0, uint256 arg1)` |
| `0xf45d0f32` | `—` | `—` |
| `0x44b106ac` | `—` | `Unresolved_44b106ac(uint256 arg0)` |
| `0x649cb8d8` | `—` | `Unresolved_649cb8d8(uint256 arg0, uint256 arg1)` |
| `0x2cfdd17b` | `—` | `Unresolved_2cfdd17b()` |

## Facet #14 — `unknown-medium`  (11 selectors)
`0x088f8f75e3b7bc5ae24b976ffb611479b2fa282e`  →  `decompiled/f14_unknown-medium_0x088f8f75e3b7bc5ae24b976ffb611479b2fa282e/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xd3c22fe7` | `—` | `Unresolved_d3c22fe7(uint256 arg0)` |
| `0x4c76390f` | `—` | `Unresolved_4c76390f(uint256 arg0) → (bytes memory)` |
| `0xed17fb87` | `—` | `—` |
| `0x57626f5f` | `—` | `Unresolved_57626f5f(uint256 arg0) → (bytes memory)` |
| `0x0646842c` | `—` | `Unresolved_0646842c(uint256 arg0) → (bytes memory)` |
| `0x5b5cc9cf` | `—` | `Unresolved_5b5cc9cf(uint256 arg0, uint256 arg1)` |
| `0x54808819` | `—` | `Unresolved_54808819(uint256 arg0, uint256 arg1)` |
| `0xb03e424e` | `—` | `Unresolved_b03e424e(uint256 arg0, uint256 arg1)` |
| `0x331de1a9` | `—` | `Unresolved_331de1a9(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0x56c5a2fb` | `—` | `Unresolved_56c5a2fb(uint256 arg0, uint256 arg1)` |
| `0xa393fdc6` | `—` | `Unresolved_a393fdc6(uint256 arg0, uint256 arg1)` |

## Facet #15 — `getters`  (83 selectors)
`0x004b3e89344161b9cc793c745156a50e97407f4a`  →  `decompiled/f15_getters_0x004b3e89344161b9cc793c745156a50e97407f4a/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xc6d114f1` | `—` | `Unresolved_c6d114f1(uint256 arg0, address arg1, address arg2, uint256 arg3, uint` |
| `0x46be235e` | `currentPaymentChannelCount()` | `Unresolved_46be235e() → (uint256)` |
| `0x7d8f7243` | `fetchMerchantAcceptedOrders(address)` | `fetchMerchantAcceptedOrders(address arg0)` |
| `0x4f843f8b` | `fetchMerchantAssignedOrders(address)` | `fetchMerchantAssignedOrders(address arg0)` |
| `0xdef7fe02` | `getActiveLiquidity(bytes32)` | `getActiveLiquidity(bytes32 arg0) → (bytes memory)` |
| `0xb677d43d` | `getAdditionalOrderDetails(uint256)` | `Unresolved_b677d43d(uint256 arg0) → (bytes memory)` |
| `0x1b3e22fc` | `—` | `Unresolved_1b3e22fc(uint256 arg0) → (uint256)` |
| `0x36b0ec9a` | `—` | `Unresolved_36b0ec9a(uint256 arg0, uint256 arg1, uint256 arg2, address arg3, uint` |
| `0x7fdf732d` | `getAssignedOrdersThreshold()` | `—` |
| `0xb85c25ae` | `—` | `Unresolved_b85c25ae(address arg0, uint256 arg1)` |
| `0x3ffe5c8a` | `—` | `Unresolved_3ffe5c8a(address arg0, uint256 arg1, uint256 arg2)` |
| `0x25ba147d` | `getCashbackConfig()` | `getCashbackConfig() → (bytes memory)` |
| `0x5fbfb4d2` | `getCashbackPercentage()` | `—` |
| `0x81137634` | `getChainlinkForwarderAddress()` | `—` |
| `0x587711ee` | `—` | `Unresolved_587711ee(uint256 arg0) → (address)` |
| `0xe7aa6770` | `—` | `Unresolved_e7aa6770(uint256 arg0) → (uint256)` |
| `0xe3a7e9ca` | `—` | `Unresolved_e3a7e9ca(uint256 arg0)` |
| `0x7da6dacc` | `—` | `Unresolved_7da6dacc(uint256 arg0) → (address)` |
| `0x6129f71a` | `—` | `Unresolved_6129f71a(uint256 arg0) → (uint256)` |
| `0xa5fcf5f4` | `getCurrentPaymentChannelId()` | `Unresolved_a5fcf5f4() → (uint256)` |
| `0xc4e5de74` | `—` | `Unresolved_c4e5de74(address arg0) → (bool)` |
| `0x4bb031b7` | `—` | `Unresolved_4bb031b7(uint256 arg0)` |
| `0x817cdd32` | `—` | `Unresolved_817cdd32(uint256 arg0) → (uint256)` |
| `0xb01dcb31` | `getExchangeStatus()` | `—` |
| `0x6f0b0769` | `getExpectedWorkflowOwner()` | `—` |
| `0x47991cb8` | `—` | `Unresolved_47991cb8(address arg0) → (bytes memory)` |
| `0x3395a79a` | `getFunctionsRouter()` | `—` |
| `0x1f431fa6` | `getMarketPrice(bytes32)` | `getMarketPrice(bytes32 arg0) → (uint256)` |
| `0xad641d16` | `—` | `Unresolved_ad641d16(uint256 arg0) → (uint256)` |
| `0x7e9aafc8` | `—` | `Unresolved_7e9aafc8(uint256 arg0) → (uint256)` |
| `0x399ac68c` | `—` | `Unresolved_399ac68c(uint256 arg0) → (uint256)` |
| `0x9d866c4b` | `—` | `Unresolved_9d866c4b(address arg0)` |
| `0x93a42939` | `getMerchantConfig(address)` | `getMerchantConfig(address arg0) → (bytes memory)` |
| `0x4e6cdee4` | `getMerchantDetails(address)` | `getMerchantDetails(address arg0)` |
| `0x3a29a687` | `—` | `Unresolved_3a29a687(uint256 arg0) → (uint256)` |
| `0x55a8069d` | `—` | `Unresolved_55a8069d(address arg0)` |
| `0x98f21159` | `—` | `Unresolved_98f21159(address arg0)` |
| `0x67391532` | `—` | `Unresolved_67391532(address arg0) → (bytes memory)` |
| `0x27bc9fac` | `—` | `Unresolved_27bc9fac(address arg0) → (bool)` |
| `0xa60cb55f` | `getMerchantStake(address)` | `getMerchantStake(address arg0) → (uint256)` |
| `0xa7240588` | `—` | `Unresolved_a7240588(address arg0)` |
| `0x0d5ad0fc` | `—` | `Unresolved_0d5ad0fc(address arg0) → (bytes memory)` |
| `0xc472ec88` | `—` | `Unresolved_c472ec88(uint256 arg0) → (uint256)` |
| `0x2a7f59df` | `—` | `Unresolved_2a7f59df(uint256 arg0) → (uint256)` |
| `0xd4786e0e` | `—` | `Unresolved_d4786e0e(uint256 arg0) → (uint256)` |
| `0x8158900b` | `getNextOrderId()` | `—` |
| `0xd481b371` | `—` | `Unresolved_d481b371(address arg0) → (uint256)` |
| `0xbed55ee0` | `—` | `Unresolved_bed55ee0(address arg0)` |
| `0xfe805b45` | `—` | `—` |
| `0xc3c2d5bf` | `—` | `Unresolved_c3c2d5bf(uint256 arg0) → (bytes memory)` |
| `0x5097bbcc` | `getOrderExpiry()` | `—` |
| `0x164de6e4` | `—` | `Unresolved_164de6e4(uint256 arg0) → (uint64)` |
| `0xcea99cd6` | `getOrdersById(uint256)` | `getOrdersById(uint256 arg0)` |
| `0xd773a0fa` | `—` | `Unresolved_d773a0fa(uint256 arg0) → (bool)` |
| `0x2681b36e` | `getPaymentChannelConfigs()` | `Unresolved_2681b36e()` |
| `0x58ae4f57` | `—` | `Unresolved_58ae4f57(address arg0)` |
| `0x30764388` | `—` | `Unresolved_30764388(address arg0) → (uint16)` |
| `0x1aee9b21` | `—` | `Unresolved_1aee9b21(uint256 arg0) → (uint256)` |
| `0x67c84efd` | `—` | `Unresolved_67c84efd(uint256 arg0) → (bytes memory)` |
| `0x60a25cbf` | `—` | `Unresolved_60a25cbf(uint256 arg0) → (address)` |
| `0x0e759e32` | `getProcessingTime()` | `Unresolved_0e759e32() → (bytes memory)` |
| `0x7608ccbf` | `—` | `Unresolved_7608ccbf(uint256 arg0) → (bytes memory)` |
| `0x695fbea3` | `—` | `Unresolved_695fbea3(uint256 arg0) → (uint64)` |
| `0x1e277523` | `—` | `Unresolved_1e277523(uint256 arg0) → (uint64)` |
| `0x925f1e1b` | `—` | `Unresolved_925f1e1b(uint256 arg0) → (uint64)` |
| `0x6b2d3913` | `getSmallOrderThreshold(bytes32)` | `Unresolved_6b2d3913(uint256 arg0) → (uint256)` |
| `0x63304e64` | `—` | `Unresolved_63304e64(uint256 arg0) → (uint256)` |
| `0x3234fef5` | `getTotalStake(bytes32)` | `getTotalStake(bytes32 arg0) → (uint256)` |
| `0x929dfc27` | `—` | `Unresolved_929dfc27(address arg0) → (bool)` |
| `0x6f77926b` | `getUser(address)` | `getUser(address arg0) → (bytes memory)` |
| `0x99940a13` | `—` | `Unresolved_99940a13(address arg0) → (uint256)` |
| `0xb8f48fe6` | `—` | `Unresolved_b8f48fe6(address arg0)` |
| `0x24fd71ab` | `—` | `Unresolved_24fd71ab(uint256 arg0) → (uint256)` |
| `0xdebaa93a` | `—` | `Unresolved_debaa93a(address arg0) → (bool)` |
| `0x4e5aa977` | `—` | `Unresolved_4e5aa977(address arg0, uint256 arg1)` |
| `0x24d7806c` | `isAdmin(address)` | `isAdmin(address arg0) → (bool)` |
| `0xfe575a87` | `isBlacklisted(address)` | `isBlacklisted(address arg0) → (bool)` |
| `0x97575c8b` | `—` | `Unresolved_97575c8b(uint256 arg0) → (bool)` |
| `0x59c69313` | `isOrderExpired(uint256)` | `isOrderExpired(uint256 arg0) → (bool)` |
| `0xdf7f453b` | `isSuperAdmin(address)` | `isSuperAdmin(address arg0) → (bool)` |
| `0x9ba18bf8` | `userOrdersArr(address)` | `userOrdersArr(address arg0)` |
| `0x4ecbda32` | `—` | `—` |
| `0x432ed230` | `—` | `—` |

## Facet #16 — `stake-lifecycle`  (22 selectors)
`0x2f9cd409617e0570564a1f54043f6c6a6b8ce724`  →  `decompiled/f16_stake-lifecycle_0x2f9cd409617e0570564a1f54043f6c6a6b8ce724/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xbe6e309a` | `—` | `Unresolved_be6e309a(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0xed646826` | `—` | `Unresolved_ed646826(address arg0) → (uint256)` |
| `0x5273b255` | `—` | `Unresolved_5273b255(address arg0, uint256 arg1)` |
| `0x209e0d60` | `—` | `Unresolved_209e0d60(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0xa6fdac35` | `approveUnstake(address)` | `approveUnstake(address arg0)` |
| `0x549f332a` | `cancelUnstakeRequest(address)` | `cancelUnstakeRequest(address arg0)` |
| `0x0ea94341` | `finalizeUnstake()` | `finalizeUnstake()` |
| `0xd49c9c71` | `—` | `Unresolved_d49c9c71(address arg0)` |
| `0x5915bf24` | `—` | `Unresolved_5915bf24(address arg0) → (uint256)` |
| `0x6a320984` | `getUnstakeCooldownEndTime(address)` | `getUnstakeCooldownEndTime(address arg0) → (uint256)` |
| `0xc81575f7` | `isMerchantEligible(address)` | `isMerchantEligible(address arg0) → (bool)` |
| `0x73e44ef0` | `—` | `Unresolved_73e44ef0(uint256 arg0)` |
| `0x02c8fcaf` | `—` | `Unresolved_02c8fcaf(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0x0ac8af55` | `—` | `Unresolved_0ac8af55(uint256 arg0)` |
| `0xfc63958e` | `requestUnstake()` | `requestUnstake()` |
| `0xd85da810` | `—` | `Unresolved_d85da810(address arg0, uint256 arg1)` |
| `0x83c592cf` | `stake(uint256,bytes32)` | `Unresolved_83c592cf(uint256 arg0)` |
| `0x3e9a2a3c` | `—` | `Unresolved_3e9a2a3c(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint` |
| `0xbd90c2f6` | `—` | `Unresolved_bd90c2f6(address arg0, uint256 arg1)` |
| `0xfffbd845` | `—` | `—` |
| `0xcb75cc5e` | `—` | `Unresolved_cb75cc5e(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0x72b864af` | `—` | `Unresolved_72b864af(uint256 arg0, uint256 arg1)` |

## Facet #17 — `chainlink-oracle`  (11 selectors)
`0xcc691a84afa85cd70cb8c8a9b99bffda471bc2c1`  →  `decompiled/f17_chainlink-oracle_0xcc691a84afa85cd70cb8c8a9b99bffda471bc2c1/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0xd7800d83` | `—` | `—` |
| `0x5692dd55` | `—` | `Unresolved_5692dd55(uint256 arg0)` |
| `0x73fb8124` | `—` | `—` |
| `0x0ca76175` | `handleOracleFulfillment(bytes32,bytes,bytes)` | `Unresolved_0ca76175(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0x95ce1c2d` | `—` | `Unresolved_95ce1c2d(uint256 arg0, uint256 arg1) → (bool)` |
| `0x805f2132` | `onReport(bytes,bytes)` | `Unresolved_805f2132(uint256 arg0, uint256 arg1)` |
| `0x996979df` | `—` | `Unresolved_996979df(uint256 arg0, uint256 arg1, bool arg2, uint64 arg3, uint64 a` |
| `0x8f865cd6` | `—` | `Unresolved_8f865cd6(address arg0)` |
| `0xcfb51928` | `stringToBytes32(string)` | `Unresolved_cfb51928(uint256 arg0) → (uint256)` |
| `0xce912508` | `updatePrice(bytes32)` | `updatePrice(bytes32 arg0)` |
| `0x5f5487ab` | `—` | `Unresolved_5f5487ab(uint256 arg0, uint256 arg1, uint256 arg2)` |

## Facet #18 — `moderation`  (11 selectors)
`0xf2a632a7dca5d8f2ee614a889b0a14910e04946d`  →  `decompiled/f18_moderation_0xf2a632a7dca5d8f2ee614a889b0a14910e04946d/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x37a4eea1` | `—` | `Unresolved_37a4eea1(uint256 arg0)` |
| `0x30321dcc` | `blacklistMerchant(address)` | `blacklistMerchant(address arg0)` |
| `0x8cc93d61` | `—` | `Unresolved_8cc93d61(uint256 arg0)` |
| `0xbae22911` | `—` | `Unresolved_bae22911(address arg0)` |
| `0x3e10bc04` | `—` | `Unresolved_3e10bc04(address arg0, uint256 arg1)` |
| `0x014a4bc6` | `—` | `Unresolved_014a4bc6(address arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint` |
| `0xeb91e651` | `removeBlacklist(address)` | `removeBlacklist(address arg0)` |
| `0xeff07b53` | `—` | `—` |
| `0x017a99ff` | `—` | `Unresolved_017a99ff(uint256 arg0, uint256 arg1, uint256 arg2)` |
| `0x90dc5b65` | `toggleOnlineOffline()` | `toggleOnlineOffline()` |
| `0x2b2a198c` | `—` | `Unresolved_2b2a198c(address arg0)` |

## Facet #19 — `unknown-medium2`  (9 selectors)
`0x371541382f60ae2684b32bd31ef08df50b7d2dfa`  →  `decompiled/f19_unknown-medium2_0x371541382f60ae2684b32bd31ef08df50b7d2dfa/decompiled.sol`

| selector | canonical | heimdall-recovered |
|----------|-----------|---------------------|
| `0x8b5ab606` | `—` | `Unresolved_8b5ab606(address arg0)` |
| `0x17353447` | `—` | `Unresolved_17353447(address arg0) → (bool)` |
| `0xc0bc0d14` | `—` | `Unresolved_c0bc0d14(uint256 arg0) → (address)` |
| `0xe46bf834` | `—` | `—` |
| `0x1e61c167` | `—` | `Unresolved_1e61c167(uint256 arg0)` |
| `0xb92046a2` | `—` | `Unresolved_b92046a2(uint256 arg0)` |
| `0x4b5b392e` | `—` | `Unresolved_4b5b392e(address arg0, uint256 arg1, uint256 arg2, address arg3, uint` |
| `0x3095f2be` | `—` | `Unresolved_3095f2be(address arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint` |
| `0x36ed9d14` | `—` | `Unresolved_36ed9d14(address arg0, uint256 arg1)` |

