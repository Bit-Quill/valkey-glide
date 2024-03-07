/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

using static Glide.AsyncClient;

namespace Glide;

public abstract class Errors
{
    public sealed class UnspecifiedException : Exception
    {
        internal UnspecifiedException(string? message) : base(message) { }
    }

    public sealed class ExecutionAbortedException : Exception
    {
        internal ExecutionAbortedException(string? message) : base(message) { }
    }

    public sealed class DisconnectedException : Exception
    {
        internal DisconnectedException(string? message) : base(message) { }
    }

    internal static Exception MakeException(ErrorType type, string? message) => type switch
    {
        ErrorType.ExecAbort => new ExecutionAbortedException(message),
        ErrorType.Disconnect => new DisconnectedException(message),
        ErrorType.Timeout => new TimeoutException(message),
        _ => new UnspecifiedException(message),
    };
}
