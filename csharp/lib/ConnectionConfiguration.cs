/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

using System.Runtime.InteropServices;

namespace Glide;

public abstract class ConnectionConfiguration
{

    #region Structs and Enums definitions
    /// <summary>
    /// A mirror of <c>ConnectionRequest</c> from <c>connection_request.proto</c>.
    /// </summary>
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    internal struct ConnectionRequest
    {
        public nuint address_count;
        public IntPtr addresses; // ** NodeAddress - array pointer
        public TlsMode tls_mode;
        public bool cluster_mode;
        public uint request_timeout;
        public ReadFrom read_from;
        public RetryStrategy connection_retry_strategy;
        public AuthenticationInfo authentication_info;
        public uint database_id;
        public Protocol protocol;
        [MarshalAs(UnmanagedType.LPStr)]
        public string? client_name;
    }

    /// <summary>
    /// Represents the address and port of a node in the cluster.
    /// </summary>
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    internal struct NodeAddress
    {
        [MarshalAs(UnmanagedType.LPStr)]
        public string Host;
        public ushort Port;
    }

    /// <summary>
    /// Represents the strategy used to determine how and when to reconnect, in case of connection
    /// failures. The time between attempts grows exponentially, to the formula <c>rand(0 ... factor *
    /// (exponentBase ^ N))</c>, where <c>N</c> is the number of failed attempts.
    /// <para>
    /// Once the maximum value is reached, that will remain the time between retry attempts until a
    /// reconnect attempt is successful. The client will attempt to reconnect indefinitely.
    /// </para>
    /// </summary>
    [StructLayout(LayoutKind.Sequential)]
    public struct RetryStrategy
    {
        /// <summary>
        /// Number of retry attempts that the client should perform when disconnected from the server,
        /// where the time between retries increases. Once the retries have reached the maximum value, the
        /// time between retries will remain constant until a reconnect attempt is successful.
        /// </summary>
        public uint Number_of_retries;
        /// <summary>
        /// The multiplier that will be applied to the waiting time between each retry.
        /// </summary>
        public uint Factor;
        /// <summary>
        /// The exponent base configured for the strategy.
        /// </summary>
        public uint Exponent_base;

        public RetryStrategy(uint number_of_retries, uint factor, uint exponent_base)
        {
            Number_of_retries = number_of_retries;
            Factor = factor;
            Exponent_base = exponent_base;
        }
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    internal struct AuthenticationInfo
    {
        [MarshalAs(UnmanagedType.LPStr)]
        public string? Username;
        [MarshalAs(UnmanagedType.LPStr)]
        public string? Password;

        public AuthenticationInfo(string? username, string? password)
        {
            Username = username;
            Password = password;
        }
    }

    // TODO doc
    public enum TlsMode : uint
    {
        NoTls = 0,
        SecureTls = 1,
        InsecureTls = 2,
    }

    /// <summary>
    /// Represents the client's read from strategy.
    /// </summary>
    public enum ReadFrom : uint
    {
        /// <summary>
        /// Always get from primary, in order to get the freshest data.
        /// </summary>
        Primary = 0,
        /// <summary>
        /// Spread the requests between all replicas in a round-robin manner. If no replica is available, route the requests to the primary.
        /// </summary>
        PreferReplica = 1,
        // TODO: doc or comment out/remove
        LowestLatency = 2,
        AZAffinity = 3,
    }

    /// <summary>
    /// Represents the communication protocol with the server.
    /// </summary>
    public enum Protocol : uint
    {
        /// <summary>
        /// Use RESP3 to communicate with the server nodes.
        /// </summary>
        RESP3 = 0,
        /// <summary>
        /// Use RESP2 to communicate with the server nodes.
        /// </summary>
        RESP2 = 1,
    }
    #endregion

    private static readonly string DEFAULT_HOST = "localhost";
    private static readonly ushort DEFAULT_PORT = 6379;

