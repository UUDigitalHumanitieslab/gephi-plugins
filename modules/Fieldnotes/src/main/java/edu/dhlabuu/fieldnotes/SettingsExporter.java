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
import org.gephi.graph.api.AttributeUtils;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Table;
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

        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        FilterModel filterModel = filterController.getModel();
        LayoutController layoutController = Lookup.getDefault().lookup(LayoutController.class);
        LayoutModel layoutModel = layoutController.getModel();

        if (exportVisible)
        {
            graph = model.getGraphVisible();
        } else
        {
            graph = model.getGraph();
        }
        graph.readLock();


        final Map<String, String> settings = new LinkedHashMap<String, String>();
        //logger.log(Level.INFO, "isDirected: {0}", graph.isDirected());

        Progress.setDisplayName(ticket, getMessage("WritingSettingsFile"));
        try
        {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            settings.put("isDirected", String.valueOf(graph.isDirected()));
            settings.put("timeZone: ", String.valueOf(graph.getModel().getTimeZone()));
            settings.put("edgeCount: ", String.valueOf(graph.getEdgeCount()));
            settings.put("nodeCount: ", String.valueOf(graph.getNodeCount()));
            settings.put("attributeKeys: ", String.valueOf(graph.getAttributeKeys()));

            Query[] queries = filterModel.getQueries();

            for (int i = 0; i < queries.length; i++)
            {
                Query query = queries[i];
                if (query.getPropertiesCount() > 0)
                {
                    for (int j = 0; j < query.getPropertiesCount(); j++)
                    {
                        settings.put(query.getPropertyName(j), String.valueOf(query.getPropertyValue(j)));
                    }
                }
            }

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
                        settings.put(propertyName, propertyValue);
                    }
                }
            }

            String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            String filepath = "settings_" + timeLog + ".settings";

            writeSettings(settings, filepath);
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

    private void writeSettings(Map<String, String> settings, String filepath) throws IOException
    {
        BufferedWriter writer = null;
        try {
            File file = new File(filepath);

            writer = new BufferedWriter(new FileWriter(file));

            for (Map.Entry<String, String> entry : settings.entrySet())
            {
                String key = entry.getKey();
                String value = entry.getValue();
                writer.write(key + ": " + value);
                writer.newLine();
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
