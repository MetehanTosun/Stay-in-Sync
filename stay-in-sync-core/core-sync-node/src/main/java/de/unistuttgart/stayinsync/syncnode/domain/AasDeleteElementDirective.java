package de.unistuttgart.stayinsync.syncnode.domain;

public class AasDeleteElementDirective {

    private String __directiveType;
    private String arcAlias;
    private String elementIdShortPath;

    public AasDeleteElementDirective() {}

    public AasDeleteElementDirective(String __directiveType,String arcAlias, String elementIdShortPath) {
        this.__directiveType = __directiveType;
        this.arcAlias = arcAlias;
        this.elementIdShortPath = elementIdShortPath;
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
}
