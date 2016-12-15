# MultiDex</br>
热修复效果图，首先第一次点击点击计算是，程序出异常，我再代码中是给的10/0，我修复好了代码之后，打包dex，在不从新的安装应用程序时修复了此代码。</br>
![](https://github.com/MakeWang/MultiDex/blob/master/images/acss.gif)

# 原理</br>
MultiDex热修复是从Android如何加载classes.dex文件来处理的，主要的核心就是分包，打包多个.dex文件，然后通过类加载机制讲修复好的包给合并。</br>

# 主要事项</br>

1、分包，在gradle中配置
```java
dependencies {
    compile 'com.android.support:multidex:1.0.1'
}

buildTypes {
        release {
            multiDexKeepFile file('dex.keep')
            def myFile = file('dex.keep')
            println("isFileExists:"+myFile.exists())
            println "dex keep"
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }


defaultConfig {
        multiDexEnabled true
    }
```
2、在Application中配置 
```java
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        MultiDex.install(base);
        FixDexUtils.loadFixedDex(base);
        super.attachBaseContext(base);
    }
}
```
3、在打包应用程序时Instant Run -->   Enable Instant Run to hot swap code/resoure changes on.....钩给去掉，不然就修复不了。</br>


# 核心关键类</br>
# FixDexUtils</br>
```java
public class FixDexUtils {

    private static final String TAG = "multidex";

    private static HashSet<File> loadDex = new HashSet<File>();

    static {
        loadDex.clear();
    }

    public static void loadFixedDex(Context mContext){
        if(mContext == null){
            return;
        }

        //遍历所有修复的dex
        File fileDir = mContext.getDir(MyConstants.DEX_DIR,Context.MODE_PRIVATE);
        File []listFiles = fileDir.listFiles();
        Log.i(TAG,"listFiles  "+listFiles.length);
        for (File f : listFiles){
            if(f.getName().startsWith("classes") && f.getName().endsWith(".dex")){
                loadDex.add(f);//存入集合
                Log.i(TAG,"存入集合");
            }
        }
        //dex合并
        doDexInject(mContext,fileDir,loadDex);
    }


    private static void doDexInject(final Context appContext,File fileDir,HashSet<File> loadDex){
        String optimizeDir = fileDir.getAbsolutePath()+File.separator+"opt_dex";
        Log.i(TAG,"optimizeDir   "+optimizeDir);
        try {
            File fopt = new File(optimizeDir);
            if(!fopt.exists()){
                fopt.mkdirs();
            }

            //1.加载应用程序的dex
            PathClassLoader pathLoad = (PathClassLoader) appContext.getClassLoader();

            for (File f : loadDex){
                Log.i(TAG,"循环");
                //2.加载指定的修复的dex文件。
                DexClassLoader classLoader = new DexClassLoader(f.getAbsolutePath(),fopt.getAbsolutePath(),null,pathLoad);
                //3.合并
                Object dexObj = getPathList(classLoader);
                Object pathObj = getPathList(pathLoad);
                Object mDexElementsList = getDexElements(dexObj);
                Object pathDexElementsList = getDexElements(pathObj);

                //合并完成
                Object dexElements = combineArray(mDexElementsList,pathDexElementsList);
                //重写给PathList里面的lement[] dexElements;赋值
                Object pathList = getPathList(pathLoad);
                setField(pathList,pathList.getClass(),"dexElements",dexElements);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Object getPathList(Object baseDexClassLoader) throws Exception {
        return getField(baseDexClassLoader,Class.forName("dalvik.system.BaseDexClassLoader"),"pathList");
    }

    private static Object getDexElements(Object obj) throws Exception {
        return getField(obj,obj.getClass(),"dexElements");
    }

    private static Object getField(Object obj,Class<?> cl,String filed) throws Exception {
        Field localField = cl.getDeclaredField(filed);
        localField.setAccessible(true);
        return localField.get(obj);
    }

    private static void setField(Object obj,Class<?> cl, String field, Object value) throws Exception {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        localField.set(obj,value);
    }

    /**
     * 两个数组合并
     * @param arrayLhs
     * @param arrayRhs
     * @return
     */
    private static Object combineArray(Object arrayLhs, Object arrayRhs) {
        Class<?> localClass = arrayLhs.getClass().getComponentType();
        int i = Array.getLength(arrayLhs);
        int j = i + Array.getLength(arrayRhs);
        Object result = Array.newInstance(localClass, j);
        for (int k = 0; k < j; ++k) {
            if (k < i) {
                Array.set(result, k, Array.get(arrayLhs, k));
            } else {
                Array.set(result, k, Array.get(arrayRhs, k - i));
            }
        }
        return result;
    }
}

```
# MainActivity</br>
```java
public class MainActivity extends Activity {

    private static final String TAG = "multidex";

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
        Log.i(TAG,filePath);
        Log.i(TAG,"abc："+Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+name);
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
                Log.i(TAG,"拿到了dex");
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

```

# .dex打包</br>
1、在MultiDexDemo\app\build\intermediates\classes\debug路劲下面找到修改的.classs文件。</br>
2、将AndroidSDK这路劲配置环境变量AndroidSDK\android-sdk_r24.4.1-windows\android-sdk-windows\build-tools\23.0.1中。</br>
3、在1这个目录中打开cmd命令窗口。</br>
   命令：dx --dex --output=C:\Users\tgkj\Desktop\test\classes2.dex C:\Users\tgkj\Desktop\test</br>
   C:\Users\tgkj\Desktop\test\classes2.dex  打包的生成的文件路劲及文件。</br>
   C:\Users\tgkj\Desktop\test    打包的路劲，包名一定要全。</br>
