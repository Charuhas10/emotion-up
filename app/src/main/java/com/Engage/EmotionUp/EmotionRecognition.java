package com.Engage.EmotionUp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class EmotionRecognition {
    private Interpreter interpreter;

    private int Input_Size;      //Defined Input size

    private int height = 0;     //Defined height of the original frame
    private int width = 0;      //Defined width of the original frame

    private GpuDelegate gpuDelegate=null;   //used to implement gpu in interpreter

    private CascadeClassifier cascadeClassifier;    //Defined cascade classifier for face detection

    private TextToSpeech textToSpeech;
    String emotion;

    EmotionRecognition(TextView text_change, Button speech_button, AssetManager assetManager, Context context, String path, int inputSize ) throws IOException {
        Input_Size = inputSize;
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();

        options.addDelegate(gpuDelegate);
        options.setNumThreads(8);

        interpreter = new Interpreter(loadModelFile(assetManager, path), options);      //Will load model weight to interpreter

        textToSpeech=new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        speech_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                text_change.setText(emotion);
                textToSpeech.speak(emotion,TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        Log.d("Emotion_Recognition","Model is loaded");

        try{
            InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("Cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt");
            FileOutputStream outputStream = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int readByte;

            while((readByte=inputStream.read(buffer)) != -1){
                outputStream.write(buffer, 0 , readByte);
            }

            inputStream.close();;
            outputStream.close();
            cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());

            Log.d("Emotion_Recognition", "Classifier is Loaded");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }


    public Mat imageRecognize(Mat mat_image){

        Core.flip(mat_image.t(), mat_image, 1); //rotating mat_image by +90 degrees

        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        height = grayscaleImage.height();
        width = grayscaleImage.width();

        int absFaceSize = (int)(height*0.1);

        MatOfRect faces = new MatOfRect();      //To store faces

        if (cascadeClassifier!=null){
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2,
                    2, new Size(absFaceSize, absFaceSize), new Size());
        }

        Rect[] arrayFace = faces.toArray();

        for(int i=0;i<arrayFace.length;i++)
        {
            Imgproc.rectangle(mat_image,arrayFace[i].tl(),arrayFace[i].br(),new Scalar(255,0,0,255),2);     //rectangle around the face
            Rect roi = new Rect((int)arrayFace[i].tl().x,(int)arrayFace[i].tl().y,
                    ((int)arrayFace[i].br().x)-(int)(arrayFace[i].tl().x),
                    ((int)arrayFace[i].br().y)-(int)(arrayFace[i].tl().y));

            Mat croppedRGBA = new Mat(mat_image,roi);
            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(croppedRGBA.cols(),croppedRGBA.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(croppedRGBA,bitmap);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, false);       //resizing the bitmap to (48,48)

            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaled);

            float[][] emotions = new float[1][1];
            interpreter.run(byteBuffer,emotions);

            Log.d("emotion_recognition","Output:" + Array.get(Array.get(emotions,0),0));

            float emotionVal = (float)Array.get(Array.get(emotions,0), 0);
            Log.d("emotion_recognition","Output:" + emotionVal);

            emotion = get_emotion_text(emotionVal);

            Imgproc.putText(mat_image,emotion + "(" + emotionVal + ")",
                    new Point((int)arrayFace[i].tl().x+10,(int)arrayFace[i].tl().y+20), 1, 1.5, new Scalar(255,255,255,150),2);
        }

        Core.flip(mat_image.t(), mat_image, 0); //rotating mat_image by -90 degrees after prediction
        return mat_image;
    }

    private String get_emotion_text(float emotionVal) {
        String val ="";

        if(emotionVal >= 0 & emotionVal<0.5){
            val = "Surprised!";
        }
        else if (emotionVal >= 0.5 & emotionVal<1.5){
            val = "Fear";
        }
        else if (emotionVal >= 1.5 & emotionVal<2.7){
            val = "Angry";
        }
        else if (emotionVal >= 2.7 & emotionVal<3.5){
            val = "Neutral";
        }
        else if (emotionVal >= 3.5 & emotionVal<4.7){
            val = "Sad";
        }
        else if (emotionVal >= 4.7 & emotionVal<5.5){
            val = "Disgusted";
        }
        else {
            val = "Happy!";
        }
        return val;
    }


    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaled) {
        ByteBuffer byteBuffer;
        int sizeImage = Input_Size; //48
        byteBuffer = ByteBuffer.allocateDirect(4*1*sizeImage*sizeImage*3);      //4 is multiplied for float input and 3 is for RGB
        byteBuffer.order(ByteOrder.nativeOrder());
        int [] intValues = new int[sizeImage*sizeImage];
        scaled.getPixels(intValues, 0, scaled.getWidth(),0, 0, scaled.getWidth(), scaled.getHeight());
        int pixel=0;
        for (int i=0;i<sizeImage;++i){
            for (int j=0;j<sizeImage;++j){
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f);
                byteBuffer.putFloat((((val>>8)&0xFF))/255.0f);
                byteBuffer.putFloat(((val & 0xFF))/255.0f);
            }
        }
        return byteBuffer;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String path) throws IOException{
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(path);        //will give description of the file
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }       // Used to load our trained model

}
