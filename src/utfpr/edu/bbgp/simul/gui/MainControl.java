/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import utfpr.edu.bbgp.agent.PerceptionEntry;
import utfpr.edu.bbgp.simul.utils.AgentGenerator;
import utfpr.edu.argumentation.diagram.ArgumentionFramework;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.GoalMemory;
import utfpr.edu.bbgp.agent.parser.PerceptionsParser;

/**
 *
 * @author henri
 */
public class MainControl extends JFrame {

    private JButton stop;
    private JButton play_pause;
    private JButton play_to;
    private JTextField play_upto;
    private Integer currentUpTo;

    private JTextPane beliefBase;

    private JTextPane intentions;

    private JTextField scriptInput;
    private JButton submitScript;
    private JList<PerceptionEntry> scriptQueue;
    private JList<GoalMemory> goalList;
    private ArgumentionFramework cluster;

    private JTextPane output;

    private Boolean running = false;

    private String filePath = "";
    private AgentThread aThread;
    private ArrayList<PerceptionEntry> perceptions;

    protected ImageIcon playI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/play.png"));
    protected ImageIcon pauseI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/pause.png"));

    public MainControl() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);

        perceptions = new ArrayList<>();

        ImageIcon skipI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/skip.png"));
        ImageIcon stopI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/stop.png"));

        stop = new JButton(stopI);
        play_pause = new JButton(playI);
        play_pause.setEnabled(false);
        play_to = new JButton(skipI);

        play_pause.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (play_pause.isEnabled()) {
                    if (!running) {
                        run();
                    } else {
                        stopRunning();
                    }
                }
            }
        });

        play_upto = new JFormattedTextField(NumberFormat.getIntegerInstance());
        play_upto.setPreferredSize(new Dimension(100, 46));
        play_upto.setText("1");

        currentUpTo = 1;

        play_upto.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                boolean update = false;
                int i = currentUpTo;
                try {
                    i = Integer.parseInt(play_upto.getText());

                    if (i > 0 && i < 100000) {
                        update = true;
                    }
                } catch (NumberFormatException ex) {
                } finally {
                    if (update) {
                        currentUpTo = i;
                    } else {
                        play_upto.setText(currentUpTo.toString());
                    }
                }
            }
        });
        play_upto.addActionListener((arg0) -> {
            boolean update = false;
            int i = currentUpTo;
            try {
                i = Integer.parseInt(play_upto.getText());

                if (i > 0 && i < 100000) {
                    update = true;
                }
            } catch (NumberFormatException ex) {
            } finally {
                if (update) {
                    currentUpTo = i;
                } else {
                    play_upto.setText(currentUpTo.toString());
                }
            }
        });

        JPanel toolP = new JPanel(new FlowLayout(FlowLayout.RIGHT));

//        toolP.add(stop);
        toolP.add(play_pause);
//        toolP.add(play_upto);
//        toolP.add(play_to);

        scriptQueue = new JList<>(new PerceptionQueueListModel());
        scriptQueue.setCellRenderer(new PerceptionQueueCellRenderer());
        scriptQueue.setPreferredSize(new Dimension(200, 30));

        beliefBase = new JTextPane();
        intentions = new JTextPane();
        intentions.setPreferredSize(new Dimension(200, 300));
        output = new JTextPane();
        output.setPreferredSize(new Dimension(400, 100));

        JSplitPane splitPanelKB = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPanelKB.setLeftComponent(new JScrollPane(scriptQueue));
        splitPanelKB.setRightComponent(new JScrollPane(beliefBase));

        JPanel kbInspectorPanel = new JPanel(new BorderLayout());

        kbInspectorPanel.add(toolP, BorderLayout.PAGE_START);
        kbInspectorPanel.add(splitPanelKB, BorderLayout.CENTER);
