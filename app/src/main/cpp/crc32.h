#pragma once
#include <cstddef>
#include <cstdint>

namespace opou {
uint32_t crc32(const unsigned char* data, size_t length);
}
