package lee.vioson.buletoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        boolean b = BlueToothHelper.getInstace(this).checkBtIsValueble();
        Log.d(TAG, "该设备" + (b ? "有" : "没有") + "蓝牙功能");

        if (b) {
//        BlueToothHelper.getInstace(this).enableBT();
            BlueToothHelper.getInstace(this).openBT(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        BlueToothHelper.getInstace(this).onActivityResult(requestCode, resultCode, new BlueToothHelper.ActionListener() {
            @Override
            public void onSuccess(Object o) {
                Log.d(TAG, "打开蓝牙了");
                BlueToothHelper.getInstace(context).discoveryBT();
                BlueToothHelper.getInstace(context).registerScan(actionListener);
            }

            @Override
            public void onFail() {
                Log.d(TAG, "打开蓝牙失败");
            }
        });
    }

    private BlueToothHelper.ActionListener<BluetoothDevice> actionListener = new BlueToothHelper.ActionListener<BluetoothDevice>() {
        @Override
        public void onSuccess(BluetoothDevice device) {
            Log.d(TAG,device.getAddress());
            Toast.makeText(context, device.getAddress(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFail() {
            Log.d(TAG,"没有设备被找到");
        }
    };
}