    /// <summary>
    /// Configuration for a standalone client. Use <see cref="StandaloneClientConfigurationBuilder"/> to create an instance.
    /// </summary>
    public sealed class StandaloneClientConfiguration : ConnectionConfiguration
    {
        internal ConnectionRequest request;

        internal StandaloneClientConfiguration() { }

        internal ConnectionRequest Request() { return request; }
    }

    /// <summary>
    /// Configuration for a cluster client. Use <see cref="ClusterClientConfigurationBuilder"/> to create an instance.
    /// </summary>
    public sealed class ClusterClientConfiguration : ConnectionConfiguration
    {
        internal ConnectionRequest request;

        internal ClusterClientConfiguration() { }

        internal ConnectionRequest Request() { return request; }
    }

    /// <summary>
    /// Builder for configuration of common parameters for standalone and cluster client.
    /// </summary>
    /// <typeparam name="T">Derived builder class</typeparam>
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

        /// <inheritdoc cref="Addresses"/>
        /// <b>Add</b> a new address to the list.<br />
        /// See also <seealso cref="Addresses"/>.
        // <summary>
        // </summary>

        protected (string? host, ushort? port) Address
        {
            set
            {
                addresses.Add(new NodeAddress
                {
                    Host = value.host ?? DEFAULT_HOST,
                    Port = value.port ?? DEFAULT_PORT
                });
            }
        }

        /// <inheritdoc cref="Address"/>
        public T WithAddress((string? host, ushort? port) address)
        {
            Address = (address.host, address.port);
            return (T)this;
        }

        /// <inheritdoc cref="Address"/>
        public T WithAddress((string host, ushort port) address)
        {
            Address = (address.host, address.port);
            return (T)this;
        }

        /// <inheritdoc cref="Address"/>
        public T WithAddress(string? host, ushort? port)
        {
            Address = (host, port);
            return (T)this;
        }

        /// <inheritdoc cref="Address"/>
        public T WithAddress(string host, ushort port)
        {
            Address = (host, port);
            return (T)this;
        }

        /// <summary>
        /// <b>Add</b> a new address to the list with default port.
        /// </summary>
        public T WithAddress(string host)
        {
            Address = (host, DEFAULT_PORT);
            return (T)this;
        }

        /// <summary>
        /// <b>Add</b> a new address to the list with default host.
        /// </summary>
        public T WithAddress(ushort port)
        {
            Address = (DEFAULT_HOST, port);
            return (T)this;
        }

        /// <summary>
        /// Syntax sugar helper class for adding addresses.
        /// </summary>
        public sealed class AddressBuilder
        {
            private readonly ClientConfigurationBuilder<T> owner;

            internal AddressBuilder(ClientConfigurationBuilder<T> owner)
            {
                this.owner = owner;
            }

            /// <inheritdoc cref="Address"/>
            public static AddressBuilder operator +(AddressBuilder builder, (string? host, ushort? port) address)
            {
                builder.owner.WithAddress(address);
                return builder;
            }

            /// <inheritdoc cref="Address"/>
            public static AddressBuilder operator +(AddressBuilder builder, (string host, ushort port) address)
            {
                builder.owner.WithAddress(address);
                return builder;
            }

            /// <inheritdoc cref="WithAddress(string)"/>
            public static AddressBuilder operator +(AddressBuilder builder, string host)
            {
                builder.owner.WithAddress(host);
                return builder;
            }

            /// <inheritdoc cref="WithAddress(ushort)"/>
            public static AddressBuilder operator +(AddressBuilder builder, ushort port)
            {
                builder.owner.WithAddress(port);
                return builder;
            }
        }

