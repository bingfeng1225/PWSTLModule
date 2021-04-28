package cn.haier.bio.medical.pce;


import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class STLTools {

    public static final byte[] TAILER = {(byte) 0x7E};
    public static final byte[] HEADER = {(byte) 0xE7};

    public static boolean checkHeader(byte[] header) {
        return Arrays.equals(HEADER, header);
    }

    public static boolean checkTailer(byte[] tailer) {
        return Arrays.equals(TAILER, tailer);
    }

    public static boolean checkCommand(int command) {
        if (command == 0xC0) {
            return true;
        }
        if (command == 0xC1) {
            return true;
        }
        return false;
    }

    public static byte[] generateControlCommand(boolean open) {
        ByteBuf buffer = Unpooled.buffer(4);
        buffer.writeBytes(HEADER);
        buffer.writeByte(0xC2);
        buffer.writeByte(0x05);
        buffer.writeByte(open ? 0x01 : 0x00);
        buffer.writeBytes(TAILER);
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data, 0, data.length);
        buffer.release();
        return data;
    }

    public static byte[] generateParameterCommand(int dutyCycle, int frequency, int temperature) {
        ByteBuf buffer = Unpooled.buffer(4);
        buffer.writeBytes(HEADER);
        buffer.writeByte(0xC3);
        buffer.writeByte(0x08);

        buffer.writeByte(dutyCycle);
        buffer.writeByte(frequency);
        buffer.writeByte(temperature);

        byte[] checks = new byte[buffer.readableBytes()];
        buffer.getBytes(0, checks);
        buffer.writeByte(computeL8SumCode(checks, 3, 3));
        buffer.writeBytes(TAILER);

        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data, 0, data.length);
        buffer.release();
        return data;
    }

    public static String bytes2HexString(byte[] data, boolean hexFlag, String separator) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        return bytes2HexString(data, 0, data.length, hexFlag, separator);
    }

    public static String bytes2HexString(byte[] data, int offset, int len, boolean hexFlag, String separator) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        if (offset < 0 || offset > data.length - 1) {
            throw new IllegalArgumentException("The offset index out of bounds");
        }
        if (len < 0 || offset + len > data.length) {
            throw new IllegalArgumentException("The len can not be < 0 or (offset + len) index out of bounds");
        }
        String format = "%02X";
        if (hexFlag) {
            format = "0x%02X";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = offset; i < offset + len; i++) {
            buffer.append(String.format(format, data[i]));
            if (separator == null) {
                continue;
            }
            if (i != (offset + len - 1)) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }

    public static byte computeL8SumCode(byte[] data, int offset, int len) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        if (offset < 0 || offset > data.length - 1) {
            throw new IllegalArgumentException("The offset index out of bounds");
        }
        if (len < 0 || offset + len > data.length) {
            throw new IllegalArgumentException("The len can not be < 0 or (offset + len) index out of bounds");
        }
        int sum = 0;
        for (int pos = offset; pos < offset + len; pos++) {
            sum += data[pos];
        }
        return (byte) sum;
    }

    public static int indexOf(ByteBuf haystack, byte[] needle) {
        //遍历haystack的每一个字节
        for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
            int needleIndex;
            int haystackIndex = i;
            /*haystack是否出现了delimiter，注意delimiter是一个ChannelBuffer（byte[]）
            例如对于haystack="ABC\r\nDEF"，needle="\r\n"
            那么当haystackIndex=3时，找到了“\r”，此时needleIndex=0
            继续执行循环，haystackIndex++，needleIndex++，
            找到了“\n”
            至此，整个needle都匹配到了
            程序然后执行到if (needleIndex == needle.capacity())，返回结果
            */
            for (needleIndex = 0; needleIndex < needle.length; needleIndex++) {
                if (haystack.getByte(haystackIndex) != needle[needleIndex]) {
                    break;
                } else {
                    haystackIndex++;
                    if (haystackIndex == haystack.writerIndex() && needleIndex != needle.length - 1) {
                        return -1;
                    }
                }
            }

            if (needleIndex == needle.length) {
                // Found the needle from the haystack!
                return i - haystack.readerIndex();
            }
        }
        return -1;
    }
}
