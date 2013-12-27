package edu.usc.cs.nsl.lookingglass.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author matt
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {
    
    private String lgName = "empty";
    private String type = "empty";
    private String target = "empty";
    
    public Request() {
    }

    public Request(String lgName, String target, String type) {
        this.lgName = lgName;
        this.target = target;
        this.type = type;
    }
    
    public void setLgName(String lgName) {
        this.lgName = lgName;
    }

    public String getLgName() {
        return lgName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "type: "+type+" lgName: "+lgName+" target: "+target;
    }
    
}
