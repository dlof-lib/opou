#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace opou {

/**
 * ترميز/فك ترميز Base64 عالي الأداء (جدول بحث + معالجة بالكتل).
 * مصمم للتعامل مع صور مضغوطة (حتى عدة ميغابايت) بأقل استهلاك ذاكرة ووقت ممكن،
 * مناسب للتشغيل المتكرر عند كل نشر فقرة أو تحديث صورة شخصية.
 */
std::string base64Encode(const uint8_t* data, size_t length);
std::vector<uint8_t> base64Decode(const std::string& encoded);

}  // namespace opou
