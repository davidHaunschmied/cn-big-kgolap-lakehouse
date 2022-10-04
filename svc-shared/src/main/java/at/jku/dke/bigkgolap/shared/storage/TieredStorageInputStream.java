package at.jku.dke.bigkgolap.shared.storage;

import java.io.IOException;
import java.io.InputStream;

public class TieredStorageInputStream extends InputStream {
    private final InputStream inputStream;
    private final boolean cachedLocally;

    public TieredStorageInputStream(InputStream inputStream, boolean cachedLocally) {
        this.inputStream = inputStream;
        this.cachedLocally = cachedLocally;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    public boolean isCachedLocally() {
        return cachedLocally;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
