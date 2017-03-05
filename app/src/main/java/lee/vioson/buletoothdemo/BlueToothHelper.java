package lee.vioson.buletoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Author:李烽
 * Date:17/3/4
 * FIXME
 * TODO 蓝牙操作的助手类
 */

public class BlueToothHelper {

    private static final String UUID_STRING = "17ceb52e-1bdb-4d81-8bec-624e39ef3081";
    private static final int REQUEST_CODE_OPEN_BLUETOOTH = 0x001;

    private final BluetoothAdapter mBluetoothAdapter;

    private Context context;
    private static BlueToothHelper instance = null;
    private static BluetoothSocket mSocket;
    private static String getSocketErrorMessage;
    private static String matchDeviceErrorMsg;
    private static String socketConnectErrorMsg;
    private static String socketCloseErrorMsg;


    public class HandlerArg {
        private static final int TO_GET_ADAPTER = 0x0001;
        private static final int TO_GET_REMOTE_DEVICE = 0x0002;
        private static final int TO_GET_SOCKET = 0x0003;
        private static final int GET_SOCKET_ERROR = 0x0004;
        private static final int GET_SOCKET_OK = 0x0005;
        private static final int MATCH_DEVICE_ING = 0x0006;
        private static final int MATCH_DEVICE_OK = 0x0007;
        private static final int MATCH_DEVICE_ERROR = 0x0008;
        public static final int SOCKET_CONNECTTING = 0x0009;
        public static final int SOCKET_CONNECT_ERROR = 0x0010;
        public static final int SOCKET_CLOSE_ERROR = 0x0011;
    }

