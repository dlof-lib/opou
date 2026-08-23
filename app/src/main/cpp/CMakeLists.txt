cmake_minimum_required(VERSION 3.22.1)
project("opou_native")

add_library(
    opou_native
    SHARED
    native-lib.cpp
    shaabiya.cpp
    crc32.cpp
    base64.cpp
)

find_library(log-lib log)

target_link_libraries(
    opou_native
    ${log-lib}
)

target_compile_options(opou_native PRIVATE -Wall -Wextra -O2)
