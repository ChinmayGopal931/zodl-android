package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.AbiAddress
import xyz.justzappit.evm.abi.AbiArg
import xyz.justzappit.evm.abi.AbiEncoder
import xyz.justzappit.evm.abi.AbiInt
import xyz.justzappit.evm.abi.AbiString
import xyz.justzappit.evm.abi.AbiUint
import xyz.justzappit.evm.abi.AbiUint8
import xyz.justzappit.evm.types.Address
import java.math.BigInteger

enum class OrderType(val onChain: Int) {
    BUY(0),
    SELL(1),
    PAY(2),
}

data class PlaceOrderArgs(
    val relayPubKeyEthCrypto: String,
    val usdcAmount: BigInteger,
    val recipientAddress: Address,
    val orderType: OrderType,
    val currency: CurrencyCode,
    val circleId: BigInteger,
    val fiatAmountLimit: BigInteger = BigInteger.ZERO,
    val preferredPaymentChannelConfigId: BigInteger = BigInteger.ZERO,
)

object DiamondCalls {
    fun placeOrderCalldata(args: PlaceOrderArgs): ByteArray {
        val isBuy = args.orderType == OrderType.BUY
        val pubKey = if (isBuy) "" else args.relayPubKeyEthCrypto
        val userPubKey = if (isBuy) args.relayPubKeyEthCrypto else ""

        val abiArgs = listOf<AbiArg>(
            AbiString(pubKey),
            AbiUint(args.usdcAmount),
            AbiAddress(args.recipientAddress),
            AbiUint8(args.orderType.onChain),
            AbiString(""),
            AbiString(userPubKey),
            AbiEncoder.bytes32String(args.currency.code),
            AbiUint(args.preferredPaymentChannelConfigId),
            AbiUint(args.circleId),
            AbiUint(args.fiatAmountLimit),
        )
        return AbiEncoder.encodeFunctionCall(
            "placeOrder(string,uint256,address,uint8,string,string,bytes32,uint256,uint256,uint256)",
            abiArgs,
        )
    }

    fun setSellOrderUpiCalldata(
        orderId: BigInteger,
        encryptedUpiHex: String,
        updatedAmount: BigInteger = BigInteger.ZERO,
    ): ByteArray = AbiEncoder.encodeFunctionCall(
        "setSellOrderUpi(uint256,string,uint256)",
        listOf(
            AbiUint(orderId),
            AbiString(encryptedUpiHex),
            AbiUint(updatedAmount),
        ),
    )

    fun getOrdersByIdCalldata(orderId: BigInteger): ByteArray =
        AbiEncoder.encodeFunctionCall(
            "getOrdersById(uint256)",
            listOf(AbiUint(orderId)),
        )

    fun getPriceConfigCalldata(currency: CurrencyCode): ByteArray =
        AbiEncoder.encodeFunctionCall(
            "getPriceConfig(bytes32)",
            listOf(AbiEncoder.bytes32String(currency.code)),
        )

    fun getAssignableMerchantsFromCircleCalldata(
        circleId: BigInteger,
        assignUpTo: BigInteger,
        currency: CurrencyCode,
        user: Address,
        usdtAmount: BigInteger,
        fiatAmount: BigInteger,
        orderType: OrderType,
        preferredPCConfigId: BigInteger = BigInteger.ZERO,
    ): ByteArray = AbiEncoder.encodeFunctionCall(
        "getAssignableMerchantsFromCircle(uint256,uint256,bytes32,address,uint256,uint256,int256,uint256)",
        listOf(
            AbiUint(circleId),
            AbiUint(assignUpTo),
            AbiEncoder.bytes32String(currency.code),
            AbiAddress(user),
            AbiUint(usdtAmount),
            AbiUint(fiatAmount),
            AbiInt(BigInteger.valueOf(orderType.onChain.toLong())),
            AbiUint(preferredPCConfigId),
        ),
    )
}
