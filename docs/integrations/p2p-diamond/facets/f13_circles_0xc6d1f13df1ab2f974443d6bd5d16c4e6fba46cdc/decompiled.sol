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
    mapping(bytes32 => bytes32) storage_map_h;
    mapping(bytes32 => bytes32) storage_map_a;
    mapping(bytes32 => bytes32) storage_map_i;
    mapping(bytes32 => bytes32) storage_map_p;
    mapping(bytes32 => bytes32) storage_map_r;
    mapping(bytes32 => bytes32) storage_map_c;
    mapping(bytes32 => bytes32) storage_map_f;
    uint256 public getCircleCount;
    mapping(bytes32 => bytes32) storage_map_g;
    mapping(bytes32 => bytes32) storage_map_e;
    mapping(bytes32 => bytes32) storage_map_m;
    bytes32 store_n;
    bytes32 store_b;
    address public unresolved_b3e3ae20;
    mapping(bytes32 => bytes32) storage_map_q;
    mapping(bytes32 => bytes32) storage_map_o;
    mapping(bytes32 => bytes32) storage_map_d;
    mapping(bytes32 => bytes32) storage_map_k;
    
    event Event_6b346ee7();
    event Event_cd56bcaa();
    event Event_c809ad64();
    error AddressInsufficientBalance(address);
    event Event_bafcd2e6();
    
    /// @custom:selector    0x93d42fa1
    /// @custom:signature   Unresolved_93d42fa1(uint256 arg0) public payable returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_93d42fa1(uint256 arg0) public payable returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        uint256 var_d = storage_map_a[var_a];
        (bool success, bytes memory ret0) = address(store_b).Unresolved_67c84efd(var_d); // staticcall
        var_a = arg0;
        var_b = 0xb56ae6c586ae74faf72772197c5301befd3dbebfa5c8aaab96d3b6679813bf63;
        require(!((storage_map_c[var_a] * 0x0f4240) / (storage_map_c[var_a]) == 0x0f4240) | (!storage_map_c[var_a]));
        require(!var_b);
        var_a = arg0;
        var_b = 0xb56ae6c586ae74faf72772197c5301befd3dbebfa5c8aaab96d3b6679813bf63;
        require(storage_map_d[var_a] - ((storage_map_c[var_a] * 0x0f4240) / var_b) > (storage_map_d[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return (storage_map_d[var_a]) - ((storage_map_c[var_a] * 0x0f4240) / var_b);
        require(0x80 > ret0.length);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        uint256 var_f = var_f + 0x80;
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        var_f = var_f + 0x80;
        require(var_f.length - var_f.length);
    }
    
    /// @custom:selector    0x23c5ac83
    /// @custom:signature   Unresolved_23c5ac83(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    function Unresolved_23c5ac83(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg3 > 0xffffffffffffffff);
        require(arg3 > 0xffffffffffffffff);
        require(arg4 - arg4);
    }
    
    /// @custom:selector    0xba096bd4
    /// @custom:signature   Unresolved_ba096bd4(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_ba096bd4(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(arg0 == 0, CustomError_dab11ea6());
        address var_a = address(msg.sender);
        require(storage_map_e[var_a] == 0, CustomError_dab11ea6());
        var_a = address(msg.sender);
        require(storage_map_f[var_a] > 0, CustomError_dab11ea6());
        var_a = address(msg.sender);
        require(arg0 > storage_map_e[var_a], CustomError_78317f44());
        var_a = storage_map_g[var_a];
        require((storage_map_e[var_a] - arg0) > storage_map_e[var_a], CustomError_78317f44());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((storage_map_e[var_a] - arg0) < (storage_map_f[var_a]), CustomError_78317f44());
        storage_map_f[var_a] = (uint256(arg0)) | (0 & (storage_map_f[var_a]));
        storage_map_h[var_a] = (uint256(block.timestamp)) | (0 & (storage_map_h[var_a]));
        var_a = storage_map_a[var_a];
        require(block.timestamp > (block.timestamp + (storage_map_f[var_a])), CustomError_a8143fbc());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        emit Event_6b346ee7(address(msg.sender), storage_map_e[var_a], arg0, block.timestamp + (storage_map_f[var_a]));
    }
    
    /// @custom:selector    0x6366a783
    /// @custom:signature   Unresolved_6366a783(uint256 arg0, address arg1) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_6366a783(uint256 arg0, address arg1) public payable {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_bb1cb70b());
        require(arg0 > 0x01f4, CustomError_bb1cb70b());
        require(!0 < (arg0));
        require(!0 < (arg0));
        require(address(0 + (0x20 + (arg0))) - (0 + (0x20 + (arg0))));
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_i[var_a] > (storage_map_i[var_a]), CustomError_16c726b1());
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_i[var_a] - (storage_map_i[var_a]) > (storage_map_i[var_a]), CustomError_16c726b1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_i[var_a] = (uint256(storage_map_i[var_a] - (storage_map_i[var_a]))) | (0 & (storage_map_i[var_a]));
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_16c726b1());
    }
    
    /// @custom:selector    0x649cb8d8
    /// @custom:signature   Unresolved_649cb8d8(uint256 arg0, uint256 arg1) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_649cb8d8(uint256 arg0, uint256 arg1) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_bb1cb70b());
        require(arg1 > 0x01f4, CustomError_bb1cb70b());
        var_a = arg0;
        storage_map_c[var_a] = 0 | (0 & (storage_map_c[var_a]));
        require(!0 < (arg1));
        require(!0 < (arg1));
        require(address(0 + (0x20 + (arg1))) - (0 + (0x20 + (arg1))));
        var_a = address(0 + (0x20 + (arg1)));
        if (0x64 < (storage_map_a[var_a])) {
            if (!0 < 0x64) {
                require(0x64 < (storage_map_a[var_a]), CustomError_16c726b1());
                require(!(0 < 0x64), CustomError_16c726b1());
                require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_16c726b1());
                require(!(0x06 > 0x05), CustomError_16c726b1());
            }
        }
        var_a = arg0;
        require(storage_map_a[var_a] - 0, CustomError_16c726b1());
        var_a = storage_map_a[var_a];
        storage_map_f[var_a] = 0 | (address(storage_map_f[var_a]));
        storage_map_f[var_a] = 0 | (address(storage_map_f[var_a]));
        require(0x01, CustomError_16c726b1());
        var_a = arg0;
        emit Event_cd56bcaa(arg0, storage_map_c[var_a]);
        require(0x01 > getCircleCount, CustomError_16c726b1());
        var_a = 0x01;
        require(storage_map_a[var_a] - (storage_map_a[var_a]), CustomError_16c726b1());
        var_a = 0x01;
        require(address(storage_map_h[var_a]) == 0, CustomError_16c726b1());
        var_a = address(storage_map_h[var_a]);
        require(0x01, CustomError_16c726b1());
        require(!0x01, CustomError_16c726b1());
        require(address(storage_map_e[var_a]) == (address(storage_map_e[var_a])), CustomError_16c726b1());
        var_a = address(storage_map_e[var_a]);
        require(storage_map_k[var_a] == 0, CustomError_16c726b1());
        var_a = address(storage_map_e[var_a]);
        require(!(0 < (storage_map_a[var_a])), CustomError_16c726b1());
        require(!(0 < (storage_map_a[var_a])), CustomError_16c726b1());
        require(!(0x06 > 0x02), CustomError_16c726b1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(0x06 > (bytes1(storage_map_l[0 + keccak256(var_a)]))), CustomError_16c726b1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_l[0 + keccak256(var_a)]) - 0x02, CustomError_16c726b1());
        var_a = address(storage_map_e[var_a]);
        var_a = storage_map_m[0 + keccak256(var_a)];
        require(0 > (0 + storage_map_e[var_a]), CustomError_16c726b1());
        var_a = arg0;
        emit Event_cd56bcaa(arg0, storage_map_c[var_a]);
        var_a = arg0;
        emit Event_cd56bcaa(arg0, storage_map_c[var_a]);
    }
    
    /// @custom:selector    0x76148a7e
    /// @custom:signature   Unresolved_76148a7e(address arg0) public view returns (bytes memory)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_76148a7e(address arg0) public view returns (bytes memory) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return abi.encodePacked(storage_map_e[var_a], storage_map_h[var_a]);
    }
    
    /// @custom:selector    0xc3e55bab
    /// @custom:signature   Unresolved_c3e55bab(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_c3e55bab(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_d[var_a];
    }
    
    /// @custom:selector    0x10b99fc9
    /// @custom:signature   Unresolved_10b99fc9(uint256 arg0, address arg1) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_10b99fc9(uint256 arg0, address arg1) public payable {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_bb1cb70b());
        require(arg0 > 0x01f4, CustomError_bb1cb70b());
        require(!0 < (arg0));
        require(!0 < (arg0));
        require(address(0 + (0x20 + (arg0))) - (0 + (0x20 + (arg0))));
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_a[var_a] > (storage_map_i[var_a]), CustomError_16c726b1());
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_a[var_a] - (storage_map_i[var_a]) > (storage_map_a[var_a]), CustomError_16c726b1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_a[var_a] = (uint256(storage_map_a[var_a] - (storage_map_i[var_a]))) | (0 & (storage_map_a[var_a]));
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_16c726b1());
    }
    
    /// @custom:selector    0x77da3815
    /// @custom:signature   Unresolved_77da3815(address arg0) public view returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_77da3815(address arg0) public view returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return storage_map_e[var_a];
    }
    
    /// @custom:selector    0x6252ec94
    /// @custom:signature   Unresolved_6252ec94(address arg0) public view returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_6252ec94(address arg0) public view returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return storage_map_e[var_a];
    }
    
    /// @custom:selector    0x99c02a82
    /// @custom:signature   Unresolved_99c02a82(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_99c02a82(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xa1af3c87
    /// @custom:signature   Unresolved_a1af3c87(uint256 arg0, uint256 arg1, uint256 arg2) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_a1af3c87(uint256 arg0, uint256 arg1, uint256 arg2) public view {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_a24a13a6());
        require(arg1 - (arg2), CustomError_a24a13a6());
        var_a = arg0;
        require(storage_map_f[var_a] == 0, CustomError_b4fa3fb3());
        require(arg1 > 0xffffffffffffffff, CustomError_b4fa3fb3());
        require(((var_e + (uint248(0x1f + ((arg1 * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_e + (uint248(0x1f + ((arg1 * 0x20) + 0x20)))) < var_e), CustomError_b4fa3fb3());
    }
    
    /// @custom:selector    0x44b106ac
    /// @custom:signature   Unresolved_44b106ac(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_44b106ac(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xd79d6cae
    /// @custom:signature   Unresolved_d79d6cae(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_d79d6cae(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_eb1ce40b());
        var_a = address(arg0);
        require(storage_map_e[var_a] == 0, CustomError_eb1ce40b());
        var_a = address(arg0);
        require(storage_map_f[var_a] == 0, CustomError_eb1ce40b());
        storage_map_f[var_a] = 0 | (0 & (storage_map_f[var_a]));
        storage_map_h[var_a] = 0 | (0 & (storage_map_h[var_a]));
        emit Event_c809ad64(address(arg0), storage_map_e[var_a], storage_map_f[var_a]);
    }
    
    /// @custom:selector    0x2cfdd17b
    /// @custom:signature   Unresolved_2cfdd17b() public payable
    function Unresolved_2cfdd17b() public payable {
        require(msg.value);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_n), CustomError_8beb9d16());
        store_n = (bytes1(0x01)) | (uint248(store_n));
        address var_b = address(msg.sender);
        require(storage_map_o[var_b] == 0, CustomError_bf2d0ba1());
        var_b = address(msg.sender);
        require(storage_map_p[var_b] == 0, CustomError_bf2d0ba1());
        var_b = storage_map_q[var_b];
        require(storage_map_r[var_b] > (storage_map_r[var_b] + (storage_map_p[var_b])), CustomError_bf2d0ba1());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(block.timestamp < (storage_map_r[var_b] + (storage_map_p[var_b])), CustomError_bf2d0ba1());
        storage_map_p[var_b] = 0 | (0 & (storage_map_p[var_b]));
        storage_map_r[var_b] = 0 | (0 & (storage_map_r[var_b]));
        var_b = address(msg.sender);
        require(storage_map_o[var_b] - (storage_map_p[var_b]) > storage_map_o[var_b], CustomError_cd786059());
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_o[var_b] = (uint256(storage_map_o[var_b] - (storage_map_p[var_b]))) | (0 & (storage_map_o[var_b]));
        address var_g = address(msg.sender);
        require(((var_i + (uint248(0x1f + ((((var_i + 0x20) + 0x04) + 0x40) - var_i)))) > 0xffffffffffffffff) | ((var_i + (uint248(0x1f + ((((var_i + 0x20) + 0x04) + 0x40) - var_i)))) < var_i), CustomError_cd786059());
        uint248 var_i = var_i + (uint248(0x1f + ((((var_i + 0x20) + 0x04) + 0x40) - var_i)));
        require(address(this).balance < 0, CustomError_cd786059());
        (bool success, bytes memory ret0) = address(unresolved_b3e3ae20).{ value: 0 ether }Unresolved_a9059cbb(var_g); // call
        require(!ret0.length);
        require(!var_l > 0);
        require(var_l == 0);
        require(var_l == 0);
        require(!var_l == 0);
        require(((0x80 + var_l) - 0x80) < 0x20);
        require(var_m - var_m);
        require(!var_m, CustomError_5274afe7());
        var_b = address(msg.sender);
        emit Event_bafcd2e6(address(msg.sender), storage_map_o[var_b], storage_map_p[var_b], storage_map_o[var_b]);
        require(address(msg.sender) - (address(this)), CustomError_5274afe7());
        store_n = (bytes1(0)) | (uint248(store_n));
        require(!(var_l == 0), CustomError_5274afe7());
        require(address(unresolved_b3e3ae20).code.length == 0, CustomError_9996b315());
        require(ret0.length > 0xffffffffffffffff, CustomError_1425ea42());
        require(((var_i + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) > 0xffffffffffffffff) | ((var_i + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)))) < var_i), CustomError_1425ea42());
        var_i = var_i + (uint248(0x1f + ((0x1f + ret0.length) + 0x20)));
        require(!(var_i.length > 0), CustomError_1425ea42());
        require(var_i.length == 0, CustomError_9996b315());
        require(address(unresolved_b3e3ae20).code.length == 0, CustomError_9996b315());
        if (!var_i.length == 0) {
        }
    }
    
    /// @custom:selector    0x9c313d72
    /// @custom:signature   Unresolved_9c313d72(uint256 arg0, address arg1, uint256 arg2, uint256 arg3, uint256 arg4, uint256 arg5) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    /// @param              arg5 ["uint256", "bytes32", "int256"]
    function Unresolved_9c313d72(uint256 arg0, address arg1, uint256 arg2, uint256 arg3, uint256 arg4, uint256 arg5) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg3 > 0xffffffffffffffff);
        require(arg3 > 0xffffffffffffffff);
        require(arg4 > 0xffffffffffffffff);
        require(arg4 > 0xffffffffffffffff);
        require(arg5 - arg5);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_6540a51d());
        var_a = arg0;
        require(!(bytes1(storage_map_e[var_a])), CustomError_6540a51d());
        require(arg2 == 0, CustomError_6540a51d());
        require(arg3 == 0, CustomError_6540a51d());
        require(arg4 == 0, CustomError_6540a51d());
        require(((var_f + (uint248(0x1f + ((var_f + 0x20) + (arg2) - var_f)))) > 0xffffffffffffffff) | ((var_f + (uint248(0x1f + ((var_f + 0x20) + (arg2) - var_f)))) < var_f), CustomError_6540a51d());
        uint256 var_f = var_f + (uint248(0x1f + ((var_f + 0x20) + (arg2) - var_f)));
        var_a = keccak256(var_h);
        require(storage_map_e[var_a] - 0, CustomError_6540a51d());
        var_a = address(arg1);
        require(storage_map_e[var_a] - 0, CustomError_201c1ffc());
        require(getCircleCount == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff);
        getCircleCount = (uint256(0x01 + getCircleCount)) | (0 & (getCircleCount));
        require(((var_f + 0x01c0) > 0xffffffffffffffff) | ((var_f + 0x01c0) < var_f));
        var_f = var_f + 0x01c0;
        require(arg2 > 0xffffffffffffffff);
        require(((var_f + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) > 0xffffffffffffffff) | ((var_f + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) < var_f));
    }
    
    /// @custom:selector    0x4e419537
    /// @custom:signature   Unresolved_4e419537(uint256 arg0, address arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_4e419537(uint256 arg0, address arg1) public view {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_bb1cb70b());
        require(arg0 > 0x01f4, CustomError_bb1cb70b());
        require(!0 < (arg0));
        require(!0 < (arg0));
        require(address(0 + (0x20 + (arg0))) - (0 + (0x20 + (arg0))));
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_a[var_a] == (storage_map_i[var_a]), CustomError_16c726b1());
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_16c726b1());
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_a[var_a] - (storage_map_i[var_a]) > (storage_map_a[var_a]), CustomError_16c726b1());
    }
    
    /// @custom:selector    0x64ccecde
    /// @custom:signature   getCircle(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function getCircle(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x01c0) > 0xffffffffffffffff) | ((var_a + 0x01c0) < var_a));
    }
    
    /// @custom:selector    0x258b1c1e
    /// @custom:signature   Unresolved_258b1c1e(uint256 arg0) public view returns (address)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_258b1c1e(uint256 arg0) public view returns (address) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return address(storage_map_e[var_a]);
    }
    
    /// @custom:selector    0x4a408331
    /// @custom:signature   Unresolved_4a408331(uint256 arg0, address arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_4a408331(uint256 arg0, address arg1) public view {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_e[var_a])), CustomError_bb1cb70b());
        require(arg0 > 0x01f4, CustomError_bb1cb70b());
        require(!0 < (arg0));
        require(!0 < (arg0));
        require(address(0 + (0x20 + (arg0))) - (0 + (0x20 + (arg0))));
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_i[var_a] == (storage_map_i[var_a]), CustomError_16c726b1());
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_16c726b1());
        var_a = address(0 + (0x20 + (arg0)));
        var_a = address(0 + (0x20 + (arg0)));
        require(storage_map_i[var_a] - (storage_map_i[var_a]) > (storage_map_i[var_a]), CustomError_16c726b1());
    }
    
    /// @custom:selector    0x9c6628ff
    /// @custom:signature   Unresolved_9c6628ff(uint256 arg0) public view returns (bool)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_9c6628ff(uint256 arg0) public view returns (bool) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        require(!(address(storage_map_h[var_a])) == 0);
        var_a = arg0;
        require(!(storage_map_a[var_a]) == 0);
        var_a = arg0;
        require(storage_map_f[var_a] == 0);
        var_a = storage_map_a[var_a];
        require(storage_map_f[var_a] > 0);
        var_a = address(storage_map_h[var_a]);
        require(storage_map_e[var_a] < (storage_map_f[var_a]));
        return 0;
        return 0x01;
        return 0;
    }
    
    /// @custom:selector    0xe0ed9d38
    /// @custom:signature   Unresolved_e0ed9d38(uint256 arg0) public view returns (uint256)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_e0ed9d38(uint256 arg0) public view returns (uint256) {
        require(msg.value);
        require(arg0 - arg0);
        uint256 var_a = arg0;
        return storage_map_c[var_a];
    }
}