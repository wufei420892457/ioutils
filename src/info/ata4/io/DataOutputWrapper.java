/*
 ** 2013 Juli 10
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.io;

import info.ata4.io.socket.IOSocket;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.commons.io.EndianUtils;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DataOutputWrapper extends IOWrapper implements DataOutput {
    
    private final DataOutput out;
    protected boolean swap;

    public DataOutputWrapper(IOSocket socket) {
        super(socket);
        this.out = socket.getDataOutput();
    }
    
    @Override
    public boolean isSwap() {
        if (super.isSwappable()) {
            return super.isSwap();
        } else {
            return swap;
        }
    }

    @Override
    public void setSwap(boolean swap) {
        if (super.isSwappable()) {
            super.setSwap(swap);
        } else {
            this.swap = swap;
        }
    }

    @Override
    public boolean isSwappable() {
        // supports manual swapping using EndianUtils if required
        return true;
    }
    
    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        if (swap) {
            v = EndianUtils.swapShort((short) v);
        }
        out.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        if (swap) {
            v = EndianUtils.swapInteger(v);
        }
        out.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        if (swap) {
            v = EndianUtils.swapLong(v);
        }
        out.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        if (swap) {
            // NOTE: don't use writeFloat() plus EndianUtils.swapFloat() here!
            writeInt(Float.floatToRawIntBits(v));
        } else {
            out.writeFloat(v);
        }
    }

    @Override
    public void writeDouble(double v) throws IOException {
        if (swap) {
            // NOTE: don't use writeDouble() plus EndianUtils.swapDouble() here!
            writeLong(Double.doubleToRawLongBits(v));
        } else {
            out.writeDouble(v);
        }
    }

    @Override
    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        out.writeUTF(s);
    }
}
