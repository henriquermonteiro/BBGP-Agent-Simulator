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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.GoalMemory;
import utfpr.edu.bbgp.agent.GoalMemoryPacket;
import utfpr.edu.bbgp.agent.parser.PerceptionsParser;
import utfpr.edu.bbgp.simul.utils.Tuple;

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

    private JList<PerceptionEntry> scriptQueue;
//    private JList<GoalMemory> goalList;
    private JTree goalsTree;
    private DefaultMutableTreeNode goalsTreeRoot;
    private ArgumentionFramework cluster;
//    private Boolean clusterLocked = false;
    private JTree explanationsTree;
    private DefaultMutableTreeNode explanationsTreeRoot;
    private JTextArea explanationArea;

    private JSplitPane splitPanelKB;
    private JSplitPane splitPanelGM;
    private JSplitPane splitPanelExplaination;

//    private JTextPane output;

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
//        output = new JTextPane();
//        output.setPreferredSize(new Dimension(400, 100));

        splitPanelKB = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPanelKB.setLeftComponent(new JScrollPane(scriptQueue) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }
        });
        splitPanelKB.setRightComponent(new JScrollPane(beliefBase) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }
        });

        JPanel kbInspectorPanel = new JPanel(new BorderLayout());

        kbInspectorPanel.add(toolP, BorderLayout.PAGE_START);
        kbInspectorPanel.add(splitPanelKB, BorderLayout.CENTER);

        cluster = new ArgumentionFramework();
        cluster.setEmptyMessage("No relevant arguments.");
        cluster.setScaling(1.5);

        GridBagLayout gBLayout = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 32, 2, 2);
        gridBagConstraints.anchor = GridBagConstraints.CENTER;

        JPanel cPanel = new JPanel(gBLayout);
        cPanel.add(cluster, gridBagConstraints);
        cPanel.setBackground(cluster.getBackground());

        JPanel afPanel = new JPanel(new BorderLayout(0, 5));
        afPanel.add(cluster.createDiagramColorLegend(FlowLayout.CENTER, 5, 5), BorderLayout.NORTH);
        afPanel.add(new JScrollPane(cPanel), BorderLayout.CENTER);
        afPanel.setMinimumSize(new Dimension(0, 0));

//        goalList = new JList<>(new DefaultListModel<>());
//        goalList.setCellRenderer(new GoalMemoryListCellRenderer());
//        goalList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//
//        goalList.addListSelectionListener((event) -> {
//            if (event.getFirstIndex() < 0) {
//                return;
//            }
//
//            GoalMemory gM = goalList.getSelectedValue();
//            if (gM != null) {
//                gM.showInCluster(cluster);
//            }
//
//        });

//        goalList.setSelectedIndex(0);

        splitPanelGM = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

