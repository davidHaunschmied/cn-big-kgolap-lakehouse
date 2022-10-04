package at.jku.dke.bigkgolap.engines.iwxxm;

import at.jku.dke.bigkgolap.api.engines.Engine;
import at.jku.dke.bigkgolap.engines.iwxxm.analyzer.IwxxmAnalyzer;
import at.jku.dke.bigkgolap.engines.iwxxm.mapper.IwxxmMapper;

public class IwxxmEngine implements Engine<IwxxmAnalyzer, IwxxmMapper> {

    public IwxxmEngine() {
        // Default constructor necessary for service loader
    }

    @Override
    public String getUniqueId() {
        return "IWXXM";
    }

    @Override
    public IwxxmAnalyzer getAnalyzer() {
        return new IwxxmAnalyzer();
    }

    @Override
    public IwxxmMapper getMapper() {
        return new IwxxmMapper();
    }
}