    private BlueToothHelper(Context context) {
        this.context = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BlueToothHelper getInstace(Context c) {
        synchronized (BlueToothHelper.class) {
            if (instance == null)
                instance = new BlueToothHelper(c);
            return instance;
        }
    }


    /**
     * 参数 无
     * 返回值 true 表示可以用嘛，否则不可以
     * 异常 无
     * 描述：这个方法用于检查蓝牙是否可用
     */
    public boolean checkBtIsValueble() {
        if (mBluetoothAdapter == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 打开蓝牙
     * 带提示的
     */
    public void openBT(Activity context) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivityForResult(intent, REQUEST_CODE_OPEN_BLUETOOTH);
    }

    public void onActivityResult(int requestCode, int resultCode, ActionListener listener) {
        if (requestCode == REQUEST_CODE_OPEN_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK)
                listener.onSuccess(null);
            else listener.onFail();
        }
    }

    /**
     * 打开蓝牙没有提示的
     */
    public void enableBT() {

        mBluetoothAdapter.enable();
    }

    /**
     * 关闭蓝牙
     */
    public void closeBT() {
        mBluetoothAdapter.disable();
    }

    /**
     * 开始扫描
     */
    public boolean discoveryBT() {
        return mBluetoothAdapter.startDiscovery();
    }

    /**
     * 注册监听
     */
    public void registerScan(ActionListener<BluetoothDevice> listener) {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(new BTDiscoveryBroadcastReceiver(listener), intentFilter);
    }

    /**
     * 扫描到的蓝牙设备
     */
    public class BTDiscoveryBroadcastReceiver extends BroadcastReceiver {
        public BTDiscoveryBroadcastReceiver(ActionListener<BluetoothDevice> listener) {
            this.listener = listener;
        }

        private ActionListener listener;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action))
                listener.onSuccess(bluetoothDevice);
            else listener.onFail();
        }
    }

    /**
     * 配对之前的获取设备
     *
     * @param address
     */
    private void createSocket(String address) {
        if (mBluetoothAdapter == null) {
            btHandler.sendEmptyMessage(HandlerArg.TO_GET_ADAPTER);
        }
        btHandler.sendEmptyMessage(HandlerArg.TO_GET_REMOTE_DEVICE);
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(address);

        try {
            btHandler.sendEmptyMessage(HandlerArg.TO_GET_SOCKET);
            mSocket = remoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
            btHandler.sendEmptyMessage(HandlerArg.GET_SOCKET_OK);
        } catch (IOException e) {
            e.printStackTrace();
            getSocketErrorMessage = e.getMessage();
            btHandler.sendEmptyMessage(HandlerArg.GET_SOCKET_ERROR);
        }
    }

    public void createBlueToothSocket(final String address, CreateSocketListener listener) {
        createSocketListener = listener;
        new Thread(new Runnable() {
            @Override
            public void run() {
                createSocket(address);
            }
        }).start();
    }

    private CreateSocketListener createSocketListener;

    public interface CreateSocketListener {
        void toGetAdapter();

        void toGetRemoteDevice();

        void toGetSocket();

        void getSocketOk(BluetoothSocket socket);

        void getSocketError(String msg);
    }

    /**
     * 配对
     */
    private void matchDevice(BluetoothDevice device) {
        mBluetoothAdapter.cancelDiscovery();//取消发现
        try {
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                Method method = BluetoothDevice.class.getMethod("createBond");
                btHandler.sendEmptyMessage(HandlerArg.MATCH_DEVICE_ING);
                method.invoke(device);
            } else {
                btHandler.sendEmptyMessage(HandlerArg.MATCH_DEVICE_OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            matchDeviceErrorMsg = e.getMessage();
            btHandler.sendEmptyMessage(HandlerArg.MATCH_DEVICE_ERROR);
        }

    }

    public void matchDevice(final BluetoothDevice device, MatchDeviceListener listener) {
        matchDeviceListener = listener;
        new Thread(new Runnable() {
            @Override
            public void run() {
                matchDevice(device);
            }
        }).start();
    }

    private MatchDeviceListener matchDeviceListener;

    public interface MatchDeviceListener {
        void matching();

        void matchOk();

        void matchError(String msg);
    }

    private void link(BluetoothSocket socket) {
        try {
            socket.connect();
            btHandler.sendEmptyMessage(HandlerArg.SOCKET_CONNECTTING);
        } catch (IOException e) {
            socketConnectErrorMsg = e.getMessage();
            btHandler.sendEmptyMessage(HandlerArg.SOCKET_CONNECT_ERROR);
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
            } catch (IOException e2) {
                socketCloseErrorMsg = e2.getMessage();
                btHandler.sendEmptyMessage(HandlerArg.SOCKET_CLOSE_ERROR);
            }

        }
    }

    public void connect(final BluetoothSocket socket, ConnectListener listener) {
        connectListener = listener;
        new Thread(new Runnable() {
            @Override
            public void run() {
                link(socket);
            }
        }).start();
    }

    private ConnectListener connectListener;

    public interface ConnectListener {
        void connectting(boolean isConnect);

        void connectError(String msg);

        void closeError(String msg);
    }

    public static Handler btHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            CreateSocketListener listener = instance.createSocketListener;
            MatchDeviceListener matchDeviceListener = instance.matchDeviceListener;
            ConnectListener connectListener = instance.connectListener;
            int what = msg.what;
            switch (what) {
                //获取设备
                case HandlerArg.TO_GET_ADAPTER:
                    if (listener != null) {
                        listener.toGetAdapter();
                    }
                    break;
                case HandlerArg.TO_GET_REMOTE_DEVICE:
                    if (listener != null) {
                        listener.toGetRemoteDevice();
                    }
                    break;
                case HandlerArg.TO_GET_SOCKET:
                    if (listener != null) {
                        listener.toGetSocket();
                    }
                    break;
                case HandlerArg.GET_SOCKET_OK:
                    if (listener != null) {
                        listener.getSocketOk(mSocket);
                    }
                    break;
                case HandlerArg.GET_SOCKET_ERROR:
                    if (listener != null) {
                        listener.getSocketError(getSocketErrorMessage);
                    }
                    break;

                //配对
                case HandlerArg.MATCH_DEVICE_ERROR:
                    if (matchDeviceListener != null) {
                        matchDeviceListener.matchError(matchDeviceErrorMsg);
                    }
                    break;
                case HandlerArg.MATCH_DEVICE_ING:
                    if (matchDeviceListener != null) {
                        matchDeviceListener.matching();
                    }
                    break;
                case HandlerArg.MATCH_DEVICE_OK:
                    if (matchDeviceListener != null) {
                        matchDeviceListener.matchOk();
                    }
                    break;

                //连接
                case HandlerArg.SOCKET_CONNECTTING:
                    if (connectListener != null) {
                        connectListener.connectting(mSocket.isConnected());
                    }
                    break;
                case HandlerArg.SOCKET_CONNECT_ERROR:
                    if (connectListener != null) {
                        connectListener.connectError(socketConnectErrorMsg);
                    }
                    break;
                case HandlerArg.SOCKET_CLOSE_ERROR:
                    if (connectListener != null) {
                        connectListener.closeError(socketCloseErrorMsg);
                    }
                    break;

            }
        }
    };

    public interface ActionListener<T> {
        void onSuccess(T t);

        void onFail();
    }


    public void onDestroy() {
        if (btHandler != null) {
            btHandler = null;
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.disable();
        }
    }

}
