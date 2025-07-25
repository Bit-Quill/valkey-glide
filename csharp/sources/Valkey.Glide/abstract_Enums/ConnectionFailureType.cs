﻿namespace Valkey.Glide;

/// <summary>
/// The known types of connection failure.
/// </summary>
public enum ConnectionFailureType
{
    /// <summary>
    /// This event is not a failure.
    /// </summary>
    None,

    /// <summary>
    /// No viable connections were available for this operation.
    /// </summary>
    UnableToResolvePhysicalConnection,

    /// <summary>
    /// The socket for this connection failed.
    /// </summary>
    SocketFailure,

    /// <summary>
    /// Either SSL Stream or Valkey authentication failed.
    /// </summary>
    AuthenticationFailure,

    /// <summary>
    /// An unexpected response was received from the server.
    /// </summary>
    ProtocolFailure,

    /// <summary>
    /// An unknown internal error occurred.
    /// </summary>
    InternalFailure,

    /// <summary>
    /// The socket was closed.
    /// </summary>
    SocketClosed,

    /// <summary>
    /// The socket was closed.
    /// </summary>
    ConnectionDisposed,

    /// <summary>
    /// The database is loading and is not available for use.
    /// </summary>
    Loading,

    /// <summary>
    /// It has not been possible to create an initial connection to the server(s).
    /// </summary>
    UnableToConnect,

    /// <summary>
    /// High-integrity mode was enabled, and a failure was detected.
    /// </summary>
    ResponseIntegrityFailure,
}
