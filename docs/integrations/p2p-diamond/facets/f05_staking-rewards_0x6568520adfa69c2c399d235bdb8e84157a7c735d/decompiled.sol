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
    mapping(bytes32 => bytes32) storage_map_i;
    mapping(bytes32 => bytes32) storage_map_p;
    mapping(bytes32 => bytes32) storage_map_c;
    mapping(bytes32 => bytes32) storage_map_f;
    mapping(bytes32 => bytes32) storage_map_j;
    mapping(bytes32 => bytes32) storage_map_m;
    mapping(bytes32 => bytes32) storage_map_g;
    mapping(bytes32 => bytes32) storage_map_e;
    bytes32 store_a;
    bytes32 store_h;
    mapping(bytes32 => bytes32) storage_map_b;
    mapping(bytes32 => bytes32) storage_map_n;
    mapping(bytes32 => bytes32) storage_map_o;
    mapping(bytes32 => bytes32) storage_map_d;
    mapping(bytes32 => bytes32) storage_map_k;
    
    event Event_f72dd784();
    error AddressInsufficientBalance(address);
    event RewardPaid(address, uint256, uint256);
    
    /// @custom:selector    0x357e20d1
    /// @custom:signature   Unresolved_357e20d1(uint256 arg0, address arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_357e20d1(uint256 arg0, address arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
    }
    
    /// @custom:selector    0xae169a50
    /// @custom:signature   claimReward(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function claimReward(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        require(address(msg.sender) - 0, CustomError_cd786059());
        var_b = arg0;
        var_b = arg0;
        var_b = arg0;
        require(storage_map_b[var_b] - (storage_map_c[var_b]) > (storage_map_b[var_b]), CustomError_cd786059());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(storage_map_d[var_b] * (storage_map_b[var_b] - (storage_map_c[var_b])) / storage_map_d[var_b] == (storage_map_b[var_b] - (storage_map_c[var_b])) | !storage_map_d[var_b]), CustomError_cd786059());
        require(!0x0de0b6b3a7640000, CustomError_cd786059());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((storage_map_d[var_b] * (storage_map_b[var_b] - (storage_map_c[var_b])) / 0x0de0b6b3a7640000) > ((storage_map_d[var_b] * (storage_map_b[var_b] - (storage_map_c[var_b])) / 0x0de0b6b3a7640000) + (storage_map_e[var_b])), CustomError_cd786059());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_e[var_b] = (uint256((storage_map_d[var_b] * (storage_map_b[var_b] - (storage_map_c[var_b])) / 0x0de0b6b3a7640000) + (storage_map_e[var_b]))) | (0 & (storage_map_e[var_b]));
        storage_map_f[var_b] = (uint256(storage_map_g[var_b])) | (0 & (storage_map_f[var_b]));
        var_b = arg0;
        require(storage_map_e[var_b] == 0, CustomError_cd786059());
        storage_map_e[var_b] = 0 | (0 & (storage_map_e[var_b]));
        require(storage_map_b[var_b] > (storage_map_b[var_b] + (storage_map_e[var_b])), CustomError_cd786059());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_b[var_b] = (uint256(storage_map_b[var_b] + (storage_map_e[var_b]))) | (0 & (storage_map_b[var_b]));
        var_b = var_b;
        address var_f = address(msg.sender);
        uint256 var_a = (((var_h + 0x20) + 0x04) + 0x40) - (var_h + 0x20);
        require(((var_h + (uint248(0x1f + ((((var_h + 0x20) + 0x04) + 0x40) - var_h)))) > 0xffffffffffffffff) | ((var_h + (uint248(0x1f + ((((var_h + 0x20) + 0x04) + 0x40) - var_h)))) < var_h), CustomError_cd786059());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        uint248 var_h = var_h + (uint248(0x1f + ((((var_h + 0x20) + 0x04) + 0x40) - var_h)));
        require(address(this).balance < 0, CustomError_cd786059());
        (bool success, bytes memory ret0) = address(store_h).{ value: 0 ether }Unresolved_a9059cbb(var_f); // call
        require(!ret0.length);
        require(!var_k > 0);
        require(var_k == 0, CustomError_5274afe7());
        require(var_k == 0, CustomError_5274afe7());
        require(!(var_k == 0), CustomError_5274afe7());
        require(((0x80 + var_k) - 0x80) < 0x20, CustomError_5274afe7());
        require(var_a - var_a, CustomError_5274afe7());
        require(!var_a, CustomError_5274afe7());
        emit RewardPaid(address(msg.sender), arg0, storage_map_e[var_b]);
        require(address(msg.sender) - (address(this)));
        store_a = (bytes1(0)) | (uint248(store_a));
        require(address(store_h).code.length == 0, CustomError_9996b315());
        require(ret0.length > 0xffffffffffffffff, CustomError_1425ea42());
        require(((var_h + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) > 0xffffffffffffffff) | ((var_h + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) < var_h), CustomError_1425ea42());
        var_h = var_h + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)));
        require(!(var_h.length > 0), CustomError_1425ea42());
        require(var_h.length == 0, CustomError_9996b315());
        require(address(store_h).code.length == 0, CustomError_9996b315());
        require(!(var_h.length == 0), CustomError_5274afe7());
        require(!(var_h.length == 0), CustomError_5274afe7());
    }
    
    /// @custom:selector    0x246132f9
    /// @custom:signature   Unresolved_246132f9(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_246132f9(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x721c6513
    /// @custom:signature   requestExit(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function requestExit(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - 0, CustomError_a9de99ae());
        var_a = arg0;
        var_a = arg0;
        var_a = arg0;
        require(storage_map_i[var_a] - (storage_map_j[var_a]) > (storage_map_i[var_a]), CustomError_a9de99ae());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(storage_map_k[var_a] * (storage_map_i[var_a] - (storage_map_j[var_a])) / storage_map_k[var_a] == (storage_map_i[var_a] - (storage_map_j[var_a])) | !storage_map_k[var_a]), CustomError_a9de99ae());
        require(!0x0de0b6b3a7640000, CustomError_a9de99ae());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((storage_map_k[var_a] * (storage_map_i[var_a] - (storage_map_j[var_a])) / 0x0de0b6b3a7640000) > ((storage_map_k[var_a] * (storage_map_i[var_a] - (storage_map_j[var_a])) / 0x0de0b6b3a7640000) + (storage_map_l[var_a])), CustomError_a9de99ae());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_l[var_a] = (uint256((storage_map_k[var_a] * (storage_map_i[var_a] - (storage_map_j[var_a])) / 0x0de0b6b3a7640000) + (storage_map_l[var_a]))) | (0 & (storage_map_l[var_a]));
        storage_map_m[var_a] = (uint256(storage_map_n[var_a])) | (0 & (storage_map_m[var_a]));
        var_a = arg0;
        require(storage_map_k[var_a] == 0, CustomError_a9de99ae());
        var_a = arg0;
        require(storage_map_o[var_a] - 0, CustomError_a9de99ae());
    }
    
    /// @custom:selector    0x80203152
    /// @custom:signature   Unresolved_80203152(uint256 arg0, address arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_80203152(uint256 arg0, address arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
    }
    
    /// @custom:selector    0x586c226d
    /// @custom:signature   Unresolved_586c226d(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_586c226d(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x811d581e
    /// @custom:signature   Unresolved_811d581e(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_811d581e(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x326be8eb
    /// @custom:signature   Unresolved_326be8eb(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_326be8eb(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_l[var_a];
    }
    
    /// @custom:selector    0x459efcd8
    /// @custom:signature   Unresolved_459efcd8(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_459efcd8(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x4dbc04bc
    /// @custom:signature   Unresolved_4dbc04bc(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_4dbc04bc(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x7b0472f0
    /// @custom:signature   Unresolved_7b0472f0(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_7b0472f0(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x3fe2b470
    /// @custom:signature   Unresolved_3fe2b470(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_3fe2b470(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_a), CustomError_8beb9d16());
        store_a = (bytes1(0x01)) | (uint248(store_a));
        var_b = arg0;
        var_b = arg0;
        require(storage_map_d[var_b] == 0, CustomError_9ab7872d());
        var_b = arg0;
        var_b = storage_map_e[var_b];
        require(storage_map_b[var_b] > (storage_map_b[var_b] + (storage_map_c[var_b])), CustomError_9ab7872d());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(block.timestamp < (storage_map_b[var_b] + (storage_map_c[var_b])), CustomError_9ab7872d());
        require(storage_map_c[var_b] < (storage_map_e[var_b]), CustomError_549e2555());
        storage_map_p[var_b] = 0 | (0 & (storage_map_p[var_b]));
        storage_map_b[var_b] = 0 | (0 & (storage_map_b[var_b]));
        var_b = var_b;
        var_f = address(msg.sender);
        uint256 var_a = (((var_i + 0x20) + 0x04) + 0x40) - (var_i + 0x20);
        require(((var_i + (uint248(0x1f + ((((var_i + 0x20) + 0x04) + 0x40) - var_i)))) > 0xffffffffffffffff) | ((var_i + (uint248(0x1f + ((((var_i + 0x20) + 0x04) + 0x40) - var_i)))) < var_i), CustomError_cd786059());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        uint248 var_i = var_i + (uint248(0x1f + ((((var_i + 0x20) + 0x04) + 0x40) - var_i)));
        require(address(this).balance < 0, CustomError_cd786059());
        (bool success, bytes memory ret0) = address(store_h).{ value: 0 ether }Unresolved_a9059cbb(var_f); // call
        require(!ret0.length);
        require(!var_l > 0);
        require(var_l == 0, CustomError_5274afe7());
        require(var_l == 0, CustomError_5274afe7());
        require(!(var_l == 0), CustomError_5274afe7());
        require(((0x80 + var_l) - 0x80) < 0x20, CustomError_5274afe7());
        require(var_a - var_a, CustomError_5274afe7());
        require(!var_a, CustomError_5274afe7());
        emit Event_f72dd784(address(msg.sender), arg0, storage_map_d[var_b]);
        require(address(msg.sender) - (address(this)));
        store_a = (bytes1(0)) | (uint248(store_a));
        require(address(store_h).code.length == 0, CustomError_9996b315());
        require(ret0.length > 0xffffffffffffffff, CustomError_1425ea42());
        require(((var_i + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) > 0xffffffffffffffff) | ((var_i + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) < var_i), CustomError_1425ea42());
        var_i = var_i + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)));
        require(!(var_i.length > 0), CustomError_1425ea42());
        require(var_i.length == 0, CustomError_9996b315());
        require(address(store_h).code.length == 0, CustomError_9996b315());
        require(!(var_i.length == 0), CustomError_5274afe7());
        require(!(var_i.length == 0), CustomError_5274afe7());
    }
    
    /// @custom:selector    0x56ffbecc
    /// @custom:signature   Unresolved_56ffbecc(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_56ffbecc(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xa0c7f71c
    /// @custom:signature   Unresolved_a0c7f71c(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_a0c7f71c(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
}