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
    mapping(bytes32 => bytes32) storage_map_l;
    mapping(bytes32 => bytes32) storage_map_h;
    mapping(bytes32 => bytes32) storage_map_a;
    mapping(bytes32 => bytes32) storage_map_i;
    mapping(bytes32 => bytes32) storage_map_c;
    mapping(bytes32 => bytes32) storage_map_f;
    mapping(bytes32 => bytes32) storage_map_j;
    mapping(bytes32 => bytes32) storage_map_m;
    mapping(bytes32 => bytes32) storage_map_g;
    mapping(bytes32 => bytes32) storage_map_e;
    mapping(bytes32 => bytes32) storage_map_n;
    mapping(bytes32 => bytes32) storage_map_b;
    mapping(bytes32 => bytes32) storage_map_o;
    mapping(bytes32 => bytes32) storage_map_d;
    mapping(bytes32 => bytes32) storage_map_k;
    
    error TokenAlreadyExists();
    event Event_0aadf93e();
    event Event_a6a2df24();
    
    /// @custom:selector    0x30321dcc
    /// @custom:signature   blacklistMerchant(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function blacklistMerchant(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(storage_map_a[var_a] == 0, CustomError_ea8e4eb5());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0x01, CustomError_ea8e4eb5());
        var_a = address(arg0);
        require(!(bytes1(storage_map_c[var_a])), CustomError_5f765689());
        var_a = address(arg0);
        require(bytes1(storage_map_b[var_a]), CustomError_5f765689());
        var_a = address(arg0);
        storage_map_b[var_a] = (bytes1(0x01)) | (uint248(storage_map_b[var_a]));
        var_a = storage_map_a[var_a];
        if (address(storage_map_d[var_a]) == 0) {
            var_a = address(storage_map_d[var_a]);
            require(address(storage_map_d[var_a]) == 0, CustomError_a6af7ebe());
            require(address(storage_map_b[var_a]) == (address(arg0)), CustomError_a6af7ebe());
            var_a = address(storage_map_d[var_a]);
            require(address(storage_map_b[var_a]) == (address(arg0)), CustomError_a6af7ebe());
            require(!0x01, CustomError_a6af7ebe());
            require(address(storage_map_d[var_a]) - (address(storage_map_d[var_a])), CustomError_a6af7ebe());
            var_a = address(storage_map_b[var_a]);
            require(address(storage_map_b[var_a]) == (address(arg0)), CustomError_a6af7ebe());
        }
        require(0x01, CustomError_a6af7ebe());
        emit Event_0aadf93e(address(arg0), 0x01);
        var_a = address(arg0);
        require(storage_map_e[var_a] == 0, CustomError_a6af7ebe());
        var_a = address(arg0);
        require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
        require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
        require(!(0x06 > 0x02), CustomError_a6af7ebe());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(0x06 > (bytes1(storage_map_g[0 + keccak256(var_a)]))), CustomError_a6af7ebe());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_g[0 + keccak256(var_a)]) - 0x02, CustomError_a6af7ebe());
        var_a = storage_map_h[0 + keccak256(var_a)];
        require(0 > (0 + storage_map_b[var_a]), CustomError_a6af7ebe());
        var_a = storage_map_e[var_a];
        require(address(storage_map_i[var_a]) - (storage_map_i[var_a]) > (address(storage_map_i[var_a])), CustomError_a6af7ebe());
        var_a = storage_map_a[var_a];
        var_a = address(arg0);
        var_a = address(storage_map_d[var_a]);
        storage_map_b[var_a] = (address(storage_map_b[var_a])) | (uint96(storage_map_b[var_a]));
        require(address(storage_map_d[var_a]) == (address(arg0)), CustomError_a6af7ebe());
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (uint96(storage_map_b[var_a]));
        emit Event_0aadf93e(address(arg0), 0x01);
        storage_map_d[var_a] = (address(storage_map_d[var_a])) | (uint96(storage_map_d[var_a]));
        require(address(storage_map_d[var_a]) == (address(arg0)), CustomError_a6af7ebe());
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (uint96(storage_map_b[var_a]));
        emit Event_0aadf93e(address(arg0), 0x01);
        storage_map_d[var_a] = 0 | (uint96(storage_map_d[var_a]));
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (uint96(storage_map_b[var_a]));
        emit Event_0aadf93e(address(arg0), 0x01);
        require(0, CustomError_a6af7ebe());
        emit Event_0aadf93e(address(arg0), 0x01);
        emit Event_0aadf93e(address(arg0), 0x01);
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        var_a = storage_map_a[var_a];
        require(address(storage_map_d[var_a]) == (address(msg.sender)), CustomError_ea8e4eb5());
        require(((var_h + (uint248(0x1f + (((((var_h + 0x20) + 0x20) + 0x14) + 0x04) - var_h)))) > 0xffffffffffffffff) | ((var_h + (uint248(0x1f + (((((var_h + 0x20) + 0x20) + 0x14) + 0x04) - var_h)))) < var_h), CustomError_ea8e4eb5());
        var_a = keccak256(var_i);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0, CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x2b2a198c
    /// @custom:signature   Unresolved_2b2a198c(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_2b2a198c(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(storage_map_a[var_a] == 0, CustomError_ea8e4eb5());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0x01, CustomError_ea8e4eb5());
        var_a = address(arg0);
        storage_map_d[var_a] = (bytes1(!storage_map_d[var_a])) | (uint248(storage_map_d[var_a]));
        var_a = storage_map_a[var_a];
        var_a = address(arg0);
        if (!storage_map_d[var_a]) {
            if (storage_map_d[var_a]) {
                if (storage_map_d[var_a]) {
                    if (address(storage_map_b[var_a]) == 0) {
                        var_a = storage_map_a[var_a];
                        require(!(bytes1(storage_map_d[var_a])), CustomError_ea8e4eb5());
                        var_a = address(storage_map_d[var_a]);
                        require(!(!bytes1(storage_map_d[var_a])), CustomError_ea8e4eb5());
                        require(!(!bytes1(storage_map_d[var_a])), CustomError_ea8e4eb5());
                        var_a = address(storage_map_d[var_a]);
                        require(!(!(address(storage_map_b[var_a])) == 0), CustomError_ea8e4eb5());
                        require(address(storage_map_d[var_a]) == 0, CustomError_ea8e4eb5());
                        require(address(storage_map_b[var_a]) == (address(arg0)), CustomError_ea8e4eb5());
                        var_a = address(storage_map_b[var_a]);
                        require(address(storage_map_b[var_a]) == (address(arg0)), CustomError_ea8e4eb5());
                    }
                    require(!0x01, CustomError_ea8e4eb5());
                    var_a = address(arg0);
                    require(address(storage_map_d[var_a]) - (address(storage_map_d[var_a])), CustomError_ea8e4eb5());
                    require(address(storage_map_b[var_a]) == (address(arg0)), CustomError_ea8e4eb5());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(0x01, CustomError_ea8e4eb5());
                    require(!(bytes1(storage_map_b[var_a])), CustomError_ea8e4eb5());
                    var_a = keccak256(var_a);
                    require(((storage_map_b[var_a] / 0x02) < 0x20) == (bytes1(storage_map_b[var_a])), CustomError_ea8e4eb5());
                    require(!(bytes1(storage_map_b[var_a])), CustomError_ea8e4eb5());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    var_a = keccak256(var_a) + 0x02;
                    require(0x01 == (bytes1(storage_map_b[var_a])), CustomError_ea8e4eb5());
                    require(0 < (storage_map_b[var_a] / 0x02), CustomError_ea8e4eb5());
                    require(!(0x02 > (bytes1(storage_map_d[var_a]))), CustomError_ea8e4eb5());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(0 < (storage_map_f[var_a]), CustomError_ea8e4eb5());
                    require(!(bytes1(storage_map_f[var_a])), CustomError_ea8e4eb5());
                    var_a = keccak256(var_a) + 0x02;
                    require(((storage_map_f[var_a] / 0x02) < 0x20) == (bytes1(storage_map_f[var_a])), CustomError_ea8e4eb5());
                    require(!(bytes1(storage_map_f[var_a])), CustomError_ea8e4eb5());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    emit Event_a6a2df24(address(arg0), (var_d + 0x0180) - var_d, storage_map_i[var_a], !(!bytes1(storage_map_d[var_a])), storage_map_f[var_a], storage_map_j[var_a], storage_map_k[var_a], storage_map_l[var_a], !(!bytes1(storage_map_c[var_a])), storage_map_e[var_a], storage_map_m[var_a], storage_map_a[var_a], storage_map_n[var_a], 0x60, bytes1(storage_map_d[var_a]), ((0x20 + ((var_d + 0x0180) + 0x60)) + 0) - (var_d + 0x0180), storage_map_b[var_a] / 0x02, storage_map_f[var_a]);
                    var_a = address(arg0);
                    require(0x01 == (bytes1(storage_map_f[var_a])), CustomError_ea8e4eb5());
                    var_a = address(arg0);
                    require(0 < (storage_map_f[var_a] / 0x02), CustomError_ea8e4eb5());
                    require(!(0x06 > (bytes1(storage_map_j[var_a]))), CustomError_ea8e4eb5());
                    var_a = keccak256(var_a) + 0x02;
                    require(storage_map_e[var_a] == 0, CustomError_ea8e4eb5());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(!(0 < (storage_map_f[var_a])), CustomError_ea8e4eb5());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(!(0 < (storage_map_f[var_a])), CustomError_ea8e4eb5());
                    var_a = storage_map_h[0 + keccak256(var_a)];
                    require(!(0x06 > 0x02), CustomError_ea8e4eb5());
                    var_a = storage_map_e[var_a];
                    require(!(0x06 > (bytes1(storage_map_g[0 + keccak256(var_a)]))), CustomError_ea8e4eb5());
                    var_a = storage_map_a[var_a];
                    var_a = address(arg0);
                    var_a = address(storage_map_d[var_a]);
                    storage_map_b[var_a] = (address(storage_map_b[var_a])) | (uint96(storage_map_b[var_a]));
                    require(bytes1(storage_map_g[0 + keccak256(var_a)]) - 0x02, CustomError_ea8e4eb5());
                    storage_map_d[var_a] = (address(storage_map_d[var_a])) | (uint96(storage_map_d[var_a]));
                    require(0 > (0 + storage_map_b[var_a]), CustomError_ea8e4eb5());
                }
            }
            require(address(storage_map_i[var_a]) - (storage_map_i[var_a]) > (address(storage_map_i[var_a])), CustomError_ea8e4eb5());
        }
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        var_a = storage_map_a[var_a];
        require(address(storage_map_d[var_a]) == (address(msg.sender)), CustomError_ea8e4eb5());
        require(((var_d + (uint248(0x1f + (((((var_d + 0x20) + 0x20) + 0x14) + 0x04) - var_d)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + (((((var_d + 0x20) + 0x20) + 0x14) + 0x04) - var_d)))) < var_d), CustomError_ea8e4eb5());
        var_a = keccak256(var_aa);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0, CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x017a99ff
    /// @custom:signature   Unresolved_017a99ff(uint256 arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_017a99ff(uint256 arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
    }
    
    /// @custom:selector    0x3e10bc04
    /// @custom:signature   Unresolved_3e10bc04(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_3e10bc04(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0x90dc5b65
    /// @custom:signature   toggleOnlineOffline() public payable
    function toggleOnlineOffline() public payable {
        require(msg.value);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_c[var_a])), CustomError_9ae55bc7());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_9ae55bc7());
        var_a = address(msg.sender);
        storage_map_d[var_a] = (bytes1(!storage_map_d[var_a])) | (uint248(storage_map_d[var_a]));
        var_a = storage_map_a[var_a];
        var_a = address(msg.sender);
        if (!storage_map_d[var_a]) {
            if (storage_map_d[var_a]) {
                if (storage_map_d[var_a]) {
                    if (address(storage_map_b[var_a]) == 0) {
                        var_a = storage_map_a[var_a];
                        require(!(bytes1(storage_map_d[var_a])), CustomError_a6af7ebe());
                        var_a = address(storage_map_d[var_a]);
                        require(!(!bytes1(storage_map_d[var_a])), CustomError_a6af7ebe());
                        require(!(!bytes1(storage_map_d[var_a])), CustomError_a6af7ebe());
                        var_a = address(storage_map_d[var_a]);
                        require(!(!(address(storage_map_b[var_a])) == 0), CustomError_a6af7ebe());
                        require(address(storage_map_d[var_a]) == 0, CustomError_a6af7ebe());
                        require(address(storage_map_b[var_a]) == (address(msg.sender)), CustomError_a6af7ebe());
                        var_a = address(storage_map_b[var_a]);
                        require(address(storage_map_b[var_a]) == (address(msg.sender)), CustomError_a6af7ebe());
                    }
                    require(!0x01, CustomError_a6af7ebe());
                    var_a = address(msg.sender);
                    require(address(storage_map_d[var_a]) - (address(storage_map_d[var_a])), CustomError_a6af7ebe());
                    require(address(storage_map_b[var_a]) == (address(msg.sender)), CustomError_a6af7ebe());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(0x01, CustomError_a6af7ebe());
                    require(!(bytes1(storage_map_b[var_a])), CustomError_a6af7ebe());
                    var_a = keccak256(var_a);
                    require(((storage_map_b[var_a] / 0x02) < 0x20) == (bytes1(storage_map_b[var_a])), CustomError_a6af7ebe());
                    require(!(bytes1(storage_map_b[var_a])), CustomError_a6af7ebe());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    var_a = keccak256(var_a) + 0x02;
                    require(0x01 == (bytes1(storage_map_b[var_a])), CustomError_a6af7ebe());
                    require(0 < (storage_map_b[var_a] / 0x02), CustomError_a6af7ebe());
                    require(!(0x02 > (bytes1(storage_map_d[var_a]))), CustomError_a6af7ebe());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(0 < (storage_map_f[var_a]), CustomError_a6af7ebe());
                    require(!(bytes1(storage_map_f[var_a])), CustomError_a6af7ebe());
                    var_a = keccak256(var_a) + 0x02;
                    require(((storage_map_f[var_a] / 0x02) < 0x20) == (bytes1(storage_map_f[var_a])), CustomError_a6af7ebe());
                    require(!(bytes1(storage_map_f[var_a])), CustomError_a6af7ebe());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    emit Event_a6a2df24(address(msg.sender), (var_d + 0x0180) - var_d, storage_map_i[var_a], !(!bytes1(storage_map_d[var_a])), storage_map_f[var_a], storage_map_j[var_a], storage_map_k[var_a], storage_map_l[var_a], !(!bytes1(storage_map_c[var_a])), storage_map_e[var_a], storage_map_m[var_a], storage_map_a[var_a], storage_map_n[var_a], 0x60, bytes1(storage_map_d[var_a]), ((0x20 + ((var_d + 0x0180) + 0x60)) + 0) - (var_d + 0x0180), storage_map_b[var_a] / 0x02, storage_map_f[var_a]);
                    var_a = address(msg.sender);
                    require(0x01 == (bytes1(storage_map_f[var_a])), CustomError_a6af7ebe());
                    var_a = address(msg.sender);
                    require(0 < (storage_map_f[var_a] / 0x02), CustomError_a6af7ebe());
                    require(!(0x06 > (bytes1(storage_map_j[var_a]))), CustomError_a6af7ebe());
                    var_a = keccak256(var_a) + 0x02;
                    require(storage_map_e[var_a] == 0, CustomError_a6af7ebe());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
                    var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
                    require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
                    var_a = storage_map_h[0 + keccak256(var_a)];
                    require(!(0x06 > 0x02), CustomError_a6af7ebe());
                    var_a = storage_map_e[var_a];
                    require(!(0x06 > (bytes1(storage_map_g[0 + keccak256(var_a)]))), CustomError_a6af7ebe());
                    var_a = storage_map_a[var_a];
                    var_a = address(msg.sender);
                    var_a = address(storage_map_d[var_a]);
                    storage_map_b[var_a] = (address(storage_map_b[var_a])) | (uint96(storage_map_b[var_a]));
                    require(bytes1(storage_map_g[0 + keccak256(var_a)]) - 0x02, CustomError_a6af7ebe());
                    storage_map_d[var_a] = (address(storage_map_d[var_a])) | (uint96(storage_map_d[var_a]));
                    require(0 > (0 + storage_map_b[var_a]), CustomError_a6af7ebe());
                }
            }
            require(address(storage_map_i[var_a]) - (storage_map_i[var_a]) > (address(storage_map_i[var_a])), CustomError_a6af7ebe());
        }
    }
    
    /// @custom:selector    0x8cc93d61
    /// @custom:signature   Unresolved_8cc93d61(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_8cc93d61(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x014a4bc6
    /// @custom:signature   Unresolved_014a4bc6(address arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    function Unresolved_014a4bc6(address arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
        require(arg2 - arg2);
        require(arg3 - arg3);
        require(arg4 - arg4);
    }
    
    /// @custom:selector    0xeb91e651
    /// @custom:signature   removeBlacklist(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function removeBlacklist(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(storage_map_a[var_a] == 0, CustomError_ea8e4eb5());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0x01, CustomError_ea8e4eb5());
        var_a = address(arg0);
        if (!storage_map_b[var_a]) {
            var_a = address(arg0);
            require(!(bytes1(storage_map_b[var_a])), CustomError_a6af7ebe());
            var_a = address(arg0);
            storage_map_b[var_a] = (bytes1(0)) | (uint248(storage_map_b[var_a]));
            var_a = address(arg0);
            require(!(bytes1(storage_map_c[var_a])), CustomError_a6af7ebe());
            require(!(bytes1(storage_map_d[var_a])), CustomError_a6af7ebe());
            var_a = address(arg0);
            require(!(bytes1(storage_map_c[var_a])), CustomError_a6af7ebe());
            require(bytes1(storage_map_b[var_a]), CustomError_a6af7ebe());
            var_a = address(arg0);
            require(!0, CustomError_a6af7ebe());
            require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
            var_a = keccak256(var_a) + 0x02;
            require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
            require(!(0x06 > 0x02), CustomError_a6af7ebe());
            var_a = storage_map_a[var_a];
            var_a = address(arg0);
            require(!0, CustomError_a6af7ebe());
            emit Event_0aadf93e(address(arg0), 0);
            require(address(storage_map_b[var_a]) - 0, CustomError_a6af7ebe());
            var_a = address(storage_map_d[var_a]);
            var_a = address(arg0);
            storage_map_b[var_a] = (address(storage_map_b[var_a])) | (uint96(storage_map_b[var_a]));
            var_a = address(storage_map_d[var_a]);
            storage_map_b[var_a] = (address(arg0)) | (uint96(storage_map_b[var_a]));
            storage_map_d[var_a] = (address(arg0)) | (uint96(storage_map_d[var_a]));
            var_a = address(arg0);
            require(address(storage_map_d[var_a]) - 0, CustomError_a6af7ebe());
            var_a = address(arg0);
            require(storage_map_e[var_a] == 0, CustomError_a6af7ebe());
            require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
            var_a = keccak256(var_a) + 0x02;
            require(!(0 < (storage_map_f[var_a])), CustomError_a6af7ebe());
            require(!(0x06 > 0x02), CustomError_a6af7ebe());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(!(0x06 > (bytes1(storage_map_g[0 + keccak256(var_a)]))), CustomError_a6af7ebe());
            var_a = storage_map_h[0 + keccak256(var_a)];
            require(bytes1(storage_map_g[0 + keccak256(var_a)]) - 0x02, CustomError_a6af7ebe());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            emit Event_0aadf93e(address(arg0), 0);
            emit Event_0aadf93e(address(arg0), 0);
            require(0 > (0 + storage_map_b[var_a]), CustomError_a6af7ebe());
            require(storage_map_b[var_a] == 0, CustomError_a6af7ebe());
        }
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        var_a = storage_map_a[var_a];
        require(address(storage_map_d[var_a]) == (address(msg.sender)), CustomError_ea8e4eb5());
        require(((var_h + (uint248(0x1f + (((((var_h + 0x20) + 0x20) + 0x14) + 0x04) - var_h)))) > 0xffffffffffffffff) | ((var_h + (uint248(0x1f + (((((var_h + 0x20) + 0x20) + 0x14) + 0x04) - var_h)))) < var_h), CustomError_ea8e4eb5());
        var_a = keccak256(var_i);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0, CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0xbae22911
    /// @custom:signature   Unresolved_bae22911(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_bae22911(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x37a4eea1
    /// @custom:signature   Unresolved_37a4eea1(uint256 arg0) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_37a4eea1(uint256 arg0) public view {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_c[var_a])), CustomError_9ae55bc7());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_9ae55bc7());
        require(arg0 == 0, CustomError_c991cbb1());
        var_a = address(msg.sender);
        require(!(0 < storage_map_b[var_a]), CustomError_c991cbb1());
        require(!(0 < storage_map_b[var_a]), CustomError_c991cbb1());
        var_a = keccak256(var_a);
        require(!(bytes1(storage_map_o[var_a])), CustomError_c991cbb1());
        require(((storage_map_o[var_a] / 0x02) < 0x20) == (bytes1(storage_map_o[var_a])), CustomError_c991cbb1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(bytes1(storage_map_o[var_a])), CustomError_c991cbb1());
        require(0x01 == (bytes1(storage_map_o[var_a])), CustomError_c991cbb1());
        var_a = 0 + keccak256(var_a);
        require(0 < (storage_map_o[var_a] / 0x02), CustomError_c991cbb1());
        require(((var_e + (uint248(0x1f + (((0x20 + var_e) + 0) - var_e)))) > 0xffffffffffffffff) | ((var_e + (uint248(0x1f + (((0x20 + var_e) + 0) - var_e)))) < var_e), CustomError_c991cbb1());
        uint248 var_e = var_e + (uint248(0x1f + (((0x20 + var_e) + 0) - var_e)));
        require(arg0 > 0xffffffffffffffff, CustomError_c991cbb1());
        require(((var_e + (uint248(0x1f + ((0x1f + (arg0)) + 0x20)))) > 0xffffffffffffffff) | ((var_e + (uint248(0x1f + ((0x1f + (arg0)) + 0x20)))) < var_e), CustomError_c991cbb1());
        require(keccak256(var_i) == (keccak256(var_i)), CustomError_c991cbb1());
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_c991cbb1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(storage_map_b[var_a] < 0x010000000000000000), CustomError_9f11a53f());
    }
}