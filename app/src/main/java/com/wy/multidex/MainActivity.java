package com.wy.multidex;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //计算
        Button btn1 = (Button)findViewById(R.id.add);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });

        //更新
        Button btn2 = (Button)findViewById(R.id.upd);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upd2();
            }
        });
    }

    public void add(){
        Test t = new Test();
        t.testFix(this);
    }

    public void upd2() {
        //获取目录：/data/data/packageName/odex,此文件必须放在该应用目录下
        File fileDri = getDir(MyConstants.DEX_DIR, Context.MODE_PRIVATE);
        //往该目录下面存放以配置好的dex文件
        String name = "classes2.dex";
        String filePath = fileDri.getAbsolutePath()+File.separator+name;
        //filePath: /data/user/0/com.wy.multidex/app_odex/classes2.dex
        Log.i("你好",filePath);
        Log.i("你好","abc："+Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+name);
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
        }

        //把下载好的dex文件写入所指定的文件下面filePath
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            //   /storage/emulated/0/classes2.dex
            is = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+name);
            File f2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+name);
            if(f2.exists()){
                Log.i("你好","拿到了dex");
            }
            fos = new FileOutputStream(filePath);
            int len = 0;
            byte []buffer = new byte[1024];
            while((len = is.read(buffer)) != -1){
                fos.write(buffer,0,len);
            }
            File f = new File(filePath);
            if(f.exists()){
                Toast.makeText(this,"ok",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this,"no",Toast.LENGTH_LONG).show();
            }

            //热修复
            FixDexUtils.loadFixedDex(this);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                is.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }



}
