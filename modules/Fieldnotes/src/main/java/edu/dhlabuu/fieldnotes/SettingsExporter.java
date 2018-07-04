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
import org.gephi.preview.types.*;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.*;
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
 * @author Jelmer van Nuss and Alex Hebing, 2018, Digital Humanities Lab, Utrecht University
 * 
 */
public class SettingsExporter implements GraphExporter, ByteExporter, LongTask {
    private boolean exportVisible;
    private Workspace workspace;
    private boolean cancelled;    
    // Used to set progressbar/message
    private ProgressTicket ticket;
    // This is file selected/created by the user
    private OutputStream outputStream;

    private String getMessage(String resourceName) {
        return NbBundle.getMessage(SettingsExporter.class, resourceName);
    }

    public void setExportVisible(boolean bln) {
        exportVisible = bln;
    }

    public boolean isExportVisible() {
        return exportVisible;
    }

    public boolean execute() {
        this.logMessage(Level.INFO, "Start");

        ticket.setDisplayName(getMessage("EvaluatingGraph"));
        Progress.start(ticket);

        this.logMessage(Level.INFO, "Loading controllers and settings");
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        GraphModel model = graphController.getGraphModel(workspace);
        Graph graph;

        if (exportVisible) {
            graph = model.getGraphVisible();
        } else {
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
        settingsList.put("## General settings", settings);
        settingsList.put("## Filter settings", filterSettings);
        settingsList.put("## Layout settings", layoutSettings);
        //settingsList.put("## Appearance settings", appearanceSettings);
        settingsList.put("## Statistics settings", statisticsSettings);
        settingsList.put("## Preview settings", previewSettings);

        Progress.setDisplayName(ticket, getMessage("WritingSettingsFile"));
       
        settings.put("isDirectedGraph", String.valueOf(graph.isDirected()));
        settings.put("edgeCount", String.valueOf(graph.getEdgeCount()));
        settings.put("nodeCount", String.valueOf(graph.getNodeCount()));

        addFiltersToSettings(filterSettings, filterModel);
        addLayoutToSettings(layoutSettings, layoutModel);
        //addAppearanceToSettings(appearanceSettings, appearanceModel, model, graph);
        addStatisticsToSettings(statisticsSettings, statisticsModel);
        addPreviewToSettings(previewSettings, previewModel);
        this.logMessage(Level.INFO, "Done loading controllers and settings. Starting export");

        // Export Settings (to two files)
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss").format(Calendar.getInstance().getTime());
        String fileExportedSettings = new File(System.getProperty("user.dir"), "settings_" + timeStamp + ".txt").getPath();
        File fileExportedGraph = new File(System.getProperty("user.dir"), "graph_" + timeStamp + ".gexf");

        // Export the full graph.
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            this.logMessage(Level.INFO, String.format("Starting export of graph to '%s'", fileExportedGraph.getPath()));
            ec.exportFile(new File("graph_" + timeStamp + ".gexf"));
            this.logMessage(Level.INFO, String.format("Succesfully exported to '%s'", fileExportedGraph.getPath()));
        } catch (IOException ex) {
            this.handleError(ex, getMessage("ExportSaveErrorTitle"), getMessage("ExportSaveErrorMessage"));
        }

        // Export the settings.
        this.logMessage(Level.INFO, String.format("Attempting to write settings to '%s'", fileExportedSettings));
        writeSettings(settingsList, fileExportedSettings);

        // Write message to outputstream (i.e. the file created by the user)
        this.writeSummaryToOutput(fileExportedGraph.getPath(), fileExportedSettings);
        
        // Make sure the graph is unlocked, otherwise Gephi will go unresponsive
        graph.readUnlock();
        Progress.finish(ticket);
        JOptionPane.showMessageDialog(null, getMessage("ExportCompleteMessage"),
                getMessage("ExportCompleteTitle"), JOptionPane.INFORMATION_MESSAGE);
        
        this.logMessage(Level.INFO, String.format("Succesfully wrote settings to '%s'", fileExportedSettings));
        this.logMessage(Level.INFO, "Done");

        return true;
    }

    private void addFiltersToSettings(Map<String, String> settings, FilterModel filterModel) {
        Query[] queries = filterModel.getQueries();

        for (int i = 0; i < queries.length; i++) {
            Query query = queries[i];
            settings.put("# filter " + String.valueOf(i), query.getName());

            if (query.getPropertiesCount() > 0) {
                for (int j = 0; j < query.getPropertiesCount(); j++) {
                    settings.put(query.getPropertyName(j), String.valueOf(query.getPropertyValue(j)));
                }
            }
        }
    }

    private void addLayoutToSettings(Map<String, String> settings, LayoutModel layoutModel) {
        // Layout is from .spi not .api, so cannot get to values?
        Layout layout = layoutModel.getSelectedLayout();
        if (layout != null) {
            settings.put("# layout", layout.getBuilder().getName());
            LayoutProperty[] layoutProperties = layout.getProperties();
            if (layoutProperties.length > 0) {
                for (int i = 0; i < layoutProperties.length; i++) {
                    LayoutProperty layoutProperty = layoutProperties[i];
                    // Canonical name is structured as "[layoutName].[propertyName].name", so take the second element.
                    String propertyName = layoutProperty.getCanonicalName().split("\\.")[1];
                    //String propertyName = layoutProperty.getProperty().getDisplayName();
                    String propertyValue = "Not extracted";
                    //System.out.println(layoutProperty.getProperty().getValue().toString());
                    settings.put(propertyName, propertyValue);
                }
            }
        }
    }

