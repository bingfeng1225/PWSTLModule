package cn.haier.bio.medical.pce;

/***
 * 超低温变频、T系列、双系统主控板通讯
 *
 */
public class STLManager {
    private STLSerialPort serialPort;
    private static STLManager manager;

    public static STLManager getInstance() {
        if (manager == null) {
            synchronized (STLManager.class) {
                if (manager == null)
                    manager = new STLManager();
            }
        }
        return manager;
    }

    private STLManager() {

    }

    public void init(String path) {
        if (this.serialPort == null) {
            this.serialPort = new STLSerialPort();
            this.serialPort.init(path);
        }
    }


    public void enable() {
        if (null != this.serialPort) {
            this.serialPort.enable();
        }
    }

    public void disable() {
        if (null != this.serialPort) {
            this.serialPort.disable();
        }
    }

    public void release() {
        if (null != this.serialPort) {
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    public void openMachine() {
        if (null != this.serialPort) {
            this.serialPort.openMachine();
        }
    }

    public void closeMachine() {
        if (null != this.serialPort) {
            this.serialPort.closeMachine();
        }
    }

    public void changeListener(ISTLListener listener) {
        if (null != this.serialPort) {
            this.serialPort.changeListener(listener);
        }
    }

    public void changeParameter(int dutyCycle, int frequency, int temperature) {
        if (null != this.serialPort) {
            this.serialPort.changeParameter(dutyCycle, frequency, temperature);
        }
    }
}

