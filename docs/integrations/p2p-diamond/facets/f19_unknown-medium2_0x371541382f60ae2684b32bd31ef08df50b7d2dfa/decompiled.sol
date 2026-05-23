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
    mapping(bytes32 => bytes32) storage_map_c;
    mapping(bytes32 => bytes32) storage_map_a;
    mapping(bytes32 => bytes32) storage_map_d;
    mapping(bytes32 => bytes32) storage_map_f;
    mapping(bytes32 => bytes32) storage_map_b;
    bytes32 store_e;
    
    event Event_f9bcee6f();
    error NotSuperAdmin();
    
    /// @custom:selector    0x36ed9d14
    /// @custom:signature   Unresolved_36ed9d14(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_36ed9d14(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xb92046a2
    /// @custom:signature   Unresolved_b92046a2(uint256 arg0) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_b92046a2(uint256 arg0) public view {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_29c3b7ee());
        uint256 var_b = arg0;
        require(address(storage_map_a[var_b]) == 0, CustomError_5d706033());
        var_b = arg0;
        require(!(0x03 > 0), CustomError_5d706033());
    }
    
    /// @custom:selector    0xc0bc0d14
    /// @custom:signature   Unresolved_c0bc0d14(uint256 arg0) public view returns (address)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_c0bc0d14(uint256 arg0) public view returns (address) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return address(storage_map_b[var_a]);
    }
    
    /// @custom:selector    0x17353447
    /// @custom:signature   Unresolved_17353447(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_17353447(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
        uint256 var_a = var_a + 0x80;
        address var_b = address(arg0);
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0x80;
        return abi.encodePacked(var_a.length, !(!var_t), var_u, address(var_v));
    }
    
    /// @custom:selector    0x4b5b392e
    /// @custom:signature   Unresolved_4b5b392e(address arg0, uint256 arg1, uint256 arg2, address arg3, uint256 arg4, uint256 arg5, uint256 arg6) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    /// @param              arg5 ["uint256", "bytes32", "int256"]
    /// @param              arg6 ["uint256", "bytes32", "int256"]
    function Unresolved_4b5b392e(address arg0, uint256 arg1, uint256 arg2, address arg3, uint256 arg4, uint256 arg5, uint256 arg6) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
        require(arg2 - arg2);
        require(address(arg3) - arg3);
        require(arg4 > 0xffffffffffffffff);
        require(arg4 > 0xffffffffffffffff);
        require(arg5 - arg5);
        require(arg6 - arg6);
    }
    
    /// @custom:selector    0x3095f2be
    /// @custom:signature   Unresolved_3095f2be(address arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4, uint256 arg5) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    /// @param              arg5 ["uint256", "bytes32", "int256"]
    function Unresolved_3095f2be(address arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4, uint256 arg5) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
        require(arg2 - arg2);
        require(arg3 > 0xffffffffffffffff);
        require(arg3 > 0xffffffffffffffff);
        require(arg4 - arg4);
        require(arg5 - arg5);
    }
    
    /// @custom:selector    0x8b5ab606
    /// @custom:signature   Unresolved_8b5ab606(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_8b5ab606(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_c = address(msg.sender);
        (bool success, bytes memory ret0) = address(store_e).Unresolved_df7f453b(var_c); // staticcall
        require(!0, CustomError_16c726b1());
        address var_a = address(arg0);
        storage_map_f[var_a] = (bytes1(0)) | (uint248(storage_map_f[var_a]));
        emit Event_f9bcee6f(address(arg0));
        require(0x20 > ret0.length);
        require(((var_e + 0x20) > 0xffffffffffffffff) | ((var_e + 0x20) < var_e));
        uint256 var_e = var_e + 0x20;
        require(((var_e + 0x20) - var_e) < 0x20);
        require(var_e.length - var_e.length, CustomError_16c726b1());
        require(!var_e.length, CustomError_16c726b1());
        var_a = address(arg0);
        storage_map_f[var_a] = (bytes1(0)) | (uint248(storage_map_f[var_a]));
        emit Event_f9bcee6f(address(arg0));
    }
    
    /// @custom:selector    0x1e61c167
    /// @custom:signature   Unresolved_1e61c167(uint256 arg0) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_1e61c167(uint256 arg0) public view {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_29c3b7ee());
        uint256 var_b = arg0;
        require(address(storage_map_a[var_b]) == 0, CustomError_5d706033());
        var_b = arg0;
        require(!(0x03 > 0), CustomError_5d706033());
    }
}