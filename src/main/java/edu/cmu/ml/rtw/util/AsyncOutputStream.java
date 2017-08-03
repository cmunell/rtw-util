package edu.cmu.ml.rtw.util;

import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

/** 
 * An OutputStream implementation that writes to another OutputStream from another thread and
 * through a buffer so that any nontrivial CPU load coming from the wrapped OutputStream happens in
 * its own thread.
 *
 * This is particularly useful to use in order to wrap a java.util.zip.GZIPOutputStream.  That way,
 * the gzipping process happens in a separate thread, and you don't have to pay that cost in wall
 * time provided you have multi-core cycles to spare.  Note that it is not necessary to wrap the
 * AsyncOutputStream nor the the stream that it wraps in a BufferedInputStream because
 * AsyncOutputStream already peforms buffering on both ends.
 *
 * This implementation is ripped from {@link AsyncInputStream} which is admitedly not necessarily an
 * especially good one, although it has been pretty good enough so far.
 */
public class AsyncOutputStream extends OutputStream {
    private Logger log = LogFactory.getLogger();

    /**
     * We use a ring buffer to isolate reading from writing, and this is one item ("block") in that
     * buffer
     */
    class IOBlock {
        /**
         * I'm leaving the block size at 4MB even though that seems like it ought to be
         * unnecessarily large.  With smaller block sizes, I routinely saw really weird systemwide
         * behavior.  For instance, perl scripts in processes completely separate from the JVM
         * blocking on "sleep" calls for a minute when they'd requested sleeping for 5 seconds.  CPU
         * tick accounting through /proc/stat also gets weird, sometimes showing a significant
         * number of missing ticks or of ticks spent in the kernel.  I have to wonder if the JVM
         * starts leaning on the kernel very hard as a consequence of the particular kind of
         * synchronization we're using here.
         *
         * Maybe 1MB would work, but I think I'd be happy leaving it at 4MB anyway for the extra
         * breathing room it gives.  Consider also that the original motivating use for this class
         * was to perform high-throughput sustained reads from gzipped files, and that we'd like at
         * minimum to crank out 120MB/s like a not-gzipped read from a dedicated hard drive would.
         * Even better would be to able to effortlessly read through a gzipped file at that rate and
         * crank out 500MB/s or something.  In that kind of context, a more "typical" 4k or 8k
         * buffer is silly.
         */
        final static int bufSize = 1024 * 1024 * 4;
        
        /**
         * The buffer
         */
        byte[] buf = new byte[bufSize];
        
        /**
         * How many bytes of buf are full of data ready to be read.
         *
         * A zero indicates that no more data is avilable for reading.  A negative value indicates
         * that the buffer has already been read and awaits refill.
         *
         * This is a little convoluted considering that an InputStream uses -1 to indicate
         * end-of-stream, but it makes the code easier to write in that we need only test for > 0 or
         * <= 0.
         */
        int curSize = -1;
    }

    /**
     * Ring buffer used to isolate reading from writing, composed of IOBlocks.
     *
     * As of this writing, we synchronize the entire rink buffer.  We would probably do better to
     * synchronize only the individual IOBlocks in question.  Maybe that would take a nice bite out
     * of the toll this class can take on the kernel / JVM.  But it works good enough for now.
     */
    class IOBuffer {
        /**
         * The ring buffer itself
         */
        protected List<IOBlock> blockList;

        /**
         * The position of the next block to be read from
         */
        protected int readPos = 0;

        /**
         * The position of the next block to be written to
         */
        protected int writePos = 0;

        /**
         * For debugging
         */
        protected void logState() {
            log.debug("readPos=" + readPos + ", writePos=" + writePos);
            for (int i = 0 ; i < blockList.size(); i++) {
                final IOBlock blk = blockList.get(i);
                log.debug(i + ": cursize=" + blk.curSize);
            }
        }

        /**
         * Constructor
         *
         * @param size The number of IOBlocks to have in the buffer
         */
        public IOBuffer(int size) {
            blockList = new ArrayList<IOBlock>();
            for(int i = 0; i < size; i++) {
                blockList.add(new IOBlock());
            }
        }

