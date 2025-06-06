# Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0

from __future__ import annotations

from typing import Dict, List, Mapping, Optional, Union, cast

from glide.async_commands.batch import ClusterBatch
from glide.async_commands.batch_options import ClusterBatchOptions
from glide.async_commands.command_args import ObjectType
from glide.async_commands.core import (
    CoreCommands,
    FlushMode,
    FunctionRestorePolicy,
    InfoSection,
)
from glide.constants import (
    TOK,
    TClusterResponse,
    TEncodable,
    TFunctionListResponse,
    TFunctionStatsSingleNodeResponse,
    TResult,
)
from glide.exceptions import RequestError
from glide.protobuf.command_request_pb2 import RequestType
from glide.routes import Route

from ..glide import ClusterScanCursor, Script


class ClusterCommands(CoreCommands):
    async def custom_command(
        self, command_args: List[TEncodable], route: Optional[Route] = None
    ) -> TClusterResponse[TResult]:
        """
        Executes a single command, without checking inputs.
        See the [Valkey GLIDE Wiki](https://github.com/valkey-io/valkey-glide/wiki/General-Concepts#custom-command)
        for details on the restrictions and limitations of the custom command API.

        This function should only be used for single-response commands. Commands that don't return complete response and awaits
        (such as SUBSCRIBE), or that return potentially more than a single response (such as XREAD), or that change the
        client's behavior (such as entering pub/sub mode on RESP2 connections) shouldn't be called using this function.

            For example - Return a list of all pub/sub clients from all nodes::

                await client.customCommand(["CLIENT", "LIST","TYPE", "PUBSUB"], AllNodes())

        Args:
            command_args (List[TEncodable]): List of the command's arguments, where each argument is either a string or bytes.
            Every part of the command, including the command name and subcommands, should be added as a separate value in args.
            route (Optional[Route]): The command will be routed automatically based on the passed command's default request
                policy, unless `route` is provided, in which
            case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TClusterResponse[TResult]: The returning value depends on the executed command and the route.
        """
        return cast(
            TClusterResponse[TResult],
            await self._execute_command(RequestType.CustomCommand, command_args, route),
        )

    async def info(
        self,
        sections: Optional[List[InfoSection]] = None,
        route: Optional[Route] = None,
    ) -> TClusterResponse[bytes]:
        """
        Get information and statistics about the server.

        Starting from server version 7, command supports multiple section arguments.

        See [valkey.io](https://valkey.io/commands/info/) for details.

        Args:
            sections (Optional[List[InfoSection]]): A list of InfoSection values specifying which sections of
                information to retrieve. When no parameter is provided, the default option is assumed.
            route (Optional[Route]): The command will be routed to all primaries, unless `route` is provided, in
                which case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TClusterResponse[bytes]: If a single node route is requested, returns a bytes string containing the information for
            the required sections. Otherwise, returns a dict of bytes strings, with each key containing the address of
            the queried node and value containing the information regarding the requested sections.
        """
        args: List[TEncodable] = (
            [section.value for section in sections] if sections else []
        )
        return cast(
            TClusterResponse[bytes],
            await self._execute_command(RequestType.Info, args, route),
        )

    async def exec(
        self,
        batch: ClusterBatch,
        raise_on_error: bool,
        options: Optional[ClusterBatchOptions] = None,
    ) -> Optional[List[TResult]]:
        """
        Executes a batch by processing the queued commands.

        **Routing Behavior:**

        - If a `route` is specified in `ClusterBatchOptions`, the entire batch is sent
          to the specified node.
        - If no `route` is specified:
            - **Atomic batches (Transactions):** Routed to the slot owner of the
              first key in the batch. If no key is found, the request is sent to a random node.
            - **Non-atomic batches (Pipelines):** Each command is routed to the node
              owning the corresponding key's slot. If no key is present, routing follows the
              command's request policy. Multi-node commands are automatically split and
              dispatched to the appropriate nodes.

        **Behavior notes:**

        - **Atomic Batches (Transactions):** All key-based commands must map to the
          same hash slot. If keys span different slots, the transaction will fail. If the
          transaction fails due to a `WATCH` command, `EXEC` will return `None`.

        **Retry and Redirection:**

        - If a redirection error occurs:
            - **Atomic batches (Transactions):** The entire transaction will be
              redirected.
            - **Non-atomic batches:** Only commands that encountered redirection
              errors will be redirected.
        - Retries for failures will be handled according to the configured `BatchRetryStrategy`.

        Args:
            batch (ClusterBatch): A `ClusterBatch` containing the commands to execute.
            raise_on_error (bool): Determines how errors are handled within the batch response.
                When set to `True`, the first encountered error in the batch will be raised as an
                exception of type `RequestError` after all retries and reconnections have been
                executed.
                When set to `False`, errors will be included as part of the batch response,
                allowing the caller to process both successful and failed commands together. In this case,
                error details will be provided as instances of `RequestError`.
            options (Optional[ClusterBatchOptions]): A `ClusterBatchOptions` object containing execution options.

        Returns:
            Optional[List[TResult]]: An array of results, where each entry
                corresponds to a command's execution result.

        See Also:
            [Valkey Transactions (Atomic Batches)](https://valkey.io/docs/topics/transactions/)
            [Valkey Pipelines (Non-Atomic Batches)](https://valkey.io/docs/topics/pipelining/)

        Examples:
            # Atomic batch (transaction): all keys must share the same hash slot
            >>> options = ClusterBatchOptions(timeout=1000)  # Set a timeout of 1000 milliseconds
            >>> atomic_batch = ClusterBatch(is_atomic=True)
            >>> atomic_batch.set("key", "1")
            >>> atomic_batch.incr("key")
            >>> atomic_batch.get("key")
            >>> atomic_result = await cluster_client.exec(atomic_batch, False, options)
            >>> print(f"Atomic Batch Result: {atomic_result}")
            # Output: Atomic Batch Result: [OK, 2, 2]

            # Non-atomic batch (pipeline): keys may span different hash slots
            >>> retry_strategy = BatchRetryStrategy(retry_server_error=True, retry_connection_error=False)
            >>> pipeline_options = ClusterBatchOptions(retry_strategy=retry_strategy)
            >>> non_atomic_batch = ClusterBatch(is_atomic=False)
            >>> non_atomic_batch.set("key1", "value1")
            >>> non_atomic_batch.set("key2", "value2")
            >>> non_atomic_batch.get("key1")
            >>> non_atomic_batch.get("key2")
            >>> non_atomic_result = await cluster_client.exec(non_atomic_batch, False, pipeline_options)
            >>> print(f"Non-Atomic Batch Result: {non_atomic_result}")
            # Output: Non-Atomic Batch Result: [OK, OK, value1, value2]
        """
        commands = batch.commands[:]

        if (
            batch.is_atomic
            and options
            and options.retry_strategy
            and (
                options.retry_strategy.retry_server_error
                or options.retry_strategy.retry_connection_error
            )
        ):
            raise RequestError(
                "Retry strategies are not supported for atomic batches (transactions). "
            )

        # Extract values to make the _execute_batch call cleaner
        retry_server_error = (
            options.retry_strategy.retry_server_error
            if options and options.retry_strategy
            else False
        )
        retry_connection_error = (
            options.retry_strategy.retry_connection_error
            if options and options.retry_strategy
            else False
        )
        route = options.route if options else None
        timeout = options.timeout if options else None

        return await self._execute_batch(
            commands,
            batch.is_atomic,
            raise_on_error,
            retry_server_error,
            retry_connection_error,
            route,
            timeout,
        )

    async def config_resetstat(
        self,
        route: Optional[Route] = None,
    ) -> TOK:
        """
        Resets the statistics reported by the server using the INFO and LATENCY HISTOGRAM commands.

        See [valkey.io](https://valkey.io/commands/config-resetstat/) for details.

        Args:
            route (Optional[Route]): The command will be routed automatically to all nodes, unless `route` is provided, in
                which case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            OK: Returns "OK" to confirm that the statistics were successfully reset.
        """
        return cast(
            TOK, await self._execute_command(RequestType.ConfigResetStat, [], route)
        )

    async def config_rewrite(
        self,
        route: Optional[Route] = None,
    ) -> TOK:
        """
        Rewrite the configuration file with the current configuration.

        See [valkey.io](https://valkey.io/commands/config-rewrite/) for details.

        Args:
            route (Optional[TRoute]): The command will be routed automatically to all nodes, unless `route` is provided, in
                which case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            OK: OK is returned when the configuration was rewritten properly. Otherwise an error is raised.

        Example:
            >>> await client.config_rewrite()
                'OK'
        """
        return cast(
            TOK, await self._execute_command(RequestType.ConfigRewrite, [], route)
        )

    async def client_id(
        self,
        route: Optional[Route] = None,
    ) -> TClusterResponse[int]:
        """
        Returns the current connection id.

        See [valkey.io](https://valkey.io/commands/client-id/) for more information.

        Args:
            route (Optional[Route]): The command will be sent to a random node, unless `route` is provided, in which
                case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[int]: The id of the client.

            If a single node route is requested, returns a int representing the client's id.

            Otherwise, returns a dict of [byte , int] where each key contains the address of
            the queried node and the value contains the client's id.
        """
        return cast(
            TClusterResponse[int],
            await self._execute_command(RequestType.ClientId, [], route),
        )

    async def ping(
        self, message: Optional[TEncodable] = None, route: Optional[Route] = None
    ) -> bytes:
        """
        Ping the server.

        See [valkey.io](https://valkey.io/commands/ping/) for more details.

        Args:
            message (Optional[TEncodable]): An optional message to include in the PING command. If not provided,
                the server will respond with b"PONG". If provided, the server will respond with a copy of the message.
            route (Optional[Route]): The command will be sent to all primaries, unless `route` is provided, in which
                case the client will route the command to the nodes defined by `route`

        Returns:
           bytes: b'PONG' if `message` is not provided, otherwise return a copy of `message`.

        Examples:
            >>> await client.ping()
                b"PONG"
            >>> await client.ping("Hello")
                b"Hello"
        """
        argument = [] if message is None else [message]
        return cast(
            bytes, await self._execute_command(RequestType.Ping, argument, route)
        )

    async def config_get(
        self, parameters: List[TEncodable], route: Optional[Route] = None
    ) -> TClusterResponse[Dict[bytes, bytes]]:
        """
        Get the values of configuration parameters.
        Starting from server version 7, command supports multiple parameters.

        See [valkey.io](https://valkey.io/commands/config-get/) for details.

        Args:
            parameters (List[TEncodable]): A list of configuration parameter names to retrieve values for.
            route (Optional[Route]): The command will be routed to a random node, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[Dict[bytes, bytes]]: A dictionary of values corresponding to the
            configuration parameters.
            When specifying a route other than a single node, response will be::

                {Address (bytes) : response (Dict[bytes, bytes]) , ... }

            with type of Dict[bytes, Dict[bytes, bytes]].

        Examples:
            >>> await client.config_get(["timeout"] , RandomNode())
                {b'timeout': b'1000'}
            >>> await client.config_get(["timeout" , b"maxmemory"])
                {b'timeout': b'1000', b"maxmemory": b"1GB"}
        """
        return cast(
            TClusterResponse[Dict[bytes, bytes]],
            await self._execute_command(RequestType.ConfigGet, parameters, route),
        )

    async def config_set(
        self,
        parameters_map: Mapping[TEncodable, TEncodable],
        route: Optional[Route] = None,
    ) -> TOK:
        """
        Set configuration parameters to the specified values.
        Starting from server version 7, command supports multiple parameters.

        See [valkey.io](https://valkey.io/commands/config-set/) for details.

        Args:
            parameters_map (Mapping[TEncodable, TEncodable]): A map consisting of configuration
                parameters and their respective values to set.
            route (Optional[Route]): The command will be routed to all nodes, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            OK: Returns OK if all configurations have been successfully set. Otherwise, raises an error.

        Examples:
            >>> await client.config_set({"timeout": "1000", b"maxmemory": b"1GB"})
                OK
        """
        parameters: List[TEncodable] = []
        for pair in parameters_map.items():
            parameters.extend(pair)
        return cast(
            TOK,
            await self._execute_command(RequestType.ConfigSet, parameters, route),
        )

    async def client_getname(
        self, route: Optional[Route] = None
    ) -> TClusterResponse[Optional[bytes]]:
        """
        Get the name of the connection to which the request is routed.

        See [valkey.io](https://valkey.io/commands/client-getname/) for more details.

        Args:
            route (Optional[Route]): The command will be routed to a random node, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[Optional[bytes]]: The name of the client connection as a bytes string if a name is set,
            or None if no name is assigned.

            When specifying a route other than a single node, response will be::

                {Address (bytes) : response (Optional[bytes]) , ... }

            with type of Dict[str, Optional[str]].

        Examples:
            >>> await client.client_getname()
                b'Connection Name'
            >>> await client.client_getname(AllNodes())
                {b'addr': b'Connection Name', b'addr2': b'Connection Name', b'addr3': b'Connection Name'}
        """
        return cast(
            TClusterResponse[Optional[bytes]],
            await self._execute_command(RequestType.ClientGetName, [], route),
        )

    async def dbsize(self, route: Optional[Route] = None) -> int:
        """
        Returns the number of keys in the database.

        See [valkey.io](https://valkey.io/commands/dbsize) for more details.

        Args:
            route (Optional[Route]): The command will be routed to all primaries, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            int: The number of keys in the database.

            In the case of routing the query to multiple nodes, returns the aggregated number of keys across the
            different nodes.

        Examples:
            >>> await client.dbsize()
                10  # Indicates there are 10 keys in the cluster.
        """
        return cast(int, await self._execute_command(RequestType.DBSize, [], route))

    async def echo(
        self, message: TEncodable, route: Optional[Route] = None
    ) -> TClusterResponse[bytes]:
        """
        Echoes the provided `message` back.

        See [valkey.io](https://valkey.io/commands/echo) for more details.

        Args:
            message (TEncodable): The message to be echoed back.
            route (Optional[Route]): The command will be routed to a random node, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[bytes]: The provided `message`.

            When specifying a route other than a single node, response will be::

                {Address (bytes) : response (bytes) , ... }

            with type of Dict[bytes, bytes].

        Examples:
            >>> await client.echo(b"Valkey GLIDE")
                b'Valkey GLIDE'
            >>> await client.echo("Valkey GLIDE", AllNodes())
                {b'addr': b'Valkey GLIDE', b'addr2': b'Valkey GLIDE', b'addr3': b'Valkey GLIDE'}
        """
        return cast(
            TClusterResponse[bytes],
            await self._execute_command(RequestType.Echo, [message], route),
        )

    async def function_load(
        self,
        library_code: TEncodable,
        replace: bool = False,
        route: Optional[Route] = None,
    ) -> bytes:
        """
        Loads a library to Valkey.

        See [valkey.io](https://valkey.io/commands/function-load/) for more details.

        Args:
            library_code (TEncodable): The source code that implements the library.
            replace (bool): Whether the given library should overwrite a library with the same name if
                it already exists.
            route (Optional[Route]): The command will be routed to all primaries, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            bytes: The library name that was loaded.

        Examples:
            >>> code = "#!lua name=mylib \\n redis.register_function('myfunc', function(keys, args) return args[1] end)"
            >>> await client.function_load(code, True, RandomNode())
                b"mylib"

        Since: Valkey 7.0.0.
        """
        return cast(
            bytes,
            await self._execute_command(
                RequestType.FunctionLoad,
                ["REPLACE", library_code] if replace else [library_code],
                route,
            ),
        )

    async def function_list(
        self,
        library_name_pattern: Optional[TEncodable] = None,
        with_code: bool = False,
        route: Optional[Route] = None,
    ) -> TClusterResponse[TFunctionListResponse]:
        """
        Returns information about the functions and libraries.

        See [valkey.io](https://valkey.io/commands/function-list/) for more details.

        Args:
            library_name_pattern (Optional[TEncodable]):  A wildcard pattern for matching library names.
            with_code (bool): Specifies whether to request the library code from the server or not.
            route (Optional[Route]): The command will be routed to a random node, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[TFunctionListResponse]: Info
            about all or selected libraries and their functions.

        Examples:
            >>> response = await client.function_list("myLib?_backup", True)
                [{
                    b"library_name": b"myLib5_backup",
                    b"engine": b"LUA",
                    b"functions": [{
                        b"name": b"myfunc",
                        b"description": None,
                        b"flags": {b"no-writes"},
                    }],
                    b"library_code":
                        b"#!lua name=mylib \\n redis.register_function('myfunc', function(keys, args) " \\
                        b"return args[1] end)"
                }]

        Since: Valkey 7.0.0.
        """
        args = []
        if library_name_pattern is not None:
            args.extend(["LIBRARYNAME", library_name_pattern])
        if with_code:
            args.append("WITHCODE")
        return cast(
            TClusterResponse[TFunctionListResponse],
            await self._execute_command(
                RequestType.FunctionList,
                args,
                route,
            ),
        )

    async def function_flush(
        self, mode: Optional[FlushMode] = None, route: Optional[Route] = None
    ) -> TOK:
        """
        Deletes all function libraries.

        See [valkey.io](https://valkey.io/commands/function-flush/) for more details.

        Args:
            mode (Optional[FlushMode]): The flushing mode, could be either `SYNC` or `ASYNC`.
            route (Optional[Route]): The command will be routed to all primaries, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TOK: A simple `OK`.

        Examples:
            >>> await client.function_flush(FlushMode.SYNC)
                "OK"

        Since: Valkey 7.0.0.
        """
        return cast(
            TOK,
            await self._execute_command(
                RequestType.FunctionFlush,
                [mode.value] if mode else [],
                route,
            ),
        )

    async def function_delete(
        self, library_name: TEncodable, route: Optional[Route] = None
    ) -> TOK:
        """
        Deletes a library and all its functions.

        See [valkey.io](https://valkey.io/commands/function-delete/) for more details.

        Args:
            library_name (TEncodable): The library name to delete
            route (Optional[Route]): The command will be routed to all primaries, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TOK: A simple `OK`.

        Examples:
            >>> await client.function_delete("my_lib")
                "OK"

        Since: Valkey 7.0.0.
        """
        return cast(
            TOK,
            await self._execute_command(
                RequestType.FunctionDelete,
                [library_name],
                route,
            ),
        )

    async def function_kill(self, route: Optional[Route] = None) -> TOK:
        """
        Kills a function that is currently executing.
        This command only terminates read-only functions.

        See [valkey.io](https://valkey.io/commands/function-kill/) for more details.

        Args:
            route (Optional[Route]): The command will be routed to all nodes, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TOK: A simple `OK`.

        Examples:
            >>> await client.function_kill()
                "OK"

        Since: Valkey 7.0.0.
        """
        return cast(
            TOK,
            await self._execute_command(
                RequestType.FunctionKill,
                [],
                route,
            ),
        )

    async def fcall_route(
        self,
        function: TEncodable,
        arguments: Optional[List[TEncodable]] = None,
        route: Optional[Route] = None,
    ) -> TClusterResponse[TResult]:
        """
        Invokes a previously loaded function.

        See [valkey.io](https://valkey.io/commands/fcall/) for more details.

        Args:
            function (TEncodable): The function name.
            arguments (Optional[List[TEncodable]]): A list of `function` arguments. `Arguments`
                should not represent names of keys.
            route (Optional[Route]): The command will be routed to a random primary node, unless `route` is provided, in which
                case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TClusterResponse[TResult]: If a single node route is requested,
            returns a Optional[TResult] representing the function's return value.

            Otherwise, returns a dict of [bytes , Optional[TResult]] where each key contains the address of
            the queried node and the value contains the function's return value.

        Example:
            >>> await client.fcall(
            ...     "Deep_Thought",
            ...     ["Answer", "to", "the", "Ultimate", "Question", "of", "Life,", "the", "Universe,", "and", "Everything"],
            ...     RandomNode()
            ... )
                b'new_value' # Returns the function's return value.

        Since: Valkey version 7.0.0.
        """
        args = [function, "0"]
        if arguments is not None:
            args.extend(arguments)
        return cast(
            TClusterResponse[TResult],
            await self._execute_command(RequestType.FCall, args, route),
        )

    async def fcall_ro_route(
        self,
        function: TEncodable,
        arguments: Optional[List[TEncodable]] = None,
        route: Optional[Route] = None,
    ) -> TClusterResponse[TResult]:
        """
        Invokes a previously loaded read-only function.

        See [valkey.io](https://valkey.io/commands/fcall_ro) for more details.

        Args:
            function (TEncodable): The function name.
            arguments (List[TEncodable]): An `array` of `function` arguments. `arguments` should not
                represent names of keys.
            route (Optional[Route]): Specifies the routing configuration of the command. The client
                will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[TResult]: The return value depends on the function that was executed.

        Examples:
            >>> await client.fcall_ro_route("Deep_Thought", ALL_NODES)
                42 # The return value on the function that was executed

        Since: Valkey version 7.0.0.
        """
        args: List[TEncodable] = [function, "0"]
        if arguments is not None:
            args.extend(arguments)
        return cast(
            TClusterResponse[TResult],
            await self._execute_command(RequestType.FCallReadOnly, args, route),
        )

    async def function_stats(
        self, route: Optional[Route] = None
    ) -> TClusterResponse[TFunctionStatsSingleNodeResponse]:
        """
        Returns information about the function that's currently running and information about the
        available execution engines.

        See [valkey.io](https://valkey.io/commands/function-stats/) for more details

        Args:
            route (Optional[Route]): The command will be routed automatically to all nodes, unless `route` is provided, in
                which case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TClusterResponse[TFunctionStatsSingleNodeResponse]: A `Mapping` with two keys:

                - `running_script` with information about the running script.
                - `engines` with information about available engines and their stats.

            See example for more details.

        Examples:
            >>> await client.function_stats(RandomNode())
                {
                    'running_script': {
                        'name': 'foo',
                        'command': ['FCALL', 'foo', '0', 'hello'],
                        'duration_ms': 7758
                    },
                    'engines': {
                        'LUA': {
                            'libraries_count': 1,
                            'functions_count': 1,
                        }
                    }
                }

        Since: Valkey version 7.0.0.
        """
        return cast(
            TClusterResponse[TFunctionStatsSingleNodeResponse],
            await self._execute_command(RequestType.FunctionStats, [], route),
        )

    async def function_dump(
        self, route: Optional[Route] = None
    ) -> TClusterResponse[bytes]:
        """
        Returns the serialized payload of all loaded libraries.

        See [valkey.io](https://valkey.io/commands/function-dump/) for more details.

        Args:
            route (Optional[Route]): The command will be routed to a random node, unless
                `route` is provided, in which case the client will route the command to the
                nodes defined by `route`.

        Returns:
            TClusterResponse[bytes]: The serialized payload of all loaded libraries.

        Examples:
            >>> payload = await client.function_dump()
                # The serialized payload of all loaded libraries. This response can
                # be used to restore loaded functions on any Valkey instance.
            >>> await client.function_restore(payload)
                "OK" # The serialized dump response was used to restore the libraries.

        Since: Valkey 7.0.0.
        """
        return cast(
            TClusterResponse[bytes],
            await self._execute_command(RequestType.FunctionDump, [], route),
        )

    async def function_restore(
        self,
        payload: TEncodable,
        policy: Optional[FunctionRestorePolicy] = None,
        route: Optional[Route] = None,
    ) -> TOK:
        """
        Restores libraries from the serialized payload returned by the `function_dump` command.

        See [valkey.io](https://valkey.io/commands/function-restore/) for more details.

        Args:
            payload (bytes): The serialized data from the `function_dump` command.
            policy (Optional[FunctionRestorePolicy]): A policy for handling existing libraries.
            route (Optional[Route]): The command will be sent to all primaries, unless
                `route` is provided, in which case the client will route the command to the
                nodes defined by `route`.

        Returns:
            TOK: OK.

        Examples:
            >>> payload = await client.function_dump()
                # The serialized payload of all loaded libraries. This response can
                # be used to restore loaded functions on any Valkey instance.
            >>> await client.function_restore(payload, AllPrimaries())
                "OK" # The serialized dump response was used to restore the libraries with the specified route.
            >>> await client.function_restore(payload, FunctionRestorePolicy.FLUSH, AllPrimaries())
                "OK" # The serialized dump response was used to restore the libraries with the specified route and policy.

        Since: Valkey 7.0.0.
        """
        args: List[TEncodable] = [payload]
        if policy is not None:
            args.append(policy.value)

        return cast(
            TOK, await self._execute_command(RequestType.FunctionRestore, args, route)
        )

    async def time(
        self, route: Optional[Route] = None
    ) -> TClusterResponse[List[bytes]]:
        """
        Returns the server time.

        See [valkey.io](https://valkey.io/commands/time/) for more details.

        Args:
            route (Optional[Route]): The command will be routed to a random node, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[Optional[bytes]]:  The current server time as a two items `array`:
            A Unix timestamp and the amount of microseconds already elapsed in the current second.
            The returned `array` is in a [Unix timestamp, Microseconds already elapsed] format.

            When specifying a route other than a single node, response will be::

                {Address (bytes) : response (List[bytes]) , ... }

            with type of Dict[bytes, List[bytes]].

        Examples:
            >>> await client.time()
                [b'1710925775', b'913580']
            >>> await client.time(AllNodes())
                {
                    b'addr': [b'1710925775', b'913580'],
                    b'addr2': [b'1710925775', b'913580'],
                    b'addr3': [b'1710925775', b'913580']
                }
        """
        return cast(
            TClusterResponse[List[bytes]],
            await self._execute_command(RequestType.Time, [], route),
        )

    async def lastsave(self, route: Optional[Route] = None) -> TClusterResponse[int]:
        """
        Returns the Unix time of the last DB save timestamp or startup timestamp if no save was made since then.

        See [valkey.io](https://valkey.io/commands/lastsave) for more details.

        Args:
            route (Optional[Route]): The command will be routed to a random node, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[int]: The Unix time of the last successful DB save.

            If no route is provided, or a single node route is requested, returns an int representing the Unix time
            of the last successful DB save.

            Otherwise, returns a dict of [bytes , int] where each key contains the
            address of the queried node and the value contains the Unix time of the last successful DB save.

        Examples:
            >>> await client.lastsave()
                1710925775  # Unix time of the last DB save
            >>> await client.lastsave(AllNodes())
                {b'addr1': 1710925775, b'addr2': 1710925775, b'addr3': 1710925775}  # Unix time of the last DB save on
                                                                                    # each node
        """
        return cast(
            TClusterResponse[int],
            await self._execute_command(RequestType.LastSave, [], route),
        )

    async def publish(
        self,
        message: TEncodable,
        channel: TEncodable,
        sharded: bool = False,
    ) -> int:
        """
        Publish a message on pubsub channel.
        This command aggregates PUBLISH and SPUBLISH commands functionalities.
        The mode is selected using the 'sharded' parameter.
        For both sharded and non-sharded mode, request is routed using hashed channel as key.

        See [PUBLISH](https://valkey.io/commands/publish) and [SPUBLISH](https://valkey.io/commands/spublish)
        for more details.

        Args:
            message (TEncodable): Message to publish.
            channel (TEncodable): Channel to publish the message on.
            sharded (bool): Use sharded pubsub mode. Available since Valkey version 7.0.

        Returns:
            int: Number of subscriptions in that node that received the message.

        Examples:
            >>> await client.publish("Hi all!", "global-channel", False)
                1  # Published 1 instance of "Hi all!" message on global-channel channel using non-sharded mode
            >>> await client.publish(b"Hi to sharded channel1!", b"channel1", True)
                2  # Published 2 instances of "Hi to sharded channel1!" message on channel1 using sharded mode
        """
        result = await self._execute_command(
            RequestType.SPublish if sharded else RequestType.Publish, [channel, message]
        )
        return cast(int, result)

    async def pubsub_shardchannels(
        self, pattern: Optional[TEncodable] = None
    ) -> List[bytes]:
        """
        Lists the currently active shard channels.
        The command is routed to all nodes, and aggregates the response to a single array.

        See [valkey.io](https://valkey.io/commands/pubsub-shardchannels) for more details.

        Args:
            pattern (Optional[TEncodable]): A glob-style pattern to match active shard channels.
                If not provided, all active shard channels are returned.

        Returns:
            List[bytes]: A list of currently active shard channels matching the given pattern.
            If no pattern is specified, all active shard channels are returned.

        Examples:
            >>> await client.pubsub_shardchannels()
                [b'channel1', b'channel2']

            >>> await client.pubsub_shardchannels("channel*")
                [b'channel1', b'channel2']
        """
        command_args = [pattern] if pattern is not None else []
        return cast(
            List[bytes],
            await self._execute_command(RequestType.PubSubShardChannels, command_args),
        )

    async def pubsub_shardnumsub(
        self, channels: Optional[List[TEncodable]] = None
    ) -> Mapping[bytes, int]:
        """
        Returns the number of subscribers (exclusive of clients subscribed to patterns) for the specified shard channels.

        Note that it is valid to call this command without channels. In this case, it will just return an empty map.
        The command is routed to all nodes, and aggregates the response to a single map of the channels and their number of
        subscriptions.

        See [valkey.io](https://valkey.io/commands/pubsub-shardnumsub) for more details.

        Args:
            channels (Optional[List[TEncodable]]): The list of shard channels to query for the number of subscribers.
                If not provided, returns an empty map.

        Returns:
            Mapping[bytes, int]: A map where keys are the shard channel names and values are the number of subscribers.

        Examples:
            >>> await client.pubsub_shardnumsub(["channel1", "channel2"])
                {b'channel1': 3, b'channel2': 5}

            >>> await client.pubsub_shardnumsub()
                {}
        """
        return cast(
            Mapping[bytes, int],
            await self._execute_command(
                RequestType.PubSubShardNumSub, channels if channels else []
            ),
        )

    async def flushall(
        self, flush_mode: Optional[FlushMode] = None, route: Optional[Route] = None
    ) -> TOK:
        """
        Deletes all the keys of all the existing databases. This command never fails.

        See [valkey.io](https://valkey.io/commands/flushall) for more details.

        Args:
            flush_mode (Optional[FlushMode]): The flushing mode, could be either `SYNC` or `ASYNC`.
            route (Optional[Route]): The command will be routed to all primary nodes, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TOK: A simple OK response.

        Examples:
            >>> await client.flushall(FlushMode.ASYNC)
                OK  # This command never fails.
            >>> await client.flushall(FlushMode.ASYNC, AllNodes())
                OK  # This command never fails.
        """
        args: List[TEncodable] = []
        if flush_mode is not None:
            args.append(flush_mode.value)

        return cast(
            TOK,
            await self._execute_command(RequestType.FlushAll, args, route),
        )

    async def flushdb(
        self, flush_mode: Optional[FlushMode] = None, route: Optional[Route] = None
    ) -> TOK:
        """
        Deletes all the keys of the currently selected database. This command never fails.

        See [valkey.io](https://valkey.io/commands/flushdb) for more details.

        Args:
            flush_mode (Optional[FlushMode]): The flushing mode, could be either `SYNC` or `ASYNC`.
            route (Optional[Route]): The command will be routed to all primary nodes, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TOK: A simple OK response.

        Examples:
            >>> await client.flushdb()
                OK  # The keys of the currently selected database were deleted.
            >>> await client.flushdb(FlushMode.ASYNC)
                OK  # The keys of the currently selected database were deleted asynchronously.
            >>> await client.flushdb(FlushMode.ASYNC, AllNodes())
                OK  # The keys of the currently selected database were deleted asynchronously on all nodes.
        """
        args: List[TEncodable] = []
        if flush_mode is not None:
            args.append(flush_mode.value)

        return cast(
            TOK,
            await self._execute_command(RequestType.FlushDB, args, route),
        )

    async def copy(
        self,
        source: TEncodable,
        destination: TEncodable,
        replace: Optional[bool] = None,
    ) -> bool:
        """
        Copies the value stored at the `source` to the `destination` key. When `replace` is True,
        removes the `destination` key first if it already exists, otherwise performs no action.

        See [valkey.io](https://valkey.io/commands/copy) for more details.

        Note:
            Both `source` and `destination` must map to the same hash slot.

        Args:
            source (TEncodable): The key to the source value.
            destination (TEncodable): The key where the value should be copied to.
            replace (Optional[bool]): If the destination key should be removed before copying the value to it.

        Returns:
            bool: True if the source was copied. Otherwise, returns False.

        Examples:
            >>> await client.set("source", "sheep")
            >>> await client.copy(b"source", b"destination")
                True # Source was copied
            >>> await client.get("destination")
                b"sheep"

        Since: Valkey version 6.2.0.
        """
        args: List[TEncodable] = [source, destination]
        if replace is True:
            args.append("REPLACE")
        return cast(
            bool,
            await self._execute_command(RequestType.Copy, args),
        )

    async def lolwut(
        self,
        version: Optional[int] = None,
        parameters: Optional[List[int]] = None,
        route: Optional[Route] = None,
    ) -> TClusterResponse[bytes]:
        """
        Displays a piece of generative computer art and the Valkey version.

        See [valkey.io](https://valkey.io/commands/lolwut) for more details.

        Args:
            version (Optional[int]): Version of computer art to generate.
            parameters (Optional[List[int]]): Additional set of arguments in order to change the output:

                - For version `5`, those are length of the line, number of squares per row, and number of squares per column.
                - For version `6`, those are number of columns and number of lines.

            route (Optional[Route]): The command will be routed to a random node, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TClusterResponse[bytes]: A piece of generative computer art along with the current Valkey version.

            When specifying a route other than a single node, response will be::

                {Address (bytes) : response (bytes) , ... }

            with type of Dict[bytes, bytes].

        Examples:
            >>> await client.lolwut(6, [40, 20], RandomNode());
                b"Redis ver. 7.2.3" # Indicates the current Valkey version
        """
        args: List[TEncodable] = []
        if version is not None:
            args.extend(["VERSION", str(version)])
        if parameters:
            for var in parameters:
                args.extend(str(var))
        return cast(
            TClusterResponse[bytes],
            await self._execute_command(RequestType.Lolwut, args, route),
        )

    async def random_key(self, route: Optional[Route] = None) -> Optional[bytes]:
        """
        Returns a random existing key name.

        See [valkey.io](https://valkey.io/commands/randomkey) for more details.

        Args:
            route (Optional[Route]): The command will be routed to all primary nodes, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            Optional[bytes]: A random existing key name.

        Examples:
            >>> await client.random_key()
                b"random_key_name"  # "random_key_name" is a random existing key name.
        """
        return cast(
            Optional[bytes],
            await self._execute_command(RequestType.RandomKey, [], route),
        )

    async def wait(
        self,
        numreplicas: int,
        timeout: int,
        route: Optional[Route] = None,
    ) -> int:
        """
        Blocks the current client until all the previous write commands are successfully transferred
        and acknowledged by at least `numreplicas` of replicas. If `timeout` is
        reached, the command returns even if the specified number of replicas were not yet reached.

        See [valkey.io](https://valkey.io/commands/wait) for more details.

        Args:
            numreplicas (int): The number of replicas to reach.
            timeout (int): The timeout value specified in milliseconds. A value of 0 will block indefinitely.
            route (Optional[Route]): The command will be routed to all primary nodes, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            int: The number of replicas reached by all the writes performed in the context of the current connection.

        Examples:
            >>> await client.set("key", "value");
            >>> await client.wait(1, 1000);
            # return 1 when a replica is reached or 0 if 1000ms is reached.
        """
        args: List[TEncodable] = [str(numreplicas), str(timeout)]
        return cast(
            int,
            await self._execute_command(RequestType.Wait, args, route),
        )

    async def unwatch(self, route: Optional[Route] = None) -> TOK:
        """
        Flushes all the previously watched keys for a transaction. Executing a transaction will
        automatically flush all previously watched keys.

        See [valkey.io](https://valkey.io/commands/unwatch) for more details.

        Args:
            route (Optional[Route]): The command will be routed to all primary nodes, unless `route` is provided,
                in which case the client will route the command to the nodes defined by `route`.

        Returns:
            TOK: A simple "OK" response.

        Examples:
            >>> await client.unwatch()
                'OK'
        """
        return cast(
            TOK,
            await self._execute_command(RequestType.UnWatch, [], route),
        )

    async def scan(
        self,
        cursor: ClusterScanCursor,
        match: Optional[TEncodable] = None,
        count: Optional[int] = None,
        type: Optional[ObjectType] = None,
        allow_non_covered_slots: bool = False,
    ) -> List[Union[ClusterScanCursor, List[bytes]]]:
        """
        Incrementally iterates over the keys in the cluster.
        The method returns a list containing the next cursor and a list of keys.

        This command is similar to the SCAN command but is designed to work in a cluster environment.
        For each iteration, the new cursor object should be used to continue the scan.
        Using the same cursor object for multiple iterations will result in the same keys or unexpected behavior.
        For more information about the Cluster Scan implementation, see
        [Cluster Scan](https://github.com/valkey-io/valkey-glide/wiki/General-Concepts#cluster-scan).

        Like the SCAN command, the method can be used to iterate over the keys in the database,
        returning all keys the database has from when the scan started until the scan ends.
        The same key can be returned in multiple scan iterations.

        See [valkey.io](https://valkey.io/commands/scan/) for more details.

        Args:
            cursor (ClusterScanCursor): The cursor object that wraps the scan state.
                To start a new scan, create a new empty ClusterScanCursor using ClusterScanCursor().
            match (Optional[TEncodable]): A pattern to match keys against.
            count (Optional[int]): The number of keys to return in a single iteration.
                The actual number returned can vary and is not guaranteed to match this count exactly.
                This parameter serves as a hint to the server on the number of steps to perform in each iteration.
                The default value is 10.
            type (Optional[ObjectType]): The type of object to scan for.
            allow_non_covered_slots (bool): If set to True, the scan will perform even if some slots are not covered by any
                node.
                It's important to note that when set to True, the scan has no guarantee to cover all keys in the cluster,
                and the method loses its way to validate the progress of the scan. Defaults to False.

        Returns:
            List[Union[ClusterScanCursor, List[TEncodable]]]: A list containing the next cursor and a list of keys,
            formatted as [ClusterScanCursor, [key1, key2, ...]].

        Examples:
            >>> # Iterate over all keys in the cluster.
            >>> await client.mset({b'key1': b'value1', b'key2': b'value2', b'key3': b'value3'})
            >>> cursor = ClusterScanCursor()
            >>> all_keys = []
            >>> while not cursor.is_finished():
            >>>     cursor, keys = await client.scan(cursor, count=10, allow_non_covered_slots=False)
            >>>     all_keys.extend(keys)
            >>> print(all_keys)  # [b'key1', b'key2', b'key3']

            >>> # Iterate over keys matching the pattern "*key*".
            >>> await client.mset(
            ...     {
            ...         b"key1": b"value1",
            ...         b"key2": b"value2",
            ...         b"not_my_key": b"value3",
            ...         b"something_else": b"value4"
            ...     }
            ... )
            >>> cursor = ClusterScanCursor()
            >>> all_keys = []
            >>> while not cursor.is_finished():
            >>>     cursor, keys = await client.scan(cursor, match=b"*key*", count=10, allow_non_covered_slots=False)
            >>>     all_keys.extend(keys)
            >>> print(all_keys)  # [b'key1', b'key2', b'not_my_key']

            >>> # Iterate over keys of type STRING.
            >>> await client.mset({b'key1': b'value1', b'key2': b'value2', b'key3': b'value3'})
            >>> await client.sadd(b"this_is_a_set", [b"value4"])
            >>> cursor = ClusterScanCursor()
            >>> all_keys = []
            >>> while not cursor.is_finished():
            >>>     cursor, keys = await client.scan(cursor, type=ObjectType.STRING, allow_non_covered_slots=False)
            >>>     all_keys.extend(keys)
            >>> print(all_keys)  # [b'key1', b'key2', b'key3']
        """
        return cast(
            List[Union[ClusterScanCursor, List[bytes]]],
            await self._cluster_scan(
                cursor=cursor,
                match=match,
                count=count,
                type=type,
                allow_non_covered_slots=allow_non_covered_slots,
            ),
        )

    async def script_exists(
        self, sha1s: List[TEncodable], route: Optional[Route] = None
    ) -> TClusterResponse[List[bool]]:
        """
        Check existence of scripts in the script cache by their SHA1 digest.

        See [valkey.io](https://valkey.io/commands/script-exists) for more details.

        Args:
            sha1s (List[TEncodable]): List of SHA1 digests of the scripts to check.
            route (Optional[Route]): The command will be routed to all primary nodes, unless `route` is provided, in which
            case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TClusterResponse[List[bool]]: A list of boolean values indicating the existence of each script.

        Examples:
            >>> lua_script = Script("return { KEYS[1], ARGV[1] }")
            >>> await client.script_exists([lua_script.get_hash(), "sha1_digest2"])
                [True, False]
        """
        return cast(
            TClusterResponse[List[bool]],
            await self._execute_command(RequestType.ScriptExists, sha1s, route),
        )

    async def script_flush(
        self, mode: Optional[FlushMode] = None, route: Optional[Route] = None
    ) -> TOK:
        """
        Flush the Lua scripts cache.

        See [valkey.io](https://valkey.io/commands/script-flush) for more details.

        Args:
            mode (Optional[FlushMode]): The flushing mode, could be either `SYNC` or `ASYNC`.
            route (Optional[Route]): The command will be routed automatically to all nodes, unless `route` is provided, in
                which case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TOK: A simple `OK` response.

        Examples:
            >>> await client.script_flush()
                "OK"

            >>> await client.script_flush(FlushMode.ASYNC)
                "OK"
        """

        return cast(
            TOK,
            await self._execute_command(
                RequestType.ScriptFlush, [mode.value] if mode else [], route
            ),
        )

    async def script_kill(self, route: Optional[Route] = None) -> TOK:
        """
        Kill the currently executing Lua script, assuming no write operation was yet performed by the script.
        The command is routed to all nodes, and aggregates the response to a single array.

        See [valkey.io](https://valkey.io/commands/script-kill) for more details.

        Args:
            route (Optional[Route]): The command will be routed automatically to all nodes, unless `route` is provided, in
                which case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TOK: A simple `OK` response.

        Examples:
            >>> await client.script_kill()
                "OK"
        """
        return cast(TOK, await self._execute_command(RequestType.ScriptKill, [], route))

    async def invoke_script(
        self,
        script: Script,
        keys: Optional[List[TEncodable]] = None,
        args: Optional[List[TEncodable]] = None,
    ) -> TClusterResponse[TResult]:
        """
        Invokes a Lua script with its keys and arguments.
        This method simplifies the process of invoking scripts on a server by using an object that represents a Lua script.
        The script loading, argument preparation, and execution will all be handled internally.
        If the script has not already been loaded, it will be loaded automatically using the `SCRIPT LOAD` command.
        After that, it will be invoked using the `EVALSHA` command.

        Note:
            When in cluster mode, each `key` must map to the same hash slot.

        See [SCRIPT LOAD](https://valkey.io/commands/script-load/) and [EVALSHA](https://valkey.io/commands/evalsha/)
        for more details.

        Args:
            script (Script): The Lua script to execute.
            keys (Optional[List[TEncodable]]): The keys that are used in the script. To ensure the correct execution of
                the script, all names of keys that a script accesses must be explicitly provided as `keys`.
            args (Optional[List[TEncodable]]): The non-key arguments for the script.

        Returns:
            TResult: a value that depends on the script that was executed.

        Examples:
            >>> lua_script = Script("return { KEYS[1], ARGV[1] }")
            >>> await client.invoke_script(lua_script, keys=["foo"], args=["bar"] );
                [b"foo", b"bar"]
        """
        return await self._execute_script(script.get_hash(), keys, args)

    async def invoke_script_route(
        self,
        script: Script,
        args: Optional[List[TEncodable]] = None,
        route: Optional[Route] = None,
    ) -> TClusterResponse[TResult]:
        """
        Invokes a Lua script with its arguments and route.
        This method simplifies the process of invoking scripts on a server by using an object that represents a Lua script.
        The script loading, argument preparation, and execution will all be handled internally.
        If the script has not already been loaded, it will be loaded automatically using the `SCRIPT LOAD` command.
        After that, it will be invoked using the `EVALSHA` command.

        See [SCRIPT LOAD](https://valkey.io/commands/script-load/) and [EVALSHA](https://valkey.io/commands/evalsha/)
        for more details.

        Args:
            script (Script): The Lua script to execute.
            args (Optional[List[TEncodable]]): The non-key arguments for the script.
            route (Optional[Route]): The command will be routed automatically to a random node, unless `route` is provided, in
                which case the client will route the command to the nodes defined by `route`. Defaults to None.

        Returns:
            TResult: a value that depends on the script that was executed.

        Examples:
            >>> lua_script = Script("return { ARGV[1] }")
            >>> await client.invoke_script(lua_script, args=["bar"], route=AllPrimaries());
                [b"bar"]
        """
        return await self._execute_script(
            script.get_hash(), keys=None, args=args, route=route
        )
