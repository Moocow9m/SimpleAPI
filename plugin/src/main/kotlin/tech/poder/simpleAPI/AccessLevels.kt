package tech.poder.simpleAPI

import proguard.classfile.AccessConstants

internal enum class AccessLevels(private val flag: Int) {
    PUBLIC(AccessConstants.PUBLIC),
    PRIVATE(AccessConstants.PRIVATE),
    PROTECTED(AccessConstants.PROTECTED),
    STATIC(AccessConstants.STATIC),
    FINAL(AccessConstants.FINAL),
    SUPER(AccessConstants.SUPER),
    SYNCHRONIZED(AccessConstants.SYNCHRONIZED),
    VOLATILE(AccessConstants.VOLATILE),
    TRANSIENT(AccessConstants.TRANSIENT),
    BRIDGE(AccessConstants.BRIDGE),
    VARARGS(AccessConstants.VARARGS),
    NATIVE(AccessConstants.NATIVE),
    INTERFACE(AccessConstants.INTERFACE),
    ABSTRACT(AccessConstants.ABSTRACT),
    STRICT(AccessConstants.STRICT),
    SYNTHETIC(AccessConstants.SYNTHETIC),
    ANNOTATION(AccessConstants.ANNOTATION),
    ENUM(AccessConstants.ENUM),
    MANDATED(AccessConstants.MANDATED),
    MODULE(AccessConstants.MODULE),
    OPEN(AccessConstants.OPEN),
    TRANSITIVE(AccessConstants.TRANSIENT),
    STATIC_PHASE(AccessConstants.STATIC_PHASE),

    RENAMED(AccessConstants.RENAMED),

    REMOVED_METHODS(AccessConstants.REMOVED_METHODS),

    REMOVED_FIELDS(AccessConstants.REMOVED_FIELDS);

    companion object {
        private val validClassFlags = arrayOf(
            PUBLIC,
            FINAL,
            SUPER,
            INTERFACE,
            ABSTRACT,
            SYNTHETIC,
            ANNOTATION,
            MODULE,
            ENUM
        )

        private val validFieldFlags = arrayOf(
            PUBLIC,
            PRIVATE,
            PROTECTED,
            STATIC,
            FINAL,
            VOLATILE,
            TRANSIENT,
            SYNTHETIC,
            ENUM
        )

        private val validMethodFlags = arrayOf(
            PUBLIC,
            PRIVATE,
            PROTECTED,
            STATIC,
            FINAL,
            SYNCHRONIZED,
            BRIDGE,
            VARARGS,
            NATIVE,
            ABSTRACT,
            STRICT,
            SYNTHETIC
        )

        private fun resolveCommon(input: Int, array: Array<AccessLevels>): Collection<AccessLevels> {
            val common = mutableSetOf<AccessLevels>()

            array.forEach {
                val flag = it.flag
                if (input and flag == flag) {
                    common.add(it)
                }
            }
            return common
        }

        fun setFlags(levels: Collection<AccessLevels>, current: Int = 0): Int {
            var result = current
            levels.forEach {
                result = result or it.flag
            }
            return result
        }

        fun resolveClass(input: Int): Collection<AccessLevels> {
            return resolveCommon(input, validClassFlags)
        }

        fun resolveField(input: Int): Collection<AccessLevels> {
            return resolveCommon(input, validFieldFlags)
        }

        fun resolveMethod(input: Int): Collection<AccessLevels> {
            return resolveCommon(input, validMethodFlags)
        }
    }

}