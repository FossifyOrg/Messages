package org.fossify.messages.helpers

import java.util.regex.Pattern

/**
 * 多语言验证码提取器
 * 支持: 简体中文、繁体中文、英文、日语、韩语、泰语、马来语、印尼语、越南语、法语、德语、西班牙语、葡萄牙语、俄语、阿拉伯语
 */
object VerificationCodeExtractor {

    // 验证码关键词 - 多语言
    private val KEYWORDS = listOf(
        // 简体中文
        "验证码", "校验码", "检验码", "确认码", "激活码", "动态码", "安全码", "认证码", "授权码",
        "短信码", "手机码", "登录码", "注册码", "绑定码", "验证", "核验", "口令", "密码",

        // 繁体中文
        "驗證碼", "校驗碼", "檢驗碼", "確認碼", "激活碼", "動態碼", "安全碼", "認證碼", "授權碼",
        "簡訊碼", "手機碼", "登入碼", "註冊碼", "綁定碼", "驗證", "核驗", "口令", "密碼",

        // 英文
        "verification code", "verify code", "confirmation code", "security code", "auth code",
        "authentication code", "validation code", "one-time code", "onetime code", "otp",
        "pin code", "passcode", "pass code", "access code", "login code", "sign-in code",
        "code is", "code:", "your code", "the code",

        // 日语
        "認証コード", "確認コード", "検証コード", "セキュリティコード", "ワンタイムコード",
        "暗証番号", "認証番号", "確認番号", "パスコード", "コードは", "コード：",

        // 韩语
        "인증코드", "인증번호", "확인코드", "확인번호", "보안코드", "일회용코드",
        "비밀번호", "인증 코드", "인증 번호",

        // 泰语
        "รหัสยืนยัน", "รหัส OTP", "รหัสผ่าน", "รหัสความปลอดภัย", "รหัสตรวจสอบ",

        // 马来语 / 印尼语
        "kod pengesahan", "kod keselamatan", "kod otp", "kode verifikasi", "kode keamanan",
        "kode otp", "kode konfirmasi", "kode akses",

        // 越南语
        "mã xác nhận", "mã xác minh", "mã otp", "mã bảo mật", "mã đăng nhập",

        // 法语
        "code de vérification", "code de confirmation", "code de sécurité", "code d'accès",

        // 德语
        "bestätigungscode", "sicherheitscode", "verifizierungscode", "zugangscode",

        // 西班牙语
        "código de verificación", "código de confirmación", "código de seguridad", "código de acceso",

        // 葡萄牙语
        "código de verificação", "código de confirmação", "código de segurança", "código de acesso",

        // 俄语
        "код подтверждения", "код безопасности", "код доступа", "проверочный код",

        // 阿拉伯语
        "رمز التحقق", "رمز التأكيد", "رمز الأمان"
    )

