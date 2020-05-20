/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.PerceptionEntry;

/**
 *
 * @author henri
 */
public class AgentThread extends Thread {

    private final Agent agent;
    private final MainControl control;

    public AgentThread(Agent agent, MainControl control) {
        this.agent = agent;
        this.control = control;
    }

    @Override
    public synchronized void run() {
        int noActivesCount = 0;
        agent.resetIdleCycleCount();

        while (control.runCycle() && !this.isInterrupted() && noActivesCount < 10) {
            control.getPerceptionsForCycle(agent.getCycle()).forEach((arg0) -> {
                if (arg0.isBeliefType()) {
                    if (arg0.getOperation() == PerceptionEntry.ADDITION_OPERATION) {
                        agent.addBelief(arg0.getBelief());
                    }else{
                        agent.removeBelief(arg0.getBelief());
                    }
                }else{
                    agent.addResource(arg0.getResource(), (arg0.getOperation() == PerceptionEntry.DELETION_OPERATION ? -1 : 1) * arg0.getResourceAmount());
                }
            });
            agent.singleCycle();
            control.updateInfo(agent);

            if (control.hasPendingPerceptions()) {
                agent.resetIdleCycleCount();
            }

            noActivesCount = agent.idleCyclesCount();
        }

        if (noActivesCount >= 10) {
            control.stopRunning();
        }
    }

    public Agent getAgent() {
        return agent;
    }

    public void kill() {
        this.interrupt();
    }
}
