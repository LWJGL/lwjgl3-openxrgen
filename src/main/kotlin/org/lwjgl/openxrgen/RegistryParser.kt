package org.lwjgl.openxrgen

import com.thoughtworks.xstream.*
import com.thoughtworks.xstream.converters.*
import com.thoughtworks.xstream.io.*
import com.thoughtworks.xstream.io.xml.*
import java.nio.charset.*
import java.nio.file.*

internal class VendorID(
    val name: String,
    val id: String,
    val comment: String
)

internal class Platform(
    val name: String,
    val protect: String,
    val comment: String
)

internal class Tag(
    val name: String,
    val author: String,
    val contact: String
)

internal abstract class Type(val name: String)

internal object TypeIgnored : Type("<IGNORED>")

internal class TypeSystem(
    val requires: String,
    name: String
) : Type(name)

internal class TypeBase(
    val type: String,
    name: String
) : Type(name)

internal class TypePlatform(name: String) : Type(name)

internal class TypeBitmask(
    val requires: String?,
    val typedef: String,
    name: String
) : Type(name)

internal class TypeHandle(
    val parent: String?,
    val type: String,
    name: String
) : Type(name)

internal class TypeEnum(name: String, alias: String?) : Type(name)

internal interface Function {
    val proto: Field
    val params: List<Field>
}

internal class TypeFuncpointer(
    override val proto: Field,
    override val params: List<Field>
) : Type(proto.name), Function

internal abstract class TypeStruct(
    name: String,
): Type(name) {
    abstract val type: String // struct or union
    abstract val returnedonly: Boolean
    abstract val structextends: List<String>?
    abstract val members: MutableList<Field>
    abstract val alias: String?
    abstract val parentstruct: String?
}

internal class TypeStructBase(
    name: String,
    override val type: String, // struct or union
    override val returnedonly: Boolean,
    override val structextends: List<String>?,
    override val members: MutableList<Field>,
    override val alias: String?,
    override val parentstruct: String?
) : TypeStruct(name)

private class TypeStructAlias(
    name: String,
    override val alias: String,
    reference: Lazy<TypeStruct>
) : TypeStruct(name) {
    private val ref: TypeStruct by reference

    override val type: String get() = ref.type
    override val returnedonly: Boolean get() = ref.returnedonly
    override val structextends: List<String>? get() = ref.structextends
    override val members: MutableList<Field> get() = ref.members
    override val parentstruct: String? get() = ref.parentstruct
}

internal class Enum(
    val name: String,
    val alias: String?,
    val value: String?,
    val bitpos: String?,
    val extnumber: Int?,
    val offset: String?,
    val dir: String?,
    val extends: String?,
    val comment: String?
)

internal class Unused(val start: String)

internal class Enums(
    val name: String,
    val type: String?,
    val bitwidth: Int?,
    val comment: String?,
    val enums: List<Enum>?,
    val unused: Unused?
)

internal class Field(
    val modifier: String,
    val type: String,
    val indirection: String,
    val name: String,
    val bits: Int?,
    val array: String?,
    val attribs: MutableMap<String, String>
) {
    val len: Sequence<String> get() = attribs["len"].let {
        it?.splitToSequence(",") ?: emptySequence()
    }

    val optional: String? get() = attribs["optional"]
    val values: String? get() = attribs["values"]
    val externsync: String? get() = attribs["externsync"]
    val noautovalidity: String? get() = attribs["noautovalidity"]
    //val validextensionstructs: String? get() = attribs["validextensionstructs"]
}

internal class Validity

internal class ImplicitExternSyncParams(
    val params: List<Field>
)

internal class Commands(
    val commands: List<Command>
)

internal class Command(
    val name: String?,
    val alias: String?,
    val successcodes: String?,
    val errorcodes: String?,
    override val proto: Field,
    override val params: List<Field>,
    val validity: Validity?,
    val implicitexternsyncparams: ImplicitExternSyncParams?
) : Function

internal class UserPath(val path: String)
internal class Component(val user_path: String?, val subpath: String, val type: String, val system: Boolean?)

internal class InteractionProfile(
    val name: String,
    val title: String,
    val user_paths: List<UserPath>,
    val components: List<Component>,
)

internal class Extend(
    val interaction_profile_path: String,
    val components: List<Component>
)

internal class TypeRef(val name: String)

internal data class CommandRef(val name: String)
internal data class InteractionProfileRef(val name: String)

