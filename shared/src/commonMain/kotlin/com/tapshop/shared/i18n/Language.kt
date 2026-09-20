package com.tapshop.shared.i18n

enum class Language(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false,
) {
    EN("en", "English", "English"),
    FR("fr", "Français", "French"),
    ES("es", "Español", "Spanish"),
    JA("ja", "日本語", "Japanese"),
    AR("ar", "العربية", "Arabic", isRtl = true);

    val strings: AppStrings
        get() = when (this) {
            EN -> EnStrings
            FR -> FrStrings
            ES -> EsStrings
            JA -> JaStrings
            AR -> ArStrings
        }

    companion object {
        fun fromCode(code: String?): Language? {
            if (code == null) return null
            val lower = code.lowercase()
            return entries.firstOrNull { lower == it.code || lower.startsWith(it.code + "-") || lower.startsWith(it.code + "_") }
        }
    }
}