        /// <summary>
        /// DNS Addresses and ports of known nodes in the cluster. If the server is in cluster mode the
        /// list can be partial, as the client will attempt to map out the cluster and find all nodes. If
        /// the server is in standalone mode, only nodes whose addresses were provided will be used by the
        /// client.
        /// <para />
        /// For example: <code>
        /// [
        ///   ("sample-address-0001.use1.cache.amazonaws.com", 6378),
        ///   ("sample-address-0002.use2.cache.amazonaws.com"),
        ///   ("sample-address-0002.use3.cache.amazonaws.com", 6380)
        /// ]</code>
        /// </summary>
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
        #region TLS
        /// <summary>
        /// Configure whether communication with the server should use Transport Level Security.<br />
        /// Should match the TLS configuration of the server/cluster, otherwise the connection attempt will fail.
        /// </summary>
        public TlsMode TlsMode
        {
            set
            {
                config.tls_mode = value;
            }
        }
        /// <inheritdoc cref="TlsMode"/>
        public T WithTlsMode(TlsMode tls_mode)
        {
            TlsMode = tls_mode;
            return (T)this;
        }
        /// <inheritdoc cref="TlsMode"/>
        public T With(TlsMode tls_mode)
        {
            return WithTlsMode(tls_mode);
        }
        #endregion
        #region Request Timeout
        /// <summary>
        /// The duration in milliseconds that the client should wait for a request to complete. This
        /// duration encompasses sending the request, awaiting for a response from the server, and any
        /// required reconnections or retries. If the specified timeout is exceeded for a pending request,
        /// it will result in a timeout error. If not set, a default value will be used.
        /// </summary>
        public uint RequestTimeout
        {
            set
            {
                config.request_timeout = value;
            }
        }
        /// <inheritdoc cref="RequestTimeout"/>
        public T WithRequestTimeout(uint request_timeout)
        {
            RequestTimeout = request_timeout;
            return (T)this;
        }
        #endregion
        #region Read From
        /// <summary>
        /// Configure the client's read from strategy. If not set, <seealso cref="ReadFrom.Primary"/> will be used.
        /// </summary>
        public ReadFrom ReadFrom
        {
            set
            {
                config.read_from = value;
            }
        }
        /// <inheritdoc cref="ReadFrom"/>
        public T WithReadFrom(ReadFrom read_from)
        {
            ReadFrom = read_from;
            return (T)this;
        }
        /// <inheritdoc cref="ReadFrom"/>
        public T With(ReadFrom read_from)
        {
            return WithReadFrom(read_from);
        }
        #endregion
        #region Authentication
        /// <summary>
        /// Configure credentials for authentication process. If none are set, the client will not authenticate itself with the server.
        /// </summary>
        /// <value>
        /// <c>username</c> The username that will be used for authenticating connections to the Redis servers. If not supplied, <c>"default"</c> will be used.<br />
        /// <c>password</c> The password that will be used for authenticating connections to the Redis servers.
        /// </value>
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
        /// <summary>
        /// Configure credentials for authentication process. If none are set, the client will not authenticate itself with the server.
        /// </summary>
        /// <param name="username">The username that will be used for authenticating connections to the Redis servers. If not supplied, <c>"default"</c> will be used.></param>
        /// <param name="password">The password that will be used for authenticating connections to the Redis servers.</param>
        public T WithAuthentication(string? username, string? password)
        {
            Authentication = (username, password);
            return (T)this;
        }
        /// <inheritdoc cref="Authentication"/>
        public T WithAuthentication((string? username, string? password) credentials)
        {
            return WithAuthentication(credentials.username, credentials.password);
        }
        #endregion
        #region Protocol
        /// <summary>
        /// Configure the protocol version to use. If not set, <seealso cref="Protocol.RESP3"/> will be used.<br />
        /// See also <seealso cref="Protocol"/>.
        /// </summary>
        public Protocol ProtocolVersion
        {
            set
            {
                config.protocol = value;
            }
        }

        /// <inheritdoc cref="ProtocolVersion"/>
        public T WithProtocolVersion(Protocol protocol)
        {
            ProtocolVersion = protocol;
            return (T)this;
        }

