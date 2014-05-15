package com.jasperb.citybuilder.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * An interface to extend the FileInputStream/FileOutputStream with functions for writing/reading primitives other than bytes
 */
public class FileStreamUtils {
    private FileOutputStream mOutputStream = null;
    private FileInputStream mInputStream = null;

    public FileStreamUtils(FileOutputStream outstream) {
        mOutputStream = outstream;
    }

    public FileStreamUtils(FileInputStream instream) {
        mInputStream = instream;
    }

    public void write(byte[] buffer) throws IOException {
        mOutputStream.write(buffer);
    }

    public void write(byte oneByte) throws IOException {
        mOutputStream.write(oneByte);
    }

    public void write(short twoBytes) throws IOException {
        mOutputStream.write(twoBytes >> 8);
        mOutputStream.write(twoBytes);
    }

    public void write(int fourBytes) throws IOException {
        mOutputStream.write(fourBytes >> 24);
        mOutputStream.write(fourBytes >> 16);
        mOutputStream.write(fourBytes >> 8);
        mOutputStream.write(fourBytes);
    }

    public void write(boolean oneByte) throws IOException {
        mOutputStream.write(oneByte ? 1 : 0);
    }

    public void flush() throws IOException {
        mOutputStream.flush();
    }

    public void readBytes(byte[] buffer, int byteCount) throws IOException {
        mInputStream.read(buffer, 0, byteCount);
    }

    public byte readByte() throws IOException {
        return (byte) mInputStream.read();
    }

    public short readShort() throws IOException {
        short val = (short) (mInputStream.read() << 8);
        val = (short) (val | mInputStream.read());
        return val;
    }

    public int readInt() throws IOException {
        int val = mInputStream.read() << 24;
        val = val | (mInputStream.read() << 16);
        val = val | (mInputStream.read() << 8);
        val = val | mInputStream.read();
        return val;
    }

    public boolean readBoolean() throws IOException {
        return mInputStream.read() == 0 ? false : true;
    }

    public void close() throws IOException {
        if (mOutputStream != null)
            mOutputStream.close();
        if (mInputStream != null)
            mInputStream.close();
    }
}
