#pragma once
#include <cstdint>
#include <cstddef>

namespace opou {
int64_t computeShaabiyaScore(int32_t likes, int32_t dislikes, int32_t teks, int32_t comments);
int64_t reactionDelta(int32_t oldType, int32_t newType);
uint64_t fastByteHash(const uint8_t* data, size_t length);
bool validateImageBytes(const uint8_t* data, size_t length);
}
