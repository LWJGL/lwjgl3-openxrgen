package org.lwjgl.openxrgen

import org.asciidoctor.*
import org.asciidoctor.ast.*
import org.asciidoctor.converter.*
import org.asciidoctor.extension.*
import org.intellij.lang.annotations.*
import java.net.*
import java.nio.charset.*
import java.nio.file.*
import kotlin.io.path.*

private val NUMERIC_PATTERN = """^\d+$""".toRegex()
private val LWJGL_OPTIONS = HashMap<String, Any>().apply {
    this["type"] = "lwjgl"
}

internal data class AsciidoctorParser(
    val asciidoctor: Asciidoctor,
    val attribsBuilder: AttributesBuilder
)

internal fun createAsciidoctor(root: Path, structs: Map<String, TypeStruct>): AsciidoctorParser {
    // extensions.txt does not include attribs.txt
    // we always pass these as document attribs
    val commonAttribs = """^:([^:]+):"""
        .toRegex(RegexOption.MULTILINE)
        .findAll(String(Files.readAllBytes(root.resolve("config").resolve("attribs.adoc")), StandardCharsets.UTF_8))
        .map { it.groups[1]!!.value }
        .toHashSet()

    val asciidoctor = Asciidoctor.Factory.create()

    // Register the LWJGL generator template converter.
    asciidoctor
        .javaConverterRegistry()
        .register(LWJGLConverter::class.java, "lwjgl")

    // Register a preprocessor to replace attribute assignments that do not work.
    // --------------------------------------------------------------------------
    // Normally attributes are assigned in the header, before the document body. However, Vulkan-Docs assigns certain attributes (KNOWN_ATTRIBS) in the body,
    // usually before including another document that references the attribute. Such assignments are not processed by asciidoctor. This preprocessor replaces
    // the standard assignment (ATTRIB:VALUE) with an inline attribute entry ({set:ATTRIB:VALUE}).
    //
    // see (https://docs.asciidoctor.org/asciidoc/latest/attributes/inline-attribute-entries/)
    asciidoctor
        .javaExtensionRegistry()
        .preprocessor(object : Preprocessor() {
            private val ATTRIBUTE_ASSIGNMENT = """^:(\w+):\s+(.+)?$""".toRegex()
            private val BUILT_IN_ATTRIBS = setOf(
                "data-uri",
                "doctype",
                "icons",
                "leveloffset",
                "toclevels"
            )
            private val KNOWN_ATTRIBS = setOf(
                "refpage",
                "imageparam",
                "stageMaskName",
                "accessMaskName",
                "maxinstancecheck",
                "imageopts",
                "vkRefPageRoot"
            )

            private val LINKS = """link:\+\+(.+?)\+\+""".toRegex()
            private fun String.encodeURI(): String =
                if (!this.startsWith("http"))
                    this
                else
                    URL(this).let { URI(it.protocol, it.userInfo, it.host, it.port, it.path, it.query, it.ref) }.toString()

            override fun process(document: Document, reader: PreprocessorReader) {
                if (reader.file.endsWith("attribs.adoc")) {
                    return
                }

                val lines = reader.readLines()

                var i = 0
                while (i < lines.size) {
                    val line = lines[i]
                    val match = ATTRIBUTE_ASSIGNMENT.find(line)
                    if (match != null) {
                        val (attribute, value) = match.destructured
                        if (commonAttribs.contains(attribute)) {
                            lines.removeAt(i)
                            continue
                        }
                        if (!BUILT_IN_ATTRIBS.contains(attribute)) {
                            if (!KNOWN_ATTRIBS.contains(attribute)) {
                                println("LWJGL: REPLACING UNKNOWN ATTRIBUTE - $attribute:$value")
                            }
                            lines[i] = "{set:$attribute:$value}"
                        }
                    }
                    lines[i] = LINKS.replace(lines[i]) { m ->
                        "link:++${m.groups[1]!!.value.encodeURI()}++"
                    }
                    i++
                }

                reader.restoreLines(lines)
            }
        })

    asciidoctor.javaExtensionRegistry()
        .inlineMacroNormative("can")
        .inlineMacroNormative("cannot")
        .inlineMacroNormative("may")
        .inlineMacroNormative("must")
        .inlineMacroNormative("optional")
        .inlineMacroNormative("optionally")
        .inlineMacroNormative("required")
        .inlineMacroNormative("should")
        .inlineMacro("undefined", object : InlineMacroProcessor() {
            init {
                config[REGEXP] = "undefined:"
            }
            override fun process(parent: ContentNode, target: String?, attributes: MutableMap<String, Any>): Any {
                return createPhraseNode(parent, "quoted", "undefined", attributes, LWJGL_OPTIONS)
            }
        })
        .inlineMacroQuoted("dname", """dname:(\w+)""") { if (it.startsWith("XR_")) "#${it.substring(3)}" else "{@code $it}" }
        .inlineMacroQuoted("ename", """ename:(\w+)""") { if (it.startsWith("XR_")) "#${it.substring(3)}" else "{@code $it}" }
        .inlineMacroQuoted("dlink", """dlink:XR_(\w+)""") {
            if (MACROS.contains(it))
                "#XR_$it()"
            else
                "#$it"
        }
        .inlineMacroQuoted("flink", """flink:xr(\w+)""") { "#$it()" }
        .inlineMacroQuoted("reflink", """reflink:(\w+)""") { "{@code $it}" } // in see-also blocks, hidden atm
        .inlineMacroQuoted("apiext", """apiext:(\w+)""") { "{@link ${it.substring(3).template} $it}" }
        .inlineMacroQuoted("tlink", """tlink:(\w+)""") {
            if (it.startsWith("PFN_xr")) {
                if (it == "PFN_xrVoidFunction")
                    "{@code PFN_xrVoidFunction}"
                else
                    "##Xr${it.substring(6)}"
            } else {
                "{@code $it}"
            }
        }
        .inlineMacroCode("pname", """pname:(\w+(?:(?:\.|&#8594;)\w+)*)""")
        .inlineMacroCode("fname", """fname:(\w+)""")
        .inlineMacroCode("tname", """tname:(\w+)""")
        .inlineMacroCode("ptext", """ptext:([\w\*]+(?:(?:\.|&#8594;)[\w\*]+)*)""")
        .inlineMacroCode("basetype", """basetype:(\w+)""")
        .inlineMacroCode("elink", """elink:(\w+)""")
        .inlineMacroCode("etext", """etext:([\w\*_]+)""")
        .inlineMacroCode("ftext", """ftext:([\w\*]+)""")
        .inlineMacroCode("ftext", """ftext:([\w\*]+)""")
        .inlineMacro("code", object : InlineMacroProcessor() {
            init {
                config[REGEXP] = """code:(\w+(?:[.*]\w+)*\**)"""
            }
            private val XR_PATTERN = """^XR_\w+$""".toRegex()
            override fun process(parent: ContentNode, target: String, attributes: MutableMap<String, Any>): Any {
                val text = if (XR_PATTERN.matches(target)) {
                    val name = target.substring(3)
                    if (name.any { it in 'a'..'z' } && EXTENSION_TEMPLATES.containsKey(name)) {
                        "##${EXTENSION_TEMPLATES[name]}" // link to extension class
                    } else {
                        "#$name"
                    }
                } else if (NUMERIC_PATTERN.matches(target)) {
                    target
                } else {
                    "{@code $target}"
                }
                return createPhraseNode(parent, "quoted", text, attributes, LWJGL_OPTIONS)
            }
        })
        .inlineMacroStruct("sname", """sname:(\w+)""", structs)
        .inlineMacroStruct("slink", """slink:(\w+)""", structs)

    val attribsBuilder = Attributes.builder()
        .ignoreUndefinedAttributes(false)
        .attributeMissing("warn")

    asciidoctor.loadFile(
        root.resolve("config").resolve("attribs.adoc").toFile(),
        Options.builder()
            .backend("lwjgl")
            .docType("manpage")
            .safe(SafeMode.UNSAFE)
            .baseDir(root.resolve("config").toFile())
            .build()
    ).let { file ->
        commonAttribs.forEach {
            attribsBuilder.attribute(it, file.getAttribute(it.lowercase()))
        }
    }

    return AsciidoctorParser(
        asciidoctor,
        attribsBuilder
    )
}

