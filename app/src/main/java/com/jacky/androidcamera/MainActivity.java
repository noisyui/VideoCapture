package com.jacky.androidcamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    /**
     * Debug
     */
    private long timepre = 0;
    private long timeaft = 0;

    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private Camera mCamera = null;
    private Button mBtnTransmit;

    /**
     * 用户名
     */
    private String pUsername = "Jacky";
    /**
     * 服务器地址
     */
    private String serverUrl = "192.168.0.5";
    /**
     * 服务器端口
     */
    private int serverPort = 8888;
    /**
     * 视频刷新间隔
     */
    private int VideoPreRate = 1;
    /**
     * 当前视频序号
     */
    private int tempPreRate = 0;
    /**
     * 视频质量
     */
    private int VideoQuality = 85;

    /**
     * 发送视频宽度比例
     */
    private float VideoWidthRatio = 1;
    /**
     * 发送视频高度比例
     */
    private float VideoHeightRatio = 1;

    /**
     * 发送视频宽度
     */
    private int VideoWidth = 320;
    /**
     * 发送视频高度
     */
    private int VideoHeight = 240;
    /**
     * 视频格式索引
     */
    private int VideoFormatIndex = 0;
    /**
     * 是否发送视频
     */
    private boolean startSendVideo = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_preview);
        mBtnTransmit = (Button) findViewById(R.id.transmit);

        mBtnTransmit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (startSendVideo) {
                    mBtnTransmit.setText("开始传输");
                    startSendVideo = false;
                } else {
                    mBtnTransmit.setText("停止传输");
                    startSendVideo = true;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        mSurfaceHolder = mSurfaceView.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        mSurfaceHolder.addCallback(this); // SurfaceHolder加入回调接口
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 设置显示器类型，setType必须设置
        //读取配置文件
        SharedPreferences preParas = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        pUsername = preParas.getString("Username", "Jccky");
        serverUrl = preParas.getString("ServerUrl", "192.168.0.5");
        String tempStr = preParas.getString("ServerPort", "8888");
        serverPort = Integer.parseInt(tempStr);
        tempStr = preParas.getString("VideoPreRate", "1");
        VideoPreRate = Integer.parseInt(tempStr);
        tempStr = preParas.getString("VideoQuality", "85");
        VideoQuality = Integer.parseInt(tempStr);
        tempStr = preParas.getString("VideoWidthRatio", "100");
        VideoWidthRatio = Integer.parseInt(tempStr);
        tempStr = preParas.getString("VideoHeightRatio", "100");
        VideoHeightRatio = Integer.parseInt(tempStr);
        VideoWidthRatio = VideoWidthRatio / 100f;
        VideoHeightRatio = VideoHeightRatio / 100f;

        super.onStart();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(0, 0, 0, "系统设置");
        menu.add(0, 1, 1, "关于程序");
        menu.add(0, 2, 2, "退出程序");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        super.onOptionsItemSelected(item);//获取菜单
        switch (item.getItemId()) {//菜单序号
            case 0:
                //系统设置
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
            case 1://关于程序
                new AlertDialog.Builder(this)
                        .setTitle("关于本程序")
                        .setMessage("本程序由Jacky制作编写。\nEmail：fusijie@vip.qq.com")
                        .setPositiveButton("我知道了", null).show();
                break;
            case 2://退出程序
                //杀掉线程强制退出
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
        }
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        // TODO Auto-generated method stub
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        mCamera.setPreviewCallback(this);
        mCamera.setDisplayOrientation(90); //设置横行录制
        //获取摄像头参数
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(640, 480);
        //parameters.setPreviewFrameRate(10);
        mCamera.setParameters(parameters);
        Size size = parameters.getPreviewSize();
        VideoWidth = size.width;
        VideoHeight = size.height;
        VideoFormatIndex = parameters.getPreviewFormat();

        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        // TODO Auto-generated method stub
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        // TODO Auto-generated method stub
        if (null != mCamera) {
            mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        //如果没有指令传输视频，就先不传
        if (!startSendVideo)
            return;
        if (tempPreRate < VideoPreRate) {
            tempPreRate++;
            return;
        }
        tempPreRate = 0;
        try {
            if (data != null) {
                //long time1 = System.currentTimeMillis();
                YuvImage image = new YuvImage(data, VideoFormatIndex, VideoWidth, VideoHeight, null);
                //long time2 = System.currentTimeMillis();
                if (image != null) {
                    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                    //在 此设置图片的尺寸和质量
                    //long time3 = System.currentTimeMillis();
                    image.compressToJpeg(new Rect(0, 0, (int) (VideoWidthRatio * VideoWidth),
                            (int) (VideoHeightRatio * VideoHeight)), VideoQuality, outstream);
                    //long time4 = System.currentTimeMillis();
                    //long delta1=time2-time1;
                    //long delta2=time4-time3;
                    //Log.v("delta",Long.toString(delta2) );
                    outstream.flush();
                    //启用线程将图像数据发送出去
                    Thread th = new MySendFileThread(outstream, pUsername, serverUrl, serverPort);
                    th.start();
                    //Thread udpth=new UDPMySendFileThread(outstream,pUsername,serverUrl,serverPort);
                    //udpth.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送命令线程
     */
    class MySendCommandThread extends Thread {

        private String command;

        public MySendCommandThread(String command) {
            // TODO Auto-generated constructor stub
            this.command = command;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Socket commandSocket = new Socket(serverUrl, serverPort);
                PrintWriter out = new PrintWriter(commandSocket.getOutputStream());
                out.println(command);
                out.flush();
            } catch (UnknownHostException e) {
                // TODO: handle exception
            } catch (IOException e) {

            }
        }

    }

    /**
     * 发送文件线程
     */
    class MySendFileThread extends Thread {

        private String username;
        private String ipname;
        private int port;
        private byte byteBuffer[] = new byte[1024];
        private OutputStream outsocket;
        private ByteArrayOutputStream myoutputstream;

        public MySendFileThread(ByteArrayOutputStream myoutputstream, String username, String ipname, int port) {
            // TODO Auto-generated constructor stub
            this.myoutputstream = myoutputstream;
            this.username = username;
            this.ipname = ipname;
            this.port = port;
            try {
                myoutputstream.close();
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Socket tempSocket = new Socket(ipname, port);
                outsocket = tempSocket.getOutputStream();

                //String msgHead=java.net.URLEncoder.encode("PHONEVIDEO|"+username+"|","utf-8");
                //byte[] buffer=msgHead.getBytes();
                //outsocket.write(buffer);

                ByteArrayInputStream inputstream = new ByteArrayInputStream(myoutputstream.toByteArray());
                //int aa=myoutputstream.toByteArray().length;

                int amount;
                while ((amount = inputstream.read(byteBuffer)) != -1) {
                    outsocket.write(byteBuffer, 0, amount);
                }
                outsocket.flush();
                outsocket.close();
                tempSocket.close();
                timeaft = System.currentTimeMillis();
                Long delta = timeaft - timepre;
                timepre = timeaft;
                Log.v("delta", Long.toString(delta));

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    /*UDP发送线程*/
    class UDPMySendFileThread extends Thread {
        private String username;
        private String ipname;
        private int port;
        private ByteArrayOutputStream myoutputstream;

        public UDPMySendFileThread(ByteArrayOutputStream myoutputstream, String username, String ipname, int port) {
            // TODO Auto-generated constructor stub
            this.myoutputstream = myoutputstream;
            this.username = username;
            this.ipname = ipname;
            this.port = port;
            try {
                myoutputstream.close();
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            //super.run();
            try {
                DatagramSocket socket = new DatagramSocket(port);
                //创建一个InetAddree
                InetAddress serverAddress = InetAddress.getByName(ipname);

                //String msgHead=java.net.URLEncoder.encode("PHONEVIDEO|"+username+"|","utf-8");
                //byte[] buffer=msgHead.getBytes();
                //DatagramPacket packetHead = new DatagramPacket(buffer,buffer.length,serverAddress,port);
                //socket.send(packetHead);

                byte data[] = myoutputstream.toByteArray();  //这是要传输的数据
                //创建一个DatagramPacket对象，并指定要讲这个数据包发送到网络当中的哪个地址，以及端口号
                int abc = data.length;
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
                //调用socket对象的send方法，发送数据
                socket.send(packet);
                //		        int ncount=data.length/500;
                //		        for(int i=0;i<ncount;i++)
                //		        {
                //		        	DatagramPacket packet = new DatagramPacket(data,i*500,500,serverAddress,port);
                //		        	socket.send(packet);
                //		        	timeaft=System.currentTimeMillis();
                //		        	long deltasend=timeaft-timepre;
                //		        	String string=Long.toString(deltasend)+"#"+Integer.toString(500);
                //		        	 Log.v("deltaSend",string);
                //			           timepre=timeaft;
                //		        }
                //		        DatagramPacket packet = new DatagramPacket(data,ncount*500,data.length%500,serverAddress,port);
                //		        socket.send(packet);
                timeaft = System.currentTimeMillis();
                long deltasend = timeaft - timepre;
                String string = Long.toString(deltasend) + "#" + Integer.toString(abc);
                //String string=Long.toString(deltasend)+"#"+Integer.toString(data.length%500);
                Log.v("deltaSend", string);
                timepre = timeaft;
                socket.close();
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }

        }
    }
}

