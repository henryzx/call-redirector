package zx.ndkdemo;

import android.app.Application;
import android.content.Intent;

/**
 * Created by zhengxiao on 11/2/16.
 */

public class AppDelegate extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 自动启动服务
        startService(new Intent(this, MainService.class));
    }
}
