/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

using System.Runtime.InteropServices;

namespace Glide;

public class ConnectionConfiguration
{
    [StructLayout(LayoutKind.Sequential)]
    internal struct ConnectionRequest
    {
        public uint address_count;
        public IntPtr addresses; // ** NodeAddress - array pointer
        public TlsMode tls_mode;
        public bool cluster_mode;
        public uint request_timeout;
        public ReadFrom read_from;
        public ConnectionRetryStrategy connection_retry_strategy;
        public AuthenticationInfo authentication_info;
        public uint database_id;
        public ProtocolVersion protocol;
        public IntPtr client_name; // string
    }

    public struct NodeAddress
    {
        public IntPtr host; // string
        public uint port;
    }

    public struct ConnectionRetryStrategy
    {
        public uint number_of_retries;
        public uint factor;
        public uint exponent_base;
    }

    public struct AuthenticationInfo
    {
        public IntPtr username; // string
        public IntPtr password; // string
    }

    public enum TlsMode : uint
    {
        NoTls = 0,
        SecureTls = 1,
        InsecureTls = 2,
    }

    public enum ReadFrom : uint
    {
        Primary = 0,
        PreferReplica = 1,
        LowestLatency = 2,
        AZAffinity = 3,
    }

    public enum ProtocolVersion : uint
    {
        RESP3 = 0,
        RESP2 = 1,
    }
}
