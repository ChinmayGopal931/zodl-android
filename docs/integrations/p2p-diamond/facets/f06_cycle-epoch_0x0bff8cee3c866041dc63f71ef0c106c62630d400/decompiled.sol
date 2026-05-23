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
    mapping(bytes32 => bytes32) storage_map_g;
    mapping(bytes32 => bytes32) storage_map_e;
    bytes32 store_a;
    mapping(bytes32 => bytes32) storage_map_h;
    bytes32 store_b;
    mapping(bytes32 => bytes32) storage_map_m;
    bytes32 store_i;
    mapping(bytes32 => bytes32) storage_map_c;
    mapping(bytes32 => bytes32) storage_map_f;
    mapping(bytes32 => bytes32) storage_map_j;
    mapping(bytes32 => bytes32) storage_map_k;
    uint256 store_d;
    
    error NoRewardsToClaim();
    event CycleRollover(uint256, uint256);
    
    /// @custom:selector    0x671c6865
    /// @custom:signature   Unresolved_671c6865(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_671c6865(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0x74f3d7a3
    /// @custom:signature   Unresolved_74f3d7a3() public payable
    function Unresolved_74f3d7a3() public payable {
        require(msg.value);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        require(store_b == 0, CustomError_73380d99());
        require(store_b > (store_b + 0x093a80), CustomError_73380d99());
        require(block.timestamp > (store_b + 0x093a80), CustomError_73380d99());
        address var_b = address(msg.sender);
        var_b = var_b;
        require(!0, CustomError_73380d99());
        require(!0, CustomError_73380d99());
        require(storage_map_c[var_b] - 0, CustomError_73380d99());
        require(store_d - (storage_map_c[var_b]) > (store_d), CustomError_73380d99());
        require(!0x093a80, CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((store_d - (storage_map_c[var_b]) / 0x093a80) < 0x02, CustomError_73380d99());
        require(storage_map_e[var_b] > (storage_map_e[var_b] + storage_map_f[var_b]), CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(storage_map_e[var_b] > (storage_map_e[var_b] + storage_map_f[var_b]), CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_e[var_b] = (uint256(storage_map_e[var_b] + storage_map_f[var_b])) | (0 & (storage_map_e[var_b]));
        storage_map_g[var_b] = (uint256(storage_map_h[var_b])) | (0 & (storage_map_g[var_b]));
        storage_map_h[var_b] = 0 | (0 & (storage_map_h[var_b]));
        storage_map_c[var_b] = (uint256(store_d)) | (0 & (storage_map_c[var_b]));
        var_b = address(msg.sender);
        require(storage_map_e[var_b] == 0, CustomError_73380d99());
        if (storage_map_c[var_b] == 0) {
        }
        if (storage_map_c[var_b] == (store_d)) {
        }
        store_d = (uint256(block.timestamp)) | (0 & (store_d));
        emit CycleRollover(0, block.timestamp);
        var_b = address(msg.sender);
        var_b = var_b;
        if (!0x01) {
            if (storage_map_c[var_b] == (store_d)) {
                if (storage_map_c[var_b] == (store_d)) {
                }
            }
        }
    }
    
    /// @custom:selector    0x655bce22
    /// @custom:signature   getCycle() public view returns (bytes memory)
    function getCycle() public view returns (bytes memory) {
        require(msg.value);
        if (store_i - 0) {
            if (store_i > (store_i + 0x093a80)) {
                return abi.encodePacked(store_i, (store_i) + 0x093a80, 0x093a80, 0x015180);
                return abi.encodePacked(store_i, 0, 0x093a80, 0x015180);
            }
        }
    }
    
    /// @custom:selector    0x657fa066
    /// @custom:signature   Unresolved_657fa066(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_657fa066(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        require(!(storage_map_j[var_a]) == 0);
        require(block.timestamp > (storage_map_j[var_a]));
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
        require((block.timestamp - (storage_map_j[var_a])) > block.timestamp);
        require(!0x093a80);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((block.timestamp - (storage_map_j[var_a])) / 0x093a80) < 0x02);
        require(((block.timestamp - (storage_map_j[var_a])) / 0x093a80) == 0x01);
        require(storage_map_m[var_a] > (storage_map_m[var_a] + storage_map_k[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return abi.encodePacked(storage_map_l[var_a], 0, (storage_map_m[var_a]) + storage_map_k[var_a]);
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
        require(storage_map_m[var_a] > (storage_map_m[var_a] + storage_map_k[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(storage_map_j[var_a]) == 0);
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
    }
    
    /// @custom:selector    0x369a5662
    /// @custom:signature   Unresolved_369a5662(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_369a5662(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x97ec170c
    /// @custom:signature   Unresolved_97ec170c(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_97ec170c(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0x13758207
    /// @custom:signature   Unresolved_13758207(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_13758207(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xa29d08ce
    /// @custom:signature   Unresolved_a29d08ce(uint256 arg0, uint256 arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_a29d08ce(uint256 arg0, uint256 arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xc7a2170b
    /// @custom:signature   Unresolved_c7a2170b(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_c7a2170b(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(!(storage_map_j[var_a]) == 0);
        require(block.timestamp > (storage_map_j[var_a]));
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
        require((block.timestamp - (storage_map_j[var_a])) > block.timestamp);
        require(!0x093a80);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((block.timestamp - (storage_map_j[var_a])) / 0x093a80) < 0x02);
        require(((block.timestamp - (storage_map_j[var_a])) / 0x093a80) == 0x01);
        require(storage_map_m[var_a] > (storage_map_m[var_a] + storage_map_k[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return abi.encodePacked(storage_map_l[var_a], 0, (storage_map_m[var_a]) + storage_map_k[var_a]);
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
        require(storage_map_m[var_a] > (storage_map_m[var_a] + storage_map_k[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(storage_map_j[var_a]) == 0);
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
    }
    
    /// @custom:selector    0x4146780d
    /// @custom:signature   Unresolved_4146780d(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_4146780d(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(!(storage_map_j[var_a]) == 0);
        require(block.timestamp > (storage_map_j[var_a]));
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
        require((block.timestamp - (storage_map_j[var_a])) > block.timestamp);
        require(!0x093a80);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((block.timestamp - (storage_map_j[var_a])) / 0x093a80) < 0x02);
        require(((block.timestamp - (storage_map_j[var_a])) / 0x093a80) == 0x01);
        require(storage_map_m[var_a] > (storage_map_m[var_a] + storage_map_k[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return abi.encodePacked(storage_map_l[var_a], 0, (storage_map_m[var_a]) + storage_map_k[var_a]);
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
        require(storage_map_m[var_a] > (storage_map_m[var_a] + storage_map_k[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(storage_map_j[var_a]) == 0);
        return abi.encodePacked(storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a]);
    }
    
    /// @custom:selector    0xd4964514
    /// @custom:signature   Unresolved_d4964514(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_d4964514(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xdd5043fd
    /// @custom:signature   Unresolved_dd5043fd() public payable
    function Unresolved_dd5043fd() public payable {
        require(msg.value);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        require(store_b == 0, CustomError_73380d99());
        require(store_b > (store_b + 0x093a80), CustomError_73380d99());
        require(block.timestamp > (store_b + 0x093a80), CustomError_73380d99());
        address var_b = address(msg.sender);
        var_b = var_b;
        require(!0, CustomError_73380d99());
        require(!0, CustomError_73380d99());
        require(storage_map_c[var_b] - 0, CustomError_73380d99());
        require(store_d - (storage_map_c[var_b]) > (store_d), CustomError_73380d99());
        require(!0x093a80, CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((store_d - (storage_map_c[var_b]) / 0x093a80) < 0x02, CustomError_73380d99());
        require(storage_map_e[var_b] > (storage_map_e[var_b] + storage_map_f[var_b]), CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(storage_map_e[var_b] > (storage_map_e[var_b] + storage_map_f[var_b]), CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_e[var_b] = (uint256(storage_map_e[var_b] + storage_map_f[var_b])) | (0 & (storage_map_e[var_b]));
        storage_map_g[var_b] = (uint256(storage_map_h[var_b])) | (0 & (storage_map_g[var_b]));
        storage_map_h[var_b] = 0 | (0 & (storage_map_h[var_b]));
        storage_map_c[var_b] = (uint256(store_d)) | (0 & (storage_map_c[var_b]));
        var_b = address(msg.sender);
        require(storage_map_e[var_b] == 0, CustomError_73380d99());
        if (storage_map_c[var_b] == 0) {
        }
        if (storage_map_c[var_b] == (store_d)) {
        }
        store_d = (uint256(block.timestamp)) | (0 & (store_d));
        emit CycleRollover(0, block.timestamp);
        var_b = address(msg.sender);
        var_b = var_b;
        if (!0x01) {
            if (storage_map_c[var_b] == (store_d)) {
                if (storage_map_c[var_b] == (store_d)) {
                }
            }
        }
    }
    
    /// @custom:selector    0xdf735cca
    /// @custom:signature   Unresolved_df735cca(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_df735cca(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x1177fd3f
    /// @custom:signature   Unresolved_1177fd3f(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_1177fd3f(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        require(arg0 == 0, CustomError_73380d99());
        require(store_b == 0, CustomError_73380d99());
        require(store_b > (store_b + 0x093a80), CustomError_73380d99());
        require(block.timestamp > (store_b + 0x093a80), CustomError_73380d99());
        uint256 var_b = arg0;
        var_b = var_b;
        require(!0, CustomError_73380d99());
        require(!0, CustomError_73380d99());
        require(storage_map_c[var_b] - 0, CustomError_73380d99());
        require(store_d - (storage_map_c[var_b]) > (store_d), CustomError_73380d99());
        require(!0x093a80, CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((store_d - (storage_map_c[var_b]) / 0x093a80) < 0x02, CustomError_73380d99());
        require(storage_map_e[var_b] > (storage_map_e[var_b] + storage_map_f[var_b]), CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(storage_map_e[var_b] > (storage_map_e[var_b] + storage_map_f[var_b]), CustomError_73380d99());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_e[var_b] = (uint256(storage_map_e[var_b] + storage_map_f[var_b])) | (0 & (storage_map_e[var_b]));
        storage_map_g[var_b] = (uint256(storage_map_h[var_b])) | (0 & (storage_map_g[var_b]));
        storage_map_h[var_b] = 0 | (0 & (storage_map_h[var_b]));
        storage_map_c[var_b] = (uint256(store_d)) | (0 & (storage_map_c[var_b]));
        var_b = arg0;
        require(storage_map_e[var_b] == 0, CustomError_73380d99());
        if (storage_map_c[var_b] == 0) {
        }
        if (storage_map_c[var_b] == (store_d)) {
        }
        store_d = (uint256(block.timestamp)) | (0 & (store_d));
        emit CycleRollover(0, block.timestamp);
        var_b = arg0;
        var_b = var_b;
        if (!0x01) {
            if (storage_map_c[var_b] == (store_d)) {
                require(!0x01, CustomError_b4fa3fb3());
            }
        }
    }
}