package tech.poder.simpleAPI

import kotlinx.metadata.KmClass
import kotlinx.metadata.KmPackage
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import proguard.classfile.Clazz
import proguard.classfile.LibraryClass
import proguard.classfile.ProgramClass
import proguard.classfile.attribute.annotation.*
import proguard.classfile.attribute.annotation.Annotation
import proguard.classfile.constant.IntegerConstant
import proguard.classfile.constant.Utf8Constant
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.visitor.ClassVisitor

internal class MyLovelyVisitor(private val writer: ClassVisitor) : ClassVisitor {

    override fun visitAnyClass(clazz: Clazz) {
        writer.visitAnyClass(clazz)
    }

    override fun visitLibraryClass(libraryClass: LibraryClass) {
        writer.visitLibraryClass(libraryClass)
    }

    private fun noKotlinMeta(programClass: ProgramClass) {
        println("Java class ${programClass.name}")

    }

    override fun visitProgramClass(programClass: ProgramClass) {
        val classFlags = AccessLevels.resolveClass(programClass.u2accessFlags)

        if (AccessLevels.PUBLIC !in classFlags) {
            return
        }

        val kotlinClassHeader = programClass.attributes
            .filterIsInstance<RuntimeVisibleAnnotationsAttribute>()
            .mapNotNull { runtimeAnnotation ->
                runtimeAnnotation.annotations.mapNotNull { buildKotlinClassHeader(programClass, it) }.firstOrNull()
            }
            .firstOrNull() ?: noKotlinMeta(programClass).let { return }


        val metadata = KotlinClassMetadata.read(kotlinClassHeader)
        try {

            var facade: String? = null
            val clazz = when (metadata) {
                is KotlinClassMetadata.Class -> {
                    val tmpClazz = metadata.toKmClass()
                    // If it is an internal class, return
                    if (tmpClazz.flags and 2 != 2) {
                        return
                    }
                    tmpClazz
                }
                is KotlinClassMetadata.FileFacade -> {
                    metadata.toKmPackage()
                }
                is KotlinClassMetadata.SyntheticClass -> {
                    return
                }
                is KotlinClassMetadata.MultiFileClassFacade -> {
                    return
                }
                is KotlinClassMetadata.MultiFileClassPart -> {
                    facade = metadata.facadeClassName
                    metadata.toKmPackage()
                }
                else -> {
                    throw IllegalStateException("$metadata -- ${programClass.name}")
                }
            }


            val allowedFunctions = clazz.functions.filter {
                it.flags and 2 == 2
            }
            val allowedProperties = clazz.properties.filter {
                it.flags and 2 == 2
            }
            if (allowedFunctions.isEmpty() && allowedProperties.isEmpty()) {
                return
            }
            clazz.functions.clear()
            clazz.functions.addAll(allowedFunctions)
            clazz.properties.clear()
            clazz.properties.addAll(allowedProperties)

            val classBuilder = ClassBuilder(
                programClass.u4version,
                programClass.u2accessFlags,
                programClass.name,
                programClass.superName,
                programClass.featureName,
                programClass.processingFlags,
                programClass.processingInfo
            )

            val header = when (clazz) {
                is KmClass -> {
                    KotlinClassMetadata.Class.Writer().apply(clazz::accept).write().header
                }
                is KmPackage -> {
                    if (facade == null) {
                        KotlinClassMetadata.FileFacade.Writer().apply(clazz::accept).write().header
                    } else {
                        KotlinClassMetadata.MultiFileClassPart.Writer().apply(clazz::accept).write(facade).header
                    }
                }
                else -> throw IllegalStateException("not implemented!")
            }

            val attribute = rebuildKotlinClassHeader(classBuilder, header)
            val newProgram = classBuilder.programClass
            val oldAttribute = newProgram.attributes.filterIsInstance<RuntimeVisibleAnnotationsAttribute>()
                .firstOrNull { it.u2attributeNameIndex == attribute.u2attributeNameIndex }
            if (oldAttribute != null) {
                println("Reused attribute")
                oldAttribute.u2annotationsCount += attribute.u2annotationsCount
                oldAttribute.annotations = arrayOf(*oldAttribute.annotations, *attribute.annotations)
            } else {
                println("Created new attribute")
                newProgram.u2attributesCount++
                newProgram.attributes = arrayOf(*newProgram.attributes, attribute)
            }
            println("Wrote ${allowedFunctions.size + allowedProperties.size} items in class ${programClass.name}")
            writer.visitProgramClass(newProgram)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private fun buildKotlinClassHeader(
        programClass: ProgramClass,
        attributeAnnotation: Annotation
    ): KotlinClassHeader? {

        val type = programClass.constantPool[attributeAnnotation.u2typeIndex]

        if (type !is Utf8Constant || type.string != "Lkotlin/Metadata;") {
            return null
        }

        val elementsByName = attributeAnnotation.elementValues.associateBy { elementValue ->
            (programClass.constantPool[elementValue.u2elementNameIndex] as Utf8Constant).string
        }

        // TODO: Add [extraString] and [packageName]
        return KotlinClassHeader(
            kind = parseIntElement(programClass, elementsByName["k"] as ConstantElementValue?),
            extraInt = parseIntElement(programClass, elementsByName["xi"] as ConstantElementValue?),
            metadataVersion = parseIntArrayElement(programClass, elementsByName["mv"] as ArrayElementValue?),
            bytecodeVersion = parseIntArrayElement(programClass, elementsByName["mv"] as ArrayElementValue?),
            data1 = parseStringArrayElement(programClass, elementsByName["d1"] as ArrayElementValue?),
            data2 = parseStringArrayElement(programClass, elementsByName["d2"] as ArrayElementValue?),
            extraString = parseUTF8Element(programClass, elementsByName["xs"] as ConstantElementValue?),
            packageName = parseUTF8Element(programClass, elementsByName["pn"] as ConstantElementValue?),
        )
    }

    private fun rebuildKotlinClassHeader(
        programClass: ClassBuilder,
        header: KotlinClassHeader,
    ): RuntimeVisibleAnnotationsAttribute {
        val attribute = programClass.constantPoolEditor.addUtf8Constant("RuntimeVisibleAnnotations")
        val metadataLocation = programClass.constantPoolEditor.addUtf8Constant("Lkotlin/Metadata;")
        val elements = mutableListOf<ElementValue>()
        if (header.extraInt != 0) {
            elements.add(
                addIntConstant(programClass, header.extraInt, "xi")
            )
        }
        if (header.metadataVersion.isNotEmpty()) {
            elements.add(
                ArrayElementValue(
                    programClass.constantPoolEditor.addUtf8Constant("mv"),
                    header.metadataVersion.size,
                    header.metadataVersion.map {
                        addIntConstant(programClass, it)
                    }.toTypedArray()
                )
            )
        }
        /*if (header.bytecodeVersion.isNotEmpty()) {
            elements.add(
                ArrayElementValue(
                    programClass.constantPoolEditor.addUtf8Constant("bv"),
                    header.bytecodeVersion.size,
                    header.bytecodeVersion.map {
                        addIntConstant(programClass, it)
                    }.toTypedArray()
                )
            )
        }*/
        if (header.data1.isNotEmpty()) {
            elements.add(
                ArrayElementValue(
                    programClass.constantPoolEditor.addUtf8Constant("d1"),
                    header.data1.size,
                    header.data1.map {
                        addStringConstant(programClass, it)
                    }.toTypedArray()
                )
            )
        }
        if (header.data2.isNotEmpty()) {
            elements.add(
                ArrayElementValue(
                    programClass.constantPoolEditor.addUtf8Constant("d2"),
                    header.data2.size,
                    header.data2.map {
                        addStringConstant(programClass, it)
                    }.toTypedArray()
                )
            )
        }
        elements.add(
            addIntConstant(programClass, header.kind, "k")
        )
        /*if (header.packageName.isNotBlank()) {
            elements.add(
                addStringConstant(programClass, header.packageName, "pn")
            )
        }
        if (header.extraString.isNotBlank()) {
            elements.add(
                addStringConstant(programClass, header.extraString, "xs")
            )
        }*/
        return RuntimeVisibleAnnotationsAttribute(
            attribute,
            1,
            arrayOf(Annotation(metadataLocation, elements.size, elements.toTypedArray()))
        )
    }

    private fun addIntConstant(classBuilder: ClassBuilder, value: Int, name: String? = null): ConstantElementValue {
        return ConstantElementValue(
            'I',
            addName(classBuilder, name),
            classBuilder.constantPoolEditor.addIntegerConstant(value)
        )
    }

    private fun addName(classBuilder: ClassBuilder, name: String? = null): Int {
        return if (name == null) {
            0
        } else {
            classBuilder.constantPoolEditor.addUtf8Constant(name)
        }
    }

    private fun addStringConstant(
        classBuilder: ClassBuilder,
        value: String,
        name: String? = null
    ): ConstantElementValue {
        return ConstantElementValue(
            's',
            addName(classBuilder, name),
            classBuilder.constantPoolEditor.addUtf8Constant(value)
        )
    }

    private fun parseIntElement(programClass: ProgramClass, value: ConstantElementValue?): Int? {
        if (value == null) {
            return null
        }
        return (programClass.constantPool[value.u2constantValueIndex] as IntegerConstant).value
    }

    private fun parseUTF8Element(programClass: ProgramClass, value: ConstantElementValue?): String? {
        if (value == null) {
            return null
        }
        return (programClass.constantPool[value.u2constantValueIndex] as Utf8Constant).string
    }

    private fun parseStringArrayElement(programClass: ProgramClass, value: ArrayElementValue?): Array<String>? {
        if (value == null) {
            return null
        }
        return Array(value.u2elementValuesCount) {
            parseUTF8Element(programClass, value.elementValues[it] as ConstantElementValue)!!
        }
    }

    private fun parseIntArrayElement(programClass: ProgramClass, value: ArrayElementValue?): IntArray? {
        if (value == null) {
            return null
        }
        return IntArray(value.u2elementValuesCount) {
            parseIntElement(programClass, value.elementValues[it] as ConstantElementValue)!!
        }
    }

}