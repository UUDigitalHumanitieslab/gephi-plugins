package edu.dhlabuu.fieldnotes;

import org.gephi.io.exporter.api.FileType;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.exporter.spi.GraphFileExporterBuilder;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builder for Export to Earth plugin.
 *
 * @author Jelmer van Nuss
 */
@ServiceProvider(service = GraphFileExporterBuilder.class)
public class SettingsExporterBuilder implements GraphFileExporterBuilder {

    @Override
    public GraphExporter buildExporter() {
        return new SettingsExporter();
    }

    @Override
    public FileType[] getFileTypes() {
        return new FileType[]{new FileType(".settings", NbBundle.getMessage(SettingsExporterBuilder.class, "SettingsFile"))};
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(SettingsExporterBuilder.class, "SettingsFileExport");
    }
}
