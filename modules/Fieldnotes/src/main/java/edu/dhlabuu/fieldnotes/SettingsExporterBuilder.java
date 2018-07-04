package edu.dhlabuu.fieldnotes;

import org.gephi.io.exporter.api.FileType;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.exporter.spi.GraphFileExporterBuilder;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builder for Fieldnotes settings exporter plugin.
 *
 * @author Jelmer van Nuss, 2018, Digital Humanities Lab, Utrecht University
 */
@ServiceProvider(service = GraphFileExporterBuilder.class)
public class SettingsExporterBuilder implements GraphFileExporterBuilder {

    public GraphExporter buildExporter() {
        return new SettingsExporter();
    }

    public FileType[] getFileTypes() {
        return new FileType[]{new FileType(".txt", NbBundle.getMessage(SettingsExporterBuilder.class, "SettingsFile"))};
    }

    public String getName() {
        return NbBundle.getMessage(SettingsExporterBuilder.class, "SettingsFileExport");
    }
}
