/**
* Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
*/

using static Glide.AsyncClient;

namespace Glide;

public abstract class Errors
{
    public abstract class RedisError : Exception
    {
        internal RedisError(string? message) : base(message) { }
    }

    public sealed class UnspecifiedException : RedisError
    {
        internal UnspecifiedException(string? message) : base(message) { }
    }

    public sealed class ExecutionAbortedException : RedisError
    {
        internal ExecutionAbortedException(string? message) : base(message) { }
    }

    public sealed class DisconnectedException : RedisError
    {
        internal DisconnectedException(string? message) : base(message) { }
    }

    public sealed class TimeoutException : RedisError
    {
        internal TimeoutException(string? message) : base(message) { }
    }

    internal static RedisError MakeException(ErrorType type, string? message) => type switch
    {
        ErrorType.ExecAbort => new ExecutionAbortedException(message),
        ErrorType.Disconnect => new DisconnectedException(message),
        ErrorType.Timeout => new TimeoutException(message),
        _ => new UnspecifiedException(message),
    };
}
