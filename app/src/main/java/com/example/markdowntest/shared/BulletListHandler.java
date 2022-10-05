package com.example.markdowntest.shared;

import android.text.Editable;
import android.text.Spanned;

import androidx.annotation.NonNull;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.core.spans.BulletListItemSpan;
import io.noties.markwon.editor.EditHandler;
import io.noties.markwon.editor.MarkwonEditorUtils;
import io.noties.markwon.editor.PersistedSpans;

public class BulletListHandler implements EditHandler<BulletListItemSpan> {

    private MarkwonTheme theme;

    @Override
    public void init(@NonNull Markwon markwon) {
        this.theme = markwon.configuration().theme();
    }

    @Override
    public void configurePersistedSpans(@NonNull PersistedSpans.Builder builder) {
        builder.persistSpan(BulletListItemSpan.class, () -> new BulletListItemSpan(theme, 1));
    }

    @Override
    public void handleMarkdownSpan(
            @NonNull PersistedSpans persistedSpans,
            @NonNull Editable editable,
            @NonNull String input,
            @NonNull BulletListItemSpan span,
            int spanStart,
            int spanTextLength) {
        final MarkwonEditorUtils.Match match =
                MarkwonEditorUtils.findDelimited(input, spanStart, "* ");

        if (match != null) {
            final int index = input.indexOf('\n', spanStart + spanTextLength);
            final int end = index < 0
                    ? input.length()
                    : index;
            editable.setSpan(
                    persistedSpans.get(BulletListItemSpan.class),
                    match.start(),
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    @NonNull
    @Override
    public Class<BulletListItemSpan> markdownSpanType() {
        return BulletListItemSpan.class;
    }

}