internal class Require(
    val comment: String?,
    val feature: String?,
    val extension: String?,
    val types: List<TypeRef>?,
    val enums: List<Enum>?,
    val commands: List<CommandRef>?,
    val interaction_profiles: List<InteractionProfile>?,
    val extends: List<Extend>?
)

internal class Feature(
    val api: String,
    val name: String,
    val number: String,
    val requires: List<Require>
)

internal class Extension(
    val name: String,
    val number: Int,
    val type: String,
    val supported: String,
    val platform: String?,
    val promotedto: String?,
    val requires: List<Require>
)

internal class Enable(
    val version: String?,
    val extension: String?,
    val struct: String?,
    val feature: String?,
    val requires: String?
)

internal class Registry(
    val vendorids: List<VendorID>,
    val platforms: List<Platform>,
    val tags: List<Tag>,
    val types: List<Type>,
    val enums: List<Enums>,
    val commands: List<Commands>,
    val interaction_profiles: List<InteractionProfile>,
    val features: List<Feature>,
    val extensions: List<Extension>
)

private val INDIRECTION_REGEX = Regex("""([*]+)(?:\s+const\s*([*]+))?""")

private val String.indirection: String get() = if (this.isEmpty())
    this
else {
    val (p, const_p) = INDIRECTION_REGEX.matchEntire(this)!!.destructured
    "${p.indirection(".")}${if (const_p.isEmpty()) "" else const_p.indirection(".const.")}"
}

private fun String.indirection(prefix: String) = this.length
    .downTo(1)
    .asSequence()
    .map { "p" }
    .joinToString(".", prefix = prefix)

internal class FieldConverter : Converter {
    override fun marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        TODO()
    }

    private val MODIFIER_STRUCT_REGEX = "\\s*struct(?=\\s|$)".toRegex()
    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        val attribs = reader.attributeNames.asSequence()
            .map(Any?::toString)
            .associateWithTo(HashMap()) { reader.getAttribute(it) }

        val modifier = reader.value.trim().replace(MODIFIER_STRUCT_REGEX, "")

        if (!reader.hasMoreChildren())
            return Field("", "N/A", "", modifier, null, null, attribs)

        val type = StringBuilder()
        reader.moveDown()
        type.append(reader.value)
        reader.moveUp()
        val indirection = reader.value.trim().indirection
        reader.moveDown()
        val name = reader.value
        reader.moveUp()

        var bits: Int? = null
        var array: String? = null
        reader.value.trim().let {
            when {
                it.isEmpty()       -> {}
                it.startsWith(':') -> {
                    bits = it.substring(1).toInt()
                    check(!reader.hasMoreChildren())
                }
                it.startsWith('[') ->
                    when {
                        reader.hasMoreChildren() -> {
                            reader.moveDown()
                            array = "\"${reader.value}\""
                            reader.moveUp()
                            check(!reader.hasMoreChildren() && reader.value == "]")
                        }
                        it.endsWith(']')         -> array = it.substring(1, it.length - 1).let {
                            if (it.startsWith("XR_")) "\"$it\"" else it
                        }
                        else                     -> throw IllegalStateException()
                    }
                else               -> throw IllegalStateException(it)
            }
        }

        return Field(modifier, type.toString(), indirection, name, bits, array, attribs)
    }

    override fun canConvert(type: Class<*>?): Boolean = type === Field::class.java
}

private class RegistryMap {

    val bitmasks = HashMap<String, TypeBitmask>()
    val handles = HashMap<String, TypeHandle>()
    val structs = HashMap<String, TypeStruct>()

}

private val UnmarshallingContext.registryMap: RegistryMap
    get() {
        var map = this["registryMap"] as RegistryMap?
        if (map == null) {
            map = RegistryMap()
            this.put("registryMap", map)
        }
        return map
    }

internal class TypeConverter : Converter {
    companion object {
        private val FIELD_CONVERTED = FieldConverter()

        private val FUNC_POINTER_RETURN_TYPE_REGEX = Regex("""typedef\s+(?:(const|enum|struct)\s+)?(\w+)\s*([*]*)\s*[(]""")
        private val FUNC_POINTER_PARAM_MOD_REGEX = Regex("""[(,]\s*(\w+)?""")
        private val FUNC_POINTER_PARAM_NAME_REGEX = Regex("""\s*([*]*)\s*(\w+)\s*[,)]""")
    }

