package cn.haier.bio.medical.hstl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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
        manager.init("/dev/ttyS3");
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
    public byte[] onSTLPackageReceived(byte[] data) {
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
        return new byte[0];
    }
}
