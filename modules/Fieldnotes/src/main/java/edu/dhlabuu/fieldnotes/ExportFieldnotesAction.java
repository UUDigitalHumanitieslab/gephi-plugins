package edu.dhlabuu.fieldnotes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.FilterModel;
import org.gephi.filters.api.Query;
import org.gephi.layout.api.LayoutController;
import org.gephi.layout.api.LayoutModel;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutProperty;
import org.gephi.appearance.api.*;
import org.gephi.statistics.api.*;
import org.gephi.preview.api.*;
import org.gephi.preview.types.*;
import org.gephi.graph.api.*;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Lookup;
import org.gephi.io.exporter.api.ExportController;

/**
 * Example of an action accessible from the "Plugins" menu un the menubar.
 * <p>
 * The annotations on the class defines the menu's name, position and class.
 * 
 * @author Jelmer van Nuss and Alex Hebing
 */
@ActionID(category = "File", id = "org.gephi.desktop.filters.ExportFieldnotes")
@ActionRegistration(displayName = "#CTL_ExportFieldnotes")
@ActionReferences({ @ActionReference(path = "Menu/Fieldnotes", position = 3333) })
@Messages("CTL_ExportFieldnotes=Export")
public final class ExportFieldnotesAction implements ActionListener {
    // The path selected by the user in JFileChooserDialog
    private File selectedFolder;

    // Keep the graph here so we can close it on errors
    private Graph graph;

    // Keep the logos icon here so it doesn't have to load for each panel
    private ImageIcon logos;

    // This is the entry point for the current action
    @Override
    public void actionPerformed(ActionEvent e) {
        this.logMessage(Level.INFO, "Start");        
        promptUserForFolder("."); // start folder selection from application current directory
    }

    private void handleFolderSelected() {
        this.logMessage(Level.INFO, "Folder selected");
        this.logMessage(Level.INFO, "Loading controllers and settings");

        this.init();

        // Create file paths and export graph as well as settings
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss").format(Calendar.getInstance().getTime());
        File settingsFile = new File(this.selectedFolder.getAbsolutePath(), "settings_" + timeStamp + ".txt");
        File graphFile = new File(this.selectedFolder.getAbsolutePath(), "graph_" + timeStamp + ".gexf");

        LinkedHashMap<String, Map<String, String>> settings = collectSettings();

        this.logMessage(Level.INFO, "Done loading controllers and settings. Starting export");

        writeSettings(settings, settingsFile);

        this.exportGraph(graphFile);

        this.messageToUser("Export Complete", "Fieldnotes Export Complete");

        this.dispose();

        this.logMessage(Level.INFO, "Done");
    }

    private void init() {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        this.graph = graphController.getGraphModel().getGraph();
        this.graph.readLock();

        // load the logo's from the resources
        this.logos = new ImageIcon(ExportFieldnotesAction.class.getResource("logos.jpg"));
    }

    private void dispose() {
        this.graph.readUnlock();
    }

    private void promptUserForFolder(String startPath) {
        final JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new java.io.File(startPath)); 
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // limit options to folders
        fc.setControlButtonsAreShown(false); // do not show default buttons: use Gephi's instead

