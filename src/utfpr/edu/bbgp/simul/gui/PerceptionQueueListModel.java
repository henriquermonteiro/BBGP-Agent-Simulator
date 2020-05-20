/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import utfpr.edu.bbgp.agent.PerceptionEntry;
import java.util.ArrayList;
import java.util.TreeSet;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author henri
 */
public class PerceptionQueueListModel implements ListModel<PerceptionEntry> {

    protected final ArrayList<ListDataListener> listeners;
    protected final TreeSet<PerceptionEntry> active;
    protected final TreeSet<PerceptionEntry> inactive;
    protected Long cycle;

    public PerceptionQueueListModel() {
        listeners = new ArrayList<>();
        active = new TreeSet<>();
        inactive = new TreeSet<>();
        cycle = -1l;
    }

    public long getCycle() {
        return cycle;
    }

    public void setCycle(Long cycle) {
        boolean updated = !this.cycle.equals(cycle);
        this.cycle = cycle;

        if (updated) {
            updatedCycle();
        }
    }

    protected void updatedCycle() {
        ArrayList<PerceptionEntry> entriesInactivated = new ArrayList<>();
        ArrayList<PerceptionEntry> entriesActivated = new ArrayList<>();

        active.stream().filter((arg0) -> {
            return arg0.getCycle() < (cycle);
        }).forEach((arg0) -> {
            entriesInactivated.add(arg0);
        });
        
        inactive.stream().filter((arg0) -> {
            return arg0.getCycle() >= (cycle);
        }).forEach((arg0) -> {
            entriesActivated.add(arg0);
        });

        entriesInactivated.stream().forEach((arg0) -> {
            active.remove(arg0);
            inactive.add(arg0);
            notifyChangeSwap(arg0);
        });

        entriesActivated.stream().forEach((arg0) -> {
            inactive.remove(arg0);
            active.add(arg0);
            notifyChangeSwap(arg0);
        });
    }

    @Override
    public int getSize() {
        return active.size() + inactive.size();
    }
    
    public boolean isActiveElementAt(int arg0) {
        return arg0 < active.size();
    }

    @Override
    public PerceptionEntry getElementAt(int arg0) {
        if (arg0 < active.size()) {
            int k = 0;
            for (PerceptionEntry p : active) {
                if (k == arg0) {
                    return p;
                }

                k++;
            }
        }
        if (arg0 < active.size() + inactive.size()) {
            int k = active.size();
            for (PerceptionEntry p : inactive) {
                if (k == arg0) {
                    return p;
                }

                k++;
            }
        }

        return null;
    }

    public void addAllPerceptionEntry(ArrayList<PerceptionEntry> added) {
        active.addAll(added);
        updatedCycle();
        
        notifyChangeAddedAll(added);
    }

    public void addPerceptionEntry(PerceptionEntry added) {
        active.add(added);
        updatedCycle();
        
        notifyChangeAdded(added);
    }
    
    protected void notifyChangeSwap(PerceptionEntry entry){
        notifyChangeAdded(null);
    }
    
    protected void notifyChangeAdded(PerceptionEntry added){
        listeners.forEach((arg0) -> {
            arg0.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, this.getSize()));
        });
    }
    
    protected void notifyChangeAddedAll(ArrayList<PerceptionEntry> added){
        notifyChangeAdded(null);
    }

    @Override
    public void addListDataListener(ListDataListener arg0) {
        listeners.add(arg0);
    }

    @Override
    public void removeListDataListener(ListDataListener arg0) {
        listeners.remove(arg0);
    }

}
