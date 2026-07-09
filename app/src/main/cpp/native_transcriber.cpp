#include <jni.h>

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <ctime>
#include <fstream>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

#include <sys/stat.h>

#include <android/log.h>

#include "whisper.h"

namespace {

constexpr const char * LogTag = "LazyJournalWhisper";
using Clock = std::chrono::steady_clock;

struct WavAudio {
    int sample_rate = 0;
    int channels = 0;
    int bits_per_sample = 0;
    std::vector<float> pcm;
};

struct ModelSignature {
    std::string path;
    off_t size = 0;
    time_t modified_at = 0;
};

std::mutex g_whisper_mutex;
whisper_context * g_cached_context = nullptr;
ModelSignature g_cached_model;

long long elapsed_ms(Clock::time_point started_at) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        Clock::now() - started_at
    ).count();
}

uint16_t read_u16_le(const uint8_t * data) {
    return static_cast<uint16_t>(data[0] | (data[1] << 8));
}

uint32_t read_u32_le(const uint8_t * data) {
    return static_cast<uint32_t>(
        data[0] |
        (data[1] << 8) |
        (data[2] << 16) |
        (data[3] << 24)
    );
}

bool chunk_id_equals(const uint8_t * data, const char * id) {
    return data[0] == id[0] && data[1] == id[1] && data[2] == id[2] && data[3] == id[3];
}

WavAudio read_wav_file(const std::string & path) {
    std::ifstream input(path, std::ios::binary);
    if (!input) {
        throw std::runtime_error("Could not open recorded audio file.");
    }

    input.seekg(0, std::ios::end);
    const auto file_size = input.tellg();
    input.seekg(0, std::ios::beg);

    if (file_size < 44) {
        throw std::runtime_error("Recorded audio file is too small.");
    }

    std::vector<uint8_t> bytes(static_cast<size_t>(file_size));
    input.read(reinterpret_cast<char *>(bytes.data()), file_size);

    if (!chunk_id_equals(bytes.data(), "RIFF") || !chunk_id_equals(bytes.data() + 8, "WAVE")) {
        throw std::runtime_error("Recorded audio is not a WAV file.");
    }

    WavAudio audio;
    size_t offset = 12;
    size_t data_offset = 0;
    size_t data_size = 0;

    while (offset + 8 <= bytes.size()) {
        const uint8_t * chunk = bytes.data() + offset;
        const uint32_t chunk_size = read_u32_le(chunk + 4);
        const size_t next_offset = offset + 8 + chunk_size + (chunk_size % 2);

        if (next_offset > bytes.size() + 1) {
            throw std::runtime_error("Recorded WAV file is corrupt.");
        }

        if (chunk_id_equals(chunk, "fmt ")) {
            if (chunk_size < 16) {
                throw std::runtime_error("Recorded WAV format chunk is invalid.");
            }
            const uint16_t audio_format = read_u16_le(chunk + 8);
            audio.channels = read_u16_le(chunk + 10);
            audio.sample_rate = static_cast<int>(read_u32_le(chunk + 12));
            audio.bits_per_sample = read_u16_le(chunk + 22);

            if (audio_format != 1) {
                throw std::runtime_error("Only PCM WAV audio is supported.");
            }
        } else if (chunk_id_equals(chunk, "data")) {
            data_offset = offset + 8;
            data_size = chunk_size;
        }

        offset = next_offset;
    }

    if (audio.sample_rate != 16000 || audio.channels != 1 || audio.bits_per_sample != 16) {
        throw std::runtime_error("Recorded audio must be 16 kHz mono 16-bit PCM WAV.");
    }
    if (data_offset == 0 || data_size == 0) {
        throw std::runtime_error("Recorded WAV file has no audio samples.");
    }

    const size_t sample_count = data_size / 2;
    audio.pcm.reserve(sample_count);
    for (size_t i = 0; i < sample_count; ++i) {
        const size_t sample_offset = data_offset + (i * 2);
        const int16_t sample = static_cast<int16_t>(read_u16_le(bytes.data() + sample_offset));
        audio.pcm.push_back(static_cast<float>(sample) / 32768.0f);
    }

    return audio;
}

ModelSignature read_model_signature(const std::string & path) {
    struct stat model_stat {};
    if (stat(path.c_str(), &model_stat) != 0) {
        throw std::runtime_error("Could not inspect Whisper model file.");
    }
    if (model_stat.st_size <= 0) {
        throw std::runtime_error("Whisper model file is empty.");
    }

    return ModelSignature {
        path,
        model_stat.st_size,
        model_stat.st_mtime
    };
}