        /**
         * Return a reference to the next block that has data to be read
         *
         * If all available data has already been read, this blocks until more data is ready.
         *
         * Per the semantics of IOBlock, an IOBlock returned by this function with a size of 0
         * indicates an end-of-stream condition.
         *
         * When the block returned is done being read, it must be returned to the ring buffer's use
         * with doneReadingBlock.
         */
        public IOBlock getNextReadableBlock() throws InterruptedException {
            // The reader and the writer both start at zero with all block marked as ready
            // to be filled.  So, we must wait where we are until the current buffer is
            // marked as having something in it.
            synchronized(this) {
                while (blockList.get(readPos).curSize < 0) {
                    wait();
                }

                // I wouldn't think that this stuff needs synchronized, but I'm doing it anyway
                // because I am observing occasional threading-related issues that look like
                // deadlocking
                IOBlock blk = blockList.get(readPos);
                readPos++;
                if (readPos >= blockList.size()) readPos = 0;
                return blk;
            }
        }

        /**
         * Signal that a block returned by getNextReadableBlock is done being read and may be reused
         */
        public void doneReadingBlock(IOBlock block) {
            synchronized(this) {
                // Mark is ready to be written to and notify self in case writing to new blocks is
                // waiting.
                block.curSize = -1;
                this.notifyAll();
            }
        }

        /**
         * Return a reference to the next block that is ready to be written to
         *
         * If all blocks are already full of data that has yet to be read, then this blocks until
         * more blocks are ready to be reused.
         *
         * When the block returned is done being written to, it must be given back to the ring
         * buffer with doneWritingBlock
         */
        public IOBlock getNextWritableBlock() throws InterruptedException {
            // The reader and the writer both start at zero with all blocks marked as ready to be
            // filled.  So if the writer ever goes to the next block and finds it not ready, then it
            // must wait for the reading to get around to be done reading one of the blocks.  That
            // means hanging out in a loop waiting until the current block is marked as ready.
            synchronized(this) {
                while (blockList.get(writePos).curSize >= 0) {
                    wait();
                }

                // I wouldn't think that this stuff needs synchronized, but I'm doing it anyway
                // because I am observing occasional threading-related issues that look like
                // deadlocking
                IOBlock blk = blockList.get(writePos);
                writePos++;
                if (writePos >= blockList.size()) writePos = 0;
                return blk;
            }
        }
        
        /**
         * Indicate that a block returned by getNextWritableBlock is done being written to and ready
         * to be read.
         *
         * Per the semantics of IOBlock, a size of zero should be used to indicate an end-of-stream
         * condition.  Passing a negative size is not allowed.
         */
        public void doneWritingBlock(IOBlock block, int size) {
            if (size < 0) throw new RuntimeException("Size may not be < 0");
            synchronized(this) {
                // Mark as ready to be read and notify self in case reading from new blocks is
                // waiting.
                block.curSize = size;
                this.notifyAll();
            }
        }
    }

    /**
     * Thread that reads from our ring buffer and writes it to an OutputStream
     *
     * I don't know if this abort thing quite works properly.  We'll just have to use it and see how
     * it goes.
     */
    class OutputWriter implements Runnable {
        /**
         * Set this if this thread should abort 
         */
        protected boolean shouldAbort = false;

