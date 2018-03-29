package com.example.x.listenme;

//My GitHub

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.drm.DrmStore;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends FragmentActivity {
    String strFilePath;
    boolean isShowText = false;
    final int REQUEST_CODE_OPEN_FILE = 6;

    static MediaPlayer player = new MediaPlayer();
    AudioManager audioManager;
    FileDialog fileDialog;

    TextView txtFilePath, txtPosition, txtText, txtVolume, txtTemp;
    Button btnOpenFile, btnLRC, btnClear, btnForward, btnBack, btnRePlay, btnPlayOrPause, btnPre, btnNext, btnInsertPoint, btnDelPoint, btnShowText, btnVolumeUp, btnVolumeDown;
    Button btnSimple, btnComplex;
    TextView txtShowTextInSecond;
    Button btnShowTextInSecond, btnNextInSecond, btnAnotherNextInSecond, btnPreInSecond;

    ImageView imageViewProgress;

    SortedList pointList = new SortedList();
    private View complexView, simpleView;

    static class HandlerProgress extends Handler {
        WeakReference<MainActivity> mActivity;

        HandlerProgress(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity theActivity = mActivity.get();
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    ImageView image = theActivity.imageViewProgress;
                    MediaPlayer player = theActivity.player;
                    SortedList list = theActivity.pointList;

                    int width = image.getWidth();
                    int height = image.getHeight();

                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    Paint paint = new Paint();
                    paint.setColor(Color.argb(0xff, 0x11, 0x11, 0x11));
                    canvas.drawRect(0, 0, width, height, paint);
                    paint.setColor(Color.argb(0xff, 0x55, 0x55, 0x55));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1);
                    paint.setColor(Color.argb(0xff, 0x55, 0x55, 0x55));
                    canvas.drawRect(40, 30, 1040, 90, paint);
                    //canvas.drawRect(40, 40, 1040, 80, paint);
                    try {
                        if (player.getDuration() > 0) {
                            int duration = player.getDuration();
                            int position = player.getCurrentPosition();
                            int value = (position * 1000) / duration;
                            int startValue = (int) ((Math.round(list.getValueByPosition(list.position) * 1000) / duration) + 40);
                            paint.setStyle(Paint.Style.FILL);
                            canvas.drawRect(startValue, 46, value + 40, 74, paint);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(3);
                            for (int i = 0; i < list.getSize(); i++) {
                                value = (int) ((Math.round(list.getValueByPosition(i)) * 1000) / duration + 40);
                                canvas.drawLine(value, 30, value, 90, paint);
                            }
                            value = (int) ((Math.round(list.getValueByPosition(list.position) * 1000) / duration) + 40);
                            paint.setStyle(Paint.Style.FILL);
                            canvas.drawCircle(value, 82, 6, paint);
                            if (player.getCurrentPosition() >= list.getNextPoint())
                                player.pause();
                            String str = "#";
                            for (int i = 0; i < theActivity.pointList.getSize(); i++)
                                str = str + " " + theActivity.pointList.getValueByPosition(i);
                            int m = (position / 1000) / 60;
                            int s = (position / 1000) % 60;
                            long currentPoint = (long) list.getCurrentPoint();
                            String currentPointString = String.valueOf((currentPoint / 1000) / 60) + "分" + String.valueOf((currentPoint / 1000) % 60) + "秒";
                            theActivity.txtPosition.setText(currentPointString + "->" + String.valueOf(m) + "分" + String.valueOf(s) + "秒");

                            if (theActivity.isShowText) {
                                theActivity.btnShowText.setText("隐藏");
                                theActivity.btnShowTextInSecond.setText("隐");
                                str = list.getCurrentDataString();
                                if (str != null) {
                                    theActivity.txtText.setText(str);
                                    theActivity.txtShowTextInSecond.setText(str);
                                } else {
                                    theActivity.txtText.setText("无");
                                    theActivity.txtShowTextInSecond.setText("");
                                }
                            } else {
                                theActivity.btnShowText.setText("显示");
                                theActivity.btnShowTextInSecond.setText("显");
                                theActivity.txtText.setText("");
                                theActivity.txtShowTextInSecond.setText("");
                            }

                            theActivity.txtFilePath.setText(theActivity.strFilePath);

                            theActivity.showVolume();
                        }
                    } catch (Exception e) {
                        theActivity.txtPosition.setText(e.getMessage());
                    }
                    image.setImageBitmap(bitmap);
                    break;
            }
        }
    }

    private final HandlerProgress handlerProgress = new HandlerProgress(this);


    class ThreadProgress extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    txtPosition.setText(e.getMessage());
                }
                Message message = new Message();
                message.what = 1;
                handlerProgress.sendMessage(message);
            }
        }
    }

