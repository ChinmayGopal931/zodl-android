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
    mapping(bytes32 => bytes32) storage_map_r;
    mapping(bytes32 => bytes32) storage_map_s;
    mapping(bytes32 => bytes32) storage_map_c;
    mapping(bytes32 => bytes32) storage_map_f;
    bytes32 store_o;
    bytes32 store_p;
    bytes32 store_j;
    mapping(bytes32 => bytes32) storage_map_g;
    mapping(bytes32 => bytes32) storage_map_e;
    mapping(bytes32 => bytes32) storage_map_n;
    mapping(bytes32 => bytes32) storage_map_m;
    mapping(bytes32 => bytes32) storage_map_b;
    mapping(bytes32 => bytes32) storage_map_q;
    mapping(bytes32 => bytes32) storage_map_d;
    mapping(bytes32 => bytes32) storage_map_k;
    
    event UnstakeRequestCancelled(address);
    error CustomError_00000000();
    event Event_f21de172();
    
    /// @custom:selector    0xd49c9c71
    /// @custom:signature   Unresolved_d49c9c71(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_d49c9c71(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x0ea94341
    /// @custom:signature   finalizeUnstake() public payable
    function finalizeUnstake() public payable {
        require(msg.value);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_9ae55bc7());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_9ae55bc7());
        var_a = address(msg.sender);
        require(!(bytes1(storage_map_b[var_a])), CustomError_9ab7872d());
        var_a = storage_map_c[var_a];
        require(storage_map_b[var_a] > (storage_map_b[var_a] + (storage_map_d[var_a])), CustomError_9ab7872d());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(block.timestamp < (storage_map_b[var_a] + (storage_map_d[var_a])), CustomError_9ab7872d());
        var_a = address(msg.sender);
        require(storage_map_e[var_a] - storage_map_b[var_a] > (storage_map_e[var_a]), CustomError_865b21e1());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_865b21e1());
        var_a = address(msg.sender);
        storage_map_b[var_a] = (bytes1(0)) | (uint248(storage_map_b[var_a]));
        var_a = address(msg.sender);
        storage_map_b[var_a] = 0 | (0 & (storage_map_b[var_a]));
        var_a = address(msg.sender);
        storage_map_b[var_a] = 0 | (0 & (storage_map_b[var_a]));
        var_a = address(msg.sender);
        require(storage_map_e[var_a] - storage_map_b[var_a] > (storage_map_e[var_a]), CustomError_865b21e1());
        var_a = address(msg.sender);
        require(!(0 < (storage_map_d[var_a])), CustomError_865b21e1());
        require(!(0 < (storage_map_d[var_a])), CustomError_865b21e1());
        var_a = storage_map_g[0 + keccak256(var_a)];
        require(0 > (0 + storage_map_b[var_a]), CustomError_865b21e1());
        var_a = storage_map_f[var_a];
        require(!((0 == 0x0f4240) | !0), CustomError_865b21e1());
        var_a = storage_map_d[var_a];
        require(!(storage_map_h[var_a]), CustomError_865b21e1());
        require(!((0 / (storage_map_h[var_a])) > (storage_map_e[var_a])), CustomError_865b21e1());
        require(storage_map_b[var_a] > 0, CustomError_865b21e1());
        if (storage_map_i[var_a] - storage_map_b[var_a] > (storage_map_i[var_a])) {
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(storage_map_i[var_a] - storage_map_b[var_a] > (storage_map_i[var_a]), CustomError_0b7c70f3());
        }
    }
    
    /// @custom:selector    0xd85da810
    /// @custom:signature   Unresolved_d85da810(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_d85da810(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xc81575f7
    /// @custom:signature   isMerchantEligible(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function isMerchantEligible(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(!bytes1(storage_map_h[var_a]));
        require(!bytes1(storage_map_a[var_a]));
        var_a = address(arg0);
        require(bytes1(storage_map_b[var_a]));
        require(!0);
        var_a = address(arg0);
        require(!0 < (storage_map_d[var_a]));
        require(!0 < (storage_map_d[var_a]));
        require(!0x06 > 0x02);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return 0;
        return 0;
        require(storage_map_b[var_a] == 0);
        require(!0x01);
        return 0;
    }
    
    /// @custom:selector    0xfc63958e
    /// @custom:signature   requestUnstake() public payable
    function requestUnstake() public payable {
        require(msg.value);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_8beb9d16());
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_j), CustomError_8beb9d16());
        store_j = (bytes1(0x01)) | (uint248(store_j));
        var_a = address(msg.sender);
        require(storage_map_b[var_a] > 0, CustomError_9c54e5a8());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_a9de99ae());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_2d3087f9());
        var_a = address(msg.sender);
        require(storage_map_b[var_a] == 0, CustomError_2d3087f9());
        var_a = address(msg.sender);
        storage_map_b[var_a] = (bytes1(0x01)) | (uint248(storage_map_b[var_a]));
        var_a = address(msg.sender);
        storage_map_b[var_a] = (uint256(block.timestamp)) | (0 & (storage_map_b[var_a]));
        var_a = address(msg.sender);
        storage_map_b[var_a] = (uint256(storage_map_b[var_a])) | (0 & (storage_map_b[var_a]));
        var_a = address(msg.sender);
        storage_map_b[var_a] = (uint256(storage_map_b[var_a])) | (0 & (storage_map_b[var_a]));
        require(!(bytes1(storage_map_b[var_a])), CustomError_2d3087f9());
        require(((storage_map_b[var_a] / 0x02) < 0x20) == (bytes1(storage_map_b[var_a])), CustomError_2d3087f9());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(bytes1(storage_map_b[var_a])), CustomError_2d3087f9());
        require(0x01 == (bytes1(storage_map_b[var_a])), CustomError_2d3087f9());
        var_a = keccak256(var_a);
        require(0 < (storage_map_b[var_a] / 0x02), CustomError_2d3087f9());
        require(!(0x02 > (bytes1(storage_map_h[var_a]))), CustomError_2d3087f9());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = keccak256(var_a) + 0x02;
        require(0 < (storage_map_d[var_a]), CustomError_2d3087f9());
        require(!(bytes1(storage_map_d[var_a])), CustomError_2d3087f9());
        require(((storage_map_d[var_a] / 0x02) < 0x20) == (bytes1(storage_map_d[var_a])), CustomError_2d3087f9());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(bytes1(storage_map_d[var_a])), CustomError_2d3087f9());
        require(0x01 == (bytes1(storage_map_d[var_a])), CustomError_2d3087f9());
        var_a = keccak256(var_a) + 0x02;
        require(0 < (storage_map_d[var_a] / 0x02), CustomError_2d3087f9());
        require(!(0x06 > (bytes1(storage_map_k[var_a]))), CustomError_2d3087f9());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        emit Event_f21de172(address(msg.sender), (var_d + 0x01a0) - var_d, storage_map_e[var_a], !(!bytes1(storage_map_h[var_a])), storage_map_d[var_a], storage_map_k[var_a], storage_map_l[var_a], storage_map_m[var_a], !(!bytes1(storage_map_a[var_a])), storage_map_c[var_a], storage_map_n[var_a], storage_map_f[var_a], storage_map_i[var_a], storage_map_b[var_a], 0x60, bytes1(storage_map_h[var_a]), ((0x20 + ((var_d + 0x01a0) + 0x60)) + 0) - (var_d + 0x01a0), storage_map_b[var_a] / 0x02, storage_map_d[var_a]);
        require(address(msg.sender) - (address(this)), CustomError_2d3087f9());
        store_o = (bytes1(0)) | (uint248(store_o));
        require(!0 < (storage_map_d[var_a]));
        var_a = address(msg.sender);
        require(!0 < (storage_map_d[var_a]));
        var_a = storage_map_g[0 + keccak256(var_a)];
        address var_b = keccak256(var_a);
        require(0 > (0 + storage_map_b[var_a]));
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff);
        require(!(0 == 0x0f4240) | !0);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        address var_aa = storage_map_c[var_a];
        (bool success, bytes memory ret0) = address(store_p).Unresolved_67c84efd(var_aa, var_ab); // staticcall
        require(!var_b);
        require(0x80 > ret0.length);
        require(((var_d + 0x80) > 0xffffffffffffffff) | ((var_d + 0x80) < var_d));
        uint256 var_d = var_d + 0x80;
        require(((var_d + 0x80) - var_d) < 0x80);
        require(((var_d + 0x80) - var_d) < 0x80);
        require(((var_d + 0x80) > 0xffffffffffffffff) | ((var_d + 0x80) < var_d));
        var_d = var_d + 0x80;
        require(var_d.length - var_d.length);
    }
    
    /// @custom:selector    0x5915bf24
    /// @custom:signature   Unresolved_5915bf24(address arg0) public payable returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_5915bf24(address arg0) public payable returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        address var_d = storage_map_c[var_a];
        (bool success, bytes memory ret0) = address(store_p).Unresolved_67c84efd(var_d); // staticcall
        var_a = address(arg0);
        require(storage_map_e[var_a] - 0);
        var_a = address(arg0);
        require(!0 < (storage_map_d[var_a]));
        var_a = address(arg0);
        var_b = 0xd8b7119d98f3b9a11f68b97c850ed595ac3b152112579e0a054fa14456ba3add;
        require(!0 < (storage_map_d[var_a]));
        require(!0x06 > 0x02);
        require(!(0 == 0x0f4240) | !0);
        require(!var_b);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((0 / var_b) > (storage_map_e[var_a]));
        return 0;
        require(storage_map_e[var_a] - (0 / var_b) > (storage_map_e[var_a]));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return (storage_map_e[var_a]) - (0 / var_b);
        return 0;
        require(0x80 > ret0.length);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        uint256 var_f = var_f + 0x80;
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        var_f = var_f + 0x80;
        require(var_f.length - var_f.length);
    }
    
    /// @custom:selector    0xa6fdac35
    /// @custom:signature   approveUnstake(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function approveUnstake(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        require(address(msg.sender) - (address(this)), CustomError_8beb9d16());
        require(bytes1(store_j), CustomError_8beb9d16());
        store_j = (bytes1(0x01)) | (uint248(store_j));
        address var_a = address(arg0);
        require(!(bytes1(storage_map_b[var_a])), CustomError_9c54e5a8());
        var_a = address(arg0);
        require(storage_map_b[var_a] > 0, CustomError_9c54e5a8());
        var_a = address(arg0);
        require(bytes1(storage_map_b[var_a]), CustomError_9ae55bc7());
        var_a = storage_map_c[var_a];
        require(storage_map_b[var_a] > (storage_map_b[var_a] + (storage_map_d[var_a])), CustomError_9ab7872d());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(block.timestamp < (storage_map_b[var_a] + (storage_map_d[var_a])), CustomError_9ab7872d());
        var_a = address(arg0);
        require(storage_map_e[var_a] - storage_map_b[var_a] > (storage_map_e[var_a]), CustomError_865b21e1());
        var_a = address(arg0);
        require(bytes1(storage_map_b[var_a]), CustomError_865b21e1());
        var_a = address(arg0);
        storage_map_b[var_a] = (bytes1(0)) | (uint248(storage_map_b[var_a]));
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (0 & (storage_map_b[var_a]));
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (0 & (storage_map_b[var_a]));
        var_a = address(arg0);
        require(storage_map_e[var_a] - storage_map_b[var_a] > (storage_map_e[var_a]), CustomError_865b21e1());
        var_a = address(arg0);
        require(!(0 < (storage_map_d[var_a])), CustomError_865b21e1());
        require(!(0 < (storage_map_d[var_a])), CustomError_865b21e1());
        var_a = storage_map_g[0 + keccak256(var_a)];
        require(0 > (0 + storage_map_b[var_a]), CustomError_865b21e1());
        var_a = storage_map_f[var_a];
        require(!((0 == 0x0f4240) | !0), CustomError_865b21e1());
        var_a = storage_map_d[var_a];
        require(!(storage_map_h[var_a]), CustomError_865b21e1());
        require(!((0 / (storage_map_h[var_a])) > (storage_map_e[var_a])), CustomError_865b21e1());
        require(storage_map_b[var_a] > 0, CustomError_865b21e1());
        if (storage_map_i[var_a] - storage_map_b[var_a] > (storage_map_i[var_a])) {
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(storage_map_i[var_a] - storage_map_b[var_a] > (storage_map_i[var_a]), CustomError_0b7c70f3());
        }
    }
    
    /// @custom:selector    0xbd90c2f6
    /// @custom:signature   Unresolved_bd90c2f6(address arg0, uint256 arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_bd90c2f6(address arg0, uint256 arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0x0ac8af55
    /// @custom:signature   Unresolved_0ac8af55(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_0ac8af55(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x73e44ef0
    /// @custom:signature   Unresolved_73e44ef0(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_73e44ef0(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x5273b255
    /// @custom:signature   Unresolved_5273b255(address arg0, uint256 arg1) public view
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_5273b255(address arg0, uint256 arg1) public view {
        require(msg.value);
        require(address(arg0) - arg0);
        require(!0x04 > arg1);
        address var_a = arg0;
        address var_b = address(var_a);
        require(storage_map_q[var_b] == 0, CustomError_ea8e4eb5());
        var_b = address(msg.sender);
        require(bytes1(storage_map_r[var_b]), CustomError_ea8e4eb5());
        require(!0x01, CustomError_ea8e4eb5());
        require(!(0x04 > 0x02), CustomError_92aa7d0f());
        require(!(0x04 > arg1), CustomError_92aa7d0f());
        require(!(arg1 == 0x02), CustomError_92aa7d0f());
        require(!(arg1 == 0x02), CustomError_92aa7d0f());
        require(!(0x04 > 0x01), CustomError_92aa7d0f());
        var_b = address(msg.sender);
        require(bytes1(storage_map_r[var_b]), CustomError_ea8e4eb5());
        var_b = storage_map_q[var_b];
        require(address(storage_map_s[var_b]) == (address(msg.sender)), CustomError_ea8e4eb5());
        require(((var_i + (uint248(0x1f + (((((var_i + 0x20) + 0x20) + 0x14) + 0x04) - var_i)))) > 0xffffffffffffffff) | ((var_i + (uint248(0x1f + (((((var_i + 0x20) + 0x20) + 0x14) + 0x04) - var_i)))) < var_i), CustomError_ea8e4eb5());
        var_b = keccak256(var_j);
        require(bytes1(storage_map_r[var_b]), CustomError_ea8e4eb5());
        require(!0, CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x3e9a2a3c
    /// @custom:signature   Unresolved_3e9a2a3c(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    function Unresolved_3e9a2a3c(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3, uint256 arg4) public view {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
        require(arg2 - arg2);
        require(arg3 - arg3);
        require(arg4 > 0xffffffffffffffff);
        require(arg4 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_9ae55bc7());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_9ae55bc7());
        var_a = storage_map_c[var_a];
        require(!(bytes1(storage_map_b[var_a])), CustomError_552ff5ec());
        require(!(arg2 == 0), CustomError_552ff5ec());
        var_a = arg2;
        require(!(bytes1(storage_map_l[var_a])), CustomError_552ff5ec());
        require(!(0 < (storage_map_d[var_a])), CustomError_552ff5ec());
        require(!(0 < (storage_map_d[var_a])), CustomError_552ff5ec());
        var_a = keccak256(var_a) + 0x02;
        require(arg1 == (storage_map_g[0 + keccak256(var_a)]), CustomError_552ff5ec());
        require(arg1 == (storage_map_g[0 + keccak256(var_a)]), CustomError_552ff5ec());
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_552ff5ec());
        require(!0x01, CustomError_552ff5ec());
        require(!0, CustomError_552ff5ec());
    }
    
    /// @custom:selector    0x549f332a
    /// @custom:signature   cancelUnstakeRequest(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function cancelUnstakeRequest(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(storage_map_f[var_a] == 0, CustomError_ea8e4eb5());
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0x01, CustomError_ea8e4eb5());
        var_a = address(arg0);
        require(!(bytes1(storage_map_b[var_a])), CustomError_0b7c70f3());
        var_a = address(arg0);
        storage_map_b[var_a] = (bytes1(0)) | (uint248(storage_map_b[var_a]));
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (0 & (storage_map_b[var_a]));
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (0 & (storage_map_b[var_a]));
        var_a = address(arg0);
        storage_map_b[var_a] = 0 | (0 & (storage_map_b[var_a]));
        emit UnstakeRequestCancelled(address(arg0));
        var_a = address(msg.sender);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        var_a = storage_map_f[var_a];
        require(address(storage_map_h[var_a]) == (address(msg.sender)), CustomError_ea8e4eb5());
        require(((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) < var_g), CustomError_ea8e4eb5());
        var_a = keccak256(var_i);
        require(bytes1(storage_map_b[var_a]), CustomError_ea8e4eb5());
        require(!0, CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x72b864af
    /// @custom:signature   Unresolved_72b864af(uint256 arg0, uint256 arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_72b864af(uint256 arg0, uint256 arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xbe6e309a
    /// @custom:signature   Unresolved_be6e309a(uint256 arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_be6e309a(uint256 arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(((var_c + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) < var_c));
    }
    
    /// @custom:selector    0x209e0d60
    /// @custom:signature   Unresolved_209e0d60(uint256 arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_209e0d60(uint256 arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(!0x06 > arg0);
        require(arg1 - arg1);
        require(arg2 - arg2);
    }
    
    /// @custom:selector    0xcb75cc5e
    /// @custom:signature   Unresolved_cb75cc5e(uint256 arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_cb75cc5e(uint256 arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(((var_c + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) < var_c));
    }
    
    /// @custom:selector    0x02c8fcaf
    /// @custom:signature   Unresolved_02c8fcaf(uint256 arg0, uint256 arg1, uint256 arg2) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_02c8fcaf(uint256 arg0, uint256 arg1, uint256 arg2) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(((var_c + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) < var_c));
    }
    
    /// @custom:selector    0xed646826
    /// @custom:signature   Unresolved_ed646826(address arg0) public payable returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_ed646826(address arg0) public payable returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        var_a = address(arg0);
        require(bytes1(storage_map_b[var_a]));
        var_a = address(arg0);
        return storage_map_b[var_a];
        require(!0 < (storage_map_d[var_a]));
        var_a = address(arg0);
        require(!0 < (storage_map_d[var_a]));
        var_a = storage_map_g[0 + keccak256(var_a)];
        address var_b = keccak256(var_a);
        require(0 > (0 + storage_map_b[var_a]));
        require(0 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff);
        require(!(0 == 0x0f4240) | !0);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        address var_e = storage_map_c[var_a];
        (bool success, bytes memory ret0) = address(store_p).Unresolved_67c84efd(var_e); // staticcall
        require(!var_b);
        require(0x80 > ret0.length);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        uint256 var_f = var_f + 0x80;
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) - var_f) < 0x80);
        require(((var_f + 0x80) > 0xffffffffffffffff) | ((var_f + 0x80) < var_f));
        var_f = var_f + 0x80;
        require(var_f.length - var_f.length);
    }
    
    /// @custom:selector    0x6a320984
    /// @custom:signature   getUnstakeCooldownEndTime(address arg0) public view returns (uint256)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function getUnstakeCooldownEndTime(address arg0) public view returns (uint256) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(!(bytes1(storage_map_b[var_a])), CustomError_0b7c70f3());
        var_a = storage_map_c[var_a];
        require(storage_map_b[var_a] > (storage_map_b[var_a] + (storage_map_d[var_a])), CustomError_0b7c70f3());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        return storage_map_b[var_a] + (storage_map_d[var_a]);
    }
    
    /// @custom:selector    0x83c592cf
    /// @custom:signature   Unresolved_83c592cf(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_83c592cf(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
}