# AndroidHookStartActivity

## AMSHook

两行代码实现不注册Activity启动，只需要注册一下！

```
//OtherActivity 是你的要启动的Activity。 HostActivity是壳容器
  AMSHookUtil.hookStartActivity(OtherActivity.class,HostActivity.class); 
```
```
 <application  >
           //配置壳Activity
        <activity android:name=".HostActivity" />
    </application>
```
以后就可以按照标准的Intent启动为那些未被注册的Activity。
```
                Intent intent = new Intent(MainActivity.this, OtherActivity.class);
                startActivity(intent);
```

原理详解：http://www.jianshu.com/p/2ad105f54d07


