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
    bytes32 store_a;
    
    fallback() external payable {
        var_a = 0x80;
        require(0x1f931c1c - (msg.data[0] >> 0xe0));
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(address(arg1) - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(!(address(msg.sender) == (address(store_a))), "LibDiamond: Must be contract owner");
        require(arg0 > 0xffffffffffffffff);
        require(((var_a + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) < var_a));
        uint256 var_a = var_a + (uint248(0x1f + ((arg0 * 0x20) + 0x20)));
        require((0x20 + (0x04 + arg0)) < ((0x20 + (0x04 + arg0)) + (arg0 * 0x20)));
        require((0x20 + (arg0)) > 0xffffffffffffffff);
        require(((var_a + 0x60) > 0xffffffffffffffff) | ((var_a + 0x60) < var_a));
        var_a = var_a + 0x60;
        require(address((0x20 + (arg0)) + (0x20 + (arg0))) - ((0x20 + (arg0)) + (0x20 + (arg0))));
        require(!0x03 > ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x20));
        require(((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) > 0xffffffffffffffff);
        require((0x20 + (arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) > 0xffffffffffffffff);
        require(((var_a + (uint248(0x1f + (((0x20 + (arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_a + (uint248(0x1f + (((0x20 + (arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) * 0x20) + 0x20)))) < var_a));
        require(((0x20 + (0x04 + arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) + 0x20) < (((0x20 + (0x04 + arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) + 0x20) + ((0x20 + (arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) * 0x20)));
        require(uint32((0x20 + (arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) + 0x20) - ((0x20 + (arg0)) + (0x20 + (arg0)) + ((0x20 + (arg0)) + (0x20 + (arg0)) + 0x40) + 0x20));
        require(arg2 > 0xffffffffffffffff);
    }
    
}