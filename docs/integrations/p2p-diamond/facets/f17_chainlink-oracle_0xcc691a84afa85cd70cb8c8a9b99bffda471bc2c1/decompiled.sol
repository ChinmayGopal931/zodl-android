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
    address public unresolved_73fb8124;
    address store_b;
    
    event Event_9785481d();
    event SellPriceUpdated(address, bytes32, uint256, uint256);
    error NotSuperAdmin();
    event BuyPriceUpdated(address, bytes32, uint256, uint256);
    
    /// @custom:selector    0x8f865cd6
    /// @custom:signature   Unresolved_8f865cd6(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_8f865cd6(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_81d88eeb());
        require(!(address(arg0) == 0), CustomError_81d88eeb());
        require(!(address(store_b) == 0), CustomError_81d88eeb());
        address var_d = address(store_b);
        require(address(arg0) == 0);
        require(address(arg0) == 0);
        store_b = (address(arg0)) | (uint96(store_b));
        emit Event_9785481d(address(msg.sender), address(store_b), address(arg0));
        (bool success, bytes memory ret0) = address(store_b).Unresolved_2605224f(var_d); // staticcall
        require(0, CustomError_87bf6e69());
        var_d = 0;
        (bool success, bytes memory ret0) = address(store_b).Unresolved_602b67cb(var_d); // staticcall
        require(0, CustomError_fde2d25d());
        store_b = (address(arg0)) | (uint96(store_b));
        emit Event_9785481d(address(msg.sender), address(store_b), address(arg0));
        require(0x20 > ret0.length);
        require(((var_e + 0x20) > 0xffffffffffffffff) | ((var_e + 0x20) < var_e));
        uint256 var_e = var_e + 0x20;
        require(((var_e + 0x20) - var_e) < 0x20);
        require(var_e.length - var_e.length);
        require(var_e.length - 0, CustomError_fde2d25d());
        store_b = (address(arg0)) | (uint96(store_b));
        emit Event_9785481d(address(msg.sender), address(store_b), address(arg0));
        if (0x20 > ret0.length) {
        }
        require(!(address(store_b) == 0), CustomError_16c726b1());
        store_b = (address(arg0)) | (uint96(store_b));
        emit Event_9785481d(address(msg.sender), address(store_b), address(arg0));
    }
    
    /// @custom:selector    0xcfb51928
    /// @custom:signature   Unresolved_cfb51928(uint256 arg0) public pure returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_cfb51928(uint256 arg0) public pure returns (uint256) {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(((var_c + (uint248(0x1f + ((0x1f + (arg0)) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((0x1f + (arg0)) + 0x20)))) < var_c));
        return keccak256(var_g);
    }
    
    /// @custom:selector    0x996979df
    /// @custom:signature   Unresolved_996979df(uint256 arg0, uint256 arg1, bool arg2, uint64 arg3, uint64 arg4, uint32 arg5) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["bool", "uint8", "bytes1", "int8"]
    /// @param              arg3 ["uint64", "bytes8", "int64"]
    /// @param              arg4 ["uint64", "bytes8", "int64"]
    /// @param              arg5 ["uint32", "bytes4", "int32"]
    function Unresolved_996979df(uint256 arg0, uint256 arg1, bool arg2, uint64 arg3, uint64 arg4, uint32 arg5) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(bytes1(arg2) - arg2);
        require(uint64(arg3) - arg3);
        require(uint64(arg4) - arg4);
        require(uint32(arg5) - arg5);
    }
    
    /// @custom:selector    0xce912508
    /// @custom:signature   updatePrice(bytes32 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function updatePrice(bytes32 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        require(storage_map_a[var_a] == 0, CustomError_bb6c216c());
        require(!0x01, CustomError_bb6c216c());
        var_a = arg0;
        require(!(storage_map_a[var_a] == 0), CustomError_bb6c216c());
        require(storage_map_a[var_a] == 0, CustomError_bb6c216c());
        var_a = arg0;
        require(storage_map_c[var_a] == 0, CustomError_bb6c216c());
        require(!(((storage_map_a[var_a] * 0x0f4240) / storage_map_a[var_a] == 0x0f4240) | !storage_map_a[var_a]), CustomError_bb6c216c());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(storage_map_c[var_a]), CustomError_bb6c216c());
        require(!(storage_map_a[var_a] > ((storage_map_a[var_a] * 0x0f4240) / (storage_map_c[var_a]))), CustomError_bb6c216c());
        require(storage_map_a[var_a] - ((storage_map_a[var_a] * 0x0f4240) / (storage_map_c[var_a])) > storage_map_a[var_a], CustomError_bb6c216c());
        require(!(0 == 0), CustomError_bb6c216c());
        require(0 == 0, CustomError_bb6c216c());
        var_a = arg0;
        require(!(0 == 0), CustomError_bb6c216c());
        require(0 == 0, CustomError_bb6c216c());
        require(0 - (storage_map_d[var_a]), CustomError_bb6c216c());
        storage_map_d[var_a] = 0 | (0 & (storage_map_d[var_a]));
        uint256 var_g = 0;
        emit BuyPriceUpdated(address(msg.sender), arg0, storage_map_d[var_a], 0);
        require(0 - (storage_map_c[var_a]), CustomError_bb6c216c());
        storage_map_c[var_a] = var_g | (0 & (storage_map_c[var_a]));
        emit SellPriceUpdated(address(msg.sender), arg0, storage_map_c[var_a], 0);
    }
    
    /// @custom:selector    0x5f5487ab
    /// @custom:signature   Unresolved_5f5487ab(uint256 arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_5f5487ab(uint256 arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
    }
    
    /// @custom:selector    0x0ca76175
    /// @custom:signature   Unresolved_0ca76175(uint256 arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_0ca76175(uint256 arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
    }
    
    /// @custom:selector    0x805f2132
    /// @custom:signature   Unresolved_805f2132(uint256 arg0, uint256 arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_805f2132(uint256 arg0, uint256 arg1) public pure {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
    }
    
    /// @custom:selector    0x95ce1c2d
    /// @custom:signature   Unresolved_95ce1c2d(uint256 arg0, uint256 arg1) public view returns (bool)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_95ce1c2d(uint256 arg0, uint256 arg1) public view returns (bool) {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        uint256 var_a = arg0;
        require(arg1 > 0xffffffffffffffff);
        require(((var_d + (uint248(0x1f + ((0x1f + (arg1)) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((0x1f + (arg1)) + 0x20)))) < var_d));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return !(!storage_map_a[var_a] == (keccak256(var_h)));
    }
    
    /// @custom:selector    0x5692dd55
    /// @custom:signature   Unresolved_5692dd55(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_5692dd55(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
}