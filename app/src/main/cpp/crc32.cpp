#include "crc32.h"
#include <array>
#include <cstdint>

namespace opou {

namespace {
constexpr std::array<uint32_t, 256> buildTable() {
    std::array<uint32_t, 256> table{};
    for (uint32_t i = 0; i < 256; ++i) {
        uint32_t c = i;
        for (int k = 0; k < 8; ++k) {
            c = (c & 1) ? (0xEDB88320u ^ (c >> 1)) : (c >> 1);
        }
        table[i] = c;
    }
    return table;
}

const std::array<uint32_t, 256> kCrcTable = buildTable();
}  // namespace

uint32_t crc32(const unsigned char* data, size_t length) {
    uint32_t crc = 0xFFFFFFFFu;
    for (size_t i = 0; i < length; ++i) {
        const uint8_t index = static_cast<uint8_t>((crc ^ data[i]) & 0xFFu);
        crc = kCrcTable[index] ^ (crc >> 8);
    }
    return crc ^ 0xFFFFFFFFu;
}

}  // namespace opou
