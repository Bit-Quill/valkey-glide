package glide

import (
	"testing"
)

func TestNothing(t *testing.T) {
	someVar := true
	if !someVar {
		t.Fatalf("Expected someVar to be true, but was false.")
	}
}
