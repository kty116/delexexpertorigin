package com.delex.delexexpert.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * 이미지가 돌아가는 현상 있을때 util
 */

public class EditImageUtil {

    private File mFolderPath = new File(Environment.getExternalStorageDirectory(), "담담");
    public static final String TAG = EditImageUtil.class.getSimpleName();
    private ExifInterface mExifInterface;

    /**
     * 사진 회전 메인 메소드
     */
    public Bitmap rotateImage(Context context, String imageUri) {

        Bitmap bitmap = null;
        int orientation = -1;

        Log.d(TAG, "rotateImage: " + imageUri);


        try {

            mExifInterface = new ExifInterface(imageUri);

            switch (getOrientation()) {
                case ExifInterface.ORIENTATION_NORMAL:
                    Log.d(TAG, "rotateImage: ORIENTATION_NORMAL");
                    orientation = -1; //디폴트값
                    break;

                case ExifInterface.ORIENTATION_ROTATE_90:
                    Log.d(TAG, "rotateImage: ORIENTATION_ROTATE_90");
                    orientation = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    Log.d(TAG, "rotateImage: ORIENTATION_ROTATE_180");
                    orientation = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    Log.d(TAG, "rotateImage: ORIENTATION_ROTATE_270");
                    orientation = 270;
                    break;
            }

            Log.d(TAG, "rotateImage: " + orientation);

            if (orientation != -1) {


                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse("file://" + imageUri));

                Bitmap rotationBitmap = rotateImage(bitmap, orientation);  //이미지 회전값 만큼 비트맵 객체 회전

                File file = new File(imageUri);

                saveImage(file, rotationBitmap);

                saveExif(file);

                return rotationBitmap;
            } else {
                //회전 하지않으면
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse("file://" + imageUri));

                return bitmap;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;

    }

    /**
     * 사진에 저장된 방향값 가져오기
     *
     * @return
     */
    public int getOrientation() {
        int orientationValue = mExifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
        return orientationValue;
    }

    /**
     * 사진에 방향값 저장하기
     *
     * @param finalExif
     */
    public void setOrientation(ExifInterface finalExif) {
        finalExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
    }

    /**
     * 이미지 회전값 만큼 비트맵 객체 회전
     *
     * @param src
     * @param degree
     * @return Bitmap
     */
    public Bitmap rotateImage(Bitmap src, float degree) {

        Matrix matrix = new Matrix();  // 회전 각도 셋팅
        matrix.postRotate(degree);  // 이미지와 Matrix 를 셋팅해서 Bitmap 객체 생성
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                src.getHeight(), matrix, true);
    }

    /**
     * 회전한 비트맵 이미지 저장
     *
     * @param file
     * @param bitmap
     */
    public void saveImage(File file, Bitmap bitmap) {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * exif 원래 값 유지하고 orientation 값 회전 후 넣기
     *
     * @param file
     * @return 새로 저장한 ExifInterface값 = finalExif
     */
    public ExifInterface saveExif(File file) {

        ExifInterface originalExif = mExifInterface;
        ExifInterface finalExif = null;

        try {
            finalExif = new ExifInterface(file.getAbsolutePath());
            copyExifWithoutLengthWidth(originalExif, finalExif);
            finalExif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return finalExif;
    }

    private void copyExifWithoutLengthWidth(ExifInterface originalExif, ExifInterface finalExif) {

        for (Field f : ExifInterface.class.getFields()) {
            String name = f.getName();
            if (!name.startsWith("TAG_")) {
                continue;
            }

            String key = null;
            try {
                key = (String) f.get(null);
            } catch (Exception e) {
                continue;
            }

            if (key == null) {
                continue;
            }

            if (key.equals(ExifInterface.TAG_IMAGE_LENGTH) || key.equals(ExifInterface.TAG_IMAGE_WIDTH)) {
                continue;
            }

            String value = originalExif.getAttribute(key);
            if (value == null) {
                continue;
            }
            finalExif.setAttribute(key, value);
        }
        setOrientation(finalExif);
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        //사진이 정사각형이 아니고

        if (originalHeight > maxDimension || originalWidth > maxDimension) {  //높이나 넓이 값이 maxDimension 값을 넘을때만 사이즈 리사이징

            if (originalHeight > originalWidth) {  //세로 이미지면
                resizedHeight = maxDimension;
                resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
                Log.d(TAG, "scaleBitmapDown: resizedWidth" + resizedHeight);
                Log.d(TAG, "scaleBitmapDown: resizedWidth" + resizedWidth);

            } else if (originalWidth > originalHeight) {  //가로 이미지
                resizedWidth = maxDimension;
                resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
                Log.d(TAG, "scaleBitmapDown: resizedHeight" + resizedWidth);
                Log.d(TAG, "scaleBitmapDown: resizedHeight" + resizedHeight);
            } else if (originalHeight == originalWidth) {
                resizedHeight = maxDimension;
                resizedWidth = maxDimension;
            }

            return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
        }

        return bitmap;
    }

    public String saveBitmapToJpeg(Context context, Bitmap bitmap) {

        File storage = context.getCacheDir(); // 이 부분이 임시파일 저장 경로

        String fileName = System.currentTimeMillis() + ".jpg";  // 파일이름은 마음대로!

        File tempFile = new File(storage, fileName);

        try {
            tempFile.createNewFile();  // 파일을 생성해주고

            FileOutputStream out = new FileOutputStream(tempFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);  // 넘거 받은 bitmap을 jpeg(손실압축)으로 저장해줌

            out.close(); // 마무리로 닫아줍니다.

//            tempFile.deleteOnExit();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "file://" + tempFile.getAbsolutePath();   // 임시파일 저장경로를 리턴해주면 끝!
    }

    public void fileDelete(Context context) {

//        for (int i = 0; i < thumbnailUris.length; i++) {

            File file = new File(String.valueOf(context.getCacheDir()));
            Log.d(TAG, "fileDelete: " + file.getAbsolutePath());

            if (file.exists()) {
                file.delete();
                Log.d(TAG, "fileDelete: 파일이 존재하므로 삭제됨");
            }
//        }
    }

}
