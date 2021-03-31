package cn.haier.bio.medical.pce;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.qd.peiwen.serialport.PWSerialPortHelper;
import cn.qd.peiwen.serialport.PWSerialPortListener;
import cn.qd.peiwen.serialport.PWSerialPortState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class STLSerialPort implements PWSerialPortListener {
    private ByteBuf buffer;
    private STLHandler handler;
    private HandlerThread thread;
    private PWSerialPortHelper helper;

    private boolean ready = false;
    private boolean enabled = false;
    private WeakReference<ISTLListener> listener;

    public STLSerialPort() {

    }

    public void init(String path) {
        this.createHandler();
        this.createHelper(path);
        this.createBuffer();
    }

    public void enable() {
        if (this.isInitialized() && !this.enabled) {
            this.enabled = true;
            this.helper.open();
        }
    }

    public void disable() {
        if (this.isInitialized() && this.enabled) {
            this.enabled = false;
            this.helper.close();
        }
    }

    public void release() {
        this.listener = null;
        this.destoryHandler();
        this.destoryHelper();
        this.destoryBuffer();
    }

    public void changeListener(ISTLListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    private boolean isInitialized() {
        if (this.handler == null) {
            return false;
        }
        if (this.helper == null) {
            return false;
        }
        return this.buffer != null;
    }

    private void createHelper(String path) {
        if (this.helper == null) {
            this.helper = new PWSerialPortHelper("STLSerialPort");
            this.helper.setTimeout(10);
            this.helper.setPath(path);
            this.helper.setBaudrate(38400);
            this.helper.init(this);
        }
    }

    private void destoryHelper() {
        if (null != this.helper) {
            this.helper.release();
            this.helper = null;
        }
    }

    private void createHandler() {
        if (this.thread == null && this.handler == null) {
            this.thread = new HandlerThread("STLSerialPort");
            this.thread.start();
            this.handler = new STLHandler(this.thread.getLooper());
        }
    }

    private void destoryHandler() {
        if (null != this.thread) {
            this.thread.quitSafely();
            this.thread = null;
            this.handler = null;
        }
    }

    private void createBuffer() {
        if (this.buffer == null) {
            this.buffer = Unpooled.buffer(4);
        }
    }

    private void destoryBuffer() {
        if (null != this.buffer) {
            this.buffer.release();
            this.buffer = null;
        }
    }

    private void write(byte[] data) {
        if (!this.isInitialized() || !this.enabled) {
            return;
        }
        this.helper.writeAndFlush(data);
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSTLPrint("STLSerialPort Send:" + STLTools.bytes2HexString(data, true, ", "));
        }
    }

    private boolean ignorePackage() {
        int index = STLTools.indexOf(this.buffer, STLTools.HEADER);
        if (index != -1) {
            byte[] data = new byte[index];
            this.buffer.readBytes(data, 0, data.length);
            this.buffer.discardReadBytes();
            if (null != this.listener && null != this.listener.get()) {
                this.listener.get().onSTLPrint("STLSerialPort 指令丢弃:" + STLTools.bytes2HexString(data, true, ", "));
            }
            return this.processBytesBuffer();
        }
        return false;
    }

    private boolean processBytesBuffer() {
        if (this.buffer.readableBytes() < 4) {
            return true;
        }
        byte[] header = new byte[STLTools.HEADER.length];
        this.buffer.getBytes(0, header);
        if (!STLTools.checkHeader(header)) {
            return this.ignorePackage();
        }
        int index = STLTools.indexOf(this.buffer, STLTools.TAILER);
        if (index == -1) {
            return true;
        }

        this.buffer.markReaderIndex();
        byte[] data = new byte[index + STLTools.TAILER.length];
        this.buffer.readBytes(data, 0, data.length);

        if (!STLTools.checkFrame(data)) {
            this.buffer.resetReaderIndex();
            //当前包不合法 丢掉正常的包头以免重复判断
            this.buffer.skipBytes(STLTools.HEADER.length);
            this.buffer.discardReadBytes();
            return this.ignorePackage();
        }
        this.buffer.discardReadBytes();
        if (!this.ready) {
            this.ready = true;
            if (null != this.listener && null != this.listener.get()) {
                this.listener.get().onSTLReady();
            }
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSTLPrint("STLSerialPort Recv:" + STLTools.bytes2HexString(data, true, ", "));
        }
        Message msg = Message.obtain();
        msg.obj = data;
        this.handler.sendMessage(msg);
        return true;
    }

    private void processPackageReceived(byte[] data) {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSTLPackageReceived(data);
        }
    }

    @Override
    public void onConnected(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.ready = false;
        this.buffer.clear();
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSTLConnected();
        }
    }

    @Override
    public void onReadThreadReleased(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSTLPrint("STLSerialPort read thread released");
        }
    }

    @Override
    public void onException(PWSerialPortHelper helper, Throwable throwable) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.ready = false;
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSTLException(throwable);
        }
    }

    @Override
    public void onStateChanged(PWSerialPortHelper helper, PWSerialPortState state) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onSTLPrint("STLSerialPort state changed: " + state.name());
        }
    }

    @Override
    public boolean onByteReceived(PWSerialPortHelper helper, byte[] buffer, int length) throws IOException {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return false;
        }
        this.buffer.writeBytes(buffer, 0, length);
        return this.processBytesBuffer();
    }

    private class STLHandler extends Handler {
        public STLHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            STLSerialPort.this.processPackageReceived((byte[]) msg.obj);
        }
    }
}
