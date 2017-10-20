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

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    /**
     * Debug
     */
    private long timePre;
    private long timeAft;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Button mBtnTransmit;

    private String username = "Jacky";
    private String serverUrl = "192.168.1.106";
    private int serverPort = 8888;

    private int videoPreRate = 1;
    private int tempPreRate;
    private int videoQuality = 85;
    private float videoWidthRatio = 1;
    private float videoHeightRatio = 1;
    private int videoWidth = 320;
    private int videoHeight = 240;
    private int videoFormatIndex;
    private boolean startSendVideo;


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
                if (startSendVideo) {
                    mBtnTransmit.setText(R.string.start_transfer);
                    startSendVideo = false;
                } else {
                    mBtnTransmit.setText(R.string.stop_transfer);
                    startSendVideo = true;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        SharedPreferences preParas = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        username = preParas.getString("Username", "Jacky");
        serverUrl = preParas.getString("ServerUrl", "192.168.1.106");
        String tempStr = preParas.getString("ServerPort", "8888");
        serverPort = Integer.parseInt(tempStr);
        tempStr = preParas.getString("VideoPreRate", "1");
        videoPreRate = Integer.parseInt(tempStr);
        tempStr = preParas.getString("videoQuality", "85");
        videoQuality = Integer.parseInt(tempStr);
        tempStr = preParas.getString("videoWidthRatio", "100");
        videoWidthRatio = Integer.parseInt(tempStr);
        tempStr = preParas.getString("videoHeightRatio", "100");
        videoHeightRatio = Integer.parseInt(tempStr);
        videoWidthRatio = videoWidthRatio / 100f;
        videoHeightRatio = videoHeightRatio / 100f;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
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
        menu.add(0, 0, 0, "System Config");
        menu.add(0, 1, 1, "About");
        menu.add(0, 2, 2, "Exit");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 0:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
            case 1:
                new AlertDialog.Builder(this)
                        .setTitle("About")
                        .setMessage("My Email：fusijie@vip.qq.com")
                        .setPositiveButton("OK", null).show();
                break;
            case 2:
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
        }
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        mCamera.setPreviewCallback(this);
        mCamera.setDisplayOrientation(90);
        // get the parameters of camera
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(640, 480);
        //parameters.setPreviewFrameRate(10);
        mCamera.setParameters(parameters);
        Size size = parameters.getPreviewSize();
        videoWidth = size.width;
        videoHeight = size.height;
        videoFormatIndex = parameters.getPreviewFormat();

        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
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
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!startSendVideo)
            return;
        if (tempPreRate < videoPreRate) {
            tempPreRate++;
            return;
        }
        tempPreRate = 0;
        try {
            if (data != null) {
                YuvImage image = new YuvImage(data, videoFormatIndex, videoWidth, videoHeight, null);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                // set the size and quality of the image
                image.compressToJpeg(new Rect(0, 0, (int) (videoWidthRatio * videoWidth),
                        (int) (videoHeightRatio * videoHeight)), videoQuality, outStream);
                outStream.flush();

                Thread th = new MySendFileThread(outStream, username, serverUrl, serverPort);
                th.start();

                //Thread udpth=new UDPMySendFileThread(outstream,username,serverUrl,serverPort);
                //udpth.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MySendCommandThread extends Thread {

        private String command;

        public MySendCommandThread(String command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                Socket commandSocket = new Socket(serverUrl, serverPort);
                PrintWriter out = new PrintWriter(commandSocket.getOutputStream());
                out.println(command);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class MySendFileThread extends Thread {

        private String username;
        private String ipName;
        private int port;
        private byte byteBuffer[] = new byte[1024];
        private OutputStream outSocket;
        private ByteArrayOutputStream myOutputStream;

        MySendFileThread(ByteArrayOutputStream myOutputStream, String username, String ipName, int port) {
            this.myOutputStream = myOutputStream;
            this.username = username;
            this.ipName = ipName;
            this.port = port;
            try {
                myOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                Socket tempSocket = new Socket(ipName, port);
                outSocket = tempSocket.getOutputStream();

                ByteArrayInputStream inputStream = new ByteArrayInputStream(myOutputStream.toByteArray());

                int amount;
                while ((amount = inputStream.read(byteBuffer)) != -1) {
                    outSocket.write(byteBuffer, 0, amount);
                }
                outSocket.flush();
                outSocket.close();
                tempSocket.close();
                timeAft = System.currentTimeMillis();
                Long delta = timeAft - timePre;
                timePre = timeAft;
                Log.v("delta", Long.toString(delta));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    class UDPMySendFileThread extends Thread {
        private String username;
        private String ipName;
        private int port;
        private ByteArrayOutputStream myOutputStream;

        public UDPMySendFileThread(ByteArrayOutputStream myOutputStream, String username, String ipname, int port) {
            this.myOutputStream = myOutputStream;
            this.username = username;
            this.ipName = ipname;
            this.port = port;
            try {
                myOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket(port);

                InetAddress serverAddress = InetAddress.getByName(ipName);

                byte data[] = myOutputStream.toByteArray();
                // create a DatagramPacket object and specify the address and port
                int abc = data.length;
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
                // send data
                socket.send(packet);
                timeAft = System.currentTimeMillis();
                long deltaSend = timeAft - timePre;
                String string = Long.toString(deltaSend) + "#" + Integer.toString(abc);
                Log.v("deltaSend", string);
                timePre = timeAft;
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

