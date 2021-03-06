/*
 ** 2013 December 5
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.io.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.*;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ByteBuffer utility class.
 * 
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class ByteBufferUtils {
    
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);
    private static final int DIRECT_THRESHOLD = 10240; // 10 KB

    private ByteBufferUtils() {
    }
    
    private static int getLength(FileChannel fc, int length) throws IOException {
        return (int) Math.min(length > 0 ? length : fc.size(), Integer.MAX_VALUE);
    }
    
    private static ByteBuffer allocate(int size) {
        // allocateDirect is pretty slow when used frequently, use it for larger
        // buffers only
        if (size > DIRECT_THRESHOLD) {
            return ByteBuffer.allocateDirect(size);
        } else {
            try {
                return ByteBuffer.allocate(size);
            } catch (OutOfMemoryError ex) {
                // not enough space in the heap, try direct allocation instead
                return ByteBuffer.allocateDirect(size);
            }
        }
    }
    
    /**
     * Reads a file and puts its content into a byte buffer, using the given length
     * and offset.
     * 
     * @param path
     * @param offset
     * @param length
     * @return
     * @throws IOException 
     */
    public static ByteBuffer load(Path path, int offset, int length) throws IOException {
        try (FileChannel fc = FileChannel.open(path, READ)) {
            ByteBuffer bb = allocate(getLength(fc, length));
            fc.position(offset);
            fc.read(bb);
            bb.flip();
            return bb;
        }
    }

    /**
     * Reads a file and puts its whole content into a byte buffer. If the file is
     * larger than {@link java.lang.Integer#MAX_VALUE}, then all bytes beyond
     * that limit are omitted.
     * 
     * @param path
     * @return
     * @throws IOException 
     */
    public static ByteBuffer load(Path path) throws IOException {
        return load(path, 0, -1);
    }
    
    /**
     * Reads a list of files and puts their contents into a byte buffer. The
     * combined size of all files must not exceed {@link java.lang.Integer#MAX_VALUE}.
     * 
     * @param paths
     * @return
     * @throws IOException 
     */
    public static ByteBuffer load(List<Path> paths) throws IOException {
        long size = 0;
        Map<Path, FileChannel> channelMap = new HashMap<>();
        
        try {
            for (Path path : paths) {
                FileChannel fc = FileChannel.open(path, READ);
                size += fc.size();
                channelMap.put(path, fc);
            }

            if (size > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Files are too large to load");
            }

            ByteBuffer bb = allocate((int) size);

            for (Path path : paths) {
                channelMap.get(path).read(bb);
            }

            bb.flip();

            return bb;
        } finally {
            for (FileChannel fc : channelMap.values()) {
                fc.close();
            }
        }
    }
    
    /**
     * Writes the remaining bytes of a byte buffer to the given file. 
     * If the file already exists, it will be overwritten and its size will be 
     * truncated to the amount of remaining bytes in the given buffer.
     * 
     * @param path
     * @param bb
     * @throws IOException 
     */
    public static void save(Path path, ByteBuffer bb) throws IOException {
        try (FileChannel fc = FileChannel.open(path, WRITE, CREATE)) {
            fc.truncate(bb.remaining());
            fc.write(bb);
        }
    }
    
    /**
     * Maps a portion of a file to memory and returns the mapped read-only byte
     * buffer using the given offset and length.
     * 
     * @param path
     * @param offset
     * @param length
     * @return
     * @throws IOException 
     */
    public static MappedByteBuffer openReadOnly(Path path, int offset, int length) throws IOException {
        try (FileChannel fc = FileChannel.open(path, READ)) {
            return fc.map(READ_ONLY, offset, getLength(fc, length));
        }
    }
    
    /**
     * Maps the whole file to memory and returns the mapped read-only byte buffer.
     * If the file is larger than {@link java.lang.Integer#MAX_VALUE}, then all
     * bytes beyond that limit are omitted.
     * 
     * @param path
     * @return
     * @throws IOException 
     */
    public static MappedByteBuffer openReadOnly(Path path) throws IOException {
        return openReadOnly(path, 0, -1);
    }
    
    /**
     * Maps a portion of a file to memory and returns the mapped writable byte
     * buffer using the given offset and length. If the file doesn't exist, it
     * will be created.
     * 
     * @param path
     * @param offset
     * @param length
     * @return
     * @throws IOException 
     */
    public static MappedByteBuffer openReadWrite(Path path, int offset, int length) throws IOException {
        try (FileChannel fc = FileChannel.open(path, READ, WRITE, CREATE)) {
            return fc.map(READ_WRITE, offset, getLength(fc, length));
        }
    }
    
    /**
     * Maps the whole file to memory and returns the mapped writable byte buffer.
     * If the file is larger than {@link java.lang.Integer#MAX_VALUE}, then all
     * bytes beyond that limit are omitted. If the file doesn't exist, it will be
     * created.
     * 
     * @param path
     * @return
     * @throws IOException 
     */
    public static MappedByteBuffer openReadWrite(Path path) throws IOException {
        return openReadWrite(path, 0, -1);
    }
    
    /**
     * Returns the subset of a byte buffer, using the given offset and length.
     * The position and limit of the original buffer won't change after this
     * operation.
     * 
     * @param bb
     * @param offset
     * @param length
     * @return 
     */
    public static ByteBuffer getSlice(ByteBuffer bb, int offset, int length) {
        if (length == 0) {
            // very funny
            return EMPTY;
        }
        
        ByteOrder order = bb.order();
        
        // create duplicate so the position/limit of the original won't change
        bb = bb.duplicate();
        
        // go to offset
        bb.position(offset);
        
        // set new limit if length is provided, use current limit otherwise
        if (length > 0) {
            bb.limit(offset + length);
        }
        
        // do the actual slicing
        ByteBuffer bbSlice = bb.slice();
        
        // set same byte order
        bbSlice.order(order);
        
        return bbSlice;
    }
    
    /**
     * Returns the subset of a byte buffer, starting from the given offset up to
     * the current limit.
     * The position and limit of the original buffer won't change after this
     * operation.
     * 
     * @param bb
     * @param offset
     * @return 
     */
    public static ByteBuffer getSlice(ByteBuffer bb, int offset) {
        return getSlice(bb, offset, -1);
    }
    
    /**
     * Concatenates one or more byte buffers to one large buffer. The combined
     * size of all buffers must not exceed {@link java.lang.Integer#MAX_VALUE}.
     * 
     * @param bbs
     * @return 
     */
    public static ByteBuffer concat(List<ByteBuffer> bbs) {
        long length = 0;
        
        // get amount of remaining bytes from all buffers
        for (ByteBuffer bb : bbs) {
            bb.rewind();
            length += bb.remaining();
        }
        
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Buffers are too large for concatenation");
        }
        
        if (length == 0) {
            // very funny
            return EMPTY;
        }
        
        ByteBuffer bbNew = ByteBuffer.allocateDirect((int) length);
        
        // put all buffers from list
        for (ByteBuffer bb : bbs) {
            bb.rewind();
            bbNew.put(bb);
        }
        
        bbNew.rewind();
        
        return bbNew;
    }
}
