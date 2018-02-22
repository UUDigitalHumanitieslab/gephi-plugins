package edu.dhlabuu.fieldnotes;

import javax.swing.JPanel;

import org.gephi.graph.api.*;
import org.gephi.filters.api.*;
import org.gephi.io.exporter.spi.Exporter;
import org.gephi.io.exporter.spi.ExporterUI;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * UI for panel
 * 
 * @author Jelmer van Nuss
 */
@ServiceProvider(service = ExporterUI.class)
public class SettingsExporterUI implements ExporterUI {

    private ColumnSelectionPanel panel;
    private SettingsExporter exporter;

    private GraphModel model;


    private String getMessage(String message) {
        return NbBundle.getMessage(SettingsExporterUI.class, message);
    }

    @Override
    public JPanel getPanel() {
        // get all fields
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        ProjectController projectController = Lookup.getDefault().lookup(ProjectController.class);
        if (model == null) {
            model = graphController.getGraphModel(projectController.getCurrentWorkspace());
        }

        Table nodeTable = model.getNodeTable();
        Column[] columns = new Column[nodeTable.countColumns()];
        for (int i = 0; i < nodeTable.countColumns(); i++) {
            columns[i] = nodeTable.getColumn(i);
        }

        GraphModel graphModel = graphController.getGraphModel();
        Graph graph = graphModel.getGraphVisible();
        graph.readLock();
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        FilterModel filterModel = filterController.getModel();

        System.out.println("isDirected: " + graph.isDirected());
        System.out.println("timeZone: " + graph.getModel().getTimeZone());
        System.out.println("edgeCount: " + graph.getEdgeCount());
        System.out.println("nodeCount: " + graph.getNodeCount());
        System.out.println("attributeKeys: " + graph.getAttributeKeys());
        System.out.println("edgeTypeLabels: " + graph.getModel().getEdgeTypeLabels());
        System.out.println("configuration: " + graph.getModel().getConfiguration());
        Query[] queries = filterModel.getQueries();

        for (int i = 0; i < queries.length; i++)
        {
            Query query = queries[i];
            System.out.println("filter query: " + query.getName());
            for (int j = 0; j < query.getPropertiesCount(); j++) {
                System.out.println("\t" + query.getPropertyName(j) + ": " + query.getPropertyValue(j));
            }
        }
        // get geocoordinate fields
//        GeoAttributeFinder gaf = new GeoAttributeFinder();
//        gaf.findGeoFields(columns);
//        longitudeColumn = gaf.getLongitudeColumn();
//        latitudeColumn = gaf.getLatitudeColumn();
//        // for each column, create a new label, checkbox, lat radio button and lon radio button
//        // checkboxes are stored in a hash of objects, item to column name
//        panel = new ColumnSelectionPanel(columns, longitudeColumn, latitudeColumn);
        return panel;
    }

    @Override
    public void setup(Exporter exprtr) {
        exporter = (SettingsExporter)exprtr;
    }

    @Override
    public void unsetup(boolean update) {
        if (update)
        {
            // the user hit OK; save everything
//            exporter.setColumnsToUse(panel.getLongitudeColumn(),
//                    panel.getLatitudeColumn(),
//                    panel.getColumnsToExport()
//                )
//            exporter.setEdgeAndNodeDimensions(
//                panel.getMaxEdgeWidth(),
//                panel.getMaxNodeRadius()
//            );
        } else
            {
            // cancel was hit
        }
        panel = null;
        exporter = null;
    }

    @Override
    public boolean isUIForExporter(Exporter exporter) {
        if (exporter instanceof SettingsExporter) {
            this.exporter = (SettingsExporter)exporter;
        }
        return exporter instanceof SettingsExporter;
    }

    @Override
    public String getDisplayName() {
        return getMessage("UIDisplayName");
    }
}