//        splitPanelGM.setLeftComponent(new JScrollPane(goalList) {
//            @Override
//            public Dimension getMinimumSize() {
//                return new Dimension(0, 0);
//            }
//        });
        splitPanelGM.setRightComponent(afPanel);

        goalsTreeRoot = new DefaultMutableTreeNode("root");
        goalsTree = new JTree(goalsTreeRoot);
        
        GoalMemoryTreeCellRenderer treeRenderer = new GoalMemoryTreeCellRenderer();
        treeRenderer.setLeafIcon(null);
        treeRenderer.setClosedIcon(null);
        treeRenderer.setOpenIcon(null);
        
        goalsTree.setCellRenderer(treeRenderer);
        goalsTree.setRootVisible(false);
        goalsTree.setShowsRootHandles(true);
        goalsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        goalsTree.addTreeSelectionListener((arg0) -> {
            if (arg0.getNewLeadSelectionPath() == null) {
                return;
            }

            TreeExplanationEntry entry = (TreeExplanationEntry) ((DefaultMutableTreeNode) arg0.getNewLeadSelectionPath().getLastPathComponent()).getUserObject();

            if (entry != null) {
//                new Thread(){
//                    @Override
//                    public void run() {
//                        int tries = 0;
//                        synchronized(this){
//                            while(clusterLocked){
//                                try {
//                                    tries++;
//                                    wait(100);
//                                } catch (InterruptedException ex) {
//                                }
//
//                                if(tries > 10){
//                                    return;
//                                }
//                            }
//                            clusterLocked = true;
                            cluster.clear();
                            entry.showInCluster(cluster);
                            
                            System.out.println(entry.toString());
                            
                            Collection<Tuple<String, String>> list = aThread.getAgent().getArgumentIDAsList();
                            for(Tuple<String, String> t : list){
                                System.out.println("\t - " + t.getT() + " : " + t.getU());
                            }
//                            clusterLocked = false;
//                        }
//                    }
//                }.start();
            }
        });
        
        splitPanelGM.setLeftComponent(new JScrollPane(goalsTree) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }
        });
        
        explanationsTreeRoot = new DefaultMutableTreeNode("root");
        explanationsTree = new JTree(explanationsTreeRoot);
        
        treeRenderer = new GoalMemoryTreeCellRenderer();
        treeRenderer.setLeafIcon(null);
        treeRenderer.setClosedIcon(null);
        treeRenderer.setOpenIcon(null);
        
        explanationsTree.setCellRenderer(treeRenderer);
        explanationsTree.setRootVisible(false);
        explanationsTree.setShowsRootHandles(true);
        explanationsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        explanationsTree.addTreeSelectionListener((arg0) -> {
            if (arg0.getNewLeadSelectionPath() == null) {
                return;
            }

            TreeExplanationEntry entry = (TreeExplanationEntry) ((DefaultMutableTreeNode) arg0.getNewLeadSelectionPath().getLastPathComponent()).getUserObject();

            explanationArea.setText(entry.getExplanation());
        });

//        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
//        Icon noIcon = null;
//        renderer.setLeafIcon(noIcon);
//        renderer.setClosedIcon(noIcon);
//        renderer.setOpenIcon(noIcon);
//        explanationsTree.setCellRenderer(renderer);

        explanationArea = new JTextArea();
        explanationArea.setLineWrap(true);

        splitPanelExplaination = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

        splitPanelExplaination.setLeftComponent(new JScrollPane(explanationsTree) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }
        });
        splitPanelExplaination.setRightComponent(new JScrollPane(explanationArea) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }
        });

        JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
        tabPane.addTab("Belief inspector", kbInspectorPanel);
        tabPane.addTab("Goal memory inspector", splitPanelGM);
        tabPane.addTab("Goal explanation", splitPanelExplaination);

        JPanel innerP2 = new JPanel(new BorderLayout(6, 6));
        innerP2.add(toolP, BorderLayout.PAGE_START);
        innerP2.add(tabPane, BorderLayout.CENTER);

        this.add(innerP2);

        JMenuItem openAgent = new JMenuItem("Load agent");
        openAgent.addActionListener((arg0) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setCurrentDirectory(new File("." + File.separator));
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileNameExtensionFilter("*.bbgpagent", "bbgpagent"));

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    filePath = chooser.getSelectedFile().getCanonicalPath();

                    if (aThread != null) {
                        aThread.kill();
                    }

                    aThread = new AgentThread(AgentGenerator.getAgentFromFolBase(new FileReader(filePath)), this);
