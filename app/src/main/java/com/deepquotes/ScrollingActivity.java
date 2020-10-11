package com.deepquotes;

import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import com.dingmouren.colorpicker.ColorPickerDialog;
import com.dingmouren.colorpicker.OnColorPickerListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

public class ScrollingActivity extends AppCompatActivity {

    private SharedPreferences appConfigSP;
    private SharedPreferences.Editor appConfigSPEditor;
    private SharedPreferences historyQuotesSP;
    private SharedPreferences.Editor historyQuotesSPEditor;

    private DrawerLayout mDrawerLayout;
    private TextView headlineTextView;

    private ColorPickerDialog mColorPickerDialog;

    private RemoteViews remoteViews;
    private AppWidgetManager appWidgetManager;
    private ComponentName componentName;

    private Handler handler;
    private myBroadcast broadcast;
    private IntentFilter intentFilter;

    private ClipboardManager clipboardManager;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static String TAG = "ScrollingActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        appConfigSP = getSharedPreferences("appConfig",MODE_PRIVATE);
        appConfigSPEditor = appConfigSP.edit();


        intentFilter = new IntentFilter();
        intentFilter.addAction("com.deepquotes.broadcast.updateTextView");
        broadcast = new myBroadcast();
        registerReceiver(broadcast,intentFilter);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        final Switch isEnableHitokoto = findViewById(R.id.is_enable_hitokoto);
        final TextView hitokotoType = findViewById(R.id.hitokoto_type);
        headlineTextView = findViewById(R.id.headline_text_view);
        TextView updateNowTextView = findViewById(R.id.update_now);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);



