/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent.manager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import utfpr.edu.bbgp.extended.ResourceFolFormula;

/**
 *
 * @author henri
 */
public class ResourceManager {

    private HashMap<String, Double> resourcesAvaliable;
    private HashMap<String, Double> resourcesReserved;
    private boolean allowNegativeResources = false;

    public ResourceManager() {
        resourcesAvaliable = new HashMap<>();
        resourcesReserved = new HashMap<>();
    }

    public void allowNegativeResources(boolean allowNegativeResources) {
        this.allowNegativeResources = allowNegativeResources;
    }

    public Set<String> getAvailableResourcesNames() {
        return resourcesAvaliable.keySet();
    }

    public boolean addResource(String name, double value) {
        if (!resourcesAvaliable.containsKey(name)) {
            resourcesAvaliable.put(name, 0.0);
            resourcesReserved.put(name, 0.0);
        }

        if (resourcesAvaliable.get(name) + value < 0.0 && !allowNegativeResources) {
            resourcesAvaliable.put(name, 0.0);
        } else {
            resourcesAvaliable.put(name, resourcesAvaliable.get(name) + value);
        }

        return true;
    }

    public boolean consumeResource(String name, double value) {
        if (!resourcesAvaliable.containsKey(name)) {
            return false;
        }

        double initialV = resourcesReserved.get(name);

        if (initialV >= value) {
            resourcesReserved.put(name, initialV - value);
        } else {
            resourcesReserved.put(name, 0.0);

            double avaliable = resourcesAvaliable.get(name);
            resourcesAvaliable.put(name, avaliable - (initialV - value));

            if (avaliable < (initialV - value)) {
                return false;
            }
        }

        return true;
    }

    public boolean alocateResources(String name, double value) {
        if (!resourcesAvaliable.containsKey(name)) {
            return false;
        }

        double initialV = resourcesAvaliable.get(name);

        if (initialV >= value) {
            resourcesAvaliable.put(name, initialV - value);
            resourcesReserved.put(name, resourcesReserved.get(name) + value);
        }

        return initialV >= value;
    }

    public boolean isAvaliable(ResourceFolFormula resourceContext) {
        if (resourceContext == null) {
            return false;
        }

        if (!resourcesAvaliable.keySet().contains(resourceContext.getResourceName())) {
            return false;
        }

        String key = resourceContext.getResourceName();
        Double value = resourceContext.getAmount();
        if (value >= 0) {
            if (value > resourcesAvaliable.get(key)) {
                return false;
            }
        } else {
            value *= -1;
            if (value <= resourcesAvaliable.get(key)) {
                return false;
            }
        }

        return true;
    }

    public boolean isAvaliable(Set<ResourceFolFormula> resourceContext) {
        if (resourceContext == null) {
            return false;
        }

//        for (ResourceFolFormula resFF : resourceContext) {
//            if (!resourcesAvaliable.keySet().contains(resFF.getResourceName())) {
//                return false;
//            }
//        }

        for (ResourceFolFormula resFF : resourceContext) {
//            String key = resFF.getResourceName();
//            Double value = resFF.getAmount();
//            if (value >= 0) {
//                if (value > resourcesAvaliable.get(key)) {
//                    return false;
//                }
//            } else {
//                value *= -1;
//                if (value <= resourcesAvaliable.get(key)) {
//                    return false;
//                }
//            }
            if(!isAvaliable(resFF)){
                return false;
            }
        }

        return true;
    }

    public boolean checkCompatibility(HashSet<ResourceFolFormula> resourceContext, HashSet<ResourceFolFormula> resourceContext0) {
//        HashSet<String> keys = new HashSet<>(resourceContext.keySet());
//        keys.addAll(resourceContext0.keySet());
        HashSet<String> keys = new HashSet<>();
        HashMap<String, ResourceFolFormula> ctxt1 = new HashMap<>();
        HashMap<String, ResourceFolFormula> ctxt2 = new HashMap<>();
        resourceContext.forEach((arg0) -> {
            keys.add(arg0.getResourceName());
            ctxt1.put(arg0.getResourceName(), arg0);
        });
        resourceContext0.forEach((arg0) -> {
            keys.add(arg0.getResourceName());
            ctxt2.put(arg0.getResourceName(), arg0);
        });

        HashSet<ResourceFolFormula> resources = new HashSet<>(resourceContext.size());

        for (String k : keys) {
            double value;

            double valueC1 = 0.0;
            if (ctxt1.containsKey(k)) {
                valueC1 = ctxt1.get(k).getAmount();
            }

            double valueC2 = 0.0;
            if (ctxt2.containsKey(k)) {
                valueC2 = ctxt2.get(k).getAmount();
            }

            if (valueC1 >= 0 && valueC2 >= 0) {
                value = valueC1 + valueC2;
            } else if (valueC1 < 0 && valueC2 < 0) {
                value = Math.max(valueC1, valueC2);
            } else {
                if ((valueC1 + valueC2) < 0) {
                    value = Math.min(valueC1, valueC2);
                } else {
                    return false;
                }
            }

            resources.add(new ResourceFolFormula(k, value));
        }

        return isAvaliable(resources);
    }

    public Double getAvaliability(String res) {
        return (resourcesAvaliable.containsKey(res) ? resourcesAvaliable.get(res) : 0.0);
    }

}
