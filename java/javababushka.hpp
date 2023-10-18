#include <cstdarg>
#include <cstdint>
#include <cstdlib>
#include <ostream>
#include <new>

struct BabushkaResultStr {
  const char *error;
  const char *result;
};

struct BabushkaValue {
  const char *str;
  int64_t num;
};

struct BabushkaResult {
  const char *error;
  uint8_t value_type;
  BabushkaValue value;
};

extern "C" {

BabushkaResultStr static_function_which_throws();

BabushkaResultStr static_function4();

BabushkaResult static_function2_0();

BabushkaResult static_function2_1();

BabushkaResult static_function2_2();

BabushkaResult static_function2_3();

BabushkaResult static_function2_4();

} // extern "C"
