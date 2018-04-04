# Gephi Fieldnotes

## Installation instructions

If you choose to build the project from this source, you can use the procedure as defined in https://github.com/gephi/gephi-plugins

If you choose to use the prebuilt `nbm` package from this repository, open Gephi and `Tools > Plugins > Downloaded > Add plugins...` and then select the nbm file from your hard disk to install Gephi Fieldnotes.

## What is Gephi Fieldnotes?

A Gephi user can have the need to share his research procedure by sharing intermediate graphs and results.
Currently, there is no way of saving most settings that are available in Gephi.
This plugin allows the user to create a timestamped snapshot of these settings and the graph itself.
This makes research reproducible as it allows the user to easily refer to a certain snapshot which lets the reader of the research follow the procedure step by step.

The settings that are to be saved are divided in various setting panels with different structures and properties.
Based on these panels, we identify the types General, Filter, Layout, Appearance, Statistics and Preview.
Everything that cannot be placed under the other types are clustered under General.
All settings are visually separated using a line break:
`------------------------------------------------------------------------------------------------------------------------`

In the following part, all elements in between `[brackets]` are to be replaced with the corresponding variable value.
For example, `[filter name]` can be used in practice as `Edge Weight` or some other filter name.


### General settings
General settings contain the following properties:
- `isDirectedGraph`: True/False depending on whether the graph is directed or undirected.
- `edgeCount`: Amount of edges in the graph.
- `nodeCount`: Amount of nodes in the graph.

### Filter settings
Filter settings contains a, possibly empty, list of numbered filters and their names.
The printed format looks like:
`# filter [number]: [filter name]`

After each filter line, the filter properties follow.
Filters can contain different properties, all of which are printed on a separate line as:
`[filter property name]: [filter property value]`

An example of the filter settings is:
```
## Filter settings
# filter 0: Edge Weight
range: 1.0 - 4.0
```

### Layout settings
The selected graph layout is shown by its name:
`# layout: [layout name]`
Layout settings contain various properties, all of which are printed on a separate line as:
`[layout property name]: [layout property value]`

### Appearance settings
Appearance settings result in changes to nodes or edges (the changes are in properties such as colour or size).
These changes directly affect the individual nodes and edges and are stored whenever the graph is saved as a `.gexf` file.
As these properties are automatically set whenever the `.gexf` is loaded, these settings are not saved in the settings file.
After all, why setting these options manually if it can be done automatically?

### Statistics settings
Statistics settings contain both the statistics models used, and the HTML results that are rendered.
Currently, we've been unable to retrieve the statistics results in a printable manner.

### Preview settings
Preview settings determine how the user sees the graph.
Storing these settings will contribute to the visual reproducibility of the graph.
All settings are applied to either a type `edge`, `node` or `arrow`, each with their own properties.
These properties are printed on a separate line as:
`[type].[preview property name]: [preview property value]`

Some properties use numbers as values.
These can simply be set to the value from the setting file.
For example: `node.border.width: 1.0`

Other properties use the booleans True/False.
In this case, there is a checkbox option where True means the box should be checked and False means the box should be left empty.
For example: `edge.show: true`

Color properties return a Java Color class where the `[r=?,g=?,b=?]` is important.
The `?` here stands for any value from 0 to 255 denoting the `r`ed, `g`reen and `b`lue values of the color respectively.
For example: `node.border.color: java.awt.Color[r=0,g=255,b=0]`

Font properties return a Java Font class where the `[family=?,name=?,style=?,size=?]` is of importance.
The `?` here stands for the font family, specific font name within the family, font style and font size respectively.
For example: `node.label.font: java.awt.Font[family=Dialog,name=Arial,style=plain,size=12]`

An example of the preview settings is:
```
## Preview settings
edge.show: true
node.border.width: 1.0
node.border.color: java.awt.Color[r=0,g=0,b=0]
node.opacity: 100.0
node.per.node.opacity: false
node.label.show: false
edge.label.show: false
edge.thickness: 1.0
edge.rescale-weight: false
edge.rescale-weight.min: 0.1
edge.rescale-weight.max: 1.0
edge.color: java.awt.Color[r=0,g=0,b=0]
edge.opacity: 100.0
edge.curved: true
edge.radius: 0.0
arrow.size: 3.0
node.label.font: java.awt.Font[family=Dialog,name=Arial,style=plain,size=12]
node.label.proportionalSize: true
node.label.color: java.awt.Color[r=0,g=0,b=0]
node.label.shorten: false
node.label.max-char: 30
node.label.outline.size: 0.0
node.label.outline.color: java.awt.Color[r=255,g=255,b=255]
node.label.outline.opacity: 80.0
node.label.box: false
node.label.box.color: java.awt.Color[r=0,g=0,b=0]
node.label.box.opacity: 100.0
edge.label.font: java.awt.Font[family=Dialog,name=Arial,style=plain,size=10]
edge.label.color: java.awt.Color[r=0,g=0,b=0]
edge.label.shorten: false
edge.label.max-char: 30
edge.label.outline.size: 0.0
edge.label.outline.color: java.awt.Color[r=255,g=255,b=255]
edge.label.outline.opacity: 80.0
```
