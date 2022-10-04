package at.jku.dke.bigkgolap.api.engines;

import java.io.InputStream;

public interface Analyzer {
    AnalyzerResult analyze(InputStream fileData);
}
