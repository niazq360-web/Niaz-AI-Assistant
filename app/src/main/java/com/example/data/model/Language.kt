package com.example.data.model

enum class Language(val code: String, val displayName: String, val nativeName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", "English", false),
    URDU("ur", "Urdu", "اردو", true),
    SINDHI("sd", "Sindhi", "سنڌي", true)
}

object LocalizationStrings {
    fun getGreeting(lang: Language, ownerName: String = "Niaz Ahmed"): String {
        return when (lang) {
            Language.ENGLISH -> "Hello $ownerName! How can I assist your workflow today?"
            Language.URDU -> "اسلام علیکم $ownerName! آج میں آپ کے ڈیجیٹل کاموں میں کیسے مدد کر سکتا ہوں؟"
            Language.SINDHI -> "سلام $ownerName! اڄ مان توهان جي ڪمن ۾ ڪيئن مدد ڪري سگهان ٿو؟"
        }
    }

    fun getSubtitle(lang: Language): String {
        return when (lang) {
            Language.ENGLISH -> "Your Personal AI Assistant — Work Smarter, Automatically."
            Language.URDU -> "آپ کا ذاتی اے آئی اسسٹنٹ — زیادہ ہوشیاری سے اور خودکار طریقے سے کام کریں۔"
            Language.SINDHI -> "توهان جو ذاتي اي آءِ اسسٽنٽ — ذهانت سان ۽ خودڪار طريقي سان ڪم ڪريو."
        }
    }

    fun getNotConnectedMsg(service: String, lang: Language): String {
        return when (lang) {
            Language.ENGLISH -> "$service is not connected. Please connect $service from Connected Apps first."
            Language.URDU -> "$service منسلک نہیں ہے۔ براہ کرم پہلے کنیکٹڈ ایپس سے $service جوڑیں۔"
            Language.SINDHI -> "$service ڳنڍيل نه آهي. مهرباني ڪري پهرين ڪنيڪٽ ٿيل ائپس مان $service ڳنڍيو."
        }
    }

    fun getActionSuccess(action: String, lang: Language): String {
        return when (lang) {
            Language.ENGLISH -> "Action completed successfully: $action"
            Language.URDU -> "کارروائی کامیابی سے مکمل ہو گئی: $action"
            Language.SINDHI -> "ڪم ڪاميابي سان مڪمل ٿيو: $action"
        }
    }

    fun getConfirmQuestion(lang: Language): String {
        return when (lang) {
            Language.ENGLISH -> "Do you authorize NIAZ AI to execute this action?"
            Language.URDU -> "کیا آپ نیازی اے آئی کو یہ کارروائی کرنے کی اجازت دیتے ہیں؟"
            Language.SINDHI -> "ڇا توهان نياز اي آءِ کي هي ڪم ڪرڻ جو اختيار ڏيو ٿا؟"
        }
    }
}
