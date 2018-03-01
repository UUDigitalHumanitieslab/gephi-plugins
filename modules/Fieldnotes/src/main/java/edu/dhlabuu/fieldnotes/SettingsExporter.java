package edu.dhlabuu.fieldnotes;

import java.awt.Color;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.FilterModel;
import org.gephi.filters.api.Query;
import org.gephi.layout.api.LayoutController;
import org.gephi.layout.api.LayoutModel;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutProperty;
import org.gephi.appearance.api.*;
import org.gephi.appearance.spi.*;
import org.gephi.statistics.api.*;
import org.gephi.preview.api.*;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.spi.ByteExporter;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.project.api.Workspace;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * Exports Gephi graphs to Settings files.
 *
 * @author Jelmer van Nuss
 */
public class SettingsExporter implements GraphExporter, ByteExporter, LongTask
{

    private Logger logger = Logger.getLogger("");
    private boolean exportVisible;
    private Workspace workspace;
    private boolean cancelled;
    private ProgressTicket ticket;
    private OutputStream outputStream;

    private String getMessage(String resourceName) {
        return NbBundle.getMessage(SettingsExporter.class, resourceName);
    }

    @Override
    public void setExportVisible(boolean bln) {
        exportVisible = bln;
    }

    @Override
    public boolean isExportVisible() {
        return exportVisible;
    }