//                    ((DefaultListModel) goalList.getModel()).clear();
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
            chooser.setCurrentDirectory(new File("." + File.separator));
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

                    int pos = splitPanelKB.getInsets().left + scriptQueue.getPreferredSize().width;
                    Insets scrollInsets = ((JScrollPane) splitPanelKB.getLeftComponent()).getInsets();
                    pos += scrollInsets.left + scrollInsets.right;
                    JScrollBar vBar = ((JScrollPane) splitPanelKB.getLeftComponent()).getVerticalScrollBar();
                    pos += (vBar.isVisible() ? vBar.getWidth() : 0);
                    splitPanelKB.setDividerLocation(pos);
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
        checkingRuleString = checkingRuleString.replaceAll("\r\n", "\r\n\t ").replaceAll("\n\t $", "\n");
        
        String planLibrary = agent.getPlanLibraryToString();
        planLibrary = "Plan Library: \r\n" + planLibrary;
        planLibrary = planLibrary.replaceAll("\r\n", "\r\n\t ").replaceAll("\n\t $", "");
        
        beliefBase.setText(beliefBaseString.concat(resourcesBaseString.concat(standardRuleString.concat(activationRuleString.concat(evaluationRuleString.concat(deliberationRuleString.concat(checkingRuleString.concat(planLibrary))))))));

        ((PerceptionQueueListModel) scriptQueue.getModel()).setCycle(aThread.getAgent().getCycle());
        scriptQueue.revalidate();
        scriptQueue.repaint();

//        String gMemorytToString = "";

        ArrayList<GoalMemory> memoryEntries = new ArrayList<>();
        ArrayList<Long> pursuableEntriesCycles = new ArrayList<>();
//        DefaultListModel model = ((DefaultListModel) goalList.getModel());
        boolean changed = false;
        for(GoalMemoryPacket mP : agent.getGoalMemoryPackets()){
//            for (GoalMemory m : agent.getGoalMemory()) {
            for (GoalMemory m : mP.getAllGoalMemory()) {
//                gMemorytToString += m.toString() + "\n\n";

//                if (!model.contains(m)) {
//                    model.add(model.size(), m);
//                    changed = true;
//                }

    //            if (m.getGoalStage() == GoalStage.Choosen) {
                memoryEntries.add(m);
                if (!pursuableEntriesCycles.contains(m.getCycle())) {
                    pursuableEntriesCycles.add(m.getCycle());

                }
    //            }
            }
        }

//        if (changed) {
//            int pos = splitPanelGM.getInsets().left + goalList.getPreferredSize().width;
//            Insets scrollInsets = ((JScrollPane) splitPanelGM.getLeftComponent()).getInsets();
//            pos += scrollInsets.left + scrollInsets.right;
//            JScrollBar vBar = ((JScrollPane) splitPanelGM.getLeftComponent()).getVerticalScrollBar();
//            pos += (vBar.isVisible() ? vBar.getWidth() : 0);
//            splitPanelGM.setDividerLocation(pos);
//        }
//
//        if (goalList.getSelectedIndex() < 0) {
//            goalList.setSelectedIndex(0);
//        }

