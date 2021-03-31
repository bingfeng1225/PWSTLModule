package cn.haier.bio.medical.pce;

public interface ISTLListener {
    void onSTLConnected();
    void onSTLPrint(String message);
    void onSTLException(Throwable throwable);
    byte[] onSTLPackageReceived(byte[] data);
}
