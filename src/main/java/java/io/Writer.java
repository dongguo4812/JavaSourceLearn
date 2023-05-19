/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.io;


/**
 * Abstract class for writing to character streams.  The only methods that a
 * subclass must implement are write(char[], int, int), flush(), and close().
 * Most subclasses, however, will override some of the methods defined here in
 * order to provide higher efficiency, additional functionality, or both.
 *
 * @see Writer
 * @see   BufferedWriter
 * @see   CharArrayWriter
 * @see   FilterWriter
 * @see   OutputStreamWriter
 * @see     FileWriter
 * @see   PipedWriter
 * @see   PrintWriter
 * @see   StringWriter
 * @see Reader
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public abstract class Writer implements Appendable, Closeable, Flushable {

    /**
     * Temporary buffer used to hold writes of strings and single characters
     */
    //临时的字符数组保存写入的字符
    private char[] writeBuffer;

    /**
     * Size of writeBuffer, must be >= 1
     */
    // 临时字符数组的大小1024
    private static final int WRITE_BUFFER_SIZE = 1024;

    /**
     * The object used to synchronize operations on this stream.  For
     * efficiency, a character-stream object may use an object other than
     * itself to protect critical sections.  A subclass should therefore use
     * the object in this field rather than <tt>this</tt> or a synchronized
     * method.
     * 用于同步针对此流的操作的对象
     */
    protected Object lock;

    /**
     * Creates a new character-stream writer whose critical sections will
     * synchronize on the writer itself.
     */
    protected Writer() {
        this.lock = this;
    }

    /**
     * Creates a new character-stream writer whose critical sections will
     * synchronize on the given object.
     *
     * @param  lock
     *         Object to synchronize on
     */
    protected Writer(Object lock) {
        if (lock == null) {
            throw new NullPointerException();
        }
        this.lock = lock;
    }

    /**
     * Writes a single character.  The character to be written is contained in
     * the 16 low-order bits of the given integer value; the 16 high-order bits
     * are ignored.
     *
     * <p> Subclasses that intend to support efficient single-character output
     * should override this method.
     *
     * @param  c
     *         int specifying a character to be written
     *
     * @throws  IOException
     *          If an I/O error occurs
     *          写入单个字符。要写入的字符包含在给定整数值的 16 个低位中，16 高位被忽略
     */
    public void write(int c) throws IOException {
        synchronized (lock) {
            if (writeBuffer == null){
                writeBuffer = new char[WRITE_BUFFER_SIZE];
            }
            // 字符2个字节，int是4个字节丢弃高位的2个，保留低位2个存入临时字符数组
            writeBuffer[0] = (char) c;
            // 写进输出流1个字符
            write(writeBuffer, 0, 1);
        }
    }

    /**
     * Writes an array of characters.
     *
     * @param  cbuf
     *         Array of characters to be written
     *
     * @throws  IOException
     *          If an I/O error occurs
     *          字符数组 cbuf 中写入输出流
     */
    public void write(char cbuf[]) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    /**
     * Writes a portion of an array of characters.
     *
     * @param  cbuf
     *         Array of characters
     *
     * @param  off
     *         Offset from which to start writing characters
     *
     * @param  len
     *         Number of characters to write
     *
     * @throws  IOException
     *          If an I/O error occurs
     *          将字符数组 cbuf 中的字符写入输出流，从下标 off 开始，共 len 个字符
     */
    abstract public void write(char cbuf[], int off, int len) throws IOException;

    /**
     * Writes a string.
     *
     * @param  str
     *         String to be written
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public void write(String str) throws IOException {
        write(str, 0, str.length());
    }

    /**
     * Writes a portion of a string.
     *
     * @param  str
     *         A String
     *
     * @param  off
     *         Offset from which to start writing characters
     *
     * @param  len
     *         Number of characters to write
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>off</tt> is negative, or <tt>len</tt> is negative,
     *          or <tt>off+len</tt> is negative or greater than the length
     *          of the given string
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            char cbuf[];
            if (len <= WRITE_BUFFER_SIZE) {
                if (writeBuffer == null) {
                    writeBuffer = new char[WRITE_BUFFER_SIZE];
                }
                cbuf = writeBuffer;
            } else {    // Don't permanently allocate very large buffers.
                cbuf = new char[len];
            }
            str.getChars(off, (off + len), cbuf, 0);
            write(cbuf, 0, len);
        }
    }

    /**
     * Appends the specified character sequence to this writer.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.write(csq.toString()) </pre>
     *
     * <p> Depending on the specification of <tt>toString</tt> for the
     * character sequence <tt>csq</tt>, the entire sequence may not be
     * appended. For instance, invoking the <tt>toString</tt> method of a
     * character buffer will return a subsequence whose content depends upon
     * the buffer's position and limit.
     *
     * @param  csq
     *         The character sequence to append.  If <tt>csq</tt> is
     *         <tt>null</tt>, then the four characters <tt>"null"</tt> are
     *         appended to this writer.
     *
     * @return  This writer
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @since  1.5
     * 将指定字符序列添加到此 writer
     */
    public Writer append(CharSequence csq) throws IOException {
        if (csq == null)
            write("null");
        else
            write(csq.toString());
        return this;
    }

    /**
     * Appends a subsequence of the specified character sequence to this writer.
     * <tt>Appendable</tt>.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq, start,
     * end)</tt> when <tt>csq</tt> is not <tt>null</tt> behaves in exactly the
     * same way as the invocation
     *
     * <pre>
     *     out.write(csq.subSequence(start, end).toString()) </pre>
     *
     * @param  csq
     *         The character sequence from which a subsequence will be
     *         appended.  If <tt>csq</tt> is <tt>null</tt>, then characters
     *         will be appended as if <tt>csq</tt> contained the four
     *         characters <tt>"null"</tt>.
     *
     * @param  start
     *         The index of the first character in the subsequence
     *
     * @param  end
     *         The index of the character following the last character in the
     *         subsequence
     *
     * @return  This writer
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>start</tt> or <tt>end</tt> are negative, <tt>start</tt>
     *          is greater than <tt>end</tt>, or <tt>end</tt> is greater than
     *          <tt>csq.length()</tt>
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @since  1.5
     * 将指定字符序列添加到此 writer
     */
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        CharSequence cs = (csq == null ? "null" : csq);
        write(cs.subSequence(start, end).toString());
        return this;
    }

    /**
     * Appends the specified character to this writer.
     *
     * <p> An invocation of this method of the form <tt>out.append(c)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.write(c) </pre>
     *
     * @param  c
     *         The 16-bit character to append
     *
     * @return  This writer
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @since 1.5
     * 将指定字符添加到此 writer
     */
    public Writer append(char c) throws IOException {
        write(c);
        return this;
    }

    /**
     * Flushes the stream.  If the stream has saved any characters from the
     * various write() methods in a buffer, write them immediately to their
     * intended destination.  Then, if that destination is another character or
     * byte stream, flush it.  Thus one flush() invocation will flush all the
     * buffers in a chain of Writers and OutputStreams.
     *
     * <p> If the intended destination of this stream is an abstraction provided
     * by the underlying operating system, for example a file, then flushing the
     * stream guarantees only that bytes previously written to the stream are
     * passed to the operating system for writing; it does not guarantee that
     * they are actually written to a physical device such as a disk drive.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *          刷新该流的缓冲。如果该流已保存缓冲区中各种 write() 方法的所有字符，则立即将它们写入预期目标。
     *          然后，如果该目标是另一个字符或字节流，则将其刷新。因此，一次 flush() 调用将刷新 Writer 和 OutputStream 链中的所有缓冲区。
     */
    abstract public void flush() throws IOException;

    /**
     * Closes the stream, flushing it first. Once the stream has been closed,
     * further write() or flush() invocations will cause an IOException to be
     * thrown. Closing a previously closed stream has no effect.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *          关闭此流，但要先刷新它。在关闭该流之后，再调用 write() 或 flush() 将导致抛出 IOException。关闭以前关闭的流无效。
     */
    abstract public void close() throws IOException;

}