//        output.setText(gMemorytToString);

        DefaultTreeModel treeModel = (DefaultTreeModel) goalsTree.getModel();
        if (goalsTreeRoot.getChildCount() != pursuableEntriesCycles.size()) {
            Long[] cycleList = pursuableEntriesCycles.toArray(new Long[]{});
            Arrays.sort(cycleList);

            goalsTreeRoot.removeAllChildren();

            for (Long cycle : cycleList) {
                DefaultMutableTreeNode cycleNode = null;
                DefaultMutableTreeNode cycleNodeActive = null;
                double cycleNodeActivePreference = -1.0;
                DefaultMutableTreeNode cycleNodePursuable = null;
                double cycleNodePursuablePreference = -1.0;
                DefaultMutableTreeNode cycleNodeChosen = null;
                double cycleNodeChosenPreference = -1.0;
                DefaultMutableTreeNode cycleNodeExecutible = null;
                double cycleNodeExecutiblePreference = -1.0;
                for (GoalMemory gM : memoryEntries) {
                    if (Objects.equals(gM.getCycle(), cycle)) {
                        if (cycleNode == null) {
                            cycleNode = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.CYCLE_TYPE), true);
                        }

                        double pref = gM.getGoalPreference();

                        switch (gM.getGoalStage()) {
                            case Active:
                                if (cycleNodeActive == null) {
                                    cycleNodeActive = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodeActivePreference = pref;
                                } else if (pref > cycleNodeActivePreference) {
                                    cycleNodeActive.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodeActivePreference = pref;
                                }

                                cycleNodeActive.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                            case Pursuable:
                                if (cycleNodePursuable == null) {
                                    cycleNodePursuable = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodePursuablePreference = pref;
                                } else if (pref > cycleNodePursuablePreference) {
                                    cycleNodePursuable.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodePursuablePreference = pref;
                                }

                                cycleNodePursuable.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                            case Chosen:
                                if (cycleNodeChosen == null) {
                                    cycleNodeChosen = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodeChosenPreference = pref;
                                } else if (pref > cycleNodeChosenPreference) {
                                    cycleNodeChosen.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodeChosenPreference = pref;
                                }

                                cycleNodeChosen.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                            case Executive:
                                if (cycleNodeExecutible == null) {
                                    cycleNodeExecutible = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodeExecutiblePreference = pref;
                                } else if (pref > cycleNodeExecutiblePreference) {
                                    cycleNodeExecutible.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodeExecutiblePreference = pref;
                                }

                                cycleNodeExecutible.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                        }
                    }
                }

                if (cycleNodeActive != null) {
                    cycleNode.add(cycleNodeActive);
                }
                if (cycleNodePursuable != null) {
                    cycleNode.add(cycleNodePursuable);
                }
                if (cycleNodeChosen != null) {
                    cycleNode.add(cycleNodeChosen);
                }
                if (cycleNodeExecutible != null) {
                    cycleNode.add(cycleNodeExecutible);
                }

                if (!cycleNode.isLeaf()) {
                    goalsTreeRoot.add(cycleNode);
                }
            }

            treeModel.reload();

            goalsTreeRoot.children().asIterator().forEachRemaining((arg0) -> {
                DefaultMutableTreeNode el = (DefaultMutableTreeNode) arg0;
                TreePath path = new TreePath(((DefaultMutableTreeNode)el.getChildAt(0)).getPath());
                goalsTree.makeVisible(path);
                el.children().asIterator().forEachRemaining((arg1) -> {
                    DefaultMutableTreeNode el2 = (DefaultMutableTreeNode) arg1;
                    TreePath path2 = new TreePath(((DefaultMutableTreeNode) el2.getChildAt(0)).getPath());
                    goalsTree.makeVisible(path2);
                });
            });

            int pos = splitPanelGM.getInsets().left + goalsTree.getPreferredSize().width;
            Insets scrollInsets = ((JScrollPane) splitPanelGM.getLeftComponent()).getInsets();
            pos += scrollInsets.left + scrollInsets.right;
            JScrollBar vBar = ((JScrollPane) splitPanelGM.getLeftComponent()).getVerticalScrollBar();
            pos += (vBar.isVisible() ? vBar.getWidth() : 0);
            splitPanelGM.setDividerLocation(pos);
        }
        

