package com.example.markdowntest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.os.Build
import android.os.Bundle
import android.text.style.BulletSpan
import android.text.style.ReplacementSpan

import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.example.markdowntest.databinding.ActivityMainBinding
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import io.noties.markwon.*
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.MarkwonTheme

import org.commonmark.node.ListItem

import com.example.markdowntest.shared.*
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.editor.PersistedSpans


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        val markwon = Markwon.builder(this)
//            .usePlugin(SoftBreakAddsNewLinePlugin.create())
//            .usePlugin(CorePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(object :AbstractMarkwonPlugin() {
                 override fun configureTheme(@NonNull builder: MarkwonTheme.Builder) {
                    builder
                        .codeTextColor(Color.BLACK)
                        .codeBackgroundColor(Color.GREEN) // highlight
                        .bulletListItemStrokeWidth(13)
                        .bulletWidth(13)
                }

//                override fun configureSpansFactory(@NonNull builder: MarkwonSpansFactory.Builder) {
//
//                    // store original span factory (provides both bullet and ordered lists)
//                    val  original : SpanFactory = builder.getFactory(ListItem::class.java)!!
//
//                    builder.setFactory(ListItem::class.java) { configuration, props ->
//                        if (CoreProps.LIST_ITEM_TYPE.require(props) == CoreProps.ListItemType.BULLET) {
//                            // additionally inspect bullet level
//                            val level = CoreProps.BULLET_LIST_ITEM_LEVEL.require(props)
//                            println(level)
//
//                            // return _system_ bullet span, but can be any
//                            return@setFactory BulletSpan()
//                        }
//                        if (original != null) original.getSpans(configuration, props) else null
//                    }
//                }

            })
            .build()

        val editor = MarkwonEditor.builder(markwon)
            .useEditHandler(EmphasisEditHandler()) // "*, _" slanted
            .useEditHandler(StrongEmphasisEditHandler()) // "**, __" bold
            .useEditHandler(StrikethroughEditHandler()) // "~~" strike thru in the middle
            .useEditHandler(CodeEditHandler()) // "'" code font
            .useEditHandler(HeadingEditHandler()) // "#, ##" 2 headers only
            .useEditHandler(BulletListHandler())

//            .useEditHandler(BlockQuoteEditHandler()) // ">" indented replies

            .punctuationSpan( HidePunctuationSpan().javaClass, object: PersistedSpans.SpanFactory<HidePunctuationSpan> {
                @NonNull
                override fun create(): HidePunctuationSpan {
                    return HidePunctuationSpan()
                }
            }) //Hiding Markdown Syntax
            .build()


        val markwon_editText = findViewById<EditText>(R.id.markwon_edittext)

        initBottomBar(this, markwon_editText)

        // set edit listener
        markwon_editText.addTextChangedListener(MarkwonEditorTextWatcher.withProcess(editor));

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        binding.fab.hide()
        
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}



private class HidePunctuationSpan: ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        @Nullable fm: FontMetricsInt?
    ): Int {
        // last space (which is swallowed until next non-space character appears)
        // block quote
        // code tick

//      Debug.i("text: '%s', %d-%d (%d)", text.subSequence(start, end), start, end, text.length());
        if (end == text.length) {
            // TODO: find first non-space character (not just first one because commonmark allows
            //  arbitrary (0-3) white spaces before content starts

            //  TODO: if all white space - render?
            val c = text[start]
            if ('#' == c || '>' == c
                || '-' == c //
                || '+' == c // `*` is fine but only for a list
//                || '*' == c //comment

                || isBulletList(text, c, start, end)
                || Character.isDigit(c) // assuming ordered list (replacement should only happen for ordered lists)
                || Character.isWhitespace(c)
            ) {
                return (paint.measureText(text, start, end) + 0.5f).toInt()
            }
        }
        return 0
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun draw(
        @NonNull canvas:Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        @NonNull paint: Paint)
    {
        // will be called only when getSize is not 0 (and if it was once reported as 0...)
        if (end == text.length) {

            // if first non-space is `*` then check for is bullet
            //  else `**` would be still rendered at the end of the emphasis
            if (text.get(start) == '*'
                && !isBulletList(text, '*', start, end)) {
                return;
            }

            // TODO: inline code last tick received here, handle it (do not highlight)
            //  why can't we have reported width in this method for supplied text?

            // let's use color to make it distinct from the rest of the text for demonstration purposes
            paint.setColor(Color.GREEN)

            canvas.drawText(text, start, end, x, y.toFloat(), paint);
        }
    }

}

private fun isBulletList(@NonNull text:CharSequence, firstChar:Char, start:Int , end:Int): Boolean {
    println('*' == firstChar
            && ((end - start == 1) || (Character.isWhitespace(text.elementAt(start + 1)))))
    return '*' == firstChar
            && ((end - start == 1) || (Character.isWhitespace(text.elementAt(start + 1))))
}

private fun initBottomBar(view: MainActivity, editText: EditText) {
    // all except block-quote wraps if have selection, or inserts at current cursor position
    val bold: Button = view.findViewById(R.id.bold)
    val italic: Button = view.findViewById(R.id.italic)
    val strike: Button = view.findViewById(R.id.strike)
    val quote: Button = view.findViewById(R.id.quote)
    val code: Button = view.findViewById(R.id.code)

//    addSpan(bold, StrongEmphasisSpan())
//    addSpan(italic, EmphasisSpan())
//    addSpan(strike, StrikethroughSpan())

    bold.setOnClickListener(InsertOrWrapClickListener(editText, "**"))
    italic.setOnClickListener(InsertOrWrapClickListener(editText, "_"))
    strike.setOnClickListener(InsertOrWrapClickListener(editText, "~~"))
    code.setOnClickListener(InsertOrWrapClickListener(editText, "`"))
    quote.setOnClickListener(InsertOrWrapClickListener(editText, "-"))
//    quote.setOnClickListener {
//        val start = editText.selectionStart
//        val end = editText.selectionEnd
//        if (start < 0) {
//            return@setOnClickListener
//        }
//        if (start == end) {
//            editText.text.insert(start, "* ")
//        } else {
//            // wrap the whole selected area in a quote
//            val newLines: MutableList<Int> = ArrayList(3)
//            newLines.add(start)
//            val text = editText.text.subSequence(start, end).toString()
//            var index = text.indexOf('\n')
//            while (index != -1) {
//                newLines.add(start + index + 1)
//                index = text.indexOf('\n', index + 1)
//            }
//            for (i in newLines.indices.reversed()) {
//                editText.text.insert(newLines[i], "* ")
//            }
//        }
//    }
}

private class InsertOrWrapClickListener(
    private val editText: EditText,
    private val text: String
) : View.OnClickListener {
    override fun onClick(v: View) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start < 0) {
            return
        }
        if (start == end) {
            // insert at current position
            editText.text.insert(start, text)
        } else {
            editText.text.insert(end, text)
            editText.text.insert(start, text)
        }
    }

}


