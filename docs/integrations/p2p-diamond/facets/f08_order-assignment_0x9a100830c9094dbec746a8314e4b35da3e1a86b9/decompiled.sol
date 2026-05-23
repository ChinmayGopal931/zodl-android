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
    mapping(bytes32 => bytes32) storage_map_d;
    bytes32 store_a;
    mapping(bytes32 => bytes32) storage_map_e;
    bytes32 store_b;
    mapping(bytes32 => bytes32) storage_map_f;
    
    error CustomError_00000000();
    
    /// @custom:selector    0x514fcac7
    /// @custom:signature   cancelOrder(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function cancelOrder(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        require(!0x015180);
        require(!0x05 > 0x03);
    }
    
    /// @custom:selector    0x6d5da5ad
    /// @custom:signature   Unresolved_6d5da5ad(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_6d5da5ad(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x1dc46885
    /// @custom:signature   Unresolved_1dc46885(uint256 arg0, uint256 arg1, address arg2, uint256 arg3, uint256 arg4, uint256 arg5, uint256 arg6, uint256 arg7, uint256 arg8) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    /// @param              arg5 ["uint256", "bytes32", "int256"]
    /// @param              arg6 ["uint256", "bytes32", "int256"]
    /// @param              arg7 ["uint256", "bytes32", "int256"]
    /// @param              arg8 ["uint256", "bytes32", "int256"]
    function Unresolved_1dc46885(uint256 arg0, uint256 arg1, address arg2, uint256 arg3, uint256 arg4, uint256 arg5, uint256 arg6, uint256 arg7, uint256 arg8) public pure {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(arg1 - arg1);
        require(address(arg2) - arg2);
        require(!0x03 > arg3);
        require(arg4 > 0xffffffffffffffff);
        require(arg4 > 0xffffffffffffffff);
        require(arg5 > 0xffffffffffffffff);
        require(arg5 > 0xffffffffffffffff);
        require(arg6 - arg6);
        require(arg7 - arg7);
        require(arg8 - arg8);
    }
    
    /// @custom:selector    0x0a74aece
    /// @custom:signature   assignMerchants(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function assignMerchants(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        (bool success, bytes memory ret0) = address(store_b).Unresolved_(var_b); // staticcall
        require(!0, CustomError_02a6fdd2());
        var_c = storage_map_c[var_c];
        require(!(bytes1(storage_map_d[var_c])), CustomError_02a6fdd2());
        require(!(0x03 > 0), CustomError_02a6fdd2());
        require(0x20 > ret0.length);
        require(((var_f + 0x20) > 0xffffffffffffffff) | ((var_f + 0x20) < var_f));
        uint256 var_f = var_f + 0x20;
        require(((var_f + 0x20) - var_f) < 0x20);
        require(var_f.length - var_f.length, CustomError_4bbac5de());
        require(!var_f.length, CustomError_4bbac5de());
    }
    
    /// @custom:selector    0xdbc96580
    /// @custom:signature   Unresolved_dbc96580(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_dbc96580(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xee8e56bf
    /// @custom:signature   Unresolved_ee8e56bf(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_ee8e56bf(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xe8576b23
    /// @custom:signature   Unresolved_e8576b23(uint256 arg0, uint256 arg1, uint256 arg2) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_e8576b23(uint256 arg0, uint256 arg1, uint256 arg2) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg2 - arg2);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        require(!(0x03 > 0), CustomError_c56873ba());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(0x03 > (bytes1(storage_map_e[var_b] >> 0x10))), CustomError_c56873ba());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_e[var_b] >> 0x10) - 0, CustomError_c56873ba());
        require(!(0x05 > 0), CustomError_c56873ba());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(0x05 > (bytes1(storage_map_e[var_b] >> 0x08))), CustomError_c56873ba());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_e[var_b] >> 0x08) - 0, CustomError_c56873ba());
        require(storage_map_f[var_b] == 0, CustomError_c56873ba());
        require(storage_map_f[var_b] > (storage_map_f[var_b] + 0xb4), CustomError_c56873ba());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(block.timestamp < (storage_map_f[var_b] + 0xb4)), CustomError_c56873ba());
        require(0, CustomError_c56873ba());
    }
}