    @Override
    public boolean execute()
    {
        ticket.setDisplayName(getMessage("EvaluatingGraph"));
        Progress.start(ticket);
        
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        GraphModel model = graphController.getGraphModel(workspace);
        Graph graph;

        if (exportVisible)
        {
            graph = model.getGraphVisible();
        } else
        {
            graph = model.getGraph();
        }
        graph.readLock();

        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        FilterModel filterModel = filterController.getModel();
        LayoutController layoutController = Lookup.getDefault().lookup(LayoutController.class);
        LayoutModel layoutModel = layoutController.getModel();
        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();
        StatisticsController statisticsController = Lookup.getDefault().lookup(StatisticsController.class);
        StatisticsModel statisticsModel = statisticsController.getModel();
        PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();

        final Map<String, String> settings = new LinkedHashMap<String, String>();
        final Map<String, String> filterSettings = new LinkedHashMap<String, String>();
        final Map<String, String> layoutSettings = new LinkedHashMap<String, String>();
        final Map<String, String> appearanceSettings = new LinkedHashMap<String, String>();
        final Map<String, String> statisticsSettings = new LinkedHashMap<String, String>();
        final Map<String, String> previewSettings = new LinkedHashMap<String, String>();

        LinkedHashMap<String, Map<String, String>> settingsList = new LinkedHashMap<String, Map<String, String>>();
        settingsList.put("General settings", settings);
        settingsList.put("Filter settings", filterSettings);
        settingsList.put("Layout settings", layoutSettings);
        settingsList.put("Appearance settings", appearanceSettings);
        settingsList.put("Statistics settings", statisticsSettings);
        settingsList.put("Preview settings", previewSettings);

        Progress.setDisplayName(ticket, getMessage("WritingSettingsFile"));
        try
        {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            settings.put("isDirectedGraph", String.valueOf(graph.isDirected()));
            settings.put("timeZone: ", String.valueOf(graph.getModel().getTimeZone()));
            settings.put("edgeCount: ", String.valueOf(graph.getEdgeCount()));
            settings.put("nodeCount: ", String.valueOf(graph.getNodeCount()));
            settings.put("attributeKeys: ", String.valueOf(graph.getAttributeKeys()));

            addFiltersToSettings(filterSettings, filterModel);
            addLayoutToSettings(layoutSettings, layoutModel);
            addAppearanceToSettings(appearanceSettings, appearanceModel, graph);
            addStatisticsToSettings(statisticsSettings, statisticsModel);
            addPreviewToSettings(previewSettings, previewModel);

            String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            String filepath = "settings_" + timeLog + ".txt";

            writeSettings(settingsList, filepath);
            JOptionPane.showMessageDialog(null, getMessage("ExportCompleteMessage"),
                getMessage("ExportCompleteTitle"), JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException ex)
        {
            Logger.getLogger(SettingsExporter.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, getMessage("ExportSaveErrorMessage"),
                    getMessage("ExportSaveErrorTitle"), JOptionPane.ERROR_MESSAGE
            );
        } finally
        {
            Progress.finish(ticket);
        }
        
        return true;
    }

    private void addFiltersToSettings(Map<String, String> settings, FilterModel filterModel)
    {
        Query[] queries = filterModel.getQueries();

        for (int i = 0; i < queries.length; i++)
        {
            Query query = queries[i];
            settings.put("filter " + String.valueOf(i), query.getName());

            if (query.getPropertiesCount() > 0)
            {
                for (int j = 0; j < query.getPropertiesCount(); j++)
                {
                    settings.put(query.getPropertyName(j), String.valueOf(query.getPropertyValue(j)));
                }
            }
        }
    }

    private void addLayoutToSettings(Map<String, String> settings, LayoutModel layoutModel)
    {
        Layout layout = layoutModel.getSelectedLayout();
        if (layout != null)
        {
            LayoutProperty[] layoutProperties = layout.getProperties();
            if (layoutProperties.length > 0)
            {
                for (int i = 0; i < layoutProperties.length; i++)
                {
                    LayoutProperty layoutProperty = layoutProperties[i];
                    String propertyName = "property0";
                    //String propertyName = layoutProperty.getProperty().getDisplayName();
                    String propertyValue = "0";
                    //layoutProperty.getProperty().getValue().toString()
                    //System.out.println(layoutProperty.getProperty().getValue().toString());
                    settings.put(propertyName, propertyValue);
                }
            }
        }
    }

    private void addAppearanceToSettings(Map<String, String> settings, AppearanceModel appearanceModel, Graph graph)
    {
        System.out.println(appearanceModel.getEdgeFunctions(graph)[0].getTransformer().toString());
    }

    private void addStatisticsToSettings(Map<String, String> settings, StatisticsModel statisticsModel)
    {
        String statisticsName = "statisticsModel";
        String statisticsValue = statisticsModel.toString();
        settings.put(statisticsName, statisticsValue);
    }

    private void addPreviewToSettings(Map<String, String> settings, PreviewModel previewModel)
    {
        PreviewProperties previewProperties = previewModel.getProperties();
        PreviewProperty[] previewPropertiesList = previewModel.getProperties().getProperties();
        if (previewPropertiesList.length > 0)
        {
            for (int j = 0; j < previewPropertiesList.length; j++)
            {
                PreviewProperty previewProperty = previewPropertiesList[j];
                String previewPropertyName = previewProperty.getName();
                String previewPropertyValue = previewProperties.getValue(previewPropertyName).toString();
                settings.put(previewPropertyName, previewPropertyValue);
            }
        }
    }

    private void writeSettings(LinkedHashMap<String, Map<String, String>> settingsList, String filepath) throws IOException
    {
        BufferedWriter writer = null;
        try {
            File file = new File(filepath);

            writer = new BufferedWriter(new FileWriter(file));


            for (Map.Entry<String, Map<String, String>> settingsEntry : settingsList.entrySet())
            {
                String settingsType = settingsEntry.getKey();
                Map<String, String> settings = settingsEntry.getValue();
                writer.write(settingsType);
                writer.newLine();

                for (Map.Entry<String, String> entry : settings.entrySet())
                {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    writer.write(key + ": " + value);
                    writer.newLine();
                }
            }

        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                writer.close();
            } catch (Exception e)
            {
            }
        }
    }

    @Override
    public void setWorkspace(Workspace wrkspc) {
        workspace = wrkspc;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public boolean cancel() {
        return cancelled = true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        ticket = pt;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        outputStream = out;
    }
}
