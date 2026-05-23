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
    mapping(bytes32 => bytes32) storage_map_u;
    mapping(bytes32 => bytes32) storage_map_a;
    bytes32 store_c;
    mapping(bytes32 => bytes32) storage_map_i;
    mapping(bytes32 => bytes32) storage_map_r;
    address store_f;
    mapping(bytes32 => bytes32) storage_map_j;
    bytes32 store_p;
    address store_v;
    mapping(bytes32 => bytes32) storage_map_m;
    mapping(bytes32 => bytes32) storage_map_n;
    mapping(bytes32 => bytes32) storage_map_e;
    address store_s;
    address store_h;
    uint16 store_b;
    mapping(bytes32 => bytes32) storage_map_q;
    uint24 store_g;
    mapping(bytes32 => bytes32) storage_map_o;
    bytes32 store_t;
    mapping(bytes32 => bytes32) storage_map_k;
    address store_d;
    
    event Event_65592336();
    event Event_e7a8ccaa();
    event Event_471c950e();
    error ZeroAddress();
    event Event_f1277a22();
    event ExpectedWorkflowOwnerUpdated(address, address);
    event Event_0067245d();
    event UpdatedExchangeStatus(address, bool, bool);
    event Event_cc790d80();
    event Event_000fc11d();
    event Event_f96b30e3();
    event Event_60f48695();
    event Event_6880bcb1();
    event Event_68a9923f();
    
    /// @custom:selector    0xad541c20
    /// @custom:signature   Unresolved_ad541c20(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_ad541c20(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xd7307fc7
    /// @custom:signature   Unresolved_d7307fc7(uint16 arg0) public payable
    /// @param              arg0 ["uint16", "bytes2", "int16"]
    function Unresolved_d7307fc7(uint16 arg0) public payable {
        require(msg.value);
        require(uint16(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        store_b = (uint16(arg0)) | (uint240(store_b));
        emit Event_e7a8ccaa(uint16(arg0));
    }
    
    /// @custom:selector    0xebe3bb62
    /// @custom:signature   Unresolved_ebe3bb62(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_ebe3bb62(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x0f3e94d5
    /// @custom:signature   Unresolved_0f3e94d5(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_0f3e94d5(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_d92e233d());
        require(address(arg0) == 0, CustomError_d92e233d());
        store_c = (address(arg0)) | (uint96(store_c));
        emit Event_60f48695(address(store_c), address(arg0));
    }
    
    /// @custom:selector    0x6893c391
    /// @custom:signature   Unresolved_6893c391(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_6893c391(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x67f75e85
    /// @custom:signature   Unresolved_67f75e85(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_67f75e85(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x26f6d76a
    /// @custom:signature   Unresolved_26f6d76a(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_26f6d76a(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x7cae13a1
    /// @custom:signature   Unresolved_7cae13a1(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_7cae13a1(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        var_a = arg0;
        storage_map_a[var_a] = (bytes1(0)) | (uint248(storage_map_a[var_a]));
        emit Event_65592336(arg0, 0);
    }
    
    /// @custom:selector    0x670a6fd9
    /// @custom:signature   Unresolved_670a6fd9(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_670a6fd9(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xb80d8390
    /// @custom:signature   Unresolved_b80d8390(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_b80d8390(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xf6067dd1
    /// @custom:signature   Unresolved_f6067dd1(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_f6067dd1(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x0db354bc
    /// @custom:signature   Unresolved_0db354bc(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_0db354bc(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x68a39b5f
    /// @custom:signature   Unresolved_68a39b5f(uint256 arg0, address arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint128", "bytes16", "int128"]
    function Unresolved_68a39b5f(uint256 arg0, address arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
    }
    
    /// @custom:selector    0x70a26ec4
    /// @custom:signature   Unresolved_70a26ec4(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_70a26ec4(uint256 arg0) public pure {
        require(msg.value);
        require(((var_a + 0x80) > 0xffffffffffffffff) | ((var_a + 0x80) < var_a));
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x81fbf4d2
    /// @custom:signature   Unresolved_81fbf4d2(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    function Unresolved_81fbf4d2(uint256 arg0, uint256 arg1, uint256 arg2, uint256 arg3) public view {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg3 - arg3);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_a24a13a6());
        require(!(!(arg0) == (arg1)), CustomError_a24a13a6());
        require(!(arg0 == (arg1)), CustomError_a24a13a6());
        require(arg0 > 0x01f4, CustomError_bb1cb70b());
        require(arg3 == 0, CustomError_16c726b1());
        require(!0x015180, CustomError_16c726b1());
        require(!0x023ab1, CustomError_16c726b1());
        require(!0x015180, CustomError_16c726b1());
    }
    
    /// @custom:selector    0xd02c21b1
    /// @custom:signature   setChainlinkForwarder(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function setChainlinkForwarder(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_e6c4247b());
        require(address(arg0) == 0, CustomError_e6c4247b());
        store_d = (address(arg0)) | (uint96(store_d));
        emit Event_68a9923f(address(arg0));
    }
    
    /// @custom:selector    0xd418f29a
    /// @custom:signature   Unresolved_d418f29a(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_d418f29a(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xed9f18fb
    /// @custom:signature   Unresolved_ed9f18fb(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_ed9f18fb(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x8b62fa8c
    /// @custom:signature   Unresolved_8b62fa8c(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_8b62fa8c(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xc8016fb9
    /// @custom:signature   Unresolved_c8016fb9(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_c8016fb9(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x88c76f79
    /// @custom:signature   Unresolved_88c76f79(address arg0, address arg1) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_88c76f79(address arg0, address arg1) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
        require(address(arg1) - arg1);
    }
    
    /// @custom:selector    0x89d2e8fa
    /// @custom:signature   Unresolved_89d2e8fa(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_89d2e8fa(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xcca6ba8a
    /// @custom:signature   Unresolved_cca6ba8a(uint256 arg0, uint256 arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_cca6ba8a(uint256 arg0, uint256 arg1) public view {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!bytes1(storage_map_a[var_a]));
        require(arg0 > 0xffffffffffffffff);
        require(((var_d + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) < var_d));
        require((0x20 + (0x04 + arg0)) < ((0x20 + (0x04 + arg0)) + (arg0 * 0x20)));
        require((0x20 + (arg0)) - (0x20 + (arg0)));
    }
    
    /// @custom:selector    0xb66d6bcb
    /// @custom:signature   Unresolved_b66d6bcb(uint256 arg0, uint256 arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_b66d6bcb(uint256 arg0, uint256 arg1) public view {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!bytes1(storage_map_a[var_a]));
        require(arg0 > 0xffffffffffffffff);
        require(((var_d + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) < var_d));
        require((0x20 + (0x04 + arg0)) < ((0x20 + (0x04 + arg0)) + (arg0 * 0x20)));
        require((0x20 + (arg0)) - (0x20 + (arg0)));
    }
    
    /// @custom:selector    0x134238dd
    /// @custom:signature   Unresolved_134238dd(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_134238dd(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x5bbcb659
    /// @custom:signature   Unresolved_5bbcb659(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_5bbcb659(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        var_a = arg0;
        storage_map_e[var_a] = 0 | (uint240(storage_map_e[var_a]));
        emit Event_f1277a22(arg0);
    }
    
    /// @custom:selector    0xb79ea77f
    /// @custom:signature   setFunctionsRouter(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function setFunctionsRouter(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        store_f = (address(arg0)) | (uint96(store_f));
        emit Event_471c950e(address(arg0));
    }
    
    /// @custom:selector    0x2e692c3e
    /// @custom:signature   Unresolved_2e692c3e(uint24 arg0) public payable
    /// @param              arg0 ["uint24", "bytes3", "int24"]
    function Unresolved_2e692c3e(uint24 arg0) public payable {
        require(msg.value);
        require(uint24(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_b4fa3fb3());
        require(uint24(arg0) > 0x03e8, CustomError_b4fa3fb3());
        store_g = (uint24(arg0 << 0xb8)) | (uint232(store_g));
        emit Event_000fc11d(uint24(arg0));
    }
    
    /// @custom:selector    0xc1f91760
    /// @custom:signature   Unresolved_c1f91760(uint256 arg0, uint256 arg1) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_c1f91760(uint256 arg0, uint256 arg1) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(!0x04 > arg1);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        var_a = arg0;
        require(!(0x04 > arg1), CustomError_16c726b1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_e[var_a] = (bytes1(arg1 << 0x10)) | (uint248(storage_map_e[var_a]));
        require(!(0x04 > arg1), CustomError_16c726b1());
        emit Event_f96b30e3(address(msg.sender), arg0, arg1);
    }
    
    /// @custom:selector    0xa98fa0d4
    /// @custom:signature   Unresolved_a98fa0d4(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_a98fa0d4(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x9e54503b
    /// @custom:signature   Unresolved_9e54503b(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_9e54503b(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        var_a = arg0;
        storage_map_a[var_a] = (bytes1(0x01)) | (uint248(storage_map_a[var_a]));
        emit Event_65592336(arg0, 0x01);
    }
    
    /// @custom:selector    0xb8fa10f7
    /// @custom:signature   Unresolved_b8fa10f7(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_b8fa10f7(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_e6c4247b());
        require(address(arg0) == 0, CustomError_e6c4247b());
        store_h = (address(arg0)) | (uint96(store_h));
    }
    
    /// @custom:selector    0xcd26d379
    /// @custom:signature   Unresolved_cd26d379(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_cd26d379(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        if (!storage_map_a[var_a]) {
            var_a = storage_map_i[var_a];
            require(!(bytes1(storage_map_a[var_a])), CustomError_7bfa4b9f());
            var_a = address(storage_map_j[var_a]);
            require(address(storage_map_j[var_a]) == 0, CustomError_7bfa4b9f());
            require(address(storage_map_a[var_a]) == (address(arg0)), CustomError_7bfa4b9f());
            var_a = address(storage_map_j[var_a]);
            require(address(storage_map_a[var_a]) == (address(arg0)), CustomError_7bfa4b9f());
            require(!0x01, CustomError_7bfa4b9f());
            require(address(storage_map_j[var_a]) - (address(storage_map_j[var_a])), CustomError_7bfa4b9f());
            var_a = address(storage_map_a[var_a]);
            require(address(storage_map_a[var_a]) == (address(arg0)), CustomError_7bfa4b9f());
        }
        require(0x01, CustomError_7bfa4b9f());
        var_a = address(arg0);
        require(storage_map_k[var_a] == 0, CustomError_7bfa4b9f());
        var_a = address(arg0);
        require(!(0 < (storage_map_l[var_a])), CustomError_7bfa4b9f());
        require(!(0 < (storage_map_l[var_a])), CustomError_7bfa4b9f());
        require(!(0x06 > 0x02), CustomError_7bfa4b9f());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(0x06 > (bytes1(storage_map_m[0 + keccak256(var_a)]))), CustomError_7bfa4b9f());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(bytes1(storage_map_m[0 + keccak256(var_a)]) - 0x02, CustomError_7bfa4b9f());
        var_a = address(arg0);
        var_a = storage_map_n[0 + keccak256(var_a)];
        require(0 > (0 + storage_map_a[var_a]), CustomError_7bfa4b9f());
        var_a = storage_map_k[var_a];
        require(address(storage_map_o[var_a]) - (storage_map_o[var_a]) > (address(storage_map_o[var_a])), CustomError_7bfa4b9f());
        var_a = storage_map_i[var_a];
        var_a = address(arg0);
        var_a = address(storage_map_j[var_a]);
        storage_map_a[var_a] = (address(storage_map_a[var_a])) | (uint96(storage_map_a[var_a]));
        require(address(storage_map_j[var_a]) == (address(arg0)), CustomError_7bfa4b9f());
        var_a = address(arg0);
        storage_map_a[var_a] = 0 | (uint96(storage_map_a[var_a]));
        storage_map_j[var_a] = (address(storage_map_j[var_a])) | (uint96(storage_map_j[var_a]));
        require(address(storage_map_j[var_a]) == (address(arg0)), CustomError_7bfa4b9f());
        var_a = address(arg0);
        storage_map_a[var_a] = 0 | (uint96(storage_map_a[var_a]));
        storage_map_j[var_a] = 0 | (uint96(storage_map_j[var_a]));
        var_a = address(arg0);
        storage_map_a[var_a] = 0 | (uint96(storage_map_a[var_a]));
        require(0, CustomError_7bfa4b9f());
    }
    
    /// @custom:selector    0x57bc58e1
    /// @custom:signature   Unresolved_57bc58e1(uint256 arg0, uint256 arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_57bc58e1(uint256 arg0, uint256 arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xd122f407
    /// @custom:signature   Unresolved_d122f407(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_d122f407(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xa2db4ed9
    /// @custom:signature   Unresolved_a2db4ed9(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_a2db4ed9(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xdf4b1239
    /// @custom:signature   Unresolved_df4b1239() public view
    function Unresolved_df4b1239() public view {
        require(msg.value);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        require(0x01, CustomError_16c726b1());
        require(!(0x01 < store_p), CustomError_16c726b1());
        var_a = 0x01;
        require(!(bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        require(((storage_map_j[var_a] / 0x02) < 0x20) == (bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        require(0x01 == (bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        var_a = keccak256(var_a) + 0x01;
        require(0 < (storage_map_j[var_a] / 0x02), CustomError_16c726b1());
        emit Event_6880bcb1(0x01, storage_map_r[var_a], (var_d + 0x60) - var_d, storage_map_l[var_a], !(!bytes1(storage_map_q[var_a])), (storage_map_j[var_a]) / 0x02);
        require(0x01 == 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, CustomError_16c726b1());
        require(0x01, CustomError_16c726b1());
    }
    
    /// @custom:selector    0x4f619ba4
    /// @custom:signature   Unresolved_4f619ba4() public payable
    function Unresolved_4f619ba4() public payable {
        require(msg.value);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_ea8e4eb5());
        require(!(bytes1(storage_map_a[var_a])), CustomError_ea8e4eb5());
        store_c = 0 | (uint96(store_c));
        emit Event_60f48695(address(store_c), 0);
        require(!(address(msg.sender) == (address(store_c))), CustomError_ea8e4eb5());
        store_c = 0 | (uint96(store_c));
        emit Event_60f48695(address(store_c), 0);
    }
    
    /// @custom:selector    0x4457cd38
    /// @custom:signature   Unresolved_4457cd38(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_4457cd38(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xcec5599c
    /// @custom:signature   Unresolved_cec5599c(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_cec5599c(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x26bbf2ae
    /// @custom:signature   setExpectedWorkflowOwner(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function setExpectedWorkflowOwner(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_e6c4247b());
        require(address(arg0) == 0, CustomError_e6c4247b());
        store_s = (address(arg0)) | (uint96(store_s));
        emit ExpectedWorkflowOwnerUpdated(address(store_s), address(arg0));
    }
    
    /// @custom:selector    0x2952c715
    /// @custom:signature   toggleExchangeStatus() public payable
    function toggleExchangeStatus() public payable {
        require(msg.value);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        emit UpdatedExchangeStatus(address(msg.sender), !(!bytes1(store_t)), (!bytes1(store_t)));
        store_t = (bytes1(!store_t)) | (uint248(store_t));
    }
    
    /// @custom:selector    0x5f4c82d2
    /// @custom:signature   Unresolved_5f4c82d2(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_5f4c82d2(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        if (!storage_map_a[var_a]) {
            var_a = address(arg0);
            require(!(bytes1(storage_map_a[var_a])), CustomError_7bfa4b9f());
            require(!(bytes1(storage_map_j[var_a])), CustomError_7bfa4b9f());
            var_a = address(arg0);
            require(!(bytes1(storage_map_u[var_a])), CustomError_7bfa4b9f());
            require(bytes1(storage_map_a[var_a]), CustomError_7bfa4b9f());
            var_a = address(arg0);
            require(!0, CustomError_7bfa4b9f());
            require(!(0 < (storage_map_l[var_a])), CustomError_7bfa4b9f());
            var_a = keccak256(var_a) + 0x02;
            require(!(0 < (storage_map_l[var_a])), CustomError_7bfa4b9f());
            require(!(0x06 > 0x02), CustomError_7bfa4b9f());
            var_a = address(arg0);
            var_a = storage_map_i[var_a];
            var_a = address(arg0);
            require(!0, CustomError_7bfa4b9f());
            require(address(storage_map_a[var_a]) - 0, CustomError_7bfa4b9f());
            var_a = address(storage_map_j[var_a]);
            var_a = address(arg0);
            storage_map_a[var_a] = (address(storage_map_a[var_a])) | (uint96(storage_map_a[var_a]));
            var_a = address(storage_map_j[var_a]);
            storage_map_a[var_a] = (address(arg0)) | (uint96(storage_map_a[var_a]));
            storage_map_j[var_a] = (address(arg0)) | (uint96(storage_map_j[var_a]));
            var_a = address(arg0);
            require(address(storage_map_j[var_a]) - 0, CustomError_7bfa4b9f());
            var_a = address(arg0);
            require(storage_map_k[var_a] == 0, CustomError_7bfa4b9f());
            require(!(0 < (storage_map_l[var_a])), CustomError_7bfa4b9f());
            var_a = keccak256(var_a) + 0x02;
            require(!(0 < (storage_map_l[var_a])), CustomError_7bfa4b9f());
            require(!(0x06 > 0x02), CustomError_7bfa4b9f());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(!(0x06 > (bytes1(storage_map_m[0 + keccak256(var_a)]))), CustomError_7bfa4b9f());
            var_a = address(arg0);
            var_a = storage_map_n[0 + keccak256(var_a)];
            require(bytes1(storage_map_m[0 + keccak256(var_a)]) - 0x02, CustomError_7bfa4b9f());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(0 > (0 + storage_map_a[var_a]), CustomError_7bfa4b9f());
            require(storage_map_a[var_a] == 0, CustomError_7bfa4b9f());
        }
    }
    
    /// @custom:selector    0x7b6a00d8
    /// @custom:signature   Unresolved_7b6a00d8(uint256 arg0, uint256 arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_7b6a00d8(uint256 arg0, uint256 arg1) public view {
        require(msg.value);
        require(arg0 > 0xffffffffffffffff);
        require(arg0 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        require(arg1 > 0xffffffffffffffff);
        address var_a = address(msg.sender);
        require(!bytes1(storage_map_a[var_a]));
        require(arg0 > 0xffffffffffffffff);
        require(((var_d + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((arg0 * 0x20) + 0x20)))) < var_d));
        require((0x20 + (0x04 + arg0)) < ((0x20 + (0x04 + arg0)) + (arg0 * 0x20)));
        require(address(0x20 + (arg0)) - (0x20 + (arg0)));
    }
    
    /// @custom:selector    0x7e3f9d49
    /// @custom:signature   Unresolved_7e3f9d49(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_7e3f9d49(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x2177be25
    /// @custom:signature   Unresolved_2177be25(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_2177be25(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0xa9e66a67
    /// @custom:signature   Unresolved_a9e66a67(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_a9e66a67(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_883e365f());
        require(address(arg0) == 0, CustomError_883e365f());
        require(address(store_v) - 0, CustomError_883e365f());
        store_v = (address(arg0)) | (uint96(store_v));
        emit Event_0067245d(0, address(arg0));
    }
    
    /// @custom:selector    0x23962f1a
    /// @custom:signature   Unresolved_23962f1a(uint256 arg0) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_23962f1a(uint256 arg0) public payable {
        require(msg.value);
        require(arg0 - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_16c726b1());
        var_a = arg0;
        storage_map_q[var_a] = (bytes1(0)) | (uint248(storage_map_q[var_a]));
        require(!(bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        require(((storage_map_j[var_a] / 0x02) < 0x20) == (bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        require(0x01 == (bytes1(storage_map_j[var_a])), CustomError_16c726b1());
        var_a = keccak256(var_a) + 0x01;
        require(0 < (storage_map_j[var_a] / 0x02), CustomError_16c726b1());
        emit Event_cc790d80(0x20, storage_map_o[var_a], ((var_f + 0x20) + 0xa0) - (var_f + 0x20), storage_map_l[var_a], storage_map_r[var_a], !(!bytes1(storage_map_q[var_a])), (storage_map_j[var_a]) / 0x02);
        emit Event_cc790d80();
        emit Event_cc790d80(0x20, storage_map_o[var_a], ((var_f + 0x20) + 0xa0) - (var_f + 0x20), storage_map_l[var_a], storage_map_r[var_a], !(!bytes1(storage_map_q[var_a])), (storage_map_j[var_a]) / 0x02);
    }
    
    /// @custom:selector    0xaa050846
    /// @custom:signature   Unresolved_aa050846() public payable
    function Unresolved_aa050846() public payable {
        require(msg.value);
        require(address(msg.sender) - (address(store_c)), CustomError_ea8e4eb5());
        require(address(msg.sender).code.length == 0, CustomError_09ee12d5());
        store_v = (address(msg.sender)) | (uint96(store_v));
        store_c = 0 | (uint96(store_c));
        emit Event_0067245d(address(store_v), address(msg.sender));
    }
}