package com.eastsoft.android.esbic.ativity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.eastsoft.android.esbic.R;
import com.eastsoft.android.esbic.jni.DeviceInfo;
import com.eastsoft.android.esbic.jni.DeviceTypeEnum;
import com.eastsoft.android.esbic.jni.IpAddressInfo;
import com.eastsoft.android.esbic.jni.MessageInfoEnum;
import com.eastsoft.android.esbic.service.BroadcastTypeEnum;
import com.eastsoft.android.esbic.service.IModelService;
import com.eastsoft.android.esbic.service.ModelServiceImpl;
import com.eastsoft.android.esbic.table.AlarmInfo;
import com.eastsoft.android.esbic.table.MessageInfo;
import com.eastsoft.android.esbic.table.ParaInfo;
import com.eastsoft.android.esbic.util.BoardCastFilterInfo;
import com.eastsoft.android.esbic.util.JsonUtil;
import com.eastsoft.android.esbic.util.LogUtil;
import com.eastsoft.android.esbic.util.TimeUtil;
import com.eastsoft.android.esbic.util.WifiScan;
import com.eastsoft.android.esbic.weather.WeatherEnum;
import com.eastsoft.android.esbic.weather.WeatherInfo;
import com.eastsoft.android.esbic.weather.WeatherUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sofa on 2016/1/22.
 */
public class MainActivity extends BaseActivity implements View.OnClickListener
{
    private Button message,callRecord,alarmRecord,voice,screenBrightness,wifi,
            leaveHome,callManagement,monitor,callOtherUser,setting,callElevator;
    private TextView weather,weather_tmp, week,yearMonthDay;
    private ImageView weatherIcon,hourFront,hourAfter,timeIcon,minuteFront,minuterAfter;
    private LinearLayout weatherLinearLayout;
    private Dialog progressDialog;
    private String cityName;
    private Intent intent;
    private Date now;
    private TextClock clock;
    private volatile long clickCount;
    private String time;
    private WeatherInfo weatherInfo;
    private SimpleDateFormat simpleDateFormat;
    private BoardCastFilterInfo boardCastFilterInfo;
    private IModelService modelService;
    private MyReceiver myReceiver;
    private MyReceiver2 myReceiver2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if (msg.what==1)
                {
                    if (weatherInfo != null)
                    {
                        weather.setText(weatherInfo.description);
                        weather_tmp.setText(weatherInfo.lowTemperate +"～"+ weatherInfo.hightTemperate + "℃");
                        weatherIcon.setBackgroundResource(WeatherEnum.find(weatherInfo.description).icon);
                    }
                }else if(msg.what == 2)
                {
                    simpleDateFormat=new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
                    now =new Date();
                    yearMonthDay.setText(simpleDateFormat.format(now));
                    week.setText(new SimpleDateFormat("E", Locale.CHINA).format(now));
                }
            }
        };
        initData();
   }

    private void  initData()
    {
        message=(Button)this.findViewById(R.id.message);
        callRecord=(Button)this.findViewById(R.id.call_record);
        alarmRecord=(Button)this.findViewById(R.id.alarm_record);
        voice=(Button)this.findViewById(R.id.volume);
        screenBrightness=(Button)this.findViewById(R.id.brightness);
        wifi=(Button)this.findViewById(R.id.wifi);
        leaveHome=(Button)this.findViewById(R.id.leave_home);
        callManagement=(Button)this.findViewById(R.id.call_center_management);
        monitor=(Button)this.findViewById(R.id.monitor_main);
        callOtherUser=(Button)this.findViewById(R.id.call_other_user);
        setting=(Button)this.findViewById(R.id.set);
        callElevator=(Button)this.findViewById(R.id.call_elevator);
        yearMonthDay=(TextView) this.findViewById(R.id.year_mouth_day);
        week=(TextView)this.findViewById(R.id.week);

        weather=(TextView)this.findViewById(R.id.weather);
        weather_tmp=(TextView)this.findViewById(R.id.weather_tmp);
        weatherIcon=(ImageView)this.findViewById(R.id.weather_icon);
        weatherLinearLayout = (LinearLayout)this.findViewById(R.id.weatherLinearLayout);
        weatherLinearLayout.setOnClickListener(this);

        clock = (TextClock)this.findViewById(R.id.clock);
        clock.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                clickCount++;
                if (clickCount > 1)
                {
                    if (clickCount == 4)
                    {
                        LogUtil.print(LogUtil.LogPriorityEnum.CORE_LOG_PRI_ERROR, "finish application, bye bye!");
                        android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
                        System.exit(0);
                    }
                    return;
                }
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try {
                            Thread.sleep(2000);
                            clickCount = 0;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        message.setOnClickListener(this);
        callRecord.setOnClickListener(this);
        alarmRecord.setOnClickListener(this);
        wifi.setOnClickListener(this);
        voice.setOnClickListener(this);
        screenBrightness.setOnClickListener(this);
        setting.setOnClickListener(this);


        callManagement.setOnClickListener(this);
        monitor.setOnClickListener(this);
        callOtherUser.setOnClickListener(this);
        leaveHome.setOnClickListener(this);
        callElevator.setOnClickListener(this);

        intent=getIntents();
        simpleDateFormat=new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        now =new Date();
        yearMonthDay.setText(simpleDateFormat.format(now));
        week.setText(new SimpleDateFormat("E", Locale.CHINA).format(now));
        boardCastFilterInfo=new BoardCastFilterInfo();

        ((MyApplication)getApplication()).setModelService(new ModelServiceImpl(getApplicationContext()));

        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.eastsoft.android.esbic.model");
        registerReceiver(myReceiver, intentFilter);

        myReceiver2 = new MyReceiver2();
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.eastsoft.android.esbic.app");
        registerReceiver(myReceiver2, intentFilter2);

        modelService = ((MyApplication)getApplication()).getModelService();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                DeviceInfo deviceInfo = modelService.getDeviceInfo();
                if(deviceInfo != null)
                {
                    modelService.init_intercom_core(deviceInfo);
                    IpAddressInfo ipAddressInfo = modelService.getIpAddressInfo();
                    if(ipAddressInfo != null && ipAddressInfo.getImpAdress().compareTo("") != 0)
                    {
                        modelService.init_imp_task(ipAddressInfo.getImpAdress());
                    }
                }

                new Thread(new QueryWeather()).start();
                new Thread(new UpdateDate()).start();
                new WifiScan(getApplicationContext()).openWifi();
            }
        }, 100);

