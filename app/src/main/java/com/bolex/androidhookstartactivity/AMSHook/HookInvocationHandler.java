package com.bolex.androidhookstartactivity.AMSHook;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class HookInvocationHandler implements InvocationHandler {

    private Object mAmsObj;
    private String mPackageName;
    private String cls;

    public HookInvocationHandler(Object amsObj, String packageName, String cls) {
        this.mAmsObj = amsObj;
        this.mPackageName = packageName;
        this.cls = cls;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // TODO: 2017/6/20 对 startActivity进行Hook
        if (method.getName().equals("startActivity")) {
            int index = 0;
            // TODO: 2017/6/20 找到我们启动时的intent
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }


            // TODO: 2017/6/20 取出在真实的Intent
            Intent originallyIntent = (Intent) args[index];
            Log.i("AMSHookUtil","HookInvocationHandler:" + originallyIntent.getComponent().getClassName());
            // TODO: 2017/6/20  自己伪造一个配置文件已注册过的Activity Intent
            Intent proxyIntent = new Intent();
            // TODO: 2017/6/20   因为我们调用的Activity没有注册，所以这里我们先偷偷换成已注册。使用一个假的Intent
            ComponentName componentName = new ComponentName(mPackageName, cls);
            proxyIntent.setComponent(componentName);
            // TODO: 2017/6/20 在这里把未注册的Intent先存起来 一会儿我们需要在Handle里取出来用
            proxyIntent.putExtra("originallyIntent", originallyIntent);
            args[index] = proxyIntent;
        }
        return method.invoke(mAmsObj, args);
    }
}