package com.wy.multidex;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * User : wy
 * Date : 2016/12/13
 */
public class FixDexUtils {


    private static HashSet<File> loadDex = new HashSet<File>();

    static {
        Log.i("你好","loadDex 清除");
        loadDex.clear();
    }

    public static void loadFixedDex(Context mContext){
        if(mContext == null){
            return;
        }

        //遍历所有修复的dex
        File fileDir = mContext.getDir(MyConstants.DEX_DIR,Context.MODE_PRIVATE);
        File []listFiles = fileDir.listFiles();
        Log.i("你好","listFiles  "+listFiles.length);
        for (File f : listFiles){
            if(f.getName().startsWith("classes") && f.getName().endsWith(".dex")){
                loadDex.add(f);//存入集合
                Log.i("你好","存入集合");
            }
        }
        //dex合并
        doDexInject(mContext,fileDir,loadDex);
    }


    private static void doDexInject(final Context appContext,File fileDir,HashSet<File> loadDex){
        String optimizeDir = fileDir.getAbsolutePath()+File.separator+"opt_dex";
        Log.i("你好","optimizeDir   "+optimizeDir);
        try {
            File fopt = new File(optimizeDir);
            if(!fopt.exists()){
                fopt.mkdirs();
            }

            //1.加载应用程序的dex
            PathClassLoader pathLoad = (PathClassLoader) appContext.getClassLoader();

            for (File f : loadDex){
                Log.i("你好","循环");
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
