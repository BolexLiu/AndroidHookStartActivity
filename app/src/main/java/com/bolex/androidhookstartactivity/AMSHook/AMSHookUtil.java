package com.bolex.androidhookstartactivity.AMSHook;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.bolex.androidhookstartactivity.HostActivity;
import com.bolex.androidhookstartactivity.MainActivity;
import com.bolex.androidhookstartactivity.OtherActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Bolex on 2017/6/20.
 */

public class AMSHookUtil {


    /**
     *   * 这里我们通过反射获取到AMS的代理本地代理对象
     * Hook以后动态串改Intent为已注册的来躲避检测
     * @param proxyActivity      未被注册的Activity
     * @param originallyActivity 空壳Activity
     */
    public static void hookStartActivity(Class<?> proxyActivity, Class<?> originallyActivity) {
        try {
            Class<?> amnClazz = Class.forName("android.app.ActivityManagerNative");
            Field defaultField = amnClazz.getDeclaredField("gDefault");
            defaultField.setAccessible(true);
            Object gDefaultObj = defaultField.get(null); //所有静态对象的反射可以通过传null获取。如果是实列必须传实例
            Class<?> singletonClazz = Class.forName("android.util.Singleton");
            Field amsField = singletonClazz.getDeclaredField("mInstance");
            amsField.setAccessible(true);
            Object amsObj = amsField.get(gDefaultObj);
            amsObj = Proxy.newProxyInstance(MainActivity.class.getClass().getClassLoader(),
                    amsObj.getClass().getInterfaces(),
                    new HookInvocationHandler(amsObj, proxyActivity, originallyActivity));
            amsField.set(gDefaultObj, amsObj);
            hookLaunchActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hook handle 在这里我们需要替换我们未被注册的Activity
     *
     * @throws Exception
     */
    public static void hookLaunchActivity() throws Exception {
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        Field sCurrentActivityThreadField = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
        sCurrentActivityThreadField.setAccessible(true);
        Object sCurrentActivityThreadObj = sCurrentActivityThreadField.get(null);
        Field mHField = activityThreadClazz.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(sCurrentActivityThreadObj);
        Field callBackField = Handler.class.getDeclaredField("mCallback");
        callBackField.setAccessible(true);
        callBackField.set(mH, new ActivityThreadHandlerCallBack());
    }

    static class ActivityThreadHandlerCallBack implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            int LAUNCH_ACTIVITY = 0;
            try {
                Class<?> clazz = Class.forName("android.app.ActivityThread$H");
                Field field = clazz.getField("LAUNCH_ACTIVITY");
                LAUNCH_ACTIVITY = field.getInt(null);
            } catch (Exception e) {
            }
            if (msg.what == LAUNCH_ACTIVITY) {
                handleLaunchActivity(msg);
            }
            return false;
        }
    }

    public static void handleLaunchActivity(Message msg) {
        try {
            Object obj = msg.obj;
            Field intentField = obj.getClass().getDeclaredField("intent");
            intentField.setAccessible(true);
            Intent proxyIntent = (Intent) intentField.get(obj);
            //拿到之前真实要被启动的Intent 然后把Intent换掉
            Intent originallyIntent = proxyIntent.getParcelableExtra("originallyIntent");
            proxyIntent.setComponent(originallyIntent.getComponent());

            //todo:兼容AppCompatActivity
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field field = forName.getDeclaredField("sCurrentActivityThread");
            field.setAccessible(true);
            Object activityThread = field.get(null);
            Method getPackageManager = activityThread.getClass().getDeclaredMethod("getPackageManager");
            Object iPackageManager = getPackageManager.invoke(activityThread);
            PackageManagerHandler handler = new PackageManagerHandler(iPackageManager);
            Class<?> iPackageManagerIntercept = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{iPackageManagerIntercept}, handler);
            // 获取 sPackageManager 属性
            Field iPackageManagerField = activityThread.getClass().getDeclaredField("sPackageManager");
            iPackageManagerField.setAccessible(true);
            iPackageManagerField.set(activityThread, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static class PackageManagerHandler implements InvocationHandler {
        private Object mActivityManagerObject;

        public PackageManagerHandler(Object mActivityManagerObject) {
            this.mActivityManagerObject = mActivityManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("getActivityInfo")) {
                ComponentName componentName = new ComponentName(OtherActivity.class.getPackage().getName(), HostActivity.class.getName());
                args[0] = componentName;
            }
            return method.invoke(mActivityManagerObject, args);
        }
    }

}