private fun JavaExtensionRegistry.inlineMacroNormative(macroName: String) = this.inlineMacro(macroName, object : InlineMacroProcessor() {
    init {
        config[REGEXP] = "$macroName:"
    }
    override fun process(parent: ContentNode, target: String?, attributes: MutableMap<String, Any>): Any {
        return createPhraseNode(parent, "quoted", "<b>$macroName</b>", attributes, LWJGL_OPTIONS)
    }
})
private fun JavaExtensionRegistry.inlineMacroQuoted(macroName: String, @RegExp regexp : String, macro: (String) -> String) = this.inlineMacro(macroName, object : InlineMacroProcessor() {
    init {
        config[REGEXP] = regexp
    }
    override fun process(parent: ContentNode, target: String, attributes: MutableMap<String, Any>): Any {
        return createPhraseNode(parent, "quoted", macro(target), attributes, LWJGL_OPTIONS)
    }
})
private fun JavaExtensionRegistry.inlineMacroCode(macroName: String, @RegExp regexp : String) = inlineMacroQuoted(macroName, regexp) { "{@code $it}" }
private fun JavaExtensionRegistry.inlineMacroStruct(macroName: String, @RegExp regexp : String, structs: Map<String, TypeStruct>) = inlineMacroQuoted(macroName, regexp) {
    if (structs.containsKey(it))
        "##$it" // struct
    else
        "{@code $it}" // handle
}

