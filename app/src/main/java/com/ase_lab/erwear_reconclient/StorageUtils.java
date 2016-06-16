package com.ase_lab.erwear_reconclient;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by chris on 28/05/15.
 */
public class StorageUtils {

    public static final String DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();

    public static final String JPEG = ".jpg";
    public static final String MP4 = ".mp4";
    public static final String MP4_MIME = "video/mp4";

    public static SimpleDateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static ContentValues getVideoData(CamcorderProfile camProfile, long dateTaken) {
        String title = "VID_"+dateFormat.format(dateTaken);
        String filename = title + MP4;
        String path = DIRECTORY + '/' + filename;
        ContentValues videoValues = new ContentValues(9);
        videoValues.put(MediaStore.Video.Media.TITLE, title);
        videoValues.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        videoValues.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        videoValues.put(MediaStore.MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        videoValues.put(MediaStore.Video.Media.MIME_TYPE, MP4_MIME);
        videoValues.put(MediaStore.Video.Media.DATA, path);
        videoValues.put(MediaStore.Video.Media.RESOLUTION,
                Integer.toString(camProfile.videoFrameWidth) + "x" + Integer.toString(camProfile.videoFrameHeight));

        return videoValues;
    }

    public static ContentValues getPhotoData(long dateTaken) {
        String title = "IMG_"+dateFormat.format(dateTaken);
        String filename = title + JPEG;
        String path = DIRECTORY + '/' + filename;
        ContentValues photoValues = new ContentValues(5);
        photoValues.put(MediaStore.Images.ImageColumns.TITLE, title);
        photoValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, filename);
        photoValues.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
        photoValues.put(MediaStore.Images.ImageColumns.DATA, path);
        return photoValues;
    }

    public static String getVideoPath(ContentValues values) {
        return values.getAsString(MediaStore.Video.Media.DATA);
    }

    public static String getPhotoPath(ContentValues values) {
        return values.getAsString(MediaStore.Images.ImageColumns.DATA);
    }

    public static Uri insertVideo(Context context, String tmpPath, ContentValues metadata, long duration) {
        metadata.put(MediaStore.Video.Media.SIZE, new File(tmpPath).length());
        metadata.put(MediaStore.Video.Media.DURATION, duration);

        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        try {
            uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, metadata);

            // Rename the video file to the final name. This avoids other
            // apps reading incomplete data.  We need to do it after we are
            // certain that the previous insert to MediaProvider is completed.
            String finalName = metadata.getAsString(
                    MediaStore.Video.Media.DATA);
            if(!new File(tmpPath).renameTo(new File(finalName))) {
                Log.e("SaveVideo", "failed to rename tmp file");
            } else {
                Log.v("SaveVideo", "Saved video to "+finalName);
                //TODO: Handle upload
                Map<String, String> params = new HashMap<String, String>(2);
                //params.put("foo", hash);
                //params.put("bar", caption);

                //String result = multipartRequest(URL_UPLOAD_VIDEO, params, finalName, "video", "video/mp4");
            }

            resolver.update(uri, metadata, null, null);
        } catch (Exception e) {
            // We failed to insert into the database. This can happen if
            // the SD card is unmounted.
            Log.e("SaveVideo", "failed to add video to media store", e);
            uri = null;
        } finally {
            Log.v("SaveVideo", "Current video URI: " + uri);
        }
        return uri;
    }

    public static Uri insertJpeg(Context context, byte[] data, long dateTaken) {

        ContentValues metaData = getPhotoData(dateTaken);
        String path = getPhotoPath(metaData);

        FileOutputStream out = null;
        Uri uri = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, metaData);
            Log.v("SavePhoto", "Saved image to "+path);
        } catch (Exception e) {
            Log.e("SavePhoto", "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e("SavePhoto", "Failed to close file after write", e);
            }
        }
        return uri;
    }

    private static String UploadTag = "ERWEAR_Upload";
    public static String multipartRequest(String urlTo, Map<String, String> parmas, String filepath, String filefield, String fileMimeType) {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";

        String result = "";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        String[] q = filepath.split("/");
        int idx = q.length - 1;

        try {
            File file = new File(filepath);
            FileInputStream fileInputStream = new FileInputStream(file);

            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            // Upload POST Data
            Iterator<String> keys = parmas.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = parmas.get(key);

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(value);
                outputStream.writeBytes(lineEnd);
            }

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


            if (200 != connection.getResponseCode()) {
                throw new Exception("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            inputStream = connection.getInputStream();

            result = convertStreamToString(inputStream);

            fileInputStream.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return result;
        } catch (Exception e) {
            Log.e(UploadTag ,e.toString());
            //throw new CustomException(e);
        }

    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
