package api

import (
	"fmt"

	"github.com/valkey-io/valkey-glide/go/glide/api/options"
)

func ExampleGlideClient_SAdd() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set", []string{"member1", "member2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)

	// Output: 2
}

func ExampleGlideClient_SRem() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SRem("my_set", []string{"member1", "member2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)

	// Output: 2
}

func ExampleGlideClient_SMembers() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set", []string{"member1", "member2"})
	result1, err := client.SMembers("my_set")
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)

	// Output:
	// 2
	// map[member1:{} member2:{}]
}

func ExampleGlideClient_SCard() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set", []string{"member1", "member2"})
	result1, err := client.SCard("my_set")
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)

	// Output:
	// 2
	// 2
}

func ExampleGlideClient_SIsMember() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set", []string{"member1", "member2"})
	result1, err := client.SIsMember("my_set", "member2")
	result2, err := client.SIsMember("my_set", "nonExistentMember")
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// true
	// false
}

func ExampleGlideClient_SDiff() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	result1, err := client.SAdd("my_set2", []string{"member2", "member3"})
	result2, err := client.SDiff([]string{"my_set1", "my_set2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// 2
	// map[member1:{}]
}

func ExampleGlideClient_SDiffStore() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	result1, err := client.SAdd("my_set2", []string{"member2", "member3"})
	result2, err := client.SDiffStore("my_set3", []string{"my_set1", "my_set2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// 2
	// 1
}

func ExampleGlideClient_SInter() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	result1, err := client.SAdd("my_set2", []string{"member2", "member3"})
	result2, err := client.SInter([]string{"my_set1", "my_set2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// 2
	// map[member2:{}]
}

func ExampleGlideClient_SInterStore() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	result1, err := client.SAdd("my_set2", []string{"member2", "member3"})
	result2, err := client.SInterStore("my_set3", []string{"my_set1", "my_set2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// 2
	// 1
}

func ExampleGlideClient_SInterCard() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	result1, err := client.SAdd("my_set2", []string{"member2", "member3"})
	result2, err := client.SInterCard([]string{"my_set1", "my_set2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// 2
	// 1
}

func ExampleGlideClient_SInterCardLimit() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2", "member3", "member4"})
	result1, err := client.SAdd("my_set2", []string{"member2", "member3", "member4", "member5"})
	result2, err := client.SInterCardLimit([]string{"my_set1", "my_set2"}, 2)
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 4
	// 4
	// 2
}

func ExampleGlideClient_SRandMember() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	// Assume we did SAdd and added more than one value. But for the purposes of this test
	// we'll only add one member to get a consistent output.
	result, err := client.SAdd("my_set", []string{"member1"}) // picks a random element from my_set
	result1, err := client.SRandMember("my_set")
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)

	// Output:
	// 1
	// {member1 false}
}

func ExampleGlideClient_SPop() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set", []string{"member1"})
	result1, err := client.SPop("my_set")
	result2, err := client.SPop("non_existent_set")
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2.IsNil())

	// Output:
	// 1
	// {member1 false}
	// true
}

func ExampleGlideClient_SMIsMember() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set", []string{"member1", "member2", "member3"})
	result1, err := client.SMIsMember("my_set", []string{"member1", "member2", "member4"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)

	// Output:
	// 3
	// [true true false]
}

func ExampleGlideClient_SUnionStore() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2", "member3"})
	result1, err := client.SAdd("my_set2", []string{"member4", "member5", "member6"})
	result2, err := client.SUnionStore("my_set3", []string{"my_set1", "my_set2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 3
	// 3
	// 6
}

func ExampleGlideClient_SUnion() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	result1, err := client.SAdd("my_set2", []string{"member3", "member4"})
	result2, err := client.SUnion([]string{"my_set1", "my_set2"})
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// 2
	// map[member1:{} member2:{} member3:{} member4:{}]
}

func ExampleGlideClient_SScan() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	resCursor, resCol, err := client.SScan("my_set1", "0")
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(resCursor)
	fmt.Println(resCol)

	// Output:
	// 2
	// 0
	// [member1 member2]
}

func ExampleGlideClient_SScanWithOptions() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	resCursor, resCol, err := client.SScanWithOptions("my_set1", "0", options.NewBaseScanOptionsBuilder().SetMatch("*"))
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(resCursor)
	fmt.Println(resCol)

	// Output:
	// 2
	// 0
	// [member1 member2]
}

func ExampleGlideClient_SMove() {
	var client *GlideClient = getExampleGlideClient() // example helper function

	result, err := client.SAdd("my_set1", []string{"member1", "member2"})
	result1, err := client.SAdd("my_set2", []string{"member3", "member4"})
	result2, err := client.SMove("my_set2", "my_set1", "member3")
	if err != nil {
		fmt.Println("Glide example failed with an error: ", err)
	}
	fmt.Println(result)
	fmt.Println(result1)
	fmt.Println(result2)

	// Output:
	// 2
	// 2
	// true
}
