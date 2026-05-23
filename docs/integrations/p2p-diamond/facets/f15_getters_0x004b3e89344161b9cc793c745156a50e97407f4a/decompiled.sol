// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0;

/// @title            Decompiled Contract
/// @author           Jonathan Becker <jonathan@jbecker.dev>
/// @custom:version   heimdall-rs v0.9.2
///
/// @notice           This contract was decompiled using the heimdall-rs decompiler.
///                     It was generated directly by tracing the EVM opcodes from this contract.
///                     As a result, it may not compile or even be valid solidity code.
///                     Despite this, it should be obvious what each function does. Overall
///                     logic should have been preserved throughout decompiling.
///
/// @custom:github    You can find the open-source decompiler here:
///                       https://heimdall.rs

contract DecompiledContract {
    address public unresolved_432ed230;
    mapping(bytes32 => bytes32) storage_map_p;
    uint256 public getNextOrderId;
    mapping(bytes32 => bytes32) storage_map_c;
    bytes32 store_aa;
    bool public getExchangeStatus;
    uint24 public unresolved_5fbfb4d2;
    bytes32 store_x;
    mapping(bytes32 => bytes32) storage_map_e;
    uint256 public unresolved_5097bbcc;
    address public unresolved_6f0b0769;
    mapping(bytes32 => bytes32) storage_map_q;
    bytes32 store_z;
    mapping(bytes32 => bytes32) storage_map_d;
    address public unresolved_81137634;
    mapping(bytes32 => bytes32) storage_map_k;
    mapping(bytes32 => bytes32) storage_map_h;
    mapping(bytes32 => bytes32) storage_map_a;
    mapping(bytes32 => bytes32) storage_map_r;
    address public unresolved_4ecbda32;
    mapping(bytes32 => bytes32) storage_map_f;
    bytes32 store_o;
    bytes32 store_m;
    mapping(bytes32 => bytes32) storage_map_g;
    bytes32 store_y;
    mapping(bytes32 => bytes32) storage_map_b;
    address public getFunctionsRouter;
    bytes32 store_i;
    uint16 public unresolved_7fdf732d;
    
    error CurrencyNotSupported();
    
    /// @custom:selector    0x3ffe5c8a
    /// @custom:signature   Unresolved_3ffe5c8a(address arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_3ffe5c8a(address arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
        require(arg2 - arg2);
    }
    
    /// @custom:selector    0x1e277523
    /// @custom:signature   Unresolved_1e277523(uint256 arg0) public view returns (uint64)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_1e277523(uint256 arg0) public view returns (uint64) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return uint64(storage_map_a[var_a]);
    }
    
    /// @custom:selector    0x2a7f59df
    /// @custom:signature   Unresolved_2a7f59df(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_2a7f59df(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x99940a13
    /// @custom:signature   Unresolved_99940a13(address arg0) public view returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_99940a13(address arg0) public view returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return storage_map_b[var_a];
    }
    
    /// @custom:selector    0x3a29a687
    /// @custom:signature   Unresolved_3a29a687(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_3a29a687(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x7da6dacc
    /// @custom:signature   Unresolved_7da6dacc(uint256 arg0) public view returns (address)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_7da6dacc(uint256 arg0) public view returns (address) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return address(storage_map_c[var_a]);
    }
    
    /// @custom:selector    0x98f21159
    /// @custom:signature   Unresolved_98f21159(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_98f21159(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xd481b371
    /// @custom:signature   Unresolved_d481b371(address arg0) public view returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_d481b371(address arg0) public view returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x7d8f7243
    /// @custom:signature   fetchMerchantAcceptedOrders(address arg0) public view
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function fetchMerchantAcceptedOrders(address arg0) public view {
        require(msg.value);
        require(address(arg0) - arg0);
        require(((var_c + 0x40) > 0xffffffffffffffff) | ((var_c + 0x40) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        uint256 var_c = var_c + 0x40;
        require(storage_map_c[var_a] > 0xffffffffffffffff);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((var_c + (uint248(0x1f + ((storage_map_c[var_a] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((storage_map_c[var_a] * 0x20) + 0x20)))) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_c = var_c + (uint248(0x1f + ((storage_map_c[var_a] * 0x20) + 0x20)));
        var_a = keccak256(var_a) + 0x01;
        require(0 < (storage_map_c[var_a]));
        require(((var_c + 0x40) > 0xffffffffffffffff) | ((var_c + 0x40) < var_c));
        require(var_c.length == 0);
        require(var_h > (var_i));
        require(!(var_i) > 0x64);
        require(0x64 > 0xffffffffffffffff);
    }
    
    /// @custom:selector    0xd773a0fa
    /// @custom:signature   Unresolved_d773a0fa(uint256 arg0) public view returns (bool)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_d773a0fa(uint256 arg0) public view returns (bool) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0xa0) > 0xffffffffffffffff) | ((var_a + 0xa0) < var_a));
        uint256 var_a = var_a + 0xa0;
        uint256 var_b = arg0;
        require(((var_a + 0xa0) > 0xffffffffffffffff) | ((var_a + 0xa0) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0xa0;
        require(!bytes1(storage_map_e[var_b]));
        require(((storage_map_e[var_b] / 0x02) < 0x20) == (bytes1(storage_map_e[var_b])));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!bytes1(storage_map_e[var_b]));
        require(0x01 == (bytes1(storage_map_e[var_b])));
        var_b = keccak256(var_b) + 0x01;
        require(0 < (storage_map_e[var_b] / 0x02));
        require(((var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)))) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)));
        require(0 < (var_t));
        return abi.encodePacked(0x20, var_a.length, ((var_a + 0x20) + 0xa0) - (var_a + 0x20), var_ab, var_ac, !(!var_ad), var_ae);
        require(((var_a + (uint248(0x1f + (0 - var_a)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + (0 - var_a)))) < var_a));
    }
    
    /// @custom:selector    0x0d5ad0fc
    /// @custom:signature   Unresolved_0d5ad0fc(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_0d5ad0fc(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        require(((var_a + 0xa0) > 0xffffffffffffffff) | ((var_a + 0xa0) < var_a));
        uint256 var_a = var_a + 0xa0;
        require(((var_a + 0xa0) > 0xffffffffffffffff) | ((var_a + 0xa0) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0xa0;
        require(!0x04 > (bytes1(storage_map_h[var_b])));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!0x04 > (var_v));
        return abi.encodePacked(var_a.length, var_x, var_y, var_z, var_aa);
    }
    
    /// @custom:selector    0xdf7f453b
    /// @custom:signature   isSuperAdmin(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function isSuperAdmin(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0xe7aa6770
    /// @custom:signature   Unresolved_e7aa6770(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_e7aa6770(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        require(address(storage_map_c[var_a]) == 0);
        var_a = address(storage_map_c[var_a]);
        require(!0x01);
        require(address(storage_map_a[var_a]) - (address(storage_map_a[var_a])));
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff);
        var_a = address(storage_map_a[var_a]);
        require(!0);
        require(address(storage_map_a[var_a]) - (address(storage_map_a[var_a])));
        return 0x01;
        return 0;
        return 0;
    }
    
    /// @custom:selector    0x30764388
    /// @custom:signature   Unresolved_30764388(address arg0) public view returns (uint16)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_30764388(address arg0) public view returns (uint16) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return uint16(storage_map_a[var_a]);
    }
    
    /// @custom:selector    0x24fd71ab
    /// @custom:signature   Unresolved_24fd71ab(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_24fd71ab(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x25ba147d
    /// @custom:signature   getCashbackConfig() public view returns (bytes memory)
    function getCashbackConfig() public view returns (bytes memory) {
        require(msg.value);
        if (((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a)) {
            uint256 var_a = var_a + 0x80;
            if (((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a)) {
                var_a = var_a + 0x80;
                return abi.encodePacked(address(var_a.length), address(var_s), uint24(var_t), uint24(var_u));
            }
        }
    }
    
    /// @custom:selector    0x164de6e4
    /// @custom:signature   Unresolved_164de6e4(uint256 arg0) public view returns (uint64)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_164de6e4(uint256 arg0) public view returns (uint64) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return uint64(storage_map_k[var_a]);
    }
    
    /// @custom:selector    0xfe575a87
    /// @custom:signature   isBlacklisted(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function isBlacklisted(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0x6129f71a
    /// @custom:signature   Unresolved_6129f71a(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_6129f71a(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x63304e64
    /// @custom:signature   Unresolved_63304e64(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_63304e64(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x7608ccbf
    /// @custom:signature   Unresolved_7608ccbf(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_7608ccbf(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        var_a = arg0;
        return abi.encodePacked(storage_map_a[var_a], storage_map_a[var_a]);
    }
    
    /// @custom:selector    0x2681b36e
    /// @custom:signature   Unresolved_2681b36e() public view
    function Unresolved_2681b36e() public view {
        require(msg.value);
        if (store_m > 0xffffffffffffffff) {
            if (((var_c + (uint248(0x1f + ((store_m * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((store_m * 0x20) + 0x20)))) < var_c)) {
            }
        }
    }
    
    /// @custom:selector    0x3234fef5
    /// @custom:signature   getTotalStake(bytes32 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getTotalStake(bytes32 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x925f1e1b
    /// @custom:signature   Unresolved_925f1e1b(uint256 arg0) public view returns (uint64)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_925f1e1b(uint256 arg0) public view returns (uint64) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return uint64(storage_map_a[var_a]);
    }
    
    /// @custom:selector    0x67c84efd
    /// @custom:signature   Unresolved_67c84efd(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_67c84efd(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
        uint256 var_a = var_a + 0x80;
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0x80;
        return abi.encodePacked(var_a.length, var_t, var_u, var_v);
    }
    
    /// @custom:selector    0x36b0ec9a
    /// @custom:signature   Unresolved_36b0ec9a(uint256 arg0, uint256 arg1, uint256 arg2, address arg3, uint256 arg4, uint256 arg5, uint256 arg6) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    /// @param              arg5 ["uint256", "bytes32", "int256"]
    /// @param              arg6 ["uint256", "bytes32", "int256"]
    function Unresolved_36b0ec9a(uint256 arg0, uint256 arg1, uint256 arg2, address arg3, uint256 arg4, uint256 arg5, uint256 arg6) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
        require(arg2 - arg2);
        require(address(arg3) - arg3);
        require(arg4 - arg4);
        require(arg5 - arg5);
        require(arg6 - arg6);
    }
    
    /// @custom:selector    0x60a25cbf
    /// @custom:signature   Unresolved_60a25cbf(uint256 arg0) public view returns (address)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_60a25cbf(uint256 arg0) public view returns (address) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return address(storage_map_a[var_a]);
    }
    
    /// @custom:selector    0xa5fcf5f4
    /// @custom:signature   Unresolved_a5fcf5f4() public view returns (uint256)
    function Unresolved_a5fcf5f4() public view returns (uint256) {
        require(msg.value);
        return store_o;
    }
    
    /// @custom:selector    0x24d7806c
    /// @custom:signature   isAdmin(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function isAdmin(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(!bytes1(storage_map_a[var_a]));
        return !(!bytes1(storage_map_a[var_a]));
        var_a = address(arg0);
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0x55a8069d
    /// @custom:signature   Unresolved_55a8069d(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_55a8069d(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x9d866c4b
    /// @custom:signature   Unresolved_9d866c4b(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_9d866c4b(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x4bb031b7
    /// @custom:signature   Unresolved_4bb031b7(uint256 arg0) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_4bb031b7(uint256 arg0) public view {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        require(address(storage_map_c[var_a]) == 0);
        var_a = arg0;
        require(address(storage_map_c[var_a]) == 0);
        var_a = address(storage_map_c[var_a]);
        require(!0x01);
        require(address(storage_map_a[var_a]) - (address(storage_map_a[var_a])));
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff);
        var_a = address(storage_map_a[var_a]);
        require(!0);
        require(address(storage_map_a[var_a]) - (address(storage_map_a[var_a])));
        require(0x01 > 0xffffffffffffffff);
        require(((var_d + 0x40) > 0xffffffffffffffff) | ((var_d + 0x40) < var_d));
    }
    
    /// @custom:selector    0x929dfc27
    /// @custom:signature   Unresolved_929dfc27(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_929dfc27(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0x4e6cdee4
    /// @custom:signature   getMerchantDetails(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function getMerchantDetails(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(((var_a + 0x0160) > 0xffffffffffffffff) | ((var_a + 0x0160) < var_a));
    }
    
    /// @custom:selector    0xd4786e0e
    /// @custom:signature   Unresolved_d4786e0e(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_d4786e0e(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x695fbea3
    /// @custom:signature   Unresolved_695fbea3(uint256 arg0) public view returns (uint64)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_695fbea3(uint256 arg0) public view returns (uint64) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return uint64(storage_map_a[var_a]);
    }
    
    /// @custom:selector    0xc4e5de74
    /// @custom:signature   Unresolved_c4e5de74(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_c4e5de74(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0xdebaa93a
    /// @custom:signature   Unresolved_debaa93a(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_debaa93a(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return !(!storage_map_a[var_a] > 0);
    }
    
    /// @custom:selector    0x46be235e
    /// @custom:signature   Unresolved_46be235e() public view returns (uint256)
    function Unresolved_46be235e() public view returns (uint256) {
        require(msg.value);
        return store_o;
    }
    
    /// @custom:selector    0x817cdd32
    /// @custom:signature   Unresolved_817cdd32(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_817cdd32(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x47991cb8
    /// @custom:signature   Unresolved_47991cb8(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_47991cb8(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        if (storage_map_a[var_a] > 0xffffffffffffffff) {
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(storage_map_a[var_a] > 0xffffffffffffffff);
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            address var_d = var_d + (uint248(0x1f + ((storage_map_a[var_a] * 0x20) + 0x20)));
            var_a = keccak256(var_a);
            require(((var_d + (uint248(0x1f + ((storage_map_a[var_a] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((storage_map_a[var_a] * 0x20) + 0x20)))) < var_d));
            require(0 < storage_map_a[var_a]);
            require(!bytes1(storage_map_a[var_a]));
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(((storage_map_a[var_a] / 0x02) < 0x20) == (bytes1(storage_map_a[var_a])));
            require(!bytes1(storage_map_a[var_a]));
            var_a = keccak256(var_a);
            require(0x01 == (bytes1(storage_map_a[var_a])));
            require(0 < (storage_map_a[var_a] / 0x02));
            require(((var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)))) < var_d));
            require(((var_d + (uint248(0x1f + (0 - var_d)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + (0 - var_d)))) < var_d));
        }
        return abi.encodePacked(0x20, var_d.length);
    }
    
    /// @custom:selector    0x93a42939
    /// @custom:signature   getMerchantConfig(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function getMerchantConfig(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        if (((var_a + 0x60) > 0xffffffffffffffff) | ((var_a + 0x60) < var_a)) {
            uint256 var_a = var_a + 0x60;
            require(((var_a + 0x60) > 0xffffffffffffffff) | ((var_a + 0x60) < var_a));
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            var_a = var_a + 0x60;
            require(((var_a + 0x60) > 0xffffffffffffffff) | ((var_a + 0x60) < var_a));
            require(!bytes1(storage_map_d[var_b]));
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(((storage_map_d[var_b] / 0x02) < 0x20) == (bytes1(storage_map_d[var_b])));
            require(!bytes1(storage_map_d[var_b]));
            var_b = keccak256(var_b);
            require(0x01 == (bytes1(storage_map_d[var_b])));
            require(0 < (storage_map_d[var_b] / 0x02));
            var_a = var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)));
            require(((var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)))) < var_a));
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(!0x02 > (bytes1(storage_map_e[var_b])));
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(storage_map_f[var_b] > 0xffffffffffffffff);
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            var_a = var_a + (uint248(0x1f + ((storage_map_f[var_b] * 0x20) + 0x20)));
            var_b = keccak256(var_b) + 0x02;
            require(((var_a + (uint248(0x1f + ((storage_map_f[var_b] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + ((storage_map_f[var_b] * 0x20) + 0x20)))) < var_a));
            require(0 < (storage_map_f[var_b]));
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            var_a = var_a + 0xe0;
            require(((var_a + 0xe0) > 0xffffffffffffffff) | ((var_a + 0xe0) < var_a));
            require(!bytes1(storage_map_f[var_b]));
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(((storage_map_f[var_b] / 0x02) < 0x20) == (bytes1(storage_map_f[var_b])));
            require(!bytes1(storage_map_f[var_b]));
            var_b = keccak256(var_b) + 0x02;
            require(0x01 == (bytes1(storage_map_f[var_b])));
            require(0 < (storage_map_f[var_b] / 0x02));
            var_a = var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)));
            require(((var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + (((0x20 + var_a) + 0) - var_a)))) < var_a));
            var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(!0x06 > (bytes1(storage_map_g[var_b])));
            require(((var_a + (uint248(0x1f + (0 - var_a)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + (0 - var_a)))) < var_a));
            require(!0x02 > (var_t));
            require(0 < (var_v));
        }
        return abi.encodePacked(0x20, 0x60, var_t, (uint248(0x1f + var_r) + (0x20 + ((var_a + 0x20) + 0x60))) - (var_a + 0x20), var_r, var_x);
    }
    
    /// @custom:selector    0xad641d16
    /// @custom:signature   Unresolved_ad641d16(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_ad641d16(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0xe3a7e9ca
    /// @custom:signature   Unresolved_e3a7e9ca(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_e3a7e9ca(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x6f77926b
    /// @custom:signature   getUser(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function getUser(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
        uint256 var_a = var_a + 0x80;
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0x80;
        require(storage_map_g[var_b] > 0xffffffffffffffff);
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((var_a + (uint248(0x1f + ((storage_map_g[var_b] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + ((storage_map_g[var_b] * 0x20) + 0x20)))) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + (uint248(0x1f + ((storage_map_g[var_b] * 0x20) + 0x20)));
        var_b = keccak256(var_b) + 0x03;
        require(0 < (storage_map_g[var_b]));
        require(((var_a + 0x40) > 0xffffffffffffffff) | ((var_a + 0x40) < var_a));
        require(0 < (var_v));
        return abi.encodePacked(0x20, var_a.length, var_w, var_x, ((var_a + 0x20) + 0x80) - (var_a + 0x20), var_y);
    }
    
    /// @custom:selector    0xb8f48fe6
    /// @custom:signature   Unresolved_b8f48fe6(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_b8f48fe6(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xdef7fe02
    /// @custom:signature   getActiveLiquidity(bytes32 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getActiveLiquidity(bytes32 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x40) > 0xffffffffffffffff) | ((var_a + 0x40) < var_a));
        uint256 var_a = var_a + 0x40;
        require(((var_a + 0x40) > 0xffffffffffffffff) | ((var_a + 0x40) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0x40;
        return abi.encodePacked(address(var_a.length), address(var_l));
    }
    
    /// @custom:selector    0x59c69313
    /// @custom:signature   isOrderExpired(uint256 arg0) public view returns (bool)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function isOrderExpired(uint256 arg0) public view returns (bool) {
        require(msg.value);
        require(arg0 - arg0);
        require(!0x03 > 0);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!0x03 > (bytes1(storage_map_q[var_a] >> 0x10)));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_q[var_a] >> 0x10) - 0);
        require(!0x05 > 0);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!0x05 > (bytes1(storage_map_q[var_a] >> 0x08)));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_q[var_a] >> 0x08) - 0);
        require(storage_map_b[var_a] == 0);
        require(storage_map_b[var_a] > (storage_map_b[var_a] + 0xb4));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return !(block.timestamp < (storage_map_b[var_a] + 0xb4));
        return 0;
    }
    
    /// @custom:selector    0x9ba18bf8
    /// @custom:signature   userOrdersArr(address arg0) public view
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function userOrdersArr(address arg0) public view {
        require(msg.value);
        require(address(arg0) - arg0);
        require(((var_c + 0x80) > 0xffffffffffffffff) | ((var_c + 0x80) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        uint256 var_c = var_c + 0x80;
        require(storage_map_r[var_a] > 0xffffffffffffffff);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((var_c + (uint248(0x1f + ((storage_map_r[var_a] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((storage_map_r[var_a] * 0x20) + 0x20)))) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_c = var_c + (uint248(0x1f + ((storage_map_r[var_a] * 0x20) + 0x20)));
        var_a = keccak256(var_a) + 0x03;
        require(0 < (storage_map_r[var_a]));
        require(((var_c + 0x40) > 0xffffffffffffffff) | ((var_c + 0x40) < var_c));
        require(var_c.length == 0);
        require(!(var_j) > 0x64);
        require(0x64 > 0xffffffffffffffff);
    }
    
    /// @custom:selector    0xc472ec88
    /// @custom:signature   Unresolved_c472ec88(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_c472ec88(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x1aee9b21
    /// @custom:signature   Unresolved_1aee9b21(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_1aee9b21(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x4e5aa977
    /// @custom:signature   Unresolved_4e5aa977(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_4e5aa977(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0x6b2d3913
    /// @custom:signature   Unresolved_6b2d3913(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_6b2d3913(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x7e9aafc8
    /// @custom:signature   Unresolved_7e9aafc8(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_7e9aafc8(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        require(!(bytes1(storage_map_a[var_a])), CustomError_02a6fdd2());
        var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x58ae4f57
    /// @custom:signature   Unresolved_58ae4f57(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_58ae4f57(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xc3c2d5bf
    /// @custom:signature   Unresolved_c3c2d5bf(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_c3c2d5bf(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x40) > 0xffffffffffffffff) | ((var_a + 0x40) < var_a));
        uint256 var_a = var_a + 0x40;
        require(((var_a + 0x40) > 0xffffffffffffffff) | ((var_a + 0x40) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0x40;
        return abi.encodePacked(address(var_a.length), address(var_l));
    }
    
    /// @custom:selector    0x1f431fa6
    /// @custom:signature   getMarketPrice(bytes32 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getMarketPrice(bytes32 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0xa60cb55f
    /// @custom:signature   getMerchantStake(address arg0) public view returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function getMerchantStake(address arg0) public view returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return storage_map_k[var_a];
    }
    
    /// @custom:selector    0x27bc9fac
    /// @custom:signature   Unresolved_27bc9fac(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_27bc9fac(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(!0x02 > (bytes1(storage_map_c[var_a])));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return bytes1(storage_map_c[var_a]);
    }
    
    /// @custom:selector    0xa7240588
    /// @custom:signature   Unresolved_a7240588(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_a7240588(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xb85c25ae
    /// @custom:signature   Unresolved_b85c25ae(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_b85c25ae(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0x67391532
    /// @custom:signature   Unresolved_67391532(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_67391532(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(storage_map_b[var_a] > 0xffffffffffffffff);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((var_d + (uint248(0x1f + ((storage_map_b[var_a] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((storage_map_b[var_a] * 0x20) + 0x20)))) < var_d));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        address var_d = var_d + (uint248(0x1f + ((storage_map_b[var_a] * 0x20) + 0x20)));
        var_a = keccak256(var_a) + 0x02;
        require(0 < (storage_map_b[var_a]));
        require(((var_d + 0xe0) > 0xffffffffffffffff) | ((var_d + 0xe0) < var_d));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_d = var_d + 0xe0;
        require(!bytes1(storage_map_b[var_a]));
        require(((storage_map_b[var_a] / 0x02) < 0x20) == (bytes1(storage_map_b[var_a])));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!bytes1(storage_map_b[var_a]));
        require(0x01 == (bytes1(storage_map_b[var_a])));
        var_a = keccak256(var_a) + 0x02;
        require(0 < (storage_map_b[var_a] / 0x02));
        require(((var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)))) < var_d));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_d = var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)));
        require(!0x06 > (bytes1(storage_map_r[var_a])));
        require(((var_d + (uint248(0x1f + (0 - var_d)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + (0 - var_d)))) < var_d));
        require(0 < (var_o));
        require(!0x06 > (var_q));
        return abi.encodePacked(0x20, var_d.length);
    }
    
    /// @custom:selector    0x4f843f8b
    /// @custom:signature   fetchMerchantAssignedOrders(address arg0) public view
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function fetchMerchantAssignedOrders(address arg0) public view {
        require(msg.value);
        require(address(arg0) - arg0);
        require(((var_c + 0x40) > 0xffffffffffffffff) | ((var_c + 0x40) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        uint256 var_c = var_c + 0x40;
        require(storage_map_c[var_a] > 0xffffffffffffffff);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((var_c + (uint248(0x1f + ((storage_map_c[var_a] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((storage_map_c[var_a] * 0x20) + 0x20)))) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_c = var_c + (uint248(0x1f + ((storage_map_c[var_a] * 0x20) + 0x20)));
        var_a = keccak256(var_a) + 0x01;
        require(0 < (storage_map_c[var_a]));
        require(((var_c + 0x40) > 0xffffffffffffffff) | ((var_c + 0x40) < var_c));
        require(var_c.length == 0);
        require(!(var_h) > 0x64);
        require(0x64 > 0xffffffffffffffff);
    }
    
    /// @custom:selector    0xbed55ee0
    /// @custom:signature   Unresolved_bed55ee0(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_bed55ee0(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xcea99cd6
    /// @custom:signature   getOrdersById(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getOrdersById(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x02c0) > 0xffffffffffffffff) | ((var_a + 0x02c0) < var_a));
        uint256 var_a = var_a + 0x02c0;
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
    }
    
    /// @custom:selector    0x1b3e22fc
    /// @custom:signature   Unresolved_1b3e22fc(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_1b3e22fc(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x97575c8b
    /// @custom:signature   Unresolved_97575c8b(uint256 arg0) public view returns (bool)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_97575c8b(uint256 arg0) public view returns (bool) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0x0e759e32
    /// @custom:signature   Unresolved_0e759e32() public view returns (bytes memory)
    function Unresolved_0e759e32() public view returns (bytes memory) {
        require(msg.value);
        if (((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a)) {
            uint256 var_a = var_a + 0x80;
            if (((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a)) {
                var_a = var_a + 0x80;
                return abi.encodePacked(var_a.length, var_s, var_t, var_u);
            }
        }
    }
    
    /// @custom:selector    0x399ac68c
    /// @custom:signature   Unresolved_399ac68c(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_399ac68c(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_a[var_a];
    }
    
    /// @custom:selector    0x587711ee
    /// @custom:signature   Unresolved_587711ee(uint256 arg0) public view returns (address)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_587711ee(uint256 arg0) public view returns (address) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        var_a = arg0;
        var_a = address(storage_map_c[var_a]);
        return address(storage_map_a[var_a]);
    }
    
    /// @custom:selector    0xb677d43d
    /// @custom:signature   Unresolved_b677d43d(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_b677d43d(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0xe0) > 0xffffffffffffffff) | ((var_a + 0xe0) < var_a));
        uint256 var_a = var_a + 0xe0;
        require(((var_a + 0xe0) > 0xffffffffffffffff) | ((var_a + 0xe0) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0xe0;
        return abi.encodePacked(uint64(var_a.length), uint64(var_af), address(var_ag), address(var_ah), address(var_ai), var_aj, var_ak);
    }
    
    /// @custom:selector    0xc6d114f1
    /// @custom:signature   Unresolved_c6d114f1(uint256 arg0, address arg1, address arg2, uint256 arg3, uint256 arg4, uint256 arg5) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg2 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    /// @param              arg5 ["uint256", "bytes32", "int256"]
    function Unresolved_c6d114f1(uint256 arg0, address arg1, address arg2, uint256 arg3, uint256 arg4, uint256 arg5) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
        require(address(arg2) - arg2);
        require(arg3 - arg3);
        require(arg4 - arg4);
        require(arg5 - arg5);
    }
}