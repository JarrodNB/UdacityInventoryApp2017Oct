package com.example.android.inventoryapplication;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;

import com.example.android.inventoryapplication.data.ProductContract.ProductEntry;

import java.io.IOException;

/**
 * Created by jnbcb on 10/5/2017.
 */

public class ProductCursorAdapter extends CursorAdapter {

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        ViewHolder holder = new ViewHolder();
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
        holder.name = (TextView) view.findViewById(R.id.product_list_name);
        holder.image = (ImageView) view.findViewById(R.id.product_list_image);
        holder.price = (TextView) view.findViewById(R.id.product_list_price);
        holder.quantity = (TextView) view.findViewById(R.id.list_quantity);
        holder.saleButton = (Button) view.findViewById(R.id.sale_button);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_NAME));
        String image = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_IMAGE));
        Double price = cursor.getDouble(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_PRICE));
        final Integer quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_QUANTITY));
        ViewHolder holder = (ViewHolder) view.getTag();
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        String currencyPrice = formatter.format(price);
        holder.name.setText(name);
        holder.price.setText(currencyPrice);
        holder.quantity.setText(String.valueOf(quantity));
        Uri itemImageUri = Uri.parse(image);
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), itemImageUri);
            holder.image.setImageBitmap(bitmap);
        } catch (IOException e) {
            Toast.makeText(context, "missing images, default image used", Toast.LENGTH_SHORT);
        }
        final int id = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry._ID));
        holder.saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(quantity == 0) {
                    // display a Toast
                    Toast.makeText(context, "no negatives", Toast.LENGTH_SHORT).show();
                } else {
                    // prepare inputs required to the method that updates the db:

                    // create a Uri for the List item that was clicked
                    Uri productUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

                    // Create a new map of values, where column names are the keys
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity - 1);

                    // call the method to update the db records
                    int result = context.getContentResolver().update(productUri, contentValues, null, null);
                }
            }
        });
    }

    private static class ViewHolder{
        ImageView image;
        TextView name;
        TextView price;
        TextView quantity;
        Button saleButton;
    }
}
