/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author henri
 */
public class ResourceManager {
    private HashMap<String, Double> resourcesAvaliable;
    private HashMap<String, Double> resourcesReserved;

    public ResourceManager() {
        resourcesAvaliable = new HashMap<>();
        resourcesReserved = new HashMap<>();
    }
    
    public boolean addResource(String name, double value){
        if(!resourcesAvaliable.containsKey(name)){
            resourcesAvaliable.put(name, 0.0);
            resourcesReserved.put(name, 0.0);
        }
        
        resourcesAvaliable.put(name, resourcesAvaliable.get(name) + value);
        return true;
    }
    
    public boolean consumeResource(String name, double value){
        if(!resourcesAvaliable.containsKey(name)){
            return false;
        }
        
        double initialV = resourcesReserved.get(name);
        
        if(initialV >= value){
            resourcesReserved.put(name, initialV - value);
        }else{
            resourcesReserved.put(name, 0.0);
            
            double avaliable = resourcesAvaliable.get(name);
            resourcesAvaliable.put(name, avaliable - (initialV - value));
            
            if(avaliable < (initialV - value)){
                return false;
            }
        }
        
        return true;
    }
    
    public boolean alocateResources(String name, double value){
        if(!resourcesAvaliable.containsKey(name)){
            return false;
        }
        
        double initialV = resourcesAvaliable.get(name);
        
        if(initialV >= value){
            resourcesAvaliable.put(name, initialV - value);
            resourcesReserved.put(name, resourcesReserved.get(name) + value);
        }
        
        return initialV >= value;
    }

    public boolean isAvaliable(HashMap<String, Double> resourceContext) {
        if(resourceContext == null){
            return false;
        }
        
        if(!resourcesAvaliable.keySet().containsAll(resourceContext.keySet())){
            return false;
        }
        
        for(String key : resourceContext.keySet()){
            if(resourceContext.get(key) > resourcesAvaliable.get(key)){
                return false;
            }
        }
        
        return true;
    }

    boolean checkCompatibility(HashMap<String, Double> resourceContext, HashMap<String, Double> resourceContext0) {
        HashSet<String> keys = new HashSet<>(resourceContext.keySet());
        keys.addAll(resourceContext0.keySet());
        
        HashMap<String, Double> resources = new HashMap<>(keys.size());
        
        for(String k : keys){
            double value = 0.0;
            
            if(resourceContext.containsKey(k)){
                value += resourceContext.get(k);
            }
            
            if(resourceContext0.containsKey(k)){
                value += resourceContext0.get(k);
            }
            
            resources.put(k, value);
        }
        
        return isAvaliable(resources);
    }
    
}
