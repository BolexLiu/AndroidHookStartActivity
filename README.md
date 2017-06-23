# AndroidHookStartActivity

## AMSHook

两行代码实现不注册Activity启动，只需要注册一下！


1.application标签里配置一个壳Activity 
```   
        <activity android:name=".HostActivity" />
```

2.注册一下你需要启动的Activity ,和壳容器。OtherActivity是你未注册的要启动的Activity,HostActivity是你注册过的壳容器。
```
  AMSHookUtil.hookStartActivity(OtherActivity.class,HostActivity.class); 
```

3.以后就可以按照标准的Intent启动为那些未被注册的Activity。
```
                Intent intent = new Intent(MainActivity.this, OtherActivity.class);
                startActivity(intent);
```

原理详解：http://www.jianshu.com/p/2ad105f54d07


