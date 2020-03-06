/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;

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
    private JTextPane scriptQueue;

    private JTextPane output;
    
    private Boolean running = false;

    public MainControl() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);

        ImageIcon playI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/play.png"));
        ImageIcon pauseI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/pause.png"));
        ImageIcon skipI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/skip.png"));
        ImageIcon stopI = new ImageIcon(this.getClass().getClassLoader().getResource("utfpr/edu/bbgp/simul/gui/icons/stop.png"));

        stop = new JButton(stopI);
        play_pause = new JButton(playI);
        play_to = new JButton(skipI);
        
        play_pause.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                running = !running;
                
                if(running){
                    play_pause.setIcon(pauseI);
                }else{
                    play_pause.setIcon(playI);
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

        toolP.add(stop);
        toolP.add(play_pause);
        toolP.add(play_upto);
        toolP.add(play_to);
        
        scriptQueue = new JTextPane();
        scriptInput = new JTextField();
        
        scriptInput.setPreferredSize(new Dimension(200, 30));
        
        JPanel scriptPanel = new JPanel(new BorderLayout(6, 6));

        scriptPanel.add(scriptQueue, BorderLayout.CENTER);
        scriptPanel.add(scriptInput, BorderLayout.PAGE_START);
        
        
        beliefBase = new JTextPane();
        intentions = new JTextPane();
        intentions.setPreferredSize(new Dimension(200, 300));
        output = new JTextPane();
        output.setPreferredSize(new Dimension(400, 100));
        
        JPanel innerP = new JPanel(new BorderLayout(6, 6));
        
        innerP.add(beliefBase, BorderLayout.CENTER);
        innerP.add(intentions, BorderLayout.EAST);
        innerP.add(output, BorderLayout.PAGE_END);
        
        BorderLayout layout = new BorderLayout();
        JPanel outerP = new JPanel(layout);

        outerP.add(toolP, BorderLayout.PAGE_START);
        outerP.add(scriptPanel, BorderLayout.WEST);

        outerP.add(innerP, BorderLayout.CENTER);
        
        this.add(outerP);
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu("File"));
        
        this.setJMenuBar(menuBar);
    }
    
    protected void runOnce(){}
    protected void stopRunning(){
        if(running){
            running = false;
        }
    }
    protected void run(){
        if(!running){
            //thread run
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new MainControl().setVisible(true);
    }

    boolean runCycle() {
        return running;
    }

    void updateInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