        /// <inheritdoc cref="ProtocolVersion"/>
        public T With(Protocol protocol)
        {
            ProtocolVersion = protocol;
            return (T)this;
        }
        #endregion
        #region Client Name
        /// <summary>
        /// Client name to be used for the client. Will be used with CLIENT SETNAME command during connection establishment.
        /// </summary>
        public string? ClientName
        {
            set
            {
                config.client_name = value;
            }
        }

        /// <inheritdoc cref="ClientName"/>
        public T WithClientName(string? clientName)
        {
            ClientName = clientName;
            return (T)this;
        }
        #endregion

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

    /// <summary>
    /// Represents the configuration settings for a Standalone Redis client.
    /// </summary>
    public class StandaloneClientConfigurationBuilder : ClientConfigurationBuilder<StandaloneClientConfigurationBuilder>
    {
        public StandaloneClientConfigurationBuilder() : base(false) { }

        /// <summary>
        /// Complete the configuration with given settings.
        /// </summary>
        public new StandaloneClientConfiguration Build()
        {
            return new StandaloneClientConfiguration() { request = base.Build() };
        }

        #region DB ID
        // TODO: not used
        /// <summary>
        /// Index of the logical database to connect to.
        /// </summary>
        public uint DataBaseId
        {
            set
            {
                config.database_id = value;
            }
        }
        /// <inheritdoc cref="DataBaseId"/>
        public StandaloneClientConfigurationBuilder WithDataBaseId(uint dataBaseId)
        {
            DataBaseId = dataBaseId;
            return this;
        }
        #endregion
        #region Connection Retry Strategy
        /// <summary>
        /// Strategy used to determine how and when to reconnect, in case of connection failures.<br />
        /// See also <seealso cref="RetryStrategy"/>
        /// </summary>
        public RetryStrategy ConnectionRetryStrategy
        {
            set
            {
                config.connection_retry_strategy = value;
            }
        }
        /// <inheritdoc cref="ConnectionRetryStrategy"/>
        public StandaloneClientConfigurationBuilder WithConnectionRetryStrategy(RetryStrategy connection_retry_strategy)
        {
            ConnectionRetryStrategy = connection_retry_strategy;
            return this;
        }
        /// <inheritdoc cref="ConnectionRetryStrategy"/>
        public StandaloneClientConfigurationBuilder With(RetryStrategy connection_retry_strategy)
        {
            return WithConnectionRetryStrategy(connection_retry_strategy);
        }
        /// <inheritdoc cref="ConnectionRetryStrategy"/>
        /// <param name="number_of_retries"><inheritdoc cref="RetryStrategy.Number_of_retries" path="/summary"/></param>
        /// <param name="factor"><inheritdoc cref="RetryStrategy.Factor" path="/summary"/></param>
        /// <param name="exponent_base"><inheritdoc cref="RetryStrategy.Exponent_base" path="/summary"/></param>
        public StandaloneClientConfigurationBuilder WithConnectionRetryStrategy(uint number_of_retries, uint factor, uint exponent_base)
        {
            return WithConnectionRetryStrategy(new RetryStrategy(number_of_retries, factor, exponent_base));
        }
        #endregion
    }

    /// <summary>
    /// Represents the configuration settings for a Cluster Redis client.<br />
    /// Notes: Currently, the reconnection strategy in cluster mode is not configurable, and exponential backoff with fixed values is used.
    /// </summary>
    public class ClusterClientConfigurationBuilder : ClientConfigurationBuilder<StandaloneClientConfigurationBuilder>
    {
        public ClusterClientConfigurationBuilder() : base(true) { }

        /// <summary>
        /// Complete the configuration with given settings.
        /// </summary>
        public new ClusterClientConfiguration Build()
        {
            return new ClusterClientConfiguration() { request = base.Build() };
        }
    }
}
