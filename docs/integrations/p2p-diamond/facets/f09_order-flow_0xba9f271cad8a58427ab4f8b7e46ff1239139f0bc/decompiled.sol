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
    
    error NotAuthorized();
    
    /// @custom:selector    0x1e31508e
    /// @custom:signature   paidBuyOrder(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function paidBuyOrder(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        uint256 var_a = arg0;
        address var_d = address(msg.sender);
        (bool success, bytes memory ret0) = address(store_b).Unresolved_24d7806c(var_d); // staticcall
        if (!0) {
            require(!0, CustomError_c56873ba());
            var_a = arg0;
            require(!0, CustomError_c56873ba());
            require(!(0x03 > 0), CustomError_c56873ba());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(!(0x03 > (bytes1(storage_map_c[var_a] >> 0x10))), CustomError_c56873ba());
            require(bytes1(storage_map_c[var_a] >> 0x10) - 0, CustomError_c56873ba());
            require(!(0x05 > 0), CustomError_c56873ba());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(!(0x05 > (bytes1(storage_map_c[var_a] >> 0x08))), CustomError_c56873ba());
            require(bytes1(storage_map_c[var_a] >> 0x08) - 0, CustomError_c56873ba());
            require(storage_map_d[var_a] == 0, CustomError_c56873ba());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(storage_map_d[var_a] > (storage_map_d[var_a] + 0xb4), CustomError_c56873ba());
            require(!(block.timestamp < (storage_map_d[var_a] + 0xb4)), CustomError_c56873ba());
        }
        require(0, CustomError_c56873ba());
        require(!(address(storage_map_e[var_a]) == (address(msg.sender))), CustomError_ea8e4eb5());
        require(0x20 > ret0.length);
        require(((var_f + 0x20) > 0xffffffffffffffff) | ((var_f + 0x20) < var_f));
        uint256 var_f = var_f + 0x20;
        require(((var_f + 0x20) - var_f) < 0x20);
        if (var_f.length - var_f.length) {
            require(var_f.length - var_f.length);
        }
    }
    
    /// @custom:selector    0x1c2b2b56
    /// @custom:signature   getDayKey(uint256 arg0) public pure returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getDayKey(uint256 arg0) public pure returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        require(!0x015180);
        return arg0 / 0x015180;
    }
    
    /// @custom:selector    0xca1f8c6e
    /// @custom:signature   getYearMonth(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getYearMonth(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(!0x015180);
        require(!0x023ab1);
    }
    
    /// @custom:selector    0x1d106060
    /// @custom:signature   Unresolved_1d106060(uint256 arg0, uint256 arg1) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_1d106060(uint256 arg0, uint256 arg1) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        uint256 var_a = arg0;
        var_a = arg0;
        require(!(address(msg.sender)) == (address(this)));
        require(!(address(storage_map_f[var_a])) == (address(msg.sender)));
        address var_d = address(msg.sender);
        (bool success, bytes memory ret0) = address(store_b).Unresolved_24d7806c(var_d); // staticcall
        require(!0, CustomError_c56873ba());
        require(!(0x03 > 0), CustomError_c56873ba());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(0x03 > (bytes1(storage_map_c[var_a] >> 0x10))), CustomError_c56873ba());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_c[var_a] >> 0x10) - 0, CustomError_c56873ba());
        require(!(0x05 > 0), CustomError_c56873ba());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(0x05 > (bytes1(storage_map_c[var_a] >> 0x08))), CustomError_c56873ba());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_c[var_a] >> 0x08) - 0, CustomError_c56873ba());
        require(storage_map_d[var_a] == 0, CustomError_c56873ba());
        require(storage_map_d[var_a] > (storage_map_d[var_a] + 0xb4), CustomError_c56873ba());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(block.timestamp < (storage_map_d[var_a] + 0xb4)), CustomError_c56873ba());
        require(0, CustomError_c56873ba());
        require(0x20 > ret0.length);
        require(((var_f + 0x20) > 0xffffffffffffffff) | ((var_f + 0x20) < var_f));
        uint256 var_f = var_f + 0x20;
        require(((var_f + 0x20) - var_f) < 0x20);
        require(var_f.length - var_f.length, CustomError_ea8e4eb5());
        require(!var_f.length, CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x92d66313
    /// @custom:signature   getYear(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getYear(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(!0x015180);
        require(!0x023ab1);
    }
    
    /// @custom:selector    0x271f3558
    /// @custom:signature   Unresolved_271f3558(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_271f3558(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xa037f4a1
    /// @custom:signature   Unresolved_a037f4a1(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_a037f4a1(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
}