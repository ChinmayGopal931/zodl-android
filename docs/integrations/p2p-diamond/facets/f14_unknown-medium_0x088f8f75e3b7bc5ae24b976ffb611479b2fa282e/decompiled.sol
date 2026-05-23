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
    mapping(bytes32 => bytes32) storage_map_u;
    mapping(bytes32 => bytes32) storage_map_h;
    mapping(bytes32 => bytes32) storage_map_a;
    mapping(bytes32 => bytes32) storage_map_i;
    mapping(bytes32 => bytes32) storage_map_p;
    mapping(bytes32 => bytes32) storage_map_r;
    mapping(bytes32 => bytes32) storage_map_s;
    mapping(bytes32 => bytes32) storage_map_c;
    mapping(bytes32 => bytes32) storage_map_f;
    mapping(bytes32 => bytes32) storage_map_j;
    mapping(bytes32 => bytes32) storage_map_m;
    mapping(bytes32 => bytes32) storage_map_g;
    mapping(bytes32 => bytes32) storage_map_e;
    mapping(bytes32 => bytes32) storage_map_n;
    mapping(bytes32 => bytes32) storage_map_t;
    mapping(bytes32 => bytes32) storage_map_b;
    bytes32 store_l;
    mapping(bytes32 => bytes32) storage_map_q;
    mapping(bytes32 => bytes32) storage_map_o;
    mapping(bytes32 => bytes32) storage_map_d;
    mapping(bytes32 => bytes32) storage_map_k;
    
    error NotAuthorized();
    
    /// @custom:selector    0x54808819
    /// @custom:signature   Unresolved_54808819(uint256 arg0, uint256 arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_54808819(uint256 arg0, uint256 arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0x0646842c
    /// @custom:signature   Unresolved_0646842c(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_0646842c(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x0140) > 0xffffffffffffffff) | ((var_a + 0x0140) < var_a));
        uint256 var_a = var_a + 0x0140;
        uint256 var_b = arg0;
        require(((var_a + 0x0140) > 0xffffffffffffffff) | ((var_a + 0x0140) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0x0140;
        return abi.encodePacked(var_a.length, var_ar, var_as, var_at, var_au, var_av, var_aw, var_ax, var_ay, var_az);
    }
    
    /// @custom:selector    0x331de1a9
    /// @custom:signature   Unresolved_331de1a9(uint256 arg0, uint256 arg1, uint256 arg2) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg4 ["uint256", "bytes32", "int256"]
    /// @param              arg5 ["uint256", "bytes32", "int256"]
    function Unresolved_331de1a9(uint256 arg0, uint256 arg1, uint256 arg2) public view {
        require(msg.value);
        require(arg0 - arg0);
        require((msg.data.length - 0x24) < 0xc0);
        address var_a = address(msg.sender);
        require(!bytes1(storage_map_k[var_a]));
        require(!bytes1(storage_map_k[var_a]));
        require(arg4 - arg4);
        require(arg5 - arg5);
        require(arg4 > (arg4 + arg5), CustomError_ea8e4eb5());
        require(!(address(msg.sender) == (address(store_l))), CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x57626f5f
    /// @custom:signature   Unresolved_57626f5f(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_57626f5f(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x0100) > 0xffffffffffffffff) | ((var_a + 0x0100) < var_a));
        uint256 var_a = var_a + 0x0100;
        uint256 var_b = arg0;
        require(((var_a + 0x0100) > 0xffffffffffffffff) | ((var_a + 0x0100) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0x0100;
        return abi.encodePacked(var_a.length, var_aj, var_ak, var_al, var_am, var_an, var_ao, uint16(var_ap));
    }
    
    /// @custom:selector    0xb03e424e
    /// @custom:signature   Unresolved_b03e424e(uint256 arg0, uint256 arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_b03e424e(uint256 arg0, uint256 arg1) public view {
        require(msg.value);
        require(arg0 - arg0);
        require((msg.data.length - 0x24) < 0xc0);
        address var_a = address(msg.sender);
        require(!bytes1(storage_map_k[var_a]));
        require(!bytes1(storage_map_k[var_a]));
        require(((var_c + 0xc0) > 0xffffffffffffffff) | ((var_c + 0xc0) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(arg1 - arg1);
        require(!(address(msg.sender) == (address(store_l))), CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x4c76390f
    /// @custom:signature   Unresolved_4c76390f(uint256 arg0) public view returns (bytes memory)
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_4c76390f(uint256 arg0) public view returns (bytes memory) {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0xc0) > 0xffffffffffffffff) | ((var_a + 0xc0) < var_a));
        uint256 var_a = var_a + 0xc0;
        uint256 var_b = arg0;
        require(((var_a + 0xc0) > 0xffffffffffffffff) | ((var_a + 0xc0) < var_a));
        var_b = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = var_a + 0xc0;
        return abi.encodePacked(var_a.length, var_ab, var_ac, var_ad, var_ae, var_af);
    }
    
    /// @custom:selector    0xa393fdc6
    /// @custom:signature   Unresolved_a393fdc6(uint256 arg0, uint256 arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_a393fdc6(uint256 arg0, uint256 arg1) public view {
        require(msg.value);
        require(arg0 - arg0);
        require((msg.data.length - 0x24) < 0x0140);
        address var_a = address(msg.sender);
        require(!bytes1(storage_map_k[var_a]));
        require(!bytes1(storage_map_k[var_a]));
        var_a = arg0;
        require(((var_c + 0x0140) > 0xffffffffffffffff) | ((var_c + 0x0140) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        var_a = arg0;
        require(arg1 - arg1);
        require(!(address(msg.sender) == (address(store_l))), CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x56c5a2fb
    /// @custom:signature   Unresolved_56c5a2fb(uint256 arg0, uint256 arg1) public view
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_56c5a2fb(uint256 arg0, uint256 arg1) public view {
        require(msg.value);
        require(arg0 - arg0);
        require((msg.data.length - 0x24) < 0x0100);
        address var_a = address(msg.sender);
        require(!bytes1(storage_map_k[var_a]));
        require(!bytes1(storage_map_k[var_a]));
        require(((var_c + 0x0100) > 0xffffffffffffffff) | ((var_c + 0x0100) < var_c));
        var_a = 0x4e487b7100000000000000000000000000000000000000000000000000000000;
        require(arg1 - arg1);
        require(!(address(msg.sender) == (address(store_l))), CustomError_ea8e4eb5());
    }
    
    /// @custom:selector    0x5b5cc9cf
    /// @custom:signature   Unresolved_5b5cc9cf(uint256 arg0, uint256 arg1) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    /// @param              arg1 ["uint256", "bytes32", "int256"]
    function Unresolved_5b5cc9cf(uint256 arg0, uint256 arg1) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(arg1 - arg1);
    }
    
    /// @custom:selector    0xd3c22fe7
    /// @custom:signature   Unresolved_d3c22fe7(uint256 arg0) public pure
    /// @param              arg0 ["uint256", "bytes32", "int256"]
    function Unresolved_d3c22fe7(uint256 arg0) public pure {
        require(msg.value);
        require(arg0 - arg0);
        require(((var_a + 0x0140) > 0xffffffffffffffff) | ((var_a + 0x0140) < var_a));
    }
}