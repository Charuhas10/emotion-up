/*package com.example.imagepro;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceRec {
    private Interpreter interpreter1;
    private int inputSize1;              //Defined Input size
    private int height1 = 0;             //Defined height of the original frame
    private int width1 = 0;              //Defined width of the original frame
    private GpuDelegate gpuDelegate1=null;
    private CascadeClassifier cascadeClassifier1;

    FaceRec(AssetManager assetManager1, Context context1, String modelPath, int input_size) throws IOException{
        inputSize1 = input_size;
        Interpreter.Options options1 = new Interpreter.Options();
        gpuDelegate1 = new GpuDelegate();
        options1.setNumThreads(8);
        interpreter1 = new Interpreter(loadModel(assetManager1, modelPath),options1);

        try {
            InputStream inputStream1 = context1.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File casDir=context1.getDir("cascade1", Context.MODE_PRIVATE);
            File cascade = new File(casDir,"haarcascade_frontalface_alt");
            FileOutputStream outputStream1 = new FileOutputStream(cascade);
            byte[] buffer1=new byte[4096];
            int read1;
            while ((read1=inputStream1.read(buffer1)) != -1){
                    outputStream1.write(buffer1,0,read1);
            }
            inputStream1.close();
            outputStream1.close();

            cascadeClassifier1=new CascadeClassifier(cascade.getAbsolutePath());
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModel(AssetManager assetManager1, String modelPath) throws IOException{
        AssetFileDescriptor assetFileDescriptor1 =assetManager1.openFd(modelPath);
        FileInputStream inputStream1 = new FileInputStream(assetFileDescriptor1.getFileDescriptor());
        FileChannel channel = inputStream1.getChannel();
        long startOffset1 = assetFileDescriptor1.getStartOffset();
        long declaredLength1 = assetFileDescriptor1.getDeclaredLength();
        return channel.map(FileChannel.MapMode.READ_ONLY,startOffset1,declaredLength1);
    }
}*/