        // This is the window the user will see, with event handling for 'OK' button
        DialogDescriptor dd = new DialogDescriptor(fc, "Choose folder to export to...", false, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (evt.getSource().equals(DialogDescriptor.OK_OPTION)) {
                    selectedFolder = fc.getSelectedFile();

                    if (Files.isWritable(selectedFolder.toPath())) {
                        handleFolderSelected();
                    }
                    else {
                        messageToUser("No permission", String.format("Cannot create files in folder '%s'. Please choose an different folder.", selectedFolder.getAbsolutePath()));
                        promptUserForFolder(selectedFolder.getAbsolutePath());
                    }
                }
            }
        });
        DialogDisplayer.getDefault().notify(dd);
    }

    private void exportGraph(File file) {
        // Export the full graph.
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);

        try {
            this.logMessage(Level.INFO, String.format("Starting export of graph to '%s'", file.getPath()));
            ec.exportFile(file);
            this.logMessage(Level.INFO, String.format("Succesfully exported to '%s'", file.getPath()));
        } catch (IOException ex) {
            this.handleError(ex, "Error Saving", "Sorry, file could not be written due to a disk error.");
        }
    }

    private LinkedHashMap<String, Map<String, String>> collectSettings() {
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
        // settingsList.put("## Appearance settings", appearanceSettings);
        settingsList.put("## Statistics settings", statisticsSettings);
        settingsList.put("## Preview settings", previewSettings);

        settings.put("isDirectedGraph", String.valueOf(graph.isDirected()));
        settings.put("edgeCount", String.valueOf(graph.getEdgeCount()));
        settings.put("nodeCount", String.valueOf(graph.getNodeCount()));

        addFiltersToSettings(filterSettings, filterModel);
        addLayoutToSettings(layoutSettings, layoutModel);
        // addAppearanceToSettings(appearanceSettings, appearanceModel, model, graph);
        addStatisticsToSettings(statisticsSettings, statisticsModel);
        addPreviewToSettings(previewSettings, previewModel);

        return settingsList;
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
                    // Canonical name is structured as "[layoutName].[propertyName].name", so take
                    // the second element.
                    String propertyName = layoutProperty.getCanonicalName().split("\\.")[1];
                    // String propertyName = layoutProperty.getProperty().getDisplayName();
                    String propertyValue = "Not extracted";
                    // System.out.println(layoutProperty.getProperty().getValue().toString());
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

    private void addAppearanceToSettings(Map<String, String> settings, AppearanceModel appearanceModel,
            GraphModel graphModel, Graph graph) {
        // These appearance settings should already be in the corresponding .gexf file.
        for (Node node : graph.getNodes()) {
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
                if (previewProperty.getType() == DependantColor.class) {
                    DependantColor dependantColor = DependantColor.class.cast(previewProperty.getValue());
                    Color color = dependantColor.getCustomColor();
                    previewPropertyValue = color.toString();
                } else if (previewProperty.getType() == DependantOriginalColor.class) {
                    DependantOriginalColor dependantOriginalColor = DependantOriginalColor.class
                            .cast(previewProperty.getValue());
                    Color color = dependantOriginalColor.getCustomColor();
                    previewPropertyValue = color.toString();
                } else if (previewProperty.getType() == EdgeColor.class) {
                    EdgeColor edgeColor = EdgeColor.class.cast(previewProperty.getValue());
                    Color color = edgeColor.getCustomColor();
                    previewPropertyValue = color.toString();
                }
                settings.put(previewPropertyName, previewPropertyValue);
            }
        }
    }

    private void writeSettings(LinkedHashMap<String, Map<String, String>> settingsList, File file) {
        // Export the settings.
        this.logMessage(Level.INFO, String.format("Attempting to write settings to '%s'", file.getAbsoluteFile()));

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));

            for (Map.Entry<String, Map<String, String>> settingsEntry : settingsList.entrySet()) {
                String settingsType = settingsEntry.getKey();
                Map<String, String> settings = settingsEntry.getValue();
                writer.write(
                        "------------------------------------------------------------------------------------------------------------------------");
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
            this.handleError(ex, "Error Saving", "Sorry, file could not be written due to a disk error.");
        } finally {
            try {
                writer.close();
                this.logMessage(Level.INFO,
                        String.format("Succesfully wrote settings to '%s'", file.getAbsolutePath()));
            } catch (Exception ex) {
                this.handleError(ex, "Error Saving", "Sorry, file could not be written due to a disk error.");
            }
        }
    }

    // Some helper methods:
    private void messageToUser(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE, this.logos);
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
        Logger.getLogger("").log(level, message);
    }
}