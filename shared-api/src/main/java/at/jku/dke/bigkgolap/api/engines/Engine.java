package at.jku.dke.bigkgolap.api.engines;

public interface Engine<A extends Analyzer, M extends Mapper> {
    String getUniqueId();

    A getAnalyzer();

    M getMapper();
}
