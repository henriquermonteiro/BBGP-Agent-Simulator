/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import utfpr.edu.bbgp.agent.Agent;

/**
 *
 * @author henri
 */
public class AgentThread extends Thread{
    private final Agent agent;
    private final MainControl control;

    public AgentThread(Agent agent, MainControl control) {
        this.agent = agent;
        this.control = control;
    }

    @Override
    public synchronized void run() {
        while(control.runCycle()){
            agent.singleCycle();
            control.updateInfo();
        }
    }

    public Agent getAgent() {
        return agent;
    }
}
