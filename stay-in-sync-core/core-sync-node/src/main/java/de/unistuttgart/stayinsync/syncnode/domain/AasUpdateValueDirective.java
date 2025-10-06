package de.unistuttgart.stayinsync.syncnode.domain;

public class AasUpdateValueDirective {

    private String __directiveType;
    private String arcAlias;
    private String elementIdShortPath;
    private Object value;

    public AasUpdateValueDirective() {}

    public AasUpdateValueDirective(String __directiveType, String arcAlias, String elementIdShortPath, Object value) {
        this.__directiveType = __directiveType;
        this.arcAlias = arcAlias;
        this.elementIdShortPath = elementIdShortPath;
        this.value = value;
    }

    public String get__directiveType() {
        return __directiveType;
    }
    public void set__directiveType(String __directiveType) {
        this.__directiveType = __directiveType;
    }
    public String getArcAlias() {
        return arcAlias;
    }
    public void setArcAlias(String arcAlias) {
        this.arcAlias = arcAlias;
    }
    public String getElementIdShortPath() {
        return elementIdShortPath;
    }
    public void setElementIdShortPath(String elementIdShortPath) {
        this.elementIdShortPath = elementIdShortPath;
    }
    public Object getValue() {
        return value;
    }
    public void setValue(Object value) {
        this.value = value;
    }
}