//        TextView widgetTextView = findViewById(R.id.quotes_textview);

        remoteViews = new RemoteViews(getApplicationContext().getPackageName(),R.layout.quotes_layout);
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        componentName = new ComponentName(getApplicationContext(), QuotesWidgetProvider.class);


        dayOrNightMode();



        updateNowTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("刷新","u click updata");

                Intent intent = new Intent(ScrollingActivity.this,TimerService.class);
                startService(intent);
            }
        });





        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String deepQuote = headlineTextView.getText().toString();
                // 创建普通字符型ClipData
                ClipData mClipData = ClipData.newPlainText("deepQuote", deepQuote+"——来自「相顾无言」");
                // 将ClipData内容放到系统剪贴板里。
                clipboardManager.setPrimaryClip(mClipData);
                Toast.makeText(ScrollingActivity.this, "复制成功!", Toast.LENGTH_SHORT).show();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });




        if (appConfigSP.getBoolean("isEnableHitokoto",false)){
//            hitokotoType.setClickable(false);
//            hitokotoType.setTextColor(Color.GRAY);
            isEnableHitokoto.setChecked(true);
        }else {
//            hitokotoType.setClickable(true);
//            hitokotoType.setTextColor(Color.BLACK);
            isEnableHitokoto.setChecked(false);
            hitokotoType.setClickable(false);
            hitokotoType.setTextColor(Color.GRAY);
        }

        isEnableHitokoto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                if (isCheck){
                    if (isEnableHitokoto.isChecked()){
                        hitokotoType.setClickable(true);
                        hitokotoType.setTextColor(Color.BLACK);
                        appConfigSPEditor.putBoolean("isEnableHitokoto",true);
                        appConfigSPEditor.apply();
                    }
                }else {
                    hitokotoType.setClickable(false);
                    hitokotoType.setTextColor(Color.GRAY);
                    appConfigSPEditor.putBoolean("isEnableHitokoto",false);
                    appConfigSPEditor.apply();
                }
            }
        });


        mColorPickerDialog = new ColorPickerDialog(this,
                appConfigSP.getInt("fontColor", Color.WHITE),
                false,
                new OnColorPickerListener() {
                    @Override
                    public void onColorCancel(ColorPickerDialog dialog) {

                    }

                    @Override
                    public void onColorChange(ColorPickerDialog dialog, int color) {

                    }

                    @Override
                    public void onColorConfirm(ColorPickerDialog dialog, int color) {
                        Log.w("color",String.valueOf(color));
                        headlineTextView.setTextColor(color);

                        appConfigSPEditor.putInt("fontColor",color);
                        appConfigSPEditor.apply();

                        remoteViews.setTextColor(R.id.quotes_textview,color);
                        appWidgetManager.updateAppWidget(componentName,remoteViews);
                    }
                }
        );

    }


    @Override
    protected void onStart() {
        super.onStart();

        historyQuotesSP = getSharedPreferences("historyQuotes",MODE_PRIVATE);
        historyQuotesSPEditor = historyQuotesSP.edit();
        int defaultNum = historyQuotesSP.getInt("currentQuote",0);
        Log.d(TAG, "onCreate: "+defaultNum);

        headlineTextView.setTextColor(appConfigSP.getInt("fontColor",Color.WHITE));
        headlineTextView.setTextSize(appConfigSP.getInt("字体大小:",20));
        headlineTextView.setText(historyQuotesSP.getString(String.valueOf(defaultNum-1),"欲买桂花同载酒，终不似，少年游"));


        updateHistoryQuotes();

    }


    private void updateHistoryQuotes() {
        List<String> myList = new ArrayList<>(100);
        int maxNum;
        String maxNumQuote = historyQuotesSP.getString("100", "null");
        if (maxNumQuote.equals("null"))
            maxNum = historyQuotesSP.getInt("currentQuote", 0);
        else maxNum = 100;
        for (int inWchichQuote = 0; inWchichQuote < maxNum; inWchichQuote++)
            myList.add(historyQuotesSP.getString(String.valueOf(inWchichQuote), "null"));

        final RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        QuotesAdapter adapter = new QuotesAdapter(myList);
        recyclerView.setAdapter(adapter);
    }


    public void showHistory(View view){
        mDrawerLayout.openDrawer(Gravity.RIGHT);
    }

    public void selectFontColor(View view){
       mColorPickerDialog.show();
       Window window = mColorPickerDialog.getDialog().getWindow();
       window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_color)));

    }


    public void selectFontSize(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layoutView = LayoutInflater.from(this).inflate(R.layout.seekbar_select_layout,null);
        builder.setView(layoutView);
        layoutView.setBackgroundColor(getResources().getColor(R.color.background_color));
        final SeekBar refreshTimeSeekBar = layoutView.findViewById(R.id.seekbar_select_layout_seekbar);
        refreshTimeSeekBar.setMax(29);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            refreshTimeSeekBar.setMin(10);
        }
        refreshTimeSeekBar.setProgress(appConfigSP.getInt("字体大小:",10));
        final TextView textView = layoutView.findViewById(R.id.seekbar_select_layout_textview);
        textView.setText("字体大小:" + appConfigSP.getInt("字体大小:",10));


        refreshTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textView.setText("字体大小:" + (i+1));
                Log.d("字体大小", ": "+ (i+1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                appConfigSPEditor.putInt("字体大小:",refreshTimeSeekBar.getProgress()+1);
                appConfigSPEditor.apply();

                headlineTextView.setTextSize(refreshTimeSeekBar.getProgress());
                remoteViews.setTextViewTextSize(R.id.quotes_textview,COMPLEX_UNIT_SP,refreshTimeSeekBar.getProgress()+1);
                appWidgetManager.updateAppWidget(componentName,remoteViews);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Window window = alertDialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_color)));
    }

    public void selectRefreshTime(View view){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layoutView = LayoutInflater.from(this).inflate(R.layout.seekbar_select_layout,null);
        builder.setView(layoutView);

        final SeekBar refreshTimeSeekBar = layoutView.findViewById(R.id.seekbar_select_layout_seekbar);
        //一天1440分钟，96等分，一等分15分钟，seekbar数值：0~95
        refreshTimeSeekBar.setMax(95);
        final TextView textView = layoutView.findViewById(R.id.seekbar_select_layout_textview);

        int defaultValue = appConfigSP.getInt("当前刷新间隔(分钟):",15);
        textView.setText("当前刷新间隔(分钟):" + defaultValue);
        refreshTimeSeekBar.setProgress(defaultValue/15);

        refreshTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textView.setText("当前刷新间隔(分钟):" + (i+1)*15);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                stepTime[0] = seekBar.getProgress();
            }
        });
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int newRefreshTime = (refreshTimeSeekBar.getProgress()+1)*15;
                appConfigSPEditor.putInt("当前刷新间隔(分钟):",newRefreshTime);
                appConfigSPEditor.apply();

                JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                jobScheduler.cancel(12345);

                ComponentName componentName = new ComponentName(ScrollingActivity.this,UpdateService.class);
                JobInfo jobInfo = new JobInfo.Builder(12345,componentName)
                        .setPeriodic(newRefreshTime*60*1000)
                        .build();
                jobScheduler.schedule(jobInfo);


            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });


        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Window window = alertDialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_color)));

    }




    public void selectHitokotoType(View v){
//        ArrayList selection = new ArrayList();
//        String[] types = {"随机","动画、漫画","游戏","文学","影视","诗词","网易云"};
        AlertDialog.Builder TypeBuilder = new AlertDialog.Builder(this);
        TypeBuilder.setTitle("选择一言句子类型");
        View layoutView = LayoutInflater.from(this).inflate(R.layout.hitokoto_type_layout,null);
        TypeBuilder.setView(layoutView);


        final CheckBox randomCheckbox = layoutView.findViewById(R.id.random_checkbox);
        final CheckBox abCheckbox = layoutView.findViewById(R.id.animation_checkbox);
        final CheckBox cCheckbox = layoutView.findViewById(R.id.game_checkbox);
        final CheckBox dCheckbox = layoutView.findViewById(R.id.literature_checkbox);
        final CheckBox hCheckbox = layoutView.findViewById(R.id.film_checkbox);
        final CheckBox iCheckbox = layoutView.findViewById(R.id.poetry_checkbox);
        final CheckBox jCheckbox = layoutView.findViewById(R.id.neteasemusic_checkbox);


        List<CheckBox> checkBoxGroup = new ArrayList<CheckBox>();
        checkBoxGroup.add(randomCheckbox);
        checkBoxGroup.add(abCheckbox);
        checkBoxGroup.add(cCheckbox);
        checkBoxGroup.add(dCheckbox);
        checkBoxGroup.add(hCheckbox);
        checkBoxGroup.add(iCheckbox);
        checkBoxGroup.add(jCheckbox);

        for (CheckBox i:checkBoxGroup){
            i.setChecked (appConfigSP.getBoolean(i.getText().toString(),false));
        }




        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                if (compoundButton.isChecked()) {
                    if (compoundButton.getText().equals("随机")) {
                        Log.d("TAG", "onCheckedChanged: " + compoundButton.getText());
                        abCheckbox.setChecked(false);
                        cCheckbox.setChecked(false);
                        dCheckbox.setChecked(false);
                        hCheckbox.setChecked(false);
                        iCheckbox.setChecked(false);
                        jCheckbox.setChecked(false);
                    }
                    else{
                        randomCheckbox.setChecked(false);
                        Log.d("TAG", "onCheckedChanged: "+compoundButton.getText());
                    }
                 }
                appConfigSPEditor.putBoolean(compoundButton.getText().toString(),isCheck);
            }
        };

        randomCheckbox.setOnCheckedChangeListener(listener);
        abCheckbox.setOnCheckedChangeListener(listener);
        cCheckbox.setOnCheckedChangeListener(listener);
        dCheckbox.setOnCheckedChangeListener(listener);
        hCheckbox.setOnCheckedChangeListener(listener);
        iCheckbox.setOnCheckedChangeListener(listener);
        jCheckbox.setOnCheckedChangeListener(listener);

        TypeBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                appConfigSPEditor.apply();
            }
        });
        TypeBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog alertDialog = TypeBuilder.create();
        alertDialog.show();

        Window window = alertDialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_color)));
    }


    public void feedBack(View view){
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.coolapk.market");
        if (intent != null) {
//            intent.putExtra("type", "110");
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }else
            Toast.makeText(this,"你没有安装「酷安」app,请先安装",Toast.LENGTH_SHORT).show();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcast);

//        JobScheduler jobScheduler = (JobScheduler)this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
//        ComponentName componentName = new ComponentName(this,UpdateService.class);
//        int duration = appConfigSP.getInt("当前刷新间隔(分钟):",15);
//        JobInfo jobInfo = new JobInfo.Builder(12345, componentName)
//                .setPeriodic(duration * 60 * 1000)
//                .build();
//        int ret = jobScheduler.schedule(jobInfo);
//        if (ret == JobScheduler.RESULT_SUCCESS) {
//            Log.d(TAG, "Job scheduled successfully");
//        } else {
//            Log.d(TAG, "Job scheduling failed");
//        }

        Log.d(TAG, "onDestroy: ");
    }

    private void dayOrNightMode(){
        int currentNightMode = getApplicationContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're using the light theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're using dark theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:break;
        }
    }


    public class myBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.deepquotes.broadcast.updateTextView")) {
                headlineTextView.setText(intent.getStringExtra("quote"));

                updateHistoryQuotes();
                Log.d("广播", intent.getAction());
                Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
