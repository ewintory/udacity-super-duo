package com.ewintory.alexandria.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public final class AlexandriaDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "alexandria.db";

    public AlexandriaDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_BOOK_TABLE = "CREATE TABLE " + AlexandriaContract.BookEntry.TABLE_NAME + " ("+
                AlexandriaContract.BookEntry._ID + " INTEGER PRIMARY KEY," +
                AlexandriaContract.BookEntry.TITLE + " TEXT NOT NULL," +
                AlexandriaContract.BookEntry.SUBTITLE + " TEXT ," +
                AlexandriaContract.BookEntry.DESC + " TEXT ," +
                AlexandriaContract.BookEntry.IMAGE_URL + " TEXT, " +
                "UNIQUE ("+ AlexandriaContract.BookEntry._ID +") ON CONFLICT IGNORE)";

        final String SQL_CREATE_AUTHOR_TABLE = "CREATE TABLE " + AlexandriaContract.AuthorEntry.TABLE_NAME + " ("+
                AlexandriaContract.AuthorEntry._ID + " INTEGER," +
                AlexandriaContract.AuthorEntry.AUTHOR + " TEXT," +
                " FOREIGN KEY (" + AlexandriaContract.AuthorEntry._ID + ") REFERENCES " +
                AlexandriaContract.BookEntry.TABLE_NAME + " (" + AlexandriaContract.BookEntry._ID + "))";

        final String SQL_CREATE_CATEGORY_TABLE = "CREATE TABLE " + AlexandriaContract.CategoryEntry.TABLE_NAME + " ("+
                AlexandriaContract.CategoryEntry._ID + " INTEGER," +
                AlexandriaContract.CategoryEntry.CATEGORY + " TEXT," +
                " FOREIGN KEY (" + AlexandriaContract.CategoryEntry._ID + ") REFERENCES " +
                AlexandriaContract.BookEntry.TABLE_NAME + " (" + AlexandriaContract.BookEntry._ID + "))";


        Log.d("sql-statments",SQL_CREATE_BOOK_TABLE);
        Log.d("sql-statments",SQL_CREATE_AUTHOR_TABLE);
        Log.d("sql-statments",SQL_CREATE_CATEGORY_TABLE);

        db.execSQL(SQL_CREATE_BOOK_TABLE);
        db.execSQL(SQL_CREATE_AUTHOR_TABLE);
        db.execSQL(SQL_CREATE_CATEGORY_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