        public void run() {  //bkdb: convert this
            while (!shouldAbort) {
                try {
                    // Wait for another block to be free for reading
                    IOBlock block = buffer.getNextReadableBlock();
                    if (block.curSize == 0) {
                        // This is a signal to flush realOut and then terminate so the main thread
                        // knows that everything up to this block has been flushed.
                        realOut.flush();
                        break;
                    } else {
                        // Write it out and return it
                        realOut.write(block.buf, 0, block.curSize);
                        buffer.doneReadingBlock(block);
                    }
                } catch (InterruptedException e) {
                    // Only reason we get interrupted is if we should abort
                    synchronized(this) {
                        if (shouldAbort) break;
                        else throw new RuntimeException(e);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void abort() {
            synchronized(this) {
                shouldAbort = true;
            }
            writerThread.interrupt();  // Yeah, this is horrid
        }
    }

    protected OutputStream realOut;
    protected IOBuffer buffer;
    protected int curBlock = 0;
    protected OutputWriter ow = null;
    protected Thread writerThread = null;

    // These two hold the IOBlock most recently returned by the ring buffer as being ready for
    // writing.  We use it to satisfy read requests.
    protected IOBlock readyBlock = null;
    protected int readyPos = 0;

    /**
     * Shut down the writer thread if it is still running
     */
    protected void shutdownThread() {
        if (writerThread != null && ow != null) {
            if (writerThread.getState() != Thread.State.TERMINATED)
                ow.abort();
        }
        writerThread = null;
        ow = null;
    }

    /**
     * Constructor
     *
     * As of this writing, this uses the same sizing as for the current {@link AsyncInputStream}
     * implementation without any particular expectation for any testing or tuning based on how
     * generally acceptably AsyncInputStream seems to work.
     */
    public AsyncOutputStream(OutputStream realOut) {
        this.realOut = realOut;
        buffer = new IOBuffer(8);
        ow = new OutputWriter();
        writerThread = new Thread(ow, "AsyncOutputStream.OutputWriter");

        // What we could do to get clever here is to set a default exception handler for our thread
        // and then rethrow that exception from the next time our read is invoked.

        writerThread.start();
    }
    
    public void write(int b) throws java.io.IOException {
        // If readyBlock is null, then fetch another block from the buffer, waiting for another
        // one if the next one isn't available.
        if (readyBlock == null) {
            try {
                readyBlock = buffer.getNextWritableBlock();
            } catch (InterruptedException e) {
                throw new java.io.IOException(e.getMessage());
            }
            readyPos = 0;
        }

        // readyBlock is not null.  Add the next byte at readyPos, and if that causes it to be full
        // then it's full and ready to be sent off to the ring buffer.
        readyBlock.buf[readyPos++] = (byte)b;
        if (readyPos >= readyBlock.bufSize) {
            buffer.doneWritingBlock(readyBlock, readyPos);
            readyBlock = null;
        }
    }

    // Seems like implementing this ought to really bring down overhead, you know?
    public void write(byte[] b, int off, int len) throws java.io.IOException {
        int writeCnt = 0;
        while (writeCnt < len) {
            // If readyBlock is null, then fetch another block from the buffer, waiting for another
            // one if the next one isn't available.
            if (readyBlock == null) {
                try {
                    readyBlock = buffer.getNextWritableBlock();
                } catch (InterruptedException e) {
                    throw new java.io.IOException(e.getMessage());
                }
                readyPos = 0;
            }

            // readyBlock is not null.  Copy as much as we need / can to fulfil the write, looping
            // back up to get another block to write to if we can't fulfil the entire write.
            // request and loop back up if it wasn't enough.
            int left = readyBlock.bufSize - readyPos;
            int wanted = len - writeCnt;
            if (left < wanted) {
                System.arraycopy(b, off+writeCnt, readyBlock.buf, readyPos, left);
                readyPos += left;
                writeCnt += left;
                buffer.doneWritingBlock(readyBlock, readyPos);
                readyBlock = null;
            } else {
                System.arraycopy(b, off+writeCnt, readyBlock.buf, readyPos, wanted);
                readyPos += wanted;
                writeCnt += wanted;
                return;
            }
        }
    }

    public void flush() throws java.io.IOException {
        try {
            // If there's something in readyBlock, send it into the ring buffer to be written out
            if (readyBlock != null) {
                buffer.doneWritingBlock(readyBlock, readyPos);
                readyBlock = null;
            }

            // Send a zero-length block into the ring buffer to tell the writerThread to flush and
            // then terminate.
            readyBlock = buffer.getNextWritableBlock();
            buffer.doneWritingBlock(readyBlock, 0);

            // Wait for thread to terminate, indicating that everything in the ring buffer has
            // written and realOut has been flushed.
            writerThread.join();

            // Start a new writerThread so that we can support further writers.
            writerThread = new Thread(ow, "AsyncOutputStream.OutputWriter");
            writerThread.start();
        } catch (InterruptedException e) {
            throw new java.io.IOException(e.getMessage());
        }
    }
    
    public void close() throws java.io.IOException {
        // close is not required to wait for all pending data to be written, so just shut down
        // without attempting to flush or anything.
        shutdownThread();

        // I wonder if we should wait for the shutdown before closing realOut
        realOut.close();
        realOut = null;
        readyBlock = null;
        buffer = null;
    }
    
    protected void finalize() throws Throwable {
        shutdownThread();
    }
}

