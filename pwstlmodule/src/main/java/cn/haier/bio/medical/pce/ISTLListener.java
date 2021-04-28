package cn.haier.bio.medical.pce;

public interface ISTLListener {
    void onSTLReady();
    void onSTLConnected();
    void onSTLPrint(String message);
    void onSTLException(Throwable throwable);
    void onSTLPackageReceived(byte[] data);
    void onSTLResponseReceived(byte[] data);
}