    // 验证码模式 - 按优先级排序
    private val CODE_PATTERNS = listOf(
        // ===== 简体中文模式 =====
        // 验证码是/为 123456
        Pattern.compile("(?:验证码|校验码|检验码|确认码|激活码|动态码|安全码|认证码|授权码|口令|密码)[是为：:]*\\s*[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?"),
        // 123456 是您的验证码
        Pattern.compile("[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?\\s*(?:是您?的?|为您?的?)(?:验证码|校验码|确认码|动态码|安全码)"),
        // 您的验证码为 123456
        Pattern.compile("(?:您的|你的)\\s*(?:验证码|校验码|确认码|动态码|安全码)[是为：:]*\\s*([A-Za-z0-9]{4,8})"),

        // ===== 繁体中文模式 =====
        Pattern.compile("(?:驗證碼|校驗碼|檢驗碼|確認碼|激活碼|動態碼|安全碼|認證碼|授權碼|口令|密碼)[是為：:]*\\s*[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?"),
        Pattern.compile("[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?\\s*(?:是您?的?|為您?的?)(?:驗證碼|校驗碼|確認碼|動態碼|安全碼)"),
        Pattern.compile("(?:您的|你的)\\s*(?:驗證碼|校驗碼|確認碼|動態碼|安全碼)[是為：:]*\\s*([A-Za-z0-9]{4,8})"),

        // ===== 英文模式 =====
        // verification code is 123456 / your code is 123456
        Pattern.compile("(?:verification|confirmation|security|auth(?:entication)?|valid(?:ation)?|one[- ]?time|otp|access|login|sign[- ]?in)\\s*(?:code|pin|number)?[\\s:is]+[\\[\\(]?([A-Za-z0-9]{4,8})[\\]\\)]?", Pattern.CASE_INSENSITIVE),
        // code: 123456 / code is 123456
        Pattern.compile("(?:code|pin|otp|passcode)[\\s:is]+[\\[\\(]?([A-Za-z0-9]{4,8})[\\]\\)]?", Pattern.CASE_INSENSITIVE),
        // 123456 is your verification code
        Pattern.compile("[\\[\\(]?([A-Za-z0-9]{4,8})[\\]\\)]?\\s+(?:is\\s+)?(?:your\\s+)?(?:verification|confirmation|security|one[- ]?time)?\\s*(?:code|pin|otp)", Pattern.CASE_INSENSITIVE),
        // your code: 123456
        Pattern.compile("(?:your|the)\\s+(?:verification\\s+)?(?:code|pin|otp)[\\s:is]+([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),
        // enter code 123456 / use code 123456
        Pattern.compile("(?:enter|use|input|type)\\s+(?:code|otp|pin)?\\s*:?\\s*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 日语模式 =====
        Pattern.compile("(?:認証コード|確認コード|検証コード|セキュリティコード|ワンタイムコード|暗証番号|認証番号|確認番号|パスコード)[はは：:]*\\s*[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?"),
        Pattern.compile("コード[はは：:]*\\s*([A-Za-z0-9]{4,8})"),

        // ===== 韩语模式 =====
        Pattern.compile("(?:인증코드|인증번호|확인코드|확인번호|보안코드|일회용코드|비밀번호|인증\\s*코드|인증\\s*번호)[는은：:]*\\s*[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?"),

        // ===== 泰语模式 =====
        Pattern.compile("(?:รหัสยืนยัน|รหัส\\s*OTP|รหัสผ่าน|รหัสความปลอดภัย|รหัสตรวจสอบ)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 马来语/印尼语模式 =====
        Pattern.compile("(?:kod\\s*pengesahan|kod\\s*keselamatan|kod\\s*otp|kode\\s*verifikasi|kode\\s*keamanan|kode\\s*otp|kode\\s*konfirmasi|kode\\s*akses)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 越南语模式 =====
        Pattern.compile("(?:mã\\s*xác\\s*nhận|mã\\s*xác\\s*minh|mã\\s*otp|mã\\s*bảo\\s*mật|mã\\s*đăng\\s*nhập)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 法语模式 =====
        Pattern.compile("(?:code\\s*de\\s*vérification|code\\s*de\\s*confirmation|code\\s*de\\s*sécurité|code\\s*d'accès)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 德语模式 =====
        Pattern.compile("(?:bestätigungscode|sicherheitscode|verifizierungscode|zugangscode)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 西班牙语模式 =====
        Pattern.compile("(?:código\\s*de\\s*verificación|código\\s*de\\s*confirmación|código\\s*de\\s*seguridad|código\\s*de\\s*acceso)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 葡萄牙语模式 =====
        Pattern.compile("(?:código\\s*de\\s*verificação|código\\s*de\\s*confirmação|código\\s*de\\s*segurança|código\\s*de\\s*acesso)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 俄语模式 =====
        Pattern.compile("(?:код\\s*подтверждения|код\\s*безопасности|код\\s*доступа|проверочный\\s*код)[\\s:：]*([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),

        // ===== 阿拉伯语模式 =====
        Pattern.compile("(?:رمز\\s*التحقق|رمز\\s*التأكيد|رمز\\s*الأمان)[\\s:：]*([A-Za-z0-9]{4,8})"),

        // ===== 通用模式 =====
        // 括号内的验证码 【123456】 [123456]
        Pattern.compile("[【\\[]\\s*([A-Za-z0-9]{4,8})\\s*[】\\]]"),
        // G-123456 或 类似格式
        Pattern.compile("\\b([A-Z]-[0-9]{4,8})\\b"),
        // 纯数字验证码（需要有关键词上下文）
        Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
    )

    /**
     * 从短信内容中提取验证码
     * @param message 短信内容
     * @return 提取到的验证码，如果没有找到则返回null
     */
    fun extractCode(message: String): String? {
        if (message.isBlank()) return null

        val lowerMessage = message.lowercase()

        // 检查是否包含验证码相关关键词
        val hasKeyword = KEYWORDS.any { lowerMessage.contains(it.lowercase()) }

        // 尝试使用正则表达式匹配
        for (pattern in CODE_PATTERNS) {
            val matcher = pattern.matcher(message)
            while (matcher.find()) {
                val code = matcher.group(1)
                if (code != null && isValidCode(code)) {
                    return code
                }
            }
        }

        // 如果有关键词但没匹配到，尝试宽松匹配
        if (hasKeyword) {
            return extractCodeLoose(message)
        }

        return null
    }

    /**
     * 宽松模式提取验证码
     */
    private fun extractCodeLoose(message: String): String? {
        // 查找4-8位数字序列
        val digitPattern = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
        val matcher = digitPattern.matcher(message)
        val candidates = mutableListOf<String>()

        while (matcher.find()) {
            val code = matcher.group(1)
            if (code != null && isValidCode(code)) {
                candidates.add(code)
            }
        }

        // 过滤掉可能是日期、时间、金额的数字
        val filtered = candidates.filter { code ->
            !isLikelyDate(code) && !isLikelyTime(code) && !isLikelyAmount(message, code)
        }

        return filtered.firstOrNull()
    }

    /**
     * 验证码有效性检查
     */
    private fun isValidCode(code: String): Boolean {
        // 长度检查
        if (code.length !in 4..8) return false

        // 必须是字母数字组合
        if (!code.matches(Regex("[A-Za-z0-9]+"))) return false

        // 排除全0或全相同数字
        if (code.all { it == code[0] }) return false

        // 排除连续数字如 123456, 654321
        if (isSequential(code)) return false

        return true
    }

    /**
     * 检查是否是连续数字
     */
    private fun isSequential(code: String): Boolean {
        if (!code.all { it.isDigit() }) return false
        if (code.length < 4) return false

        val increasing = code.zipWithNext().all { (a, b) -> b - a == 1 }
        val decreasing = code.zipWithNext().all { (a, b) -> a - b == 1 }

        return increasing || decreasing
    }

    /**
     * 检查是否像日期
     */
    private fun isLikelyDate(code: String): Boolean {
        if (code.length == 8) {
            // YYYYMMDD 格式
            val year = code.substring(0, 4).toIntOrNull() ?: return false
            val month = code.substring(4, 6).toIntOrNull() ?: return false
            val day = code.substring(6, 8).toIntOrNull() ?: return false
            return year in 1900..2100 && month in 1..12 && day in 1..31
        }
        if (code.length == 6) {
            // YYMMDD 格式
            val month = code.substring(2, 4).toIntOrNull() ?: return false
            val day = code.substring(4, 6).toIntOrNull() ?: return false
            return month in 1..12 && day in 1..31
        }
        return false
    }

    /**
     * 检查是否像时间
     */
    private fun isLikelyTime(code: String): Boolean {
        if (code.length == 4) {
            // HHMM 格式
            val hour = code.substring(0, 2).toIntOrNull() ?: return false
            val minute = code.substring(2, 4).toIntOrNull() ?: return false
            return hour in 0..23 && minute in 0..59
        }
        if (code.length == 6) {
            // HHMMSS 格式
            val hour = code.substring(0, 2).toIntOrNull() ?: return false
            val minute = code.substring(2, 4).toIntOrNull() ?: return false
            val second = code.substring(4, 6).toIntOrNull() ?: return false
            return hour in 0..23 && minute in 0..59 && second in 0..59
        }
        return false
    }

    /**
     * 检查是否像金额
     */
    private fun isLikelyAmount(message: String, code: String): Boolean {
        val amountPatterns = listOf(
            "\\$\\s*${Regex.escape(code)}",
            "￥\\s*${Regex.escape(code)}",
            "¥\\s*${Regex.escape(code)}",
            "${Regex.escape(code)}\\s*(?:元|円|원|฿|RM|USD|CNY|JPY|KRW|THB)"
        )
        return amountPatterns.any { Pattern.compile(it, Pattern.CASE_INSENSITIVE).matcher(message).find() }
    }
}

