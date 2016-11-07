package zx.ndkdemo;

import javax.mail.MessagingException;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    MailCenter mailCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mailCenter = new MailCenter("smtp.qq.com", "zhengxiao1127@qq.com", "MTAzaHp4I3Fx");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "开启服务");
        menu.add(1, 1, 1, "发送邮件 (test)");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                // 开启服务
                startService(new Intent(this, MainService.class));
                return true;
            case 1:
                // 发送邮件 (test)
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mailCenter.sendMail("hello中文", "这是测试邮件哦", "zhengxiao1127@qq.com",
                                    "zhengxiao1127@foxmail.com");
                            // ok
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (final MessagingException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                        }
                    }
                }, "mailCenter").start();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
