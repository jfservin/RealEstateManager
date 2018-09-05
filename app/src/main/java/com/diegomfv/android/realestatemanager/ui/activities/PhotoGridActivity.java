package com.diegomfv.android.realestatemanager.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.diegomfv.android.realestatemanager.R;
import com.diegomfv.android.realestatemanager.adapters.RVAdapterMediaGrid;
import com.diegomfv.android.realestatemanager.constants.Constants;
import com.diegomfv.android.realestatemanager.data.entities.ImageRealEstate;
import com.diegomfv.android.realestatemanager.ui.base.BaseActivity;
import com.diegomfv.android.realestatemanager.ui.dialogfragments.InsertDescriptionDialogFragment;
import com.diegomfv.android.realestatemanager.util.FirebasePushIdGenerator;
import com.diegomfv.android.realestatemanager.util.ItemClickSupport;
import com.diegomfv.android.realestatemanager.util.ToastHelper;
import com.diegomfv.android.realestatemanager.util.Utils;

import java.io.FileNotFoundException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by Diego Fajardo on 02/09/2018.
 */

// TODO: 02/09/2018 Add Fragment add description!
// TODO: 02/09/2018 Take care, the user may leave the app and then come back and the
    //cache might be cleared!
public class PhotoGridActivity extends BaseActivity implements InsertDescriptionDialogFragment.InsertDescriptionDialogListener {

    private static final String TAG = PhotoGridActivity.class.getSimpleName();

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @BindView(R.id.recyclerView_media_id)
    RecyclerView recyclerView;

    private RVAdapterMediaGrid adapter;

    @BindView(R.id.button_add_photo_id)
    Button buttonAddPhoto;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private ActionBar actionBar;

    private Unbinder unbinder;

    private ImageRealEstate imageRealEstateCache;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called!");

        ////////////////////////////////////////////////////////////////////////////////////////////
        setContentView(R.layout.activity_photo_grid);
        unbinder = ButterKnife.bind(this);

        this.configureActionBar();

        this.configureRecyclerView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called!");
        unbinder.unbind();

    }

    @OnClick(R.id.button_add_photo_id)
    public void buttonClicked(View view) {
        Log.d(TAG, "buttonClicked: " + ((Button) view).getText().toString() + " clicked!");

        switch (view.getId()) {

            case R.id.button_add_photo_id: {
                launchGallery();
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called!");

        if (data == null) {
            ToastHelper.toastShort(PhotoGridActivity.this, getResources().getString(R.string.no_image_was_picked));

        } else {
            if (requestCode == Constants.REQUEST_CODE_GALLERY) {

                try {
                    final Uri imageUri = data.getData();
                    if (imageUri != null) {

                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                        String key = FirebasePushIdGenerator.generate();

                        getListOfImagesRealEstateCache().add(new ImageRealEstate(key, ""));
                        getListOfBitmapKeys().add(key);
                        getRepository().addBitmapToBitmapCache(key, Utils.getResizedBitmap(selectedImage, 840));

                        // TODO: 02/09/2018 Resize the bitmap according to ImageView size!

                        Log.i(TAG, "onActivityResult: " + getRepository().getCurrentSizeOfBitmapCache());
                        Log.i(TAG, "onActivityResult: " + getBitmapCache().size());

                        updateAdapterData();

                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    ToastHelper.toastShort(PhotoGridActivity.this, getResources().getString(R.string.there_was_an_error));
                }

            } else {
                ToastHelper.toastShort(PhotoGridActivity.this, getResources().getString(R.string.no_image_was_picked));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: called!");

        switch (item.getItemId()) {

            case android.R.id.home: {
                checkActivityLaunched();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(ImageRealEstate imageRealEstate) {
        Log.d(TAG, "onDatePickerDialogPositiveClick: called!");
        ToastHelper.toastLong(this, imageRealEstate.getDescription());

    }

    @Override
    public void onDialogNegativeClick() {
        Log.d(TAG, "onDatePickerDialogNegativeClick: called!");

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void configureActionBar() {
        Log.d(TAG, "configureActionBar: called!");

        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeActionContentDescription(getResources().getString(R.string.go_back));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void checkActivityLaunched () {
        Log.d(TAG, "checkActivityLaunched: called!");

        if (getIntent() != null && getIntent().getExtras() != null) {

            String fromIntent = getIntent().getStringExtra(Constants.INTENT_FROM_ACTIVITY);

            if (fromIntent.equals(Constants.INTENT_FROM_CREATE)) {
                Intent intent = new Intent(this, CreateNewListingActivity.class);
                intent.putExtra(Constants.INTENT_FROM_PHOTO_GRID_ACTIVITY, Constants.STRING_FROM_PHOTO_GRID_ACTIVITY);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, EditListingActivity.class);
                startActivity(intent);
            }
        }
    }

    private void configureRecyclerView() {
        Log.d(TAG, "configureRecyclerView: called!");

        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new GridLayoutManager(
                this, 2));
        this.adapter = new RVAdapterMediaGrid(
                this,
                getListOfBitmapKeys(),
                getRepository().getBitmapCache(),
                getImagesDir(),
                getGlide());
        this.recyclerView.setAdapter(this.adapter);

        this.configureOnClickRecyclerView();

    }

    private void configureOnClickRecyclerView() {
        Log.d(TAG, "configureOnClickRecyclerView: called!");

        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        Log.d(TAG, "onItemClicked: item(" + position + ") clicked!");

                        String key = getListOfBitmapKeys().get(position);

                        for (int i = 0; i < getListOfImagesRealEstateCache().size(); i++) {

                            if (getListOfImagesRealEstateCache().get(i).getId().equals(key)) {
                                launchAddDescriptionDialog(getListOfImagesRealEstateCache().get(i));
                                break;
                            }
                        }
                    }
                });
    }

    private void updateAdapterData () {
        Log.d(TAG, "updateAdapterData: called!");
        adapter.setDataKeys(getListOfBitmapKeys());
        adapter.setDataBitmapCache(getBitmapCache());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void launchGallery() {
        Log.d(TAG, "launchGallery: called!");

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, Constants.REQUEST_CODE_GALLERY);

    }
    private void addImageToListOfImagesInCache() {
        Log.d(TAG, "addImageToListOfImagesInCache: called!");
        getListOfImagesRealEstateCache().add(imageRealEstateCache);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void launchAddDescriptionDialog (ImageRealEstate imageRealEstate) {
        Log.d(TAG, "launchAddDescriptionDialog: called!");

        InsertDescriptionDialogFragment.newInstance(imageRealEstate)
                .show(getSupportFragmentManager(), "InsertDescriptionDialogFragment");

    }
}