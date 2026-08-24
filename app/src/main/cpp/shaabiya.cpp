#include "shaabiya.h"

namespace opou {

int64_t computeShaabiyaScore(int32_t likes, int32_t dislikes, int32_t teks, int32_t comments) {
    return static_cast<int64_t>(likes) * 3 +
           static_cast<int64_t>(teks) * 5 +
           static_cast<int64_t>(comments) -
           static_cast<int64_t>(dislikes);
}

int64_t reactionDelta(int32_t oldType, int32_t newType) {
    auto weight = [](int32_t type) -> int64_t {
        if (type == 1) return 3;       // LIKE
        if (type == 2) return -1;      // DISLIKE
        return 0;                      // NONE
    };
    return weight(newType) - weight(oldType);
}

uint64_t fastByteHash(const uint8_t* data, size_t length) {
    uint64_t h = 1469598103934665603ULL;
    for (size_t i = 0; i < length; ++i) {
        h ^= data[i];
        h *= 1099511628211ULL;
    }
    return h;
}

bool validateImageBytes(const uint8_t* d, size_t n) {
    if (!d || n < 8) return false;
    const bool png = n >= 8 && d[0] == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
    const bool jpg = n >= 2 && d[0] == 0xFF && d[1] == 0xD8;
    const bool webp = n >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F' &&
                      d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
    return png || jpg || webp;
}

} // namespace opou
