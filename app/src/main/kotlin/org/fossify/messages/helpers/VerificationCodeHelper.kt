package org.fossify.messages.helpers

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

object VerificationCodeHelper {

    // 验证码的正则表达式模式列表
    private val CODE_PATTERNS = listOf(
        // 中文模式
        Pattern.compile("(?:验证码|校验码|检验码|确认码|激活码|动态码|安全码|认证码|授权码|口令)[是为：:]*\\s*[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?"),
        Pattern.compile("[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?\\s*(?:是您?的?|为您?的?)(?:验证码|校验码|检验码|确认码|激活码|动态码|安全码|认证码|授权码)"),
        Pattern.compile("(?:code|码)[是为：:]*\\s*[\\[【「(（]?([A-Za-z0-9]{4,8})[\\]】」)）]?", Pattern.CASE_INSENSITIVE),

        // 英文模式
        Pattern.compile("(?:verification|verify|confirmation|security|auth(?:entication)?|valid(?:ation)?|one[- ]?time|otp|pin)\\s*(?:code|number|pin)?[\\s:is]*[\\[\\(]?([A-Za-z0-9]{4,8})[\\]\\)]?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:code|pin|otp)[\\s:is]+[\\[\\(]?([A-Za-z0-9]{4,8})[\\]\\)]?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("[\\[\\(]?([A-Za-z0-9]{4,8})[\\]\\)]?\\s*(?:is your|is the)?\\s*(?:verification|confirmation|security|auth(?:entication)?|one[- ]?time|otp)?\\s*(?:code|pin|number)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:enter|use|input|type)\\s*(?:code|otp|pin)?[\\s:]*[\\[\\(]?([A-Za-z0-9]{4,8})[\\]\\)]?", Pattern.CASE_INSENSITIVE),

        // 通用数字验证码模式（4-8位纯数字，通常出现在特定上下文中）
        Pattern.compile("(?:(?:验证|确认|安全|动态|校验|认证)[码号]|code|otp|pin)[^0-9]*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9]{4,8})[^0-9]*(?:(?:验证|确认|安全|动态|校验|认证)[码号]|code|otp|pin)", Pattern.CASE_INSENSITIVE),

        // 带有关键词的括号内验证码
        Pattern.compile("[【\\[]\\s*([A-Za-z0-9]{4,8})\\s*[】\\]]"),

        // 短信中常见的格式：您的验证码为123456，或 Your code is 123456
        Pattern.compile("(?:您的|你的|Your)\\s*(?:验证码|code)[是为:：\\s]+([A-Za-z0-9]{4,8})", Pattern.CASE_INSENSITIVE),
    )

    // 关键词列表，用于判断短信是否可能包含验证码
    private val VERIFICATION_KEYWORDS = listOf(
        // 中文关键词
        "验证码", "校验码", "检验码", "确认码", "激活码", "动态码", "安全码", "认证码", "授权码", "口令",
        "短信码", "手机码", "登录码", "注册码", "绑定码", "验证", "核验",
        // 英文关键词
        "verification", "verify", "code", "otp", "pin", "confirmation", "security code",
        "authentication", "one-time", "onetime", "passcode", "password", "valid"
    )

    /**
     * 从短信内容中提取验证码
     * @param message 短信内容
     * @return 提取到的验证码，如果没有找到则返回null
     */
    fun extractVerificationCode(message: String): String? {
        if (message.isBlank()) return null

        // 首先检查是否包含验证码相关关键词
        val lowerMessage = message.lowercase()
        val hasKeyword = VERIFICATION_KEYWORDS.any { lowerMessage.contains(it.lowercase()) }
        if (!hasKeyword) return null

        // 尝试使用各种模式匹配验证码
        for (pattern in CODE_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val code = matcher.group(1)
                // 验证提取的验证码是否合理（4-8位数字或字母数字组合）
                if (code != null && code.length in 4..8 && code.matches(Regex("[A-Za-z0-9]+"))) {
                    return code
                }
            }
        }

        // 如果上述模式都没有匹配到，尝试更宽松的匹配
        // 查找短信中的4-8位数字序列
        if (hasKeyword) {
            val digitPattern = Pattern.compile("\\b([0-9]{4,8})\\b")
            val matcher = digitPattern.matcher(message)
            val candidates = mutableListOf<String>()
            while (matcher.find()) {
                matcher.group(1)?.let { candidates.add(it) }
            }
            // 如果只有一个数字序列，很可能就是验证码
            if (candidates.size == 1) {
                return candidates[0]
            }
            // 如果有多个，返回第一个（通常验证码在短信靠前位置）
            if (candidates.isNotEmpty()) {
                return candidates[0]
            }
        }

        return null
    }

    /**
     * 检查短信是否包含验证码
     */
    fun containsVerificationCode(message: String): Boolean {
        return extractVerificationCode(message) != null
    }

    /**
     * 创建带有高亮验证码的SpannableString
     * @param message 原始短信内容
     * @param highlightColor 高亮颜色
     * @return 带有高亮验证码的SpannableString
     */
    fun getHighlightedMessage(message: String, highlightColor: Int): SpannableString {
        val code = extractVerificationCode(message) ?: return SpannableString(message)

        val spannable = SpannableString(message)
        var startIndex = 0

        // 查找验证码在原文中的所有位置并高亮
        while (true) {
            val index = message.indexOf(code, startIndex)
            if (index == -1) break

            spannable.setSpan(
                ForegroundColorSpan(highlightColor),
                index,
                index + code.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            startIndex = index + code.length
        }

        return spannable
    }
}

