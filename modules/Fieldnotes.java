//Init a project - and therefore a workspace
ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
pc.newProject();
Workspace workspace = pc.getCurrentWorkspace();

//Get models and controllers for this new workspace - will be useful later
AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
ImportController importController = Lookup.getDefault().lookup(ImportController.class);
FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);

//Import file
Container container;
try {
    File file = new File(getClass().getResource("/org/gephi/toolkit/demos/resources/polblogs.gml").toURI());
    container = importController.importFile(file);
    container.getLoader().setEdgeDefault(EdgeDefault.DIRECTED);   //Force DIRECTED
} catch (Exception ex) {
    ex.printStackTrace();
    return;
}

//Append imported data to GraphAPI
importController.process(container, new DefaultProcessor(), workspace);

//See if graph is well imported
DirectedGraph graph = graphModel.getDirectedGraph();
System.out.println("Nodes: " + graph.getNodeCount());
System.out.println("Edges: " + graph.getEdgeCount());

//Filter
DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
degreeFilter.init(graph);
degreeFilter.setRange(new Range(30, Integer.MAX_VALUE));     //Remove nodes with degree < 30
Query query = filterController.createQuery(degreeFilter);
GraphView view = filterController.filter(query);
graphModel.setVisibleView(view);    //Set the filter result as the visible view

//See visible graph stats
UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
System.out.println("Nodes: " + graphVisible.getNodeCount());
System.out.println("Edges: " + graphVisible.getEdgeCount());

//Run YifanHuLayout for 100 passes - The layout always takes the current visible view
YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
layout.setGraphModel(graphModel);
layout.resetPropertiesValues();
layout.setOptimalDistance(200f);
layout.initAlgo();

for (int i = 0; i < 100 && layout.canAlgo(); i++) {
    layout.goAlgo();
}
layout.endAlgo();

//Get Centrality
GraphDistance distance = new GraphDistance();
distance.setDirected(true);
distance.execute(graphModel, attributeModel);

//Rank color by Degree
Ranking degreeRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, Ranking.DEGREE_RANKING);
AbstractColorTransformer colorTransformer = (AbstractColorTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);
colorTransformer.setColors(new Color[]{new Color(0xFEF0D9), new Color(0xB30000)});
rankingController.transform(degreeRanking,colorTransformer);

//Rank size by centrality
AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());
AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
sizeTransformer.setMinSize(3);
sizeTransformer.setMaxSize(10);
rankingController.transform(centralityRanking,sizeTransformer);

//Preview
model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(0.1f));
model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));

//Export
ExportController ec = Lookup.getDefault().lookup(ExportController.class);
try {
    ec.exportFile(new File("headless_simple.pdf"));
} catch (IOException ex) {
    ex.printStackTrace();
    return;
}

BufferedWriter writer;
public void writeSettings(Map<String, String> settings)
{
    try
    {
        DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        string time = timeStampPattern.format(java.time.LocalDateTime.now());
        File file = new File("settings_" + time + ".txt");
        file.createNewFile();

        writer = new BufferedWriter(new FileWriter(file));

        Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            writer.write(entry.getKey() + ": " + entry.getValue());
            writer.newLine();
    }
    catch(FileNotFoundException e)
    {
        System.out.println("File Not Found");
        System.exit(1);
    }
    catch(IOException e)
    {
        System.out.println("something messed up");
        System.exit(1);
    }
}