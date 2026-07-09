cmake_minimum_required(VERSION 3.22.1)

project(lazyjournal_whisper)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS_3RD_PARTY OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

add_subdirectory(
    ../../../../third_party/whisper.cpp
    ${CMAKE_BINARY_DIR}/whisper.cpp
)

add_library(lazyjournal_whisper SHARED native_transcriber.cpp)

target_compile_features(lazyjournal_whisper PRIVATE cxx_std_17)

target_link_libraries(
    lazyjournal_whisper
    whisper
    android
    log
)
