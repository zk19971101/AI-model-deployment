package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button open_album, open_camera;
    RadioButton choose_mobilenet, choose_resnet;
    TextView predict_result;
    ImageView input_image;

    Bitmap bitmap = null;

    String modelName = "mobilenet_v3.pt";
    Module model = null;

    ArrayList<String> label = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        open_album = (Button) findViewById(R.id.openAlbum);
        open_camera= (Button) findViewById(R.id.openCamera);

        choose_mobilenet = (RadioButton) findViewById(R.id.button_mobilenet);
        choose_resnet = (RadioButton) findViewById(R.id.button_resnet);

        predict_result = (TextView) findViewById(R.id.predict_result);

        input_image = (ImageView) findViewById(R.id.imageView);
        open_album.setOnClickListener(this);
        open_camera.setOnClickListener(this);
        choose_resnet.setOnClickListener(this);
        choose_mobilenet.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (choose_mobilenet.isChecked()){
            Toast.makeText(this, "mobilenet", Toast.LENGTH_SHORT).show();
            modelName = "mobilenet_v3.pt";
            model = null;


        } else if (choose_resnet.isChecked()) {
            Toast.makeText(this, "resnet", Toast.LENGTH_SHORT).show();
            modelName = "resnet18.pt";
            model = null;
        }

        switch (v.getId()){
            case R.id.openAlbum:
                Toast.makeText(this, "打开相册", Toast.LENGTH_SHORT).show();
                Intent chooseIntent = new Intent(Intent.ACTION_GET_CONTENT);
                chooseIntent.setType("image/*");
                chooseIntent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(chooseIntent, 001);

                break;

            case R.id.openCamera:
                Toast.makeText(this, "打开相机", Toast.LENGTH_SHORT).show();
                Intent openCam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(openCam, 002);

                break;
        }


    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 001){
            try {
                // 传入为uri，转化为bitmap
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            input_image.setImageBitmap(bitmap);


        } else if (requestCode == 002) {
            bitmap = (Bitmap) data.getExtras().get("data");
            input_image.setImageBitmap(bitmap);

        }

        long start = System.currentTimeMillis();
        try {
            model = LiteModuleLoader.load(assertFilePath(this, modelName));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        label = ReadListFromFile(getAssets(), "label_list.txt");
        long end = System.currentTimeMillis();
        long start1 = System.currentTimeMillis();
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                MemoryFormat.CHANNELS_LAST);

        final Tensor outputTensor = model.forward(IValue.from(inputTensor)).toTensor();
        long end1 = System.currentTimeMillis();
        final float[] scores = outputTensor.getDataAsFloatArray();

        float maxScore = -Float.MAX_VALUE;
        int maxScoreIndex = -1;
        int i=0;
        for (; i<scores.length; i++){
            if (scores[i] > maxScore){
                maxScore = scores[i];
                maxScoreIndex = i;
            }
        }

//        label.get(maxScoreIndex)
        String show_text = "类别：" + label.get(maxScoreIndex) +
                "\n概率：" + maxScore+
                "\n模型加载时间：" + (end - start) + "ms" +
                "\n推理时间：" + (end1 - start1) + "ms";

        predict_result.setText(show_text);



    }
    public static String assertFilePath(Context context, String assertName) throws IOException {
        File file = new File(context.getFilesDir(), assertName);
//        return file.getAbsolutePath();
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assertName)) {
            try (OutputStream os = new FileOutputStream(file)) { //创建一个向指定 File 对象表示的文件中写入数据的文件输出流
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) { //int read()从输入流中读取数据的下一个字节；int read(byte[] b)从输入流中读取一定数量的字节，并将其存储在缓冲区数组b中
                    // 从输入流中读取下一个byte的数据，返回的是一个0~255之间的int类型数。如果已经到了流的末尾没有byte数据那么返回-1。
                    os.write(buffer, 0, read); //将指定 byte 数组中从偏移量 off 开始的 len 个字节写入此输出流。
                }
                os.flush(); // 刷新此输出流并强制写出所有缓冲的输出字节。
            }
            return file.getAbsolutePath();
        }
    }

    public static ArrayList<String> ReadListFromFile(AssetManager assetManager, String filePath){
        ArrayList<String> list = new ArrayList<String>();
        BufferedReader reader = null;
        InputStream inputStream = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(assetManager.open(filePath)));
            String line;
            while ((line = reader.readLine())!=null){
                list.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }


}