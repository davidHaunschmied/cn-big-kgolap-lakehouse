package at.jku.dke.bigkgolap.shared.service;

import java.io.IOException;
import java.io.Writer;

public class ReuseableStringWriter extends Writer {

    private StringBuffer buf;

    public ReuseableStringWriter() {
        reset();
    }

    public void reset() {
        buf = new StringBuffer();
    }

    @Override
    public void write(String str) throws IOException {
        buf.append(str);
    }

    @Override
    public String toString() {
        return buf.toString();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        buf.append(cbuf, off, len);
    }

    @Override
    public void write(int c) throws IOException {
        buf.append((char) c);
    }

    @Override
    public void flush() throws IOException {
        // nothing special required here
    }

    @Override
    public void close() throws IOException {
        // nothing special required here
    }

}
