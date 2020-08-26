package utfpr.edu.bbgp.agent;

import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author henri
 */
public class GoalMemoryPacket {
    
    private final Long agentCycle;
    private final Agent agent;
    private final ArrayList<GoalMemory> activationGoalMemory;
    private final ArrayList<GoalMemory> evaluationGoalMemory;
    private final ArrayList<GoalMemory> deliberationGoalMemory;
    private final ArrayList<GoalMemory> checkingGoalMemory;
    private final ArrayList<GoalMemory> completeGoalMemory;
    private final ArrayList<GoalMemory> cancelledGoalMemory;

    public GoalMemoryPacket(Long agentCycle, Agent agent) {
        this.agentCycle = agentCycle;
        this.agent = agent;
        
        activationGoalMemory = new ArrayList<>();
        evaluationGoalMemory = new ArrayList<>();
        deliberationGoalMemory = new ArrayList<>();
        checkingGoalMemory = new ArrayList<>();
        completeGoalMemory = new ArrayList<>();
        cancelledGoalMemory = new ArrayList<>();
    }
    
    public void addGoalMemory(GoalMemory entry){
        if(!Objects.equals(entry.getCycle(), agentCycle)){
            throw new IllegalArgumentException("GoalMemory entries should match the agent's cycle.");
        }
        
        ArrayList<GoalMemory> target;
        switch(entry.getGoalStage()){
            case Active:
                target = activationGoalMemory;
                break;
            case Pursuable:
                target = evaluationGoalMemory;
                break;
            case Chosen:
                target = deliberationGoalMemory;
                break;
            case Executive:
                target = checkingGoalMemory;
                break;
            case Completed:
                target = completeGoalMemory;
                break;
            case Cancelled:
                target = cancelledGoalMemory;
                break;
            default:
                throw new IllegalArgumentException("Invalid GoalMemory entry stage");
        }
        
        if(!target.contains(entry)){
            target.add(entry);
        }
    }

    public Long getAgentCycle() {
        return agentCycle;
    }

    public Agent getAgent() {
        return agent;
    }

    public ArrayList<GoalMemory> getActivationGoalMemory() {
        return activationGoalMemory;
    }

    public ArrayList<GoalMemory> getEvaluationGoalMemory() {
        return evaluationGoalMemory;
    }

    public ArrayList<GoalMemory> getDeliberationGoalMemory() {
        return deliberationGoalMemory;
    }

    public ArrayList<GoalMemory> getCheckingGoalMemory() {
        return checkingGoalMemory;
    }

    public ArrayList<GoalMemory> getAllGoalMemory() {
        ArrayList<GoalMemory> allEntries = new ArrayList<>(activationGoalMemory);
        allEntries.addAll(evaluationGoalMemory);
        allEntries.addAll(deliberationGoalMemory);
        allEntries.addAll(checkingGoalMemory);
        return allEntries;
    }
}
