package com.sbkcastro.monitor.ui.claude

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan

/**
 * Lightweight markdown → SpannableStringBuilder renderer for terminal-style UI.
 * Handles: **bold**, `inline code`, ```code blocks```, - lists, headers.
 */
object TerminalRenderer {

    private val CODE_BG = Color.parseColor("#1c2128")
    private val CODE_FG = Color.parseColor("#79c0ff")
    private val BOLD_FG = Color.parseColor("#f0f6fc")
    private val DIM_FG = Color.parseColor("#8b949e")

    fun render(text: String): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        val lines = text.split('\n')
        var inCodeBlock = false
        val codeBuffer = StringBuilder()

        for ((i, line) in lines.withIndex()) {
            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    appendCodeBlock(sb, codeBuffer.toString().trimEnd())
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                if (codeBuffer.isNotEmpty()) codeBuffer.append('\n')
                codeBuffer.append(line)
                continue
            }

            if (i > 0 || sb.isNotEmpty()) sb.append('\n')
            appendLine(sb, line)
        }

        // Unclosed code block — render what we have
        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            appendCodeBlock(sb, codeBuffer.toString().trimEnd())
        }

        return sb
    }

    private fun appendLine(sb: SpannableStringBuilder, line: String) {
        val trimmed = line.trimStart()

        // Headers: dim color
        if (trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
            val content = trimmed.replace(Regex("^#+\\s*"), "")
            val start = sb.length
            sb.append(content)
            sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(BOLD_FG), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return
        }

        // List items: add bullet style
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            sb.append("  ")
            val bulletStart = sb.length
            sb.append("▸ ")
            sb.setSpan(ForegroundColorSpan(DIM_FG), bulletStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            appendInline(sb, trimmed.substring(2))
            return
        }

        // Numbered list
        val numMatch = Regex("^(\\d+)\\.\\s").find(trimmed)
        if (numMatch != null) {
            sb.append("  ")
            val numStart = sb.length
            sb.append("${numMatch.groupValues[1]}. ")
            sb.setSpan(ForegroundColorSpan(DIM_FG), numStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            appendInline(sb, trimmed.substring(numMatch.range.last + 1))
            return
        }

        appendInline(sb, line)
    }

    private fun appendInline(sb: SpannableStringBuilder, text: String) {
        var i = 0
        while (i < text.length) {
            // Bold **text**
            if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    val start = sb.length
                    sb.append(text.substring(i + 2, end))
                    sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(ForegroundColorSpan(BOLD_FG), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 2
                    continue
                }
            }

            // Inline code `text`
            if (text[i] == '`' && (i + 1 >= text.length || text[i + 1] != '`')) {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    val start = sb.length
                    sb.append(" ${text.substring(i + 1, end)} ")
                    sb.setSpan(BackgroundColorSpan(CODE_BG), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(ForegroundColorSpan(CODE_FG), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(TypefaceSpan("monospace"), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 1
                    continue
                }
            }

            sb.append(text[i])
            i++
        }
    }

    private fun appendCodeBlock(sb: SpannableStringBuilder, code: String) {
        if (sb.isNotEmpty()) sb.append('\n')
        val start = sb.length
        sb.append(code)
        sb.setSpan(BackgroundColorSpan(CODE_BG), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(CODE_FG), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(TypefaceSpan("monospace"), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