    private static Column[] getColumns(Table table) {
        Column[] columns = new Column[table.countColumns()];

        for (int i = 0; i < columns.length; i++) {
            columns[i] = table.getColumn(i);
        }

        return columns;
    }

    private void addAppearanceToSettings(Map<String, String> settings, AppearanceModel appearanceModel, GraphModel graphModel, Graph graph) {
        // These appearance settings should already be in the corresponding .gexf file.
        for (Node node : graph.getNodes())
        {
            String nodeColorLabel = String.format("%s color", node.getLabel());
            String nodeColor = String.format("rgb:%f,%f,%f", node.r(), node.g(), node.b());
            settings.put(nodeColorLabel, nodeColor);

            String nodeSizeLabel = String.format("%s size", node.getLabel());
            String nodeSize = String.valueOf(node.size());
            settings.put(nodeSizeLabel, nodeSize);
        }
    }

    private void addStatisticsToSettings(Map<String, String> settings, StatisticsModel statisticsModel) {
        String statisticsName = "statisticsModel";
        String statisticsValue = statisticsModel.toString();
        settings.put(statisticsName, statisticsValue);
    }

    private void addPreviewToSettings(Map<String, String> settings, PreviewModel previewModel) {
        PreviewProperties previewProperties = previewModel.getProperties();
        PreviewProperty[] previewPropertiesList = previewModel.getProperties().getProperties();
        if (previewPropertiesList.length > 0) {
            for (int j = 0; j < previewPropertiesList.length; j++) {
                PreviewProperty previewProperty = previewPropertiesList[j];
                String previewPropertyName = previewProperty.getName();

                // Correct a typo that exists in Gephi.
                if (previewPropertyName == "node.label.proportinalSize") {
                    previewPropertyName = "node.label.proportionalSize";
                }

                String previewPropertyValue = previewProperty.getValue().toString();
                if (previewProperty.getType() == DependantColor.class)
                {
                    DependantColor dependantColor = DependantColor.class.cast(previewProperty.getValue());
                    Color color = dependantColor.getCustomColor();
                    previewPropertyValue = color.toString();
                }
                else if (previewProperty.getType() == DependantOriginalColor.class)
                {
                    DependantOriginalColor dependantOriginalColor = DependantOriginalColor.class.cast(previewProperty.getValue());
                    Color color = dependantOriginalColor.getCustomColor();
                    previewPropertyValue = color.toString();
                }
                else if (previewProperty.getType() == EdgeColor.class)
                {
                    EdgeColor edgeColor = EdgeColor.class.cast(previewProperty.getValue());
                    Color color = edgeColor.getCustomColor();
                    previewPropertyValue = color.toString();
                }
                settings.put(previewPropertyName, previewPropertyValue);
            }
        }
    }

    private void writeSettings(LinkedHashMap<String, Map<String, String>> settingsList, String filepath) {
        BufferedWriter writer = null;
        try {
            File file = new File(filepath);

            writer = new BufferedWriter(new FileWriter(file));


            for (Map.Entry<String, Map<String, String>> settingsEntry : settingsList.entrySet()) {
                String settingsType = settingsEntry.getKey();
                Map<String, String> settings = settingsEntry.getValue();
                writer.write("------------------------------------------------------------------------------------------------------------------------");
                writer.newLine();
                writer.write(settingsType);
                writer.newLine();

                for (Map.Entry<String, String> entry : settings.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    writer.write(key + ": " + value);
                    writer.newLine();
                }
            }

        } catch (Exception ex) {
            this.handleError(ex, getMessage("ExportSaveErrorTitle"), getMessage("ExportSaveErrorMessage"));
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                this.handleError(ex, getMessage("ExportSaveErrorTitle"), getMessage("ExportSaveErrorMessage"));
            }
        }
    }

    public void setWorkspace(Workspace wrkspc) {
        workspace = wrkspc;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public boolean cancel() {
        return cancelled = true;
    }

    public void setProgressTicket(ProgressTicket pt) {
        ticket = pt;
    }

    public void setOutputStream(OutputStream out) {
        outputStream = out;
    }

    private void writeSummaryToOutput(String exportFileGraph, String exportFileSettings) {
        try {
            String message = "Thank you for using the Fieldnotes plugin! Your settings were saved in two files:" +  
                System.lineSeparator() +
                System.lineSeparator() +  
                String.format("'%s' contains the graph settings", exportFileGraph) +
                System.lineSeparator() +
                System.lineSeparator() +              
                String.format("'%s' contains various other settings.", exportFileSettings) +
                System.lineSeparator() +
                System.lineSeparator() + 
                "Read more about these files and their content at https://github.com/UUDigitalHumanitieslab/gephi-plugins/tree/fieldnotes" +
                System.lineSeparator() + 
                System.lineSeparator() + 
                System.lineSeparator() +
                "UTRECHT DATA SCHOOL" +
                System.lineSeparator() +
                "DIGITAL HUMANITIES LAB" +
                System.lineSeparator() +
                "Utrecht University 2018";
            
            this.outputStream.write(message.getBytes(), 0, message.length());        
        } catch (IOException ex) {
            this.handleError(ex, "Error while writing to file", ex.getMessage());
        }  
    }

    private void handleError(Exception ex, String dialogTitle, String dialogMessage) {
        logError(ex);

        JOptionPane.showMessageDialog(null, dialogMessage, dialogTitle, JOptionPane.ERROR_MESSAGE);
    }

    private void logError(Exception ex) {
        this.logMessage(Level.SEVERE, ex.getMessage());
        ex.printStackTrace();
    }

    private void logMessage(Level level, String message) {
        Logger.getLogger(SettingsExporter.class.getName()).log(level, message);
    }
}