    override fun marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        TODO()
    }

    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any? {
        val category = reader.getAttribute("category")
        if (category == null) {
            val name = reader.getAttribute("name")
            val requires = reader.getAttribute("requires")
            return if (name != null && requires != null) {
                if ("openxr_platform_defines" == requires)
                    TypePlatform(name)
                else
                    TypeSystem(requires, name)
            } else
                TypeIgnored
        }

        return when (category) {
            "define"      -> {
                if (reader.getAttribute("name") != null) {
                    TypeIgnored
                } else {
                    reader.moveDown()
                    val name = reader.value
                    reader.moveUp()
                    if (name.startsWith("XR_")) {
                        TypeIgnored
                    } else {
                        TypePlatform(name)
                    }
                }
            }
            "basetype"    -> {
                reader.moveDown()
                val type = if (reader.nodeName == "type") {
                    try {
                        reader.value
                    } finally {
                        reader.moveUp()
                        reader.moveDown()
                    }
                } else {
                    "opaque"
                }

                val name = reader.value
                reader.moveUp()

                TypeBase(type, name)
            }
            "bitmask"     -> {
                val bitvalues = reader.getAttribute("bitvalues")

                var name = reader.getAttribute("name")
                val t = if (name == null) {
                    reader.moveDown()
                    val typedef = reader.value // e.g. XrFlags
                    reader.moveUp()

                    reader.moveDown()
                    name = reader.value
                    reader.moveUp()

                    TypeBitmask(bitvalues, typedef, name)
                } else {
                    val ref = context.registryMap.bitmasks[reader.getAttribute("alias")]!!
                    TypeBitmask(ref.requires, ref.typedef, name)
                }
                context.registryMap.bitmasks[name] = t
                t
            }
            "handle"      -> {
                val parent = reader.getAttribute("parent")

                var name = reader.getAttribute("name")
                val t = if (name == null) {
                    reader.moveDown()
                    val type = reader.value
                    reader.moveUp()

                    reader.moveDown()
                    name = reader.value
                    reader.moveUp()

                    TypeHandle(parent, type, name)
                } else {
                    val ref = context.registryMap.handles[reader.getAttribute("alias")]!!
                    TypeHandle(ref.parent, ref.type, name)
                }
                context.registryMap.handles[name] = t
                t
            }
            "enum"        -> {
                TypeEnum(reader.getAttribute("name"), reader.getAttribute("alias"))
            }
            "funcpointer" -> {
                val proto = reader.let {
                    val (modifier, type, indirection) = FUNC_POINTER_RETURN_TYPE_REGEX.find(it.value)!!.destructured
                    it.moveDown()
                    val name = it.value
                    it.moveUp()

                    Field(modifier, type, indirection.indirection, name, null, null, HashMap())
                }

                if (OPAQUE_PFN_TYPES.contains(proto.name))
                    TypePlatform(proto.name)
                else {
                    val params = ArrayList<Field>()
                    while (reader.hasMoreChildren()) {
                        val (modifier) = FUNC_POINTER_PARAM_MOD_REGEX.find(reader.value)!!.destructured

                        val type = StringBuilder()
                        reader.moveDown()
                        type.append(reader.value)
                        reader.moveUp()

                        val (indirection, paramName) = FUNC_POINTER_PARAM_NAME_REGEX.find(reader.value)!!.destructured

                        params.add(Field(modifier, type.toString(), indirection.indirection, paramName, null, null, HashMap()))
                    }

                    TypeFuncpointer(proto, params)
                }
            }
            "union",
            "struct"      -> {
                val alias = reader.getAttribute("alias")
                val name = reader.getAttribute("name")
                val parentstruct = reader.getAttribute("parentstruct")

                val t = if (alias == null) {
                    val returnedonly = reader.getAttribute("returnedonly") != null
                    val structextends = reader.getAttribute("structextends")?.split(",")

                    val members = ArrayList<Field>()
                    while (reader.hasMoreChildren()) {
                        reader.moveDown()
                        if (reader.nodeName == "member")
                            members.add(FIELD_CONVERTED.unmarshal(reader, context) as Field)
                        reader.moveUp()
                    }

                    TypeStructBase(name, category, returnedonly, structextends, members, null, parentstruct)
                } else {
                    TypeStructAlias(name, alias, lazy { context.registryMap.structs[alias] ?: throw IllegalStateException("Struct reference not found: $alias for struct $name") })
                }
                context.registryMap.structs[name] = t
                t
            }
            else          -> TypeIgnored
        }
    }

    override fun canConvert(type: Class<*>?): Boolean = type == Type::class.java
}

