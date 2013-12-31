package edu.usc.cs.nsl.lookingglass.service;

import java.util.List;

/**
 *
 * @author matt
 */
public class TracerouteResult {
    
    private String target;
    private String status;
    private String lgName;
    private List<String> hops;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLgName() {
        return lgName;
    }

    public void setLgName(String lgName) {
        this.lgName = lgName;
    }

    public List<String> getHops() {
        return hops;
    }

    public void setHops(List<String> hops) {
        this.hops = hops;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
    
}
