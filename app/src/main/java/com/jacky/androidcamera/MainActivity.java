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
     * �û���
     */
    private String pUsername = "Jacky";
    /**
     * ��������ַ
     */
    private String serverUrl = "192.168.0.5";
    /**
     * �������˿�
     */
    private int serverPort = 8888;
    /**
     * ��Ƶˢ�¼��
     */
    private int VideoPreRate = 1;
    /**
     * ��ǰ��Ƶ���
     */
    private int tempPreRate = 0;
    /**
     * ��Ƶ����
     */
    private int VideoQuality = 85;

    /**
     * ������Ƶ��ȱ���
     */
    private float VideoWidthRatio = 1;
    /**
     * ������Ƶ�߶ȱ���
     */
    private float VideoHeightRatio = 1;

    /**
     * ������Ƶ���
     */
    private int VideoWidth = 320;
    /**
     * ������Ƶ�߶�
     */
    private int VideoHeight = 240;
    /**
     * ��Ƶ��ʽ����
     */
    private int VideoFormatIndex = 0;
    /**
     * �Ƿ�����Ƶ
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
                    mBtnTransmit.setText("��ʼ����");
                    startSendVideo = false;
                } else {
                    mBtnTransmit.setText("ֹͣ����");
                    startSendVideo = true;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        mSurfaceHolder = mSurfaceView.getHolder(); // ��SurfaceView��ȡ��SurfaceHolder����
        mSurfaceHolder.addCallback(this); // SurfaceHolder����ص��ӿ�
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// ������ʾ�����ͣ�setType��������
        //��ȡ�����ļ�
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
                mCamera.setPreviewCallback(null); // �������������ǰ����Ȼ�˳�����
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
        menu.add(0, 0, 0, "ϵͳ����");
        menu.add(0, 1, 1, "���ڳ���");
        menu.add(0, 2, 2, "�˳�����");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        super.onOptionsItemSelected(item);//��ȡ�˵�
        switch (item.getItemId()) {//�˵����
            case 0:
                //ϵͳ����
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
            case 1://���ڳ���
                new AlertDialog.Builder(this)
                        .setTitle("���ڱ�����")
                        .setMessage("��������Jacky������д��\nEmail��fusijie@vip.qq.com")
                        .setPositiveButton("��֪����", null).show();
                break;
            case 2://�˳�����
                //ɱ���߳�ǿ���˳�
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
        mCamera.setDisplayOrientation(90); //���ú���¼��
        //��ȡ����ͷ����
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
            mCamera.setPreviewCallback(null); // �������������ǰ����Ȼ�˳�����
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        //���û��ָ�����Ƶ�����Ȳ���
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
                    //�� ������ͼƬ�ĳߴ������
                    //long time3 = System.currentTimeMillis();
                    image.compressToJpeg(new Rect(0, 0, (int) (VideoWidthRatio * VideoWidth),
                            (int) (VideoHeightRatio * VideoHeight)), VideoQuality, outstream);
                    //long time4 = System.currentTimeMillis();
                    //long delta1=time2-time1;
                    //long delta2=time4-time3;
                    //Log.v("delta",Long.toString(delta2) );
                    outstream.flush();
                    //�����߳̽�ͼ�����ݷ��ͳ�ȥ
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
     * ���������߳�
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
     * �����ļ��߳�
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

    /*UDP�����߳�*/
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
                //����һ��InetAddree
                InetAddress serverAddress = InetAddress.getByName(ipname);

                //String msgHead=java.net.URLEncoder.encode("PHONEVIDEO|"+username+"|","utf-8");
                //byte[] buffer=msgHead.getBytes();
                //DatagramPacket packetHead = new DatagramPacket(buffer,buffer.length,serverAddress,port);
                //socket.send(packetHead);

                byte data[] = myoutputstream.toByteArray();  //����Ҫ���������
                //����һ��DatagramPacket���󣬲�ָ��Ҫ��������ݰ����͵����統�е��ĸ���ַ���Լ��˿ں�
                int abc = data.length;
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
                //����socket�����send��������������
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

