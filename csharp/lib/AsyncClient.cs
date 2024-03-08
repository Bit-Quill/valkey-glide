/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

using System.Runtime.InteropServices;

using static Glide.ConnectionConfiguration;
using static Glide.Errors;

namespace Glide;

public class AsyncClient : IDisposable
{
    #region public methods
    public enum RequestType : uint
    {
        // copied from redis_request.proto
        CustomCommand = 1,
        GetString = 2,
        SetString = 3,
        Ping = 4,
        Info = 5,
        // to be continued ...
    }

    public AsyncClient(StandaloneClientConfiguration config)
    {
        successCallbackDelegate = SuccessCallback;
        var successCallbackPointer = Marshal.GetFunctionPointerForDelegate(successCallbackDelegate);
        failureCallbackDelegate = FailureCallback;
        var failureCallbackPointer = Marshal.GetFunctionPointerForDelegate(failureCallbackDelegate);
        var configPtr = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(ConnectionRequest)));
        Marshal.StructureToPtr(config.ToRequest(), configPtr, false);
        var responsePtr = CreateClientFfi(configPtr, successCallbackPointer, failureCallbackPointer);
        Marshal.FreeHGlobal(configPtr);
        var response = (ConnectionResponse?)Marshal.PtrToStructure(responsePtr, typeof(ConnectionResponse));

        if (response == null)
        {
            throw new DisconnectedException("Failed creating a client");
        }
        clientPointer = response?.Client ?? IntPtr.Zero;
        FreeConnectionResponse(responsePtr);

        if (clientPointer == IntPtr.Zero || !string.IsNullOrEmpty(response?.Error))
        {
            throw new DisconnectedException(response?.Error ?? "Failed creating a client");
        }
    }

    public async Task SetAsync(string key, string value)
    {
        var message = messageContainer.GetMessageForCall(key, value);
        Command(clientPointer, (ulong)message.Index, RequestType.SetString, (ulong)message.Args.Length, message.Args);
        await message;
    }

    public async Task<string?> Custom(string[] args)
    {
        var message = messageContainer.GetMessageForCall(args);
        Command(clientPointer, (ulong)message.Index, RequestType.CustomCommand, (ulong)args.Length, message.Args);
        return await message;
    }

    public async Task<string?> GetAsync(string key)
    {
        var message = messageContainer.GetMessageForCall(key);
        Command(clientPointer, (ulong)message.Index, RequestType.GetString, (ulong)message.Args.Length, message.Args);
        return await message;
    }

    public void Dispose()
    {
        if (clientPointer == IntPtr.Zero)
        {
            return;
        }
        messageContainer.DisposeWithError(null);
        CloseClientFfi(clientPointer);
        clientPointer = IntPtr.Zero;
    }

    #endregion public methods

    #region private methods

    private void SuccessCallback(ulong index, IntPtr str)
    {
        var result = str == IntPtr.Zero ? null : Marshal.PtrToStringAnsi(str);
        // Work needs to be offloaded from the calling thread, because otherwise we might starve the client's thread pool.
        Task.Run(() =>
        {
            var message = messageContainer.GetMessage((int)index);
            message.SetResult(result);
        });
    }

    private void FailureCallback(ulong index, IntPtr error_msg_ptr, ErrorType error_type)
    {
        var error = error_msg_ptr == IntPtr.Zero ? null : Marshal.PtrToStringAnsi(error_msg_ptr);
        // Work needs to be offloaded from the calling thread, because otherwise we might starve the client's thread pool.
        _ = Task.Run(() => messageContainer.GetMessage((int)index)
                .SetException(Errors.MakeException(error_type, error)));
    }

    ~AsyncClient() => Dispose();
    #endregion private methods

    #region private fields

    /// Held as a measure to prevent the delegate being garbage collected. These are delegated once
    /// and held in order to prevent the cost of marshalling on each function call.
    private readonly FailureAction failureCallbackDelegate;

    /// Held as a measure to prevent the delegate being garbage collected. These are delegated once
    /// and held in order to prevent the cost of marshalling on each function call.
    private readonly StringAction successCallbackDelegate;

    /// Raw pointer to the underlying native client.
    private IntPtr clientPointer;

    private readonly MessageContainer<string> messageContainer = new();

    #endregion private fields

    #region FFI function declarations

    private delegate void StringAction(ulong index, IntPtr str);
    /// <summary>
    /// Glide request failure callback.
    /// </summary>
    /// <param name="index">Request ID</param>
    /// <param name="error_msg_ptr">Error message</param>
    /// <param name="errorType">Error type</param>
    private delegate void FailureAction(ulong index, IntPtr error_msg_ptr, ErrorType errorType);

    [DllImport("libglide_rs", CallingConvention = CallingConvention.Cdecl, EntryPoint = "command")]
    private static extern void Command(IntPtr client, ulong index, RequestType requestType, ulong argCount, IntPtr[] args);

    private delegate void IntAction(IntPtr arg);
    [DllImport("libglide_rs", CallingConvention = CallingConvention.Cdecl, EntryPoint = "create_client_using_config")]
    private static extern IntPtr CreateClientFfi(IntPtr config, IntPtr successCallback, IntPtr failureCallback);

    [DllImport("libglide_rs", CallingConvention = CallingConvention.Cdecl, EntryPoint = "close_client")]
    private static extern void CloseClientFfi(IntPtr client);

    [DllImport("libglide_rs", CallingConvention = CallingConvention.Cdecl, EntryPoint = "free_connection_response")]
    private static extern void FreeConnectionResponse(IntPtr connectionResponsePtr);

    internal enum ErrorType : uint
    {
        /// <summary>
        /// Represented by <see cref="Errors.UnspecifiedException"/> for user
        /// </summary>
        Unspecified = 0,
        /// <summary>
        /// Represented by <see cref="Errors.ExecutionAbortedException"/> for user
        /// </summary>
        ExecAbort = 1,
        /// <summary>
        /// Represented by <see cref="TimeoutException"/> for user
        /// </summary>
        Timeout = 2,
        /// <summary>
        /// Represented by <see cref="Errors.DisconnectedException"/> for user
        /// </summary>
        Disconnect = 3,
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    internal struct ConnectionResponse
    {
        public IntPtr Client;
        public string Error;
        public ErrorType ErrorType;
    }
    #endregion
}