internal fun parse(registry: Path) = XStream(Xpp3Driver()).let { xs ->
    xs.allowTypesByWildcard(arrayOf("org.lwjgl.openxrgen.*"))

    xs.alias("registry", Registry::class.java)

    VendorID::class.java.let {
        xs.alias("vendorid", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "id")
        xs.useAttributeFor(it, "comment")
    }

    Platform::class.java.let {
        xs.alias("platform", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "protect")
        xs.useAttributeFor(it, "comment")
    }

    Tag::class.java.let {
        xs.alias("tag", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "author")
        xs.useAttributeFor(it, "contact")
    }

    Type::class.java.let {
        xs.registerConverter(TypeConverter())

        xs.alias("type", it)
    }

    Enums::class.java.let {
        xs.addImplicitCollection(Registry::class.java, "enums", "enums", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "type")
        xs.useAttributeFor(it, "bitwidth")
        xs.useAttributeFor(it, "comment")
    }

    Enum::class.java.let {
        xs.addImplicitCollection(Enums::class.java, "enums", "enum", it)
        xs.addImplicitCollection(Require::class.java, "enums", "enum", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "alias")
        xs.useAttributeFor(it, "value")
        xs.useAttributeFor(it, "bitpos")
        xs.useAttributeFor(it, "extnumber")
        xs.useAttributeFor(it, "offset")
        xs.useAttributeFor(it, "dir")
        xs.useAttributeFor(it, "extends")
        xs.useAttributeFor(it, "comment")
    }

    Commands::class.java.let {
        xs.addImplicitCollection(Registry::class.java, "commands", "commands", it)
    }

    Command::class.java.let {
        xs.addImplicitCollection(Commands::class.java, "commands", "command", it)
        xs.alias("command", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "alias")
        xs.useAttributeFor(it, "successcodes")
        xs.useAttributeFor(it, "errorcodes")
    }

    InteractionProfile::class.java.let {
        xs.alias("interaction_profile", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "title")
    }

    Extend::class.java.let {
        xs.addImplicitCollection(Require::class.java, "extends", "extend", it)
        xs.useAttributeFor(it, "interaction_profile_path")
    }

    UserPath::class.java.let {
        xs.addImplicitCollection(InteractionProfile::class.java, "user_paths", "user_path", it)
        xs.useAttributeFor(it, "path")
    }

    Component::class.java.let {
        xs.addImplicitCollection(InteractionProfile::class.java, "components", "component", it)
        xs.addImplicitCollection(Extend::class.java, "components", "component", it)
        xs.useAttributeFor(it, "user_path")
        xs.useAttributeFor(it, "subpath")
        xs.useAttributeFor(it, "type")
        xs.useAttributeFor(it, "system")
    }

    Field::class.java.let {
        xs.registerConverter(FieldConverter())

        xs.alias("proto", it)
        xs.addImplicitCollection(Command::class.java, "params", "param", it)
    }

    xs.alias("validity", Validity::class.java)
    xs.addImplicitCollection(ImplicitExternSyncParams::class.java, "params", "param", Field::class.java)

    Feature::class.java.let {
        xs.addImplicitCollection(Registry::class.java, "features", "feature", it)
        xs.useAttributeFor(it, "api")
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "number")
    }

    Require::class.java.let {
        xs.addImplicitCollection(Feature::class.java, "requires", "require", it)
        xs.addImplicitCollection(Extension::class.java, "requires", "require", it)
        xs.useAttributeFor(it, "comment")
        xs.useAttributeFor(it, "feature")
        xs.useAttributeFor(it, "extension")
    }

    TypeRef::class.java.let {
        xs.addImplicitCollection(Require::class.java, "types", "type", it)
        xs.useAttributeFor(it, "name")
    }

    CommandRef::class.java.let {
        xs.addImplicitCollection(Require::class.java, "commands", "command", it)
        xs.useAttributeFor(it, "name")
    }

    InteractionProfileRef::class.java.let {
        xs.addImplicitCollection(Require::class.java, "interaction_profiles", "interaction_profile", it)
        xs.useAttributeFor(it, "name")
    }

    Extension::class.java.let {
        xs.alias("extension", it)
        xs.useAttributeFor(it, "name")
        xs.useAttributeFor(it, "number")
        xs.useAttributeFor(it, "type")
        xs.useAttributeFor(it, "supported")
        xs.useAttributeFor(it, "platform")
        xs.useAttributeFor(it, "promotedto")
    }

    xs
}.fromXML(Files
    .readAllBytes(registry)
    .toString(StandardCharsets.UTF_8)
    .replace("""<comment>[\s\S]*?</comment>""".toRegex(), "") // easier to remove than parse correctly
) as Registry