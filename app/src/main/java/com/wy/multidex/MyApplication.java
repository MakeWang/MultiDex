package com.wy.multidex;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * User : wy
 * Date : 2016/12/13
 */
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        MultiDex.install(base);
        FixDexUtils.loadFixedDex(base);
        super.attachBaseContext(base);
    }
}
