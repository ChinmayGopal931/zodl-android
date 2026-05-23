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
    uint256 store_b;
    mapping(bytes32 => bytes32) storage_map_a;
    
    error NotSuperAdmin();
    
    /// @custom:selector    0x65b36b69
    /// @custom:signature   Unresolved_65b36b69(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_65b36b69(uint256 arg0) public payable {
        require(msg.value);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        store_b = arg0;
    }
}