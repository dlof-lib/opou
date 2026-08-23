#include "base64.h"
#include <array>
#include <stdexcept>

namespace opou {

namespace {

constexpr char kEncodeTable[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789+/";

// جدول فك ترميز مبني مسبقًا (O(1) لكل بايت بدلًا من البحث الخطي)
constexpr std::array<int8_t, 256> buildDecodeTable() {
    std::array<int8_t, 256> table{};
    for (auto& v : table) v = -1;
    for (int i = 0; i < 64; ++i) {
        table[static_cast<uint8_t>(kEncodeTable[i])] = static_cast<int8_t>(i);
    }
    table[static_cast<uint8_t>('=')] = -2; // علامة حشو (padding)
    return table;
}

const std::array<int8_t, 256> kDecodeTable = buildDecodeTable();

}  // namespace

std::string base64Encode(const uint8_t* data, size_t length) {
    if (data == nullptr || length == 0) return {};

    std::string out;
    out.reserve(((length + 2) / 3) * 4);

    size_t i = 0;
    while (i + 3 <= length) {
        const uint32_t chunk = (static_cast<uint32_t>(data[i]) << 16) |
                                (static_cast<uint32_t>(data[i + 1]) << 8) |
                                static_cast<uint32_t>(data[i + 2]);
        out.push_back(kEncodeTable[(chunk >> 18) & 0x3F]);
        out.push_back(kEncodeTable[(chunk >> 12) & 0x3F]);
        out.push_back(kEncodeTable[(chunk >> 6) & 0x3F]);
        out.push_back(kEncodeTable[chunk & 0x3F]);
        i += 3;
    }

    const size_t remaining = length - i;
    if (remaining == 1) {
        const uint32_t chunk = static_cast<uint32_t>(data[i]) << 16;
        out.push_back(kEncodeTable[(chunk >> 18) & 0x3F]);
        out.push_back(kEncodeTable[(chunk >> 12) & 0x3F]);
        out.push_back('=');
        out.push_back('=');
    } else if (remaining == 2) {
        const uint32_t chunk = (static_cast<uint32_t>(data[i]) << 16) |
                                (static_cast<uint32_t>(data[i + 1]) << 8);
        out.push_back(kEncodeTable[(chunk >> 18) & 0x3F]);
        out.push_back(kEncodeTable[(chunk >> 12) & 0x3F]);
        out.push_back(kEncodeTable[(chunk >> 6) & 0x3F]);
        out.push_back('=');
    }

    return out;
}

std::vector<uint8_t> base64Decode(const std::string& encoded) {
    std::vector<uint8_t> out;
    out.reserve((encoded.size() / 4) * 3 + 3);

    int32_t buffer = 0;
    int bits = 0;

    for (const char c : encoded) {
        if (c == '\n' || c == '\r' || c == ' ') continue; // تسامح مع فواصل الأسطر
        const int8_t value = kDecodeTable[static_cast<uint8_t>(c)];
        if (value == -2) break;      // نهاية عند الحشو
        if (value == -1) continue;   // تجاهل أي رمز غير صالح بدل رمي استثناء (قوة/تسامح)

        buffer = (buffer << 6) | value;
        bits += 6;
        if (bits >= 8) {
            bits -= 8;
            out.push_back(static_cast<uint8_t>((buffer >> bits) & 0xFF));
        }
    }

    return out;
}

}  // namespace opou