//
//        /*
//            [ => hasFractBone(man_32)] accepted
//            [ => fractBoneIs(man_32,arm)] accepted
//            [ => openFracture(man_32)] accepted
//        
//            [hasFractBone(man_32) => injuredSevere(man_32) [ => hasFractBone(man_32)]] accepted
//            [openFracture(man_32) -> injuredSevere(man_32) [ => openFracture(man_32)]] accepted
//            [fractBoneIs(man_32,arm) => !injuredSevere(man_32) [ => fractBoneIs(man_32,arm)]] rejected
//        
//            [injuredSevere(man_32) -> takeHospital(gHolder,man_32) [openFracture(man_32) -> injuredSevere(man_32) [ => openFracture(man_32)]]] accepted
//            [injuredSevere(man_32) -> takeHospital(gHolder,man_32) [hasFractBone(man_32) => injuredSevere(man_32) [ => hasFractBone(man_32)]]] accepted
//            [!injuredSevere(man_32) -> sendShelter(gHolder,man_32) [fractBoneIs(man_32,arm) => !injuredSevere(man_32) [ => fractBoneIs(man_32,arm)]]] rejected
//         */
//        Atom bel1 = new Atom("B1", false); // fractBoneIs(man_32,arm)
//        Atom bel2 = new Atom("B2", false); // hasFractBone(man_32)
//        Atom bel3 = new Atom("B3", false); // openFracture(man_32)
//        Atom bel4 = new Atom("B4", false); // injuredSevere(man_32)
//        Atom bel4_2 = new Atom("B4", false); // injuredSevere(man_32)
//        Atom bel5 = new Atom("B5", false); // takeHospital(gHolder,man_32)
//        Atom bel5_2 = new Atom("B5", false); // takeHospital(gHolder,man_32)
//        Atom bel6 = new Atom("B6", false); // sendShelter(gHolder,man_32)
//        Atom bel7 = new Atom("B7", false); // !injuredSevere(man_32)
//
//        Argument arg1 = new Argument(bel2, "Ast1", "", false, (Argument[]) null); // hasFractBone(man_32)
//        Argument arg2 = new Argument(bel1, "Ast2", "", false, (Argument[]) null); // fractBoneIs(man_32,arm)
//        Argument arg3 = new Argument(bel3, "Ast3", "", false, (Argument[]) null); // openFracture(man_32)
//
//        Argument arg4 = new Argument(bel4, "Ast4", "r_st^2", false, arg1); // hasFractBone(man_32) => injuredSevere(man_32)
//        Argument arg5 = new Argument(bel4_2, "Ast5", "r_st^4", true, arg3); // openFracture(man_32) -> injuredSevere(man_32)
//        Argument arg6 = new Argument(bel7, "Ast6", "r_st^3", false, arg2); // fractBoneIs(man_32,arm) => !injuredSevere(man_32)
//
//        Argument arg7 = new Argument(bel5, "Aac1", "r_ac^1", true, arg4); // injuredSevere(man_32) -> takeHospital(gHolder,man_32)
//        Argument arg8 = new Argument(bel5_2, "Aac2", "r_ac^1", true, arg5); // injuredSevere(man_32) -> takeHospital(gHolder,man_32)
//        Argument arg9 = new Argument(bel6, "Aac3", "r_ac^2", true, arg6); // !injuredSevere(man_32) -> sendShelter(gHolder,man_32)

        cluster = new ArgumentionFramework();
        cluster.setEmptyMessage("No relevant arguments.");
        cluster.setScaling(1.5);
