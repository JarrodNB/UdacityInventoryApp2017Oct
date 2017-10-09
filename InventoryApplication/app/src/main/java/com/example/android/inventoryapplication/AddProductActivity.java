package com.example.android.inventoryapplication;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.NumberFormatException;

import com.example.android.inventoryapplication.data.ProductContract.ProductEntry;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddProductActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    @BindView(R.id.select_image)
    TextView selectImage;
    @BindView(R.id.image_selected)
    ImageView selectedImage;
    @BindView(R.id.product_name)
    EditText productName;
    @BindView(R.id.product_description)
    EditText productDescription;
    @BindView(R.id.product_quantity)
    EditText productQuantity;
    @BindView(R.id.product_price)
    EditText productPrice;
    @BindView(R.id.product_supplier)
    EditText productSupplier;
    @BindView(R.id.submit_button)
    Button submitButton;
    @BindView(R.id.minus_button)
    Button minusButton;
    @BindView(R.id.plus_button)
    Button plusButton;
    @BindView(R.id.delete_button)
    Button deleteButton;
    @BindView(R.id.order_button)
    Button orderButton;

    private static final int PRODUCT_LOADER = 1;
    private String errorMessage;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri currentProductUri;
    // True if product is being edited
    private boolean editProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        ButterKnife.bind(this);
        // For anonymous inner classes
        final Context context = this;
        Intent intent = getIntent();
        // Gets Uri for current product if it exists
        currentProductUri = intent.getData();
        if (currentProductUri == null){
            editProduct = false;
            setTitle(R.string.add_title);
        } else {
            editProduct = true;
            setTitle(R.string.edit_title);
            // Gets cursor from database for current Product
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        }
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editProduct){
                    // if product is successfuly added then toast is shown and app goes back to main activity
                    if (submitProduct()){
                        Toast.makeText(context, context.getString(R.string.product_added), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(context, MainActivity.class);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(context, context.getString(R.string.error) + " " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // if product is successfuly updated then toast is shown and app goes back to main activity
                    if (updateProduct()){
                        Toast.makeText(context, context.getString(R.string.product_updated), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(context, MainActivity.class);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(context, context.getString(R.string.error) + " " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        // Prompts for user to select image from phone
        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantity = Integer.valueOf(String.valueOf(productQuantity.getText())) - 1;
                if (quantity < 0) return;
                productQuantity.setText(quantity + "");
            }
        });
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantity = Integer.valueOf(String.valueOf(productQuantity.getText())) + 1;
                productQuantity.setText(quantity + "");
            }
        });
        // If new product then delete and order more button are hidden
        if (!editProduct){
            productQuantity.setText(0 + "");
            deleteButton.setVisibility(View.INVISIBLE);
            orderButton.setVisibility(View.INVISIBLE);
        }
        // delete button launches alrt dialog to confirm deletion, then deletes record
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog dialog =  new AlertDialog.Builder(context)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.delete_alert)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                Toast.makeText(context, R.string.delete_message, Toast.LENGTH_SHORT).show();
                                getContentResolver().delete(currentProductUri, null,null);
                                Intent intent = new Intent(context, MainActivity.class);
                                startActivity(intent);
                            }})
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
        });
        // order more button starts intent to send message to supplier
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_EMAIL, String.valueOf(productSupplier.getText()));
                startActivity(intent);
            }
        });
    }

    // Tries to update Product. If failed then errorMessage is changed to reflect the error
    private boolean updateProduct() {
        try{
            getContentResolver().update(currentProductUri, getValues(), null, null);
            return true;
        } catch (IllegalArgumentException error) {
            errorMessage = error.getMessage();
            return false;
        }
    }

    // Gets the values of the edit texts and puts them into an instance of ContentValues
    private ContentValues getValues(){
            String name = String.valueOf(productName.getText());
            String description = String.valueOf(productDescription.getText());
            Double price = 0d;
            Integer quantity = 0;
            try {
                price = Double.valueOf(String.valueOf(productPrice.getText()));
                quantity = Integer.valueOf(String.valueOf(productQuantity.getText()));
            } catch(NumberFormatException numberError){
                errorMessage = getString(R.string.number_problem);
                return null;
            }
            String image;
            if (imageUri != null){
                image = String.valueOf(imageUri);
            } else{
                // Default image is used if none is selected
                image = "Default Image";
            }
            String supplierEmail = String.valueOf(productSupplier.getText());
            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, name);
            values.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, description);
            values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
            values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, image);
            values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, supplierEmail);
            return values;


    }

    private boolean submitProduct() {
        ContentValues values = getValues();
        if (values == null){
            return false;
        }
        try {
            getContentResolver().insert(ProductEntry.CONTENT_URI, values);
            return true;
        } catch (IllegalArgumentException error){
            errorMessage = error.getMessage();
            return false;
        }

    }

    private void selectImage(){
        Intent intent;
        // Uses different intent based off of android version to select image
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        // Stores the permissions for the image
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        }
    }

    // For the image select intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            imageUri = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            selectedImage.setImageBitmap(bitmap);
        }
        uriPermissions();
    }

    @TargetApi(19)
    private void uriPermissions(){
        getContentResolver().takePersistableUriPermission(imageUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION};
        return new CursorLoader(this,
                currentProductUri,
                projection,
                null,
                null,
                null);
    }

    // Sets edit texts to results from database for current product
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()){
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_NAME));
            String image = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_IMAGE));
            Double price = cursor.getDouble(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_PRICE));
            Integer quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_QUANTITY));
            String supplierEmail = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_DESCRIPTION));
            imageUri = Uri.parse(image);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                selectedImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                selectedImage.setImageResource(R.mipmap.ic_launcher);
            }

            productName.setText(name);
            productDescription.setText(description);
            productPrice.setText(String.valueOf(price));
            productQuantity.setText(String.valueOf(quantity));
            productSupplier.setText(supplierEmail);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
