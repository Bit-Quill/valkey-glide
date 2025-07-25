﻿// Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0

using System;
using System.Buffers;
using System.Collections.Generic;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Valkey.Glide;

/// <summary>
/// Represents a key that can be stored.
/// </summary>
public readonly struct ValkeyKey : IEquatable<ValkeyKey>
{
    internal ValkeyKey(byte[]? keyPrefix, object? keyValue)
    {
        KeyPrefix = keyPrefix?.Length == 0 ? null : keyPrefix;
        KeyValue = keyValue;
    }

    /// <summary>
    /// Creates a <see cref="ValkeyKey"/> from a string.
    /// </summary>
    public ValkeyKey(string? key) : this(null, key) { }

    internal ValkeyKey AsPrefix() => new ValkeyKey((byte[]?)this, null);

    internal bool IsNull => KeyPrefix == null && KeyValue == null;

    internal static ValkeyKey Null { get; } = new ValkeyKey(null, null);

    internal bool IsEmpty
    {
        get
        {
            if (KeyPrefix != null) return false;
            if (KeyValue == null) return true;
            if (KeyValue is string s) return s.Length == 0;
            return ((byte[])KeyValue).Length == 0;
        }
    }

    internal byte[]? KeyPrefix { get; }
    internal object? KeyValue { get; }

    /// <summary>
    /// Indicate whether two keys are not equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator !=(ValkeyKey x, ValkeyKey y) => !x.EqualsImpl(in y);

    /// <summary>
    /// Indicate whether two keys are not equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator !=(string x, ValkeyKey y) => !y.EqualsImpl(new ValkeyKey(x));

    /// <summary>
    /// Indicate whether two keys are not equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator !=(byte[] x, ValkeyKey y) => !y.EqualsImpl(new ValkeyKey(null, x));

    /// <summary>
    /// Indicate whether two keys are not equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator !=(ValkeyKey x, string y) => !x.EqualsImpl(new ValkeyKey(y));

    /// <summary>
    /// Indicate whether two keys are not equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator !=(ValkeyKey x, byte[] y) => !x.EqualsImpl(new ValkeyKey(null, y));

    /// <summary>
    /// Indicate whether two keys are equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator ==(ValkeyKey x, ValkeyKey y) => x.EqualsImpl(in y);

    /// <summary>
    /// Indicate whether two keys are equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator ==(string x, ValkeyKey y) => y.EqualsImpl(new ValkeyKey(x));

    /// <summary>
    /// Indicate whether two keys are equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator ==(byte[] x, ValkeyKey y) => y.EqualsImpl(new ValkeyKey(null, x));

    /// <summary>
    /// Indicate whether two keys are equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator ==(ValkeyKey x, string y) => x.EqualsImpl(new ValkeyKey(y));

    /// <summary>
    /// Indicate whether two keys are equal.
    /// </summary>
    /// <param name="x">The first ValkeyChannel to compare.</param>
    /// <param name="y">The second ValkeyChannel to compare.</param>
    public static bool operator ==(ValkeyKey x, byte[] y) => x.EqualsImpl(new ValkeyKey(null, y));

    /// <summary>
    /// See <see cref="object.Equals(object?)"/>.
    /// </summary>
    /// <param name="obj">The <see cref="ValkeyKey"/> to compare to.</param>
    public override bool Equals(object? obj) => obj switch
    {
        null => IsNull,
        ValkeyKey key => EqualsImpl(in key),
        string s => EqualsImpl(new ValkeyKey(s)),
        byte[] b => EqualsImpl(new ValkeyKey(null, b)),
        _ => false,
    };

    /// <summary>
    /// Indicate whether two keys are equal.
    /// </summary>
    /// <param name="other">The <see cref="ValkeyKey"/> to compare to.</param>
    public bool Equals(ValkeyKey other) => EqualsImpl(in other);

    private bool EqualsImpl(in ValkeyKey other)
    {
        if (IsNull)
        {
            return other.IsNull;
        }
        else if (other.IsNull)
        {
            return false;
        }

        // if there's no prefix, we might be able to do a simple compare
        if (ValkeyValue.Equals(KeyPrefix, other.KeyPrefix))
        {
            if ((object?)KeyValue == (object?)other.KeyValue) return true; // ref equal

            if (KeyValue is string keyString1 && other.KeyValue is string keyString2) return keyString1 == keyString2;
            if (KeyValue is byte[] keyBytes1 && other.KeyValue is byte[] keyBytes2) return ValkeyValue.Equals(keyBytes1, keyBytes2);
        }

        int len = TotalLength();
        if (len != other.TotalLength())
        {
            return false; // different length; can't be equal
        }
        if (len == 0)
        {
            return true; // both empty
        }
        if (len <= 128)
        {
            return CopyCompare(in this, in other, len, stackalloc byte[len * 2]);
        }
        else
        {
            byte[] arr = ArrayPool<byte>.Shared.Rent(len * 2);
            var result = CopyCompare(in this, in other, len, arr);
            ArrayPool<byte>.Shared.Return(arr);
            return result;
        }

        static bool CopyCompare(in ValkeyKey x, in ValkeyKey y, int length, Span<byte> span)
        {
            Span<byte> span1 = span.Slice(0, length), span2 = span.Slice(length, length);
            var written = x.CopyTo(span1);
            Debug.Assert(written == length, "length error (1)");
            written = y.CopyTo(span2);
            Debug.Assert(written == length, "length error (2)");
            return span1.SequenceEqual(span2);
        }
    }

    /// <inheritdoc/>
    public override int GetHashCode()
    {
        // note that we need need eaulity-like behavior, regardless of whether the
        // parts look like bytes or strings, and with/without prefix

        // the simplest way to do this is to use the CopyTo version, which normalizes that
        if (IsNull) return -1;
        if (TryGetSimpleBuffer(out var buffer)) return ValkeyValue.GetHashCode(buffer);
        var len = TotalLength();
        if (len == 0) return 0;

        if (len <= 256)
        {
            Span<byte> span = stackalloc byte[len];
            var written = CopyTo(span);
            Debug.Assert(written == len);
            return ValkeyValue.GetHashCode(span);
        }
        else
        {
            var arr = ArrayPool<byte>.Shared.Rent(len);
            var span = new Span<byte>(arr, 0, len);
            var written = CopyTo(span);
            Debug.Assert(written == len);
            var result = ValkeyValue.GetHashCode(span);
            ArrayPool<byte>.Shared.Return(arr);
            return result;
        }
    }

    /// <summary>
    /// Obtains a string representation of the key.
    /// </summary>
    public override string ToString() => ((string?)this) ?? "(null)";

    internal ValkeyValue AsValkeyValue()
    {
        if (KeyPrefix == null && KeyValue is string keyString) return keyString;
        return (byte[]?)this;
    }

    internal void AssertNotNull()
    {
        if (IsNull) throw new ArgumentException("A null key is not valid in this context");
    }

    /// <summary>
    /// Create a <see cref="ValkeyKey"/> from a <see cref="string"/>.
    /// </summary>
    /// <param name="key">The string to get a key from.</param>
    public static implicit operator ValkeyKey(string? key)
    {
        if (key == null) return default;
        return new ValkeyKey(null, key);
    }

    /// <summary>
    /// Create a <see cref="ValkeyKey"/> from a <see cref="T:byte[]"/>.
    /// </summary>
    /// <param name="key">The byte array to get a key from.</param>
    public static implicit operator ValkeyKey(byte[]? key)
    {
        if (key == null) return default;
        return new ValkeyKey(null, key);
    }

    /// <summary>
    /// Obtain the <see cref="ValkeyKey"/> as a <see cref="T:byte[]"/>.
    /// </summary>
    /// <param name="key">The key to get a byte array for.</param>
    public static implicit operator byte[]?(ValkeyKey key)
    {
        if (key.IsNull) return null;
        if (key.TryGetSimpleBuffer(out var arr)) return arr;

        var len = key.TotalLength();
        if (len == 0) return Array.Empty<byte>();
        arr = new byte[len];
        var written = key.CopyTo(arr);
        Debug.Assert(written == len, "length/copyto error");
        return arr;
    }

    /// <summary>
    /// Obtain the key as a <see cref="string"/>.
    /// </summary>
    /// <param name="key">The key to get a string for.</param>
    public static implicit operator string?(ValkeyKey key)
    {
        if (key.KeyPrefix is null)
        {
            return key.KeyValue switch
            {
                null => null,
                string s => s,
                object o => Get((byte[])o, -1),
            };
        }

        var len = key.TotalLength();
        var arr = ArrayPool<byte>.Shared.Rent(len);
        var written = key.CopyTo(arr);
        Debug.Assert(written == len, "length error");
        var result = Get(arr, len);
        ArrayPool<byte>.Shared.Return(arr);
        return result;

        static string? Get(byte[] arr, int length)
        {
            if (length == -1) length = arr.Length;
            if (length == 0) return "";
            try
            {
                return Encoding.UTF8.GetString(arr, 0, length);
            }
            catch
            {
                return BitConverter.ToString(arr, 0, length);
            }
        }
    }

    /// <summary>
    /// Concatenate two keys.
    /// </summary>
    /// <param name="x">The first <see cref="ValkeyKey"/> to add.</param>
    /// <param name="y">The second <see cref="ValkeyKey"/> to add.</param>
    [Obsolete("Prefer WithPrefix")]
    public static ValkeyKey operator +(ValkeyKey x, ValkeyKey y) =>
        new ValkeyKey(ConcatenateBytes(x.KeyPrefix, x.KeyValue, y.KeyPrefix), y.KeyValue);

    internal static ValkeyKey WithPrefix(byte[]? prefix, ValkeyKey value)
    {
        if (prefix == null || prefix.Length == 0) return value;
        if (value.KeyPrefix == null) return new ValkeyKey(prefix, value.KeyValue);
        if (value.KeyValue == null) return new ValkeyKey(prefix, value.KeyPrefix);

        // two prefixes; darn
        byte[] copy = new byte[prefix.Length + value.KeyPrefix.Length];
        Buffer.BlockCopy(prefix, 0, copy, 0, prefix.Length);
        Buffer.BlockCopy(value.KeyPrefix, 0, copy, prefix.Length, value.KeyPrefix.Length);
        return new ValkeyKey(copy, value.KeyValue);
    }

    internal static byte[]? ConcatenateBytes(byte[]? a, object? b, byte[]? c)
    {
        if ((a == null || a.Length == 0) && (c == null || c.Length == 0))
        {
            if (b == null) return null;
            if (b is string s) return Encoding.UTF8.GetBytes(s);
            return (byte[])b;
        }

        int aLen = a?.Length ?? 0,
            bLen = b == null ? 0 : (b is string bString
            ? Encoding.UTF8.GetByteCount(bString)
            : ((byte[])b).Length),
            cLen = c?.Length ?? 0;

        var result = new byte[aLen + bLen + cLen];
        if (aLen != 0) Buffer.BlockCopy(a!, 0, result, 0, aLen);
        if (bLen != 0)
        {
            if (b is string s)
            {
                Encoding.UTF8.GetBytes(s, 0, s.Length, result, aLen);
            }
            else
            {
                Buffer.BlockCopy((byte[])b!, 0, result, aLen, bLen);
            }
        }
        if (cLen != 0) Buffer.BlockCopy(c!, 0, result, aLen + bLen, cLen);
        return result;
    }

    /// <summary>
    /// <para>Prepends p to this ValkeyKey, returning a new ValkeyKey.</para>
    /// <para>
    /// Avoids some allocations if possible, repeated Prepend/Appends make it less possible.
    /// </para>
    /// </summary>
    /// <param name="prefix">The prefix to prepend.</param>
    public ValkeyKey Prepend(ValkeyKey prefix) => WithPrefix(prefix, this);

    /// <summary>
    /// <para>Appends p to this ValkeyKey, returning a new ValkeyKey.</para>
    /// <para>
    /// Avoids some allocations if possible, repeated Prepend/Appends make it less possible.
    /// </para>
    /// </summary>
    /// <param name="suffix">The suffix to append.</param>
    public ValkeyKey Append(ValkeyKey suffix) => WithPrefix(this, suffix);

    internal bool TryGetSimpleBuffer([NotNullWhen(true)] out byte[]? arr)
    {
        arr = KeyValue is null ? Array.Empty<byte>() : KeyValue as byte[];
        return arr is not null && (KeyPrefix is null || KeyPrefix.Length == 0);
    }

    internal int TotalLength() =>
        (KeyPrefix is null ? 0 : KeyPrefix.Length) + KeyValue switch
        {
            null => 0,
            string s => Encoding.UTF8.GetByteCount(s),
            _ => ((byte[])KeyValue).Length,
        };

    internal int CopyTo(Span<byte> destination)
    {
        int written = 0;
        if (KeyPrefix is not null && KeyPrefix.Length != 0)
        {
            KeyPrefix.CopyTo(destination);
            written += KeyPrefix.Length;
            destination = destination.Slice(KeyPrefix.Length);
        }
        switch (KeyValue)
        {
            case null:
                break; // nothing to do
            case string s:
                if (s.Length != 0)
                {
#if NETCOREAPP
                    written += Encoding.UTF8.GetBytes(s, destination);
#else
                    unsafe
                    {
                        fixed (byte* bPtr = destination)
                        {
                            fixed (char* cPtr = s)
                            {
                                written += Encoding.UTF8.GetBytes(cPtr, s.Length, bPtr, destination.Length);
                            }
                        }
                    }
#endif
                }
                break;
            default:
                var arr = (byte[])KeyValue;
                arr.CopyTo(destination);
                written += arr.Length;
                break;
        }
        return written;
    }
}