//        cluster.addArgument(arg7);
//        cluster.addArgument(arg9);
//        cluster.addArgument(arg8);
//
//        cluster.addAttack(arg4, arg6, true);
//        cluster.addAttack(arg5, arg6, true);
//        cluster.addAttack(arg9, arg7, true);
//        cluster.addAttack(arg8, arg9, true);

        GridBagLayout gBLayout = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = GridBagConstraints.CENTER;

        JPanel cPanel = new JPanel(gBLayout);
        cPanel.add(cluster, gridBagConstraints);
        cPanel.setBackground(cluster.getBackground());

        JPanel afPanel = new JPanel(new BorderLayout(0, 5));
        afPanel.add(cluster.createDiagramColorLegend(FlowLayout.CENTER, 5, 5), BorderLayout.NORTH);
        afPanel.add(new JScrollPane(cPanel), BorderLayout.CENTER);

        goalList = new JList<>(new DefaultListModel<>());
        goalList.setCellRenderer(new GoalMemoryListCellRenderer());
        goalList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        goalList.addListSelectionListener((event) -> {
            if (event.getFirstIndex() < 0) {
                return;
            }

            GoalMemory gM = goalList.getSelectedValue();
            if (gM != null) {
                gM.showInCluster(cluster);
            }

        });

        goalList.setSelectedIndex(0);

        JSplitPane splitPanelGM = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

        splitPanelGM.setLeftComponent(new JScrollPane(goalList));
        splitPanelGM.setRightComponent(afPanel);

        JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
        tabPane.addTab("Belief inspector", kbInspectorPanel);
        tabPane.addTab("Goal memory inspector", splitPanelGM);

        JPanel innerP2 = new JPanel(new BorderLayout(6, 6));
        innerP2.add(toolP, BorderLayout.PAGE_START);
        innerP2.add(tabPane, BorderLayout.CENTER);

        this.add(innerP2);

        JMenuItem openAgent = new JMenuItem("Load agent");
        openAgent.addActionListener((arg0) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileNameExtensionFilter("*.bbgpagent", "bbgpagent"));

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    filePath = chooser.getSelectedFile().getCanonicalPath();

                    if (aThread != null) {
                        aThread.kill();
                    }

                    aThread = new AgentThread(AgentGenerator.getAgentFromFolBase(new FileReader(filePath)), this);
                    updateInfo(aThread.getAgent());
                    play_pause.setEnabled(true);

                } catch (IOException ex) {
                    Logger.getLogger(MainControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        JMenuItem openPerc = new JMenuItem("Load perceptions");
        openPerc.addActionListener((arg0) -> {
            if (aThread == null) {
                JOptionPane.showMessageDialog(this, "Agent must be loaded first.", "WARN: Agent not loaded", JOptionPane.WARNING_MESSAGE);

                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileNameExtensionFilter("*.perceptions", "perceptions"));

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    filePath = chooser.getSelectedFile().getCanonicalPath();

                    ListModel model = scriptQueue.getModel();

                    if (model instanceof PerceptionQueueListModel) {
                        perceptions = PerceptionsParser.parserPerceptionFile(aThread.getAgent().getSignature(), new FileReader(filePath));
                        ((PerceptionQueueListModel) model).addAllPerceptionEntry(perceptions);
                    }

                    updateInfo(aThread.getAgent());

                } catch (IOException ex) {
                    Logger.getLogger(MainControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        JMenu file = new JMenu("File");
        file.add(openAgent);
        file.add(openPerc);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(file);

        this.setJMenuBar(menuBar);

        this.setLocationRelativeTo(null);
    }

    public boolean hasPendingPerceptions() {
        ListModel model = scriptQueue.getModel();

        if (model instanceof PerceptionQueueListModel) {
            return !((PerceptionQueueListModel) model).active.isEmpty();
        }
        
        return false;
    }

    protected void runOnce() {
    }

    protected void stopRunning() {
        if (running) {
            running = false;
            play_pause.setIcon(playI);

            if (aThread != null) {
                aThread = new AgentThread(aThread.getAgent(), this);
            }
        }
    }

    protected void run() {
        if (!running) {
            aThread.start();
            running = true;

            play_pause.setIcon(pauseI);
        }
    }

    public boolean runCycle() {
        return running;
    }

    public void updateInfo(Agent agent) {
        if (agent == null) {
            return;
        }

        this.setTitle("" + agent.getCycle());
        String beliefBaseString = agent.getBeliefBaseToString();
        beliefBaseString = "Beliefs: \r\n" + beliefBaseString;
        beliefBaseString = beliefBaseString.replaceAll("\r\n", "\r\n\t ").replaceAll("\t $", "\n");

        String resourcesBaseString = agent.getResourceBaseToString();
        resourcesBaseString = "Resources: \r\n" + resourcesBaseString;
        resourcesBaseString = resourcesBaseString.replaceAll("\r\n", "\r\n\t ").replaceAll("\t $", "\n");

        String standardRuleString = agent.getStandardRulesToString();
        standardRuleString = "Standard Rules: \r\n" + standardRuleString;
        standardRuleString = standardRuleString.replaceAll("\r\n", "\r\n\t ").replaceAll("\t $", "\n");

        String activationRuleString = agent.getActivationRulesToString();
        activationRuleString = "Activation Rules: \r\n" + activationRuleString;
        activationRuleString = activationRuleString.replaceAll("\r\n", "\r\n\t ").replaceAll("\t $", "\n");

        String evaluationRuleString = agent.getEvaluationRulesToString();
        evaluationRuleString = "Evaluation Rules: \r\n" + evaluationRuleString;
        evaluationRuleString = evaluationRuleString.replaceAll("\r\n", "\r\n\t ").replaceAll("\t $", "\n");

        String deliberationRuleString = agent.getDeliberationRulesToString();
        deliberationRuleString = "Deliberation Rules: \r\n" + deliberationRuleString;
        deliberationRuleString = deliberationRuleString.replaceAll("\r\n", "\r\n\t ").replaceAll("\t $", "\n");

        String checkingRuleString = agent.getCheckingRulesToString();
        checkingRuleString = "Checking Rules: \r\n" + checkingRuleString;
        checkingRuleString = checkingRuleString.replaceAll("\r\n", "\r\n\t ").replaceAll("\n\t $", "");

        beliefBase.setText(beliefBaseString.concat(resourcesBaseString.concat(standardRuleString.concat(activationRuleString.concat(evaluationRuleString.concat(deliberationRuleString.concat(checkingRuleString)))))));

        ((PerceptionQueueListModel) scriptQueue.getModel()).setCycle(aThread.getAgent().getCycle());
        scriptQueue.revalidate();
        scriptQueue.repaint();

        String gMemorytToString = "";

        DefaultListModel model = ((DefaultListModel) goalList.getModel());
        for (GoalMemory m : agent.getGoalMemory()) {
            gMemorytToString += m.toString() + "\n\n";

            if (!model.contains(m)) {
                model.add(model.size(), m);
            }
        }

        if (goalList.getSelectedIndex() < 0) {
            goalList.setSelectedIndex(0);
        }

        output.setText(gMemorytToString);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new MainControl().setVisible(true);
    }

    public ArrayList<PerceptionEntry> getPerceptionsForCycle(long cycle) {
        ArrayList<PerceptionEntry> list = new ArrayList<>();
        perceptions.stream().filter((arg0) -> {
            return arg0.getCycle() == cycle;
        }).forEach((arg0) -> {
            list.add(arg0);
        });
        
        return list;
    }
}
