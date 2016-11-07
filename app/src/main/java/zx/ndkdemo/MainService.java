package zx.ndkdemo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import javax.mail.MessagingException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class MainService extends Service {
    private static final String TAG = "MainService";
    private static final int NOTIFICATION_ID = 1;
    PhoneStateListener listener;
    TelephonyManager tm;
    private MailCenter mailCenter;

    public MainService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // 不支持绑定
        return null;
    }

    BroadcastReceiver smsReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        // 管理依赖
        mailCenter = new MailCenter("smtp.qq.com", "zhengxiao1127@qq.com", "MTAzaHp4I3Fx");
        listener = new MainPhoneStateListener(this, mailCenter);
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // 启动未接来电监听
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

        // 启动短信监听
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                    SmsManager smsManager = SmsManager.getDefault();
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        SmsMessage[] messages = new SmsMessage[pdus.length];
                        for (int i = 0; i < pdus.length; i++) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }

                        // 处理 SmsMessage
                        // 聚合
                        HashSet<String> smsSenders = new HashSet<>();
                        StringBuilder smsBodyBuilder = new StringBuilder();
                        for (SmsMessage message : messages) {
                            String from = message.getOriginatingAddress();
                            String msgBody = message.getMessageBody();
                            smsSenders.add(from);
                            smsBodyBuilder.append(from);
                            smsBodyBuilder.append(":\n");
                            smsBodyBuilder.append(msgBody);
                            smsBodyBuilder.append("\n\n");
                        }

                        final String smsSendersString = TextUtils.join(",", smsSenders);
                        final String smsBody = smsBodyBuilder.toString();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mailCenter.sendMail(String.format("[sms] 短信 %s", smsSendersString),
                                            "" + smsBody,
                                            "zhengxiao1127@qq.com",
                                            "zhengxiao1127@foxmail.com");
                                    // ok

                                } catch (final MessagingException e) {
                                    e.printStackTrace();

                                }
                            }
                        }, "mailCenter").start();
                    }
                }
            }
        };
        final IntentFilter filter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        registerReceiver(smsReceiver, filter);

        // 并显示通知
        startForeground(NOTIFICATION_ID, makeNotification());
    }

    @Override
    public void onDestroy() {
        tm.listen(listener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(smsReceiver);

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("stop".equals(intent.getAction())) {
            stopSelf();
        }
        return START_STICKY;
    }

    private Notification makeNotification() {
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, MainService.class).setAction
                ("stop"), 0);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setSmallIcon(R.mipmap.ic_launcher);
        b.setContentTitle("正在监听此设备的来电与短信");
        b.setContentText("点击关闭");
        b.setContentIntent(pendingIntent);
        return b.build();
    }

    static class MainPhoneStateListener extends PhoneStateListener {
        Context context;
        MailCenter mailCenter;
        HashMap<String, CallSession> callSessions = new HashMap<>();

        public MainPhoneStateListener(Context context, MailCenter mailCenter) {
            this.context = context;
            this.mailCenter = mailCenter;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            CallSession session = callSessions.get(incomingNumber);
            if (session == null) {
                session = new CallSession(incomingNumber);
                callSessions.put(incomingNumber, session);
            }

            int oldState = session.state;
            if (oldState == state) {
                // nothing happened
                return;
            } else {
                // update current state
                session.state = state;
            }

            if (state == TelephonyManager.CALL_STATE_RINGING) {
                // record ringing
                session.ringStartTime = System.currentTimeMillis();

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                session.ringEndTime = System.currentTimeMillis();
                // 处理未接来电通知
                if (!session.isAnswered) {
                    sendMail(session);
                }
                // 删除
                callSessions.remove(session.incomingNumber);
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                session.isAnswered = true;
            }
        }

        void sendMail(final CallSession session) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mailCenter.sendMail(String.format("[call] 未接电话 %s", session.incomingNumber),
                                String.format("时间: %s 响铃: %d ms", new SimpleDateFormat().format(new Date(session
                                        .ringStartTime)), (session.ringEndTime - session.ringStartTime)),
                                "zhengxiao1127@qq.com",
                                "zhengxiao1127@foxmail.com");
                        // ok

                    } catch (final MessagingException e) {
                        e.printStackTrace();

                    }
                }
            }, "mailCenter").start();
        }

    }

    static class CallSession {
        final String incomingNumber;

        public CallSession(String incomingNumber) {
            this.incomingNumber = incomingNumber;
        }

        int state = TelephonyManager.CALL_STATE_IDLE;
        long ringStartTime;
        long ringEndTime;
        boolean isAnswered = false;
    }

}
