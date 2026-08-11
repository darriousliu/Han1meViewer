package com.yenaly.han1meviewer.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

enum class StorageValueKind {
    Boolean,
    Int,
    Long,
    Float,
    Double,
    String,
    StringSet,
    ByteArray,
    UInt,
    ULong,
}

sealed interface StoredValue {
    val kind: StorageValueKind

    data class BooleanValue(val value: Boolean) : StoredValue {
        override val kind = StorageValueKind.Boolean
    }

    data class IntValue(val value: Int) : StoredValue {
        override val kind = StorageValueKind.Int
    }

    data class LongValue(val value: Long) : StoredValue {
        override val kind = StorageValueKind.Long
    }

    data class FloatValue(val value: Float) : StoredValue {
        override val kind = StorageValueKind.Float
    }

    data class DoubleValue(val value: Double) : StoredValue {
        override val kind = StorageValueKind.Double
    }

    data class StringValue(val value: String) : StoredValue {
        override val kind = StorageValueKind.String
    }

    class StringSetValue(value: Set<String>) : StoredValue {
        override val kind = StorageValueKind.StringSet
        private val storedValue = value.toSet()
        val value: Set<String>
            get() = storedValue.toSet()

        override fun equals(other: Any?): Boolean =
            other is StringSetValue && storedValue == other.storedValue

        override fun hashCode(): Int = storedValue.hashCode()

        override fun toString(): String = "StringSetValue(value=$storedValue)"
    }

    class ByteArrayValue(value: ByteArray) : StoredValue {
        override val kind = StorageValueKind.ByteArray
        private val storedValue: ByteArray = value.copyOf()
        val value: ByteArray
            get() = storedValue.copyOf()

        override fun equals(other: Any?): Boolean =
            other is ByteArrayValue && storedValue.contentEquals(other.storedValue)

        override fun hashCode(): Int = storedValue.contentHashCode()

        override fun toString(): String = "ByteArrayValue(size=${storedValue.size})"
    }

    data class UIntValue(val value: UInt) : StoredValue {
        override val kind = StorageValueKind.UInt
    }

    data class ULongValue(val value: ULong) : StoredValue {
        override val kind = StorageValueKind.ULong
    }
}

/** Converts domain values without exposing the underlying MMKV API. */
interface StorageCodec<T> {
    val id: String
    val storageKind: StorageValueKind

    /** `null` means that the persisted key must be removed. */
    fun encode(value: T): StoredValue?

    fun decode(value: StoredValue): T

    fun equivalent(first: T, second: T): Boolean = first == second

    fun copy(value: T): T = value
}

object StorageCodecs {
    val boolean: StorageCodec<Boolean> = codec(
        id = "boolean-v1",
        kind = StorageValueKind.Boolean,
        encode = { StoredValue.BooleanValue(it) },
        decode = { (it as StoredValue.BooleanValue).value },
    )

    val int: StorageCodec<Int> = codec(
        id = "int-v1",
        kind = StorageValueKind.Int,
        encode = { StoredValue.IntValue(it) },
        decode = { (it as StoredValue.IntValue).value },
    )

    fun intRange(minimum: Int, maximum: Int): StorageCodec<Int> {
        require(minimum <= maximum) { "Invalid integer range $minimum..$maximum" }
        return codec(
            id = "int-range-$minimum-$maximum-v1",
            kind = StorageValueKind.Int,
            encode = {
                require(it in minimum..maximum) { "Value $it is outside $minimum..$maximum" }
                StoredValue.IntValue(it)
            },
            decode = {
                (it as StoredValue.IntValue).value.also { value ->
                    require(value in minimum..maximum) {
                        "Stored value $value is outside $minimum..$maximum"
                    }
                }
            },
        )
    }

    val long: StorageCodec<Long> = codec(
        id = "long-v1",
        kind = StorageValueKind.Long,
        encode = { StoredValue.LongValue(it) },
        decode = { (it as StoredValue.LongValue).value },
    )

    val float: StorageCodec<Float> = codec(
        id = "float-v1",
        kind = StorageValueKind.Float,
        encode = { StoredValue.FloatValue(it) },
        decode = { (it as StoredValue.FloatValue).value },
    )

    val double: StorageCodec<Double> = codec(
        id = "double-v1",
        kind = StorageValueKind.Double,
        encode = { StoredValue.DoubleValue(it) },
        decode = { (it as StoredValue.DoubleValue).value },
    )

    val string: StorageCodec<String> = codec(
        id = "string-v1",
        kind = StorageValueKind.String,
        encode = { StoredValue.StringValue(it) },
        decode = { (it as StoredValue.StringValue).value },
    )

    val nullableString: StorageCodec<String?> = object : StorageCodec<String?> {
        override val id = "nullable-string-v1"
        override val storageKind = StorageValueKind.String

        override fun encode(value: String?): StoredValue? = value?.let { StoredValue.StringValue(it) }

        override fun decode(value: StoredValue): String = (value as StoredValue.StringValue).value
    }

    val stringSet: StorageCodec<Set<String>> = codec(
        id = "string-set-v1",
        kind = StorageValueKind.StringSet,
        encode = { StoredValue.StringSetValue(it.toSet()) },
        decode = { (it as StoredValue.StringSetValue).value },
    ).withCopy { it.toSet() }

    val byteArray: StorageCodec<ByteArray> = object : StorageCodec<ByteArray> {
        override val id = "byte-array-v1"
        override val storageKind = StorageValueKind.ByteArray

        override fun encode(value: ByteArray): StoredValue = StoredValue.ByteArrayValue(value)

        override fun decode(value: StoredValue): ByteArray {
            require(value.kind == storageKind) {
                "Codec $id expected $storageKind but received ${value.kind}"
            }
            return (value as StoredValue.ByteArrayValue).value.copyOf()
        }

        override fun equivalent(first: ByteArray, second: ByteArray): Boolean =
            first.contentEquals(second)

        override fun copy(value: ByteArray): ByteArray = value.copyOf()
    }

    val uint: StorageCodec<UInt> = codec(
        id = "uint-v1",
        kind = StorageValueKind.UInt,
        encode = { StoredValue.UIntValue(it) },
        decode = { (it as StoredValue.UIntValue).value },
    )

    val ulong: StorageCodec<ULong> = codec(
        id = "ulong-v1",
        kind = StorageValueKind.ULong,
        encode = { StoredValue.ULongValue(it) },
        decode = { (it as StoredValue.ULongValue).value },
    )

    fun <T> json(
        serializer: KSerializer<T>,
        typeId: String,
        json: Json = defaultJson,
    ): StorageCodec<T> = codec(
        id = "json:$typeId:v1",
        kind = StorageValueKind.String,
        encode = { StoredValue.StringValue(json.encodeToString(serializer, it)) },
        decode = { json.decodeFromString(serializer, (it as StoredValue.StringValue).value) },
    )

    private val defaultJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private fun <T> codec(
        id: String,
        kind: StorageValueKind,
        encode: (T) -> StoredValue,
        decode: (StoredValue) -> T,
    ): StorageCodec<T> = object : StorageCodec<T> {
        override val id = id
        override val storageKind = kind

        override fun encode(value: T): StoredValue = encode(value)

        override fun decode(value: StoredValue): T {
            require(value.kind == storageKind) {
                "Codec $id expected $storageKind but received ${value.kind}"
            }
            return decode(value)
        }
    }

    private fun <T> StorageCodec<T>.withCopy(copy: (T) -> T): StorageCodec<T> {
        val delegate = this
        return object : StorageCodec<T> by delegate {
            override fun copy(value: T): T = copy(value)
        }
    }
}
