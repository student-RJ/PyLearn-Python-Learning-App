package com.example.python1;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

public class CertificatePDFGenerator {

    public static Uri generateCertificate(Context context, String userName, String topic, String date, int score) {
        Log.d("CertificatePDF", "Generating PDF for: " + topic);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(context, "Requires Android 10 or above for PDF generation via MediaStore", Toast.LENGTH_LONG).show();
            return null;
        }

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size in points

        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Load the certificate template from drawable
        Bitmap background = BitmapFactory.decodeResource(context.getResources(), R.drawable.certi);
        Bitmap scaledBackground = Bitmap.createScaledBitmap(background, 595, 842, true);
        canvas.drawBitmap(scaledBackground, 0, 0, null);

        // Set up paint for text
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // CENTER SECTION - User details
        float centerX = 595 / 2f;
        float currentY = 350; // Starting Y position for center section

        // User name (centered)
        paint.setTextSize(36f);
        canvas.drawText(userName, centerX - (paint.measureText(userName)/2), currentY, paint);
        currentY += 50;

        // Topic
        paint.setTextSize(24f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Topic: " + topic, centerX - (paint.measureText("Topic: " + topic)/2), currentY, paint);
        currentY += 40;

        // Score
        String scoreText = "Score: " + score + "%";
        canvas.drawText(scoreText, centerX - (paint.measureText(scoreText)/2), currentY, paint);

        // TOP RIGHT - Date only (near CEO name)
        paint.setTextSize(16f);
        canvas.drawText("Date: " + date, 410, 720, paint); // Adjust these coordinates as needed

        document.finishPage(page);

        // Save PDF (same as before)
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();

        String fileName = "Certificate_" + userName.replace(" ", "_") + "_" + topic.replace(" ", "_") + ".pdf";


        Uri existingFileUri = findExistingFileUri(resolver, fileName);
        if (existingFileUri != null) {
            resolver.delete(existingFileUri, null, null);
            Log.d("CertificatePDF", "Deleted existing certificate: " + fileName);
        }

        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PyLearn_Certificates");

        Uri uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
        if (uri == null) {
            Toast.makeText(context, "Failed to create file for PDF.", Toast.LENGTH_LONG).show();
            document.close();
            return null;
        }

        try (OutputStream out = resolver.openOutputStream(uri)) {
            document.writeTo(out);
            document.close();
            Toast.makeText(context, "Certificate saved in Downloads/PyLearn_Certificates", Toast.LENGTH_SHORT).show();
            return uri;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            document.close();
            return null;
        }
    }

    private static Uri findExistingFileUri(ContentResolver resolver, String fileName) {
        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + " = ?";
        String[] selectionArgs = new String[]{fileName};

        try (android.database.Cursor cursor = resolver.query(
                collection,
                new String[]{MediaStore.MediaColumns._ID},
                selection,
                selectionArgs,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                return Uri.withAppendedPath(collection, String.valueOf(id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}