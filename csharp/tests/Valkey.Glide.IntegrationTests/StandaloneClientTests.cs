﻿// Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0

using gs = Valkey.Glide.GlideString;
namespace Valkey.Glide.IntegrationTests;

public class StandaloneClientTests
{
    // Syntax sugar - use `Args(a, b, c)` instead of `new GlideString[] { a, b, c }`
    internal static gs[] Args(params gs[] args) => args;

    [Fact]
    public void CustomCommand()
    {
        GlideClient client = TestConfiguration.DefaultStandaloneClient();
        // Assert.Multiple doesn't work with async tasks https://github.com/xunit/xunit/issues/3209
        Assert.Multiple(
            () => Assert.Equal("PONG", client.CustomCommand(Args("ping")).Result!.ToString()),
            () => Assert.Equal("piping", client.CustomCommand(Args("ping", "piping")).Result!.ToString()),
            () => Assert.Contains("# Server", client.CustomCommand(Args("INFO")).Result!.ToString())
        );
    }

    [Fact]
    public async Task CustomCommandWithBinary()
    {
        GlideClient client = TestConfiguration.DefaultStandaloneClient();
        string key1 = Guid.NewGuid().ToString();
        string key2 = Guid.NewGuid().ToString();
        string key3 = Guid.NewGuid().ToString();
        string value = Guid.NewGuid().ToString();
        Assert.Equal("OK", await client.Set(key1, value));

        gs dump = (await client.CustomCommand(Args("DUMP", key1)) as gs)!;

        Assert.Equal("OK".ToGlideString(), await client.CustomCommand(Args("RESTORE", key2, "0", dump!)));
        Assert.Equal(value, (await client.Get(key2))!);

        // Set and get a binary value
        Assert.Equal("OK", await client.Set(key3, dump!));
        Assert.Equal(dump, await client.Get(key3));
    }

    [Fact]
    public void CanConnectWithDifferentParameters()
    {
        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithClientName("GLIDE").Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithTls(false).Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithConnectionTimeout(2000).Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithRequestTimeout(2000).Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithDataBaseId(4).Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithConnectionRetryStrategy(1, 2, 3).Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithAuthentication("default", "").Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithProtocolVersion(ConnectionConfiguration.Protocol.RESP2).Build());

        _ = new GlideClient(TestConfiguration.DefaultClientConfig()
            .WithReadFrom(new ConnectionConfiguration.ReadFrom(ConnectionConfiguration.ReadFromStrategy.Primary)).Build());
    }

    [Fact]
    // Verify that client can handle complex return types, not just strings
    // TODO: remove this test once we add tests with these commands
    public async Task CustomCommandWithDifferentReturnTypes()
    {
        GlideClient client = TestConfiguration.DefaultStandaloneClient();

        string key1 = Guid.NewGuid().ToString();
        Assert.Equal(2, (long)(await client.CustomCommand(Args("hset", key1, "f1", "v1", "f2", "v2")))!);
        Assert.Equal(
            new Dictionary<gs, gs> { { "f1", "v1" }, { "f2", "v2" } },
            await client.CustomCommand(Args("hgetall", key1))
        );
        Assert.Equal(
            new gs?[] { "v1", "v2", null },
            await client.CustomCommand(Args("hmget", key1, "f1", "f2", "f3"))
        );

        string key2 = Guid.NewGuid().ToString();
        Assert.Equal(3, (long)(await client.CustomCommand(Args("sadd", key2, "a", "b", "c")))!);
        Assert.Equal(
            new HashSet<object> { new gs("a"), new gs("b"), new gs("c") },
            (await client.CustomCommand(Args("smembers", key2)) as HashSet<object>)!
        );
        Assert.Equal(
            new bool[] { true, true, false },
            await client.CustomCommand(Args("smismember", key2, "a", "b", "d"))
        );

        string key3 = Guid.NewGuid().ToString();
        _ = await client.CustomCommand(Args("xadd", key3, "0-1", "str-1-id-1-field-1", "str-1-id-1-value-1", "str-1-id-1-field-2", "str-1-id-1-value-2"));
        _ = await client.CustomCommand(Args("xadd", key3, "0-2", "str-1-id-2-field-1", "str-1-id-2-value-1", "str-1-id-2-field-2", "str-1-id-2-value-2"));
        _ = Assert.IsType<Dictionary<gs, object?>>((await client.CustomCommand(Args("xread", "streams", key3, "stream", "0-1", "0-2")))!);
        _ = Assert.IsType<Dictionary<gs, object?>>((await client.CustomCommand(Args("xinfo", "stream", key3, "full")))!);
    }
}
