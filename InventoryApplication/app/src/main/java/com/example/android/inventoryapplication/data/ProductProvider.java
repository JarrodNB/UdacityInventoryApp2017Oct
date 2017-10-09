package com.example.android.inventoryapplication.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;
import java.math.BigDecimal;

import com.example.android.inventoryapplication.data.ProductContract.ProductEntry;

/**
 * Created by jnbcb on 10/5/2017.
 */

public class ProductProvider extends ContentProvider {

    public static final String LOG_TAG = ProductProvider.class.getSimpleName();

    private static final int PRODUCTS = 100;

    private static final int PRODUCTS_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // Adds uris for the matcher
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS, PRODUCTS);
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS + "/#", PRODUCTS_ID);
    }

    private ProductDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDbHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;
        // gives id of URI
        int uriCode = sUriMatcher.match(uri);
        // determines if query is for one product or entire database
        switch(uriCode){
            case PRODUCTS:
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCTS_ID:
                selection = ProductEntry._ID + "=?";
                // gets id for where id ==
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                // throws exception if URI is wrong
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // notifies that database has been changed
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCTS_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertProduct(Uri uri, ContentValues contentValues) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Checks if values are correct
        checkContentValues(contentValues, false);
        long id = database.insert(ProductEntry.TABLE_NAME, null, contentValues);
        // if id is -1 then insertion has failed
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCTS_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        // if rowsdeleted is 0 then the deletion failed and no need to notify
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // if no values are changed then method ends
        if (contentValues.size() == 0) {
            return 0;
        }
        checkContentValues(contentValues, true);
        int rowsUpdated = 0;
        switch (match) {
            case PRODUCTS:
                rowsUpdated = database.update(ProductEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            case PRODUCTS_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsUpdated = database.update(ProductEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
        if (rowsUpdated != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    /**
     * Checks if content values are valid. If used for updating then null values will not be checked.
     * @param contentValues
     * @param update Boolean used to signify if it is being used for an update. True for updating, false for insertion
     */
    private void checkContentValues(ContentValues contentValues, boolean update){
        String name = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (!update || contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)){
            if (name == null || name.matches("")) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }

        String description = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
        if (!update || contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_DESCRIPTION)){
            if (description == null || description.matches("")){
                throw new IllegalArgumentException("Product requires a description");
            }
        }

        Integer quantity = contentValues.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        if (!update || contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY)){
            if (quantity < 0 || quantity == null){
                throw new IllegalArgumentException("Product requires a valid quantity");
            }
        }

        Double price = contentValues.getAsDouble(ProductEntry.COLUMN_PRODUCT_PRICE);
        if (!update || contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)){
            if (price < 0.0 || price == null || BigDecimal.valueOf(price).scale() > 2){
                throw new IllegalArgumentException("Product requires a valid price");
            }
        }


        String image = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_IMAGE);
        if (!update || contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_IMAGE)){
            if (image.matches("")|| image == null){
                throw new IllegalArgumentException("Product must have image");
            }
        }

        String supplierEmail = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL);
        if (!update || contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL)){
            if (supplierEmail.matches("") || !Patterns.EMAIL_ADDRESS.matcher(supplierEmail).matches() || supplierEmail == null){
                throw new IllegalArgumentException("Product must have a valid supplier email");
            }
        }
    }
}
