package cn.haier.bio.medical.hstl;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import cn.haier.bio.medical.pce.ISTLListener;
import cn.haier.bio.medical.pce.STLManager;
import cn.qd.peiwen.logger.PWLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MainActivity extends AppCompatActivity implements ISTLListener {
    private STLManager manager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = STLManager.getInstance();
        String path = "/dev/ttyS3"; //智能电子串口地址
        if ("magton".equals(Build.MODEL)) {
            path = "/dev/ttyS7";    //沃特电子串口地址
        }
        manager.init(path);
        manager.changeListener(this);
        manager.enable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.disable();
        manager.release();
    }

    @Override
    public void onSTLReady() {

    }

    @Override
    public void onSTLConnected() {

    }

    @Override
    public void onSTLPrint(String message) {
        PWLogger.error("" + message);
    }

    @Override
    public void onSTLException(Throwable throwable) {
        PWLogger.error(throwable);
    }

    @Override
    public void onSTLPackageReceived(byte[] data) {
        ByteBuf buffer = Unpooled.buffer(4);
        buffer.writeBytes(data);
        buffer.skipBytes(1);

        float v = 0.546f * buffer.readShort();
        float a = 0.055f * (buffer.readShort() - 510);
        float lt = 0.1f * buffer.readShort();
        float rt = 1.0f * buffer.readShort();
        int sum = 0xFF & buffer.readByte();
        byte alarm = buffer.readByte();

        buffer.release();
    }

    @Override
    public void onSTLResponseReceived(byte[] data) {

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                manager.openMachine();
                break;
            case R.id.button2:
                manager.closeMachine();
                break;
            case R.id.button3:
                manager.changeParameter(90,60,187);
                break;
        }
    }
}
