/**
 * JNI wrapper for the Go tls-client shared library.
 * Supports Linux/Android (dlopen), macOS (dlopen), and Windows (LoadLibrary).
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#  include <windows.h>
   typedef HMODULE dl_handle_t;
   static dl_handle_t dl_open_lib(const char* name) { return LoadLibraryA(name); }
   static void*       dl_resolve(dl_handle_t h, const char* sym) { return (void*)GetProcAddress(h, sym); }
   static dl_handle_t dl_self(void) { return GetModuleHandleA(NULL); }
#else
#  include <dlfcn.h>
   typedef void* dl_handle_t;
   static dl_handle_t dl_open_lib(const char* name) { return dlopen(name, RTLD_NOW | RTLD_GLOBAL); }
   static void*       dl_resolve(dl_handle_t h, const char* sym) { return dlsym(h, sym); }
   static dl_handle_t dl_self(void) { return dlopen(NULL, RTLD_LAZY); }
#endif

static char* (*go_request)(const char*)          = NULL;
static char* (*go_destroy_session)(const char*)  = NULL;
static char* (*go_get_cookies)(const char*)      = NULL;
static char* (*go_destroy_all)(void)             = NULL;

static int load_go_symbols(void) {
    if (go_request != NULL) return 1;

#ifdef _WIN32
    const char* lib_names[] = { "tls_client_go.dll", NULL };
#elif defined(__APPLE__)
    const char* lib_names[] = { "libtls_client_go.dylib", NULL };
#else
    const char* lib_names[] = { "libtls_client_go.so", NULL };
#endif

    dl_handle_t h = NULL;
    for (int i = 0; lib_names[i] != NULL && !h; i++)
        h = dl_open_lib(lib_names[i]);
    if (!h) h = dl_self(); /* try current process (Go lib already loaded) */
    if (!h) return 0;

    go_request         = (char* (*)(const char*)) dl_resolve(h, "request");
    go_destroy_session = (char* (*)(const char*)) dl_resolve(h, "destroySession");
    go_get_cookies     = (char* (*)(const char*)) dl_resolve(h, "getCookiesFromSession");
    go_destroy_all     = (char* (*)(void))        dl_resolve(h, "destroyAll");

    return (go_request && go_destroy_session && go_get_cookies && go_destroy_all) ? 1 : 0;
}

static jstring to_jstring(JNIEnv* env, const char* s) {
    return s ? (*env)->NewStringUTF(env, s) : NULL;
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeRequest(JNIEnv* env, jobject thiz, jstring req) {
    if (!load_go_symbols() || !req) return NULL;
    const char* in = (*env)->GetStringUTFChars(env, req, NULL);
    char* out = go_request(in);
    (*env)->ReleaseStringUTFChars(env, req, in);
    return to_jstring(env, out);
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeDestroySession(JNIEnv* env, jobject thiz, jstring payload) {
    if (!load_go_symbols() || !payload) return NULL;
    const char* in = (*env)->GetStringUTFChars(env, payload, NULL);
    char* out = go_destroy_session(in);
    (*env)->ReleaseStringUTFChars(env, payload, in);
    jstring result = to_jstring(env, out);
    if (out) free(out);
    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeGetCookiesFromSession(JNIEnv* env, jobject thiz, jstring payload) {
    if (!load_go_symbols() || !payload) return NULL;
    const char* in = (*env)->GetStringUTFChars(env, payload, NULL);
    char* out = go_get_cookies(in);
    (*env)->ReleaseStringUTFChars(env, payload, in);
    jstring result = to_jstring(env, out);
    if (out) free(out);
    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_kotlintls_NativeTlsEngine_nativeDestroyAll(JNIEnv* env, jobject thiz) {
    if (!load_go_symbols()) return NULL;
    char* out = go_destroy_all();
    jstring result = to_jstring(env, out);
    if (out) free(out);
    return result;
}