bool is_cached_model(const ModelSignature & signature) {
    return g_cached_context != nullptr &&
        g_cached_model.path == signature.path &&
        g_cached_model.size == signature.size &&
        g_cached_model.modified_at == signature.modified_at;
}

void clear_cached_model() {
    if (g_cached_context != nullptr) {
        whisper_free(g_cached_context);
        g_cached_context = nullptr;
        g_cached_model = ModelSignature {};
    }
}

whisper_context * get_cached_context(const std::string & model_path) {
    const ModelSignature signature = read_model_signature(model_path);
    if (is_cached_model(signature)) {
        __android_log_print(
            ANDROID_LOG_INFO,
            LogTag,
            "Whisper model cache hit bytes=%lld",
            static_cast<long long>(signature.size)
        );
        return g_cached_context;
    }

    clear_cached_model();

    const auto load_started_at = Clock::now();
    __android_log_print(
        ANDROID_LOG_INFO,
        LogTag,
        "Whisper model load started bytes=%lld",
        static_cast<long long>(signature.size)
    );

    whisper_context_params context_params = whisper_context_default_params();
    context_params.use_gpu = false;

    g_cached_context = whisper_init_from_file_with_params(
        model_path.c_str(),
        context_params
    );
    if (g_cached_context == nullptr) {
        throw std::runtime_error("Could not load Whisper model.");
    }

    g_cached_model = signature;
    __android_log_print(
        ANDROID_LOG_INFO,
        LogTag,
        "Whisper model load finished elapsedMs=%lld",
        elapsed_ms(load_started_at)
    );
    return g_cached_context;
}

std::string jstring_to_string(JNIEnv * env, jstring value) {
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        throw std::runtime_error("Could not read string from Java.");
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

void throw_java_error(JNIEnv * env, const std::string & message) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message.c_str());
    }
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_lazyjournal_app_data_transcription_WhisperCppTranscriber_nativeTranscribe(
    JNIEnv * env,
    jobject,
    jstring model_path,
    jstring audio_path
) {
    try {
        const std::string model = jstring_to_string(env, model_path);
        const std::string audio_file = jstring_to_string(env, audio_path);
        const WavAudio audio = read_wav_file(audio_file);
        const double audio_seconds = static_cast<double>(audio.pcm.size()) / 16000.0;

        const auto total_started_at = Clock::now();
        __android_log_print(
            ANDROID_LOG_INFO,
            LogTag,
            "Native transcription started samples=%zu durationSec=%.2f",
            audio.pcm.size(),
            audio_seconds
        );

        std::lock_guard<std::mutex> lock(g_whisper_mutex);
        whisper_context * context = get_cached_context(model);
        whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        const unsigned int hardware_threads = std::thread::hardware_concurrency();
        params.n_threads = static_cast<int>(std::max(1u, std::min(4u, hardware_threads)));
        params.language = "en";
        params.translate = false;
        params.no_context = true;
        params.no_timestamps = true;
        params.print_progress = false;
        params.print_realtime = false;
        params.print_timestamps = false;
        params.single_segment = false;

        const auto inference_started_at = Clock::now();
        const int result = whisper_full(
            context,
            params,
            audio.pcm.data(),
            static_cast<int>(audio.pcm.size())
        );
        if (result != 0) {
            throw std::runtime_error("Whisper transcription failed.");
        }
        const long long inference_elapsed_ms = elapsed_ms(inference_started_at);

        std::ostringstream transcript;
        const int segment_count = whisper_full_n_segments(context);
        for (int i = 0; i < segment_count; ++i) {
            transcript << whisper_full_get_segment_text(context, i);
        }

        const std::string output = transcript.str();
        __android_log_print(
            ANDROID_LOG_INFO,
            LogTag,
            "Native transcription finished elapsedMs=%lld inferenceMs=%lld segments=%d chars=%zu",
            elapsed_ms(total_started_at),
            inference_elapsed_ms,
            segment_count,
            output.size()
        );
        return env->NewStringUTF(output.c_str());
    } catch (const std::exception & exception) {
        __android_log_print(
            ANDROID_LOG_ERROR,
            LogTag,
            "Native transcription failed: %s",
            exception.what()
        );
        throw_java_error(env, exception.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *, void *) {
    std::lock_guard<std::mutex> lock(g_whisper_mutex);
    clear_cached_model();
}