internal class LWJGLConverter(backend: String, opts: Map<String, Any>) : StringConverter(backend, opts) {

    private val APIEXT_PATTERN = """^apiext:XR_\w+$""".toRegex()

    private val NESTED_CODE_PATTERN = """[a-z]+(?<!sname|slink):(\w+)""".toRegex() // any macro except sname/slink

    private val EXTENSION_LINK_PATTERN = """^XR_\w+$""".toRegex()
    private val EXTENSION_HTML_LINK_PATTERN = """^(\w+)\.html$""".toRegex()
    private val UNWRAP_CODE_PATTERN = """^\{@code ([^}]+)}$""".toRegex()

    override fun convert(node: ContentNode, transform: String?, opts: MutableMap<Any, Any>): String {
        if (node is PhraseNode) {
            return when (node.type) {
                "lwjgl"       -> node.text
                "monospaced"  -> {
                    if (NUMERIC_PATTERN.matches(node.text) || APIEXT_PATTERN.matches(node.text) || (node.text.startsWith("&lt;&lt;") && node.text.endsWith("&gt;&gt;")) || node.text.startsWith("link:"))
                        node.text
                    else if (node.text.contains("__")) // VK_IMAGE_ASPECT_MEMORY_PLANE__{ibit}__BIT_EXT
                        "<code>${node.text}</code>"
                    else
                        "{@code ${node.text}}"
                }
                "unquoted"    -> {
                    if (node.hasRole("eq")) {
                        // This is the only remaining awkward use of regex after the refactoring to the Asciidoc Extension API.
                        // We need it in order to have readable equations when viewing javadoc in source form (vs HTML).
                        "<code>${NESTED_CODE_PATTERN.replace(node.text, "$1")}</code>"
                    } else if (node.hasRole("big")) {
                        node.text // TODO: better rendering?
                    } else if (node.hasRole("underline")) {
                        "<u>${node.text}</u>"
                    } else
                        throw IllegalStateException(node.roles.joinToString(", "))
                }
                "xref"        -> {
                    node.getAttribute("refid").let {
                        val refid = it.toString()
                        if (EXTENSION_LINK_PATTERN.matches(refid)) {
                            "{@link ${refid.substring(3).template} $refid}"
                        } else {
                            """<a target="_blank" href="https://www.khronos.org/registry/OpenXR/specs/1.0/html/xrspec.html\#$refid">${if (node.text != null) node.text else getSectionXREF(refid)}</a>"""
                        }
                    }
                }
                "ref"         -> ""
                "emphasis"    -> "<em>${node.text}</em>"
                "strong"      -> "<b>${node.text}</b>"
                "latexmath"   -> getLatexCode(node.text)
                "line"        -> node.text
                "link"        -> {
                    val match = EXTENSION_HTML_LINK_PATTERN.find(node.target)?.groups?.get(1)?.value
                    if (match != null) {
                        if (match.startsWith("XR_") && match == node.text) {
                            "{@link ${match.substring(3).template} $match}"
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            val structs = node.document.options["structs"] as Map<String, TypeStruct>
                            if (structs.containsKey(match)) {
                                UNWRAP_CODE_PATTERN.find(node.text).let {
                                    if (it == null)
                                        "{@link $match ${node.text}}"
                                    else
                                        "{@link $match ${it.groups[1]!!.value}}"
                                }
                            } else if (hasUnnamedXREF(match)) {
                                // hack for vkAllocationFunction_return_rules
                                """<a target="_blank" href="https://www.khronos.org/registry/OpenXR/specs/1.0/html/xrspec.html\#$match">${node.text}</a>"""
                            } else {
                                "${node.text} ({@code $match})"
                            }
                        }
                    } else {
                        val href = node.target.replace("#", "\\#")
                        """<a target="_blank" href="$href">${node.text
                            .run {
                                if (startsWith("https://www.khronos.org/registry/OpenXR/specs/1.0/html/xrspec.html#"))
                                    getSectionXREF(substring("https://www.khronos.org/registry/OpenXR/specs/1.0/html/xrspec.html#".length))
                                else if (this == node.target)
                                    href
                                else
                                    this
                            }
                        }</a>"""
                    }
                }
                "subscript"   -> "<sub>${node.text}</sub>"
                "superscript" -> "<sup>${node.text}</sup>"
                "double"      -> {
                    if (node.parent.nodeName == "paragraph" && !node.text.startsWith("etext:"))
                        "&#8220;{@code ${node.text}}&#8221;"
                    else
                        "&#8220;${node.text}&#8221;"
                }
                "single"      -> "`${node.text}'"
                "icon"        -> ""
                "image"       -> "<img src=\"https://raw.githubusercontent.com/KhronosGroup/OpenXR-Docs/master/${Paths.get(node.target).relativeTo(OPENXR_DOCS_ROOT).toString().replace('\\', '/')}${if (node.target.endsWith(".svg")) "?sanitize=true" else ""}\" alt=\"${node.attributes["alt"]}\">"
                else          -> {
                    if (node.hasAttribute("terms") || node.hasAttribute("guard")) { // TODO:
                        ""
                    } else {
                        System.err.println("lwjgl: type: ${node.type}")
                        System.err.println("lwjgl: text: ${node.text}")
                        System.err.println("lwjgl: target: ${node.target}")
                        System.err.println("lwjgl: reftext: ${node.reftext}")
                        System.err.println("lwjgl: id: ${node.id}")
                        System.err.println("lwjgl: attributes: ${node.attributes}")
                        throw IllegalStateException()
                    }
                }
            }
        }
        throw IllegalStateException("${node.nodeName} ${node.javaClass}")
    }
}
