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
    mapping(bytes32 => bytes32) storage_map_e;
    mapping(bytes32 => bytes32) storage_map_d;
    mapping(bytes32 => bytes32) storage_map_b;
    bytes32 store_f;
    
    error EmptyName();
    event Event_87bc54dc();
    event Event_a9da32da();
    
    /// @custom:selector    0x67c64267
    /// @custom:signature   Unresolved_67c64267(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_67c64267(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_e6c4247b());
        require(address(arg0) == 0, CustomError_e6c4247b());
        var_a = address(arg0);
        storage_map_a[var_a] = (bytes1(0)) | (uint248(storage_map_a[var_a]));
        emit Event_87bc54dc(address(msg.sender), address(arg0));
    }
    
    /// @custom:selector    0x97f8c8b3
    /// @custom:signature   Unresolved_97f8c8b3(uint256 arg0, address arg1, uint256 arg2) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_97f8c8b3(uint256 arg0, address arg1, uint256 arg2) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg0 == 0, CustomError_ee240e49());
        uint256 var_a = arg0;
        require(!(address(storage_map_b[var_a]) == 0), CustomError_ee240e49());
        var_a = arg0;
        require(!(storage_map_c[var_a] == 0), CustomError_ee240e49());
        var_a = arg0;
        require(storage_map_d[var_a] == 0, CustomError_ee240e49());
        var_a = storage_map_c[var_a];
        require(storage_map_d[var_a] > 0, CustomError_ee240e49());
        var_a = address(storage_map_b[var_a]);
        require(storage_map_a[var_a] < (storage_map_d[var_a]), CustomError_ee240e49());
        require(!0, CustomError_ee240e49());
        var_a = arg0;
        require(!(address(storage_map_b[var_a]) == msg.sender), CustomError_ee240e49());
        var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_ee240e49());
        require(address(arg1) == 0, CustomError_ee240e49());
        var_a = arg0;
        require(storage_map_e[keccak256(var_a) + 0] == 0, CustomError_ee240e49());
        require(arg2 == 0, CustomError_ee240e49());
        require(arg2 > 0xffffffffffffffff, CustomError_ee240e49());
        require(((var_d + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)))) < var_d), CustomError_ee240e49());
        uint256 var_d = var_d + (uint248(0x1f + ((0x1f + (arg2)) + 0x20)));
        var_a = keccak256(var_h);
        require(!(address(storage_map_a[var_a]) == 0), CustomError_ee240e49());
        require(!(address(storage_map_a[var_a]) == (address(arg1))), CustomError_ee240e49());
        var_a = address(arg1);
        require(!bytes1(storage_map_a[var_a]));
        require(((storage_map_a[var_a] / 0x02) < 0x20) == (bytes1(storage_map_a[var_a])));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((storage_map_a[var_a] / 0x02) > 0);
        require(!bytes1(storage_map_a[var_a]));
        require(((storage_map_a[var_a] / 0x02) < 0x20) == (bytes1(storage_map_a[var_a])));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!bytes1(storage_map_a[var_a]));
        require(0x01 == (bytes1(storage_map_a[var_a])));
        var_a = keccak256(var_a);
        require(0 < (storage_map_a[var_a] / 0x02));
        require(((var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)))) < var_d));
        var_d = var_d + (uint248(0x1f + (((0x20 + var_d) + 0) - var_d)));
        var_a = keccak256(var_h);
        storage_map_a[var_a] = 0 | (uint96(storage_map_a[var_a]));
        var_a = address(arg1);
        require(arg2 > 0xffffffffffffffff);
        require(((var_d + (uint248(0x1f + (0 - var_d)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + (0 - var_d)))) < var_d));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(address(storage_map_b[var_a]) == msg.sender), CustomError_a8143fbc());
        require(!0x01, CustomError_ff9b022c());
    }
    
    /// @custom:selector    0x5b2dae41
    /// @custom:signature   Unresolved_5b2dae41(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_5b2dae41(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x675dcb14
    /// @custom:signature   Unresolved_675dcb14(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_675dcb14(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x0912489a
    /// @custom:signature   Unresolved_0912489a(address arg0) public view
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_0912489a(address arg0) public view {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        require(storage_map_e[keccak256(var_a) + 0] > 0xffffffffffffffff);
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(((var_d + (uint248(0x1f + ((storage_map_e[keccak256(var_a) + 0] * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_d + (uint248(0x1f + ((storage_map_e[keccak256(var_a) + 0] * 0x20) + 0x20)))) < var_d));
    }
    
    /// @custom:selector    0xe09cf319
    /// @custom:signature   Unresolved_e09cf319(address arg0) public payable
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_e09cf319(address arg0) public payable {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(msg.sender);
        require(!(bytes1(storage_map_a[var_a])), CustomError_e6c4247b());
        require(address(arg0) == 0, CustomError_e6c4247b());
        var_a = address(arg0);
        storage_map_a[var_a] = (bytes1(0x01)) | (uint248(storage_map_a[var_a]));
        emit Event_a9da32da(address(msg.sender), address(arg0));
    }
    
    /// @custom:selector    0x574bc677
    /// @custom:signature   Unresolved_574bc677(uint256 arg0, address arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_574bc677(uint256 arg0, address arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
    }
    
    /// @custom:selector    0xb9aaddff
    /// @custom:signature   Unresolved_b9aaddff(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_b9aaddff(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0x503f2600
    /// @custom:signature   Unresolved_503f2600(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_503f2600(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0xdbafa9f0
    /// @custom:signature   isGlobalAdmin(address arg0) public view returns (bool)
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function isGlobalAdmin(address arg0) public view returns (bool) {
        require(msg.value);
        require(address(arg0) - arg0);
        address var_a = address(arg0);
        return !(!bytes1(storage_map_a[var_a]));
    }
    
    /// @custom:selector    0x7eeab954
    /// @custom:signature   Unresolved_7eeab954(address arg0) public pure
    /// @param              arg0 ["address", "uint160", "bytes20", "int160"]
    function Unresolved_7eeab954(address arg0) public pure {
        require(msg.value);
        require(address(arg0) - arg0);
    }
    
    /// @custom:selector    0xcd9e511e
    /// @custom:signature   Unresolved_cd9e511e(uint256 arg0, address arg1, uint256 arg2) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    function Unresolved_cd9e511e(uint256 arg0, address arg1, uint256 arg2) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg0 == 0);
        uint256 var_a = arg0;
        require(!(address(storage_map_b[var_a])) == 0);
        var_a = arg0;
        require(!(storage_map_c[var_a]) == 0);
        var_a = arg0;
        require(storage_map_d[var_a] == 0);
        var_a = storage_map_c[var_a];
        require(storage_map_d[var_a] > 0);
        var_a = address(storage_map_b[var_a]);
        require(storage_map_a[var_a] < (storage_map_d[var_a]));
        require(!0);
        var_a = arg0;
        require(!(address(storage_map_b[var_a])) == msg.sender);
        var_a = address(msg.sender);
        require(!bytes1(storage_map_a[var_a]));
        require(address(arg1) == 0);
        require(!0 < (arg2));
        require(!0 < (arg2));
        require(uint32(0 + (0x20 + (arg2))) - (0 + (0x20 + (arg2))));
        if (((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) < var_g)) {
            uint248 var_g = var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)));
            require(((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) < var_g), CustomError_e6c4247b());
            var_a = keccak256(var_i);
            storage_map_a[var_a] = (bytes1(0)) | (uint248(storage_map_a[var_a]));
            var_a = arg0;
            var_a = uint32(0 + (0x20 + (arg2)));
            require(!(bytes1(storage_map_a[var_a])), CustomError_e6c4247b());
            require(storage_map_a[var_a] == 0, CustomError_e6c4247b());
            require((storage_map_a[var_a] - 0x01) > storage_map_a[var_a], CustomError_e6c4247b());
            require((storage_map_e[keccak256(var_a) + 0] - 0x01) > (storage_map_e[keccak256(var_a) + 0]), CustomError_e6c4247b());
            require((storage_map_a[var_a] - 0x01) - (storage_map_e[keccak256(var_a) + 0] - 0x01), CustomError_e6c4247b());
            var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
            require(!((storage_map_e[keccak256(var_a) + 0] - 0x01) < (storage_map_e[keccak256(var_a) + 0])), CustomError_e6c4247b());
            var_a = arg0;
            require(!(storage_map_e[keccak256(var_a) + 0]), CustomError_e6c4247b());
        }
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(address(storage_map_b[var_a]) == msg.sender), CustomError_a8143fbc());
        require(!0x01, CustomError_ff9b022c());
    }
    
    /// @custom:selector    0xa0ee740f
    /// @custom:signature   Unresolved_a0ee740f(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_a0ee740f(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
    }
    
    /// @custom:selector    0x5e4b61c5
    /// @custom:signature   Unresolved_5e4b61c5() public view
    function Unresolved_5e4b61c5() public view {
        require(msg.value);
        if (store_f > 0xffffffffffffffff) {
            if (((var_c + (uint248(0x1f + ((store_f * 0x20) + 0x20)))) > 0xffffffffffffffff) | ((var_c + (uint248(0x1f + ((store_f * 0x20) + 0x20)))) < var_c)) {
            }
        }
    }
    
    /// @custom:selector    0x4616d0b4
    /// @custom:signature   Unresolved_4616d0b4(uint256 arg0, address arg1, uint256 arg2, uint256 arg3) public payable
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["address", "uint160", "bytes20", "int160"]
    /// @param              arg2 ["uint256", "bytes32", "int256"]
    /// @param              arg3 ["uint256", "bytes32", "int256"]
    function Unresolved_4616d0b4(uint256 arg0, address arg1, uint256 arg2, uint256 arg3) public payable {
        require(msg.value);
        require(arg0 - arg0);
        require(address(arg1) - arg1);
        require(arg2 > 0xffffffffffffffff);
        require(arg2 > 0xffffffffffffffff);
        require(arg3 > 0xffffffffffffffff);
        require(arg3 > 0xffffffffffffffff);
        require(arg0 == 0);
        uint256 var_a = arg0;
        require(!(address(storage_map_b[var_a])) == 0);
        var_a = arg0;
        require(!(storage_map_c[var_a]) == 0);
        var_a = arg0;
        require(storage_map_d[var_a] == 0);
        var_a = storage_map_c[var_a];
        require(storage_map_d[var_a] > 0);
        var_a = address(storage_map_b[var_a]);
        require(storage_map_a[var_a] < (storage_map_d[var_a]));
        require(!0);
        var_a = arg0;
        require(!(address(storage_map_b[var_a])) == msg.sender);
        var_a = address(msg.sender);
        require(!bytes1(storage_map_a[var_a]));
        require(address(arg1) == 0);
        require(!0 < (arg2));
        require(!0 < (arg2));
        require(uint32(0 + (0x20 + (arg2))) - (0 + (0x20 + (arg2))));
        require(((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)))) < var_g), CustomError_ee240e49());
        uint248 var_g = var_g + (uint248(0x1f + (((((var_g + 0x20) + 0x20) + 0x14) + 0x04) - var_g)));
        var_a = keccak256(var_i);
        require(bytes1(storage_map_a[var_a]), CustomError_ee240e49());
        var_a = arg0;
        require(storage_map_e[keccak256(var_a) + 0] == 0, CustomError_ee240e49());
        require(storage_map_e[keccak256(var_a) + 0] == 0, CustomError_ee240e49());
        require(arg3 > 0, CustomError_ee240e49());
        require(arg3 == 0, CustomError_ee240e49());
        require(arg3 > 0xffffffffffffffff, CustomError_ee240e49());
        require(((var_g + (uint248(0x1f + ((0x1f + (arg3)) + 0x20)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + ((0x1f + (arg3)) + 0x20)))) < var_g), CustomError_ee240e49());
        var_g = var_g + (uint248(0x1f + ((0x1f + (arg3)) + 0x20)));
        var_a = keccak256(var_i);
        require(!(address(storage_map_a[var_a]) == 0), CustomError_ee240e49());
        require(!(address(storage_map_a[var_a]) == (address(arg1))), CustomError_ee240e49());
        var_a = address(arg1);
        require(!bytes1(storage_map_a[var_a]));
        require(((storage_map_a[var_a] / 0x02) < 0x20) == (bytes1(storage_map_a[var_a])));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require((storage_map_a[var_a] / 0x02) > 0);
        require(!bytes1(storage_map_a[var_a]));
        require(((storage_map_a[var_a] / 0x02) < 0x20) == (bytes1(storage_map_a[var_a])));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!bytes1(storage_map_a[var_a]));
        require(0x01 == (bytes1(storage_map_a[var_a])));
        var_a = keccak256(var_a);
        require(0 < (storage_map_a[var_a] / 0x02));
        require(((var_g + (uint248(0x1f + (((0x20 + var_g) + 0) - var_g)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + (((0x20 + var_g) + 0) - var_g)))) < var_g));
        var_g = var_g + (uint248(0x1f + (((0x20 + var_g) + 0) - var_g)));
        var_a = keccak256(var_i);
        storage_map_a[var_a] = 0 | (uint96(storage_map_a[var_a]));
        var_a = address(arg1);
        require(arg3 > 0xffffffffffffffff);
        require(((var_g + (uint248(0x1f + (0 - var_g)))) > 0xffffffffffffffff) | ((var_g + (uint248(0x1f + (0 - var_g)))) < var_g));
        var_a = keccak256(var_i);
        storage_map_a[var_a] = (bytes1(0x01)) | (uint248(storage_map_a[var_a]));
        var_a = arg0;
        var_a = uint32(0 + (0x20 + (arg2)));
        require(!(storage_map_a[var_a] == 0), CustomError_2ef13105());
        require(!(storage_map_d[var_a] < 0x010000000000000000), CustomError_2ef13105());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        storage_map_d[var_a] = (storage_map_d[var_a]) + 0x01;
        require(!(storage_map_d[var_a] < (storage_map_d[var_a])), CustomError_2ef13105());
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(!(address(storage_map_b[var_a]) == msg.sender), CustomError_a8143fbc());
        require(!0x01, CustomError_ff9b022c());
    }
}