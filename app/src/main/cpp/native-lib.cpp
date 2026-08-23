#include <jni.h>
#include <string>
#include "shaabiya.h"
#include "crc32.h"
#include "base64.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_OPEN_OU_util_NativeBridge_computeShaabiyaScoreNative(
        JNIEnv* /*env*/, jobject /*thiz*/,
        jint likes, jint dislikes, jint teks, jint comments) {
    return static_cast<jlong>(opou::computeShaabiyaScore(likes, dislikes, teks, comments));
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_OPEN_OU_util_NativeBridge_fingerprintNative(
        JNIEnv* env, jobject /*thiz*/, jstring content) {
    if (content == nullptr) return 0;
    const char* chars = env->GetStringUTFChars(content, nullptr);
    const jsize byteLen = env->GetStringUTFLength(content);
    const uint32_t crc = opou::crc32(reinterpret_cast<const unsigned char*>(chars),
                                      static_cast<size_t>(byteLen));
    env->ReleaseStringUTFChars(content, chars);
    return static_cast<jlong>(crc);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_OPEN_OU_util_NativeBridge_encodeBase64Native(
        JNIEnv* env, jobject /*thiz*/, jbyteArray data) {
    if (data == nullptr) return env->NewStringUTF("");
    const jsize len = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);

    const std::string encoded = opou::base64Encode(
            reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(len));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return env->NewStringUTF(encoded.c_str());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_OPEN_OU_util_NativeBridge_decodeBase64Native(
        JNIEnv* env, jobject /*thiz*/, jstring encoded) {
    if (encoded == nullptr) return env->NewByteArray(0);
    const char* chars = env->GetStringUTFChars(encoded, nullptr);
    std::string input(chars);
    env->ReleaseStringUTFChars(encoded, chars);

    const std::vector<uint8_t> decoded = opou::base64Decode(input);

    jbyteArray result = env->NewByteArray(static_cast<jsize>(decoded.size()));
    if (!decoded.empty()) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(decoded.size()),
                                 reinterpret_cast<const jbyte*>(decoded.data()));
    }
    return result;
}
