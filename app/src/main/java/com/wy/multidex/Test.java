package com.wy.multidex;

import android.content.Context;
import android.widget.Toast;

/**
 * User : wy
 * Date : 2016/12/13
 */
public class Test {

    public void testFix(Context mContext){
        int i = 10;
        int j = 2;
        Toast.makeText(mContext,"计算结果："+i/j,Toast.LENGTH_SHORT).show();
    }
}
