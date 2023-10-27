#include <cstdarg>
#include <cstdint>
#include <cstdlib>
#include <ostream>
#include <new>

struct BabushkaResultStr {
  const char *error;
  const char *result;
};

struct BabushkaResult {
  const char *error;
  uint32_t value_type;
  const char *str;
  int64_t num;
};

extern "C" {

BabushkaResultStr static_function_which_throws();

BabushkaResultStr static_function4();

BabushkaResult static_function2_0();

BabushkaResult static_function2_1();

BabushkaResult static_function2_2();

BabushkaResult static_function2_3();

BabushkaResult static_function2_4();

void rust_ctor();

uint64_t init_client0(int32_t data);

BabushkaResult test0(uint64_t ptr, const char *address);

BabushkaResult connect0(uint64_t ptr, const char *address);

BabushkaResult set0(uint64_t ptr, const char *key, const char *value);

BabushkaResult get0(uint64_t ptr, const char *key);

} // extern "C"
