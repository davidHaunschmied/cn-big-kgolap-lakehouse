package at.jku.dke.bigkgolap.engines.aixm;

import at.jku.dke.bigkgolap.api.engines.Engine;
import at.jku.dke.bigkgolap.engines.aixm.analyzer.AixmAnalyzer;
import at.jku.dke.bigkgolap.engines.aixm.mapper.AixmMapper;

public class AixmEngine implements Engine<AixmAnalyzer, AixmMapper> {

    private final AixmAnalyzer analyzer;
    private final AixmMapper mapper;

    public AixmEngine() {
        // Default constructor necessary for service loader
        this.analyzer = new AixmAnalyzer();
        this.mapper = new AixmMapper();
    }

    @Override
    public String getUniqueId() {
        return "AIXM";
    }

    @Override
    public AixmAnalyzer getAnalyzer() {
        return analyzer;
    }

    @Override
    public AixmMapper getMapper() {
        return mapper;
    }
}
