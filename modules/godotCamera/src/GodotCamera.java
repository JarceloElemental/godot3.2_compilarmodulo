// Author: Thiago Bruno (thiago.bruno@birdy.studio)
// https://github.com/thiagobruno/godot3.2_compilarmodulo

package org.godotengine.godot;

import android.util.Log;
import java.io.*;
import java.util.Date;
import android.util.Base64;
import java.text.SimpleDateFormat;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.graphics.Bitmap; 
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import androidx.core.content.FileProvider;
import android.graphics.Matrix;

public class GodotCamera extends Godot.SingletonBase {
	private static final String TAG = "godot";
	private String aplicationId = "com.thiagobruno.godotcamera";
	private static Activity activity;
	private Integer cameraCallbackId = 0;
	private static File cameraPathFile;

	static final int REQUEST_IMAGE_CAPTURE = 1;
	private Bitmap mImageBitmap;
	private String currentPhotoPath;
	private int degreeRotation = 0;
	private int imageSize = 500;
	private String cleanImage;

	public void openCamera()
	{
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
		    // Create the File where the photo should go
		    File photoFile = null;
		    try {
			photoFile = createImageFile();
			
		    } catch (IOException ex) {
			// Error occurred while creating the File
			Log.d(TAG, "Ocorreu um erro ao criar o arquivo de foto. Verifique as permissões!");
		    }
		    // Continue only if the File was successfully created
		    if (photoFile != null) {
			Uri photoURI = FileProvider.getUriForFile(activity, aplicationId, photoFile);
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
			activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
		    }
		}

	}

	public Bitmap scaleImageKeepAspectRatio(Bitmap scaledGalleryBitmap){

		Matrix matrix = new Matrix();
		matrix.postRotate(degreeRotation);

		int imageWidth = scaledGalleryBitmap.getWidth();
		int imageHeight = scaledGalleryBitmap.getHeight();
		int newHeight = (imageHeight * imageSize)/imageWidth;
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(scaledGalleryBitmap, imageSize, newHeight, false);
		Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
		return rotatedBitmap;
	}

	private File createImageFile() throws IOException {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "PNG_" + timeStamp + "_";
		File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		File image = File.createTempFile(
		    imageFileName,  /* prefix */
		    ".png",         /* suffix */
		    storageDir      /* directory */
		);
		currentPhotoPath = image.getAbsolutePath();
		return image;
	}

	static public Godot.SingletonBase initialize (Activity p_activity) {
		return new GodotCamera(p_activity);
	}

	public void setCameraCallbackId(int _cameraCallbackId) {
		this.cameraCallbackId = _cameraCallbackId;
	}

	public void setImageSize(int _imageSize) {
		this.imageSize = _imageSize;
	}

	public void setImageRotated(int _rotatedDegree) {
		this.degreeRotation = _rotatedDegree;
	}

	public GodotCamera(Activity p_activity) {
		registerClass("GodotCamera", new String[]
		{
			"openCamera",
			"setImageSize",
			"setImageRotated",
			"setCameraCallbackId"
		});
		activity = p_activity;
	}

	public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
	{
	    ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
	    image.compress(compressFormat, quality, byteArrayOS);
	    return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
	}

	protected void onMainActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == activity.RESULT_OK) {
			Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
			imageBitmap = scaleImageKeepAspectRatio(imageBitmap);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			imageBitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
			byte[] imageBytes = baos.toByteArray();
			String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
			cleanImage = imageString.replace("data:image/png;base64,", "").replace("data:image/jpeg;base64,","");

			GodotLib.calldeferred(cameraCallbackId, "_on_GodotCamera_success", new Object[] { cleanImage });
		}
	}

}
