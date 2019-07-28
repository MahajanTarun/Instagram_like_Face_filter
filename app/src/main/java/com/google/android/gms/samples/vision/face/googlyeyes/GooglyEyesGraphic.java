/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.googlyeyes;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.samples.vision.face.googlyeyes.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

/**
 * Graphics class for rendering Googly Eyes on a graphic overlay given the current eye positions.
 */
class GooglyEyesGraphic extends GraphicOverlay.Graphic {
    private static final float EYE_RADIUS_PROPORTION = 0.45f;
    private static final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;
    public static boolean front_mode = true;
    public static int filter = 1;
    public static Bitmap myBitmap;

    private Paint mEyeWhitesPaint;
    private Paint mEyeIrisPaint;
    private Paint mEyeOutlinePaint;
    private Paint mEyeLidPaint;

    // Keep independent physics state for each eye.
    private EyePhysics mLeftPhysics = new EyePhysics();
    private EyePhysics mRightPhysics = new EyePhysics();

    private volatile PointF mLeftPosition;
    private volatile boolean mLeftOpen;

    private volatile Face maskFace;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;

    private volatile PointF mRightPosition;
    private volatile boolean mRightOpen;

    float faceRadius;

    Context mContext;
    private Face face;

//    Bitmap eyePatchBitmap;

    //==============================================================================================
    // Methods
    //==============================================================================================

    GooglyEyesGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);

        mContext = context;

        mEyeWhitesPaint = new Paint();
        mEyeWhitesPaint.setColor(Color.WHITE);
        mEyeWhitesPaint.setStyle(Paint.Style.FILL);

        mEyeLidPaint = new Paint();
        mEyeLidPaint.setColor(Color.YELLOW);
        mEyeLidPaint.setStyle(Paint.Style.FILL);

        mEyeIrisPaint = new Paint();
        mEyeIrisPaint.setColor(Color.BLACK);
        mEyeIrisPaint.setStyle(Paint.Style.FILL);

        mEyeOutlinePaint = new Paint();
        mEyeOutlinePaint.setColor(Color.GREEN);
        mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
        mEyeOutlinePaint.setStrokeWidth(5);
    }

    /**
     * Updates the eye positions and state from the detection of the most recent frame.  Invalidates
     * the relevant portions of the overlay to trigger a redraw.
     */
    void updateEyes(PointF leftPosition, boolean leftOpen,
                    PointF rightPosition, boolean rightOpen) {
        mLeftPosition = leftPosition;
        mLeftOpen = leftOpen;

        mRightPosition = rightPosition;
        mRightOpen = rightOpen;

        postInvalidate();
    }

    public void updateMask(Face face) {
        this.face = face;
        Log.d("Eular_angles: Y:", face.getEulerY() + " & Z:" + face.getEulerZ());
    }

    /**
     * Draws the current eye state to the supplied canvas.  This will draw the eyes at the last
     * reported position from the tracker, and the iris positions according to the physics
     * simulations for each iris given motion and other forces.
     */
    @Override
    public void draw(Canvas canvas) {
        PointF detectLeftPosition = mLeftPosition;
        PointF detectRightPosition = mRightPosition;

//        Log.v(">>>>>>>", eyePatchBitmap.getWidth()+"");

        if ((detectLeftPosition == null) || (detectRightPosition == null)) {
            return;
        }


        PointF leftPosition =
                new PointF(translateX(detectLeftPosition.x), translateY(detectLeftPosition.y));
        PointF rightPosition =
                new PointF(translateX(detectRightPosition.x), translateY(detectRightPosition.y));

        PointF center =
                new PointF(translateX((detectRightPosition.x + detectLeftPosition.x) / 2), translateY((detectRightPosition.y + detectLeftPosition.y) / 2));

        // Use the inter-eye distance to set the size of the eyes.
        float distance = (float) Math.sqrt(
                Math.pow(rightPosition.x - leftPosition.x, 2) +
                        Math.pow(rightPosition.y - leftPosition.y, 2));

        float faceRadius = 0.9f * distance;
        float eyeRadius = 1.9f * distance;
//        float eyeRadius = EYE_RADIUS_PROPORTION * distance;
        float irisRadius = IRIS_RADIUS_PROPORTION * distance;

        // Advance the current left iris position, and draw left eye.
        PointF leftIrisPosition =
                mLeftPhysics.nextIrisPosition(leftPosition, eyeRadius, irisRadius);
//        drawEye(canvas, leftPosition, eyeRadius, leftIrisPosition, irisRadius, mLeftOpen);

        // Advance the current right iris position, and draw right eye.
        PointF rightIrisPosition = mRightPhysics.nextIrisPosition(rightPosition, eyeRadius, irisRadius);

        switch (filter) {
            case 1:
                drawEye(canvas, rightPosition, eyeRadius, rightIrisPosition, irisRadius, mRightOpen);
                break;
            case 2:
                drawCrown(canvas, center, eyeRadius, rightIrisPosition, irisRadius, mRightOpen);
                break;
            case 3:
                draw9Head(canvas, center, eyeRadius, rightIrisPosition, irisRadius, mRightOpen);
                break;
            case 4:
                drawHead(canvas, center, eyeRadius, rightIrisPosition, irisRadius, mRightOpen);
                break;

        }
//        drawEye(canvas, rightPosition, eyeRadius, rightIrisPosition, irisRadius, mRightOpen);
//        canvas.drawCircle(rightPosition.x, rightPosition.y, eyeRadius, eyePatchBitmap);

//        float x = translateX(this.maskFace.getPosition().x + this.maskFace.getWidth() / 2);
//        float y = translateY(this.maskFace.getPosition().y + this.maskFace.getHeight() / 2);
//        canvas.drawBitmap(eyePatchBitmap, x - ID_X_OFFSET*2, y, null);
    }

    /**
     * Draws the eye, either closed or open with the iris in the current position.
     */
    private void drawEye(Canvas canvas, PointF eyePosition, float faceRadius,
                         PointF irisPosition, float irisRadius, Boolean isOpen) {
        Bitmap resized;
        if (front_mode) {
            resized = BitmapFactory.decodeResource(mContext.getResources(), R.raw.eye_patch_front);
        } else {
            resized = BitmapFactory.decodeResource(mContext.getResources(), R.raw.eye_patch_back);
        }
        Bitmap eyePatchBitmap = Bitmap.createScaledBitmap(resized, (int) faceRadius, (int) faceRadius, true);
        float height = eyePatchBitmap.getHeight();
        float width = eyePatchBitmap.getWidth();

//        if (isOpen) {
//            canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeWhitesPaint);
//            canvas.drawCircle(irisPosition.x, irisPosition.y, irisRadius, mEyeIrisPaint);
//        } else {
//            canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeLidPaint);
//            float y = eyePosition.y;
//            float start = eyePosition.x - eyeRadius;
//            float end = eyePosition.x + eyeRadius;
//            canvas.drawLine(start, y, end, y, mEyeOutlinePaint);
//        }
//        canvas.drawCircle(eyePosition.x - width/2, eyePosition.y - height/2, 1, mEyeOutlinePaint);
        canvas.drawBitmap(eyePatchBitmap, eyePosition.x - width / 2, eyePosition.y - height / 2, null);
        resized.recycle();
    }

    private void drawCrown(Canvas canvas, PointF eyePosition, float faceRadius,
                           PointF irisPosition, float irisRadius, Boolean isOpen) {
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        float xOffset = scaleX(face.getWidth() / 3.2f);
        float yOffset = scaleY(face.getHeight() / 3.2f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        Paint tempPaint = new Paint();
        tempPaint.setColor(Color.RED);
        tempPaint.setStyle(Paint.Style.STROKE);
        tempPaint.setStrokeWidth(5.0f);

        Bitmap resized = BitmapFactory.decodeResource(mContext.getResources(), R.raw.r2);
        Bitmap image = Bitmap.createScaledBitmap(resized, (int) faceRadius * 4, (int) faceRadius * 2, true);

        float height = image.getHeight();
        float width = image.getWidth();

        Matrix mat1 = new Matrix();
        mat1.postRotate(this.face.getEulerZ(), image.getWidth() / 2, image.getHeight() / 2);
        Bitmap temp1 = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), mat1, true);


        canvas.drawBitmap(temp1, eyePosition.x - width / 2, eyePosition.y - height / 2, null);
        resized.recycle();
    }

    private void drawHead(Canvas canvas, PointF eyePosition, float faceRadius,
                          PointF irisPosition, float irisRadius, Boolean isOpen) {

        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        float xOffset = scaleX(face.getWidth() / 3.2f);
        float yOffset = scaleY(face.getHeight() / 3.2f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        Paint tempPaint = new Paint();
        tempPaint.setColor(Color.RED);
        tempPaint.setStyle(Paint.Style.STROKE);
        tempPaint.setStrokeWidth(5.0f);

        for (Landmark landmark : face.getLandmarks()) {
            int cx = (int) (landmark.getPosition().x * 2.0f);
            int cy = (int) (landmark.getPosition().y * 2.0f);
            canvas.drawCircle(cx, cy, 10, tempPaint);
        }

//        canvas.drawRect(left, top, right, bottom, tempPaint);

        try {
            if (this.myBitmap != null && face.getPosition().x > 0 && face.getPosition().y > 0) {
                Log.d("*****>>>>>", this.myBitmap.getWidth() + " & " + this.myBitmap.getHeight());

                Bitmap temp;
                if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // Crop the face part from the complete image, for Landscape mode.
                    Matrix lsMode = new Matrix();
                    if (front_mode){
                        lsMode.postRotate(90, this.myBitmap.getWidth()/2, this.myBitmap.getHeight()/2);
                    }else {
                        lsMode.postRotate(-90, this.myBitmap.getWidth()/2, this.myBitmap.getHeight()/2);
                    }
                    Bitmap lsImage = Bitmap.createBitmap(this.myBitmap, 0, 0, this.myBitmap.getWidth(), this.myBitmap.getHeight(), lsMode, true);
                    temp = Bitmap.createBitmap(lsImage, (int) (face.getPosition().x + face.getWidth() / 8), (int) face.getPosition().y, (int) (face.getWidth() - face.getWidth() / 5), (int) face.getHeight());
                } else {
                    // Crop the face part from the complete image. for potraite mode.
                    temp = Bitmap.createBitmap(this.myBitmap, (int) (face.getPosition().x + face.getWidth() / 8), (int) face.getPosition().y, (int) (face.getWidth() - face.getWidth() / 5), (int) face.getHeight());
                }

                // Resize the face-image to render(Canvas.Draw()) the bitmap of the desired size.
                Bitmap newImg = Bitmap.createScaledBitmap(temp, (int) faceRadius * 3 / 4, (int) faceRadius * 3 / 4, true);
                Bitmap eyePatchBitmap;
                if (front_mode){
                    Matrix mat1 = new Matrix();
                    mat1.setScale(-1, 1);
                    eyePatchBitmap = Bitmap.createBitmap(newImg, 0, 0, newImg.getWidth(), newImg.getHeight(), mat1, true);
                }else {
                    eyePatchBitmap = newImg;
                }

                canvas.drawBitmap(eyePatchBitmap, x + xOffset, top + (yOffset / 2), null);
                canvas.drawBitmap(eyePatchBitmap, left - eyePatchBitmap.getWidth(), top + (yOffset / 2), null);

                canvas.drawBitmap(eyePatchBitmap, x + xOffset + eyePatchBitmap.getWidth(), top + (yOffset / 2), null);
                canvas.drawBitmap(eyePatchBitmap, left - 2 * eyePatchBitmap.getWidth(), top + (yOffset / 2), null);

                canvas.drawBitmap(eyePatchBitmap, x + xOffset + 2 * eyePatchBitmap.getWidth(), top + (yOffset / 2), null);
                canvas.drawBitmap(eyePatchBitmap, left - 3 * eyePatchBitmap.getWidth(), top + (yOffset / 2), null);
//                }
            }
        } catch (Exception e) {
            Log.e("Exception:", String.valueOf(e));
        }
    }

    private void draw9Head(Canvas canvas, PointF eyePosition, float faceRadius,
                           PointF irisPosition, float irisRadius, Boolean isOpen) {
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        float xOffset = scaleX(face.getWidth() / 3.2f);
        float yOffset = scaleY(face.getHeight() / 3.2f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        Paint tempPaint = new Paint();
        tempPaint.setColor(Color.RED);
        tempPaint.setStyle(Paint.Style.STROKE);
        tempPaint.setStrokeWidth(5.0f);

        Bitmap resized = BitmapFactory.decodeResource(mContext.getResources(), R.raw.nine_ravan);
        Bitmap image = Bitmap.createScaledBitmap(resized, (int) faceRadius * 4, (int) faceRadius * 2, true);

        float height = image.getHeight();
        float width = image.getWidth();

        Matrix mat1 = new Matrix();
        mat1.postRotate(this.face.getEulerZ(), eyePosition.x - width / 2, eyePosition.y - height / 2);
        Bitmap temp1 = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), mat1, true);


        canvas.drawBitmap(temp1, eyePosition.x - width / 2, eyePosition.y - height / 2, null);
        resized.recycle();
    }

}
