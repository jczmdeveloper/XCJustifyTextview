package com.czm.xctextview

import android.content.Context
import android.graphics.Canvas
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import java.io.UnsupportedEncodingException
import java.util.*

/**
 * Description: 自定义两端左右对齐的TextView，中英文排版效果
 * 非中文单词换行截断
 */
class XCJustifyTextView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    /**
     * 绘制文字的起始Y坐标
     */
    private var mLineY = 0f
    /**
     * 文字的宽度
     */
    private var mViewWidth = 0
    /**
     * 段落间距
     */
    private val paragraphSpacing = dipToPx(getContext(), 15f)
    /**
     * 行间距
     */
    private val lineSpacing = dipToPx(getContext(), 2f)
    /**
     * 当前所有行数的集合
     */
    private var mParagraphLineList: ArrayList<List<List<String>>>? =
        null
    /**
     * 当前所有的行数
     */
    private var mLineCount = 0
    /**
     * 每一段单词的内容集合
     */
    private var mParagraphWordList: ArrayList<List<String>>? =
        null
    /**
     * 英语单词元音字母
     */
    private val vowel = arrayOf("a", "e", "i", "o", "u")
    /**
     * 英语单词元音字母集合
     */
    private val vowels =
        Arrays.asList(*vowel)
    /**
     * 当前测量的间距
     */
    private var mMeasuredWidth = 0
    /**
     * 左padding
     */
    private var mPaddingStart = 0
    /**
     * 右padding
     */
    private var mPaddingEnd = 0
    /**
     * 顶padding
     */
    private var mPaddingTop = 0
    /**
     * 底padding
     */
    private var mPaddingBottom = 0
    /**
     * 布局的方向
     */
    private var textGravity = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mParagraphLineList = null
        mParagraphWordList = null
        mLineY = 0f
        mMeasuredWidth = getMeasuredWidth()
        mPaddingStart = getPaddingStart()
        mPaddingEnd = getPaddingEnd()
        mPaddingTop = getPaddingTop()
        mPaddingBottom = getPaddingBottom()
        mViewWidth = mMeasuredWidth - mPaddingStart - mPaddingEnd
        paragraphList
        for (frontList in mParagraphWordList!!) {
            mParagraphLineList!!.add(getLineList(frontList))
        }
        setMeasuredDimension(
            mMeasuredWidth,
            (mParagraphLineList!!.size - 1) * paragraphSpacing + mLineCount * (lineHeight + lineSpacing) + mPaddingTop + mPaddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        val paint = paint
        paint.color = currentTextColor
        paint.drawableState = drawableState
        mLineY = 0f
        val textSize = textSize
        mLineY += textSize + mPaddingTop
        val layout = layout ?: return
        textGravity = getTextGravity()
        adjust(canvas, paint)
    }

    /**
     * @param frontList
     * @return 计算每一段绘制的内容
     */
    @Synchronized
    private fun getLineList(frontList: List<String>): List<List<String>> {
        Log.i(TAG, "getLineList ")
        val sb = StringBuilder()
        val lineLists: MutableList<List<String>> =
            ArrayList()
        val lineList: MutableList<String> =
            ArrayList()
        var width = 0f
        var temp = ""
        var front = ""
        for (i in frontList.indices) {
            front = frontList[i]
            if (!TextUtils.isEmpty(temp)) {
                sb.append(temp)
                lineList.add(temp)
                if (!isCN(temp)) {
                    sb.append(BLANK)
                }
                temp = ""
            }
            if (isCN(front)) {
                sb.append(front)
            } else {
                if (i + 1 < frontList.size) {
                    val nextFront = frontList[i + 1]
                    if (isCN(nextFront)) {
                        sb.append(front)
                    } else {
                        sb.append(front).append(BLANK)
                    }
                } else {
                    sb.append(front)
                }
            }
            lineList.add(front)
            width = StaticLayout.getDesiredWidth(sb.toString(), paint)
            if (width > mViewWidth) { // 先判断最后一个单词是否是英文的，是的话则切割，否则的话就移除最后一个
                val lastIndex = lineList.size - 1
                val lastWord = lineList[lastIndex]
                var lastTemp = ""
                lineList.removeAt(lastIndex)
                if (isCN(lastWord)) {
                    addLines(lineLists, lineList)
                    lastTemp = lastWord
                } else { // 否则的话则截取字符串
                    val substring = sb.substring(0, sb.length - lastWord.length - 1)
                    sb.delete(0, sb.toString().length)
                    sb.append(substring).append(BLANK)
                    var tempLastWord = ""
                    val length = lastWord.length
                    if (length <= 3) {
                        addLines(lineLists, lineList)
                        lastTemp = lastWord
                    } else {
                        var cutoffIndex = 0
                        for (j in 0 until length) {
                            tempLastWord = lastWord[j].toString()
                            sb.append(tempLastWord)
                            if (tempLastWord in vowels) { // 根据元音字母来进行截断
                                if (j + 1 < length) {
                                    val nextTempLastWord =
                                        lastWord[j + 1].toString()
                                    sb.append(nextTempLastWord)
                                    width = StaticLayout.getDesiredWidth(sb.toString(), paint)
                                    cutoffIndex = j
                                    if (width > mViewWidth) {
                                        lastTemp =
                                            if (j > 2 && j <= length - 2) { // 单词截断后，前面的字符小于2个时，则不进行截断
                                                val lastFinalWord =
                                                    lastWord.substring(0, cutoffIndex + 2) + "-"
                                                lineList.add(lastFinalWord)
                                                addLines(lineLists, lineList)
                                                lastWord.substring(cutoffIndex + 2, length)
                                            } else {
                                                addLines(lineLists, lineList)
                                                lastWord
                                            }
                                        break
                                    }
                                } else {
                                    addLines(lineLists, lineList)
                                    lastTemp = lastWord
                                    break
                                }
                            }
                            width = StaticLayout.getDesiredWidth(sb.toString(), paint)
                            // 找不到元音，则走默认的逻辑
                            if (width > mViewWidth) {
                                lastTemp =
                                    if (j > 2 && j <= length - 2) { // 单词截断后，前面的字符小于2个时，则不进行截断
                                        val lastFinalWord =
                                            lastWord.substring(0, j) + "-"
                                        lineList.add(lastFinalWord)
                                        addLines(lineLists, lineList)
                                        lastWord.substring(j, length)
                                    } else {
                                        addLines(lineLists, lineList)
                                        lastWord
                                    }
                                break
                            }
                        }
                    }
                }
                sb.delete(0, sb.toString().length)
                temp = lastTemp
            }
            if (lineList.size > 0 && i == frontList.size - 1) {
                addLines(lineLists, lineList)
            }
        }
        if (!TextUtils.isEmpty(temp)) {
            lineList.add(temp)
            addLines(lineLists, lineList)
        }
        mLineCount += lineLists.size
        return lineLists
    }

    /**
     * 添加一行到单词内容
     *
     * @param lineLists 总单词集合
     * @param lineList 当前要添加的集合
     */
    private fun addLines(
        lineLists: MutableList<List<String>>?,
        lineList: MutableList<String>?
    ) {
        if (lineLists == null || lineList == null) {
            return
        }
        val tempLines: List<String> =
            ArrayList(lineList)
        lineLists.add(tempLines)
        lineList.clear()
    }

    /**
     * 获取段落
     */
    private val paragraphList: Unit
        private get() {
            val text =
                text.toString().replace("  ".toRegex(), "").replace("   ".toRegex(), "")
                    .replace("\\r".toRegex(), "").trim { it <= ' ' }
            mLineCount = 0
            val items = text.split("\\n").toTypedArray()
            mParagraphLineList =
                ArrayList()
            mParagraphWordList = ArrayList()
            for (item in items) {
                if (item.length != 0) {
                    mParagraphWordList!!.add(getWordList(item))
                }
            }
        }

    /**
     * 截取每一段内容的每一个单词
     *
     * @param text
     * @return
     */
    @Synchronized
    private fun getWordList(text: String): List<String> {
        if (TextUtils.isEmpty(text)) {
            return ArrayList()
        }
        Log.i(TAG, "getWordList ")
        val frontList: MutableList<String> =
            ArrayList()
        val str = StringBuilder()
        for (i in 0 until text.length) {
            val charAt = text[i].toString()
            if (charAt != BLANK) {
                if (checkIsSymbol(charAt)) {
                    val isEmptyStr = str.length == 0
                    str.append(charAt)
                    if (!isEmptyStr) { // 中英文都需要将字符串添加到这里；
                        frontList.add(str.toString())
                        str.delete(0, str.length)
                    }
                } else {
                    if (isCN(str.toString())) {
                        frontList.add(str.toString())
                        str.delete(0, str.length)
                    }
                    str.append(charAt)
                }
            } else {
                if (!TextUtils.isEmpty(str.toString())) {
                    frontList.add(
                        str.toString().replace(
                            BLANK.toRegex(),
                            ""
                        )
                    )
                    str.delete(0, str.length)
                }
            }
        }
        if (str.length != 0) {
            frontList.add(str.toString())
            str.delete(0, str.length)
        }
        return frontList
    }

    /**
     * 中英文排版效果
     *
     * @param canvas
     */
    @Synchronized
    private fun adjust(canvas: Canvas, paint: TextPaint) {
        val size = mParagraphWordList!!.size
        for (j in 0 until size) { // 遍历每一段
            val lineList =
                mParagraphLineList!![j]
            for (i in lineList.indices) { // 遍历每一段的每一行
                val lineWords = lineList[i]
                if (i == lineList.size - 1) {
                    drawScaledEndText(canvas, lineWords, paint)
                } else {
                    drawScaledText(canvas, lineWords, paint)
                }
                mLineY += (lineHeight + lineSpacing).toFloat()
            }
            mLineY += paragraphSpacing.toFloat()
        }
    }

    /**
     * 绘制最后一行文字
     *
     * @param canvas
     * @param lineWords
     * @param paint
     */
    private fun drawScaledEndText(
        canvas: Canvas?,
        lineWords: List<String>?,
        paint: TextPaint?
    ) {
        if (canvas == null || lineWords == null || paint == null) {
            return
        }
        val sb = StringBuilder()
        for (aSplit in lineWords) {
            if (isCN(aSplit)) {
                sb.append(aSplit)
            } else {
                sb.append(aSplit).append(BLANK)
            }
        }
        /**
         * 最后一行适配布局方向
         * android:gravity=""
         * android:textAlignment=""
         * 默认不设置则为左边
         * 如果同时设置gravity和textAlignment属性，则以textAlignment的属性为准
         * 也就是说textAlignment的属性优先级大于gravity的属性
         *
         */
        if (GRAVITY_START == textGravity) {
            canvas.drawText(sb.toString(), mPaddingStart.toFloat(), mLineY, paint)
        } else if (GRAVITY_END == textGravity) {
            val width = StaticLayout.getDesiredWidth(sb.toString(), getPaint())
            canvas.drawText(sb.toString(), mMeasuredWidth - width - mPaddingStart, mLineY, paint)
        } else {
            val width = StaticLayout.getDesiredWidth(sb.toString(), getPaint())
            canvas.drawText(sb.toString(), (mViewWidth - width) / 2, mLineY, paint)
        }
    }

    /**
     * 获取布局的方向
     */
    private fun getTextGravity(): Int {
        val layoutDirection = layoutDirection
        val absoluteGravity =
            Gravity.getAbsoluteGravity(gravity, layoutDirection)
        val lastGravity = absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK
        val textAlignment = textAlignment
        return if (View.TEXT_ALIGNMENT_TEXT_START == textAlignment || View.TEXT_ALIGNMENT_VIEW_START == textAlignment || Gravity.LEFT == lastGravity
        ) {
            GRAVITY_START
        } else if (View.TEXT_ALIGNMENT_TEXT_END == textAlignment || View.TEXT_ALIGNMENT_VIEW_END == textAlignment || Gravity.RIGHT == lastGravity
        ) {
            GRAVITY_END
        } else {
            GRAVITY_CENTER
        }
    }

    /**
     * 绘制左右对齐效果
     *
     * @param canvas
     * @param line
     * @param paint
     */
    private fun drawScaledText(
        canvas: Canvas?,
        line: List<String>?,
        paint: TextPaint?
    ) {
        if (canvas == null || line == null || paint == null) {
            return
        }
        val sb = StringBuilder()
        for (aSplit in line) {
            sb.append(aSplit)
        }
        val lineWidth = StaticLayout.getDesiredWidth(sb, getPaint())
        var cw = 0f
        cw = if (GRAVITY_START == textGravity) {
            mPaddingStart.toFloat()
        } else if (GRAVITY_END == textGravity) {
            mPaddingEnd.toFloat()
        } else {
            mPaddingStart.toFloat()
        }
        val d = (mViewWidth - lineWidth) / (line.size - 1)
        for (aSplit in line) {
            canvas.drawText(aSplit, cw, mLineY, getPaint())
            cw += StaticLayout.getDesiredWidth(aSplit + "", paint) + d
        }
    }

    /**
     * 功能：判断字符串是否有中文
     *
     * @param str
     * @return
     */
    fun isCN(str: String): Boolean {
        try {
            val bytes = str.toByteArray(charset("UTF-8"))
            return if (bytes.size == str.length) {
                false
            } else {
                true
            }
        } catch (e: UnsupportedEncodingException) { // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return false
    }

    /**
     * 判断是否包含标点符号等内容
     *
     * @param s
     * @return
     */
    fun checkIsSymbol(s: String): Boolean {
        var b = false
        var tmp = s
        tmp = tmp.replace("\\p{P}".toRegex(), "")
        if (s.length != tmp.length) {
            b = true
        }
        return b
    }

    companion object {
        private val TAG = XCJustifyTextView::class.java.simpleName
        /**
         * 起始位置
         */
        private const val GRAVITY_START = 1001
        /**
         * 结尾位置
         */
        private const val GRAVITY_END = 1002
        /**
         * 中间位置
         */
        private const val GRAVITY_CENTER = 1003
        /**
         * 空格字符
         */
        private const val BLANK = " "

        fun dipToPx(var0: Context, var1: Float): Int {
            val var2 = var0.resources.displayMetrics.density
            return (var1 * var2 + 0.5f).toInt()
        }
    }
}
