/**
 * JNI wrapper for the Go tls-client shared library.
 * Build this with the Android NDK (or your platform toolchain), then load the Go .so first:
 *   System.loadLibrary("tls_client_go");
 *   System.loadLibrary("tls_client_jni");
 *
 * The Go library must export C symbols: request, destroySession, getCookiesFromSession, destroyAll.
 * (Build the Go tls-client with -buildmode=c-shared and use the generated .so.)
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>

static char* (*go_request)(const char*) = NULL;
static char* (*go_destroy_session)(const char*) = NULL;
static char* (*go_get_cookies)(const char*) = NULL;
static char* (*go_destroy_all)(void) = NULL;

static int load_go_symbols(void) {
    if (go_request != NULL) return 1;
    void* h = dlopen("libtls_client_go.so", RTLD_NOW | RTLD_GLOBAL);
    if (!h) h = dlopen(NULL, RTLD_LAZY);  /* try current process (if Go .so loaded first) */
    if (!h) return 0;
    go_request = (char* (*)(const char*)) dlsym(h, "request");
    go_destroy_session = (char* (*)(const char*)) dlsym(h, "destroySession");
    go_get_cookies = (char* (*)(const char*)) dlsym(h, "getCookiesFromSession");
    go_destroy_all = (char* (*)(void)) dlsym(h, "destroyAll");
    return (go_request && go_destroy_session && go_get_cookies && go_destroy_all) ? 1 : 0;
}

static jstring copy_to_java(JNIEnv* env, const char* cstr) {
    if (!cstr) return NULL;
    jstring jstr = (*env)->NewStringUTF(env, cstr);
    return jstr;
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeRequest(JNIEnv* env, jobject thiz, jstring request_json) {
    if (!load_go_symbols() || !request_json) return NULL;
    const char* in = (*env)->GetStringUTFChars(env, request_json, NULL);
    if (!in) return NULL;
    char* out = go_request(in);
    (*env)->ReleaseStringUTFChars(env, request_json, in);
    jstring result = copy_to_java(env, out);
    /* Do not free(out): Go tls-client owns it; use freeMemory(id) if exposed */
    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeDestroySession(JNIEnv* env, jobject thiz, jstring payload_json) {
    if (!load_go_symbols() || !payload_json) return NULL;
    const char* in = (*env)->GetStringUTFChars(env, payload_json, NULL);
    if (!in) return NULL;
    char* out = go_destroy_session(in);
    (*env)->ReleaseStringUTFChars(env, payload_json, in);
    jstring result = copy_to_java(env, out);
    if (out) free(out);
    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeGetCookiesFromSession(JNIEnv* env, jobject thiz, jstring payload_json) {
    if (!load_go_symbols() || !payload_json) return NULL;
    const char* in = (*env)->GetStringUTFChars(env, payload_json, NULL);
    if (!in) return NULL;
    char* out = go_get_cookies(in);
    (*env)->ReleaseStringUTFChars(env, payload_json, in);
    jstring result = copy_to_java(env, out);
    if (out) free(out);
    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeDestroyAll(JNIEnv* env, jobject thiz) {
    if (!load_go_symbols()) return NULL;
    char* out = go_destroy_all();
    jstring result = copy_to_java(env, out);
    if (out) free(out);
    return result;
}
