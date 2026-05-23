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
    mapping(bytes32 => bytes32) storage_map_a;
    bytes32 store_c;
    mapping(bytes32 => bytes32) storage_map_b;
    
    
    /// @custom:selector    0xadfca15e
    /// @custom:signature   facetFunctionSelectors(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function facetFunctionSelectors(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        var_a = keccak256(var_a) + 0;
        if (0x01) {
            require(0x01);
            require(0 < (storage_map_a[var_a]));
            require(0x01 < (storage_map_a[var_a]));
            require(0x02 < (storage_map_a[var_a]));
            require(0x03 < (storage_map_a[var_a]));
            require(0x04 < (storage_map_a[var_a]));
            require(0x05 < (storage_map_a[var_a]));
            require(0x06 < (storage_map_a[var_a]));
            require(0x07 < (storage_map_a[var_a]));
            uint248 var_m = var_m + (uint248(0x1f + ((((((((((0x20 + var_m) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) - var_m)));
            return abi.encodePacked(0x20, var_m.length);
            require(((var_m + (uint248(0x1f + ((((((((((0x20 + var_m) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) - var_m)))) > 0xffffffffffffffff) | ((var_m + (uint248(0x1f + ((((((((((0x20 + var_m) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) + 0x20) - var_m)))) < var_m));
        }
    }
    
    /// @custom:selector    0x52ef6b2c
    /// @custom:signature   facetAddresses() public view returns (bytes memory)
    function facetAddresses() public view returns (bytes memory) {
        require(msg.value);
        if (0 < store_c) {
            if (((var_c + (uint248(0x1f + ((0x20 + var_c) - var_c)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((0x20 + var_c) - var_c)))) < var_c)) {
                uint248 var_c = var_c + (uint248(0x1f + ((0x20 + var_c) - var_c)));
                return abi.encodePacked(0x20, var_c.length);
            }
        }
    }
    
    /// @custom:selector    0x01ffc9a7
    /// @custom:signature   supportsInterface(bytes4 arg0) public view returns (bool)
    /// @param              arg0 ["uint32", "bytes4", "int32"]
    function supportsInterface(bytes4 arg0) public view returns (bool) {
        require(msg.value);
        require(uint32(arg0) - arg0);
        uint32 var_a = uint32(arg0);
        return !(!bytes1(storage_map_b[var_a]));
    }
    
    /// @custom:selector    0x7a0ed627
    /// @custom:signature   facets() public view
    function facets() public view {
        require(msg.value);
        if (store_c > 0xffffffffffffffff) {
            if (((var_c + (uint248(0x1f + ((store_c * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((store_c * 0x20) + 0x20)))) < var_c)) {
            }
        }
    }
}