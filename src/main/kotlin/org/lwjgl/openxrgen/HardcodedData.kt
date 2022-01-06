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
    "XR_FB_android_surface_swapchain_create",
    "XR_FB_swapchain_update_state_android_surface",
    "XR_OCULUS_android_session_state_enable"
)

internal val EXTENSION_TOKEN_REPLACEMENTS = mapOf(
    "hp" to "HP",
    "opengl" to "OpenGL"
)

internal val IMPORTS = mapOf(
    "EGL/egl.h" to Import("egl.*", "org.lwjgl.egl.*"),
    "GL/glxext.h" to Import("opengl.*", "org.lwjgl.opengl.*"),
    "vulkan/vulkan.h" to Import("vulkan.*", "org.lwjgl.vulkan.*"),
    "windows.h" to Import("core.windows.*", "org.lwjgl.system.windows.*"),
    "X11/Xlib.h" to Import("core.linux.*", "org.lwjgl.system.linux.*")
)

internal val SYSTEM_STRUCTS = mapOf(
    "IUnknown" to TypeStruct("struct", "IUnknown", false, null, emptyList(), null),
    "LARGE_INTEGER" to TypeStruct("union", "LARGE_INTEGER", false, null, emptyList(), null),
    "timespec" to TypeStruct("struct", "struct timespec", false, null, emptyList(), null)
)

internal val MACROS = setOf(
    "MAKE_VERSION",
    "VERSION_MAJOR",
    "VERSION_MINOR",
    "VERSION_PATCH"
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
    enumClassMap["XR_MIN_COMPOSITION_LAYERS_SUPPORTED"] = "XR10"
}

private val UNNAMED_XREFS = setOf<String>(
    //
)
internal fun hasUnnamedXREF(section: String) = UNNAMED_XREFS.contains(section)

private val SECTION_XREFS = mapOf<String, String>(
    //
)
private val SECTION_XREFS_USED = HashSet<String>()
internal fun getSectionXREF(section: String): String {
    if (section.startsWith("XR_")) {
        return section
    }

    val text = SECTION_XREFS[section]
    if (text == null) {
        System.err.println("lwjgl: Missing section reference: $section")
        return section
    }
    SECTION_XREFS_USED.add(section)
    return text
}
internal fun printUnusedSectionXREFs() {
    SECTION_XREFS.keys.asSequence()
        .filter { !SECTION_XREFS_USED.contains(it) }
        .forEach {
            System.err.println("lwjgl: Unused section XREF:\n$it")
        }
}

private val LATEX_REGISTRY = mapOf<String, String>(
    //
)
private val LATEX_REGISTRY_USED = HashSet<String>()
internal fun getLatexCode(source: String): String {
    //val code = LATEX_REGISTRY[source] ?: throw IllegalStateException("Missing LaTeX equation:\n$source")
    val code = LATEX_REGISTRY[source] ?: LATEX_REGISTRY[source.replace("\\s+".toRegex(), " ")]
    if (code == null) {
        System.err.println("lwjgl: Missing LateX equation:\n$source")
        return source
    }
    LATEX_REGISTRY_USED.add(source)
    return code
}
internal fun printUnusedLatexEquations() {
    LATEX_REGISTRY.keys.asSequence()
        .filter { !LATEX_REGISTRY_USED.contains(it) }
        .forEach {
            System.err.println("lwjgl: Unused LateX equation:\n$it")
        }
}
