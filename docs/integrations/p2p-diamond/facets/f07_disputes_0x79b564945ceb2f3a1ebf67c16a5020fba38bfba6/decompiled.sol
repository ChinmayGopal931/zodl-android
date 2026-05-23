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
    mapping(bytes32 => bytes32) storage_map_h;
    mapping(bytes32 => bytes32) storage_map_a;
    bytes32 store_e;
    bytes32 store_c;
    address store_b;
    mapping(bytes32 => bytes32) storage_map_g;
    bytes32 store_f;
    bytes32 store_d;
    
    error AddressInsufficientBalance(address);
    event EmergencyWithdrawal(address, uint256);
    event ReputationManagerSet(address);
    
    /// @custom:selector    0x56a837f4
    /// @custom:signature   setReputationManager(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function setReputationManager(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        store_b = (address(arg0)) | (uint96(store_b));
        emit ReputationManagerSet(address(arg0));
    }
    
    /// @custom:selector    0xfce1235c
    /// @custom:signature   failSafe(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function failSafe(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_8beb9d16());
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_c), CustomError_8beb9d16());
        store_c = (bytes1(0x01)) | (uint248(store_c));
        address var_e = address(msg.sender);
        uint256 var_c = (((var_g + 0x20) + 0x04) + 0x40) - (var_g + 0x20);
        require(((var_g + (uint248(0x1f + ((((var_g + 0x20) + 0x04) + 0x40) - var_g)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + ((((var_g + 0x20) + 0x04) + 0x40) - var_g)))) < var_g), CustomError_cd786059());
        uint248 var_g = var_g + (uint248(0x1f + ((((var_g + 0x20) + 0x04) + 0x40) - var_g)));
        require(address(this).balance < 0, CustomError_cd786059());
        (bool success, bytes memory ret0) = address(store_d).{ value: 0 ether }Unresolved_a9059cbb(var_e); // call
        require(!ret0.length);
        require(!var_k > 0);
        require(var_k == 0, CustomError_5274afe7());
        require(var_k == 0, CustomError_5274afe7());
        require(!(var_k == 0), CustomError_5274afe7());
        require(((0x80 + var_k) - 0x80) < 0x20, CustomError_5274afe7());
        require(var_c - var_c, CustomError_5274afe7());
        require(!var_c, CustomError_5274afe7());
        emit EmergencyWithdrawal(address(msg.sender), arg0);
        require(address(msg.sender) - (address(this)));
        store_e = (bytes1(0)) | (uint248(store_e));
        require(address(store_d).code.length == 0, CustomError_9996b315());
        require(ret0.length > 0xffffffffffffffff, CustomError_1425ea42());
        require(((var_g + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) < var_g), CustomError_1425ea42());
        var_g = var_g + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)));
        require(!(var_g.length > 0), CustomError_1425ea42());
        require(var_g.length == 0, CustomError_9996b315());
        require(address(store_d).code.length == 0, CustomError_9996b315());
        require(!(var_g.length == 0), CustomError_5274afe7());
        require(!(var_g.length == 0), CustomError_5274afe7());
    }
    
    /// @custom:selector    0x1b624123
    /// @custom:signature   Unresolved_1b624123(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_1b624123(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xe66128d7
    /// @custom:signature   Unresolved_e66128d7(uint256 arg0) public payable returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_e66128d7(uint256 arg0) public payable returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_c = arg0;
        (bool success, bytes memory ret0) = address(store_f).Unresolved_67c84efd(var_c); // staticcall
        uint256 var_a = arg0;
        uint256 var_d = var_a + 0x08;
        require(!((storage_map_a[var_a] * 0x0f4240) / storage_map_a[var_a] == 0x0f4240) | !storage_map_a[var_a]);
        require(!var_d);
        var_a = arg0;
        var_d = 0xd8b7119d98f3b9a11f68b97c850ed595ac3b152112579e0a054fa14456ba3ada;
        require(storage_map_a[var_a] - ((storage_map_a[var_a] * 0x0f4240) / var_d) > storage_map_a[var_a]);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return storage_map_a[var_a] - ((storage_map_a[var_a] * 0x0f4240) / var_d);
        require(0x80 > ret0.length);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        uint256 var_f = var_f + 0x80;
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        var_f = var_f + 0x80;
        require(var_f.length - var_f.length);
    }
    
    /// @custom:selector    0x9f686097
    /// @custom:signature   Unresolved_9f686097(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_9f686097(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x29ef9e00
    /// @custom:signature   Unresolved_29ef9e00(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_29ef9e00(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x66b7467b
    /// @custom:signature   Unresolved_66b7467b(uint256 arg0, uint256 arg1) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_66b7467b(uint256 arg0, uint256 arg1) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(!0x04 > arg1);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_c), CustomError_8beb9d16());
        store_c = (bytes1(0x01)) | (uint248(store_c));
        uint256 var_a = arg0;
        require(storage_map_g[var_a] == 0, CustomError_ea8e4eb5());
        var_a = address(msg.sender);
        require(bytes1(storage_map_a[var_a]), CustomError_ea8e4eb5());
        require(!0x01, CustomError_ea8e4eb5());
        require(!(0x04 > 0), CustomError_b4fa3fb3());
        require(!(0x04 > arg1), CustomError_b4fa3fb3());
        require(arg1 == 0, CustomError_b4fa3fb3());
        require(!(0x03 > 0x01), CustomError_b4fa3fb3());
        var_a = address(msg.sender);
        require(bytes1(storage_map_a[var_a]), CustomError_ea8e4eb5());
        var_a = storage_map_g[var_a];
        require(address(storage_map_h[var_a]) == (address(msg.sender)), CustomError_ea8e4eb5());
        require(((var_h + (uint248(0x1f + (((((var_h + 0x20) + 0x20) + 0x14) + 0x04) - var_h)))) > 0xffffffffffffffff) | ((var_h + (uint248(0x1f + (((((var_h + 0x20) + 0x20) + 0x14) + 0x04) - var_h)))) < var_h), CustomError_ea8e4eb5());
        var_a = keccak256(var_i);
        require(bytes1(storage_map_a[var_a]), CustomError_ea8e4eb5());
        require(!0, CustomError_ea8e4eb5());
    }
}