//        MessageInfo info = new MessageInfo(MessageInfoEnum.MESSAGE.getType(), 0, "这是消息内容 " + TimeUtil.getDateTimeofNow2());
//        info.save();
//        AlarmInfo alarmInfo = new AlarmInfo(1);
//        alarmInfo.save();
//        alarmInfo = new AlarmInfo(4);
//        alarmInfo.save();

        //注册自定义动态广播信息
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(listenScreenClose, filter);
    }

    //使用BroadcastReceiver创建广播监听，监听屏幕的关闭。
    protected BroadcastReceiver listenScreenClose=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent i=new Intent();
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setClass(context,StandByActivity.class);
            context.startActivity(i);
        }
    };

    @Override
    public void onClick(View view) {
        playMusic();
        if (view.getId()==weatherLinearLayout.getId()){
            intent.setClass(MainActivity.this,WeatherSettingActivity.class);
            startActivity(intent);
        }
        else if (view.getId()==message.getId()){
            intent.setClass(MainActivity.this,MessageContentActivity.class);
            startActivity(intent);
        }
        else if (view.getId()==callRecord.getId()){
            intent.setClass(MainActivity.this,CallRecordActivity.class);
            startActivity(intent);
        }
        else if(view.getId()==alarmRecord.getId()){
            intent.setClass(MainActivity.this,AlarmRecordActivity.class);
            startActivity(intent);
        }
        else if(view.getId()==wifi.getId()){
            intent.setClass(MainActivity.this,WifiSettingActivity.class);
            startActivity(intent);
        }
        else if(view.getId()==voice.getId()){
            intent.setClass(MainActivity.this,VolumeActivity.class);
            startActivity(intent);
        }
        else if(view.getId()==screenBrightness.getId()){
            intent.setClass(MainActivity.this,ScreenLightActivity.class);
            startActivity(intent);
        }
        else if (view.getId()==setting.getId()){
            intent.setClass(MainActivity.this,SettingActivity.class);
            startActivity(intent);
        }
        else if (view.getId()==monitor.getId()){
            intent.setClass(MainActivity.this,MonitorActivity.class);
            startActivity(intent);
        }
        else if (view.getId()==callOtherUser.getId()){
            intent.setClass(MainActivity.this,CallMain.class);
            startActivity(intent);
        }
        else if (view.getId()==callManagement.getId()){
            IpAddressInfo ipAddressInfo = modelService.getIpAddressInfo();
            if(ipAddressInfo == null || ipAddressInfo.getCenterAddress() == null || ipAddressInfo.getCenterAddress().compareTo("") == 0)
            {
                showLongToast("请先设置中心管理机的地址！");
                return;
            }
            intent.setClass(MainActivity.this,CallManagementCenterActivity.class);
            startActivity(intent);
        }
        else if (view.getId()==leaveHome.getId()){
            intent.setClass(MainActivity.this,StandByActivity.class);
            startActivity(intent);
        }
        else if (view.getId()==callElevator.getId()){
            showShortToast("绿色出行，请走楼梯!");
        }
    }

  //查询天气
    private boolean getWeatherInformation()
    {
        String city = "青岛";
        ParaInfo paraInfo = modelService.getParaInfoByName("city");
        if(paraInfo!= null && paraInfo.getValue().compareTo("")!=0)
        {
            city = paraInfo.getValue();
        }
        weatherInfo = WeatherUtil.getWeather(city);
        if(weatherInfo == null)
        {
            handler.postDelayed(new QueryWeather(), 10000);
            return false;
        }
        return true;
    }

   class QueryWeather implements Runnable
   {
       @Override
       public void run()
       {
           Message message=new Message();
           boolean result = getWeatherInformation();
           if(!result)
           {
               return;
           }
           Bundle bundle=new Bundle();
           bundle.putBoolean("1",false);
           message.setData(bundle);
           message.what=1;
           handler.sendMessageDelayed(message, 200);
       }
   }

    class UpdateDate implements Runnable
    {
        @Override
        public void run()
        {
            Message message=new Message();
            message.what=2;
            handler.sendMessageDelayed(message, 200);
            if(TimeUtil.isSystemTimeCorrect())
            {
                return;
            }
            handler.postDelayed(new UpdateDate(), 3000);
        }
    }

    public class MyReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent2)
        {
            Bundle bundle = intent2.getExtras();
            int cmd = bundle.getInt("cmd");
            BroadcastTypeEnum e = BroadcastTypeEnum.find(cmd);
            String value = bundle.getString("value");
            switch (e)
            {
                case CALL_REQUEST :
                    DeviceInfo deviceInfo = JsonUtil.fromJson(value, DeviceInfo.class);
                    if(deviceInfo == null)
                    {
                        return;
                    }
                    if(deviceInfo.getDevice_type() == DeviceTypeEnum.DT_UNIT_DOOR_MACHINE.getType())
                    {
                        intent.setClass(MainActivity.this, OnCallActivity.class);
                    }else if(deviceInfo.getDevice_type() == DeviceTypeEnum.DT_ROOM_MACHINE.getType())
                    {
                        intent.setClass(MainActivity.this, RoomCallActivity.class);
                    }else
                    {
                        LogUtil.print("Device type is error ! " + deviceInfo.toString());
                        return;
                    }
                    intent.putExtra("value", value);
                    startActivity(intent);
                    break;
                case PLAY_VIDEO :
                    showLongToast("视频url" + value);
                    break;
                case HANG_UP :
                    showLongToast("对方已挂断！");
                    break;
                case CALL_CONFIRM :
                    showLongToast("您呼叫的设备已经找到！");
                    break;
                case OPEN_LOCK_CONFIRM :
                    showLongToast("门已开！");
                    break;
                case DEVICE_BUSY :
                    showLongToast("您呼叫的设备正在忙，请稍后再拨！");
                    break;
                case CALL_ANSWER_CONFIRM :
                    showLongToast("您呼叫的设备已接听！");
                    break;
                case RECEIVE_MESSAGE :
                    showLongToast("收到新的消息：" + value);
                    break;
                case RECEIVE_AD :
                    showLongToast("收到新的广告" + value);
                    break;
                case RECEIVE_ALARM :
                    showLongToast("收到新的警报" + value);
                    break;
                default:
                    break;
            }
        }
    }

    public class MyReceiver2 extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent2)
        {
            Bundle bundle = intent2.getExtras();
            int cmd = bundle.getInt("cmd");
            switch (cmd)
            {
                case 1 :
                    new Thread(new QueryWeather()).start();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(myReceiver);
        unregisterReceiver(myReceiver2);
        unregisterReceiver(listenScreenClose);
        LogUtil.print(this.getClass().getSimpleName() + " onDestroy !");
    }
}