//    class KeyBroadcastReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//                txtTemp.setText("好");
//                abortBroadcast();
//            }
//        }
//    }
//
//    private KeyBroadcastReceiver keyBroadcastReceiver; //= new KeyBroadcastReceiver(this);
//    private IntentFilter keyIntentFilter;

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        complexView = getLayoutInflater().inflate(R.layout.activity_main, null);
        simpleView = getLayoutInflater().inflate(R.layout.layout_simple, null);
        setContentView(complexView);

        verifyStoragePermissions(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        imageViewProgress = complexView.findViewById(R.id.surfaceViewProgress);
        new ThreadProgress().start();

//        try {
//            keyBroadcastReceiver = new KeyBroadcastReceiver();
//            keyIntentFilter = new IntentFilter();
//            keyIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//            registerReceiver(keyBroadcastReceiver, keyIntentFilter);
//        } catch (Exception e) {
//            txtTemp.setText(e.getMessage());
//        }

        txtFilePath = (TextView) complexView.findViewById(R.id.txtFilePath);
        txtPosition = (TextView) complexView.findViewById(R.id.txtPosition);
        txtText = complexView.findViewById(R.id.txtText);
        txtVolume = complexView.findViewById(R.id.txtVolume);
        txtTemp = complexView.findViewById(R.id.textView2);

        btnOpenFile = (Button) complexView.findViewById(R.id.btnOpenFile);
        btnLRC = (Button) complexView.findViewById(R.id.btnLRC);
        btnClear = (Button) complexView.findViewById(R.id.btnClear);
        btnForward = (Button) complexView.findViewById(R.id.btnForward);
        btnBack = (Button) complexView.findViewById(R.id.btnBack);
        btnRePlay = (Button) complexView.findViewById(R.id.btnRePlay);
        btnPlayOrPause = (Button) complexView.findViewById(R.id.btnPlayOrPause);
        btnNext = (Button) complexView.findViewById(R.id.btnNext);
        btnPre = (Button) complexView.findViewById(R.id.btnPre);
        btnDelPoint = (Button) complexView.findViewById(R.id.btnDelPoint);
        btnInsertPoint = (Button) complexView.findViewById(R.id.btnInsertPoint);
        btnShowText = complexView.findViewById(R.id.btnShowText);
        btnVolumeUp = complexView.findViewById(R.id.btnVolumeUp);
        btnVolumeDown = complexView.findViewById(R.id.btnVolumeDown);

        btnSimple = complexView.findViewById(R.id.btnSimple);
        btnComplex = simpleView.findViewById(R.id.btnComplex);

        txtShowTextInSecond = simpleView.findViewById(R.id.txtShowTextInSecond);
        btnNextInSecond = simpleView.findViewById(R.id.btnNextInSecond);
        btnShowTextInSecond = simpleView.findViewById(R.id.btnShowTextInSecond);
        btnPreInSecond = simpleView.findViewById(R.id.btnPreInSecond);
        btnAnotherNextInSecond = simpleView.findViewById(R.id.btnAnotherNextInSecond);

        initCustomSetting();

        String dir = strFilePath.substring(0,strFilePath.lastIndexOf("/"));//"/storage/";//
        File dialogDir=new File(dir);
        if(!dialogDir.exists())
            dialogDir=new File("/storage/");
        Log.v("dir",dir);
        txtTemp.setText(strFilePath+"\n"+ dir);

        fileDialog = new FileDialog(this, dialogDir, ".mp3");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file) {
                strFilePath=file.getAbsolutePath();
            }
        });

        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toOpenFile();
            }
        });

        btnRePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toRePlay();
            }
        });

        btnPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPlayOrPause();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNext();
            }
        });

        btnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPre();
            }
        });

        btnInsertPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toInsertPoint();
            }
        });

        btnDelPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDelPoint();
            }
        });

        btnLRC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toLRC();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toClear();
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toForward();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toBack();
            }
        });

        btnShowText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toShowText();
            }
        });

        btnVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDownVolume();
            }
        });

        btnVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toUpVolume();
            }
        });

        btnSimple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(simpleView);
            }
        });

        btnComplex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(complexView);
            }
        });

        btnShowTextInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toShowText();
            }
        });

        btnNextInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNext();
            }
        });

        btnAnotherNextInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toNext();
            }
        });

        btnPreInSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPre();
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
//        txtTemp.setText(event.toString());
        //本函数处理onkeydown无法处理的几个键
        String characters = event.getCharacters();//返回全角字符
        if (characters != null) {
            switch (characters) {
                case "＋":
                    toPlayOrPause();
                    break;
                case "－":
                    toRePlay();
                    break;
                case "＊":
                    toForward();
                    break;
                case "／":
                    toBack();
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        txtTemp.setText(String.valueOf(keyCode) + "&&&" + event.toString());
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: //音量键 上
                toRePlay();
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN: //音量键 下
                if ((!player.isPlaying()) && (pointList.getNextPoint() <= player.getCurrentPosition()))
                    toRePlay();
                else
                    toPlayOrPause();
                break;

            case KeyEvent.KEYCODE_NUMPAD_9:
                toPre();
                break;
            case KeyEvent.KEYCODE_NUMPAD_3:
                toNext();
                break;

            case KeyEvent.KEYCODE_NUMPAD_0:
                toInsertPoint();
                break;
            case KeyEvent.KEYCODE_NUMPAD_DOT:
                toDelPoint();
                break;

            case KeyEvent.KEYCODE_NUMPAD_5:
                toLRC();
                break;

            case KeyEvent.KEYCODE_BACK: //不屏蔽返回建
                super.onKeyDown(keyCode, event);
                break;
        }
        return true;
    }

    private void showVolume() {
        txtVolume.setText(String.valueOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
    }

    private int getCurrentVolume() {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void toUpVolume() {
        int oldIndex = getCurrentVolume();
        int maxIndex = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (oldIndex + 1 < maxIndex)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldIndex + 1, AudioManager.FLAG_PLAY_SOUND);
        showVolume();
    }

    private void toDownVolume() {
        int oldIndex = getCurrentVolume();
        if (oldIndex - 1 >= 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldIndex - 1, AudioManager.FLAG_PLAY_SOUND);
        showVolume();
    }

    private void toClear() {
        pointList.clearPoint();
    }

    private void toLRC() {
        loadLRC();
    }

    private void toShowText() {
        isShowText = !isShowText;
    }

    private void toBack() {
        int pos = player.getCurrentPosition();
        if (pos - 2000 > 0)
            player.seekTo(pos - 2000);
        else
            player.seekTo(0);
        player.start();
    }

    private void toForward() {
        int pos = player.getCurrentPosition();
        if (pos + 2000 < player.getDuration())
            player.seekTo(pos + 2000);
        player.start();
    }

    private void toDelPoint() {
        pointList.deleteCurrentPoint();
        toRePlay();
    }

    private void toInsertPoint() {
        pointList.position = pointList.insertByOrder(player.getCurrentPosition());
        toRePlay();
    }

    private void toPre() {
        if (pointList.position > 0) {
            pointList.position--;
        }
        toRePlay();
    }

    private void toNext() {
        if (pointList.position < pointList.getSize() - 2) {
            pointList.position++;
        }
        toRePlay();
    }

    private void toPlayOrPause() {
        if (player.isPlaying()) player.pause();
        else player.start();
    }

    private void toRePlay() {
        player.seekTo((int) Math.round(pointList.getCurrentPoint()));
        player.start();
    }

    private void toOpenFile() {
        fileDialog.showDialog();
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("audio/MP3");//设置类型，我这里是任意类型，任意后缀的可以这样写。
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopCustomSettings();
//        unregisterReceiver(keyBroadcastReceiver);
    }

    private void stopCustomSettings() {
        String settingFileName = getApplicationContext().getFilesDir() + "/setting.txt";
        File file = new File(settingFileName);
        if (file.exists())
            file.delete();

        try {
            file.createNewFile();
            OutputStream outStream = new FileOutputStream(file);//设置输出流
            OutputStreamWriter out = new OutputStreamWriter(outStream);//设置内容输出方式
            out.write(strFilePath + "\n");//输出内容到文件中
            out.close();
        } catch (java.io.IOException e) {
            android.util.Log.i("stop", e.getMessage());
        }
    }

    private void initCustomSetting() {
        String settingFileName = getApplicationContext().getFilesDir() + "/setting.txt";
        File file = new File(settingFileName);

        try {
            if (file.exists()) {
                InputStream inputStream = new FileInputStream(file);//读取输入流
                InputStreamReader inputReader = new InputStreamReader(inputStream);//设置流读取方式
                BufferedReader bufferedReader = new BufferedReader(inputReader);
                String line = bufferedReader.readLine();
                strFilePath = line;
                player.reset();
                player.setDataSource(line);
                player.prepare();
                initPoint();
            }
        } catch (Exception e) {
            android.util.Log.i("init", e.getMessage());
        }
    }

    protected void initPoint() {
        pointList.insertByOrder(0);
        pointList.insertByOrder(player.getDuration());
    }

    protected void loadLRC() {
        try {
            String fileName = strFilePath.substring(0, strFilePath.length() - 4) + ".lrc";
            File file = new File(strFilePath);
            if (file.exists()) {
                FileReader fr = new FileReader(fileName);
                BufferedReader br = new BufferedReader(fr);
                String str;
                while ((str = br.readLine()) != null) {
                    Double value = Double.valueOf(str.substring(1, 3)) * 60 * 1000 + Double.valueOf(str.substring(4, 10)) * 1000;
                    String string = str.substring(11, str.length());
                    pointList.insertByOrder(value, string);
                }
                br.close();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_OPEN_FILE) {
                strFilePath = getRealPath(data.getData());
                txtFilePath.setText(strFilePath);
                try {
                    player.reset();
                    player.setDataSource(strFilePath);
                    player.prepare();
                    initPoint();
                } catch (IOException e) {
                    txtFilePath.setText(e.getMessage());
                }
            }
        }
    }

    protected String getRealPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow("_data");
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        } else {
            return null;
        }
    }


    //

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
