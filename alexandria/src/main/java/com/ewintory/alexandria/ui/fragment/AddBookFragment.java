package com.ewintory.alexandria.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ewintory.alexandria.R;
import com.ewintory.alexandria.provider.AlexandriaContract;
import com.ewintory.alexandria.service.BookService;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public final class AddBookFragment extends BaseDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";

    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private final int LOADER_ID = 1;
    private final String EAN_CONTENT = "eanContent";

    private View rootView;
    @Bind(R.id.add_book_card) CardView mBookCardView;
    @Bind(R.id.add_book_ean) EditText mEanView;
    @Bind(R.id.add_book_title) TextView mBookTitleView;
    @Bind(R.id.add_book_subtitle) TextView mBookSubtitleView;
    @Bind(R.id.add_book_authors) TextView mAuthorsView;
    @Bind(R.id.add_book_categories) TextView mCategoriesView;
    @Bind(R.id.add_book_cover) ImageView mBookCoverView;
    @Bind(R.id.save_button) Button mSaveButton;
    @Bind(R.id.dismiss_button) Button mDismissButton;

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    public AddBookFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AlexandriaTheme_Dialog);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().setTitle(R.string.title_add_book);

        mEanView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {/** ignore */}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {/** ignore */}

            @Override public void afterTextChanged(Editable s) {
                String ean = s.toString();
                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                if (ean.length() < 13) {
                    hideCard();
                    return;
                }
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EXTRA_EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBookFragment.this.restartLoader();
            }
        });

        if (savedInstanceState != null) {
            mEanView.setText(savedInstanceState.getString(EAN_CONTENT));
            mEanView.setHint("");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEanView != null) {
            outState.putString(EAN_CONTENT, mEanView.getText().toString());
        }
    }

    //@OnClick(R.id.scan_button)
    public void onScanButton() {
        // This is the callback method that the system will invoke when your button is
        // clicked. You might do this by launching another app or by including the
        //functionality directly in this app.
        // Hint: Use a Try/Catch block to handle the Intent dispatch gracefully, if you
        // are using an external app.
        //when you're done, remove the toast below.
        Context context = getActivity();
        CharSequence text = "This button should let you scan a book for its barcode!";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @OnClick(R.id.save_button)
    public void onSaveButton() {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EXTRA_EAN, mEanView.getText().toString());
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);
        mEanView.setText("");
    }

    @OnClick(R.id.cancel_button)
    public void onCancelButton() {
        mEanView.setText("");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mEanView.getText().length() == 0) {
            return null;
        }
        String eanStr = mEanView.getText().toString();
        if (eanStr.length() == 10 && !eanStr.startsWith("978")) {
            eanStr = "978" + eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        mBookTitleView.setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        mBookSubtitleView.setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        mAuthorsView.setLines(authorsArr.length);
        mAuthorsView.setText(authors.replace(",", "\n"));

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            Glide.with(this)
                    .load(imgUrl)
                    .crossFade()
                    .into(mBookCoverView);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        mCategoriesView.setText(categories);

        mBookCardView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {/** ignore */}

    private void restartLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    private void hideCard() {
        mBookCardView.setVisibility(View.GONE);
    }

    static final ButterKnife.Setter<TextView, String> TEXT = new ButterKnife.Setter<TextView, String>() {
        @Override public void set(TextView textView, String text, int index) {
            textView.setText(text);
        }
    };

    static final ButterKnife.Setter<View, Boolean> VISIBLE = new ButterKnife.Setter<View, Boolean>() {
        @Override public void set(View view, Boolean visible, int index) {
            view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    };

    static final ButterKnife.Setter<View, Boolean> ENABLED = new ButterKnife.Setter<View, Boolean>() {
        @Override public void set(View view, Boolean value, int index) {
            view.setEnabled(value);
        }
    };
}