//        DefaultTreeModel treeModel = (DefaultTreeModel) explanationsTree.getModel();
        treeModel = (DefaultTreeModel) explanationsTree.getModel();
        if (explanationsTreeRoot.getChildCount() != pursuableEntriesCycles.size()) {
            Long[] cycleList = pursuableEntriesCycles.toArray(new Long[]{});
            Arrays.sort(cycleList);

            explanationsTreeRoot.removeAllChildren();

            for (Long cycle : cycleList) {
                DefaultMutableTreeNode cycleNode = null;
                DefaultMutableTreeNode cycleNodeActive = null;
                double cycleNodeActivePreference = -1.0;
                DefaultMutableTreeNode cycleNodePursuable = null;
                double cycleNodePursuablePreference = -1.0;
                DefaultMutableTreeNode cycleNodeChosen = null;
                double cycleNodeChosenPreference = -1.0;
                DefaultMutableTreeNode cycleNodeExecutible = null;
                double cycleNodeExecutiblePreference = -1.0;
                for (GoalMemory gM : memoryEntries) {
                    if (Objects.equals(gM.getCycle(), cycle)) {
                        if (cycleNode == null) {
                            cycleNode = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.CYCLE_TYPE), true);
                        }

                        double pref = gM.getGoalPreference();

                        switch (gM.getGoalStage()) {
                            case Active:
                                if (cycleNodeActive == null) {
                                    cycleNodeActive = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodeActivePreference = pref;
                                } else if (pref > cycleNodeActivePreference) {
                                    cycleNodeActive.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodeActivePreference = pref;
                                }

                                cycleNodeActive.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                            case Pursuable:
                                if (cycleNodePursuable == null) {
                                    cycleNodePursuable = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodePursuablePreference = pref;
                                } else if (pref > cycleNodePursuablePreference) {
                                    cycleNodePursuable.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodePursuablePreference = pref;
                                }

                                cycleNodePursuable.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                            case Chosen:
                                if (cycleNodeChosen == null) {
                                    cycleNodeChosen = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodeChosenPreference = pref;
                                } else if (pref > cycleNodeChosenPreference) {
                                    cycleNodeChosen.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodeChosenPreference = pref;
                                }

                                cycleNodeChosen.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                            case Executive:
                                if (cycleNodeExecutible == null) {
                                    cycleNodeExecutible = new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE), true);
                                    cycleNodeExecutiblePreference = pref;
                                } else if (pref > cycleNodeExecutiblePreference) {
                                    cycleNodeExecutible.setUserObject(new TreeExplanationEntry(gM, TreeExplanationEntry.STAGE_TYPE));
                                    cycleNodeExecutiblePreference = pref;
                                }

                                cycleNodeExecutible.add(new DefaultMutableTreeNode(new TreeExplanationEntry(gM, TreeExplanationEntry.GOAL_TYPE), false));
                                break;
                        }
                    }
                }

                if (cycleNodeActive != null) {
                    cycleNode.add(cycleNodeActive);
                }
                if (cycleNodePursuable != null) {
                    cycleNode.add(cycleNodePursuable);
                }
                if (cycleNodeChosen != null) {
                    cycleNode.add(cycleNodeChosen);
                }
                if (cycleNodeExecutible != null) {
                    cycleNode.add(cycleNodeExecutible);
                }

                if (!cycleNode.isLeaf()) {
                    explanationsTreeRoot.add(cycleNode);
                }
            }

            treeModel.reload();

            explanationsTreeRoot.children().asIterator().forEachRemaining((arg0) -> {
                DefaultMutableTreeNode el = (DefaultMutableTreeNode) arg0;
                TreePath path = new TreePath(((DefaultMutableTreeNode)el.getChildAt(0)).getPath());
                explanationsTree.makeVisible(path);
                el.children().asIterator().forEachRemaining((arg1) -> {
                    DefaultMutableTreeNode el2 = (DefaultMutableTreeNode) arg1;
                    TreePath path2 = new TreePath(((DefaultMutableTreeNode) el2.getChildAt(0)).getPath());
                    explanationsTree.makeVisible(path2);
                });
            });

            int pos = splitPanelExplaination.getInsets().left + explanationsTree.getPreferredSize().width;
            Insets scrollInsets = ((JScrollPane) splitPanelExplaination.getLeftComponent()).getInsets();
            pos += scrollInsets.left + scrollInsets.right;
            JScrollBar vBar = ((JScrollPane) splitPanelExplaination.getLeftComponent()).getVerticalScrollBar();
            pos += (vBar.isVisible() ? vBar.getWidth() : 0);
            splitPanelExplaination.setDividerLocation(pos);
        }
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
