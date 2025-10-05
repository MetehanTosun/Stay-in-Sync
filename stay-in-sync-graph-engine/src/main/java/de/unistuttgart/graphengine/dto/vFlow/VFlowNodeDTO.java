package de.unistuttgart.graphengine.dto.vFlow;



/**
 * Represents a single node from ngx-vflow.
 */

public class VFlowNodeDTO {
    /**
     * The unique identifier of the node, typically a string.
     */
    private String id;

    /**
     * The visual position of the node on the canvas.
     */
    private PointDTO point;

    /**
     * The type of the node, used by the frontend to render the correct component.
     */
    private String type;

    /**
     * The visual width of the node.
     */
    private double width;

    /**
     * The visual height of the node.
     */
    private double height;

    /**
     * The data payload containing specific information for the logic engine.
     */
    private VFlowNodeDataDTO data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PointDTO getPoint() {
        return point;
    }

    public void setPoint(PointDTO point) {
        this.point = point;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public VFlowNodeDataDTO getData() {
        return data;
    }

    public void setData(VFlowNodeDataDTO data) {
        this.data = data;
    }
}
