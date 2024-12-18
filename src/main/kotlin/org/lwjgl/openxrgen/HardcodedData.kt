package org.lwjgl.openxrgen

// Character sequence used for alignment
internal const val t = "    "

internal const val S = "\$"
internal const val QUOTES3 = "\"\"\""

internal const val HEADER = """/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 * MACHINE GENERATED FILE, DO NOT EDIT
 */
"""

internal val VERSION_HISTORY = mapOf<String, String>(
    //
)

internal val DISABLED_EXTENSIONS = setOf(
    "XR_KHR_D3D11_enable",
    "XR_KHR_D3D12_enable",
    "XR_KHR_android_create_instance",
    "XR_KHR_android_surface_swapchain",
    "XR_KHR_android_thread_settings",
    "XR_KHR_loader_init_android",
    "XR_KHR_metal_enable",
    "XR_KHR_opengl_es_enable",
    "XR_FB_android_surface_swapchain_create",
    "XR_FB_swapchain_update_state_android_surface",
    "XR_OCULUS_android_session_state_enable"
)

internal val EXTENSION_TOKEN_REPLACEMENTS = mapOf(
    "egl" to "EGL",
    "es" to "ES",
    "hp" to "HP",
    "opengl" to "OpenGL",
    "uuid" to "UUIUD"
)

internal val IMPORTS = mapOf(
    "EGL/egl.h" to Import("egl.*", "org.lwjgl.egl.*"),
    "GL/glxext.h" to Import("opengl.*", "org.lwjgl.opengl.*"),
    "vulkan/vulkan.h" to Import("vulkan.*", "org.lwjgl.vulkan.*"),
    "windows.h" to Import("core.windows.*", "org.lwjgl.system.windows.*"),
    "X11/Xlib.h" to Import("core.linux.*", "org.lwjgl.system.linux.*")
)

internal val SYSTEM_STRUCTS = mapOf(
    "IUnknown" to TypeStruct("struct", "IUnknown", false, null, emptyList(), null, null),
    "LARGE_INTEGER" to TypeStruct("union", "LARGE_INTEGER", false, null, emptyList(), null, null),
    "timespec" to TypeStruct("struct", "struct timespec", false, null, emptyList(), null, null)
)

internal val OPAQUE_PFN_TYPES = setOf(
    "PFN_xrVoidFunction",
    "PFN_xrEglGetProcAddressMNDX"
)

internal fun configAPIConstantImports(enumClassMap: MutableMap<String, String>) {
    enumClassMap["XR_MAX_EXTENSION_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_API_LAYER_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_API_LAYER_DESCRIPTION_SIZE"] = "XR10"
    enumClassMap["XR_MAX_SYSTEM_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_APPLICATION_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_ENGINE_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_RUNTIME_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_PATH_LENGTH"] = "XR10"
    enumClassMap["XR_MAX_STRUCTURE_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_RESULT_STRING_SIZE"] = "XR10"
    enumClassMap["XR_MAX_GRAPHICS_APIS_SUPPORTED"] = "XR10"
    enumClassMap["XR_MAX_ACTION_SET_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_ACTION_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_LOCALIZED_ACTION_SET_NAME_SIZE"] = "XR10"
    enumClassMap["XR_MAX_LOCALIZED_ACTION_NAME_SIZE"] = "XR10"
    enumClassMap["XR_UUID_SIZE"] = "XR10"
    enumClassMap["XR_MIN_COMPOSITION_LAYERS_SUPPORTED"] = "XR10"
    enumClassMap["XR_API_LAYER_MAX_SETTINGS_PATH_SIZE"] = "XRLoader10"
}
