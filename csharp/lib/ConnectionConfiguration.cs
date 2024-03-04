/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

using System.Runtime.InteropServices;

namespace Glide;

public abstract class ConnectionConfiguration
{
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
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
        [MarshalAs(UnmanagedType.LPStr)]
        public string? client_name;

    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    internal struct NodeAddress
    {
        [MarshalAs(UnmanagedType.LPStr)]
        public string host;
        public uint port;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct ConnectionRetryStrategy
    {
        public uint number_of_retries;
        public uint factor;
        public uint exponent_base;

        public ConnectionRetryStrategy(uint number_of_retries, uint factor, uint exponent_base)
        {
            this.number_of_retries = number_of_retries;
            this.factor = factor;
            this.exponent_base = exponent_base;
        }
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    internal struct AuthenticationInfo
    {
        [MarshalAs(UnmanagedType.LPStr)]
        public string? username;
        [MarshalAs(UnmanagedType.LPStr)]
        public string? password;

        public AuthenticationInfo(string? username, string? password)
        {
            this.username = username;
            this.password = password;
        }
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

    private static readonly string DEFAULT_HOST = "localhost";
    private static readonly uint DEFAULT_PORT = 6379;

    public sealed class StandaloneClientConfiguration : ConnectionConfiguration
    {
        internal ConnectionRequest request;

        internal StandaloneClientConfiguration() { }

        internal ConnectionRequest Request() { return request; }
    }

    public sealed class ClusterClientConfiguration : ConnectionConfiguration
    {
        internal ConnectionRequest request;

        internal ClusterClientConfiguration() { }

        internal ConnectionRequest Request() { return request; }
    }

    public abstract class ClientConfigurationBuilder<T> : IDisposable
        where T : ClientConfigurationBuilder<T>, new()
    {
        internal ConnectionRequest config;

        private bool built = false;

        protected ClientConfigurationBuilder(bool cluster_mode)
        {
            config = new ConnectionRequest { cluster_mode = cluster_mode };
        }

        #region address
        private readonly List<NodeAddress> addresses = new();

        public (string? host, uint? port) Address
        {
            set
            {
                addresses.Add(new NodeAddress
                {
                    host = value.host ?? DEFAULT_HOST,
                    port = value.port ?? DEFAULT_PORT
                });
            }
        }

        public T WithAddress((string? host, uint? port) address)
        {
            Address = (address.host, address.port);
            return (T)this;
        }

        public T WithAddress((string host, uint port) address)
        {
            Address = (address.host, address.port);
            return (T)this;
        }

        public T WithAddress(string? host, uint? port)
        {
            Address = (host, port);
            return (T)this;
        }

        public T WithAddress(string host, uint port)
        {
            Address = (host, port);
            return (T)this;
        }

        public T WithAddress(string host)
        {
            Address = (host, DEFAULT_PORT);
            return (T)this;
        }

        public T WithAddress(uint port)
        {
            Address = (DEFAULT_HOST, port);
            return (T)this;
        }

        public class AddressBuilder
        {
            private readonly ClientConfigurationBuilder<T> owner;

            internal AddressBuilder(ClientConfigurationBuilder<T> owner)
            {
                this.owner = owner;
            }

            public static AddressBuilder operator +(AddressBuilder builder, (string? host, uint? port) address)
            {
                builder.owner.WithAddress(address);
                return builder;
            }

            public static AddressBuilder operator +(AddressBuilder builder, (string host, uint port) address)
            {
                builder.owner.WithAddress(address);
                return builder;
            }

            public static AddressBuilder operator +(AddressBuilder builder, string host)
            {
                builder.owner.WithAddress(host);
                return builder;
            }

            public static AddressBuilder operator +(AddressBuilder builder, uint port)
            {
                builder.owner.WithAddress(port);
                return builder;
            }
        }

        public AddressBuilder Addresses
        {
            get
            {
                return new AddressBuilder(this);
            }
            set { } // needed for +=
        }
        // TODO possible options : list and array
        #endregion

        public TlsMode TlsMode
        {
            set
            {
                config.tls_mode = value;
            }
        }

        public T WithTlsMode(TlsMode tls_mode)
        {
            TlsMode = tls_mode;
            return (T)this;
        }

        public T With(TlsMode tls_mode)
        {
            return WithTlsMode(tls_mode);
        }


        public uint RequestTimeout
        {
            set
            {
                config.request_timeout = value;
            }
        }

        public T WithRequestTimeout(uint request_timeout)
        {
            RequestTimeout = request_timeout;
            return (T)this;
        }


        public ReadFrom ReadFrom
        {
            set
            {
                config.read_from = value;
            }
        }

        public T WithReadFrom(ReadFrom read_from)
        {
            ReadFrom = read_from;
            return (T)this;
        }

        public T With(ReadFrom read_from)
        {
            return WithReadFrom(read_from);
        }


        public ConnectionRetryStrategy ConnectionRetryStrategy
        {
            set
            {
                config.connection_retry_strategy = value;
            }
        }

        public T WithConnectionRetryStrategy(ConnectionRetryStrategy connection_retry_strategy)
        {
            ConnectionRetryStrategy = connection_retry_strategy;
            return (T)this;
        }

        public T With(ConnectionRetryStrategy connection_retry_strategy)
        {
            return WithConnectionRetryStrategy(connection_retry_strategy);
        }

        public T WithConnectionRetryStrategy(uint number_of_retries, uint factor, uint exponent_base)
        {
            return WithConnectionRetryStrategy(new ConnectionRetryStrategy(number_of_retries, factor, exponent_base));
        }


        public (string? username, string? password) Authentication
        {
            set
            {
                config.authentication_info = new AuthenticationInfo
                    (
                        value.username,
                        value.password
                    );
            }
        }

        public T WithAuthentication(string? username, string? password)
        {
            Authentication = (username, password);
            return (T)this;
        }

        public T WithAuthentication((string? username, string? password) credentials)
        {
            return WithAuthentication(credentials.username, credentials.password);
        }


        public uint DataBaseId
        {
            set
            {
                config.database_id = value;
            }
        }

        public T WithDataBaseId(uint dataBaseId)
        {
            DataBaseId = dataBaseId;
            return (T)this;
        }


        public ProtocolVersion ProtocolVersion
        {
            set
            {
                config.protocol = value;
            }
        }

        public T WithProtocolVersion(ProtocolVersion protocol)
        {
            ProtocolVersion = protocol;
            return (T)this;
        }

        public T With(ProtocolVersion protocol)
        {
            ProtocolVersion = protocol;
            return (T)this;
        }


        public string? ClientName
        {
            set
            {
                config.client_name = value;
            }
        }

        public T WithClientName(string? clientName)
        {
            ClientName = clientName;
            return (T)this;
        }

        public void Dispose() => Clean();

        private void Clean()
        {
            if (built)
                Marshal.FreeHGlobal(config.addresses);
        }

        internal ConnectionRequest Build()
        {
            Clean(); // memory leak protection on rebuilding a config from the builder
            built = true;
            config.address_count = (uint)addresses.Count;
            var address_size = Marshal.SizeOf(typeof(NodeAddress));
            config.addresses = Marshal.AllocHGlobal(address_size * addresses.Count);
            for (int i = 0; i < addresses.Count; i++)
            {
                Marshal.StructureToPtr(addresses[i], config.addresses + i * address_size, false);
            }
            return config;
        }
    }

    public class StandaloneClientConfigurationBuilder : ClientConfigurationBuilder<StandaloneClientConfigurationBuilder>
    {
        public StandaloneClientConfigurationBuilder() : base(false) { }

        public new StandaloneClientConfiguration Build()
        {
            return new StandaloneClientConfiguration() { request = base.Build() };
        }
    }

    public class ClusterClientConfigurationBuilder : ClientConfigurationBuilder<StandaloneClientConfigurationBuilder>
    {
        public ClusterClientConfigurationBuilder() : base(true) { }

        public new ClusterClientConfiguration Build()
        {
            return new ClusterClientConfiguration() { request = base.Build() };
        }
    }
}
