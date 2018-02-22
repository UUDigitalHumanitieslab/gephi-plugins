package edu.dhlabuu.fieldnotes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gephi.graph.api.Column;
import org.openide.util.NbBundle;

/**
 * Find columns with geographic attributes in Data Laboratory.
 * 
 * @author Dave Shepard
 */
public class GeoAttributeFinder {

    private Column longitudeColumn;
    private Column latitudeColumn;

    private String getMessage(String resource) {
        return NbBundle.getMessage(GeoAttributeFinder.class, resource);
    }

    Column[] findGeoFields(Column[] columns) {
        ArrayList<String> latAttributes = new ArrayList<String>();
        latAttributes.add(getMessage("Latitude"));
        for (String name : getMessage("LatitudeShortNames").split(",")) {
            latAttributes.add(name + "$");
        }
        latAttributes.add("^y$");
        for (String name : getMessage("LatitudeShortNames").split(",")) {
            latAttributes.add("(.*)" + name + "(.*)");
        }

        ArrayList<String> lonAttributes = new ArrayList<String>();
        lonAttributes.add(getMessage("Longitude"));
        for (String name : getMessage("LongitudeShortNames").split(",")) {
            lonAttributes.add(name + "$");
        }
        lonAttributes.add("^x$");
        for (String name : getMessage("LongitudeShortNames").split(",")) {
            latAttributes.add("(.*)" + name + "(.*)");
        }

        // find attributes by iterating over property names
        longitudeColumn = getAttributeField(lonAttributes.toArray(new String[0]), columns);
        latitudeColumn = getAttributeField(latAttributes.toArray(new String[0]), columns);
        Column[] result = {getLongitudeColumn(), getLatitudeColumn()};
        return result;
    }

    Column getAttributeField(String[] patterns, Column[] columns) {
        for (Column col : columns) {
            for (String str : patterns) {
                Pattern pattern = Pattern.compile(str, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(col.getTitle());
                if (matcher.find() && (col.getTypeClass() == Float.class
                        || col.getTypeClass() == Double.class
                        || col.getTypeClass() == BigDecimal.class
                        )) {
                    return col;
                }
            }
        }
        return null;
    }

    /**
     * @return the column selected as the longitude column
     */
    public Column getLongitudeColumn() {
        return longitudeColumn;
    }

    /**
     * @return the column selected as the latitude column
     */
    public Column getLatitudeColumn() {
        return latitudeColumn;
    }
}
